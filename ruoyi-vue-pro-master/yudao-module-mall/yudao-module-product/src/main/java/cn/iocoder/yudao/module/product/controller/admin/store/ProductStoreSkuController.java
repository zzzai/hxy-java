package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.dal.dataobject.sku.ProductSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchAdjustReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuOptionRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuDO;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreMappingService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 门店 SKU 映射")
@RestController
@RequestMapping("/product/store-sku")
@Validated
public class ProductStoreSkuController {

    @Resource
    private ProductStoreMappingService storeMappingService;
    @Resource
    private ProductStoreService productStoreService;
    @Resource
    private ProductSpuService productSpuService;
    @Resource
    private ProductSkuService productSkuService;

    @PostMapping("/save")
    @Operation(summary = "新增/更新门店 SKU 映射")
    @PreAuthorize("@ss.hasPermission('product:store-sku:create') || @ss.hasPermission('product:store-sku:update')")
    public CommonResult<Long> save(@Valid @RequestBody ProductStoreSkuSaveReqVO reqVO) {
        return success(storeMappingService.saveStoreSku(reqVO));
    }

    @PostMapping("/batch-save")
    @Operation(summary = "批量铺货 SKU 到门店")
    @PreAuthorize("@ss.hasPermission('product:store-sku:create') || @ss.hasPermission('product:store-sku:update')")
    public CommonResult<Integer> batchSave(@Valid @RequestBody ProductStoreSkuBatchSaveReqVO reqVO) {
        return success(storeMappingService.batchSaveStoreSku(reqVO));
    }

    @PostMapping("/batch-adjust")
    @Operation(summary = "按门店集批量调价/调库存")
    @PreAuthorize("@ss.hasPermission('product:store-sku:update')")
    public CommonResult<Integer> batchAdjust(@Valid @RequestBody ProductStoreSkuBatchAdjustReqVO reqVO) {
        return success(storeMappingService.batchAdjustStoreSku(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除门店 SKU 映射")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('product:store-sku:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        storeMappingService.deleteStoreSku(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得门店 SKU 映射")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('product:store-sku:query')")
    public CommonResult<ProductStoreSkuRespVO> get(@RequestParam("id") Long id) {
        ProductStoreSkuDO storeSku = storeMappingService.getStoreSku(id);
        if (storeSku == null) {
            return success(null);
        }
        Map<Long, ProductStoreDO> storeMap = productStoreService.getStoreMap(Collections.singleton(storeSku.getStoreId()));
        Map<Long, ProductSpuDO> spuMap = productSpuService.getSpuMap(Collections.singleton(storeSku.getSpuId()));
        Map<Long, ProductSkuDO> skuMap = buildSkuMap(Collections.singleton(storeSku.getSkuId()));
        return success(toSkuRespVO(storeSku, storeMap, spuMap, skuMap));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询门店 SKU 映射")
    @PreAuthorize("@ss.hasPermission('product:store-sku:query')")
    public CommonResult<PageResult<ProductStoreSkuRespVO>> page(@Valid ProductStoreSkuPageReqVO reqVO) {
        PageResult<ProductStoreSkuDO> pageResult = storeMappingService.getStoreSkuPage(reqVO);
        if (CollectionUtils.isEmpty(pageResult.getList())) {
            return success(BeanUtils.toBean(pageResult, ProductStoreSkuRespVO.class));
        }
        Set<Long> storeIds = pageResult.getList().stream().map(ProductStoreSkuDO::getStoreId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> spuIds = pageResult.getList().stream().map(ProductStoreSkuDO::getSpuId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> skuIds = pageResult.getList().stream().map(ProductStoreSkuDO::getSkuId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, ProductStoreDO> storeMap = productStoreService.getStoreMap(storeIds);
        Map<Long, ProductSpuDO> spuMap = productSpuService.getSpuMap(spuIds);
        Map<Long, ProductSkuDO> skuMap = buildSkuMap(skuIds);

        List<ProductStoreSkuRespVO> records = pageResult.getList().stream()
                .map(item -> toSkuRespVO(item, storeMap, spuMap, skuMap))
                .collect(Collectors.toList());
        return success(new PageResult<>(records, pageResult.getTotal()));
    }

    @GetMapping("/store-options")
    @Operation(summary = "获取门店选项列表")
    @PreAuthorize("@ss.hasPermission('product:store-sku:query')")
    public CommonResult<List<ProductStoreOptionRespVO>> getStoreOptions(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(storeMappingService.getStoreOptions(keyword));
    }

    @GetMapping("/spu-options")
    @Operation(summary = "获取可选 SPU 列表")
    @PreAuthorize("@ss.hasPermission('product:store-sku:query')")
    public CommonResult<List<ProductStoreSpuOptionRespVO>> getSpuOptions(
            @RequestParam(value = "productType", required = false) Integer productType,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(storeMappingService.getSpuOptions(productType, keyword));
    }

    @GetMapping("/sku-options")
    @Operation(summary = "获取可选 SKU 列表")
    @Parameter(name = "spuId", description = "SPU 编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('product:store-sku:query')")
    public CommonResult<List<ProductStoreSkuOptionRespVO>> getSkuOptions(@RequestParam("spuId") Long spuId) {
        return success(storeMappingService.getSkuOptions(spuId));
    }

    private Map<Long, ProductSkuDO> buildSkuMap(Set<Long> skuIds) {
        if (CollectionUtils.isEmpty(skuIds)) {
            return Collections.emptyMap();
        }
        List<ProductSkuDO> skuList = productSkuService.getSkuList(skuIds);
        if (CollectionUtils.isEmpty(skuList)) {
            return Collections.emptyMap();
        }
        Map<Long, ProductSkuDO> skuMap = new HashMap<>();
        skuList.forEach(sku -> skuMap.put(sku.getId(), sku));
        return skuMap;
    }

    private ProductStoreSkuRespVO toSkuRespVO(ProductStoreSkuDO item,
                                              Map<Long, ProductStoreDO> storeMap,
                                              Map<Long, ProductSpuDO> spuMap,
                                              Map<Long, ProductSkuDO> skuMap) {
        ProductStoreSkuRespVO respVO = BeanUtils.toBean(item, ProductStoreSkuRespVO.class);
        ProductStoreDO store = storeMap.get(item.getStoreId());
        if (store != null) {
            respVO.setStoreName(store.getName());
        }
        ProductSpuDO spu = spuMap.get(item.getSpuId());
        if (spu != null) {
            respVO.setSpuName(spu.getName());
        }
        ProductSkuDO sku = skuMap.get(item.getSkuId());
        if (sku != null) {
            respVO.setSkuSpecText(buildSkuSpecText(sku));
        }
        return respVO;
    }

    private String buildSkuSpecText(ProductSkuDO sku) {
        if (CollectionUtils.isEmpty(sku.getProperties())) {
            return "默认规格";
        }
        return sku.getProperties().stream()
                .map(property -> {
                    String propertyName = property.getPropertyName() == null ? "" : property.getPropertyName();
                    String valueName = property.getValueName() == null ? "" : property.getValueName();
                    if (!StringUtils.hasText(propertyName)) {
                        return valueName;
                    }
                    return propertyName + ":" + valueName;
                })
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("；"));
    }
}
