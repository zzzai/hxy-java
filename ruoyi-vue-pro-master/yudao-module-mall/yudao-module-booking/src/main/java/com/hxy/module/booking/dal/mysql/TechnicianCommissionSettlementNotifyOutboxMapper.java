package com.hxy.module.booking.dal.mysql;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementNotifyOutboxPageReqVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementNotifyOutboxDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TechnicianCommissionSettlementNotifyOutboxMapper
        extends BaseMapperX<TechnicianCommissionSettlementNotifyOutboxDO> {

    default PageResult<TechnicianCommissionSettlementNotifyOutboxDO> selectPage(
            TechnicianCommissionSettlementNotifyOutboxPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TechnicianCommissionSettlementNotifyOutboxDO>()
                .eqIfPresent(TechnicianCommissionSettlementNotifyOutboxDO::getSettlementId, reqVO.getSettlementId())
                .eqIfPresent(TechnicianCommissionSettlementNotifyOutboxDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TechnicianCommissionSettlementNotifyOutboxDO::getNotifyType, reqVO.getNotifyType())
                .eqIfPresent(TechnicianCommissionSettlementNotifyOutboxDO::getChannel, reqVO.getChannel())
                .eqIfPresent(TechnicianCommissionSettlementNotifyOutboxDO::getLastActionCode, reqVO.getLastActionCode())
                .eqIfPresent(TechnicianCommissionSettlementNotifyOutboxDO::getLastActionBizNo, reqVO.getLastActionBizNo())
                .betweenIfPresent(TechnicianCommissionSettlementNotifyOutboxDO::getLastActionTime, reqVO.getLastActionTime())
                .orderByDesc(TechnicianCommissionSettlementNotifyOutboxDO::getId));
    }

    default TechnicianCommissionSettlementNotifyOutboxDO selectByBizKey(String bizKey) {
        return selectOne(TechnicianCommissionSettlementNotifyOutboxDO::getBizKey, bizKey);
    }

    default List<TechnicianCommissionSettlementNotifyOutboxDO> selectPendingList(LocalDateTime now, Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        LambdaQueryWrapperX<TechnicianCommissionSettlementNotifyOutboxDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(TechnicianCommissionSettlementNotifyOutboxDO::getStatus, 0)
                .and(wrapper -> wrapper.isNull(TechnicianCommissionSettlementNotifyOutboxDO::getNextRetryTime)
                        .or()
                        .le(TechnicianCommissionSettlementNotifyOutboxDO::getNextRetryTime, now))
                .orderByAsc(TechnicianCommissionSettlementNotifyOutboxDO::getId)
                .last("LIMIT " + safeLimit);
        return selectList(queryWrapper);
    }

    default List<TechnicianCommissionSettlementNotifyOutboxDO> selectDispatchableList(
            LocalDateTime now, Integer limit, Integer maxRetryCount) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        int safeMaxRetry = Math.max(1, Math.min(ObjectUtil.defaultIfNull(maxRetryCount, 5), 20));
        LambdaQueryWrapperX<TechnicianCommissionSettlementNotifyOutboxDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.and(wrapper -> wrapper.eq(TechnicianCommissionSettlementNotifyOutboxDO::getStatus, 0)
                        .or(w -> w.eq(TechnicianCommissionSettlementNotifyOutboxDO::getStatus, 2)
                                .lt(TechnicianCommissionSettlementNotifyOutboxDO::getRetryCount, safeMaxRetry)))
                .and(wrapper -> wrapper.isNull(TechnicianCommissionSettlementNotifyOutboxDO::getNextRetryTime)
                        .or()
                        .le(TechnicianCommissionSettlementNotifyOutboxDO::getNextRetryTime, now))
                .orderByAsc(TechnicianCommissionSettlementNotifyOutboxDO::getId)
                .last("LIMIT " + safeLimit);
        return selectList(queryWrapper);
    }

    default List<TechnicianCommissionSettlementNotifyOutboxDO> selectListBySettlementAndStatus(
            Long settlementId, Integer status, Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionSettlementNotifyOutboxDO>()
                .eqIfPresent(TechnicianCommissionSettlementNotifyOutboxDO::getSettlementId, settlementId)
                .eqIfPresent(TechnicianCommissionSettlementNotifyOutboxDO::getStatus, status)
                .orderByDesc(TechnicianCommissionSettlementNotifyOutboxDO::getId)
                .last("LIMIT " + safeLimit));
    }

    default int updateByIdAndStatus(Long id, Integer status, TechnicianCommissionSettlementNotifyOutboxDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<TechnicianCommissionSettlementNotifyOutboxDO>()
                .eq(TechnicianCommissionSettlementNotifyOutboxDO::getId, id)
                .eq(TechnicianCommissionSettlementNotifyOutboxDO::getStatus, status));
    }
}
