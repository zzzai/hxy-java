package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreTagListReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreTagRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreTagSaveReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagDO;
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
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_TAG_NOT_EXISTS;

@Tag(name = "管理后台 - 门店标签")
@RestController
@RequestMapping("/product/store-tag")
@Validated
public class ProductStoreTagController {

    @Resource
    private ProductStoreService storeService;

    @PostMapping("/save")
    @Operation(summary = "新增/更新门店标签")
    @PreAuthorize("@ss.hasPermission('product:store-tag:create') || @ss.hasPermission('product:store-tag:update')")
    public CommonResult<Long> save(@Valid @RequestBody ProductStoreTagSaveReqVO reqVO) {
        return success(storeService.saveTag(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除门店标签")
    @Parameter(name = "id", description = "编号", required = true, example = "101")
    @PreAuthorize("@ss.hasPermission('product:store-tag:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        storeService.deleteTag(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得门店标签")
    @Parameter(name = "id", description = "编号", required = true, example = "101")
    @PreAuthorize("@ss.hasPermission('product:store-tag:query')")
    public CommonResult<ProductStoreTagRespVO> get(@RequestParam("id") Long id) {
        ProductStoreTagDO tag = storeService.getTag(id);
        if (tag == null) {
            throw exception(STORE_TAG_NOT_EXISTS);
        }
        return success(BeanUtils.toBean(tag, ProductStoreTagRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得门店标签列表")
    @PreAuthorize("@ss.hasPermission('product:store-tag:query')")
    public CommonResult<List<ProductStoreTagRespVO>> list(@Valid ProductStoreTagListReqVO reqVO) {
        List<ProductStoreTagDO> list = storeService.getTagList(reqVO);
        list.sort(Comparator.comparing(ProductStoreTagDO::getSort).thenComparing(ProductStoreTagDO::getId));
        return success(BeanUtils.toBean(list, ProductStoreTagRespVO.class));
    }
}
