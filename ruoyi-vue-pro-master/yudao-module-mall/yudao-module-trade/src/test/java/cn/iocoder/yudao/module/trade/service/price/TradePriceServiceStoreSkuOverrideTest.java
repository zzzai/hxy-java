package cn.iocoder.yudao.module.trade.service.price;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.product.api.store.ProductStoreSkuApi;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.template.ProductTemplateVersionApi;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateReqBO;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateRespBO;
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
                        .setProductType(ProductTypeEnum.SERVICE.getType())
        ));
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
