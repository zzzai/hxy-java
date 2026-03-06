package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayRefundNotifyReqDTO;
import com.hxy.module.booking.service.BookingOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AppBookingOrderControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppBookingOrderController controller;

    @Mock
    private BookingOrderService bookingOrderService;

    @Test
    void updateOrderRefunded_shouldParseOrderIdWithSuffix() {
        PayRefundNotifyReqDTO reqDTO = new PayRefundNotifyReqDTO();
        reqDTO.setMerchantOrderId("1001");
        reqDTO.setMerchantRefundId("1001-refund");
        reqDTO.setPayRefundId(3001L);

        CommonResult<Boolean> result = controller.updateOrderRefunded(reqDTO);

        assertTrue(result.isSuccess());
        verify(bookingOrderService).updateOrderRefunded(eq(1001L), eq(3001L));
    }

    @Test
    void updateOrderRefunded_shouldParsePlainOrderId() {
        PayRefundNotifyReqDTO reqDTO = new PayRefundNotifyReqDTO();
        reqDTO.setMerchantOrderId("1002");
        reqDTO.setMerchantRefundId("1002");
        reqDTO.setPayRefundId(3002L);

        CommonResult<Boolean> result = controller.updateOrderRefunded(reqDTO);

        assertTrue(result.isSuccess());
        verify(bookingOrderService).updateOrderRefunded(eq(1002L), eq(3002L));
    }

    @Test
    void updateOrderRefunded_shouldThrowWhenMerchantRefundIdInvalid() {
        PayRefundNotifyReqDTO reqDTO = new PayRefundNotifyReqDTO();
        reqDTO.setMerchantOrderId("1003");
        reqDTO.setMerchantRefundId("invalid-refund-no");
        reqDTO.setPayRefundId(3003L);

        assertServiceException(() -> controller.updateOrderRefunded(reqDTO),
                BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID);
        verify(bookingOrderService, never()).updateOrderRefunded(eq(1003L), eq(3003L));
    }

    @Test
    void updateOrderRefunded_shouldPropagateConflict() {
        PayRefundNotifyReqDTO reqDTO = new PayRefundNotifyReqDTO();
        reqDTO.setMerchantOrderId("1004");
        reqDTO.setMerchantRefundId("1004-refund");
        reqDTO.setPayRefundId(3004L);
        doThrow(exception(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT))
                .when(bookingOrderService).updateOrderRefunded(eq(1004L), eq(3004L));

        assertServiceException(() -> controller.updateOrderRefunded(reqDTO),
                BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT);
        verify(bookingOrderService).updateOrderRefunded(eq(1004L), eq(3004L));
    }
}

