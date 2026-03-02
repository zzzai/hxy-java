package cn.iocoder.yudao.module.trade.service.price;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.product.api.store.ProductStoreSkuApi;
import cn.iocoder.yudao.module.product.api.template.ProductTemplateVersionApi;
import cn.iocoder.yudao.module.product.api.template.dto.ProductTemplateVersionRespDTO;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.product.enums.template.ProductTemplateConstants;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateReqBO;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateRespBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.PRICE_CALCULATE_TEMPLATE_VERSION_CATEGORY_MISMATCH;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.PRICE_CALCULATE_TEMPLATE_VERSION_NOT_PUBLISHED;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.PRICE_CALCULATE_TEMPLATE_VERSION_SNAPSHOT_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class TradePriceServiceTemplateVersionValidationTest extends BaseMockitoUnitTest {

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
        when(productSkuApi.getSkuList(asSet(22L))).thenReturn(Collections.singletonList(
                new ProductSkuRespDTO().setId(22L).setSpuId(1001L).setPrice(1000).setStock(99)
        ));
        when(productSpuApi.validateSpuList(asSet(1001L))).thenReturn(Collections.singletonList(
                new ProductSpuRespDTO().setId(1001L).setName("门店零售商品")
                        .setCategoryId(8L)
                        .setGiveIntegral(0)
                        .setProductType(ProductTypeEnum.PHYSICAL.getType())
        ));
    }

    @Test
    void calculateOrderPrice_shouldRejectWhenTemplateSnapshotMissing() {
        TradePriceCalculateReqBO reqBO = buildBaseReq(701L, null);

        ServiceException ex = assertThrows(ServiceException.class, () -> tradePriceService.calculateOrderPrice(reqBO));
        assertEquals(PRICE_CALCULATE_TEMPLATE_VERSION_SNAPSHOT_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void calculateOrderPrice_shouldRejectWhenTemplateVersionNotPublished() {
        ProductTemplateVersionRespDTO draftVersion = new ProductTemplateVersionRespDTO()
                .setId(701L)
                .setCategoryId(8L)
                .setStatus(ProductTemplateConstants.TEMPLATE_STATUS_DRAFT);
        when(productTemplateVersionApi.getTemplateVersionMap(asSet(701L)))
                .thenReturn(Collections.singletonMap(701L, draftVersion));

        TradePriceCalculateReqBO reqBO = buildBaseReq(701L, "{\"version\":\"v1\"}");

        ServiceException ex = assertThrows(ServiceException.class, () -> tradePriceService.calculateOrderPrice(reqBO));
        assertEquals(PRICE_CALCULATE_TEMPLATE_VERSION_NOT_PUBLISHED.getCode(), ex.getCode());
    }

    @Test
    void calculateOrderPrice_shouldRejectWhenTemplateCategoryMismatch() {
        ProductTemplateVersionRespDTO publishedVersion = new ProductTemplateVersionRespDTO()
                .setId(701L)
                .setCategoryId(9L)
                .setStatus(ProductTemplateConstants.TEMPLATE_STATUS_PUBLISHED);
        when(productTemplateVersionApi.getTemplateVersionMap(asSet(701L)))
                .thenReturn(Collections.singletonMap(701L, publishedVersion));

        TradePriceCalculateReqBO reqBO = buildBaseReq(701L, "{\"version\":\"v1\"}");

        ServiceException ex = assertThrows(ServiceException.class, () -> tradePriceService.calculateOrderPrice(reqBO));
        assertEquals(PRICE_CALCULATE_TEMPLATE_VERSION_CATEGORY_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    void calculateOrderPrice_shouldPassWhenTemplatePublishedAndCategoryMatches() {
        ProductTemplateVersionRespDTO publishedVersion = new ProductTemplateVersionRespDTO()
                .setId(701L)
                .setCategoryId(8L)
                .setStatus(ProductTemplateConstants.TEMPLATE_STATUS_PUBLISHED);
        when(productTemplateVersionApi.getTemplateVersionMap(asSet(701L)))
                .thenReturn(Collections.singletonMap(701L, publishedVersion));

        TradePriceCalculateReqBO reqBO = buildBaseReq(701L, "{\"version\":\"v1\"}");
        TradePriceCalculateRespBO respBO = tradePriceService.calculateOrderPrice(reqBO);

        assertEquals(1000, respBO.getPrice().getPayPrice());
    }

    private static TradePriceCalculateReqBO buildBaseReq(Long templateVersionId, String templateSnapshotJson) {
        TradePriceCalculateReqBO reqBO = new TradePriceCalculateReqBO();
        reqBO.setUserId(1L);
        reqBO.setPointStatus(false);
        reqBO.setDeliveryType(DeliveryTypeEnum.EXPRESS.getType());
        reqBO.setItems(Collections.singletonList(new TradePriceCalculateReqBO.Item()
                .setSkuId(22L)
                .setCount(1)
                .setSelected(true)
                .setTemplateVersionId(templateVersionId)
                .setTemplateSnapshotJson(templateSnapshotJson)));
        return reqBO;
    }
}
