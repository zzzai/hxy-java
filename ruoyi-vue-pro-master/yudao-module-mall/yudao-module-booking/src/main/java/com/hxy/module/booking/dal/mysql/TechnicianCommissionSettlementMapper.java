package com.hxy.module.booking.dal.mysql;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementPageReqVO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementDO;
import com.hxy.module.booking.enums.CommissionSettlementStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TechnicianCommissionSettlementMapper extends BaseMapperX<TechnicianCommissionSettlementDO> {

    default PageResult<TechnicianCommissionSettlementDO> selectPage(TechnicianCommissionSettlementPageReqVO reqVO) {
        LambdaQueryWrapperX<TechnicianCommissionSettlementDO> queryWrapper = new LambdaQueryWrapperX<TechnicianCommissionSettlementDO>()
                .likeIfPresent(TechnicianCommissionSettlementDO::getSettlementNo, reqVO.getSettlementNo())
                .eqIfPresent(TechnicianCommissionSettlementDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(TechnicianCommissionSettlementDO::getTechnicianId, reqVO.getTechnicianId())
                .eqIfPresent(TechnicianCommissionSettlementDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TechnicianCommissionSettlementDO::getReviewerId, reqVO.getReviewerId())
                .eqIfPresent(TechnicianCommissionSettlementDO::getReviewWarned, reqVO.getReviewWarned())
                .eqIfPresent(TechnicianCommissionSettlementDO::getReviewEscalated, reqVO.getReviewEscalated())
                .betweenIfPresent(TechnicianCommissionSettlementDO::getReviewDeadlineTime, reqVO.getReviewDeadlineTime())
                .orderByDesc(TechnicianCommissionSettlementDO::getId);
        if (reqVO.getOverdue() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (Boolean.TRUE.equals(reqVO.getOverdue())) {
                queryWrapper.and(wrapper -> wrapper
                        .eq(TechnicianCommissionSettlementDO::getStatus,
                                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus())
                        .isNotNull(TechnicianCommissionSettlementDO::getReviewDeadlineTime)
                        .le(TechnicianCommissionSettlementDO::getReviewDeadlineTime, now));
            } else {
                queryWrapper.and(wrapper -> wrapper
                        .ne(TechnicianCommissionSettlementDO::getStatus,
                                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus())
                        .or()
                        .isNull(TechnicianCommissionSettlementDO::getReviewDeadlineTime)
                        .or()
                        .gt(TechnicianCommissionSettlementDO::getReviewDeadlineTime, now));
            }
        }
        return selectPage(reqVO, queryWrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, TechnicianCommissionSettlementDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<TechnicianCommissionSettlementDO>()
                .eq(TechnicianCommissionSettlementDO::getId, id)
                .eq(TechnicianCommissionSettlementDO::getStatus, status));
    }

    default List<TechnicianCommissionSettlementDO> selectListByStatusAndReviewDeadlineBefore(Integer status,
                                                                                              LocalDateTime deadline,
                                                                                              Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionSettlementDO>()
                .eq(TechnicianCommissionSettlementDO::getStatus, status)
                .isNotNull(TechnicianCommissionSettlementDO::getReviewDeadlineTime)
                .le(TechnicianCommissionSettlementDO::getReviewDeadlineTime, deadline)
                .orderByAsc(TechnicianCommissionSettlementDO::getReviewDeadlineTime)
                .last("LIMIT " + safeLimit));
    }

    default List<TechnicianCommissionSettlementDO> selectListByTechnicianAndStatus(Long technicianId, Integer status) {
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionSettlementDO>()
                .eqIfPresent(TechnicianCommissionSettlementDO::getTechnicianId, technicianId)
                .eqIfPresent(TechnicianCommissionSettlementDO::getStatus, status)
                .orderByDesc(TechnicianCommissionSettlementDO::getId));
    }

    default List<TechnicianCommissionSettlementDO> selectListByStatusAndReviewDeadlineBetweenAndWarned(Integer status,
                                                                                                         LocalDateTime from,
                                                                                                         LocalDateTime to,
                                                                                                         Boolean reviewWarned,
                                                                                                         Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        LambdaQueryWrapperX<TechnicianCommissionSettlementDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(TechnicianCommissionSettlementDO::getStatus, status)
                .isNotNull(TechnicianCommissionSettlementDO::getReviewDeadlineTime)
                .ge(TechnicianCommissionSettlementDO::getReviewDeadlineTime, from)
                .le(TechnicianCommissionSettlementDO::getReviewDeadlineTime, to)
                .orderByAsc(TechnicianCommissionSettlementDO::getReviewDeadlineTime);
        if (reviewWarned != null) {
            queryWrapper.eq(TechnicianCommissionSettlementDO::getReviewWarned, reviewWarned);
        }
        queryWrapper.last("LIMIT " + safeLimit);
        return selectList(queryWrapper);
    }

    default int updateWarnedByIdAndStatusAndWarned(Long id, Integer status, Boolean reviewWarned,
                                                   TechnicianCommissionSettlementDO updateObj) {
        LambdaUpdateWrapper<TechnicianCommissionSettlementDO> wrapper = new LambdaUpdateWrapper<TechnicianCommissionSettlementDO>()
                .eq(TechnicianCommissionSettlementDO::getId, id)
                .eq(TechnicianCommissionSettlementDO::getStatus, status);
        if (reviewWarned != null) {
            wrapper.eq(TechnicianCommissionSettlementDO::getReviewWarned, reviewWarned);
        }
        return update(updateObj, wrapper);
    }

    default List<TechnicianCommissionSettlementDO> selectListByStatusAndReviewDeadlineBeforeAndEscalated(
            Integer status, LocalDateTime deadline, Boolean reviewEscalated, Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        LambdaQueryWrapperX<TechnicianCommissionSettlementDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(TechnicianCommissionSettlementDO::getStatus, status)
                .isNotNull(TechnicianCommissionSettlementDO::getReviewDeadlineTime)
                .le(TechnicianCommissionSettlementDO::getReviewDeadlineTime, deadline)
                .orderByAsc(TechnicianCommissionSettlementDO::getReviewDeadlineTime);
        if (reviewEscalated != null) {
            queryWrapper.eq(TechnicianCommissionSettlementDO::getReviewEscalated, reviewEscalated);
        }
        queryWrapper.last("LIMIT " + safeLimit);
        return selectList(queryWrapper);
    }

    default int updateEscalatedByIdAndStatusAndEscalated(Long id, Integer status, Boolean reviewEscalated,
                                                          TechnicianCommissionSettlementDO updateObj) {
        LambdaUpdateWrapper<TechnicianCommissionSettlementDO> wrapper = new LambdaUpdateWrapper<TechnicianCommissionSettlementDO>()
                .eq(TechnicianCommissionSettlementDO::getId, id)
                .eq(TechnicianCommissionSettlementDO::getStatus, status);
        if (reviewEscalated != null) {
            wrapper.eq(TechnicianCommissionSettlementDO::getReviewEscalated, reviewEscalated);
        }
        return update(updateObj, wrapper);
    }

}
