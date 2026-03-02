package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreTagGroupListReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreTagGroupRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreTagGroupSaveReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagGroupDO;
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

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_TAG_GROUP_NOT_EXISTS;

@Tag(name = "管理后台 - 门店标签组")
@RestController
@RequestMapping("/product/store-tag-group")
@Validated
public class ProductStoreTagGroupController {

    @Resource
    private ProductStoreService storeService;

    @PostMapping("/save")
    @Operation(summary = "新增/更新门店标签组")
    @PreAuthorize("@ss.hasPermission('product:store-tag-group:create') || @ss.hasPermission('product:store-tag-group:update')")
    public CommonResult<Long> save(@Valid @RequestBody ProductStoreTagGroupSaveReqVO reqVO) {
        return success(storeService.saveTagGroup(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除门店标签组")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('product:store-tag-group:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        storeService.deleteTagGroup(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得门店标签组")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('product:store-tag-group:query')")
    public CommonResult<ProductStoreTagGroupRespVO> get(@RequestParam("id") Long id) {
        ProductStoreTagGroupDO group = storeService.getTagGroup(id);
        if (group == null) {
            throw exception(STORE_TAG_GROUP_NOT_EXISTS);
        }
        return success(BeanUtils.toBean(group, ProductStoreTagGroupRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得门店标签组列表")
    @PreAuthorize("@ss.hasPermission('product:store-tag-group:query')")
    public CommonResult<List<ProductStoreTagGroupRespVO>> list(@Valid ProductStoreTagGroupListReqVO reqVO) {
        List<ProductStoreTagGroupDO> list = storeService.getTagGroupList(reqVO);
        list.sort(Comparator.comparing(ProductStoreTagGroupDO::getSort).thenComparing(ProductStoreTagGroupDO::getId));
        return success(BeanUtils.toBean(list, ProductStoreTagGroupRespVO.class));
    }
}
