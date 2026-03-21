package com.hxy.module.booking.service.impl;

import lombok.Getter;

@Getter
public class BookingReviewNotifyChannelBlockedException extends RuntimeException {

    private final String reasonCode;

    public BookingReviewNotifyChannelBlockedException(String reasonCode) {
        super(reasonCode);
        this.reasonCode = reasonCode;
    }
}
