package com.zbkj.service.service;

import com.zbkj.common.request.ServiceBookingCreateRequest;
import com.zbkj.common.request.ServiceBookingPayRequest;
import com.zbkj.common.request.ServiceBookingVerifyRequest;
import com.zbkj.common.response.ServiceBookingCardCheckResponse;
import com.zbkj.common.response.OrderPayResultResponse;
import com.zbkj.common.response.ServiceBookingOrderResponse;
import com.zbkj.common.response.ServiceBookingSlotResponse;
import com.zbkj.common.response.ServiceBookingVerifyRecordResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * 服务预约业务
 */
public interface ServiceBookingService {

    /**
     * 创建服务预约订单
     */
    ServiceBookingOrderResponse create(ServiceBookingCreateRequest request);

    /**
     * 查询排班时间槽
     */
    List<ServiceBookingSlotResponse> listSlots(Integer scheduleId);

    /**
     * 查询预约订单详情
     */
    ServiceBookingOrderResponse detail(String orderNo);

    /**
     * 预约订单支付发起
     */
    OrderPayResultResponse pay(ServiceBookingPayRequest request, String ip);

    /**
     * 用户取消预约订单
     */
    Boolean cancel(String orderNo);

    /**
     * 预约订单支付成功
     */
    Boolean paySuccess(String orderNo);

    /**
     * 预约订单核销完成
     */
    Boolean verify(ServiceBookingVerifyRequest request);

    /**
     * 会员卡可用性检查
     */
    ServiceBookingCardCheckResponse checkMemberCard(String orderNo, Integer usageTimes, BigDecimal usageAmount);

    /**
     * 查询预约核销流水
     */
    List<ServiceBookingVerifyRecordResponse> listVerifyRecords(String orderNo);

    /**
     * 释放超时未支付锁单
     * @param limit 本次最大处理数量
     */
    Integer releaseExpiredLocks(Integer limit);
}
