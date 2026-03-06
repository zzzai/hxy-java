package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface BookingRefundNotifyLogMapper extends BaseMapperX<BookingRefundNotifyLogDO> {

    default PageResult<BookingRefundNotifyLogDO> selectPage(BookingRefundNotifyLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BookingRefundNotifyLogDO>()
                .eqIfPresent(BookingRefundNotifyLogDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(BookingRefundNotifyLogDO::getPayRefundId, reqVO.getPayRefundId())
                .likeIfPresent(BookingRefundNotifyLogDO::getMerchantRefundId, reqVO.getMerchantRefundId())
                .eqIfPresent(BookingRefundNotifyLogDO::getStatus, reqVO.getStatus())
                .eqIfPresent(BookingRefundNotifyLogDO::getErrorCode, reqVO.getErrorCode())
                .betweenIfPresent(BookingRefundNotifyLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(BookingRefundNotifyLogDO::getId));
    }

    default List<Long> selectDueFailIds(LocalDateTime now, Integer limit, String failStatus) {
        int safeLimit = Math.max(1, limit == null ? 200 : limit);
        return selectList(new LambdaQueryWrapper<BookingRefundNotifyLogDO>()
                .eq(BookingRefundNotifyLogDO::getStatus, failStatus)
                .isNotNull(BookingRefundNotifyLogDO::getNextRetryTime)
                .le(BookingRefundNotifyLogDO::getNextRetryTime, now)
                .orderByAsc(BookingRefundNotifyLogDO::getNextRetryTime)
                .orderByAsc(BookingRefundNotifyLogDO::getId)
                .last("LIMIT " + safeLimit))
                .stream()
                .map(BookingRefundNotifyLogDO::getId)
                .collect(Collectors.toList());
    }

    default int updateReplayAudit(Long id, String lastReplayOperator, LocalDateTime lastReplayTime,
                                  String lastReplayResult, String lastReplayRemark) {
        return update(null, new LambdaUpdateWrapper<BookingRefundNotifyLogDO>()
                .eq(BookingRefundNotifyLogDO::getId, id)
                .set(BookingRefundNotifyLogDO::getLastReplayOperator, lastReplayOperator)
                .set(BookingRefundNotifyLogDO::getLastReplayTime, lastReplayTime)
                .set(BookingRefundNotifyLogDO::getLastReplayResult, lastReplayResult)
                .set(BookingRefundNotifyLogDO::getLastReplayRemark, lastReplayRemark));
    }

    default int updateReplaySuccess(Long id, String successStatus, Integer retryCount,
                                    String lastReplayOperator, LocalDateTime lastReplayTime,
                                    String lastReplayResult, String lastReplayRemark) {
        return update(null, new LambdaUpdateWrapper<BookingRefundNotifyLogDO>()
                .eq(BookingRefundNotifyLogDO::getId, id)
                .set(BookingRefundNotifyLogDO::getStatus, successStatus)
                .set(BookingRefundNotifyLogDO::getRetryCount, retryCount)
                .set(BookingRefundNotifyLogDO::getNextRetryTime, null)
                .set(BookingRefundNotifyLogDO::getErrorCode, "")
                .set(BookingRefundNotifyLogDO::getErrorMsg, "")
                .set(BookingRefundNotifyLogDO::getLastReplayOperator, lastReplayOperator)
                .set(BookingRefundNotifyLogDO::getLastReplayTime, lastReplayTime)
                .set(BookingRefundNotifyLogDO::getLastReplayResult, lastReplayResult)
                .set(BookingRefundNotifyLogDO::getLastReplayRemark, lastReplayRemark));
    }

    default int updateReplayFailure(Long id, String failStatus, Integer retryCount, LocalDateTime nextRetryTime,
                                    String errorCode, String errorMsg,
                                    String lastReplayOperator, LocalDateTime lastReplayTime,
                                    String lastReplayResult, String lastReplayRemark) {
        return update(null, new LambdaUpdateWrapper<BookingRefundNotifyLogDO>()
                .eq(BookingRefundNotifyLogDO::getId, id)
                .set(BookingRefundNotifyLogDO::getStatus, failStatus)
                .set(BookingRefundNotifyLogDO::getRetryCount, retryCount)
                .set(BookingRefundNotifyLogDO::getNextRetryTime, nextRetryTime)
                .set(BookingRefundNotifyLogDO::getErrorCode, errorCode)
                .set(BookingRefundNotifyLogDO::getErrorMsg, errorMsg)
                .set(BookingRefundNotifyLogDO::getLastReplayOperator, lastReplayOperator)
                .set(BookingRefundNotifyLogDO::getLastReplayTime, lastReplayTime)
                .set(BookingRefundNotifyLogDO::getLastReplayResult, lastReplayResult)
                .set(BookingRefundNotifyLogDO::getLastReplayRemark, lastReplayRemark));
    }
}
