package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundRepairCandidateDO;
import com.hxy.module.booking.dal.mysql.BookingRefundNotifyLogMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReconcileQueryMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.service.BookingOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingRefundNotifyLogServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingRefundNotifyLogServiceImpl service;

    @Mock
    private BookingRefundNotifyLogMapper refundNotifyLogMapper;
    @Mock
    private BookingRefundReconcileQueryMapper refundReconcileQueryMapper;
    @Mock
    private BookingOrderService bookingOrderService;

    @Test
    void replayFailedLog_shouldMarkSuccessWhenReplayPassed() {
        BookingRefundNotifyLogDO logDO = new BookingRefundNotifyLogDO()
                .setId(1L)
                .setOrderId(1001L)
                .setPayRefundId(9001L)
                .setStatus("fail")
                .setRetryCount(1);
        when(refundNotifyLogMapper.selectById(eq(1L))).thenReturn(logDO);
        when(bookingOrderService.getOrder(eq(1001L))).thenReturn(buildRefundedOrder(1001L, 9001L));

        service.replayFailedLog(1L, 99L);

        verify(bookingOrderService).updateOrderRefunded(eq(1001L), eq(9001L));
        verify(refundNotifyLogMapper).updateReplaySuccess(eq(1L), eq("success"), eq(2));
        verify(refundNotifyLogMapper, never()).updateReplayFailure(any(), any(), any(), any(), any(), any());
    }

    @Test
    void replayFailedLog_shouldRejectWhenStatusNotFail() {
        BookingRefundNotifyLogDO logDO = new BookingRefundNotifyLogDO()
                .setId(2L)
                .setStatus("success");
        when(refundNotifyLogMapper.selectById(eq(2L))).thenReturn(logDO);

        assertServiceException(() -> service.replayFailedLog(2L, 99L), BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID);
        verify(bookingOrderService, never()).updateOrderRefunded(any(), any());
    }

    @Test
    void replayFailedLog_shouldMarkFailureWhenReplayThrows() {
        BookingRefundNotifyLogDO logDO = new BookingRefundNotifyLogDO()
                .setId(3L)
                .setOrderId(1003L)
                .setPayRefundId(9003L)
                .setStatus("fail")
                .setRetryCount(2);
        when(refundNotifyLogMapper.selectById(eq(3L))).thenReturn(logDO);
        doThrow(exception(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT))
                .when(bookingOrderService).updateOrderRefunded(eq(1003L), eq(9003L));

        assertServiceException(() -> service.replayFailedLog(3L, 99L), BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT);
        verify(refundNotifyLogMapper).updateReplayFailure(eq(3L), eq("fail"), eq(3),
                any(LocalDateTime.class), eq(String.valueOf(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT.getCode())), any());
    }

    @Test
    void reconcileRefundedOrders_shouldFailOpen() {
        BookingRefundRepairCandidateDO c1 = new BookingRefundRepairCandidateDO();
        c1.setOrderId(2001L);
        c1.setPayRefundId(92001L);
        c1.setMerchantRefundId("2001-refund");
        BookingRefundRepairCandidateDO c2 = new BookingRefundRepairCandidateDO();
        c2.setOrderId(2002L);
        c2.setPayRefundId(92002L);
        c2.setMerchantRefundId("2002-refund");
        when(refundReconcileQueryMapper.selectRepairCandidates(
                eq(PayRefundStatusEnum.SUCCESS.getStatus()),
                eq(BookingOrderStatusEnum.REFUNDED.getStatus()),
                eq(20)))
                .thenReturn(Arrays.asList(c1, c2));
        when(bookingOrderService.getOrder(eq(2001L))).thenReturn(buildRefundedOrder(2001L, 92001L));
        lenient().doThrow(exception(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT))
                .when(bookingOrderService).updateOrderRefunded(eq(2002L), eq(92002L));

        int repaired = service.reconcileRefundedOrders(20);

        assertEquals(1, repaired);
        ArgumentCaptor<BookingRefundNotifyLogDO> captor = ArgumentCaptor.forClass(BookingRefundNotifyLogDO.class);
        verify(refundNotifyLogMapper, times(2)).insert(captor.capture());
        assertEquals("success", captor.getAllValues().get(0).getStatus());
        assertEquals("fail", captor.getAllValues().get(1).getStatus());
        assertTrue(captor.getAllValues().get(1).getErrorCode().contains(
                String.valueOf(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT.getCode())));
    }

    @Test
    void reconcileRefundedOrders_shouldReturnZeroWhenNoCandidates() {
        when(refundReconcileQueryMapper.selectRepairCandidates(
                eq(PayRefundStatusEnum.SUCCESS.getStatus()),
                eq(BookingOrderStatusEnum.REFUNDED.getStatus()),
                eq(30)))
                .thenReturn(Collections.emptyList());

        int repaired = service.reconcileRefundedOrders(30);

        assertEquals(0, repaired);
        verify(refundNotifyLogMapper, never()).insert(org.mockito.ArgumentMatchers.<BookingRefundNotifyLogDO>any());
    }

    private BookingOrderDO buildRefundedOrder(Long orderId, Long payRefundId) {
        return BookingOrderDO.builder()
                .id(orderId)
                .status(BookingOrderStatusEnum.REFUNDED.getStatus())
                .payRefundId(payRefundId)
                .refundTime(LocalDateTime.now())
                .build();
    }
}
