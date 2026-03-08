package cn.iocoder.yudao.module.trade.controller.app.order;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderPayResultRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderQueryService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderUpdateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppTradeOrderControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppTradeOrderController controller;

    @Mock
    private TradeOrderUpdateService tradeOrderUpdateService;
    @Mock
    private TradeOrderQueryService tradeOrderQueryService;
    @Mock
    private PayOrderApi payOrderApi;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getOrderPayResult_shouldReturnFullPayloadWhenPayOrderExists() {
        mockLoginUser(100L);
        Long orderId = 2001L;
        Long payOrderId = 3001L;
        LocalDateTime paySuccessTime = LocalDateTime.of(2026, 3, 8, 10, 30, 0);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(orderId);
        order.setNo("T202603080001");
        order.setPayOrderId(payOrderId);
        order.setStatus(TradeOrderStatusEnum.UNDELIVERED.getStatus());
        order.setPayStatus(true);
        order.setRefundStatus(0);
        order.setRefundPrice(0);
        order.setPayChannelCode("wx_lite");
        when(tradeOrderQueryService.getOrder(eq(100L), eq(orderId))).thenReturn(order);

        PayOrderRespDTO payOrder = new PayOrderRespDTO();
        payOrder.setId(payOrderId);
        payOrder.setStatus(PayOrderStatusEnum.SUCCESS.getStatus());
        payOrder.setSuccessTime(paySuccessTime);
        when(payOrderApi.getOrder(eq(payOrderId))).thenReturn(payOrder);

        CommonResult<AppTradeOrderPayResultRespVO> result = controller.getOrderPayResult(orderId, false);

        assertTrue(result.isSuccess());
        AppTradeOrderPayResultRespVO data = result.getData();
        assertNotNull(data);
        assertEquals(orderId, data.getOrderId());
        assertEquals("T202603080001", data.getOrderNo());
        assertEquals(payOrderId, data.getPayOrderId());
        assertEquals(TradeOrderStatusEnum.UNDELIVERED.getStatus(), data.getOrderStatus());
        assertEquals(Boolean.TRUE, data.getOrderPayStatus());
        assertEquals("wx_lite", data.getPayChannelCode());
        assertEquals(PayOrderStatusEnum.SUCCESS.getStatus(), data.getPayOrderStatus());
        assertEquals("支付成功", data.getPayOrderStatusName());
        assertEquals(paySuccessTime, data.getPaySuccessTime());
        assertEquals("SUCCESS", data.getPayResultCode());
        assertEquals("支付成功", data.getPayResultDesc());
        assertNull(data.getDegraded());
        assertNull(data.getDegradeReason());
    }

    @Test
    void getOrderPayResult_shouldReturnNullWhenOrderNotExists() {
        mockLoginUser(100L);
        Long orderId = 2002L;
        when(tradeOrderQueryService.getOrder(eq(100L), eq(orderId))).thenReturn(null);

        CommonResult<AppTradeOrderPayResultRespVO> result = controller.getOrderPayResult(orderId, false);

        assertTrue(result.isSuccess());
        assertNull(result.getData());
        verifyNoInteractions(payOrderApi);
    }

    @Test
    void getOrderPayResult_shouldMarkDegradedWhenPayOrderMissing() {
        mockLoginUser(100L);
        Long orderId = 2003L;
        Long payOrderId = 3003L;

        TradeOrderDO order = new TradeOrderDO();
        order.setId(orderId);
        order.setNo("T202603080003");
        order.setPayOrderId(payOrderId);
        order.setStatus(TradeOrderStatusEnum.UNPAID.getStatus());
        order.setPayStatus(false);
        order.setRefundStatus(0);
        order.setRefundPrice(0);
        order.setPayChannelCode("wx_lite");
        when(tradeOrderQueryService.getOrder(eq(100L), eq(orderId))).thenReturn(order);
        when(payOrderApi.getOrder(eq(payOrderId))).thenReturn(null);

        CommonResult<AppTradeOrderPayResultRespVO> result = controller.getOrderPayResult(orderId, false);

        assertTrue(result.isSuccess());
        AppTradeOrderPayResultRespVO data = result.getData();
        assertNotNull(data);
        assertEquals("WAITING", data.getPayResultCode());
        assertEquals("待支付", data.getPayResultDesc());
        assertEquals(Boolean.TRUE, data.getDegraded());
        assertEquals("PAY_ORDER_NOT_FOUND", data.getDegradeReason());
    }

    private void mockLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.MEMBER.getValue());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
