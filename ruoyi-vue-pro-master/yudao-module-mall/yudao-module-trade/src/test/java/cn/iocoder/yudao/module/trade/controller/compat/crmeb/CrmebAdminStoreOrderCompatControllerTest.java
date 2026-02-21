package cn.iocoder.yudao.module.trade.controller.compat.crmeb;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.delivery.DeliveryExpressDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.delivery.DeliveryExpressMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderItemMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleRefundDecisionService;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderQueryService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderUpdateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmebAdminStoreOrderCompatControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmebAdminStoreOrderCompatController controller;

    @Mock
    private TradeOrderQueryService tradeOrderQueryService;
    @Mock
    private TradeOrderUpdateService tradeOrderUpdateService;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Mock
    private DeliveryExpressMapper deliveryExpressMapper;
    @Mock
    private AfterSaleService afterSaleService;
    @Mock
    private AfterSaleRefundDecisionService afterSaleRefundDecisionService;
    @Mock
    private AfterSaleMapper afterSaleMapper;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAdminOrderList() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(1L);
        order.setNo("ORDER_001");
        order.setPayStatus(true);
        order.setStatus(TradeOrderStatusEnum.UNDELIVERED.getStatus());
        order.setPayPrice(100);
        order.setTotalPrice(100);
        order.setCouponPrice(0);
        order.setReceiverName("张三");
        PageResult<TradeOrderDO> pageResult = new PageResult<>(Collections.singletonList(order), 1L);
        when(tradeOrderMapper.selectPage(
                any(cn.iocoder.yudao.framework.common.pojo.PageParam.class),
                any(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class)))
                .thenReturn(pageResult);

        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setOrderId(1L);
        item.setSpuId(11L);
        item.setSkuId(22L);
        item.setCount(1);
        item.setSpuName("测试商品");
        item.setPrice(100);
        when(tradeOrderQueryService.getOrderItemListByOrderId(anyCollection())).thenReturn(Collections.singletonList(item));

        cn.iocoder.yudao.module.trade.controller.app.compat.crmeb.CrmebCompatResult<CrmebAdminStoreOrderCompatController.CrmebPageRespVO<CrmebAdminStoreOrderCompatController.CrmebStoreOrderListItemRespVO>> result =
                controller.list(null, null, "all", 2, 1, 20);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getList().size());
        assertEquals("ORDER_001", result.getData().getList().get(0).getOrderId());
        assertEquals("未发货", result.getData().getList().get(0).getStatusStr().get("value"));
    }

    @Test
    void shouldReturnFailedWhenOrderInfoNotFound() {
        when(tradeOrderMapper.selectFirstByNoAndUserId("NOT_EXIST", null)).thenReturn(null);

        cn.iocoder.yudao.module.trade.controller.app.compat.crmeb.CrmebCompatResult<CrmebAdminStoreOrderCompatController.CrmebStoreOrderInfoRespVO> result =
                controller.info("NOT_EXIST");

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("订单不存在"));
    }

    @Test
    void shouldDeliveryWithExpressCode() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(9L);
        order.setNo("ORDER_DELIVERY_001");
        when(tradeOrderMapper.selectFirstByNoAndUserId("ORDER_DELIVERY_001", null)).thenReturn(order);

        DeliveryExpressDO express = new DeliveryExpressDO();
        express.setId(66L);
        when(deliveryExpressMapper.selectByCode("SF")).thenReturn(express);

        CrmebAdminStoreOrderCompatController.CrmebOrderDeliveryReqVO reqVO =
                new CrmebAdminStoreOrderCompatController.CrmebOrderDeliveryReqVO();
        reqVO.setOrderNo("ORDER_DELIVERY_001");
        reqVO.setDeliveryType("express");
        reqVO.setExpressCode("SF");
        reqVO.setExpressNumber("SF123456");

        cn.iocoder.yudao.module.trade.controller.app.compat.crmeb.CrmebCompatResult<String> result = controller.delivery(reqVO);

        assertEquals(200, result.getCode());
        assertEquals("success", result.getData());
        ArgumentCaptor<cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO.class);
        verify(tradeOrderUpdateService).deliveryOrder(captor.capture());
        assertEquals(9L, captor.getValue().getId());
        assertEquals(66L, captor.getValue().getLogisticsId());
        assertEquals("SF123456", captor.getValue().getLogisticsNo());
    }

    @Test
    void shouldReturnEmptyRefundTicketListWhenNoTicket() {
        when(tradeOrderItemMapper.selectOrderIdsByAfterSaleStatuses(anyCollection())).thenReturn(Collections.emptySet());
        when(tradeOrderMapper.selectPage(
                any(cn.iocoder.yudao.framework.common.pojo.PageParam.class),
                any(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        cn.iocoder.yudao.module.trade.controller.app.compat.crmeb.CrmebCompatResult<CrmebAdminStoreOrderCompatController.CrmebPageRespVO<CrmebAdminStoreOrderCompatController.CrmebStoreOrderListItemRespVO>> result =
                controller.refundTicketList(null, null, null, 2, 1, 20);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().getList().isEmpty());
    }

    @Test
    void shouldAgreeAndRefundWhenConfirmByOrderNo() {
        mockLoginUser(7L);
        setStoreRefundExecuteEnabled(true);

        AfterSaleDO apply = new AfterSaleDO();
        apply.setId(88L);
        apply.setOrderNo("ORDER_REFUND_001");
        apply.setStatus(AfterSaleStatusEnum.APPLY.getStatus());
        when(afterSaleMapper.selectFirstByOrderNoAndStatuses(eq("ORDER_REFUND_001"), anyCollection())).thenReturn(apply);

        AfterSaleDO waitRefund = new AfterSaleDO();
        waitRefund.setId(88L);
        waitRefund.setOrderNo("ORDER_REFUND_001");
        waitRefund.setStatus(AfterSaleStatusEnum.WAIT_REFUND.getStatus());
        when(afterSaleService.getAfterSale(88L)).thenReturn(waitRefund);

        CrmebAdminStoreOrderCompatController.CrmebRefundConfirmReqVO reqVO =
                new CrmebAdminStoreOrderCompatController.CrmebRefundConfirmReqVO();
        reqVO.setOrderNo("ORDER_REFUND_001");
        cn.iocoder.yudao.module.trade.controller.app.compat.crmeb.CrmebCompatResult<Boolean> result =
                controller.refundConfirm(reqVO, null, null, null, null, null);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(afterSaleService).agreeAfterSale(7L, 88L);
        verify(afterSaleRefundDecisionService).checkAndAuditForExecution(eq(7L), eq(UserTypeEnum.ADMIN.getValue()),
                eq(waitRefund), eq(false));
        verify(afterSaleService).refundAfterSale(eq(7L), any(), eq(88L));
    }

    @Test
    void shouldRefundWhenConfirmByQueryParams() {
        mockLoginUser(9L);
        setStoreRefundExecuteEnabled(true);

        AfterSaleDO waitRefund = new AfterSaleDO();
        waitRefund.setId(99L);
        waitRefund.setOrderNo("ORDER_REFUND_002");
        waitRefund.setStatus(AfterSaleStatusEnum.WAIT_REFUND.getStatus());
        when(afterSaleMapper.selectFirstByOrderNoAndStatuses(eq("ORDER_REFUND_002"), anyCollection())).thenReturn(waitRefund);

        cn.iocoder.yudao.module.trade.controller.app.compat.crmeb.CrmebCompatResult<Boolean> result =
                controller.refundConfirmByQuery(null, null, "ORDER_REFUND_002", null, null);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(afterSaleRefundDecisionService).checkAndAuditForExecution(eq(9L), eq(UserTypeEnum.ADMIN.getValue()),
                eq(waitRefund), eq(false));
        verify(afterSaleService).refundAfterSale(eq(9L), any(), eq(99L));
        verify(afterSaleService, never()).agreeAfterSale(9L, 99L);
    }

    @Test
    void shouldBlockRefundConfirmWhenStoreRefundExecutionDisabled() {
        mockLoginUser(11L);
        setStoreRefundExecuteEnabled(false);

        CrmebAdminStoreOrderCompatController.CrmebRefundConfirmReqVO reqVO =
                new CrmebAdminStoreOrderCompatController.CrmebRefundConfirmReqVO();
        reqVO.setOrderNo("ORDER_REFUND_BLOCK");

        cn.iocoder.yudao.module.trade.controller.app.compat.crmeb.CrmebCompatResult<Boolean> result =
                controller.refundConfirm(reqVO, null, null, null, null, null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("门店侧已禁用退款执行"));
        verify(afterSaleMapper, never()).selectFirstByOrderNoAndStatuses(any(), anyCollection());
        verify(afterSaleRefundDecisionService, never()).checkAndAuditForExecution(any(), any(), any(), anyBoolean());
        verify(afterSaleService, never()).agreeAfterSale(any(), any());
        verify(afterSaleService, never()).refundAfterSale(any(), any(), any());
    }

    private void mockLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private void setStoreRefundExecuteEnabled(boolean enabled) {
        ReflectionTestUtils.setField(controller, "storeRefundExecuteEnabled", enabled);
    }

}
