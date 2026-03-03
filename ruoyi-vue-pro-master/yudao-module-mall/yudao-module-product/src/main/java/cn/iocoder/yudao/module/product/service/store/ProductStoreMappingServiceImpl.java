package cn.iocoder.yudao.module.product.service.store;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuUpdateStockReqDTO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchAdjustReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuManualStockAdjustReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuStockFlowPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuBatchSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuSaveReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.sku.ProductSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuStockFlowDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSpuDO;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuStockFlowMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSpuMapper;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuManualStockBizTypeEnum;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuStockFlowStatusEnum;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.product.enums.spu.ProductSpuStatusEnum;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ProductStoreMappingServiceImpl implements ProductStoreMappingService {

    private static final Integer SALE_STATUS_ENABLE = 0;
    private static final int OPTION_LIMIT = 200;
    private static final int STOCK_RETRY_DEFAULT_LIMIT = 100;
    private static final int STOCK_RETRY_MAX_LIMIT = 1000;
    private static final int STOCK_RETRY_BASE_SECONDS = 30;
    private static final int STOCK_PROCESSING_LEASE_SECONDS = 120;

    @Resource
    private ProductStoreSpuMapper storeSpuMapper;
    @Resource
    private ProductStoreSkuMapper storeSkuMapper;
    @Resource
    private ProductStoreSkuStockFlowMapper storeSkuStockFlowMapper;
    @Resource
    private ProductSpuService productSpuService;
    @Resource
    private ProductSkuService productSkuService;
    @Resource
    private ProductStoreService productStoreService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveStoreSpu(ProductStoreSpuSaveReqVO reqVO) {
        productStoreService.validateStoreExists(reqVO.getStoreId());
        ProductSpuDO spu = validateSpuExists(reqVO.getSpuId());
        ProductStoreSpuDO entity = BeanUtils.toBean(reqVO, ProductStoreSpuDO.class);
        entity.setProductType(spu.getProductType());
        if (entity.getSaleStatus() == null) {
            entity.setSaleStatus(SALE_STATUS_ENABLE);
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getRemark() == null) {
            entity.setRemark("");
        }
        if (reqVO.getId() != null) {
            validateStoreSpuExists(reqVO.getId());
            storeSpuMapper.updateById(entity);
            return entity.getId();
        }
        ProductStoreSpuDO existing = storeSpuMapper.selectByStoreIdAndSpuId(reqVO.getStoreId(), reqVO.getSpuId());
        if (existing != null) {
            entity.setId(existing.getId());
            storeSpuMapper.updateById(entity);
            return existing.getId();
        }
        ProductStoreSpuDO deleted = storeSpuMapper.selectByStoreIdAndSpuIdIncludeDeleted(reqVO.getStoreId(), reqVO.getSpuId());
        if (deleted != null) {
            entity.setId(deleted.getId());
            storeSpuMapper.recoverById(entity);
            return deleted.getId();
        }
        storeSpuMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchSaveStoreSpu(ProductStoreSpuBatchSaveReqVO reqVO) {
        List<Long> storeIds = normalizeStoreIds(reqVO.getStoreIds());
        int affected = 0;
        for (Long storeId : storeIds) {
            ProductStoreSpuSaveReqVO saveReq = new ProductStoreSpuSaveReqVO();
            saveReq.setStoreId(storeId);
            saveReq.setSpuId(reqVO.getSpuId());
            saveReq.setSaleStatus(reqVO.getSaleStatus());
            saveReq.setSort(reqVO.getSort());
            saveReq.setRemark(reqVO.getRemark());
            saveStoreSpu(saveReq);
            affected++;
        }
        return affected;
    }

    @Override
    public void deleteStoreSpu(Long id) {
        validateStoreSpuExists(id);
        storeSpuMapper.deleteById(id);
    }

    @Override
    public ProductStoreSpuDO getStoreSpu(Long id) {
        return storeSpuMapper.selectById(id);
    }

    @Override
    public PageResult<ProductStoreSpuDO> getStoreSpuPage(ProductStoreSpuPageReqVO reqVO) {
        return storeSpuMapper.selectPage(reqVO);
    }

    @Override
    public List<ProductStoreOptionRespVO> getStoreOptions(String keyword) {
        return productStoreService.getStoreOptions(keyword);
    }

    @Override
    public List<ProductStoreSpuOptionRespVO> getSpuOptions(Integer productType, String keyword) {
        ProductSpuPageReqVO reqVO = new ProductSpuPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(OPTION_LIMIT);
        reqVO.setProductType(productType);
        reqVO.setName(keyword);
        List<ProductSpuDO> spus = productSpuService.getSpuPage(reqVO).getList();
        if (spus == null) {
            return new ArrayList<>();
        }
        return spus.stream()
                .filter(Objects::nonNull)
                .filter(spu -> !Objects.equals(spu.getStatus(), ProductSpuStatusEnum.RECYCLE.getStatus()))
                .map(spu -> {
                    ProductStoreSpuOptionRespVO option = new ProductStoreSpuOptionRespVO();
                    option.setId(spu.getId());
                    option.setName(spu.getName());
                    option.setProductType(spu.getProductType());
                    option.setStatus(spu.getStatus());
                    return option;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductStoreSkuOptionRespVO> getSkuOptions(Long spuId) {
        List<ProductSkuDO> skuList = productSkuService.getSkuListBySpuId(spuId);
        if (skuList == null) {
            return new ArrayList<>();
        }
        return skuList.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProductSkuDO::getId))
                .map(sku -> {
                    ProductStoreSkuOptionRespVO option = new ProductStoreSkuOptionRespVO();
                    option.setId(sku.getId());
                    option.setSpuId(sku.getSpuId());
                    option.setSpecText(buildSkuSpecText(sku));
                    option.setPrice(sku.getPrice());
                    option.setMarketPrice(sku.getMarketPrice());
                    option.setStock(sku.getStock());
                    return option;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveStoreSku(ProductStoreSkuSaveReqVO reqVO) {
        productStoreService.validateStoreExists(reqVO.getStoreId());
        ProductSkuDO sku = productSkuService.getSku(reqVO.getSkuId());
        if (sku == null) {
            throw exception(SKU_NOT_EXISTS);
        }
        ProductSpuDO spu = validateSpuExists(sku.getSpuId());
        ensureStoreSpuMapping(reqVO.getStoreId(), spu);

        ProductStoreSkuDO entity = BeanUtils.toBean(reqVO, ProductStoreSkuDO.class);
        entity.setSpuId(sku.getSpuId());
        if (entity.getSaleStatus() == null) {
            entity.setSaleStatus(SALE_STATUS_ENABLE);
        }
        if (entity.getSalePrice() == null) {
            entity.setSalePrice(sku.getPrice());
        }
        if (entity.getMarketPrice() == null) {
            entity.setMarketPrice(sku.getMarketPrice());
        }
        if (entity.getStock() == null) {
            entity.setStock(sku.getStock());
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getRemark() == null) {
            entity.setRemark("");
        }
        if (reqVO.getId() != null) {
            validateStoreSkuExists(reqVO.getId());
            storeSkuMapper.updateById(entity);
            return entity.getId();
        }
        ProductStoreSkuDO existing = storeSkuMapper.selectByStoreIdAndSkuId(reqVO.getStoreId(), reqVO.getSkuId());
        if (existing != null) {
            entity.setId(existing.getId());
            storeSkuMapper.updateById(entity);
            return existing.getId();
        }
        ProductStoreSkuDO deleted = storeSkuMapper.selectByStoreIdAndSkuIdIncludeDeleted(reqVO.getStoreId(), reqVO.getSkuId());
        if (deleted != null) {
            entity.setId(deleted.getId());
            storeSkuMapper.recoverById(entity);
            return deleted.getId();
        }
        storeSkuMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchSaveStoreSku(ProductStoreSkuBatchSaveReqVO reqVO) {
        List<Long> storeIds = normalizeStoreIds(reqVO.getStoreIds());
        int affected = 0;
        for (Long storeId : storeIds) {
            ProductStoreSkuSaveReqVO saveReq = new ProductStoreSkuSaveReqVO();
            saveReq.setStoreId(storeId);
            saveReq.setSkuId(reqVO.getSkuId());
            saveReq.setSaleStatus(reqVO.getSaleStatus());
            saveReq.setSalePrice(reqVO.getSalePrice());
            saveReq.setMarketPrice(reqVO.getMarketPrice());
            saveReq.setStock(reqVO.getStock());
            saveReq.setSort(reqVO.getSort());
            saveReq.setRemark(reqVO.getRemark());
            saveStoreSku(saveReq);
            affected++;
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchAdjustStoreSku(ProductStoreSkuBatchAdjustReqVO reqVO) {
        if (reqVO.getSaleStatus() == null
                && reqVO.getSalePrice() == null
                && reqVO.getMarketPrice() == null
                && reqVO.getStock() == null
                && reqVO.getRemark() == null) {
            throw exception(STORE_SKU_BATCH_ADJUST_FIELDS_EMPTY);
        }
        ProductSkuDO sku = productSkuService.getSku(reqVO.getSkuId());
        if (sku == null) {
            throw exception(SKU_NOT_EXISTS);
        }
        ProductSpuDO spu = validateSpuExists(sku.getSpuId());

        List<Long> storeIds = normalizeStoreIds(reqVO.getStoreIds());
        int affected = 0;
        for (Long storeId : storeIds) {
            productStoreService.validateStoreExists(storeId);
            ensureStoreSpuMapping(storeId, spu);
            ProductStoreSkuDO current = storeSkuMapper.selectByStoreIdAndSkuId(storeId, reqVO.getSkuId());
            if (current == null) {
                ProductStoreSkuSaveReqVO createReq = new ProductStoreSkuSaveReqVO();
                createReq.setStoreId(storeId);
                createReq.setSkuId(reqVO.getSkuId());
                createReq.setSaleStatus(reqVO.getSaleStatus());
                createReq.setSalePrice(reqVO.getSalePrice());
                createReq.setMarketPrice(reqVO.getMarketPrice());
                createReq.setStock(reqVO.getStock());
                createReq.setRemark(reqVO.getRemark());
                saveStoreSku(createReq);
                affected++;
                continue;
            }
            ProductStoreSkuDO update = ProductStoreSkuDO.builder()
                    .id(current.getId())
                    .storeId(current.getStoreId())
                    .spuId(current.getSpuId())
                    .skuId(current.getSkuId())
                    .saleStatus(reqVO.getSaleStatus() != null ? reqVO.getSaleStatus() : current.getSaleStatus())
                    .salePrice(reqVO.getSalePrice() != null ? reqVO.getSalePrice() : current.getSalePrice())
                    .marketPrice(reqVO.getMarketPrice() != null ? reqVO.getMarketPrice() : current.getMarketPrice())
                    .stock(reqVO.getStock() != null ? reqVO.getStock() : current.getStock())
                    .sort(current.getSort())
                    .remark(reqVO.getRemark() != null ? reqVO.getRemark() : current.getRemark())
                    .build();
            storeSkuMapper.updateById(update);
            affected++;
        }
        return affected;
    }

    @Override
    public void deleteStoreSku(Long id) {
        validateStoreSkuExists(id);
        storeSkuMapper.deleteById(id);
    }

    @Override
    public ProductStoreSkuDO getStoreSku(Long id) {
        return storeSkuMapper.selectById(id);
    }

    @Override
    public PageResult<ProductStoreSkuDO> getStoreSkuPage(ProductStoreSkuPageReqVO reqVO) {
        return storeSkuMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<ProductStoreSkuStockFlowDO> getStoreSkuStockFlowPage(ProductStoreSkuStockFlowPageReqVO reqVO) {
        return storeSkuStockFlowMapper.selectPage(reqVO);
    }

    @Override
    public Map<Long, ProductStoreSkuRespDTO> getStoreSkuMap(Long storeId, Collection<Long> skuIds) {
        if (storeId == null || skuIds == null || skuIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<Long> normalizedSkuIds = skuIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedSkuIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<ProductStoreSkuDO> mappingList = storeSkuMapper.selectListByStoreIdAndSkuIds(storeId, normalizedSkuIds);
        if (mappingList == null || mappingList.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return mappingList.stream()
                .collect(Collectors.toMap(ProductStoreSkuDO::getSkuId,
                        item -> BeanUtils.toBean(item, ProductStoreSkuRespDTO.class),
                        (o1, o2) -> o1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStoreSkuStock(ProductStoreSkuUpdateStockReqDTO updateStockReqDTO) {
        productStoreService.validateStoreExists(updateStockReqDTO.getStoreId());
        if (updateStockReqDTO.getItems() == null || updateStockReqDTO.getItems().isEmpty()) {
            return;
        }
        String bizType = normalizeStockBizField(updateStockReqDTO.getBizType(), "bizType");
        String bizNo = normalizeStockBizField(updateStockReqDTO.getBizNo(), "bizNo");
        for (ProductStoreSkuUpdateStockReqDTO.Item item : updateStockReqDTO.getItems()) {
            if (item == null || item.getSkuId() == null || item.getIncrCount() == null || item.getIncrCount() == 0) {
                continue;
            }
            ProductStoreSkuStockFlowDO flow = getOrCreateStockFlow(
                    bizType, bizNo, updateStockReqDTO.getStoreId(), item.getSkuId(), item.getIncrCount());
            Integer oldStatus = flow.getStatus() == null
                    ? ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus() : flow.getStatus();
            if (Objects.equals(oldStatus, ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus())
                    || Objects.equals(oldStatus, ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus())) {
                continue;
            }
            int claimed = storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(flow.getId(), oldStatus,
                    ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus(),
                    normalizeRetryCount(flow.getRetryCount()), calculateProcessingLeaseTime(), "");
            if (claimed == 0) {
                continue;
            }
            try {
                applyStoreSkuStock(updateStockReqDTO.getStoreId(), item);
                storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(flow.getId(),
                        ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus(),
                        ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus(),
                        normalizeRetryCount(flow.getRetryCount()), null, null);
            } catch (RuntimeException ex) {
                int nextRetryCount = normalizeRetryCount(flow.getRetryCount()) + 1;
                storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(flow.getId(),
                        ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus(),
                        ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus(),
                        nextRetryCount, calculateNextRetryTime(nextRetryCount), trimErrorMsg(ex.getMessage()));
                throw ex;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer manualAdjustStoreSkuStock(ProductStoreSkuManualStockAdjustReqVO reqVO) {
        productStoreService.validateStoreExists(reqVO.getStoreId());
        ProductStoreSkuManualStockBizTypeEnum bizTypeEnum = ProductStoreSkuManualStockBizTypeEnum
                .valueOfCode(reqVO.getBizType());
        if (bizTypeEnum == null) {
            throw exception(STORE_SKU_STOCK_MANUAL_BIZ_TYPE_INVALID, reqVO.getBizType());
        }
        List<ProductStoreSkuUpdateStockReqDTO.Item> stockItems = normalizeManualStockItems(reqVO.getItems(), bizTypeEnum);
        ProductStoreSkuUpdateStockReqDTO stockReq = new ProductStoreSkuUpdateStockReqDTO();
        stockReq.setStoreId(reqVO.getStoreId());
        stockReq.setBizType(bizTypeEnum.getStockFlowBizType());
        stockReq.setBizNo(reqVO.getBizNo());
        stockReq.setItems(stockItems);
        updateStoreSkuStock(stockReq);
        return stockItems.size();
    }

    @Override
    public int retryStoreSkuStockFlow(Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? STOCK_RETRY_DEFAULT_LIMIT : Math.min(limit, STOCK_RETRY_MAX_LIMIT);
        LocalDateTime now = LocalDateTime.now();
        storeSkuStockFlowMapper.markProcessingTimeoutAsFailed(now, "processing-lease-timeout");
        List<ProductStoreSkuStockFlowDO> retryableList = storeSkuStockFlowMapper.selectRetryableList(now, safeLimit);
        int successCount = 0;
        for (ProductStoreSkuStockFlowDO flow : retryableList) {
            if (executeRetryFlow(flow)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int retryStoreSkuStockFlowByIds(List<Long> flowIds) {
        List<Long> normalizedFlowIds = normalizeFlowIds(flowIds);
        List<ProductStoreSkuStockFlowDO> flowList = storeSkuStockFlowMapper.selectBatchIds(normalizedFlowIds);
        if (flowList == null || flowList.isEmpty()) {
            return 0;
        }
        int successCount = 0;
        for (ProductStoreSkuStockFlowDO flow : flowList) {
            if (executeRetryFlow(flow)) {
                successCount++;
            }
        }
        return successCount;
    }

    private boolean executeRetryFlow(ProductStoreSkuStockFlowDO flow) {
        if (flow == null || !ProductStoreSkuStockFlowStatusEnum.isRetryable(flow.getStatus())) {
            return false;
        }
        Integer oldStatus = flow.getStatus() == null
                ? ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus() : flow.getStatus();
        int retryCount = normalizeRetryCount(flow.getRetryCount());
        int claimed = storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(flow.getId(), oldStatus,
                ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus(),
                retryCount, calculateProcessingLeaseTime(), "");
        if (claimed == 0) {
            return false;
        }
        try {
            ProductStoreSkuUpdateStockReqDTO.Item item = new ProductStoreSkuUpdateStockReqDTO.Item();
            item.setSkuId(flow.getSkuId());
            item.setIncrCount(flow.getIncrCount());
            applyStoreSkuStock(flow.getStoreId(), item);
            int updated = storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(flow.getId(),
                    ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus(),
                    ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus(),
                    retryCount, null, null);
            return updated > 0;
        } catch (RuntimeException ex) {
            int nextRetryCount = retryCount + 1;
            storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(flow.getId(),
                    ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus(),
                    ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus(),
                    nextRetryCount, calculateNextRetryTime(nextRetryCount), trimErrorMsg(ex.getMessage()));
            return false;
        }
    }

    private ProductStoreSkuStockFlowDO getOrCreateStockFlow(String bizType, String bizNo, Long storeId,
                                                            Long skuId, Integer incrCount) {
        ProductStoreSkuStockFlowDO exist = storeSkuStockFlowMapper.selectByBizKey(bizType, bizNo, storeId, skuId);
        if (exist != null) {
            validateStockFlowIncrCount(exist, incrCount);
            if (exist.getRetryCount() == null) {
                exist.setRetryCount(0);
            }
            return exist;
        }
        ProductStoreSkuStockFlowDO flow = ProductStoreSkuStockFlowDO.builder()
                .bizType(bizType)
                .bizNo(bizNo)
                .storeId(storeId)
                .skuId(skuId)
                .incrCount(incrCount)
                .status(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus())
                .retryCount(0)
                .nextRetryTime(LocalDateTime.now())
                .lastErrorMsg("")
                .build();
        try {
            storeSkuStockFlowMapper.insert(flow);
        } catch (DuplicateKeyException ignore) {
            // 并发场景下命中唯一键，重新读取已有流水即可
        }
        ProductStoreSkuStockFlowDO latest = storeSkuStockFlowMapper.selectByBizKey(bizType, bizNo, storeId, skuId);
        if (latest == null) {
            latest = flow;
        } else if (latest.getRetryCount() == null) {
            latest.setRetryCount(0);
        }
        validateStockFlowIncrCount(latest, incrCount);
        return latest;
    }

    private List<ProductStoreSkuUpdateStockReqDTO.Item> normalizeManualStockItems(
            List<ProductStoreSkuManualStockAdjustReqVO.Item> items,
            ProductStoreSkuManualStockBizTypeEnum bizTypeEnum) {
        if (items == null || items.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Set<Long> skuIdSet = new HashSet<>();
        List<ProductStoreSkuUpdateStockReqDTO.Item> normalizedItems = new ArrayList<>(items.size());
        for (ProductStoreSkuManualStockAdjustReqVO.Item item : items) {
            if (item == null || item.getSkuId() == null) {
                throw exception(STORE_SKU_STOCK_MANUAL_INCR_COUNT_INVALID, "skuId 不能为空");
            }
            if (!skuIdSet.add(item.getSkuId())) {
                throw exception(STORE_SKU_STOCK_MANUAL_SKU_DUPLICATED, item.getSkuId());
            }
            if (item.getIncrCount() == null || !bizTypeEnum.supportsIncrCount(item.getIncrCount())) {
                String directionTip = bizTypeEnum == ProductStoreSkuManualStockBizTypeEnum.STOCKTAKE
                        ? "非 0"
                        : (bizTypeEnum.supportsIncrCount(1) ? "正数" : "负数");
                throw exception(STORE_SKU_STOCK_MANUAL_INCR_COUNT_INVALID,
                        StrUtil.format("bizType={} 要求 {}", bizTypeEnum.getCode(), directionTip));
            }
            ProductStoreSkuUpdateStockReqDTO.Item stockItem = new ProductStoreSkuUpdateStockReqDTO.Item();
            stockItem.setSkuId(item.getSkuId());
            stockItem.setIncrCount(item.getIncrCount());
            normalizedItems.add(stockItem);
        }
        return normalizedItems;
    }

    private void applyStoreSkuStock(Long storeId, ProductStoreSkuUpdateStockReqDTO.Item item) {
        ProductStoreSkuDO current = storeSkuMapper.selectByStoreIdAndSkuId(storeId, item.getSkuId());
        if (current == null) {
            throw exception(STORE_SKU_MAPPING_NOT_EXISTS);
        }
        if (current.getSpuId() != null) {
            ProductSpuDO spu = productSpuService.getSpu(current.getSpuId());
            if (spu != null && ProductTypeEnum.isService(spu.getProductType())) {
                throw exception(STORE_SKU_STOCK_SERVICE_FORBIDDEN, item.getSkuId());
            }
        }
        if (item.getIncrCount() > 0) {
            int updateCount = storeSkuMapper.updateStockIncrByStoreIdAndSkuId(storeId, item.getSkuId(), item.getIncrCount());
            if (updateCount == 0) {
                throw exception(STORE_SKU_MAPPING_NOT_EXISTS);
            }
            return;
        }
        int decrCount = -item.getIncrCount();
        if (current.getStock() == null || current.getStock() < decrCount) {
            throw exception(SKU_STOCK_NOT_ENOUGH);
        }
        int updateCount = storeSkuMapper.updateStockDecrByStoreIdAndSkuId(storeId, item.getSkuId(), decrCount);
        if (updateCount == 0) {
            throw exception(SKU_STOCK_NOT_ENOUGH);
        }
    }

    private static String normalizeStockBizField(String field, String fieldName) {
        String normalized = StrUtil.trim(field);
        if (StrUtil.isBlank(normalized)) {
            throw exception(STORE_SKU_STOCK_BIZ_KEY_REQUIRED);
        }
        if (normalized.length() > 64) {
            throw exception(STORE_SKU_STOCK_BIZ_FIELD_TOO_LONG, fieldName, 64);
        }
        return normalized;
    }

    private static LocalDateTime calculateNextRetryTime(int retryCount) {
        int multiplier = 1 << Math.max(0, Math.min(retryCount - 1, 6));
        return LocalDateTime.now().plusSeconds((long) STOCK_RETRY_BASE_SECONDS * multiplier);
    }

    private static String trimErrorMsg(String msg) {
        return StrUtil.maxLength(StrUtil.blankToDefault(msg, ""), 255);
    }

    private static LocalDateTime calculateProcessingLeaseTime() {
        return LocalDateTime.now().plusSeconds(STOCK_PROCESSING_LEASE_SECONDS);
    }

    private static Integer normalizeRetryCount(Integer retryCount) {
        return retryCount == null ? 0 : retryCount;
    }

    private static void validateStockFlowIncrCount(ProductStoreSkuStockFlowDO flow, Integer incrCount) {
        if (flow == null) {
            return;
        }
        if (!Objects.equals(flow.getIncrCount(), incrCount)) {
            throw exception(STORE_SKU_STOCK_BIZ_KEY_CONFLICT);
        }
    }

    private ProductSpuDO validateSpuExists(Long spuId) {
        ProductSpuDO spu = productSpuService.getSpu(spuId);
        if (spu == null) {
            throw exception(SPU_NOT_EXISTS);
        }
        return spu;
    }

    private void validateStoreSpuExists(Long id) {
        if (storeSpuMapper.selectById(id) == null) {
            throw exception(STORE_SPU_MAPPING_NOT_EXISTS);
        }
    }

    private void validateStoreSkuExists(Long id) {
        if (storeSkuMapper.selectById(id) == null) {
            throw exception(STORE_SKU_MAPPING_NOT_EXISTS);
        }
    }

    private void ensureStoreSpuMapping(Long storeId, ProductSpuDO spu) {
        ProductStoreSpuDO existing = storeSpuMapper.selectByStoreIdAndSpuId(storeId, spu.getId());
        if (existing != null) {
            return;
        }
        ProductStoreSpuDO ensureObj = ProductStoreSpuDO.builder()
                .storeId(storeId)
                .spuId(spu.getId())
                .productType(spu.getProductType())
                .saleStatus(SALE_STATUS_ENABLE)
                .sort(0)
                .remark("auto-created-by-store-sku")
                .build();
        ProductStoreSpuDO deleted = storeSpuMapper.selectByStoreIdAndSpuIdIncludeDeleted(storeId, spu.getId());
        if (deleted != null) {
            ensureObj.setId(deleted.getId());
            storeSpuMapper.recoverById(ensureObj);
            return;
        }
        storeSpuMapper.insert(ensureObj);
    }

    private String buildSkuSpecText(ProductSkuDO sku) {
        if (sku.getProperties() == null || sku.getProperties().isEmpty()) {
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

    private List<Long> normalizeStoreIds(List<Long> storeIds) {
        Set<Long> normalized = new LinkedHashSet<>();
        if (storeIds != null) {
            storeIds.stream().filter(Objects::nonNull).forEach(normalized::add);
        }
        if (normalized.isEmpty()) {
            throw exception(STORE_BATCH_TARGETS_EMPTY);
        }
        return normalized.stream().collect(Collectors.toList());
    }

    private List<Long> normalizeFlowIds(List<Long> flowIds) {
        Set<Long> normalized = new LinkedHashSet<>();
        if (flowIds != null) {
            flowIds.stream().filter(Objects::nonNull).forEach(normalized::add);
        }
        if (normalized.isEmpty()) {
            throw exception(STORE_SKU_STOCK_FLOW_TARGETS_EMPTY);
        }
        return normalized.stream().collect(Collectors.toList());
    }
}
