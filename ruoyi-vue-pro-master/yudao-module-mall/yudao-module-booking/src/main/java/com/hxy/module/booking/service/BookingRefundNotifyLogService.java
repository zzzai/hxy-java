package com.hxy.module.booking.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayRefundNotifyReqDTO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayRespVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;

import java.util.List;

public interface BookingRefundNotifyLogService {

    void recordNotifySuccess(Long orderId, PayRefundNotifyReqDTO notifyReqDTO);

    void recordNotifyFailure(Long orderId, PayRefundNotifyReqDTO notifyReqDTO, Throwable throwable);

    PageResult<BookingRefundNotifyLogDO> getNotifyLogPage(BookingRefundNotifyLogPageReqVO reqVO);

    BookingRefundNotifyLogReplayRespVO replayFailedLogs(List<Long> ids, boolean dryRun,
                                                        Long operatorId, String operatorNickname);

    BookingRefundNotifyLogReplayRespVO replayDueFailedLogs(Integer limit, String operator);

    int reconcileRefundedOrders(Integer limit);
}
