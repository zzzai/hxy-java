package cn.iocoder.yudao.module.product.service.template;

import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductCategoryTemplateValidateReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductCategoryTemplateValidateRespVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGenerateCommitReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGenerateCommitRespVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGeneratePreviewReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGeneratePreviewRespVO;

import javax.validation.Valid;

public interface ProductTemplateGenerateService {

    ProductCategoryTemplateValidateRespVO validateTemplate(@Valid ProductCategoryTemplateValidateReqVO reqVO);

    ProductSkuGeneratePreviewRespVO previewSkuGenerate(@Valid ProductSkuGeneratePreviewReqVO reqVO);

    ProductSkuGenerateCommitRespVO commitSkuGenerate(@Valid ProductSkuGenerateCommitReqVO reqVO);

    int retryFailedCommitTasks(int limit);
}
