package com.hxy.module.booking.service;

import java.util.Map;

public interface BookingReviewWecomRobotSender {

    String send(String receiverAccount, String notifyType, Map<String, Object> templateParams);
}
