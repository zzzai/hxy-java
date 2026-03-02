package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.OffpeakRuleDO;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.dal.mysql.BookingOrderMapper;
import com.hxy.module.booking.dal.mysql.TimeSlotMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.enums.TimeSlotStatusEnum;
import com.hxy.module.booking.service.OffpeakRuleService;
import com.hxy.module.booking.service.TechnicianCommissionService;
import com.hxy.module.booking.service.TechnicianDispatchService;
import com.hxy.module.booking.service.TimeSlotService;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static com.hxy.module.booking.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link BookingOrderServiceImpl} 的单元测试类
 */
@Import(BookingOrderServiceImpl.class)
public class BookingOrderServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BookingOrderServiceImpl bookingOrderService;

    @Resource
    private BookingOrderMapper bookingOrderMapper;

    @MockBean
    private TimeSlotService timeSlotService;
    @MockBean
    private TechnicianDispatchService technicianDispatchService;
    @MockBean
    private ProductSpuApi productSpuApi;
    @MockBean
    private ProductSkuApi productSkuApi;
    @MockBean
    private OffpeakRuleService offpeakRuleService;
    @MockBean
    private TechnicianCommissionService technicianCommissionService;
    @MockBean
    private PayOrderApi payOrderApi;
    @MockBean
    private PayRefundApi payRefundApi;

    @Test
    public void testCreateOrder_success() {
        // 准备数据
        Long userId = 1L;
        Long timeSlotId = 100L;
        Long spuId = 200L;
        Long skuId = 201L;
        String userRemark = "测试备注";

        // Mock时间槽服务
        when(timeSlotService.lockTimeSlot(eq(timeSlotId), eq(userId))).thenReturn(true);
        TimeSlotDO mockSlot = createMockTimeSlot(timeSlotId);
        when(timeSlotService.getTimeSlot(eq(timeSlotId))).thenReturn(mockSlot);

        // 调用
        Long orderId = bookingOrderService.createOrder(userId, timeSlotId, spuId, skuId, userRemark);

        // 断言
        assertNotNull(orderId);
        BookingOrderDO order = bookingOrderMapper.selectById(orderId);
        assertNotNull(order);
        assertEquals(userId, order.getUserId());
        assertEquals(timeSlotId, order.getTimeSlotId());
        assertEquals(spuId, order.getSpuId());
        assertEquals(skuId, order.getSkuId());
        assertEquals(userRemark, order.getUserRemark());
        assertEquals(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus(), order.getStatus());
        assertNotNull(order.getOrderNo());
        assertTrue(order.getOrderNo().startsWith("BK"));

        // 验证Mock调用
        verify(timeSlotService).lockTimeSlot(eq(timeSlotId), eq(userId));
        verify(timeSlotService).getTimeSlot(eq(timeSlotId));
    }

    @Test
    public void testCreateOrder_slotNotAvailable() {
        // 准备数据
        Long userId = 1L;
        Long timeSlotId = 100L;

        // Mock时间槽锁定失败
        when(timeSlotService.lockTimeSlot(eq(timeSlotId), eq(userId))).thenReturn(false);

        // 调用并断言异常
        assertServiceException(
                () -> bookingOrderService.createOrder(userId, timeSlotId, 200L, 201L, null),
                TIME_SLOT_NOT_AVAILABLE
        );
    }

    @Test
    public void testCreateOrder_offpeakDiscountFallbackFromRuleWhenSlotPriceIsZero() {
        // 准备数据
        Long userId = 1L;
        Long timeSlotId = 100L;
        Long spuId = 200L;
        Long skuId = 201L;

        // Mock 时间槽：闲时，但历史脏数据 offpeakPrice = 0
        when(timeSlotService.lockTimeSlot(eq(timeSlotId), eq(userId))).thenReturn(true);
        TimeSlotDO mockSlot = createMockTimeSlot(timeSlotId);
        mockSlot.setIsOffpeak(true);
        mockSlot.setOffpeakPrice(0);
        mockSlot.setSlotDate(LocalDate.of(2026, 2, 24)); // 周二
        mockSlot.setStartTime(LocalTime.of(13, 0));
        when(timeSlotService.getTimeSlot(eq(timeSlotId))).thenReturn(mockSlot);

        // Mock 商品价格
        ProductSkuRespDTO skuRespDTO = new ProductSkuRespDTO();
        skuRespDTO.setId(skuId);
        skuRespDTO.setPrice(10000);
        when(productSkuApi.getSku(eq(skuId))).thenReturn(skuRespDTO);
        when(productSpuApi.getSpu(eq(spuId))).thenReturn(null);

        // Mock 闲时规则（8 折）
        OffpeakRuleDO rule = OffpeakRuleDO.builder().discountRate(80).build();
        when(offpeakRuleService.matchOffpeakRule(eq(mockSlot.getStoreId()),
                eq(mockSlot.getSlotDate().getDayOfWeek().getValue()),
                eq(mockSlot.getStartTime()))).thenReturn(rule);
        when(offpeakRuleService.calculateOffpeakPrice(eq(rule), eq(10000))).thenReturn(8000);

        // 调用
        Long orderId = bookingOrderService.createOrder(userId, timeSlotId, spuId, skuId, "闲时预约");

        // 断言：实付价应按规则重算，不能为 0
        BookingOrderDO order = bookingOrderMapper.selectById(orderId);
        assertNotNull(order);
        assertEquals(10000, order.getOriginalPrice());
        assertEquals(8000, order.getPayPrice());
        assertEquals(2000, order.getDiscountPrice());
    }

    @Test
    public void testPayOrder_success() {
        // 准备数据：创建待支付订单
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        bookingOrderMapper.insert(order);

        Long payOrderId = 999L;

        // 调用
        bookingOrderService.payOrder(order.getId(), payOrderId);

        // 断言
        BookingOrderDO updatedOrder = bookingOrderMapper.selectById(order.getId());
        assertEquals(BookingOrderStatusEnum.PAID.getStatus(), updatedOrder.getStatus());
        assertEquals(payOrderId, updatedOrder.getPayOrderId());
        assertNotNull(updatedOrder.getPayTime());

        // 验证时间槽确认预约
        verify(timeSlotService).confirmBooking(eq(order.getTimeSlotId()), eq(order.getId()));
    }

    @Test
    public void testPayOrder_alreadyPaid() {
        // 准备数据：创建已支付订单
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PAID.getStatus());
        order.setPayOrderId(888L);
        order.setPayTime(LocalDateTime.now());
        bookingOrderMapper.insert(order);

        // 调用并断言异常
        assertServiceException(
                () -> bookingOrderService.payOrder(order.getId(), 999L),
                BOOKING_ORDER_STATUS_ERROR
        );
    }

    @Test
    public void testCancelOrder_fromPendingPayment() {
        // 准备数据：创建待支付订单
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        bookingOrderMapper.insert(order);

        String cancelReason = "用户主动取消";

        // 调用
        bookingOrderService.cancelOrder(order.getId(), order.getUserId(), cancelReason);

        // 断言
        BookingOrderDO updatedOrder = bookingOrderMapper.selectById(order.getId());
        assertEquals(BookingOrderStatusEnum.CANCELLED.getStatus(), updatedOrder.getStatus());
        assertEquals(cancelReason, updatedOrder.getCancelReason());
        assertNotNull(updatedOrder.getCancelTime());

        // 验证时间槽取消
        verify(timeSlotService).cancelBooking(eq(order.getTimeSlotId()));
    }

    @Test
    public void testCancelOrder_fromPaid() {
        // 准备数据：创建已支付订单
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PAID.getStatus());
        order.setPayOrderId(888L);
        order.setPayTime(LocalDateTime.now());
        bookingOrderMapper.insert(order);

        // 调用
        bookingOrderService.cancelOrder(order.getId(), order.getUserId(), "用户取消");

        // 断言
        BookingOrderDO updatedOrder = bookingOrderMapper.selectById(order.getId());
        assertEquals(BookingOrderStatusEnum.CANCELLED.getStatus(), updatedOrder.getStatus());
    }

    @Test
    public void testCancelOrder_fromInService() {
        // 准备数据：创建服务中订单
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.IN_SERVICE.getStatus());
        order.setServiceStartTime(LocalDateTime.now());
        bookingOrderMapper.insert(order);

        // 调用并断言异常：服务中不能取消
        assertServiceException(
                () -> bookingOrderService.cancelOrder(order.getId(), order.getUserId(), "取消"),
                BOOKING_ORDER_CANNOT_CANCEL
        );
    }

    @Test
    public void testStartService_success() {
        // 准备数据：创建已支付订单
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PAID.getStatus());
        order.setPayOrderId(888L);
        order.setPayTime(LocalDateTime.now());
        bookingOrderMapper.insert(order);

        // 调用
        bookingOrderService.startService(order.getId());

        // 断言
        BookingOrderDO updatedOrder = bookingOrderMapper.selectById(order.getId());
        assertEquals(BookingOrderStatusEnum.IN_SERVICE.getStatus(), updatedOrder.getStatus());
        assertNotNull(updatedOrder.getServiceStartTime());
    }

    @Test
    public void testStartService_notPaid() {
        // 准备数据：创建待支付订单
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        bookingOrderMapper.insert(order);

        // 调用并断言异常
        assertServiceException(
                () -> bookingOrderService.startService(order.getId()),
                BOOKING_ORDER_STATUS_ERROR
        );
    }

    @Test
    public void testCompleteService_success() {
        // 准备数据：创建服务中订单
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.IN_SERVICE.getStatus());
        order.setServiceStartTime(LocalDateTime.now().minusMinutes(60));
        bookingOrderMapper.insert(order);

        // 调用
        bookingOrderService.completeService(order.getId());

        // 断言
        BookingOrderDO updatedOrder = bookingOrderMapper.selectById(order.getId());
        assertEquals(BookingOrderStatusEnum.COMPLETED.getStatus(), updatedOrder.getStatus());
        assertNotNull(updatedOrder.getServiceEndTime());

        // 验证时间槽完成服务
        verify(timeSlotService).completeService(eq(order.getTimeSlotId()));
        // 验证佣金计算被触发
        verify(technicianCommissionService).calculateCommission(eq(order.getId()));
    }

    @Test
    public void testCompleteService_notInService() {
        // 准备数据：创建已支付订单（未开始服务）
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PAID.getStatus());
        bookingOrderMapper.insert(order);

        // 调用并断言异常
        assertServiceException(
                () -> bookingOrderService.completeService(order.getId()),
                BOOKING_ORDER_STATUS_ERROR
        );
    }

    @Test
    public void testRefundOrder_success() {
        // 准备数据：创建已支付订单
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PAID.getStatus());
        order.setPayOrderId(888L);
        order.setPayTime(LocalDateTime.now());
        bookingOrderMapper.insert(order);

        // 调用
        bookingOrderService.refundOrder(order.getId());

        // 断言
        BookingOrderDO updatedOrder = bookingOrderMapper.selectById(order.getId());
        assertEquals(BookingOrderStatusEnum.REFUNDED.getStatus(), updatedOrder.getStatus());

        // 验证时间槽取消
        verify(timeSlotService).cancelBooking(eq(order.getTimeSlotId()));
        // 验证佣金取消
        verify(technicianCommissionService).cancelCommission(eq(order.getId()));
        // 验证退款单创建
        verify(payRefundApi).createRefund(any());
    }

    @Test
    public void testGetOrder_notExists() {
        // 调用
        BookingOrderDO order = bookingOrderService.getOrder(999L);

        // 断言
        assertNull(order);
    }

    @Test
    public void testGetOrderByOrderNo() {
        // 准备数据
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        order.setOrderNo("BK123456789");
        bookingOrderMapper.insert(order);

        // 调用
        BookingOrderDO result = bookingOrderService.getOrderByOrderNo("BK123456789");

        // 断言
        assertNotNull(result);
        assertEquals(order.getId(), result.getId());
    }

    @Test
    public void testCreateOrder_timeSlotNull() {
        // 准备数据
        Long userId = 1L;
        Long timeSlotId = 100L;

        // Mock时间槽锁定成功但获取返回null
        when(timeSlotService.lockTimeSlot(eq(timeSlotId), eq(userId))).thenReturn(true);
        when(timeSlotService.getTimeSlot(eq(timeSlotId))).thenReturn(null);

        // 调用并断言异常
        assertServiceException(
                () -> bookingOrderService.createOrder(userId, timeSlotId, 200L, 201L, null),
                TIME_SLOT_NOT_EXISTS
        );
    }

    @Test
    public void testAutoCancelTimeoutOrders() {
        // 准备数据：创建一个超时的待支付订单（创建时间在16分钟前）
        BookingOrderDO timeoutOrder = createOrder(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        timeoutOrder.setOrderNo("BK_TIMEOUT");
        bookingOrderMapper.insert(timeoutOrder);
        // 手动更新创建时间为16分钟前
        BookingOrderDO updateTime = new BookingOrderDO();
        updateTime.setId(timeoutOrder.getId());
        updateTime.setCreateTime(LocalDateTime.now().minusMinutes(16));
        bookingOrderMapper.updateById(updateTime);

        // 创建一个未超时的待支付订单
        BookingOrderDO recentOrder = createOrder(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        recentOrder.setOrderNo("BK_RECENT");
        bookingOrderMapper.insert(recentOrder);

        // 调用
        int count = bookingOrderService.autoCancelTimeoutOrders();

        // 断言：只有超时订单被取消
        assertEquals(1, count);
        BookingOrderDO cancelledOrder = bookingOrderMapper.selectById(timeoutOrder.getId());
        assertEquals(BookingOrderStatusEnum.CANCELLED.getStatus(), cancelledOrder.getStatus());

        BookingOrderDO stillPending = bookingOrderMapper.selectById(recentOrder.getId());
        assertEquals(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus(), stillPending.getStatus());
    }

    @Test
    public void testCancelOrder_notOwner() {
        // 准备数据
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        order.setUserId(1L);
        bookingOrderMapper.insert(order);

        // 调用：不同用户尝试取消
        assertServiceException(
                () -> bookingOrderService.cancelOrder(order.getId(), 999L, "取消"),
                BOOKING_ORDER_NOT_OWNER
        );
    }

    @Test
    public void testGetOrderByUser_success() {
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PAID.getStatus());
        order.setUserId(1L);
        bookingOrderMapper.insert(order);

        BookingOrderDO result = bookingOrderService.getOrderByUser(order.getId(), 1L);
        assertNotNull(result);
        assertEquals(order.getId(), result.getId());
    }

    @Test
    public void testGetOrderByUser_notOwner() {
        BookingOrderDO order = createOrder(BookingOrderStatusEnum.PAID.getStatus());
        order.setUserId(1L);
        bookingOrderMapper.insert(order);

        assertServiceException(
                () -> bookingOrderService.getOrderByUser(order.getId(), 999L),
                BOOKING_ORDER_NOT_OWNER
        );
    }

    @Test
    public void testGetOrderListByUserId() {
        // 准备数据
        Long userId = 1L;
        BookingOrderDO order1 = createOrder(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        order1.setUserId(userId);
        order1.setOrderNo("BK001");
        bookingOrderMapper.insert(order1);

        BookingOrderDO order2 = createOrder(BookingOrderStatusEnum.PAID.getStatus());
        order2.setUserId(userId);
        order2.setOrderNo("BK002");
        bookingOrderMapper.insert(order2);

        // 不同用户的订单
        BookingOrderDO order3 = createOrder(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        order3.setUserId(2L);
        order3.setOrderNo("BK003");
        bookingOrderMapper.insert(order3);

        // 调用
        java.util.List<BookingOrderDO> result = bookingOrderService.getOrderListByUserId(userId);

        // 断言
        assertEquals(2, result.size());
    }

    /**
     * 创建测试用订单
     */
    private BookingOrderDO createOrder(Integer status) {
        return BookingOrderDO.builder()
                .orderNo("BK" + System.currentTimeMillis())
                .userId(1L)
                .storeId(1L)
                .technicianId(1L)
                .timeSlotId(100L)
                .spuId(200L)
                .skuId(201L)
                .serviceName("测试服务")
                .bookingDate(LocalDate.now())
                .bookingStartTime(LocalTime.of(9, 0))
                .bookingEndTime(LocalTime.of(10, 0))
                .duration(60)
                .originalPrice(10000)
                .discountPrice(0)
                .payPrice(10000)
                .isOffpeak(false)
                .status(status)
                .build();
    }

    /**
     * 创建Mock时间槽
     */
    private TimeSlotDO createMockTimeSlot(Long id) {
        TimeSlotDO slot = new TimeSlotDO();
        slot.setId(id);
        slot.setScheduleId(1L);
        slot.setTechnicianId(1L);
        slot.setStoreId(1L);
        slot.setSlotDate(LocalDate.now());
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setDuration(60);
        slot.setIsOffpeak(false);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.getStatus());
        return slot;
    }

}
