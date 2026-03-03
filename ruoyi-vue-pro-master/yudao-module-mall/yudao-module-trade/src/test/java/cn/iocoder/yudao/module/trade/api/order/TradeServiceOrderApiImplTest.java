package cn.iocoder.yudao.module.trade.api.order;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeServiceOrderMapper;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeServiceOrderApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeServiceOrderApiImpl api;

    @Mock
    private TradeServiceOrderMapper tradeServiceOrderMapper;
    @Mock
    private TradeServiceOrderService tradeServiceOrderService;

    @Test
    void markBookedByPayOrderId_shouldOnlyTransitionWaitBooking() {
        when(tradeServiceOrderMapper.selectListByPayOrderId(1001L)).thenReturn(Arrays.asList(
                buildServiceOrder(1L, TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus()),
                buildServiceOrder(2L, TradeServiceOrderStatusEnum.BOOKED.getStatus())
        ));

        int synced = api.markBookedByPayOrderId(1001L, "BK-1001", "SYNC");

        assertEquals(1, synced);
        verify(tradeServiceOrderService).markBooked(1L, "BK-1001", "SYNC");
        verify(tradeServiceOrderService, never()).markBooked(2L, "BK-1001", "SYNC");
    }

    @Test
    void startServingByPayOrderId_shouldOnlyTransitionBooked() {
        when(tradeServiceOrderMapper.selectListByPayOrderId(1002L)).thenReturn(Arrays.asList(
                buildServiceOrder(3L, TradeServiceOrderStatusEnum.BOOKED.getStatus()),
                buildServiceOrder(4L, TradeServiceOrderStatusEnum.SERVING.getStatus())
        ));

        int synced = api.startServingByPayOrderId(1002L, "SYNC");

        assertEquals(1, synced);
        verify(tradeServiceOrderService).startServing(3L, "SYNC");
        verify(tradeServiceOrderService, never()).startServing(4L, "SYNC");
    }

    @Test
    void finishServingByPayOrderId_shouldOnlyTransitionServing() {
        when(tradeServiceOrderMapper.selectListByPayOrderId(1003L)).thenReturn(Arrays.asList(
                buildServiceOrder(5L, TradeServiceOrderStatusEnum.SERVING.getStatus()),
                buildServiceOrder(6L, TradeServiceOrderStatusEnum.FINISHED.getStatus())
        ));

        int synced = api.finishServingByPayOrderId(1003L, "SYNC");

        assertEquals(1, synced);
        verify(tradeServiceOrderService).finishServing(5L, "SYNC");
        verify(tradeServiceOrderService, never()).finishServing(6L, "SYNC");
    }

    @Test
    void cancelByPayOrderId_shouldSkipFinishedAndCancelled() {
        when(tradeServiceOrderMapper.selectListByPayOrderId(1004L)).thenReturn(Arrays.asList(
                buildServiceOrder(7L, TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus()),
                buildServiceOrder(8L, TradeServiceOrderStatusEnum.BOOKED.getStatus()),
                buildServiceOrder(9L, TradeServiceOrderStatusEnum.SERVING.getStatus()),
                buildServiceOrder(10L, TradeServiceOrderStatusEnum.FINISHED.getStatus()),
                buildServiceOrder(11L, TradeServiceOrderStatusEnum.CANCELLED.getStatus())
        ));

        int synced = api.cancelByPayOrderId(1004L, "SYNC");

        assertEquals(3, synced);
        verify(tradeServiceOrderService).cancelServiceOrder(7L, "SYNC");
        verify(tradeServiceOrderService).cancelServiceOrder(8L, "SYNC");
        verify(tradeServiceOrderService).cancelServiceOrder(9L, "SYNC");
        verify(tradeServiceOrderService, never()).cancelServiceOrder(10L, "SYNC");
        verify(tradeServiceOrderService, never()).cancelServiceOrder(11L, "SYNC");
    }

    @Test
    void syncByPayOrderId_shouldIgnoreInvalidPayOrderId() {
        assertEquals(0, api.markBookedByPayOrderId(null, "BK", "SYNC"));
        assertEquals(0, api.startServingByPayOrderId(0L, "SYNC"));
        assertEquals(0, api.finishServingByPayOrderId(-1L, "SYNC"));
        assertEquals(0, api.cancelByPayOrderId(null, "SYNC"));
        verify(tradeServiceOrderMapper, never()).selectListByPayOrderId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void syncByPayOrderId_shouldReturnZeroWhenNoServiceOrder() {
        when(tradeServiceOrderMapper.selectListByPayOrderId(1999L)).thenReturn(Collections.emptyList());

        assertEquals(0, api.markBookedByPayOrderId(1999L, "BK-1999", "SYNC"));
        verify(tradeServiceOrderService, never()).markBooked(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private TradeServiceOrderDO buildServiceOrder(Long id, Integer status) {
        return TradeServiceOrderDO.builder()
                .id(id)
                .status(status)
                .build();
    }

}
