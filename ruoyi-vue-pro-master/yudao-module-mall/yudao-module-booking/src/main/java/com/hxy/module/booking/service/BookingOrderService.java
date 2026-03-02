package com.hxy.module.booking.service;

import com.hxy.module.booking.dal.dataobject.BookingOrderDO;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约订单 Service 接口
 */
public interface BookingOrderService {

    /**
     * 创建预约订单
     *
     * @param userId 用户ID
     * @param timeSlotId 时间槽ID（点钟模式必填，排钟模式可为null）
     * @param spuId 服务商品SPU ID
     * @param skuId 服务商品SKU ID
     * @param userRemark 用户备注
     * @return 订单ID
     */
    Long createOrder(Long userId, Long timeSlotId, Long spuId, Long skuId, String userRemark);

    /**
     * 创建预约订单（支持派单模式）
     *
     * @param userId       用户ID
     * @param timeSlotId   时间槽ID（点钟模式必填，排钟模式可为null）
     * @param spuId        服务商品SPU ID
     * @param skuId        服务商品SKU ID
     * @param userRemark   用户备注
     * @param dispatchMode 派单模式：1=点钟，2=排钟
     * @param storeId      门店ID（排钟模式必填）
     * @param bookingDate  预约日期（排钟模式必填）
     * @param startTime    开始时间（排钟模式必填）
     * @return 订单ID
     */
    Long createOrder(Long userId, Long timeSlotId, Long spuId, Long skuId, String userRemark,
                     Integer dispatchMode, Long storeId, java.time.LocalDate bookingDate, java.time.LocalTime startTime);

    /**
     * 支付订单
     */
    void payOrder(Long orderId, Long payOrderId);

    /**
     * 取消订单（用户端，校验归属）
     *
     * @param orderId 订单ID
     * @param userId 当前用户ID（用于权限校验，null表示跳过校验，如管理员或定时任务）
     * @param cancelReason 取消原因
     */
    void cancelOrder(Long orderId, Long userId, String cancelReason);

    /**
     * 开始服务
     */
    void startService(Long orderId);

    /**
     * 完成服务
     */
    void completeService(Long orderId);

    /**
     * 退款
     */
    void refundOrder(Long orderId);

    /**
     * 支付回调：更新订单为已支付
     *
     * @param id         预约订单ID
     * @param payOrderId 支付单ID
     */
    void updateOrderPaid(Long id, Long payOrderId);

    /**
     * 退款回调：更新订单退款完成
     *
     * @param id          预约订单ID
     * @param payRefundId 退款单ID
     */
    void updateOrderRefunded(Long id, Long payRefundId);

    /**
     * 获取订单（用户端，校验归属）
     *
     * @param id 订单ID
     * @param userId 当前用户ID
     * @return 订单
     */
    BookingOrderDO getOrderByUser(Long id, Long userId);

    /**
     * 根据订单号获取订单（用户端，校验归属）
     *
     * @param orderNo 订单号
     * @param userId 当前用户ID
     * @return 订单
     */
    BookingOrderDO getOrderByOrderNoAndUser(String orderNo, Long userId);

    /**
     * 获取订单（内部使用，不校验归属）
     */
    BookingOrderDO getOrder(Long id);

    /**
     * 根据订单号获取订单（内部使用，不校验归属）
     */
    BookingOrderDO getOrderByOrderNo(String orderNo);

    /**
     * 根据支付订单ID获取订单
     */
    BookingOrderDO getOrderByPayOrderId(Long payOrderId);

    /**
     * 获取用户订单列表
     */
    List<BookingOrderDO> getOrderListByUserId(Long userId);

    /**
     * 获取用户指定状态的订单列表
     */
    List<BookingOrderDO> getOrderListByUserIdAndStatus(Long userId, Integer status);

    /**
     * 获取技师指定日期的订单列表
     */
    List<BookingOrderDO> getOrderListByTechnicianAndDate(Long technicianId, LocalDate date);

    /**
     * 获取门店指定日期的订单列表
     */
    List<BookingOrderDO> getOrderListByStoreAndDate(Long storeId, LocalDate date);

    /**
     * 获取待支付超时的订单列表（定时任务用）
     */
    List<BookingOrderDO> getPendingPaymentTimeoutOrders();

    /**
     * 自动取消超时未支付订单（定时任务用）
     */
    int autoCancelTimeoutOrders();

}
