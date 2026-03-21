package com.hxy.module.booking.service;

import com.hxy.module.booking.dal.dataobject.BookingReviewDO;

public interface BookingReviewNotifyOutboxService {

    void createNegativeReviewCreatedOutbox(BookingReviewDO review);
}
