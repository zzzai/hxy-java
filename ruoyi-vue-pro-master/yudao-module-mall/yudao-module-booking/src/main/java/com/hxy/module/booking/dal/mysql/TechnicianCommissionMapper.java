package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionDO;
import com.hxy.module.booking.enums.CommissionStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface TechnicianCommissionMapper extends BaseMapperX<TechnicianCommissionDO> {

    default List<TechnicianCommissionDO> selectListByTechnicianId(Long technicianId) {
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionDO>()
                .eq(TechnicianCommissionDO::getTechnicianId, technicianId)
                .orderByDesc(TechnicianCommissionDO::getCreateTime));
    }

    default List<TechnicianCommissionDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionDO>()
                .eq(TechnicianCommissionDO::getOrderId, orderId));
    }

    default List<TechnicianCommissionDO> selectListByTechnicianIdAndStatus(Long technicianId, Integer status) {
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionDO>()
                .eq(TechnicianCommissionDO::getTechnicianId, technicianId)
                .eq(TechnicianCommissionDO::getStatus, status)
                .orderByDesc(TechnicianCommissionDO::getCreateTime));
    }

    default List<TechnicianCommissionDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionDO>().in(TechnicianCommissionDO::getId, ids));
    }

    default int bindSettlementByIds(Collection<Long> ids, Long settlementId, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return update(new TechnicianCommissionDO().setSettlementId(settlementId),
                new LambdaUpdateWrapper<TechnicianCommissionDO>()
                        .in(TechnicianCommissionDO::getId, ids)
                        .eq(TechnicianCommissionDO::getStatus, status)
                        .isNull(TechnicianCommissionDO::getSettlementId));
    }

    default int clearSettlementBindingBySettlementId(Long settlementId, Integer status) {
        if (settlementId == null) {
            return 0;
        }
        return update(new TechnicianCommissionDO().setSettlementId(null),
                new LambdaUpdateWrapper<TechnicianCommissionDO>()
                        .eq(TechnicianCommissionDO::getSettlementId, settlementId)
                        .eq(TechnicianCommissionDO::getStatus, status));
    }

    default int settleBySettlementId(Long settlementId, Integer fromStatus, LocalDateTime settledAt) {
        if (settlementId == null) {
            return 0;
        }
        return update(new TechnicianCommissionDO()
                        .setStatus(CommissionStatusEnum.SETTLED.getStatus())
                        .setSettlementTime(settledAt),
                new LambdaUpdateWrapper<TechnicianCommissionDO>()
                        .eq(TechnicianCommissionDO::getSettlementId, settlementId)
                        .eq(TechnicianCommissionDO::getStatus, fromStatus));
    }

    default int reactivateCancelledReversalById(Long id, Integer baseAmount, BigDecimal commissionRate,
                                                Integer commissionAmount, String bizType, String bizNo, Long staffId) {
        if (id == null) {
            return 0;
        }
        return update(null, new LambdaUpdateWrapper<TechnicianCommissionDO>()
                .eq(TechnicianCommissionDO::getId, id)
                .eq(TechnicianCommissionDO::getStatus, CommissionStatusEnum.CANCELLED.getStatus())
                .set(TechnicianCommissionDO::getStatus, CommissionStatusEnum.PENDING.getStatus())
                .set(TechnicianCommissionDO::getBaseAmount, baseAmount)
                .set(TechnicianCommissionDO::getCommissionRate, commissionRate)
                .set(TechnicianCommissionDO::getCommissionAmount, commissionAmount)
                .set(TechnicianCommissionDO::getBizType, bizType)
                .set(TechnicianCommissionDO::getBizNo, bizNo)
                .set(TechnicianCommissionDO::getStaffId, staffId)
                .set(TechnicianCommissionDO::getSettlementId, null)
                .set(TechnicianCommissionDO::getSettlementTime, null));
    }

    default int releaseCancelledReversalIdempotentKeyById(Long id) {
        if (id == null) {
            return 0;
        }
        return update(null, new LambdaUpdateWrapper<TechnicianCommissionDO>()
                .eq(TechnicianCommissionDO::getId, id)
                .eq(TechnicianCommissionDO::getStatus, CommissionStatusEnum.CANCELLED.getStatus())
                .set(TechnicianCommissionDO::getOriginCommissionId, null)
                .set(TechnicianCommissionDO::getBizType, "")
                .set(TechnicianCommissionDO::getBizNo, "")
                .set(TechnicianCommissionDO::getStaffId, null));
    }

    default TechnicianCommissionDO selectByBizKey(String bizType, String bizNo, Long staffId) {
        return selectOne(new LambdaQueryWrapperX<TechnicianCommissionDO>()
                .eq(TechnicianCommissionDO::getBizType, bizType)
                .eq(TechnicianCommissionDO::getBizNo, bizNo)
                .eq(TechnicianCommissionDO::getStaffId, staffId)
                .last("LIMIT 1"));
    }

    default TechnicianCommissionDO selectByOriginCommissionId(Long originCommissionId) {
        return selectOne(new LambdaQueryWrapperX<TechnicianCommissionDO>()
                .eq(TechnicianCommissionDO::getOriginCommissionId, originCommissionId)
                .last("LIMIT 1"));
    }

}
