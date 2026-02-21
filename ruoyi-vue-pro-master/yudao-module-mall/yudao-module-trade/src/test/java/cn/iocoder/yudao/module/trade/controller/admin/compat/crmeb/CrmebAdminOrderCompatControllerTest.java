package cn.iocoder.yudao.module.trade.controller.admin.compat.crmeb;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleRefundDecisionService;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleService;
import cn.iocoder.yudao.module.trade.service.delivery.DeliveryExpressService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderQueryService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderUpdateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmebAdminOrderCompatControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmebAdminOrderCompatController controller;

    @Mock
    private TradeOrderQueryService tradeOrderQueryService;
    @Mock
    private TradeOrderUpdateService tradeOrderUpdateService;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private AfterSaleService afterSaleService;
    @Mock
    private AfterSaleRefundDecisionService afterSaleRefundDecisionService;
    @Mock
    private MemberUserApi memberUserApi;
    @Mock
    private DeliveryExpressService deliveryExpressService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAdminOrderList() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(1L);
        order.setNo("ORDER_001");
        order.setUserId(10L);
        order.setPayPrice(199);
        order.setPayStatus(true);
        order.setStatus(TradeOrderStatusEnum.UNDELIVERED.getStatus());
        PageResult<TradeOrderDO> pageResult = new PageResult<>(Collections.singletonList(order), 1L);
        when(tradeOrderQueryService.getOrderPage(any())).thenReturn(pageResult);

        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setOrderId(1L);
        item.setSpuId(1001L);
        item.setSkuId(2001L);
        item.setSpuName("肩颈放松");
        item.setCount(1);
        item.setPrice(199);
        when(tradeOrderQueryService.getOrderItemListByOrderId(anyCollection()))
                .thenReturn(Collections.singletonList(item));

        MemberUserRespDTO user = new MemberUserRespDTO();
        user.setId(10L);
        user.setNickname("测试用户");
        user.setMobile("13800000000");
        when(memberUserApi.getUserList(anyCollection())).thenReturn(Collections.singletonList(user));

        CrmebCompatResult<CrmebAdminOrderCompatController.CrmebPageRespVO<CrmebAdminOrderCompatController.CrmebAdminOrderListItemRespVO>> result =
                controller.list(null, null, null, 2, null, 1, 20);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals("ORDER_001", result.getData().getList().get(0).getOrderId());
        assertEquals("测试用户", result.getData().getList().get(0).getRealName());
        assertEquals("微信支付", result.getData().getList().get(0).getPayTypeStr());
    }

    @Test
    void shouldReturnAdminOrderInfo() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(11L);
        order.setNo("ORDER_INFO_001");
        order.setUserId(20L);
        order.setPayStatus(true);
        order.setPayPrice(499);
        order.setTotalPrice(599);
        order.setDeliveryPrice(100);
        order.setStatus(TradeOrderStatusEnum.DELIVERED.getStatus());
        order.setReceiverName("张三");
        order.setReceiverMobile("13800001111");
        order.setReceiverDetailAddress("测试路 88 号");
        when(tradeOrderMapper.selectFirstByNoAndUserId("ORDER_INFO_001", null)).thenReturn(order);

        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setOrderId(11L);
        item.setSpuName("舒缓套餐");
        item.setCount(1);
        item.setPrice(599);
        when(tradeOrderQueryService.getOrderItemListByOrderId(11L)).thenReturn(Collections.singletonList(item));

        MemberUserRespDTO user = new MemberUserRespDTO();
        user.setId(20L);
        user.setNickname("小王");
        user.setMobile("13900002222");
        when(memberUserApi.getUser(20L)).thenReturn(user);

        CrmebCompatResult<CrmebAdminOrderCompatController.CrmebAdminOrderInfoRespVO> result = controller.info("ORDER_INFO_001");

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("ORDER_INFO_001", result.getData().getOrderId());
        assertEquals("张三", result.getData().getRealName());
        assertEquals("13800001111", result.getData().getUserPhone());
        assertEquals("测试路 88 号", result.getData().getUserAddress());
        assertEquals(1, result.getData().getOrderInfo().size());
    }

    @Test
    void shouldDeliveryByOrderNoAndLogisticsId() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(30L);
        order.setNo("ORDER_DELIVERY_001");
        when(tradeOrderMapper.selectFirstByNoAndUserId("ORDER_DELIVERY_001", null)).thenReturn(order);

        CrmebAdminOrderCompatController.CrmebAdminOrderDeliveryReqVO reqVO =
                new CrmebAdminOrderCompatController.CrmebAdminOrderDeliveryReqVO();
        reqVO.setOrderNo("ORDER_DELIVERY_001");
        reqVO.setDeliveryType("express");
        reqVO.setLogisticsId(9L);
        reqVO.setExpressNumber("SF123456789");

        CrmebCompatResult<Boolean> result = controller.delivery(reqVO);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));

        ArgumentCaptor<cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO.class);
        verify(tradeOrderUpdateService).deliveryOrder(captor.capture());
        assertEquals(30L, captor.getValue().getId());
        assertEquals(9L, captor.getValue().getLogisticsId());
        assertEquals("SF123456789", captor.getValue().getLogisticsNo());
    }

    @Test
    void shouldRefundConfirmById() {
        mockLoginUser(6L);
        CrmebAdminOrderCompatController.CrmebRefundTicketConfirmReqVO reqVO =
                new CrmebAdminOrderCompatController.CrmebRefundTicketConfirmReqVO();
        reqVO.setId(88L);
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setId(88L);
        afterSale.setStatus(AfterSaleStatusEnum.WAIT_REFUND.getStatus());
        when(afterSaleService.getAfterSale(88L)).thenReturn(afterSale);

        CrmebCompatResult<Boolean> result = controller.refundConfirm(reqVO, null, null);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(afterSaleRefundDecisionService).checkAndAuditForExecution(eq(6L), eq(UserTypeEnum.ADMIN.getValue()),
                eq(afterSale), eq(false));
        verify(afterSaleService).refundAfterSale(eq(6L), any(String.class), eq(88L));
    }

    @Test
    void shouldAgreeThenRefundWhenAfterSaleIsApplyStatus() {
        mockLoginUser(6L);
        CrmebAdminOrderCompatController.CrmebRefundTicketConfirmReqVO reqVO =
                new CrmebAdminOrderCompatController.CrmebRefundTicketConfirmReqVO();
        reqVO.setId(188L);

        AfterSaleDO applyAfterSale = new AfterSaleDO();
        applyAfterSale.setId(188L);
        applyAfterSale.setStatus(AfterSaleStatusEnum.APPLY.getStatus());
        AfterSaleDO waitRefundAfterSale = new AfterSaleDO();
        waitRefundAfterSale.setId(188L);
        waitRefundAfterSale.setStatus(AfterSaleStatusEnum.WAIT_REFUND.getStatus());
        when(afterSaleService.getAfterSale(188L)).thenReturn(applyAfterSale, waitRefundAfterSale);

        CrmebCompatResult<Boolean> result = controller.refundConfirm(reqVO, null, null);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(afterSaleService).agreeAfterSale(6L, 188L);
        verify(afterSaleRefundDecisionService).checkAndAuditForExecution(eq(6L), eq(UserTypeEnum.ADMIN.getValue()),
                eq(waitRefundAfterSale), eq(false));
        verify(afterSaleService).refundAfterSale(eq(6L), any(String.class), eq(188L));
    }

    @Test
    void shouldReturnRefundTicketPage() {
        AfterSaleDO ticket = new AfterSaleDO();
        ticket.setId(100L);
        ticket.setNo("AF_001");
        ticket.setOrderNo("ORDER_001");
        ticket.setStatus(AfterSaleStatusEnum.APPLY.getStatus());
        ticket.setUserId(77L);
        ticket.setSpuName("经络理疗");
        ticket.setRefundPrice(99);
        PageResult<AfterSaleDO> pageResult = new PageResult<>(Collections.singletonList(ticket), 1L);
        when(afterSaleService.getAfterSalePage(any())).thenReturn(pageResult);

        MemberUserRespDTO user = new MemberUserRespDTO();
        user.setId(77L);
        user.setNickname("会员A");
        user.setMobile("13812345678");
        when(memberUserApi.getUserList(anyCollection())).thenReturn(Collections.singletonList(user));

        CrmebCompatResult<CrmebAdminOrderCompatController.CrmebPageRespVO<CrmebAdminOrderCompatController.CrmebRefundTicketItemRespVO>> result =
                controller.refundTicketList(null, null, 1, 20);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals("AF_001", result.getData().getList().get(0).getNo());
        assertEquals("会员A", result.getData().getList().get(0).getUserName());
    }

    private void mockLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
    }
}
