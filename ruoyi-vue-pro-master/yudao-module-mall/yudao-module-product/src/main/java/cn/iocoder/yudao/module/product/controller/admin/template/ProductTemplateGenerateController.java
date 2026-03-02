package cn.iocoder.yudao.module.product.controller.admin.template;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductCategoryTemplateValidateReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductCategoryTemplateValidateRespVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGenerateCommitReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGenerateCommitRespVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGeneratePreviewReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGeneratePreviewRespVO;
import cn.iocoder.yudao.module.product.service.template.ProductTemplateGenerateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 商品类目模板与 SKU 生成")
@RestController
@RequestMapping("/product/template")
@Validated
public class ProductTemplateGenerateController {

    @Resource
    private ProductTemplateGenerateService productTemplateGenerateService;

    @PostMapping("/validate")
    @Operation(summary = "校验类目模板")
    @PreAuthorize("@ss.hasPermission('product:template:validate') or @ss.hasPermission('product:spu:update')")
    public CommonResult<ProductCategoryTemplateValidateRespVO> validate(@Valid @RequestBody ProductCategoryTemplateValidateReqVO reqVO) {
        return success(productTemplateGenerateService.validateTemplate(reqVO));
    }

    @PostMapping("/sku-generator/preview")
    @Operation(summary = "预览 SKU 自动生成")
    @PreAuthorize("@ss.hasPermission('product:template:preview') or @ss.hasPermission('product:spu:update')")
    public CommonResult<ProductSkuGeneratePreviewRespVO> preview(@Valid @RequestBody ProductSkuGeneratePreviewReqVO reqVO) {
        return success(productTemplateGenerateService.previewSkuGenerate(reqVO));
    }

    @PostMapping("/sku-generator/commit")
    @Operation(summary = "提交 SKU 自动生成")
    @PreAuthorize("@ss.hasPermission('product:template:commit') or @ss.hasPermission('product:spu:update')")
    public CommonResult<ProductSkuGenerateCommitRespVO> commit(@Valid @RequestBody ProductSkuGenerateCommitReqVO reqVO) {
        return success(productTemplateGenerateService.commitSkuGenerate(reqVO));
    }
}
