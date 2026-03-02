package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSpuDO;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreMappingService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStoreSpuControllerTest {

    @Mock
    private ProductStoreMappingService storeMappingService;
    @Mock
    private ProductStoreService productStoreService;
    @Mock
    private ProductSpuService productSpuService;

    @InjectMocks
    private ProductStoreSpuController controller;

    @Test
    void page_shouldFillStoreAndSpuNames() {
        ProductStoreSpuDO mapping = ProductStoreSpuDO.builder()
                .id(1L)
                .storeId(11L)
                .spuId(22L)
                .productType(2)
                .saleStatus(0)
                .build();
        when(storeMappingService.getStoreSpuPage(any(ProductStoreSpuPageReqVO.class)))
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

        CommonResult<PageResult<ProductStoreSpuRespVO>> result = controller.page(new ProductStoreSpuPageReqVO());

        ProductStoreSpuRespVO respVO = result.getData().getList().get(0);
        assertEquals("荷小悦-上海徐汇店", respVO.getStoreName());
        assertEquals("足疗60分钟", respVO.getSpuName());
    }
}

