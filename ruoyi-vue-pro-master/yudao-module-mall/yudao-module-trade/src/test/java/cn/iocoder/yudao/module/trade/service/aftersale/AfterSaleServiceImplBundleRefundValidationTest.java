package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.pay.api.refund.dto.PayRefundRespDTO;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;
import cn.iocoder.yudao.module.promotion.api.combination.CombinationRecordApi;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleCreateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemBundleChildDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderLogDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderItemBundleChildMapper;
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

import java.util.Objects;

import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_CREATE_FAIL_REFUND_PRICE_ERROR;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_REFUND_FAIL_REFUND_LIMIT_CHANGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    @Mock
    private TradeOrderItemBundleChildMapper tradeOrderItemBundleChildMapper;

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
    void shouldPreferOrderItemBundleSnapshotWhenServiceSnapshotAbsent() {
        Long userId = 1022L;
        TradeOrderItemDO orderItem = buildOrderItem(1122L, 5000,
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":4800,\"fulfilled\":false}]}");
        orderItem.setBundleItemSnapshotJson(
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":1600,\"fulfilled\":false}]}");
        when(tradeOrderQueryService.getOrderItem(userId, 1122L)).thenReturn(orderItem);

        ServiceException exception = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "validateOrderItemApplicable", userId, buildCreateReq(orderItem.getId(), 1700)));

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

        assertEquals(AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED.getCode(), exception.getCode());
    }

    @Test
    void shouldUseBundleItemSnapshotJsonWhenEvaluatingRefundLimit() {
        Long userId = 10301L;
        TradeOrderItemDO orderItem = buildOrderItem(11301L, 5000, null);
        when(tradeOrderQueryService.getOrderItem(userId, 11301L)).thenReturn(orderItem);
        when(tradeServiceOrderMapper.selectByOrderItemId(orderItem.getId())).thenReturn(TradeServiceOrderDO.builder()
                .id(7000101L)
                .orderItemId(orderItem.getId())
                .status(TradeServiceOrderStatusEnum.BOOKED.getStatus())
                .orderItemSnapshotJson("{\"bundleItemSnapshotJson\":\"{\\\"bundleChildren\\\":[{\\\"childCode\\\":\\\"A\\\",\\\"refundCapPrice\\\":2600,\\\"fulfilled\\\":false}]}\"}")
                .build());

        ServiceException exception = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "validateOrderItemApplicable", userId, buildCreateReq(orderItem.getId(), 3000)));

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

        assertEquals(AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED.getCode(), exception.getCode());
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
        assertEquals("FALLBACK_SNAPSHOT", captor.getValue().getRefundLimitSource());
        String detailJson = captor.getValue().getRefundLimitDetailJson();
        assertTrue(detailJson.contains("serviceOrderId"));
        assertTrue(detailJson.contains("upperBound"));
        assertTrue(detailJson.contains("bundleChildren"));
        assertTrue(detailJson.contains("childCode"));
        assertTrue(JsonUtils.parseTree(detailJson).path("upperBound").asInt() > 0);
    }

    @Test
    void shouldPersistOrderItemBundleSnapshotFieldWhenNoServiceOrder() {
        Long userId = 1041L;
        TradeOrderItemDO orderItem = buildOrderItem(1141L, 5000,
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":4800,\"fulfilled\":false}]}");
        orderItem.setBundleItemSnapshotJson(
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":1600,\"fulfilled\":false}]}");
        orderItem.setUserId(userId);
        TradeOrderDO order = buildOrder(userId, orderItem.getOrderId());
        when(tradeOrderQueryService.getOrderItem(userId, 1141L)).thenReturn(orderItem);
        when(tradeOrderQueryService.getOrder(userId, orderItem.getOrderId())).thenReturn(order);
        when(tradeAfterSaleMapper.insert(any(cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO.class))).thenAnswer(invocation -> {
            cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO target = invocation.getArgument(0);
            target.setId(88011L);
            return 1;
        });

        Long afterSaleId = service.createAfterSale(userId, buildCreateReq(orderItem.getId(), 1500));

        assertEquals(88011L, afterSaleId);
        ArgumentCaptor<cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO.class);
        verify(tradeAfterSaleMapper).insert(captor.capture());
        assertEquals("FALLBACK_SNAPSHOT", captor.getValue().getRefundLimitSource());
        assertEquals("bundleItemSnapshotJson",
                JsonUtils.parseTree(captor.getValue().getRefundLimitDetailJson()).path("snapshotField").asText());
        assertEquals(1600, JsonUtils.parseTree(captor.getValue().getRefundLimitDetailJson()).path("upperBound").asInt());
    }

    @Test
    void shouldRejectRefundExecutionWhenServiceOrderFinishedAfterApply() {
        Long orderItemId = 120L;
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setId(99001L);
        afterSale.setUserId(100L);
        afterSale.setOrderItemId(orderItemId);
        afterSale.setStatus(cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum.WAIT_REFUND.getStatus());
        afterSale.setRefundPrice(100);
        afterSale.setSpuName("测试套餐");

        TradeOrderItemDO orderItem = buildOrderItem(orderItemId, 5000,
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":3000,\"fulfilled\":false}]}");
        when(tradeAfterSaleMapper.selectById(99001L)).thenReturn(afterSale);
        when(tradeOrderQueryService.getOrderItem(afterSale.getUserId(), orderItemId)).thenReturn(orderItem);
        when(tradeServiceOrderMapper.selectByOrderItemId(orderItemId)).thenReturn(TradeServiceOrderDO.builder()
                .id(88001L)
                .orderItemId(orderItemId)
                .status(TradeServiceOrderStatusEnum.FINISHED.getStatus())
                .orderItemSnapshotJson("{\"bundleRefundSnapshotJson\":\"{\\\"bundleChildren\\\":[{\\\"childCode\\\":\\\"A\\\",\\\"refundCapPrice\\\":3000,\\\"fulfilled\\\":false}]}\"}")
                .build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.refundAfterSale(1L, "127.0.0.1", 99001L));
        assertEquals(AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED.getCode(), exception.getCode());
        verify(payRefundApi, never()).createRefund(any());
        verify(afterSaleRefundDecisionService).auditDecision(
                eq(TradeOrderLogDO.USER_ID_SYSTEM),
                eq(TradeOrderLogDO.USER_TYPE_SYSTEM),
                eq(afterSale),
                argThat(decision -> decision != null
                        && Boolean.FALSE.equals(decision.getAutoPass())
                        && "BUNDLE_CHILD_FULFILLED".equals(decision.getRuleCode())),
                eq(false));
    }

    @Test
    void shouldRefreshRefundLimitAuditBeforeRefundExecution() {
        Long orderItemId = 121L;
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setId(99002L);
        afterSale.setUserId(101L);
        afterSale.setOrderItemId(orderItemId);
        afterSale.setStatus(cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum.WAIT_REFUND.getStatus());
        afterSale.setRefundPrice(2000);
        afterSale.setSpuName("测试套餐");

        TradeOrderItemDO orderItem = buildOrderItem(orderItemId, 5000,
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":2600,\"fulfilled\":false}]}");
        when(tradeAfterSaleMapper.selectById(99002L)).thenReturn(afterSale);
        when(tradeOrderQueryService.getOrderItem(afterSale.getUserId(), orderItemId)).thenReturn(orderItem);
        when(tradeServiceOrderMapper.selectByOrderItemId(orderItemId)).thenReturn(TradeServiceOrderDO.builder()
                .id(88002L)
                .orderItemId(orderItemId)
                .status(TradeServiceOrderStatusEnum.BOOKED.getStatus())
                .orderItemSnapshotJson("{\"bundleRefundSnapshotJson\":\"{\\\"bundleChildren\\\":[{\\\"childCode\\\":\\\"A\\\",\\\"refundCapPrice\\\":2600,\\\"fulfilled\\\":false}]}\"}")
                .build());
        when(payRefundApi.createRefund(any())).thenReturn(556677L);

        service.refundAfterSale(1L, "127.0.0.1", 99002L);

        verify(tradeAfterSaleMapper).updateById(org.mockito.ArgumentMatchers.<AfterSaleDO>argThat(updated ->
                Objects.equals(updated.getId(), 99002L)
                        && "FALLBACK_SNAPSHOT".equals(updated.getRefundLimitSource())
                        && updated.getRefundLimitDetailJson() != null
                        && updated.getRefundLimitDetailJson().contains("upperBound")));
    }

    @Test
    void shouldPersistChildLedgerSourceWhenCreateAfterSale() {
        Long userId = 1042L;
        TradeOrderItemDO orderItem = buildOrderItem(1142L, 5000,
                "{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":4800,\"fulfilled\":false}]}");
        orderItem.setUserId(userId);
        TradeOrderDO order = buildOrder(userId, orderItem.getOrderId());
        when(tradeOrderQueryService.getOrderItem(userId, 1142L)).thenReturn(orderItem);
        when(tradeOrderQueryService.getOrder(userId, orderItem.getOrderId())).thenReturn(order);
        when(tradeOrderItemBundleChildMapper.selectListByOrderItemId(orderItem.getId())).thenReturn(java.util.Arrays.asList(
                buildBundleChild(1L, orderItem.getId(), "A", 1800, 200, TradeServiceOrderStatusEnum.BOOKED.getStatus()),
                buildBundleChild(2L, orderItem.getId(), "B", 1500, 0, TradeServiceOrderStatusEnum.FINISHED.getStatus())
        ));
        when(tradeAfterSaleMapper.insert(any(AfterSaleDO.class))).thenAnswer(invocation -> {
            AfterSaleDO target = invocation.getArgument(0);
            target.setId(88012L);
            return 1;
        });

        Long afterSaleId = service.createAfterSale(userId, buildCreateReq(orderItem.getId(), 1500));

        assertEquals(88012L, afterSaleId);
        ArgumentCaptor<AfterSaleDO> captor = ArgumentCaptor.forClass(AfterSaleDO.class);
        verify(tradeAfterSaleMapper).insert(captor.capture());
        assertEquals("CHILD_LEDGER", captor.getValue().getRefundLimitSource());
        String detailJson = captor.getValue().getRefundLimitDetailJson();
        assertTrue(detailJson.contains("ledgerRecordCount"));
        assertTrue(detailJson.contains("bundleChildren"));
        assertTrue(detailJson.contains("blockedByFinishedChild"));
    }

    @Test
    void shouldDistributeRefundedPriceToChildLedgerWhenRefundSuccess() {
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setId(99003L);
        afterSale.setUserId(101L);
        afterSale.setOrderItemId(122L);
        afterSale.setStatus(cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum.WAIT_REFUND.getStatus());
        afterSale.setRefundPrice(1200);
        afterSale.setSpuName("测试套餐");

        when(tradeAfterSaleMapper.selectById(99003L)).thenReturn(afterSale);
        when(tradeAfterSaleMapper.updateByIdAndStatus(eq(99003L),
                eq(cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum.WAIT_REFUND.getStatus()),
                any(AfterSaleDO.class))).thenReturn(1);
        when(payRefundApi.getRefund(556688L)).thenReturn(new PayRefundRespDTO()
                .setId(556688L)
                .setStatus(PayRefundStatusEnum.SUCCESS.getStatus())
                .setRefundPrice(1200)
                .setMerchantRefundId("99003"));
        when(tradeOrderItemBundleChildMapper.selectListByOrderItemId(122L)).thenReturn(java.util.Arrays.asList(
                buildBundleChild(11L, 122L, "A", 1000, 100, TradeServiceOrderStatusEnum.BOOKED.getStatus()),
                buildBundleChild(12L, 122L, "B", 500, 100, TradeServiceOrderStatusEnum.BOOKED.getStatus())
        ));

        service.updateAfterSaleRefunded(99003L, 70001L, 556688L);

        verify(tradeOrderItemBundleChildMapper, times(2))
                .updateById(org.mockito.ArgumentMatchers.<TradeOrderItemBundleChildDO>any());
        verify(tradeOrderItemBundleChildMapper).updateById(
                org.mockito.ArgumentMatchers.<TradeOrderItemBundleChildDO>argThat(updated ->
                Objects.equals(updated.getId(), 11L) && Objects.equals(updated.getRefundedPrice(), 1000)));
        verify(tradeOrderItemBundleChildMapper).updateById(
                org.mockito.ArgumentMatchers.<TradeOrderItemBundleChildDO>argThat(updated ->
                Objects.equals(updated.getId(), 12L) && Objects.equals(updated.getRefundedPrice(), 400)));
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

    private static TradeOrderItemBundleChildDO buildBundleChild(Long id, Long orderItemId, String childCode,
                                                                Integer payPrice, Integer refundedPrice,
                                                                Integer fulfillmentStatus) {
        return TradeOrderItemBundleChildDO.builder()
                .id(id)
                .orderId(8800L + orderItemId)
                .orderItemId(orderItemId)
                .childCode(childCode)
                .skuName("子项" + childCode)
                .quantity(1)
                .payPrice(payPrice)
                .refundedPrice(refundedPrice)
                .fulfillmentStatus(fulfillmentStatus)
                .build();
    }
}
