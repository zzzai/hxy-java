package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayRefundNotifyReqDTO;
import com.hxy.module.booking.controller.app.vo.AppBookingOrderPageReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingOrderRespVO;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.service.BookingOrderService;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Arrays;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;

class AppBookingOrderControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppBookingOrderController controller;

    @Mock
    private BookingOrderService bookingOrderService;
    @Mock
    private BookingRefundNotifyLogService refundNotifyLogService;

    @Test
    void getOrder_shouldExposePayOrderIdAndProductBinding() {
        BookingOrderDO order = BookingOrderDO.builder()
                .id(11L)
                .orderNo("BK-11")
                .payOrderId(91001L)
                .spuId(31001L)
                .skuId(41001L)
                .timeSlotId(51001L)
                .build();
        when(bookingOrderService.getOrderByUser(11L, 66L)).thenReturn(order);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<AppBookingOrderRespVO> result = controller.getOrder(11L);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(91001L, result.getData().getPayOrderId());
            assertEquals(31001L, result.getData().getSpuId());
            assertEquals(41001L, result.getData().getSkuId());
            assertEquals(51001L, result.getData().getTimeSlotId());
        }

        verify(bookingOrderService).getOrderByUser(11L, 66L);
    }

    @Test
    void getOrderList_shouldReturnPageResultUsingStatusFilterAndPageParams() {
        AppBookingOrderPageReqVO reqVO = new AppBookingOrderPageReqVO();
        reqVO.setStatus(1);
        reqVO.setPageNo(2);
        reqVO.setPageSize(1);
        BookingOrderDO first = BookingOrderDO.builder().id(21L).orderNo("BK-21").payOrderId(10021L).build();
        BookingOrderDO second = BookingOrderDO.builder().id(22L).orderNo("BK-22").payOrderId(10022L).build();
        when(bookingOrderService.getOrderListByUserIdAndStatus(77L, 1)).thenReturn(Arrays.asList(first, second));

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(77L);

            CommonResult<PageResult<AppBookingOrderRespVO>> result = controller.getOrderList(reqVO);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(2L, result.getData().getTotal());
            assertEquals(1, result.getData().getList().size());
            assertEquals(22L, result.getData().getList().get(0).getId());
            assertEquals(10022L, result.getData().getList().get(0).getPayOrderId());
        }

        verify(bookingOrderService).getOrderListByUserIdAndStatus(77L, 1);
    }

    @Test
    void updateOrderRefunded_shouldParseOrderIdWithSuffix() {
        PayRefundNotifyReqDTO reqDTO = new PayRefundNotifyReqDTO();
        reqDTO.setMerchantOrderId("1001");
        reqDTO.setMerchantRefundId("1001-refund");
        reqDTO.setPayRefundId(3001L);

        CommonResult<Boolean> result = controller.updateOrderRefunded(reqDTO);

        assertTrue(result.isSuccess());
        verify(bookingOrderService).updateOrderRefunded(eq(1001L), eq(3001L));
        verify(refundNotifyLogService).recordNotifySuccess(eq(1001L), eq(reqDTO));
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
        verify(refundNotifyLogService).recordNotifySuccess(eq(1002L), eq(reqDTO));
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
        verify(refundNotifyLogService).recordNotifyFailure(isNull(), eq(reqDTO), any(Throwable.class));
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
        verify(refundNotifyLogService).recordNotifyFailure(eq(1004L), eq(reqDTO), any(Throwable.class));
    }
}
