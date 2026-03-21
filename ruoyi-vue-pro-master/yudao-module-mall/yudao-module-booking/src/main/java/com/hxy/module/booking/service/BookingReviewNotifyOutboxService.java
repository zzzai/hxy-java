package com.hxy.module.booking.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxPageReqVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;

import java.util.List;

public interface BookingReviewNotifyOutboxService {

    void createNegativeReviewCreatedOutbox(BookingReviewDO review);

    int dispatchPendingNotifyOutbox(Integer limit);

    List<BookingReviewNotifyOutboxDO> getNotifyOutboxList(Long reviewId, String status, Integer limit);

    PageResult<BookingReviewNotifyOutboxDO> getNotifyOutboxPage(BookingReviewNotifyOutboxPageReqVO reqVO);

    int createManagerTodoSlaReminderOutbox(Integer limit);

    default int retryNotifyOutbox(List<Long> ids, String reason) {
        return retryNotifyOutbox(ids, null, reason);
    }

    int retryNotifyOutbox(List<Long> ids, Long operatorId, String reason);
}
