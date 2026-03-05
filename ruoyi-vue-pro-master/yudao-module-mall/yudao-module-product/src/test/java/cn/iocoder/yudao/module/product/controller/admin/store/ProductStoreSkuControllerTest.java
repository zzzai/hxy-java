package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuStockFlowBatchRetryReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuStockFlowBatchRetryRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.sku.ProductSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuDO;
import cn.iocoder.yudao.module.product.service.store.dto.ProductStoreSkuStockFlowBatchRetryResult;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreMappingService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class ProductStoreSkuControllerTest {

    @Mock
    private ProductStoreMappingService storeMappingService;
    @Mock
    private ProductStoreService productStoreService;
    @Mock
    private ProductSpuService productSpuService;
    @Mock
    private ProductSkuService productSkuService;

    @InjectMocks
    private ProductStoreSkuController controller;

    @Test
    void page_shouldFillReadableNamesAndSpec() {
        ProductStoreSkuDO mapping = ProductStoreSkuDO.builder()
                .id(1L)
                .storeId(11L)
                .spuId(22L)
                .skuId(33L)
                .build();
        when(storeMappingService.getStoreSkuPage(any(ProductStoreSkuPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(mapping), 1L));

        ProductStoreDO store = new ProductStoreDO();
        store.setId(11L);
        store.setName("荷小悦-上海徐汇店");
        when(productStoreService.getStoreMap(Collections.singleton(11L)))
                .thenReturn(Collections.singletonMap(11L, store));

        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(22L);
        spu.setName("足疗60分钟");
        when(productSpuService.getSpuMap(Collections.singleton(22L)))
                .thenReturn(Collections.singletonMap(22L, spu));

        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(33L);
        sku.setSpuId(22L);
        sku.setProperties(Arrays.asList(new ProductSkuDO.Property(1L, "时长", 2L, "60分钟")));
        when(productSkuService.getSkuList(Collections.singleton(33L)))
                .thenReturn(Collections.singletonList(sku));

        CommonResult<PageResult<ProductStoreSkuRespVO>> result = controller.page(new ProductStoreSkuPageReqVO());

        ProductStoreSkuRespVO respVO = result.getData().getList().get(0);
        assertEquals("荷小悦-上海徐汇店", respVO.getStoreName());
        assertEquals("足疗60分钟", respVO.getSpuName());
        assertEquals("时长:60分钟", respVO.getSkuSpecText());
    }

    @Test
    void batchRetryStockFlow_shouldDelegateService() {
        ProductStoreSkuStockFlowBatchRetryReqVO reqVO = new ProductStoreSkuStockFlowBatchRetryReqVO();
        reqVO.setIds(Arrays.asList(901L, 902L, 901L));
        reqVO.setSource("admin_ui");
        ProductStoreSkuStockFlowBatchRetryResult serviceResp = ProductStoreSkuStockFlowBatchRetryResult.builder()
                .totalCount(2)
                .successCount(1)
                .skippedCount(1)
                .failedCount(0)
                .items(Collections.singletonList(ProductStoreSkuStockFlowBatchRetryResult.Item.builder()
                        .id(901L)
                        .storeId(11L)
                        .skuId(22L)
                        .resultType("SUCCESS")
                        .reason("")
                        .status(1)
                        .retryOperator("库存运营A")
                        .retrySource("ADMIN_UI")
                        .build()))
                .build();
        when(storeMappingService.retryStoreSkuStockFlowByIds(reqVO.getIds(), "库存运营A", "admin_ui"))
                .thenReturn(serviceResp);

        try (MockedStatic<cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils> mockStatic =
                     mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class)) {
            mockStatic.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserNickname)
                    .thenReturn("库存运营A");

            CommonResult<ProductStoreSkuStockFlowBatchRetryRespVO> result = controller.batchRetryStockFlow(reqVO);

            assertEquals(2, result.getData().getTotalCount());
            assertEquals(1, result.getData().getSuccessCount());
            assertEquals(1, result.getData().getSkippedCount());
            assertEquals(901L, result.getData().getItems().get(0).getId());
            assertEquals(11L, result.getData().getItems().get(0).getStoreId());
            assertEquals(22L, result.getData().getItems().get(0).getSkuId());
        }
    }
}
