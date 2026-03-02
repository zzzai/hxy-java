package cn.iocoder.yudao.module.trade.controller.admin.order;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderMarkBookedReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderOperateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderPageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeServiceOrderControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeServiceOrderController controller;

    @Mock
    private TradeServiceOrderService tradeServiceOrderService;

    @Test
    void shouldGetServiceOrderPage() {
        TradeServiceOrderPageReqVO reqVO = new TradeServiceOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        TradeServiceOrderDO serviceOrder = new TradeServiceOrderDO();
        serviceOrder.setId(88L);
        serviceOrder.setStatus(10);
        PageResult<TradeServiceOrderDO> pageResult = new PageResult<>(Collections.singletonList(serviceOrder), 1L);
        when(tradeServiceOrderService.getServiceOrderPage(reqVO)).thenReturn(pageResult);

        CommonResult<PageResult<TradeServiceOrderRespVO>> result = controller.getServiceOrderPage(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(88L, result.getData().getList().get(0).getId());
        assertEquals("已预约", result.getData().getList().get(0).getStatusName());
        verify(tradeServiceOrderService).getServiceOrderPage(reqVO);
    }

    @Test
    void shouldGetServiceOrder() {
        TradeServiceOrderDO serviceOrder = new TradeServiceOrderDO();
        serviceOrder.setId(9L);
        serviceOrder.setStatus(0);
        serviceOrder.setBookingNo("BOOK001");

        when(tradeServiceOrderService.getServiceOrder(9L)).thenReturn(serviceOrder);

        CommonResult<TradeServiceOrderRespVO> result = controller.getServiceOrder(9L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(9L, result.getData().getId());
        assertEquals("BOOK001", result.getData().getBookingNo());
        assertEquals("待预约", result.getData().getStatusName());
        verify(tradeServiceOrderService).getServiceOrder(9L);
    }

    @Test
    void shouldGetServiceOrderListByOrderId() {
        TradeServiceOrderDO serviceOrder = new TradeServiceOrderDO();
        serviceOrder.setId(10L);
        serviceOrder.setOrderId(1001L);
        serviceOrder.setStatus(10);

        when(tradeServiceOrderService.getServiceOrderListByOrderId(1001L))
                .thenReturn(Collections.singletonList(serviceOrder));

        CommonResult<List<TradeServiceOrderRespVO>> result = controller.getServiceOrderListByOrderId(1001L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(10L, result.getData().get(0).getId());
        assertEquals("已预约", result.getData().get(0).getStatusName());
        verify(tradeServiceOrderService).getServiceOrderListByOrderId(1001L);
    }

    @Test
    void shouldMarkBooked() {
        TradeServiceOrderMarkBookedReqVO reqVO = new TradeServiceOrderMarkBookedReqVO();
        reqVO.setId(1L);
        reqVO.setBookingNo("BOOK202602240001");
        reqVO.setRemark("门店确认预约");

        CommonResult<Boolean> result = controller.markBooked(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(tradeServiceOrderService).markBooked(1L, "BOOK202602240001", "门店确认预约");
    }

    @Test
    void shouldStartServing() {
        TradeServiceOrderOperateReqVO reqVO = new TradeServiceOrderOperateReqVO();
        reqVO.setId(2L);
        reqVO.setRemark("技师已接单");

        CommonResult<Boolean> result = controller.startServing(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(tradeServiceOrderService).startServing(2L, "技师已接单");
    }

    @Test
    void shouldFinishServing() {
        TradeServiceOrderOperateReqVO reqVO = new TradeServiceOrderOperateReqVO();
        reqVO.setId(3L);
        reqVO.setRemark("服务完成");

        CommonResult<Boolean> result = controller.finishServing(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(tradeServiceOrderService).finishServing(3L, "服务完成");
    }

    @Test
    void shouldCancelServiceOrder() {
        TradeServiceOrderOperateReqVO reqVO = new TradeServiceOrderOperateReqVO();
        reqVO.setId(4L);
        reqVO.setRemark("客户改期");

        CommonResult<Boolean> result = controller.cancel(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(tradeServiceOrderService).cancelServiceOrder(4L, "客户改期");
    }

}
