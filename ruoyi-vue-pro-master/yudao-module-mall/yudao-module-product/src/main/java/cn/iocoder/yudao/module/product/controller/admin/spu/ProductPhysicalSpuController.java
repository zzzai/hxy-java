package cn.iocoder.yudao.module.product.controller.admin.spu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuRespVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuUpdateStatusReqVO;
import cn.iocoder.yudao.module.product.convert.spu.ProductSpuConvert;
import cn.iocoder.yudao.module.product.dal.dataobject.sku.ProductSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import cn.iocoder.yudao.module.product.service.spu.TypedProductSpuAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 实物商品后台管理入口。
 */
@Tag(name = "管理后台 - 实物商品")
@RestController
@RequestMapping("/product/physical-spu")
@Validated
public class ProductPhysicalSpuController {

    @Resource
    private TypedProductSpuAdminService typedProductSpuAdminService;
    @Resource
    private ProductSpuService productSpuService;
    @Resource
    private ProductSkuService productSkuService;

    @PostMapping("/create")
    @Operation(summary = "创建实物商品 SPU")
    @PreAuthorize("@ss.hasPermission('product:physical-spu:create')")
    public CommonResult<Long> createProductSpu(@Valid @RequestBody ProductSpuSaveReqVO createReqVO) {
        return success(typedProductSpuAdminService.createSpu(ProductTypeEnum.PHYSICAL, createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新实物商品 SPU")
    @PreAuthorize("@ss.hasPermission('product:physical-spu:update')")
    public CommonResult<Boolean> updateSpu(@Valid @RequestBody ProductSpuSaveReqVO updateReqVO) {
        typedProductSpuAdminService.updateSpu(ProductTypeEnum.PHYSICAL, updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除实物商品 SPU")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('product:physical-spu:delete')")
    public CommonResult<Boolean> deleteSpu(@RequestParam("id") Long id) {
        typedProductSpuAdminService.deleteSpu(id, ProductTypeEnum.PHYSICAL);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新实物商品 SPU Status")
    @PreAuthorize("@ss.hasPermission('product:physical-spu:update')")
    public CommonResult<Boolean> updateStatus(@Valid @RequestBody ProductSpuUpdateStatusReqVO updateReqVO) {
        typedProductSpuAdminService.updateSpuStatus(ProductTypeEnum.PHYSICAL, updateReqVO);
        return success(true);
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得实物商品 SPU 明细")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('product:physical-spu:query')")
    public CommonResult<ProductSpuRespVO> getSpuDetail(@RequestParam("id") Long id) {
        ProductSpuDO spu = typedProductSpuAdminService.getTypedSpu(id, ProductTypeEnum.PHYSICAL);
        List<ProductSkuDO> skus = productSkuService.getSkuListBySpuId(spu.getId());
        return success(ProductSpuConvert.INSTANCE.convert(spu, skus));
    }

    @GetMapping("/page")
    @Operation(summary = "获得实物商品 SPU 分页")
    @PreAuthorize("@ss.hasPermission('product:physical-spu:query')")
    public CommonResult<PageResult<ProductSpuRespVO>> getSpuPage(@Valid ProductSpuPageReqVO pageVO) {
        typedProductSpuAdminService.applyPageType(ProductTypeEnum.PHYSICAL, pageVO);
        PageResult<ProductSpuDO> pageResult = productSpuService.getSpuPage(pageVO);
        return success(BeanUtils.toBean(pageResult, ProductSpuRespVO.class));
    }

    @GetMapping("/get-count")
    @Operation(summary = "获得实物商品 SPU 分页 tab count")
    @PreAuthorize("@ss.hasPermission('product:physical-spu:query')")
    public CommonResult<Map<Integer, Long>> getSpuCount() {
        return success(typedProductSpuAdminService.getTabsCount(ProductTypeEnum.PHYSICAL));
    }
}

