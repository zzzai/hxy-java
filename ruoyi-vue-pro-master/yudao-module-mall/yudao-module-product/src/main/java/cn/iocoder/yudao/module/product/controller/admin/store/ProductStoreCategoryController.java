package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreCategoryListReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreCategoryRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreCategorySaveReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreCategoryDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_CATEGORY_NOT_EXISTS;

@Tag(name = "管理后台 - 门店分类")
@RestController
@RequestMapping("/product/store-category")
@Validated
public class ProductStoreCategoryController {

    @Resource
    private ProductStoreService storeService;

    @PostMapping("/save")
    @Operation(summary = "新增/更新门店分类")
    @PreAuthorize("@ss.hasPermission('product:store-category:create') || @ss.hasPermission('product:store-category:update')")
    public CommonResult<Long> save(@Valid @RequestBody ProductStoreCategorySaveReqVO reqVO) {
        return success(storeService.saveCategory(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除门店分类")
    @Parameter(name = "id", description = "编号", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('product:store-category:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        storeService.deleteCategory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得门店分类")
    @Parameter(name = "id", description = "编号", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('product:store-category:query')")
    public CommonResult<ProductStoreCategoryRespVO> get(@RequestParam("id") Long id) {
        ProductStoreCategoryDO category = storeService.getCategory(id);
        if (category == null) {
            throw exception(STORE_CATEGORY_NOT_EXISTS);
        }
        return success(BeanUtils.toBean(category, ProductStoreCategoryRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得门店分类列表")
    @PreAuthorize("@ss.hasPermission('product:store-category:query')")
    public CommonResult<List<ProductStoreCategoryRespVO>> list(@Valid ProductStoreCategoryListReqVO reqVO) {
        List<ProductStoreCategoryDO> list = storeService.getCategoryList(reqVO);
        list.sort(Comparator.comparing(ProductStoreCategoryDO::getSort).thenComparing(ProductStoreCategoryDO::getId));
        return success(BeanUtils.toBean(list, ProductStoreCategoryRespVO.class));
    }
}
