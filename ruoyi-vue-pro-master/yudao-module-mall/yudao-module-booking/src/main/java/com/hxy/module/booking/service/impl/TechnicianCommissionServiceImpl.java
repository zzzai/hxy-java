package com.hxy.module.booking.service.impl;

import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionConfigDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionDO;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionConfigMapper;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionMapper;
import com.hxy.module.booking.enums.AddonTypeEnum;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.enums.CommissionStatusEnum;
import com.hxy.module.booking.enums.CommissionTypeEnum;
import com.hxy.module.booking.enums.DispatchModeEnum;
import com.hxy.module.booking.service.BookingOrderService;
import com.hxy.module.booking.service.TechnicianCommissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Validated
@Slf4j
public class TechnicianCommissionServiceImpl implements TechnicianCommissionService {

    private static final String REVERSAL_BIZ_TYPE = "ORDER_CANCEL_REVERSAL";

    private final TechnicianCommissionMapper commissionMapper;
    private final TechnicianCommissionConfigMapper commissionConfigMapper;
    private final BookingOrderService bookingOrderService;

    public TechnicianCommissionServiceImpl(
            TechnicianCommissionMapper commissionMapper,
            TechnicianCommissionConfigMapper commissionConfigMapper,
            @Lazy BookingOrderService bookingOrderService) {
        this.commissionMapper = commissionMapper;
        this.commissionConfigMapper = commissionConfigMapper;
        this.bookingOrderService = bookingOrderService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateCommission(Long orderId) {
        BookingOrderDO order = bookingOrderService.getOrder(orderId);
        if (order == null) {
            log.warn("计算佣金失败，订单不存在，orderId={}", orderId);
            return;
        }
        if (!BookingOrderStatusEnum.COMPLETED.getStatus().equals(order.getStatus())) {
            log.warn("计算佣金失败，订单状态非已完成，orderId={}, status={}", orderId, order.getStatus());
            return;
        }
        // 防止重复计算
        List<TechnicianCommissionDO> existing = commissionMapper.selectListByOrderId(orderId);
        if (!existing.isEmpty()) {
            log.warn("佣金已计算，跳过，orderId={}", orderId);
            return;
        }

        // 确定佣金类型
        CommissionTypeEnum commissionType = resolveCommissionType(order);
        // 获取佣金比例
        BigDecimal rate = resolveCommissionRate(order.getStoreId(), commissionType);
        // 计算佣金金额
        int baseAmount = order.getPayPrice() != null ? order.getPayPrice() : 0;
        int commissionAmount = BigDecimal.valueOf(baseAmount)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        TechnicianCommissionDO commission = TechnicianCommissionDO.builder()
                .technicianId(order.getTechnicianId())
                .orderId(orderId)
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .commissionType(commissionType.getType())
                .baseAmount(baseAmount)
                .commissionRate(rate)
                .commissionAmount(commissionAmount)
                .status(CommissionStatusEnum.PENDING.getStatus())
                .build();
        commissionMapper.insert(commission);
        log.info("创建佣金记录，commissionId={}, orderId={}, technicianId={}, amount={}",
                commission.getId(), orderId, order.getTechnicianId(), commissionAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelCommission(Long orderId) {
        List<TechnicianCommissionDO> commissions = commissionMapper.selectListByOrderId(orderId);
        for (TechnicianCommissionDO commission : commissions) {
            if (CommissionStatusEnum.PENDING.getStatus().equals(commission.getStatus())
                    && !isReversalCommission(commission)) {
                TechnicianCommissionDO update = new TechnicianCommissionDO();
                update.setId(commission.getId());
                update.setStatus(CommissionStatusEnum.CANCELLED.getStatus());
                commissionMapper.updateById(update);
                log.info("取消佣金记录，commissionId={}, orderId={}", commission.getId(), orderId);
                continue;
            }
            if (CommissionStatusEnum.SETTLED.getStatus().equals(commission.getStatus())
                    && commission.getId() != null) {
                String reversalBizNo = buildReversalBizNo(commission);
                TechnicianCommissionDO existingReversal = findReversalByBizKey(
                        commissions, REVERSAL_BIZ_TYPE, reversalBizNo, commission.getTechnicianId());
                if (existingReversal != null) {
                    if (CommissionStatusEnum.CANCELLED.getStatus().equals(existingReversal.getStatus())) {
                        reactivateCancelledReversal(existingReversal, commission);
                        log.info("重置已取消的佣金冲正记录，reversalId={}, originCommissionId={}, orderId={}",
                                existingReversal.getId(), commission.getId(), orderId);
                    } else {
                        log.info("命中佣金冲正幂等键，跳过重复请求，originCommissionId={}, reversalId={}, orderId={}, bizNo={}",
                                commission.getId(), existingReversal.getId(), orderId, reversalBizNo);
                    }
                    continue;
                }
                TechnicianCommissionDO reversal = TechnicianCommissionDO.builder()
                        .technicianId(commission.getTechnicianId())
                        .orderId(commission.getOrderId())
                        .userId(commission.getUserId())
                        .storeId(commission.getStoreId())
                        .commissionType(commission.getCommissionType())
                        .baseAmount(negateAmount(commission.getBaseAmount()))
                        .commissionRate(commission.getCommissionRate())
                        .commissionAmount(negateAmount(commission.getCommissionAmount()))
                        .bizType(REVERSAL_BIZ_TYPE)
                        .bizNo(reversalBizNo)
                        .staffId(commission.getTechnicianId())
                        .status(CommissionStatusEnum.PENDING.getStatus())
                        .build();
                try {
                    commissionMapper.insert(reversal);
                    log.info("创建佣金冲正记录，originCommissionId={}, reversalCommissionId={}, orderId={}, bizNo={}",
                            commission.getId(), reversal.getId(), orderId, reversalBizNo);
                } catch (DuplicateKeyException ex) {
                    // 并发重复冲正请求由唯一键兜底，按幂等命中处理
                    log.info("命中佣金冲正幂等唯一键，跳过重复插入，originCommissionId={}, orderId={}, bizNo={}",
                            commission.getId(), orderId, reversalBizNo);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleCommission(Long commissionId) {
        TechnicianCommissionDO commission = commissionMapper.selectById(commissionId);
        if (commission == null) {
            return;
        }
        if (!CommissionStatusEnum.PENDING.getStatus().equals(commission.getStatus())) {
            return;
        }
        TechnicianCommissionDO update = new TechnicianCommissionDO();
        update.setId(commissionId);
        update.setStatus(CommissionStatusEnum.SETTLED.getStatus());
        update.setSettlementTime(LocalDateTime.now());
        commissionMapper.updateById(update);
        log.info("结算佣金，commissionId={}, technicianId={}, amount={}",
                commissionId, commission.getTechnicianId(), commission.getCommissionAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSettleByTechnician(Long technicianId) {
        List<TechnicianCommissionDO> pendingList = commissionMapper
                .selectListByTechnicianIdAndStatus(technicianId, CommissionStatusEnum.PENDING.getStatus());
        for (TechnicianCommissionDO commission : pendingList) {
            settleCommission(commission.getId());
        }
        log.info("批量结算技师佣金，technicianId={}, count={}", technicianId, pendingList.size());
    }

    @Override
    public List<TechnicianCommissionDO> getCommissionListByTechnician(Long technicianId) {
        return commissionMapper.selectListByTechnicianId(technicianId);
    }

    @Override
    public List<TechnicianCommissionDO> getPendingCommissionListByTechnician(Long technicianId) {
        return commissionMapper.selectListByTechnicianIdAndStatus(
                technicianId, CommissionStatusEnum.PENDING.getStatus());
    }

    @Override
    public List<TechnicianCommissionDO> getCommissionListByOrder(Long orderId) {
        return commissionMapper.selectListByOrderId(orderId);
    }

    @Override
    public int getPendingCommissionAmount(Long technicianId) {
        List<TechnicianCommissionDO> pendingList = getPendingCommissionListByTechnician(technicianId);
        return pendingList.stream()
                .mapToInt(c -> c.getCommissionAmount() != null ? c.getCommissionAmount() : 0)
                .sum();
    }

    /**
     * 根据订单信息确定佣金类型
     */
    private CommissionTypeEnum resolveCommissionType(BookingOrderDO order) {
        // 加钟订单
        if (Integer.valueOf(1).equals(order.getIsAddon())) {
            Integer addonType = order.getAddonType();
            if (addonType != null && addonType.equals(AddonTypeEnum.EXTEND.getType())) {
                return CommissionTypeEnum.EXTEND;
            }
            return CommissionTypeEnum.BASE;
        }
        // 点钟模式
        if (DispatchModeEnum.DESIGNATED.getMode().equals(order.getDispatchMode())) {
            return CommissionTypeEnum.DESIGNATED;
        }
        // 默认基础佣金
        return CommissionTypeEnum.BASE;
    }

    /**
     * 获取佣金比例：优先门店配置，回退到枚举默认值
     */
    private BigDecimal resolveCommissionRate(Long storeId, CommissionTypeEnum commissionType) {
        if (storeId != null) {
            TechnicianCommissionConfigDO config = commissionConfigMapper
                    .selectByStoreIdAndType(storeId, commissionType.getType());
            if (config != null && config.getRate() != null) {
                return config.getRate();
            }
        }
        return BigDecimal.valueOf(commissionType.getDefaultRate());
    }

    private TechnicianCommissionDO findReversalByBizKey(List<TechnicianCommissionDO> commissions,
                                                        String bizType, String bizNo, Long staffId) {
        for (TechnicianCommissionDO item : commissions) {
            if (item == null) {
                continue;
            }
            if (!Objects.equals(bizType, item.getBizType())
                    || !Objects.equals(bizNo, item.getBizNo())
                    || !Objects.equals(staffId, item.getStaffId())) {
                continue;
            }
            return item;
        }
        return null;
    }

    private String buildReversalBizNo(TechnicianCommissionDO settledCommission) {
        return String.valueOf(settledCommission.getId());
    }

    private void reactivateCancelledReversal(TechnicianCommissionDO existingReversal,
                                             TechnicianCommissionDO settledCommission) {
        commissionMapper.reactivateCancelledReversalById(existingReversal.getId(),
                negateAmount(settledCommission.getBaseAmount()),
                settledCommission.getCommissionRate(),
                negateAmount(settledCommission.getCommissionAmount()),
                REVERSAL_BIZ_TYPE,
                buildReversalBizNo(settledCommission),
                settledCommission.getTechnicianId());
    }

    private boolean isReversalCommission(TechnicianCommissionDO commission) {
        return commission != null
                && commission.getBaseAmount() != null
                && commission.getCommissionAmount() != null
                && commission.getBaseAmount() < 0
                && commission.getCommissionAmount() < 0;
    }

    private Integer negateAmount(Integer amount) {
        if (amount == null) {
            return 0;
        }
        return -Math.abs(amount);
    }

}
