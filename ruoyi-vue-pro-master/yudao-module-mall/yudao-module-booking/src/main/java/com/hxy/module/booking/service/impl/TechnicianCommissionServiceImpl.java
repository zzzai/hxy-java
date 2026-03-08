package com.hxy.module.booking.service.impl;

import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionConfigDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementLogDO;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionConfigMapper;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionMapper;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionSettlementLogMapper;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionSettlementMapper;
import com.hxy.module.booking.enums.AddonTypeEnum;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.enums.CommissionStatusEnum;
import com.hxy.module.booking.enums.CommissionTypeEnum;
import com.hxy.module.booking.enums.DispatchModeEnum;
import com.hxy.module.booking.service.BookingOrderService;
import com.hxy.module.booking.service.TechnicianCommissionService;
import com.hxy.module.booking.service.support.FinanceLogFieldValidator;
import cn.iocoder.yudao.module.trade.api.order.TradeServiceOrderApi;
import cn.iocoder.yudao.module.trade.api.order.dto.TradeServiceOrderTraceRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT;
import static com.hxy.module.booking.enums.ErrorCodeConstants.COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT;

@Service
@Validated
@Slf4j
public class TechnicianCommissionServiceImpl implements TechnicianCommissionService {

    private static final String REVERSAL_BIZ_TYPE = "ORDER_CANCEL_REVERSAL";
    private static final String ACCRUAL_BIZ_TYPE = "FULFILLMENT_COMPLETE";
    private static final String LOG_ACTION_REVERSAL = "REVERSAL";
    private static final String ACCRUAL_SOURCE_PREFIX = "BOOKING_COMMISSION_ACCRUAL:";
    private static final String REVERSAL_SOURCE_PREFIX = "BOOKING_COMMISSION_REVERSAL:";

    private final TechnicianCommissionMapper commissionMapper;
    private final TechnicianCommissionConfigMapper commissionConfigMapper;
    private final TechnicianCommissionSettlementMapper settlementMapper;
    private final TechnicianCommissionSettlementLogMapper settlementLogMapper;
    private final BookingOrderService bookingOrderService;
    private final TradeServiceOrderApi tradeServiceOrderApi;

