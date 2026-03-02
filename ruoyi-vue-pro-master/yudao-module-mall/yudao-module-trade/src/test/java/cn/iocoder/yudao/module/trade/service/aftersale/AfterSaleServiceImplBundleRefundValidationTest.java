package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;
import cn.iocoder.yudao.module.promotion.api.combination.CombinationRecordApi;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleCreateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeServiceOrderMapper;
import cn.iocoder.yudao.module.trade.dal.redis.no.TradeNoRedisDAO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleWayEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderItemAfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import cn.iocoder.yudao.module.trade.framework.order.config.TradeOrderProperties;
import cn.iocoder.yudao.module.trade.service.delivery.DeliveryExpressService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderQueryService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_CREATE_FAIL_REFUND_PRICE_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AfterSaleServiceImplBundleRefundValidationTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleServiceImpl service;

    @Mock
    private TradeOrderUpdateService tradeOrderUpdateService;
    @Mock
    private TradeOrderQueryService tradeOrderQueryService;
    @Mock
    private DeliveryExpressService deliveryExpressService;
    @Mock
    private AfterSaleMapper tradeAfterSaleMapper;
    @Mock
    private PayRefundApi payRefundApi;
    @Mock
    private CombinationRecordApi combinationRecordApi;
    @Mock
    private AfterSaleRefundDecisionService afterSaleRefundDecisionService;
    @Mock
    private TradeServiceOrderMapper tradeServiceOrderMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tradeOrderProperties", new TradeOrderProperties());
        ReflectionTestUtils.setField(service, "tradeNoRedisDAO", new TradeNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "_AUTO_001";
            }
        });
    }

    @Test
    void shouldRejectRefundPriceWhenBundleChildCapExceeded() {
        Long userId = 100L;
        TradeOrderItemDO orderItem = buildOrderItem(110L, 5000,
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"fulfilled\":true,\"refundCapPrice\":0}," +
                        "{\"childCode\":\"B\",\"fulfilled\":false,\"refundCapPrice\":3000}]}");
        when(tradeOrderQueryService.getOrderItem(userId, 110L)).thenReturn(orderItem);

        ServiceException exception = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "validateOrderItemApplicable", userId, buildCreateReq(orderItem.getId(), 3500)));

        assertEquals(AFTER_SALE_CREATE_FAIL_REFUND_PRICE_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldAllowCreateAfterSaleForNonBundleSnapshot() {
        Long userId = 101L;
        TradeOrderItemDO orderItem = buildOrderItem(111L, 5000, null);
        TradeOrderDO order = buildOrder(userId, orderItem.getOrderId());
        when(tradeOrderQueryService.getOrderItem(userId, 111L)).thenReturn(orderItem);
        when(tradeOrderQueryService.getOrder(userId, orderItem.getOrderId())).thenReturn(order);

        TradeOrderItemDO result = ReflectionTestUtils.invokeMethod(
                service, "validateOrderItemApplicable", userId, buildCreateReq(orderItem.getId(), 4000));

        assertSame(orderItem, result);
    }

    @Test
    void shouldFallbackToOrderItemPayPriceWhenBundleSnapshotMalformed() {
        Long userId = 102L;
        TradeOrderItemDO orderItem = buildOrderItem(112L, 5000, "{\"bundleChildren\":[");
        TradeOrderDO order = buildOrder(userId, orderItem.getOrderId());
        when(tradeOrderQueryService.getOrderItem(userId, 112L)).thenReturn(orderItem);
        when(tradeOrderQueryService.getOrder(userId, orderItem.getOrderId())).thenReturn(order);

        TradeOrderItemDO result = ReflectionTestUtils.invokeMethod(
                service, "validateOrderItemApplicable", userId, buildCreateReq(orderItem.getId(), 5000));

        assertSame(orderItem, result);
    }

    @Test
    void shouldUseChildFulfillmentCapWhenExplicitBundlePriceExists() {
        Long userId = 1021L;
        TradeOrderItemDO orderItem = buildOrderItem(1121L, 5000,
                "{\"bundleRefundablePrice\":5000,\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":1200,\"fulfilled\":false}," +
                        "{\"childCode\":\"B\",\"refundCapPrice\":1800,\"fulfilled\":true}]}");
        when(tradeOrderQueryService.getOrderItem(userId, 1121L)).thenReturn(orderItem);

        ServiceException exception = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "validateOrderItemApplicable", userId, buildCreateReq(orderItem.getId(), 1500)));

        assertEquals(AFTER_SALE_CREATE_FAIL_REFUND_PRICE_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectRefundWhenServiceOrderFinishedAndBundleExists() {
        Long userId = 103L;
        TradeOrderItemDO orderItem = buildOrderItem(113L, 5000,
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":3000,\"fulfilled\":false}]}");
        when(tradeOrderQueryService.getOrderItem(userId, 113L)).thenReturn(orderItem);
        when(tradeServiceOrderMapper.selectByOrderItemId(orderItem.getId())).thenReturn(TradeServiceOrderDO.builder()
                .id(70001L)
                .orderItemId(orderItem.getId())
                .status(TradeServiceOrderStatusEnum.FINISHED.getStatus())
                .orderItemSnapshotJson("{\"bundleRefundSnapshotJson\":\"{\\\"bundleChildren\\\":[{\\\"childCode\\\":\\\"A\\\",\\\"refundCapPrice\\\":3000,\\\"fulfilled\\\":false}]}\"}")
                .build());

        ServiceException exception = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "validateOrderItemApplicable", userId, buildCreateReq(orderItem.getId(), 1)));

        assertEquals(AFTER_SALE_CREATE_FAIL_REFUND_PRICE_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectRefundWhenServiceOrderFinishedWithoutSnapshot() {
        Long userId = 1031L;
        TradeOrderItemDO orderItem = buildOrderItem(1131L, 5000, null);
        when(tradeOrderQueryService.getOrderItem(userId, 1131L)).thenReturn(orderItem);
        when(tradeServiceOrderMapper.selectByOrderItemId(orderItem.getId())).thenReturn(TradeServiceOrderDO.builder()
                .id(70011L)
                .orderItemId(orderItem.getId())
                .status(TradeServiceOrderStatusEnum.FINISHED.getStatus())
                .orderItemSnapshotJson(null)
                .build());

        ServiceException exception = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "validateOrderItemApplicable", userId, buildCreateReq(orderItem.getId(), 1)));

        assertEquals(AFTER_SALE_CREATE_FAIL_REFUND_PRICE_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldAllowRefundWhenServiceOrderBookedAndSnapshotMissing() {
        Long userId = 1032L;
        TradeOrderItemDO orderItem = buildOrderItem(1132L, 5000, null);
        TradeOrderDO order = buildOrder(userId, orderItem.getOrderId());
        when(tradeOrderQueryService.getOrderItem(userId, 1132L)).thenReturn(orderItem);
        when(tradeOrderQueryService.getOrder(userId, orderItem.getOrderId())).thenReturn(order);
        when(tradeServiceOrderMapper.selectByOrderItemId(orderItem.getId())).thenReturn(TradeServiceOrderDO.builder()
                .id(70012L)
                .orderItemId(orderItem.getId())
                .status(TradeServiceOrderStatusEnum.BOOKED.getStatus())
                .orderItemSnapshotJson(null)
                .build());

        TradeOrderItemDO result = ReflectionTestUtils.invokeMethod(
                service, "validateOrderItemApplicable", userId, buildCreateReq(orderItem.getId(), 3000));

        assertSame(orderItem, result);
    }

    @Test
    void shouldPersistRefundLimitAuditWhenCreateAfterSale() {
        Long userId = 104L;
        TradeOrderItemDO orderItem = buildOrderItem(114L, 5000,
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":4800,\"fulfilled\":false}]}");
        orderItem.setUserId(userId);
        TradeOrderDO order = buildOrder(userId, orderItem.getOrderId());
        when(tradeOrderQueryService.getOrderItem(userId, 114L)).thenReturn(orderItem);
        when(tradeOrderQueryService.getOrder(userId, orderItem.getOrderId())).thenReturn(order);
        when(tradeServiceOrderMapper.selectByOrderItemId(orderItem.getId())).thenReturn(TradeServiceOrderDO.builder()
                .id(70002L)
                .orderItemId(orderItem.getId())
                .status(TradeServiceOrderStatusEnum.BOOKED.getStatus())
                .orderItemSnapshotJson("{\"bundleRefundSnapshotJson\":\"{\\\"bundleChildren\\\":[{\\\"childCode\\\":\\\"A\\\",\\\"refundCapPrice\\\":2600,\\\"fulfilled\\\":false}]}\"}")
                .build());
        when(tradeAfterSaleMapper.insert(any(cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO.class))).thenAnswer(invocation -> {
            cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO target = invocation.getArgument(0);
            target.setId(88001L);
            return 1;
        });

        Long afterSaleId = service.createAfterSale(userId, buildCreateReq(orderItem.getId(), 2500));

        assertEquals(88001L, afterSaleId);
        ArgumentCaptor<cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO.class);
        org.mockito.Mockito.verify(tradeAfterSaleMapper).insert(captor.capture());
        assertEquals("SERVICE_ORDER_SNAPSHOT", captor.getValue().getRefundLimitSource());
        String detailJson = captor.getValue().getRefundLimitDetailJson();
        assertTrue(detailJson.contains("serviceOrderId"));
        assertTrue(detailJson.contains("upperBound"));
        assertTrue(detailJson.contains("bundleChildren"));
        assertTrue(detailJson.contains("childCode"));
        assertTrue(JsonUtils.parseTree(detailJson).path("upperBound").asInt() > 0);
    }

    private static TradeOrderItemDO buildOrderItem(Long orderItemId, Integer payPrice, String priceSourceSnapshotJson) {
        TradeOrderItemDO orderItem = new TradeOrderItemDO();
        orderItem.setId(orderItemId);
        orderItem.setOrderId(7001L + orderItemId);
        orderItem.setUserId(1000L + orderItemId);
        orderItem.setPayPrice(payPrice);
        orderItem.setSpuName("测试套餐");
        orderItem.setAfterSaleStatus(TradeOrderItemAfterSaleStatusEnum.NONE.getStatus());
        orderItem.setPriceSourceSnapshotJson(priceSourceSnapshotJson);
        return orderItem;
    }

    private static TradeOrderDO buildOrder(Long userId, Long orderId) {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(orderId);
        order.setUserId(userId);
        order.setNo("ORDER_" + orderId);
        order.setStatus(TradeOrderStatusEnum.DELIVERED.getStatus());
        return order;
    }

    private static AppAfterSaleCreateReqVO buildCreateReq(Long orderItemId, Integer refundPrice) {
        AppAfterSaleCreateReqVO reqVO = new AppAfterSaleCreateReqVO();
        reqVO.setOrderItemId(orderItemId);
        reqVO.setWay(AfterSaleWayEnum.REFUND.getWay());
        reqVO.setRefundPrice(refundPrice);
        reqVO.setApplyReason("退款");
        return reqVO;
    }
}
