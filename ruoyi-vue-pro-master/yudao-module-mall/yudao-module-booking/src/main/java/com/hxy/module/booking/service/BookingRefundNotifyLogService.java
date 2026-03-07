package com.hxy.module.booking.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayRefundNotifyReqDTO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogSummaryRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogSyncTicketRespVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunLogDO;

import java.util.List;

public interface BookingRefundNotifyLogService {

    void recordNotifySuccess(Long orderId, PayRefundNotifyReqDTO notifyReqDTO);

    void recordNotifyFailure(Long orderId, PayRefundNotifyReqDTO notifyReqDTO, Throwable throwable);

    PageResult<BookingRefundNotifyLogDO> getNotifyLogPage(BookingRefundNotifyLogPageReqVO reqVO);

    BookingRefundNotifyLogReplayRespVO replayFailedLogs(List<Long> ids, boolean dryRun,
                                                        Long operatorId, String operatorNickname);

    BookingRefundNotifyLogReplayRespVO replayDueFailedLogs(Integer limit, boolean dryRun,
                                                           Long operatorId, String operatorNickname,
                                                           String triggerSource);

    default BookingRefundNotifyLogReplayRespVO replayDueFailedLogs(Integer limit, String operator) {
        return replayDueFailedLogs(limit, false, null, operator, "JOB");
    }

    PageResult<BookingRefundReplayRunLogDO> getReplayRunLogPage(BookingRefundReplayRunLogPageReqVO reqVO);

    BookingRefundReplayRunLogDO getReplayRunLog(Long id);

    BookingRefundReplayRunLogSummaryRespVO getReplayRunLogSummary(String runId);

    BookingRefundReplayRunLogSyncTicketRespVO syncReplayRunLogTickets(String runId, boolean onlyFail,
                                                                      Long operatorId, String operatorNickname);

    int reconcileRefundedOrders(Integer limit);
}