    public TechnicianCommissionServiceImpl(
            TechnicianCommissionMapper commissionMapper,
            TechnicianCommissionConfigMapper commissionConfigMapper,
            TechnicianCommissionSettlementMapper settlementMapper,
            TechnicianCommissionSettlementLogMapper settlementLogMapper,
            @Lazy BookingOrderService bookingOrderService,
            TradeServiceOrderApi tradeServiceOrderApi) {
        this.commissionMapper = commissionMapper;
        this.commissionConfigMapper = commissionConfigMapper;
        this.settlementMapper = settlementMapper;
        this.settlementLogMapper = settlementLogMapper;
        this.bookingOrderService = bookingOrderService;
        this.tradeServiceOrderApi = tradeServiceOrderApi;
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
        CommissionTraceInfo traceInfo = resolveTraceInfo(order);
        String sourceBizNo = buildAccrualSourceBizNo(orderId, traceInfo.getServiceOrderId());

        TechnicianCommissionDO commission = TechnicianCommissionDO.builder()
                .technicianId(order.getTechnicianId())
                .orderId(orderId)
                .orderItemId(traceInfo.getOrderItemId())
                .serviceOrderId(traceInfo.getServiceOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .commissionType(commissionType.getType())
                .baseAmount(baseAmount)
                .commissionRate(rate)
                .commissionAmount(commissionAmount)
                .bizType(ACCRUAL_BIZ_TYPE)
                .bizNo(sourceBizNo)
                .sourceBizNo(sourceBizNo)
                .staffId(order.getTechnicianId())
                .status(CommissionStatusEnum.PENDING.getStatus())
                .build();
        try {
            commissionMapper.insert(commission);
            log.info("创建佣金记录，commissionId={}, orderId={}, technicianId={}, amount={}, sourceBizNo={}",
                    commission.getId(), orderId, order.getTechnicianId(), commissionAmount, sourceBizNo);
            FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                    orderId, -1L, sourceBizNo, "COMMISSION_ACCRUAL_OK", "commission_accrual");
            log.info("[finance-audit][scene=commission_accrual_success][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                    fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                    fields.getSourceBizNo(), fields.getErrorCode());
        } catch (DuplicateKeyException ex) {
            TechnicianCommissionDO existingByBizKey = commissionMapper.selectByBizKey(ACCRUAL_BIZ_TYPE, sourceBizNo, order.getTechnicianId());
            if (existingByBizKey == null) {
                throw ex;
            }
            ensureAccrualPayloadConsistent(existingByBizKey, orderId, traceInfo.getOrderItemId(), traceInfo.getServiceOrderId(),
                    baseAmount, rate, commissionAmount, sourceBizNo, order.getTechnicianId());
            log.info("命中佣金计提幂等键，跳过重复创建，orderId={}, commissionId={}, sourceBizNo={}",
                    orderId, existingByBizKey.getId(), sourceBizNo);
            FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                    orderId, -1L, sourceBizNo, "COMMISSION_ACCRUAL_IDEMPOTENT_HIT", "commission_accrual");
            log.info("[finance-audit][scene=commission_accrual_idempotent_hit][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                    fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                    fields.getSourceBizNo(), fields.getErrorCode());
        }
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
                Integer expectedBaseAmount = negateAmount(commission.getBaseAmount());
                BigDecimal expectedCommissionRate = commission.getCommissionRate();
                Integer expectedCommissionAmount = negateAmount(commission.getCommissionAmount());
                TechnicianCommissionDO existingReversal = findReversalByOriginCommissionId(
                        commissions, commission.getId());
                if (existingReversal != null) {
                    if (CommissionStatusEnum.CANCELLED.getStatus().equals(existingReversal.getStatus())) {
                        commissionMapper.releaseCancelledReversalIdempotentKeyById(existingReversal.getId());
                        log.info("释放已取消冲正记录幂等键，reversalId={}, originCommissionId={}, orderId={}",
                                existingReversal.getId(), commission.getId(), orderId);
                    } else {
                        ensureReversalPayloadConsistent(existingReversal, expectedBaseAmount,
                                expectedCommissionRate, expectedCommissionAmount, commission.getId(), reversalBizNo);
                        log.info("命中佣金冲正幂等键，跳过重复请求，originCommissionId={}, reversalId={}, orderId={}, bizNo={}",
                                commission.getId(), existingReversal.getId(), orderId, reversalBizNo);
                        continue;
                    }
                }
                TechnicianCommissionDO reversal = TechnicianCommissionDO.builder()
                        .technicianId(commission.getTechnicianId())
                        .orderId(commission.getOrderId())
                        .orderItemId(commission.getOrderItemId())
                        .serviceOrderId(commission.getServiceOrderId())
                        .userId(commission.getUserId())
                        .storeId(commission.getStoreId())
                        .commissionType(commission.getCommissionType())
                        .baseAmount(expectedBaseAmount)
                        .commissionRate(expectedCommissionRate)
                        .commissionAmount(expectedCommissionAmount)
                        .bizType(REVERSAL_BIZ_TYPE)
                        .bizNo(reversalBizNo)
                        .sourceBizNo(buildReversalSourceBizNo(commission.getOrderId(), commission.getId()))
                        .staffId(commission.getTechnicianId())
                        .originCommissionId(commission.getId())
                        .status(CommissionStatusEnum.PENDING.getStatus())
                        .build();
                try {
                    commissionMapper.insert(reversal);
                    log.info("创建佣金冲正记录，originCommissionId={}, reversalCommissionId={}, orderId={}, bizNo={}",
                            commission.getId(), reversal.getId(), orderId, reversalBizNo);
                    appendSettlementReversalAudit(commission, reversalBizNo, expectedCommissionAmount);
                    FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                            orderId, -1L, reversal.getSourceBizNo(), "COMMISSION_REVERSAL_OK", "commission_reversal");
                    log.info("[finance-audit][scene=commission_reversal_success][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                            fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                            fields.getSourceBizNo(), fields.getErrorCode());
                } catch (DuplicateKeyException ex) {
                    TechnicianCommissionDO reversalByOrigin = commissionMapper.selectByOriginCommissionId(commission.getId());
                    if (reversalByOrigin == null) {
                        reversalByOrigin = commissionMapper.selectByBizKey(REVERSAL_BIZ_TYPE, reversalBizNo, commission.getTechnicianId());
                    }
                    if (reversalByOrigin == null) {
                        throw ex;
                    }
                    if (CommissionStatusEnum.CANCELLED.getStatus().equals(reversalByOrigin.getStatus())) {
                        commissionMapper.releaseCancelledReversalIdempotentKeyById(reversalByOrigin.getId());
                        log.info("命中冲正唯一键但记录已取消，释放幂等键后重试生成，reversalId={}, originCommissionId={}, orderId={}, bizNo={}",
                                reversalByOrigin.getId(), commission.getId(), orderId, reversalBizNo);
                        try {
                            commissionMapper.insert(reversal);
                            log.info("重试后创建佣金冲正记录，originCommissionId={}, reversalCommissionId={}, orderId={}, bizNo={}",
                                    commission.getId(), reversal.getId(), orderId, reversalBizNo);
                            appendSettlementReversalAudit(commission, reversalBizNo, expectedCommissionAmount);
                            FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                                    orderId, -1L, reversal.getSourceBizNo(), "COMMISSION_REVERSAL_RETRY_OK", "commission_reversal");
                            log.info("[finance-audit][scene=commission_reversal_retry_success][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                                    fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                                    fields.getSourceBizNo(), fields.getErrorCode());
                        } catch (DuplicateKeyException retryEx) {
                            TechnicianCommissionDO existingAfterRetry = commissionMapper.selectByOriginCommissionId(commission.getId());
                            if (existingAfterRetry == null
                                    || CommissionStatusEnum.CANCELLED.getStatus().equals(existingAfterRetry.getStatus())) {
                                throw retryEx;
                            }
                            ensureReversalPayloadConsistent(existingAfterRetry, expectedBaseAmount,
                                    expectedCommissionRate, expectedCommissionAmount, commission.getId(), reversalBizNo);
                            log.info("重试后命中佣金冲正幂等键，跳过重复插入，originCommissionId={}, reversalId={}, orderId={}, bizNo={}",
                                    commission.getId(), existingAfterRetry.getId(), orderId, reversalBizNo);
                        }
                    } else {
                        ensureReversalPayloadConsistent(reversalByOrigin, expectedBaseAmount,
                                expectedCommissionRate, expectedCommissionAmount, commission.getId(), reversalBizNo);
                        log.info("命中佣金冲正幂等唯一键，跳过重复插入，originCommissionId={}, reversalId={}, orderId={}, bizNo={}",
                                commission.getId(), reversalByOrigin.getId(), orderId, reversalBizNo);
                    }
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

    private TechnicianCommissionDO findReversalByOriginCommissionId(List<TechnicianCommissionDO> commissions,
                                                                    Long originCommissionId) {
        for (TechnicianCommissionDO item : commissions) {
            if (item == null) {
                continue;
            }
            if (!Objects.equals(originCommissionId, item.getOriginCommissionId())) {
                continue;
            }
            return item;
        }
        return null;
    }

    private String buildReversalBizNo(TechnicianCommissionDO settledCommission) {
        return String.valueOf(settledCommission.getId());
    }

    private boolean isReversalCommission(TechnicianCommissionDO commission) {
        return commission != null
                && commission.getBaseAmount() != null
                && commission.getCommissionAmount() != null
                && commission.getBaseAmount() < 0
                && commission.getCommissionAmount() < 0;
    }

    private void ensureReversalPayloadConsistent(TechnicianCommissionDO reversal,
                                                 Integer expectedBaseAmount,
                                                 BigDecimal expectedCommissionRate,
                                                 Integer expectedCommissionAmount,
                                                 Long originCommissionId,
                                                 String reversalBizNo) {
        if (Objects.equals(reversal.getBaseAmount(), expectedBaseAmount)
                && isRateEqual(reversal.getCommissionRate(), expectedCommissionRate)
                && Objects.equals(reversal.getCommissionAmount(), expectedCommissionAmount)) {
            return;
        }
        log.warn("佣金冲正幂等键冲突，originCommissionId={}, reversalId={}, bizType={}, bizNo={}, staffId={}, expectedBase={}, actualBase={}, expectedRate={}, actualRate={}, expectedAmount={}, actualAmount={}",
                originCommissionId, reversal.getId(), reversal.getBizType(), reversalBizNo, reversal.getStaffId(),
                expectedBaseAmount, reversal.getBaseAmount(), expectedCommissionRate, reversal.getCommissionRate(),
                expectedCommissionAmount, reversal.getCommissionAmount());
        FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                reversal.getOrderId(), -1L, reversalBizNo,
                String.valueOf(COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT.getCode()), "commission_reversal");
        log.warn("[finance-audit][scene=commission_reversal_conflict][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                fields.getSourceBizNo(), fields.getErrorCode());
        throw exception(COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT);
    }

    private void ensureAccrualPayloadConsistent(TechnicianCommissionDO existing,
                                                Long expectedOrderId,
                                                Long expectedOrderItemId,
                                                Long expectedServiceOrderId,
                                                Integer expectedBaseAmount,
                                                BigDecimal expectedCommissionRate,
                                                Integer expectedCommissionAmount,
                                                String sourceBizNo,
                                                Long staffId) {
        if (Objects.equals(existing.getOrderId(), expectedOrderId)
                && Objects.equals(existing.getOrderItemId(), expectedOrderItemId)
                && Objects.equals(existing.getServiceOrderId(), expectedServiceOrderId)
                && Objects.equals(existing.getBaseAmount(), expectedBaseAmount)
                && isRateEqual(existing.getCommissionRate(), expectedCommissionRate)
                && Objects.equals(existing.getCommissionAmount(), expectedCommissionAmount)) {
            return;
        }
        log.warn("佣金计提幂等键冲突，commissionId={}, sourceBizNo={}, staffId={}, expectedOrderId={}, actualOrderId={}, expectedOrderItemId={}, actualOrderItemId={}, expectedServiceOrderId={}, actualServiceOrderId={}, expectedBaseAmount={}, actualBaseAmount={}, expectedRate={}, actualRate={}, expectedAmount={}, actualAmount={}",
                existing.getId(), sourceBizNo, staffId, expectedOrderId, existing.getOrderId(),
                expectedOrderItemId, existing.getOrderItemId(), expectedServiceOrderId, existing.getServiceOrderId(),
                expectedBaseAmount, existing.getBaseAmount(), expectedCommissionRate, existing.getCommissionRate(),
                expectedCommissionAmount, existing.getCommissionAmount());
        FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                expectedOrderId, -1L, sourceBizNo,
                String.valueOf(COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT.getCode()), "commission_accrual");
        log.warn("[finance-audit][scene=commission_accrual_conflict][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                fields.getSourceBizNo(), fields.getErrorCode());
        throw exception(COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT);
    }

    private boolean isRateEqual(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private Integer negateAmount(Integer amount) {
        if (amount == null) {
            return 0;
        }
        return -Math.abs(amount);
    }

    private CommissionTraceInfo resolveTraceInfo(BookingOrderDO order) {
        if (order == null || order.getPayOrderId() == null || order.getPayOrderId() <= 0) {
            return CommissionTraceInfo.empty();
        }
        List<TradeServiceOrderTraceRespDTO> traces = tradeServiceOrderApi.listTraceByPayOrderId(order.getPayOrderId());
        if (traces == null || traces.isEmpty()) {
            return CommissionTraceInfo.empty();
        }
        Optional<TradeServiceOrderTraceRespDTO> exactMatch = traces.stream()
                .filter(trace -> trace != null)
                .filter(trace -> Objects.equals(trace.getSkuId(), order.getSkuId()))
                .findFirst();
        TradeServiceOrderTraceRespDTO selected = exactMatch.orElseGet(() -> traces.stream()
                .filter(trace -> trace != null)
                .filter(trace -> Objects.equals(trace.getSpuId(), order.getSpuId()))
                .min(Comparator.comparing(TradeServiceOrderTraceRespDTO::getServiceOrderId, Comparator.nullsLast(Long::compareTo)))
                .orElseGet(() -> traces.stream().filter(Objects::nonNull).findFirst().orElse(null)));
        if (selected == null) {
            return CommissionTraceInfo.empty();
        }
        return new CommissionTraceInfo(selected.getOrderItemId(), selected.getServiceOrderId());
    }

    private String buildAccrualSourceBizNo(Long orderId, Long serviceOrderId) {
        long safeOrderId = orderId == null ? 0L : orderId;
        long safeServiceOrderId = serviceOrderId == null ? 0L : serviceOrderId;
        return ACCRUAL_SOURCE_PREFIX + safeOrderId + ":" + safeServiceOrderId;
    }

    private String buildReversalSourceBizNo(Long orderId, Long originCommissionId) {
        long safeOrderId = orderId == null ? 0L : orderId;
        long safeOriginCommissionId = originCommissionId == null ? 0L : originCommissionId;
        return REVERSAL_SOURCE_PREFIX + safeOrderId + ":" + safeOriginCommissionId;
    }

    private void appendSettlementReversalAudit(TechnicianCommissionDO originCommission,
                                               String reversalBizNo,
                                               Integer reversalAmount) {
        if (originCommission == null || originCommission.getSettlementId() == null) {
            return;
        }
        TechnicianCommissionSettlementDO settlement = settlementMapper.selectById(originCommission.getSettlementId());
        if (settlement == null) {
            return;
        }
        settlementLogMapper.insert(new TechnicianCommissionSettlementLogDO()
                .setSettlementId(settlement.getId())
                .setAction(LOG_ACTION_REVERSAL)
                .setFromStatus(settlement.getStatus())
                .setToStatus(settlement.getStatus())
                .setOperatorType("SYSTEM")
                .setOperateRemark("BIZ#" + reversalBizNo + ",amount=" + reversalAmount)
                .setActionTime(LocalDateTime.now()));
    }

    private static final class CommissionTraceInfo {
        private final Long orderItemId;
        private final Long serviceOrderId;

        private CommissionTraceInfo(Long orderItemId, Long serviceOrderId) {
            this.orderItemId = orderItemId;
            this.serviceOrderId = serviceOrderId;
        }

        private static CommissionTraceInfo empty() {
            return new CommissionTraceInfo(null, null);
        }

        private Long getOrderItemId() {
            return orderItemId;
        }

        private Long getServiceOrderId() {
            return serviceOrderId;
        }
    }

    private FinanceLogFieldValidator.FinanceLogFields validateFinanceLogFields(Long orderId, Long payRefundId,
                                                                                String sourceBizNo, String errorCode,
                                                                                String scene) {
        FinanceLogFieldValidator.FinanceLogFields fields = FinanceLogFieldValidator.validate(
                "NO_RUN", orderId, payRefundId, sourceBizNo, errorCode);
        if (!fields.isComplete()) {
            log.warn("[finance-log-validate][scene={}][missingFields={}]", scene, fields.getMissingFields());
        }
        return fields;
    }

}
