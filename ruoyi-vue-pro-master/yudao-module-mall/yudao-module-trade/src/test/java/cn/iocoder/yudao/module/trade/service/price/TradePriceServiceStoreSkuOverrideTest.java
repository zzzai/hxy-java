package cn.iocoder.yudao.module.trade.service.price;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.product.api.store.ProductStoreSkuApi;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.template.ProductTemplateVersionApi;
import cn.iocoder.yudao.module.product.api.template.dto.ProductTemplateVersionRespDTO;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.product.enums.template.ProductTemplateConstants;
import cn.iocoder.yudao.module.promotion.enums.common.PromotionTypeEnum;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateReqBO;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateRespBO;
import cn.iocoder.yudao.module.trade.service.price.calculator.TradePriceCalculator;
import cn.iocoder.yudao.module.trade.service.price.calculator.TradePriceCalculatorHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SKU_STOCK_NOT_ENOUGH;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.PRICE_CALCULATE_SERVICE_ITEM_EXPRESS_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class TradePriceServiceStoreSkuOverrideTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradePriceServiceImpl tradePriceService;

    @Mock
    private ProductSkuApi productSkuApi;
    @Mock
    private ProductSpuApi productSpuApi;
    @Mock
    private ProductStoreSkuApi productStoreSkuApi;
    @Mock
    private ProductTemplateVersionApi productTemplateVersionApi;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tradePriceService, "priceCalculators", Collections.emptyList());
    }

    @Test
    void calculateOrderPrice_shouldUseStorePriceWhenPickupOrder() {
        when(productSkuApi.getSkuList(asSet(22L))).thenReturn(Collections.singletonList(
                new ProductSkuRespDTO().setId(22L).setSpuId(1001L).setPrice(1000).setMarketPrice(1200).setStock(99)
        ));
        when(productSpuApi.validateSpuList(asSet(1001L))).thenReturn(Collections.singletonList(
                new ProductSpuRespDTO().setId(1001L).setName("足疗服务").setGiveIntegral(0)
                        .setProductType(ProductTypeEnum.PHYSICAL.getType())
        ));
        ProductStoreSkuRespDTO storeSku = new ProductStoreSkuRespDTO();
        storeSku.setSkuId(22L);
        storeSku.setSaleStatus(0);
        storeSku.setSalePrice(880);
        storeSku.setMarketPrice(1000);
        storeSku.setStock(5);
        when(productStoreSkuApi.getStoreSkuMap(11L, asSet(22L))).thenReturn(Collections.singletonMap(22L, storeSku));

        TradePriceCalculateReqBO reqBO = new TradePriceCalculateReqBO();
        reqBO.setUserId(1L);
        reqBO.setPointStatus(false);
        reqBO.setDeliveryType(DeliveryTypeEnum.PICK_UP.getType());
        reqBO.setPickUpStoreId(11L);
        reqBO.setItems(Collections.singletonList(
                new TradePriceCalculateReqBO.Item().setSkuId(22L).setCount(2).setSelected(true)
                        .setAddonType(1)
                        .setAddonSnapshotJson("{\"addonCode\":\"ADD_ON_EXTEND_30M\"}")
        ));

        TradePriceCalculateRespBO respBO = tradePriceService.calculateOrderPrice(reqBO);

        assertEquals(1760, respBO.getPrice().getTotalPrice());
        assertEquals(1760, respBO.getPrice().getPayPrice());
        assertEquals(880, respBO.getItems().get(0).getPrice());
        assertEquals(1, respBO.getItems().get(0).getAddonType());
        assertEquals("{\"addonCode\":\"ADD_ON_EXTEND_30M\"}", respBO.getItems().get(0).getAddonSnapshotJson());
        JsonNode snapshot = JsonUtils.parseTree(respBO.getItems().get(0).getPriceSourceSnapshotJson());
        assertEquals("hxy-price-source-v2", snapshot.path("version").asText());
        assertEquals("STORE_SKU_OVERRIDE", snapshot.path("baseSource").asText());
        assertEquals(1000, snapshot.path("headquarterPrice").asInt());
        assertEquals(880, snapshot.path("storePrice").asInt());
        assertEquals(880, snapshot.path("basePrice").asInt());
        assertEquals(11L, snapshot.path("pickUpStoreId").asLong());
        assertEquals(0, snapshot.path("activityDiscountPrice").asInt());
        assertEquals(1760, snapshot.path("payPrice").asInt());
    }

    @Test
    void calculateOrderPrice_shouldPreserveExistingPriceSourcePayloadAndInjectDecisionFields() {
        when(productSkuApi.getSkuList(asSet(44L))).thenReturn(Collections.singletonList(
                new ProductSkuRespDTO().setId(44L).setSpuId(1002L).setPrice(960).setMarketPrice(1200).setStock(99)
        ));
        when(productSpuApi.validateSpuList(asSet(1002L))).thenReturn(Collections.singletonList(
                new ProductSpuRespDTO().setId(1002L).setName("零售商品").setGiveIntegral(0)
                        .setProductType(ProductTypeEnum.PHYSICAL.getType())
        ));
        ProductStoreSkuRespDTO storeSku = new ProductStoreSkuRespDTO();
        storeSku.setSkuId(44L);
        storeSku.setSaleStatus(0);
        storeSku.setSalePrice(900);
        storeSku.setStock(8);
        when(productStoreSkuApi.getStoreSkuMap(12L, asSet(44L))).thenReturn(Collections.singletonMap(44L, storeSku));

        TradePriceCalculateReqBO reqBO = new TradePriceCalculateReqBO();
        reqBO.setUserId(1L);
        reqBO.setPointStatus(false);
        reqBO.setDeliveryType(DeliveryTypeEnum.PICK_UP.getType());
        reqBO.setPickUpStoreId(12L);
        reqBO.setItems(Collections.singletonList(
                new TradePriceCalculateReqBO.Item().setSkuId(44L).setCount(1).setSelected(true)
                        .setPriceSourceSnapshotJson("{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":500}]}")
        ));

        TradePriceCalculateRespBO respBO = tradePriceService.calculateOrderPrice(reqBO);
        JsonNode snapshot = JsonUtils.parseTree(respBO.getItems().get(0).getPriceSourceSnapshotJson());
        assertTrue(snapshot.path("bundleChildren").isArray());
        assertEquals("STORE_SKU_OVERRIDE", snapshot.path("baseSource").asText());
        assertEquals(900, snapshot.path("basePrice").asInt());
    }

    @Test
    void calculateOrderPrice_shouldRecordActivityAndBenefitInSnapshot() {
        ReflectionTestUtils.setField(tradePriceService, "priceCalculators", Collections.singletonList((TradePriceCalculator) (req, resp) -> {
            TradePriceCalculateRespBO.OrderItem orderItem = resp.getItems().get(0);
            orderItem.setDiscountPrice(120);
            orderItem.setCouponPrice(30);
            orderItem.setPointPrice(20);
            orderItem.setVipPrice(10);
            TradePriceCalculatorHelper.recountPayPrice(orderItem);
            TradePriceCalculatorHelper.recountAllPrice(resp);
            TradePriceCalculatorHelper.addPromotion(resp, orderItem, 9001L, "门店活动",
                    PromotionTypeEnum.DISCOUNT_ACTIVITY.getType(), "活动减免", 120);
        }));
        when(productSkuApi.getSkuList(asSet(55L))).thenReturn(Collections.singletonList(
                new ProductSkuRespDTO().setId(55L).setSpuId(1003L).setPrice(1000).setStock(99)
        ));
        when(productSpuApi.validateSpuList(asSet(1003L))).thenReturn(Collections.singletonList(
                new ProductSpuRespDTO().setId(1003L).setName("零售商品").setGiveIntegral(0)
                        .setProductType(ProductTypeEnum.PHYSICAL.getType())
        ));

        TradePriceCalculateReqBO reqBO = new TradePriceCalculateReqBO();
        reqBO.setUserId(1L);
        reqBO.setPointStatus(false);
        reqBO.setDeliveryType(DeliveryTypeEnum.EXPRESS.getType());
        reqBO.setItems(Collections.singletonList(
                new TradePriceCalculateReqBO.Item().setSkuId(55L).setCount(1).setSelected(true)
        ));

        TradePriceCalculateRespBO respBO = tradePriceService.calculateOrderPrice(reqBO);
        JsonNode snapshot = JsonUtils.parseTree(respBO.getItems().get(0).getPriceSourceSnapshotJson());
        assertEquals("HEADQUARTER", snapshot.path("baseSource").asText());
        assertEquals(120, snapshot.path("activityDiscountPrice").asInt());
        assertEquals(30, snapshot.path("couponPrice").asInt());
        assertEquals(20, snapshot.path("pointPrice").asInt());
        assertEquals(10, snapshot.path("vipPrice").asInt());
        assertEquals(820, snapshot.path("payPrice").asInt());
        assertEquals(1, snapshot.path("promotionHits").size());
        assertEquals(9001L, snapshot.path("promotionHits").get(0).path("promotionId").asLong());
    }

    @Test
    void calculateOrderPrice_shouldThrowWhenStoreStockNotEnough() {
        when(productSkuApi.getSkuList(asSet(22L))).thenReturn(Collections.singletonList(
                new ProductSkuRespDTO().setId(22L).setSpuId(1001L).setPrice(1000).setMarketPrice(1200).setStock(99)
        ));
        when(productSpuApi.validateSpuList(asSet(1001L))).thenReturn(Collections.singletonList(
                new ProductSpuRespDTO().setId(1001L).setName("零售商品").setGiveIntegral(0)
                        .setProductType(ProductTypeEnum.PHYSICAL.getType())
        ));
        ProductStoreSkuRespDTO storeSku = new ProductStoreSkuRespDTO();
        storeSku.setSkuId(22L);
        storeSku.setSaleStatus(0);
        storeSku.setSalePrice(880);
        storeSku.setMarketPrice(1000);
        storeSku.setStock(5);
        when(productStoreSkuApi.getStoreSkuMap(11L, asSet(22L))).thenReturn(Collections.singletonMap(22L, storeSku));

        TradePriceCalculateReqBO reqBO = new TradePriceCalculateReqBO();
        reqBO.setUserId(1L);
        reqBO.setPointStatus(false);
        reqBO.setDeliveryType(DeliveryTypeEnum.PICK_UP.getType());
        reqBO.setPickUpStoreId(11L);
        reqBO.setItems(Collections.singletonList(
                new TradePriceCalculateReqBO.Item().setSkuId(22L).setCount(6).setSelected(true)
        ));

        ServiceException ex = assertThrows(ServiceException.class, () -> tradePriceService.calculateOrderPrice(reqBO));
        assertEquals(SKU_STOCK_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    void calculateOrderPrice_shouldSkipStockCheckForServiceProduct() {
        when(productSkuApi.getSkuList(asSet(22L))).thenReturn(Collections.singletonList(
                new ProductSkuRespDTO().setId(22L).setSpuId(1001L).setPrice(1000).setMarketPrice(1200).setStock(0)
        ));
        when(productSpuApi.validateSpuList(asSet(1001L))).thenReturn(Collections.singletonList(
                new ProductSpuRespDTO().setId(1001L).setName("服务商品").setGiveIntegral(0)
                        .setCategoryId(8L)
                        .setProductType(ProductTypeEnum.SERVICE.getType())
        ));
        when(productTemplateVersionApi.getTemplateVersionMap(asSet(701L))).thenReturn(Collections.singletonMap(701L,
                new ProductTemplateVersionRespDTO()
                        .setId(701L)
                        .setCategoryId(8L)
                        .setStatus(ProductTemplateConstants.TEMPLATE_STATUS_PUBLISHED)));
        ProductStoreSkuRespDTO storeSku = new ProductStoreSkuRespDTO();
        storeSku.setSkuId(22L);
        storeSku.setSaleStatus(0);
        storeSku.setSalePrice(880);
        storeSku.setMarketPrice(1000);
        storeSku.setStock(0);
        when(productStoreSkuApi.getStoreSkuMap(11L, asSet(22L))).thenReturn(Collections.singletonMap(22L, storeSku));

        TradePriceCalculateReqBO reqBO = new TradePriceCalculateReqBO();
        reqBO.setUserId(1L);
        reqBO.setPointStatus(false);
        reqBO.setDeliveryType(DeliveryTypeEnum.PICK_UP.getType());
        reqBO.setPickUpStoreId(11L);
        reqBO.setItems(Collections.singletonList(
                new TradePriceCalculateReqBO.Item().setSkuId(22L).setCount(6).setSelected(true)
                        .setTemplateVersionId(701L)
                        .setTemplateSnapshotJson("{\"version\":\"v1\"}")
        ));

        TradePriceCalculateRespBO respBO = tradePriceService.calculateOrderPrice(reqBO);
        assertEquals(5280, respBO.getPrice().getPayPrice());
    }

    @Test
    void calculateOrderPrice_shouldRejectServiceItemWhenExpressDelivery() {
        when(productSkuApi.getSkuList(asSet(22L))).thenReturn(Collections.singletonList(
                new ProductSkuRespDTO().setId(22L).setSpuId(1001L).setPrice(1000).setStock(99)
        ));
        when(productSpuApi.validateSpuList(asSet(1001L))).thenReturn(Collections.singletonList(
                new ProductSpuRespDTO().setId(1001L).setName("服务商品")
                        .setProductType(ProductTypeEnum.SERVICE.getType())
        ));

        TradePriceCalculateReqBO reqBO = new TradePriceCalculateReqBO();
        reqBO.setUserId(1L);
        reqBO.setPointStatus(false);
        reqBO.setDeliveryType(DeliveryTypeEnum.EXPRESS.getType());
        reqBO.setItems(Collections.singletonList(
                new TradePriceCalculateReqBO.Item().setSkuId(22L).setCount(1).setSelected(true)
        ));

        ServiceException ex = assertThrows(ServiceException.class, () -> tradePriceService.calculateOrderPrice(reqBO));
        assertEquals(PRICE_CALCULATE_SERVICE_ITEM_EXPRESS_FORBIDDEN.getCode(), ex.getCode());
    }
}
