package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogPageReqVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface BookingRefundReplayRunLogMapper extends BaseMapperX<BookingRefundReplayRunLogDO> {

    default PageResult<BookingRefundReplayRunLogDO> selectPage(BookingRefundReplayRunLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BookingRefundReplayRunLogDO>()
                .likeIfPresent(BookingRefundReplayRunLogDO::getRunId, reqVO.getRunId())
                .eqIfPresent(BookingRefundReplayRunLogDO::getTriggerSource, reqVO.getTriggerSource())
                .likeIfPresent(BookingRefundReplayRunLogDO::getOperator, reqVO.getOperator())
                .eqIfPresent(BookingRefundReplayRunLogDO::getStatus, reqVO.getStatus())
                .eqIfPresent(BookingRefundReplayRunLogDO::getDryRun, reqVO.getDryRun())
                .betweenIfPresent(BookingRefundReplayRunLogDO::getStartTime, reqVO.getStartTime())
                .orderByDesc(BookingRefundReplayRunLogDO::getId));
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
