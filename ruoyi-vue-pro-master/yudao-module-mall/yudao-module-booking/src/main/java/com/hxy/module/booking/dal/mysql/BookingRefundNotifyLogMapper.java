package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

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

    default int updateReplaySuccess(Long id, String successStatus, Integer retryCount) {
        return update(null, new LambdaUpdateWrapper<BookingRefundNotifyLogDO>()
                .eq(BookingRefundNotifyLogDO::getId, id)
                .set(BookingRefundNotifyLogDO::getStatus, successStatus)
                .set(BookingRefundNotifyLogDO::getRetryCount, retryCount)
                .set(BookingRefundNotifyLogDO::getNextRetryTime, null)
                .set(BookingRefundNotifyLogDO::getErrorCode, "")
                .set(BookingRefundNotifyLogDO::getErrorMsg, ""));
    }

    default int updateReplayFailure(Long id, String failStatus, Integer retryCount, LocalDateTime nextRetryTime,
                                    String errorCode, String errorMsg) {
        return update(null, new LambdaUpdateWrapper<BookingRefundNotifyLogDO>()
                .eq(BookingRefundNotifyLogDO::getId, id)
                .set(BookingRefundNotifyLogDO::getStatus, failStatus)
                .set(BookingRefundNotifyLogDO::getRetryCount, retryCount)
                .set(BookingRefundNotifyLogDO::getNextRetryTime, nextRetryTime)
                .set(BookingRefundNotifyLogDO::getErrorCode, errorCode)
                .set(BookingRefundNotifyLogDO::getErrorMsg, errorMsg));
    }
}
