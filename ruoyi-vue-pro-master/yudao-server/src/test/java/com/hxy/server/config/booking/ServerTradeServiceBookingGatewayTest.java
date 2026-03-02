package com.hxy.server.config.booking;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.service.BookingOrderService;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerTradeServiceBookingGatewayTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ServerTradeServiceBookingGateway gateway;

    @Mock
    private BookingOrderService bookingOrderService;
    @Mock
    private TradeServiceOrderService tradeServiceOrderService;

    @Test
    void createPendingBooking_skipWhenNull() {
        gateway.createPendingBooking(null);

        verify(bookingOrderService, never()).getOrderByPayOrderId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void createPendingBooking_skipWhenStatusNotWaitBooking() {
        TradeServiceOrderDO serviceOrder = TradeServiceOrderDO.builder()
                .id(1L)
                .status(TradeServiceOrderStatusEnum.BOOKED.getStatus())
                .payOrderId(100L)
                .build();

        gateway.createPendingBooking(serviceOrder);

        verify(bookingOrderService, never()).getOrderByPayOrderId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void createPendingBooking_skipWhenPayOrderIdMissing() {
        TradeServiceOrderDO serviceOrder = TradeServiceOrderDO.builder()
                .id(2L)
                .status(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus())
                .build();

        gateway.createPendingBooking(serviceOrder);

        verify(bookingOrderService, never()).getOrderByPayOrderId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void createPendingBooking_createPlaceholderWhenBookingMissing() {
        TradeServiceOrderDO serviceOrder = TradeServiceOrderDO.builder()
                .id(3L)
                .status(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus())
                .userId(9003L)
                .spuId(3003L)
                .skuId(4003L)
                .payOrderId(300L)
                .build();
        when(bookingOrderService.getOrderByPayOrderId(300L)).thenReturn(null);
        BookingOrderDO placeholder = BookingOrderDO.builder().id(31L).orderNo("BK_PLACEHOLDER_300").build();
        when(bookingOrderService.createPlaceholderOrder(9003L, 3003L, 4003L, 300L,
                "AUTO_CREATE_PLACEHOLDER_BOOKING")).thenReturn(placeholder);

        gateway.createPendingBooking(serviceOrder);

        verify(bookingOrderService).getOrderByPayOrderId(300L);
        verify(bookingOrderService).createPlaceholderOrder(9003L, 3003L, 4003L, 300L,
                "AUTO_CREATE_PLACEHOLDER_BOOKING");
        verify(tradeServiceOrderService).markBooked(3L, "BK_PLACEHOLDER_300", "SYNC_FROM_BOOKING_MODULE_BY_PAY_ORDER");
    }

    @Test
    void createPendingBooking_markBookedWhenBookingExists() {
        TradeServiceOrderDO serviceOrder = TradeServiceOrderDO.builder()
                .id(4L)
                .status(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus())
                .payOrderId(400L)
                .build();
        BookingOrderDO bookingOrder = BookingOrderDO.builder()
                .id(41L)
                .orderNo("BK123")
                .build();
        when(bookingOrderService.getOrderByPayOrderId(400L)).thenReturn(bookingOrder);

        gateway.createPendingBooking(serviceOrder);

        verify(tradeServiceOrderService).markBooked(4L, "BK123", "SYNC_FROM_BOOKING_MODULE_BY_PAY_ORDER");
    }

    @Test
    void createPendingBooking_ignoreServiceException() {
        TradeServiceOrderDO serviceOrder = TradeServiceOrderDO.builder()
                .id(5L)
                .status(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus())
                .payOrderId(500L)
                .build();
        BookingOrderDO bookingOrder = BookingOrderDO.builder()
                .id(51L)
                .orderNo("BK500")
                .build();
        when(bookingOrderService.getOrderByPayOrderId(500L)).thenReturn(bookingOrder);
        doThrow(new ServiceException(1_000_000_001, "状态已更新"))
                .when(tradeServiceOrderService).markBooked(5L, "BK500", "SYNC_FROM_BOOKING_MODULE_BY_PAY_ORDER");

        gateway.createPendingBooking(serviceOrder);

        verify(tradeServiceOrderService).markBooked(5L, "BK500", "SYNC_FROM_BOOKING_MODULE_BY_PAY_ORDER");
    }

}
