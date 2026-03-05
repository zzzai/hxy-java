package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.*;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreCategoryDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_NOT_EXISTS;

@Tag(name = "管理后台 - 门店管理")
@RestController
@RequestMapping("/product/store")
@Validated
public class ProductStoreController {

    @Resource
    private ProductStoreService storeService;

    @PostMapping("/save")
    @Operation(summary = "新增/更新门店")
    @PreAuthorize("@ss.hasPermission('product:store:create') || @ss.hasPermission('product:store:update')")
    public CommonResult<Long> saveStore(@Valid @RequestBody ProductStoreSaveReqVO reqVO) {
        return success(storeService.saveStore(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除门店")
    @Parameter(name = "id", description = "编号", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('product:store:delete')")
    public CommonResult<Boolean> deleteStore(@RequestParam("id") Long id) {
        storeService.deleteStore(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得门店")
    @Parameter(name = "id", description = "编号", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<ProductStoreRespVO> getStore(@RequestParam("id") Long id) {
        ProductStoreDO store = storeService.getStore(id);
        if (store == null) {
            throw exception(STORE_NOT_EXISTS);
        }
        ProductStoreRespVO respVO = BeanUtils.toBean(store, ProductStoreRespVO.class);
        ProductStoreCategoryDO category = storeService.getCategory(store.getCategoryId());
        respVO.setCategoryName(category == null ? null : category.getName());
        respVO.setTagIds(storeService.getStoreTagIds(id));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询门店")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<PageResult<ProductStoreRespVO>> pageStore(@Valid ProductStorePageReqVO reqVO) {
        PageResult<ProductStoreDO> pageResult = storeService.getStorePage(reqVO);
        List<ProductStoreCategoryDO> categories = storeService.getCategoryList(new ProductStoreCategoryListReqVO());
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(ProductStoreCategoryDO::getId, ProductStoreCategoryDO::getName, (v1, v2) -> v1));
        PageResult<ProductStoreRespVO> respPage = BeanUtils.toBean(pageResult, ProductStoreRespVO.class);
        respPage.getList().forEach(item -> item.setCategoryName(categoryNameMap.get(item.getCategoryId())));
        return success(respPage);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得门店精简列表（用于下拉）")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<List<ProductStoreSimpleRespVO>> simpleList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(storeService.getStoreSimpleList(keyword));
    }

    @GetMapping("/tag-ids")
    @Operation(summary = "获得门店标签编号列表")
    @Parameter(name = "storeId", description = "门店编号", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<List<Long>> getStoreTagIds(@RequestParam("storeId") Long storeId) {
        List<Long> tagIds = storeService.getStoreTagIds(storeId);
        return success(tagIds == null ? Collections.emptyList() : tagIds);
    }

    @PostMapping("/update-lifecycle")
    @Operation(summary = "更新门店生命周期状态")
    @PreAuthorize("@ss.hasPermission('product:store:update')")
    public CommonResult<Boolean> updateLifecycle(@Valid @RequestBody ProductStoreLifecycleUpdateReqVO reqVO) {
        storeService.updateStoreLifecycle(reqVO.getId(), reqVO.getLifecycleStatus(), reqVO.getReason());
        return success(true);
    }

    @GetMapping("/lifecycle-guard")
    @Operation(summary = "获取门店生命周期守卫详情")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<ProductStoreLifecycleGuardRespVO> getLifecycleGuard(
            @RequestParam("id") Long id,
            @RequestParam("lifecycleStatus") Integer lifecycleStatus) {
        return success(storeService.getLifecycleGuard(id, lifecycleStatus));
    }

    @PostMapping("/lifecycle-guard/batch")
    @Operation(summary = "批量获取门店生命周期守卫详情")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<List<ProductStoreLifecycleGuardRespVO>> getLifecycleGuardBatch(
            @Valid @RequestBody ProductStoreBatchLifecycleReqVO reqVO) {
        return success(storeService.getLifecycleGuardBatch(reqVO.getStoreIds(), reqVO.getLifecycleStatus()));
    }

    @PostMapping("/lifecycle-guard/recheck-by-batch")
    @Operation(summary = "按批次复核门店生命周期守卫（不执行状态变更）")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<ProductStoreLifecycleGuardBatchRecheckRespVO> recheckLifecycleGuardByBatch(
            @Valid @RequestBody ProductStoreLifecycleGuardBatchRecheckReqVO reqVO) {
        return success(storeService.recheckLifecycleGuardByBatch(reqVO));
    }

    @PostMapping("/lifecycle-guard/recheck-by-batch/execute")
    @Operation(summary = "按批次复核并落台账（不执行状态变更）")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<ProductStoreLifecycleGuardBatchRecheckRespVO> executeLifecycleGuardRecheckByBatch(
            @Valid @RequestBody ProductStoreLifecycleGuardBatchRecheckReqVO reqVO) {
        return success(storeService.executeLifecycleGuardRecheckByBatch(reqVO));
    }

    @GetMapping("/check-launch-readiness")
    @Operation(summary = "检查门店上线门禁")
    @Parameter(name = "id", description = "门店编号", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('product:store:check-launch-readiness')")
    public CommonResult<ProductStoreLaunchReadinessRespVO> checkLaunchReadiness(@RequestParam("id") Long id) {
        return success(storeService.getLaunchReadiness(id));
    }

    @PostMapping("/batch/category")
    @Operation(summary = "批量更新门店分类")
    @PreAuthorize("@ss.hasPermission('product:store:batch-category')")
    public CommonResult<Boolean> batchUpdateCategory(@Valid @RequestBody ProductStoreBatchCategoryReqVO reqVO) {
        storeService.batchUpdateCategory(reqVO);
        return success(true);
    }

    @PostMapping("/batch/tags")
    @Operation(summary = "批量更新门店标签")
    @PreAuthorize("@ss.hasPermission('product:store:batch-tags')")
    public CommonResult<Boolean> batchUpdateTags(@Valid @RequestBody ProductStoreBatchTagReqVO reqVO) {
        storeService.batchUpdateTags(reqVO);
        return success(true);
    }

    @PostMapping("/batch/lifecycle")
    @Operation(summary = "批量更新门店生命周期")
    @PreAuthorize("@ss.hasPermission('product:store:batch-lifecycle')")
    public CommonResult<Boolean> batchUpdateLifecycle(@Valid @RequestBody ProductStoreBatchLifecycleReqVO reqVO) {
        storeService.batchUpdateLifecycle(reqVO);
        return success(true);
    }

    @PostMapping("/batch/lifecycle/execute")
    @Operation(summary = "批量执行门店生命周期更新（返回执行结果）")
    @PreAuthorize("@ss.hasPermission('product:store:batch-lifecycle')")
    public CommonResult<ProductStoreBatchLifecycleExecuteRespVO> batchUpdateLifecycleExecute(
            @Valid @RequestBody ProductStoreBatchLifecycleReqVO reqVO) {
        return success(storeService.batchUpdateLifecycleWithResult(reqVO));
    }
}
