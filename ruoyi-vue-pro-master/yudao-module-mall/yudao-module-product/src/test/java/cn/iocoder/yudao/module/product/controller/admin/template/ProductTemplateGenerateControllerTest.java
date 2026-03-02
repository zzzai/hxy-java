package cn.iocoder.yudao.module.product.controller.admin.template;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductCategoryTemplateValidateReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductCategoryTemplateValidateRespVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGenerateCommitReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGenerateCommitRespVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGeneratePreviewReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGeneratePreviewRespVO;
import cn.iocoder.yudao.module.product.service.template.ProductTemplateGenerateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductTemplateGenerateControllerTest {

    @Mock
    private ProductTemplateGenerateService productTemplateGenerateService;

    @InjectMocks
    private ProductTemplateGenerateController controller;

    @Test
    void validate_shouldDelegateService() {
        ProductCategoryTemplateValidateRespVO respVO = new ProductCategoryTemplateValidateRespVO();
        respVO.setPass(true);
        when(productTemplateGenerateService.validateTemplate(any(ProductCategoryTemplateValidateReqVO.class)))
                .thenReturn(respVO);

        ProductCategoryTemplateValidateReqVO reqVO = new ProductCategoryTemplateValidateReqVO();
        ProductCategoryTemplateValidateReqVO.Item item = new ProductCategoryTemplateValidateReqVO.Item();
        item.setAttributeId(1L);
        item.setAttrRole(2);
        item.setRequired(true);
        item.setAffectsPrice(true);
        item.setAffectsStock(false);
        reqVO.setCategoryId(101L);
        reqVO.setItems(Collections.singletonList(item));

        CommonResult<ProductCategoryTemplateValidateRespVO> result = controller.validate(reqVO);
        assertEquals(true, result.getData().getPass());
    }

    @Test
    void preview_shouldDelegateService() {
        ProductSkuGeneratePreviewRespVO respVO = new ProductSkuGeneratePreviewRespVO();
        respVO.setTaskNo("TASK_PREVIEW");
        when(productTemplateGenerateService.previewSkuGenerate(any(ProductSkuGeneratePreviewReqVO.class)))
                .thenReturn(respVO);

        ProductSkuGeneratePreviewReqVO reqVO = new ProductSkuGeneratePreviewReqVO();
        CommonResult<ProductSkuGeneratePreviewRespVO> result = controller.preview(reqVO);
        assertEquals("TASK_PREVIEW", result.getData().getTaskNo());
    }

    @Test
    void commit_shouldDelegateService() {
        ProductSkuGenerateCommitRespVO respVO = new ProductSkuGenerateCommitRespVO();
        respVO.setTaskNo("TASK_COMMIT");
        when(productTemplateGenerateService.commitSkuGenerate(any(ProductSkuGenerateCommitReqVO.class)))
                .thenReturn(respVO);

        ProductSkuGenerateCommitReqVO reqVO = new ProductSkuGenerateCommitReqVO();
        CommonResult<ProductSkuGenerateCommitRespVO> result = controller.commit(reqVO);
        assertEquals("TASK_COMMIT", result.getData().getTaskNo());
    }
}
