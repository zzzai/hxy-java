package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuBatchSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuSaveReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSpuDO;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreMappingService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.CollectionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 门店 SPU 映射")
@RestController
@RequestMapping("/product/store-spu")
@Validated
public class ProductStoreSpuController {

    @Resource
    private ProductStoreMappingService storeMappingService;
    @Resource
    private ProductStoreService productStoreService;
    @Resource
    private ProductSpuService productSpuService;

    @PostMapping("/save")
    @Operation(summary = "新增/更新门店 SPU 映射")
    @PreAuthorize("@ss.hasPermission('product:store-spu:create') || @ss.hasPermission('product:store-spu:update')")
    public CommonResult<Long> save(@Valid @RequestBody ProductStoreSpuSaveReqVO reqVO) {
        return success(storeMappingService.saveStoreSpu(reqVO));
    }

    @PostMapping("/batch-save")
    @Operation(summary = "批量铺货 SPU 到门店")
    @PreAuthorize("@ss.hasPermission('product:store-spu:create') || @ss.hasPermission('product:store-spu:update')")
    public CommonResult<Integer> batchSave(@Valid @RequestBody ProductStoreSpuBatchSaveReqVO reqVO) {
        return success(storeMappingService.batchSaveStoreSpu(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除门店 SPU 映射")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('product:store-spu:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        storeMappingService.deleteStoreSpu(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得门店 SPU 映射")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('product:store-spu:query')")
    public CommonResult<ProductStoreSpuRespVO> get(@RequestParam("id") Long id) {
        ProductStoreSpuDO storeSpu = storeMappingService.getStoreSpu(id);
        if (storeSpu == null) {
            return success(null);
        }
        Map<Long, ProductStoreDO> storeMap = productStoreService.getStoreMap(Collections.singleton(storeSpu.getStoreId()));
        Map<Long, ProductSpuDO> spuMap = productSpuService.getSpuMap(Collections.singleton(storeSpu.getSpuId()));
        ProductStoreSpuRespVO respVO = toSpuRespVO(storeSpu, storeMap, spuMap);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询门店 SPU 映射")
    @PreAuthorize("@ss.hasPermission('product:store-spu:query')")
    public CommonResult<PageResult<ProductStoreSpuRespVO>> page(@Valid ProductStoreSpuPageReqVO reqVO) {
        PageResult<ProductStoreSpuDO> pageResult = storeMappingService.getStoreSpuPage(reqVO);
        if (CollectionUtils.isEmpty(pageResult.getList())) {
            return success(BeanUtils.toBean(pageResult, ProductStoreSpuRespVO.class));
        }
        Set<Long> storeIds = pageResult.getList().stream().map(ProductStoreSpuDO::getStoreId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> spuIds = pageResult.getList().stream().map(ProductStoreSpuDO::getSpuId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ProductStoreDO> storeMap = productStoreService.getStoreMap(storeIds);
        Map<Long, ProductSpuDO> spuMap = productSpuService.getSpuMap(spuIds);
        List<ProductStoreSpuRespVO> records = pageResult.getList().stream()
                .map(item -> toSpuRespVO(item, storeMap, spuMap))
                .collect(Collectors.toList());
        return success(new PageResult<>(records, pageResult.getTotal()));
    }

    @GetMapping("/store-options")
    @Operation(summary = "获取门店选项列表")
    @PreAuthorize("@ss.hasPermission('product:store-spu:query')")
    public CommonResult<List<ProductStoreOptionRespVO>> getStoreOptions(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(storeMappingService.getStoreOptions(keyword));
    }

    @GetMapping("/spu-options")
    @Operation(summary = "获取可选 SPU 列表")
    @PreAuthorize("@ss.hasPermission('product:store-spu:query')")
    public CommonResult<List<ProductStoreSpuOptionRespVO>> getSpuOptions(
            @RequestParam(value = "productType", required = false) Integer productType,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(storeMappingService.getSpuOptions(productType, keyword));
    }

    private ProductStoreSpuRespVO toSpuRespVO(ProductStoreSpuDO item,
                                              Map<Long, ProductStoreDO> storeMap,
                                              Map<Long, ProductSpuDO> spuMap) {
        ProductStoreSpuRespVO respVO = BeanUtils.toBean(item, ProductStoreSpuRespVO.class);
        ProductStoreDO store = storeMap.get(item.getStoreId());
        if (store != null) {
            respVO.setStoreName(store.getName());
        }
        ProductSpuDO spu = spuMap.get(item.getSpuId());
        if (spu != null) {
            respVO.setSpuName(spu.getName());
        }
        return respVO;
    }
}
