package cn.iocoder.yudao.module.product.service.spu;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSkuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuSaveReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductCategoryAttrTplVersionDO;
import cn.iocoder.yudao.module.product.dal.mysql.spu.ProductSpuMapper;
import cn.iocoder.yudao.module.product.dal.mysql.template.ProductCategoryAttrTplVersionMapper;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.product.enums.template.ProductTemplateConstants;
import cn.iocoder.yudao.module.product.service.brand.ProductBrandService;
import cn.iocoder.yudao.module.product.service.category.ProductCategoryService;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SKU_TEMPLATE_VERSION_MISMATCH;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SPU_TEMPLATE_VERSION_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSpuTemplateVersionBindingTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ProductSpuServiceImpl productSpuService;

    @Mock
    private ProductSpuMapper productSpuMapper;
    @Mock
    private ProductSkuService productSkuService;
    @Mock
    private ProductBrandService brandService;
    @Mock
    private ProductCategoryService categoryService;
    @Mock
    private ProductCategoryAttrTplVersionMapper templateVersionMapper;

    @Test
    void createSpu_shouldRejectServiceProductWithoutTemplateVersion() {
        ProductSpuSaveReqVO reqVO = buildBaseServiceReq();
        reqVO.setTemplateVersionId(null);
        when(categoryService.getCategoryLevel(reqVO.getCategoryId())).thenReturn(2);

        ServiceException ex = assertThrows(ServiceException.class, () -> productSpuService.createSpu(reqVO));
        assertEquals(SPU_TEMPLATE_VERSION_REQUIRED.getCode(), ex.getCode());
        verify(productSpuMapper, never()).insert(any(ProductSpuDO.class));
    }

    @Test
    void createSpu_shouldRejectWhenSkuTemplateVersionMismatchesSpu() {
        ProductSpuSaveReqVO reqVO = buildBaseServiceReq();
        reqVO.getSkus().get(0).setTemplateVersionId(202603030002L);
        mockCategoryAndTemplate(reqVO.getCategoryId(), reqVO.getTemplateVersionId());

        ServiceException ex = assertThrows(ServiceException.class, () -> productSpuService.createSpu(reqVO));
        assertEquals(SKU_TEMPLATE_VERSION_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    void createSpu_shouldInheritSkuTemplateVersionFromSpu() {
        ProductSpuSaveReqVO reqVO = buildBaseServiceReq();
        mockCategoryAndTemplate(reqVO.getCategoryId(), reqVO.getTemplateVersionId());
        doAnswer(invocation -> {
            ProductSpuDO spu = invocation.getArgument(0);
            spu.setId(88001L);
            return 1;
        }).when(productSpuMapper).insert(any(ProductSpuDO.class));

        Long spuId = productSpuService.createSpu(reqVO);

        assertEquals(88001L, spuId);
        assertEquals(reqVO.getTemplateVersionId(), reqVO.getSkus().get(0).getTemplateVersionId());
        ArgumentCaptor<ProductSpuDO> spuCaptor = ArgumentCaptor.forClass(ProductSpuDO.class);
        verify(productSpuMapper).insert(spuCaptor.capture());
        assertEquals(reqVO.getTemplateVersionId(), spuCaptor.getValue().getTemplateVersionId());
        verify(productSkuService).createSkuList(88001L, reqVO.getTemplateVersionId(), reqVO.getSkus());
    }

    private void mockCategoryAndTemplate(Long categoryId, Long templateVersionId) {
        when(categoryService.getCategoryLevel(categoryId)).thenReturn(2);
        ProductCategoryAttrTplVersionDO templateVersion = ProductCategoryAttrTplVersionDO.builder()
                .id(templateVersionId)
                .categoryId(categoryId)
                .status(ProductTemplateConstants.TEMPLATE_STATUS_PUBLISHED)
                .build();
        when(templateVersionMapper.selectById(templateVersionId)).thenReturn(templateVersion);
    }

    private ProductSpuSaveReqVO buildBaseServiceReq() {
        ProductSkuSaveReqVO sku = new ProductSkuSaveReqVO();
        sku.setName("60分钟标准服务");
        sku.setPrice(9800);
        sku.setMarketPrice(10800);
        sku.setCostPrice(3500);
        sku.setPicUrl("https://hxy/sku.png");
        sku.setStock(9999);
        sku.setTemplateVersionId(null);
        sku.setProperties(Collections.singletonList(new ProductSkuSaveReqVO.Property(1L, "时长", 11L, "60分钟")));

        ProductSpuSaveReqVO reqVO = new ProductSpuSaveReqVO();
        reqVO.setName("颈肩舒缓服务");
        reqVO.setKeyword("舒缓");
        reqVO.setIntroduction("服务介绍");
        reqVO.setDescription("服务详情");
        reqVO.setCategoryId(8001L);
        reqVO.setBrandId(9001L);
        reqVO.setPicUrl("https://hxy/spu.png");
        reqVO.setSliderPicUrls(Collections.singletonList("https://hxy/spu1.png"));
        reqVO.setSort(1);
        reqVO.setProductType(ProductTypeEnum.SERVICE.getType());
        reqVO.setTemplateVersionId(202603030001L);
        reqVO.setSpecType(false);
        reqVO.setDeliveryTypes(Collections.singletonList(2));
        reqVO.setDeliveryTemplateId(1L);
        reqVO.setGiveIntegral(0);
        reqVO.setSubCommissionType(false);
        reqVO.setSkus(Collections.singletonList(sku));
        return reqVO;
    }
}
