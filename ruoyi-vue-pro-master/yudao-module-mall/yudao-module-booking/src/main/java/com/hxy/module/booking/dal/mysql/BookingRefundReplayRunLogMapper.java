package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogPageReqVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface BookingRefundReplayRunLogMapper extends BaseMapperX<BookingRefundReplayRunLogDO> {

    default PageResult<BookingRefundReplayRunLogDO> selectPage(BookingRefundReplayRunLogPageReqVO reqVO) {
        LambdaQueryWrapperX<BookingRefundReplayRunLogDO> wrapper = new LambdaQueryWrapperX<BookingRefundReplayRunLogDO>()
                .likeIfPresent(BookingRefundReplayRunLogDO::getRunId, reqVO.getRunId())
                .eqIfPresent(BookingRefundReplayRunLogDO::getTriggerSource, reqVO.getTriggerSource())
                .likeIfPresent(BookingRefundReplayRunLogDO::getOperator, reqVO.getOperator())
                .eqIfPresent(BookingRefundReplayRunLogDO::getStatus, reqVO.getStatus())
                .eqIfPresent(BookingRefundReplayRunLogDO::getDryRun, reqVO.getDryRun())
                .geIfPresent(BookingRefundReplayRunLogDO::getFailCount, reqVO.getMinFailCount())
                .betweenIfPresent(BookingRefundReplayRunLogDO::getStartTime, reqVO.getStartTime())
                .orderByDesc(BookingRefundReplayRunLogDO::getId);
        if (reqVO.getHasWarning() != null) {
            if (Boolean.TRUE.equals(reqVO.getHasWarning())) {
                wrapper.like(BookingRefundReplayRunLogDO::getErrorMsg, "WARN#");
            } else {
                wrapper.and(w -> w.notLike(BookingRefundReplayRunLogDO::getErrorMsg, "WARN#")
                        .or().isNull(BookingRefundReplayRunLogDO::getErrorMsg)
                        .or().eq(BookingRefundReplayRunLogDO::getErrorMsg, ""));
            }
        }
        return selectPage(reqVO, wrapper);
    }

    default BookingRefundReplayRunLogDO selectByRunId(String runId) {
        return selectOne(new LambdaQueryWrapper<BookingRefundReplayRunLogDO>()
                .eq(BookingRefundReplayRunLogDO::getRunId, runId)
                .last("LIMIT 1"));
    }

    default int updateRunResult(Long id, Integer scannedCount, Integer successCount, Integer skipCount,
                                Integer failCount, String status, String errorMsg, LocalDateTime endTime) {
        return update(null, new LambdaUpdateWrapper<BookingRefundReplayRunLogDO>()
                .eq(BookingRefundReplayRunLogDO::getId, id)
                .set(BookingRefundReplayRunLogDO::getScannedCount, scannedCount)
                .set(BookingRefundReplayRunLogDO::getSuccessCount, successCount)
                .set(BookingRefundReplayRunLogDO::getSkipCount, skipCount)
                .set(BookingRefundReplayRunLogDO::getFailCount, failCount)
                .set(BookingRefundReplayRunLogDO::getStatus, status)
                .set(BookingRefundReplayRunLogDO::getErrorMsg, errorMsg)
                .set(BookingRefundReplayRunLogDO::getEndTime, endTime));
    }
}
