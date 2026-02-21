package cn.iocoder.yudao.module.trade.controller.app.compat.crmeb;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.api.channel.PayChannelApi;
import cn.iocoder.yudao.module.pay.api.wallet.PayWalletApi;
import cn.iocoder.yudao.module.pay.api.wallet.dto.PayWalletRespDTO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderSettlementRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.config.TradeConfigDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderItemMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderItemAfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleService;
import cn.iocoder.yudao.module.trade.service.config.TradeConfigService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderQueryService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderUpdateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmebFrontOrderCompatControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmebFrontOrderCompatController controller;

    @Mock
    private AfterSaleService afterSaleService;
    @Mock
    private TradeOrderQueryService tradeOrderQueryService;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Mock
    private TradeOrderUpdateService tradeOrderUpdateService;
    @Mock
    private CrmebPreOrderStore crmebPreOrderStore;
    @Mock
    private PayChannelApi payChannelApi;
    @Mock
    private PayWalletApi payWalletApi;
    @Mock
    private TradeConfigService tradeConfigService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnRefundReasonList() {
        CrmebCompatResult<java.util.List<String>> result = controller.refundReason();
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());
    }

    @Test
    void shouldReturnApplyRefundOrderInfoByOrderNo() {
        mockLoginUser(100L);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(1L);
        order.setNo("NO_20260219_001");
        order.setPayStatus(true);
        order.setPayPrice(199);
        when(tradeOrderMapper.selectFirstByNoAndUserId("NO_20260219_001", 100L)).thenReturn(order);

        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setSkuId(301L);
        item.setSpuId(501L);
        item.setCount(2);
        item.setPicUrl("https://img.example/a.png");
        item.setSpuName("肩颈理疗套餐");
        item.setPrice(99);
        item.setCommentStatus(false);
        when(tradeOrderQueryService.getOrderItemListByOrderId(1L)).thenReturn(Collections.singletonList(item));

        CrmebCompatResult<CrmebFrontOrderCompatController.CrmebApplyRefundOrderInfoRespVO> result =
                controller.applyRefundOrder("NO_20260219_001");

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("NO_20260219_001", result.getData().getOrderId());
        assertTrue(Boolean.TRUE.equals(result.getData().getPaid()));
        assertEquals(new BigDecimal("1.99"), result.getData().getPayPrice());
        assertEquals(2, result.getData().getTotalNum());
        assertNotNull(result.getData().getOrderInfoList());
        assertEquals(1, result.getData().getOrderInfoList().size());
        assertEquals("肩颈理疗套餐", result.getData().getOrderInfoList().get(0).getStoreName());
    }

    @Test
    void shouldReturnOrderDetailByOrderNo() {
        mockLoginUser(200L);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(8L);
        order.setNo("NO_20260219_DETAIL_001");
        order.setDeliveryType(1);
        order.setPayStatus(true);
        order.setStatus(TradeOrderStatusEnum.DELIVERED.getStatus());
        order.setPayChannelCode("wx_lite");
        order.setTotalPrice(1200);
        order.setPayPrice(1000);
        order.setDeliveryPrice(200);
        order.setCouponPrice(100);
        order.setPointPrice(50);
        order.setCouponId(9L);
        order.setUsePoint(10);
        order.setReceiverName("张三");
        order.setReceiverMobile("13800000000");
        order.setReceiverDetailAddress("测试路 100 号");
        when(tradeOrderMapper.selectFirstByNoAndUserId("NO_20260219_DETAIL_001", 200L)).thenReturn(order);

        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setSpuId(6001L);
        item.setSkuId(7001L);
        item.setSpuName("肩颈放松");
        item.setPicUrl("https://img.example/1.png");
        item.setCount(1);
        item.setPrice(1200);
        when(tradeOrderQueryService.getOrderItemListByOrderId(8L)).thenReturn(Collections.singletonList(item));

        CrmebCompatResult<CrmebFrontOrderCompatController.CrmebStoreOrderDetailRespVO> result =
                controller.detail("NO_20260219_DETAIL_001");

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("NO_20260219_DETAIL_001", result.getData().getOrderId());
        assertEquals("张三", result.getData().getRealName());
        assertEquals("13800000000", result.getData().getUserPhone());
        assertEquals("测试路 100 号", result.getData().getUserAddress());
        assertEquals(new BigDecimal("10.00"), result.getData().getPayPrice());
        assertEquals("待收货", result.getData().getOrderStatusMsg());
        assertEquals("微信支付", result.getData().getPayTypeStr());
        assertEquals(1, result.getData().getStatus());
    }

    @Test
    void shouldReturnOrderListPageByTypeUnpaid() {
        mockLoginUser(210L);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(31L);
        order.setNo("NO_LIST_001");
        order.setPayStatus(false);
        order.setStatus(TradeOrderStatusEnum.UNPAID.getStatus());
        order.setPayPrice(199);
        order.setDeliveryPrice(0);
        order.setDeliveryType(1);
        PageResult<TradeOrderDO> pageResult = new PageResult<>(Collections.singletonList(order), 1L);
        when(tradeOrderMapper.selectPage(any(PageParam.class), any())).thenReturn(pageResult);

        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setOrderId(31L);
        item.setSkuId(701L);
        item.setSpuId(901L);
        item.setCount(1);
        item.setSpuName("肩颈护理");
        item.setPicUrl("https://img.example/list.png");
        item.setPrice(199);
        when(tradeOrderQueryService.getOrderItemListByOrderId(anyCollection()))
                .thenReturn(Collections.singletonList(item));

        CrmebCompatResult<CrmebFrontOrderCompatController.CrmebPageRespVO<CrmebFrontOrderCompatController.CrmebOrderListItemRespVO>> result =
                controller.list("", 0, 1, 20);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals("NO_LIST_001", result.getData().getList().get(0).getOrderId());
        assertEquals("待支付", result.getData().getList().get(0).getOrderStatus());
        assertFalse(Boolean.TRUE.equals(result.getData().getList().get(0).getPaid()));
    }

    @Test
    void shouldReturnOrderDataSummary() {
        mockLoginUser(220L);

        when(tradeOrderQueryService.getOrderCount(220L, null, null)).thenReturn(8L);
        when(tradeOrderQueryService.getOrderCount(220L, TradeOrderStatusEnum.UNPAID.getStatus(), null)).thenReturn(1L);
        when(tradeOrderQueryService.getOrderCount(220L, TradeOrderStatusEnum.UNDELIVERED.getStatus(), null)).thenReturn(2L);
        when(tradeOrderQueryService.getOrderCount(220L, TradeOrderStatusEnum.DELIVERED.getStatus(), null)).thenReturn(3L);
        when(tradeOrderQueryService.getOrderCount(220L, TradeOrderStatusEnum.COMPLETED.getStatus(), false)).thenReturn(1L);
        when(tradeOrderQueryService.getOrderCount(220L, TradeOrderStatusEnum.COMPLETED.getStatus(), null)).thenReturn(2L);
        when(tradeOrderItemMapper.selectOrderIdsByUserIdAndAfterSaleStatuses(eq(220L), anyCollection()))
                .thenReturn(new HashSet<>(Arrays.asList(301L, 302L)));
        when(tradeOrderMapper.selectSumPayPriceByUserId(220L)).thenReturn(12345L);

        CrmebCompatResult<CrmebFrontOrderCompatController.CrmebOrderDataRespVO> result = controller.data();

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(8, result.getData().getOrderCount());
        assertEquals(1, result.getData().getUnPaidCount());
        assertEquals(2, result.getData().getUnShippedCount());
        assertEquals(3, result.getData().getReceivedCount());
        assertEquals(1, result.getData().getEvaluatedCount());
        assertEquals(2, result.getData().getCompleteCount());
        assertEquals(2, result.getData().getRefundCount());
        assertEquals(new BigDecimal("123.45"), result.getData().getSumPrice());
    }

    @Test
    void shouldCreatePreOrderForBuyNow() {
        mockLoginUser(230L);

        CrmebFrontOrderCompatController.CrmebPreOrderDetailRequest detailRequest =
                new CrmebFrontOrderCompatController.CrmebPreOrderDetailRequest();
        detailRequest.setProductId(1001);
        detailRequest.setAttrValueId(2001);
        detailRequest.setProductNum(2);

        CrmebFrontOrderCompatController.CrmebPreOrderRequest request =
                new CrmebFrontOrderCompatController.CrmebPreOrderRequest();
        request.setPreOrderType("buyNow");
        request.setOrderDetails(Collections.singletonList(detailRequest));

        when(tradeOrderUpdateService.settlementOrder(eq(230L), any()))
                .thenReturn(buildSettlementResp(599, 0, 399, 39, 1, 500, 0));

        CrmebCompatResult<Map<String, Object>> result = controller.preOrder(request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        String preOrderNo = (String) result.getData().get("preOrderNo");
        assertTrue(StrUtil.isNotBlank(preOrderNo));
        verify(crmebPreOrderStore).save(eq(230L), any(CrmebPreOrderStore.CrmebPreOrderContext.class));
    }

    @Test
    void shouldLoadPreOrderByNo() {
        mockLoginUser(240L);
        mockPayConfig(240L);

        CrmebPreOrderStore.CrmebPreOrderContext context = new CrmebPreOrderStore.CrmebPreOrderContext();
        context.setPreOrderNo("pre240");
        context.setPreOrderType("buyNow");
        context.setShippingType(1);
        context.setUseIntegral(false);
        CrmebPreOrderStore.CrmebPreOrderItem item = new CrmebPreOrderStore.CrmebPreOrderItem();
        item.setSkuId(2001L);
        item.setCount(1);
        context.setItems(Collections.singletonList(item));
        when(crmebPreOrderStore.get(240L, "pre240")).thenReturn(context);
        when(tradeOrderUpdateService.settlementOrder(eq(240L), any()))
                .thenReturn(buildSettlementResp(599, 100, 538, 39, 6, 1000, 50));

        CrmebCompatResult<CrmebFrontOrderCompatController.CrmebPreOrderLoadRespVO> result =
                controller.loadPreOrder("pre240");

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getOrderInfoVo());
        assertEquals(new BigDecimal("5.99"), result.getData().getOrderInfoVo().getProTotalFee());
        assertEquals("1", result.getData().getYuePayStatus());
        assertEquals("1", result.getData().getPayWeixinOpen());
    }

    @Test
    void shouldComputePriceByPreOrderNo() {
        mockLoginUser(250L);

        CrmebPreOrderStore.CrmebPreOrderContext context = new CrmebPreOrderStore.CrmebPreOrderContext();
        context.setPreOrderNo("pre250");
        context.setShippingType(1);
        context.setUseIntegral(false);
        CrmebPreOrderStore.CrmebPreOrderItem item = new CrmebPreOrderStore.CrmebPreOrderItem();
        item.setSkuId(2001L);
        item.setCount(1);
        context.setItems(Collections.singletonList(item));
        when(crmebPreOrderStore.get(250L, "pre250")).thenReturn(context);
        when(tradeOrderUpdateService.settlementOrder(eq(250L), any()))
                .thenReturn(buildSettlementResp(599, 100, 499, 39, 5, 1000, 50));

        CrmebFrontOrderCompatController.CrmebOrderComputedPriceRequest request =
                new CrmebFrontOrderCompatController.CrmebOrderComputedPriceRequest();
        request.setPreOrderNo("pre250");
        request.setShippingType(1);
        request.setAddressId(12);
        request.setUseIntegral(true);
        request.setCouponId(9);

        CrmebCompatResult<CrmebFrontOrderCompatController.CrmebComputedOrderPriceRespVO> result =
                controller.computedPrice(request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(new BigDecimal("4.99"), result.getData().getPayFee());
        assertEquals(5, result.getData().getUsedIntegral());
        assertEquals(995, result.getData().getSurplusIntegral());
    }

    @Test
    void shouldCreateOrderByPreOrderNo() {
        mockLoginUser(260L);

        CrmebPreOrderStore.CrmebPreOrderContext context = new CrmebPreOrderStore.CrmebPreOrderContext();
        context.setPreOrderNo("pre260");
        context.setShippingType(1);
        context.setAddressId(10L);
        context.setUseIntegral(false);
        CrmebPreOrderStore.CrmebPreOrderItem item = new CrmebPreOrderStore.CrmebPreOrderItem();
        item.setSkuId(2001L);
        item.setCount(1);
        context.setItems(Collections.singletonList(item));
        when(crmebPreOrderStore.get(260L, "pre260")).thenReturn(context);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(66L);
        order.setNo("NO_260");
        when(tradeOrderUpdateService.createOrder(eq(260L), any(AppTradeOrderCreateReqVO.class))).thenReturn(order);

        CrmebFrontOrderCompatController.CrmebCreateOrderRequest request =
                new CrmebFrontOrderCompatController.CrmebCreateOrderRequest();
        request.setPreOrderNo("pre260");
        request.setShippingType(1);
        request.setAddressId(10);
        request.setUseIntegral(false);
        request.setMark("测试下单");

        CrmebCompatResult<Map<String, Object>> result = controller.createOrder(request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("NO_260", result.getData().get("orderNo"));
        verify(crmebPreOrderStore).remove(260L, "pre260");
    }

    @Test
    void shouldGetPayConfig() {
        mockLoginUser(270L);
        mockPayConfig(270L);

        CrmebCompatResult<CrmebFrontOrderCompatController.CrmebPayConfigRespVO> result = controller.getPayConfig();

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("1", result.getData().getPayWeixinOpen());
        assertEquals("1", result.getData().getYuePayStatus());
        assertEquals(new BigDecimal("12.34"), result.getData().getUserBalance());
    }

    @Test
    void shouldCancelOrderByIdFromRequestBody() {
        mockLoginUser(300L);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(18L);
        order.setNo("NO_20260219_CANCEL_001");
        when(tradeOrderQueryService.getOrder(300L, 18L)).thenReturn(order);

        CrmebFrontOrderCompatController.CrmebOrderCancelRequest reqVO =
                new CrmebFrontOrderCompatController.CrmebOrderCancelRequest();
        reqVO.setId("18");
        CrmebCompatResult<Boolean> result = controller.cancel(reqVO, null);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(tradeOrderUpdateService).cancelOrderByMember(300L, 18L);
    }

    @Test
    void shouldTakeOrderByIdFromRequestBody() {
        mockLoginUser(310L);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(28L);
        order.setNo("NO_20260219_TAKE_001");
        when(tradeOrderQueryService.getOrder(310L, 28L)).thenReturn(order);

        CrmebFrontOrderCompatController.CrmebOrderCancelRequest reqVO =
                new CrmebFrontOrderCompatController.CrmebOrderCancelRequest();
        reqVO.setId("28");
        CrmebCompatResult<Boolean> result = controller.take(reqVO, null);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(tradeOrderUpdateService).receiveOrderByMember(310L, 28L);
    }

    @Test
    void shouldDeleteOrderByIdFromRequestBody() {
        mockLoginUser(320L);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(38L);
        order.setNo("NO_20260219_DEL_001");
        when(tradeOrderQueryService.getOrder(320L, 38L)).thenReturn(order);

        CrmebFrontOrderCompatController.CrmebOrderCancelRequest reqVO =
                new CrmebFrontOrderCompatController.CrmebOrderCancelRequest();
        reqVO.setId("38");
        CrmebCompatResult<Boolean> result = controller.delete(reqVO, null);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(tradeOrderUpdateService).deleteOrder(320L, 38L);
    }

    @Test
    void shouldCreateAfterSaleWhenSingleItemOrder() {
        mockLoginUser(100L);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(1L);
        order.setNo("T20260219001");
        when(tradeOrderQueryService.getOrder(100L, 1L)).thenReturn(order);

        TradeOrderItemDO orderItem = new TradeOrderItemDO();
        orderItem.setId(11L);
        orderItem.setOrderId(1L);
        orderItem.setPayPrice(100);
        orderItem.setAfterSaleStatus(TradeOrderItemAfterSaleStatusEnum.NONE.getStatus());
        when(tradeOrderQueryService.getOrderItemListByOrderId(1L))
                .thenReturn(Collections.singletonList(orderItem));
        when(afterSaleService.createAfterSale(eq(100L), any(AppAfterSaleCreateReqVO.class))).thenReturn(9001L);

        CrmebFrontOrderCompatController.CrmebOrderRefundApplyRequest reqVO =
                new CrmebFrontOrderCompatController.CrmebOrderRefundApplyRequest();
        reqVO.setId(1L);
        reqVO.setText("商品质量问题");
        reqVO.setReasonImage("https://a.png, https://b.png");
        reqVO.setExplain("外包装破损");

        CrmebCompatResult<Boolean> result = controller.refundApply(reqVO);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));

        ArgumentCaptor<AppAfterSaleCreateReqVO> captor = ArgumentCaptor.forClass(AppAfterSaleCreateReqVO.class);
        verify(afterSaleService).createAfterSale(eq(100L), captor.capture());
        assertEquals(11L, captor.getValue().getOrderItemId());
        assertEquals(100, captor.getValue().getRefundPrice());
        assertEquals("商品质量问题", captor.getValue().getApplyReason());
        assertEquals("外包装破损", captor.getValue().getApplyDescription());
        assertNotNull(captor.getValue().getApplyPicUrls());
        assertEquals(2, captor.getValue().getApplyPicUrls().size());
    }

    @Test
    void shouldFailWhenOrderContainsMultipleItemsWithoutOrderItemId() {
        mockLoginUser(101L);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(2L);
        order.setNo("T20260219002");
        when(tradeOrderQueryService.getOrder(101L, 2L)).thenReturn(order);

        TradeOrderItemDO item1 = new TradeOrderItemDO();
        item1.setId(21L);
        item1.setOrderId(2L);
        item1.setPayPrice(100);
        item1.setAfterSaleStatus(TradeOrderItemAfterSaleStatusEnum.NONE.getStatus());

        TradeOrderItemDO item2 = new TradeOrderItemDO();
        item2.setId(22L);
        item2.setOrderId(2L);
        item2.setPayPrice(200);
        item2.setAfterSaleStatus(TradeOrderItemAfterSaleStatusEnum.NONE.getStatus());

        when(tradeOrderQueryService.getOrderItemListByOrderId(2L)).thenReturn(Arrays.asList(item1, item2));

        CrmebFrontOrderCompatController.CrmebOrderRefundApplyRequest reqVO =
                new CrmebFrontOrderCompatController.CrmebOrderRefundApplyRequest();
        reqVO.setId(2L);
        reqVO.setText("不想买了");

        CrmebCompatResult<Boolean> result = controller.refundApply(reqVO);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("orderItemId"));
    }

    private void mockPayConfig(Long userId) {
        when(payChannelApi.existsEnabledChannelByCodePrefix(eq("wx_"))).thenReturn(true);
        when(payChannelApi.existsEnabledChannelByCodePrefix(eq("wallet"))).thenReturn(true);
        when(payChannelApi.existsEnabledChannelByCodePrefix(eq("alipay_"))).thenReturn(false);

        PayWalletRespDTO walletRespDTO = new PayWalletRespDTO();
        walletRespDTO.setBalance(1234);
        when(payWalletApi.getOrCreateWallet(eq(userId), eq(UserTypeEnum.MEMBER.getValue()))).thenReturn(walletRespDTO);

        TradeConfigDO tradeConfigDO = new TradeConfigDO();
        tradeConfigDO.setDeliveryPickUpEnabled(true);
        when(tradeConfigService.getTradeConfig()).thenReturn(tradeConfigDO);
    }

    private AppTradeOrderSettlementRespVO buildSettlementResp(int totalPrice, int couponPrice, int payPrice,
                                                              int deliveryPrice, int usePoint, int totalPoint,
                                                              int pointPrice) {
        AppTradeOrderSettlementRespVO respVO = new AppTradeOrderSettlementRespVO();
        AppTradeOrderSettlementRespVO.Price price = new AppTradeOrderSettlementRespVO.Price();
        price.setTotalPrice(totalPrice);
        price.setCouponPrice(couponPrice);
        price.setPayPrice(payPrice);
        price.setDeliveryPrice(deliveryPrice);
        price.setPointPrice(pointPrice);
        respVO.setPrice(price);

        AppTradeOrderSettlementRespVO.Address address = new AppTradeOrderSettlementRespVO.Address();
        address.setId(10L);
        address.setName("张三");
        address.setMobile("13800000000");
        address.setAreaName("浙江省杭州市西湖区");
        respVO.setAddress(address);

        AppTradeOrderSettlementRespVO.Item item = new AppTradeOrderSettlementRespVO.Item();
        item.setSpuId(1001L);
        item.setSkuId(2001);
        item.setSpuName("肩颈舒缓");
        item.setPicUrl("https://img.example/order.png");
        item.setPrice(totalPrice);
        item.setCount(1);
        respVO.setItems(Collections.singletonList(item));

        respVO.setUsePoint(usePoint);
        respVO.setTotalPoint(totalPoint);
        return respVO;
    }

    private void mockLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.MEMBER.getValue());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
