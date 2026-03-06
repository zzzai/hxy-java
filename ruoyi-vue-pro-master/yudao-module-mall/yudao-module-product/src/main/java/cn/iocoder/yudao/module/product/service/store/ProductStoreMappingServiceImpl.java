package cn.iocoder.yudao.module.product.service.store;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuUpdateStockReqDTO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchAdjustReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuStockAdjustOrderActionReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuStockAdjustOrderCreateReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuStockAdjustOrderPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuTransferOrderActionReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuTransferOrderCreateReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuTransferOrderPageReqVO;
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
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuStockAdjustOrderDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuStockFlowDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuTransferOrderDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSpuDO;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuStockAdjustOrderMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuStockFlowMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuTransferOrderMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSpuMapper;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuStockAdjustOrderStatusEnum;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuManualStockBizTypeEnum;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuStockFlowStatusEnum;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuTransferOrderStatusEnum;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.product.enums.spu.ProductSpuStatusEnum;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import cn.iocoder.yudao.module.product.service.store.dto.ProductStoreSkuStockFlowBatchRetryResult;
import cn.iocoder.yudao.module.trade.api.reviewticket.TradeReviewTicketApi;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketUpsertReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class ProductStoreMappingServiceImpl implements ProductStoreMappingService {

    private static final Integer SALE_STATUS_ENABLE = 0;
    private static final int OPTION_LIMIT = 200;
    private static final int STOCK_RETRY_DEFAULT_LIMIT = 100;
    private static final int STOCK_RETRY_MAX_LIMIT = 1000;
    private static final int STOCK_RETRY_BASE_SECONDS = 30;
    private static final int STOCK_PROCESSING_LEASE_SECONDS = 120;
    private static final String STOCK_RETRY_SOURCE_DEFAULT = "ADMIN_UI";
    private static final String STOCK_RETRY_OPERATOR_DEFAULT = "SYSTEM";
    private static final String STOCK_ADJUST_SOURCE_DEFAULT = "ADMIN_UI";
    private static final String STOCK_ADJUST_ACTION_CREATE = "CREATE";
    private static final String STOCK_ADJUST_ACTION_SUBMIT = "SUBMIT";
    private static final String STOCK_ADJUST_ACTION_APPROVE = "APPROVE";
    private static final String STOCK_ADJUST_ACTION_REJECT = "REJECT";
    private static final String STOCK_ADJUST_ACTION_CANCEL = "CANCEL";
    private static final String TRANSFER_ORDER_SOURCE_DEFAULT = "ADMIN_UI";
    private static final String TRANSFER_ORDER_ACTION_CREATE = "CREATE";
    private static final String TRANSFER_ORDER_ACTION_SUBMIT = "SUBMIT";
    private static final String TRANSFER_ORDER_ACTION_APPROVE = "APPROVE";
    private static final String TRANSFER_ORDER_ACTION_REJECT = "REJECT";
    private static final String TRANSFER_ORDER_ACTION_CANCEL = "CANCEL";
    private static final String STOCKTAKE_AUDIT_ENABLED_CONFIG_KEY = "product.stocktake.audit-ticket.enabled";
    private static final String STOCKTAKE_AUDIT_THRESHOLD_CONFIG_KEY = "product.stocktake.audit-threshold";
    private static final String STOCKTAKE_AUDIT_ENABLED_CONFIG_KEY_LEGACY = "hxy.product.stocktake.audit-ticket.enabled";
    private static final String STOCKTAKE_AUDIT_THRESHOLD_CONFIG_KEY_LEGACY = "hxy.product.stocktake.audit-threshold";
    private static final boolean STOCKTAKE_AUDIT_ENABLED_DEFAULT = true;
    private static final int STOCKTAKE_AUDIT_THRESHOLD_DEFAULT = 20;
    private static final int STOCKTAKE_AUDIT_THRESHOLD_MIN = 1;
    private static final int STOCKTAKE_AUDIT_THRESHOLD_MAX = 1000000;
    private static final int STOCKTAKE_AUDIT_TICKET_TYPE = 60;
    private static final String STOCKTAKE_AUDIT_SOURCE_BIZ_PREFIX = "STOCKTAKE_AUDIT:";
    private static final String STOCKTAKE_AUDIT_RULE_CODE = "STOCKTAKE_DIFF_AUDIT";
    private static final String STOCKTAKE_AUDIT_ACTION_CODE = "STOCKTAKE_AUDIT_TRIGGER";
    private static final String STOCKTAKE_AUDIT_SEVERITY = "P1";
    private static final DateTimeFormatter STOCK_ADJUST_ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TRANSFER_ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private ProductStoreSpuMapper storeSpuMapper;
    @Resource
    private ProductStoreSkuMapper storeSkuMapper;
    @Resource
    private ProductStoreSkuStockFlowMapper storeSkuStockFlowMapper;
    @Resource
    private ProductStoreSkuStockAdjustOrderMapper stockAdjustOrderMapper;
    @Resource
    private ProductStoreSkuTransferOrderMapper transferOrderMapper;
    @Resource
    private ProductSpuService productSpuService;
    @Resource
    private ProductSkuService productSkuService;
    @Resource
    private ProductStoreService productStoreService;
    @Resource
    private ConfigApi configApi;
    @Resource
    private TradeReviewTicketApi tradeReviewTicketApi;

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
        normalizeStockFlowPageReq(reqVO);
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
    @Transactional(rollbackFor = Exception.class)
    public Long createStockAdjustOrder(ProductStoreSkuStockAdjustOrderCreateReqVO reqVO) {
        ProductStoreDO store = validateStoreExistsAndGet(reqVO.getStoreId());
        ProductStoreSkuManualStockBizTypeEnum bizTypeEnum = ProductStoreSkuManualStockBizTypeEnum
                .valueOfCode(reqVO.getBizType());
        if (bizTypeEnum == null) {
            throw exception(STORE_SKU_STOCK_MANUAL_BIZ_TYPE_INVALID, reqVO.getBizType());
        }
        List<ProductStoreSkuUpdateStockReqDTO.Item> detailItems = normalizeManualStockItems(
                convertStockAdjustCreateItems(reqVO.getItems()), bizTypeEnum);
        LocalDateTime now = LocalDateTime.now();
        String operator = resolveStockAdjustOperator();
        ProductStoreSkuStockAdjustOrderDO order = ProductStoreSkuStockAdjustOrderDO.builder()
                .orderNo(buildStockAdjustOrderNo())
                .storeId(store.getId())
                .storeName(store.getName())
                .bizType(bizTypeEnum.getCode())
                .reason(StrUtil.trim(reqVO.getReason()))
                .remark(StrUtil.trimToEmpty(reqVO.getRemark()))
                .status(ProductStoreSkuStockAdjustOrderStatusEnum.DRAFT.getStatus())
                .detailJson(JsonUtils.toJsonString(detailItems))
                .applyOperator(operator)
                .applySource(normalizeStockAdjustApplySource(reqVO.getApplySource()))
                .lastActionCode(STOCK_ADJUST_ACTION_CREATE)
                .lastActionOperator(operator)
                .lastActionTime(now)
                .build();
        stockAdjustOrderMapper.insert(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitStockAdjustOrder(ProductStoreSkuStockAdjustOrderActionReqVO reqVO) {
        ProductStoreSkuStockAdjustOrderDO order = validateStockAdjustOrderStatus(
                reqVO.getId(), ProductStoreSkuStockAdjustOrderStatusEnum.DRAFT.getStatus());
        LocalDateTime now = LocalDateTime.now();
        ProductStoreSkuStockAdjustOrderDO updateObj = ProductStoreSkuStockAdjustOrderDO.builder()
                .id(order.getId())
                .status(ProductStoreSkuStockAdjustOrderStatusEnum.PENDING.getStatus())
                .lastActionCode(STOCK_ADJUST_ACTION_SUBMIT)
                .lastActionOperator(resolveStockAdjustOperator())
                .lastActionTime(now)
                .build();
        stockAdjustOrderMapper.updateStatusByIdAndOldStatus(updateObj,
                ProductStoreSkuStockAdjustOrderStatusEnum.DRAFT.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveStockAdjustOrder(ProductStoreSkuStockAdjustOrderActionReqVO reqVO) {
        ProductStoreSkuStockAdjustOrderDO order = validateStockAdjustOrderStatus(
                reqVO.getId(), ProductStoreSkuStockAdjustOrderStatusEnum.PENDING.getStatus());
        ProductStoreSkuManualStockBizTypeEnum bizTypeEnum = ProductStoreSkuManualStockBizTypeEnum
                .valueOfCode(order.getBizType());
        if (bizTypeEnum == null) {
            throw exception(STORE_SKU_STOCK_MANUAL_BIZ_TYPE_INVALID, order.getBizType());
        }
        List<ProductStoreSkuManualStockAdjustReqVO.Item> detailItems = parseStockAdjustDetailItems(order.getDetailJson());
        // 再次复核明细方向与 SKU 唯一性，避免草稿阶段与审批阶段之间数据被污染。
        List<ProductStoreSkuUpdateStockReqDTO.Item> normalizedItems = normalizeManualStockItems(detailItems, bizTypeEnum);

        ProductStoreSkuUpdateStockReqDTO stockReq = new ProductStoreSkuUpdateStockReqDTO();
        stockReq.setStoreId(order.getStoreId());
        stockReq.setBizType(bizTypeEnum.getStockFlowBizType());
        stockReq.setBizNo(order.getOrderNo());
        stockReq.setItems(normalizedItems);
        updateStoreSkuStock(stockReq);

        LocalDateTime now = LocalDateTime.now();
        ProductStoreSkuStockAdjustOrderDO updateObj = ProductStoreSkuStockAdjustOrderDO.builder()
                .id(order.getId())
                .status(ProductStoreSkuStockAdjustOrderStatusEnum.APPROVED.getStatus())
                .approveOperator(resolveStockAdjustOperator())
                .approveRemark(StrUtil.trimToEmpty(reqVO.getRemark()))
                .approveTime(now)
                .lastActionCode(STOCK_ADJUST_ACTION_APPROVE)
                .lastActionOperator(resolveStockAdjustOperator())
                .lastActionTime(now)
                .build();
        stockAdjustOrderMapper.updateStatusByIdAndOldStatus(updateObj,
                ProductStoreSkuStockAdjustOrderStatusEnum.PENDING.getStatus());
        triggerStocktakeAuditTicketIfNeeded(order, normalizedItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectStockAdjustOrder(ProductStoreSkuStockAdjustOrderActionReqVO reqVO) {
        ProductStoreSkuStockAdjustOrderDO order = validateStockAdjustOrderStatus(
                reqVO.getId(), ProductStoreSkuStockAdjustOrderStatusEnum.PENDING.getStatus());
        LocalDateTime now = LocalDateTime.now();
        ProductStoreSkuStockAdjustOrderDO updateObj = ProductStoreSkuStockAdjustOrderDO.builder()
                .id(order.getId())
                .status(ProductStoreSkuStockAdjustOrderStatusEnum.REJECTED.getStatus())
                .approveOperator(resolveStockAdjustOperator())
                .approveRemark(StrUtil.trimToEmpty(reqVO.getRemark()))
                .approveTime(now)
                .lastActionCode(STOCK_ADJUST_ACTION_REJECT)
                .lastActionOperator(resolveStockAdjustOperator())
                .lastActionTime(now)
                .build();
        stockAdjustOrderMapper.updateStatusByIdAndOldStatus(updateObj,
                ProductStoreSkuStockAdjustOrderStatusEnum.PENDING.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelStockAdjustOrder(ProductStoreSkuStockAdjustOrderActionReqVO reqVO) {
        ProductStoreSkuStockAdjustOrderDO order = getStockAdjustOrder(reqVO.getId());
        Integer status = order.getStatus();
        if (!Objects.equals(status, ProductStoreSkuStockAdjustOrderStatusEnum.DRAFT.getStatus())
                && !Objects.equals(status, ProductStoreSkuStockAdjustOrderStatusEnum.PENDING.getStatus())) {
            throw exception(STORE_SKU_STOCK_ADJUST_ORDER_STATUS_INVALID, status,
                    ProductStoreSkuStockAdjustOrderStatusEnum.DRAFT.getStatus() + "/"
                            + ProductStoreSkuStockAdjustOrderStatusEnum.PENDING.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        ProductStoreSkuStockAdjustOrderDO updateObj = ProductStoreSkuStockAdjustOrderDO.builder()
                .id(order.getId())
                .status(ProductStoreSkuStockAdjustOrderStatusEnum.CANCELLED.getStatus())
                .approveOperator(resolveStockAdjustOperator())
                .approveRemark(StrUtil.trimToEmpty(reqVO.getRemark()))
                .approveTime(now)
                .lastActionCode(STOCK_ADJUST_ACTION_CANCEL)
                .lastActionOperator(resolveStockAdjustOperator())
                .lastActionTime(now)
                .build();
        stockAdjustOrderMapper.updateStatusByIdAndOldStatus(updateObj, status);
    }

    @Override
    public ProductStoreSkuStockAdjustOrderDO getStockAdjustOrder(Long id) {
        ProductStoreSkuStockAdjustOrderDO order = stockAdjustOrderMapper.selectById(id);
        if (order == null) {
            throw exception(STORE_SKU_STOCK_ADJUST_ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public PageResult<ProductStoreSkuStockAdjustOrderDO> getStockAdjustOrderPage(
            ProductStoreSkuStockAdjustOrderPageReqVO reqVO) {
        ProductStoreSkuStockAdjustOrderPageReqVO normalizedReq = normalizeStockAdjustOrderPageReq(reqVO);
        return stockAdjustOrderMapper.selectPage(normalizedReq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTransferOrder(ProductStoreSkuTransferOrderCreateReqVO reqVO) {
        ProductStoreDO fromStore = validateStoreExistsAndGet(reqVO.getFromStoreId());
        ProductStoreDO toStore = validateStoreExistsAndGet(reqVO.getToStoreId());
        if (Objects.equals(fromStore.getId(), toStore.getId())) {
            throw exception(STORE_SKU_TRANSFER_ORDER_STORE_INVALID);
        }
        List<ProductStoreSkuTransferOrderCreateReqVO.Item> detailItems = normalizeTransferCreateItems(reqVO.getItems());
        LocalDateTime now = LocalDateTime.now();
        String operator = resolveStockAdjustOperator();
        ProductStoreSkuTransferOrderDO order = ProductStoreSkuTransferOrderDO.builder()
                .orderNo(buildTransferOrderNo())
                .fromStoreId(fromStore.getId())
                .fromStoreName(fromStore.getName())
                .toStoreId(toStore.getId())
                .toStoreName(toStore.getName())
                .reason(StrUtil.trim(reqVO.getReason()))
                .remark(StrUtil.trimToEmpty(reqVO.getRemark()))
                .status(ProductStoreSkuTransferOrderStatusEnum.DRAFT.getStatus())
                .detailJson(JsonUtils.toJsonString(detailItems))
                .applyOperator(operator)
                .applySource(normalizeTransferApplySource(reqVO.getApplySource()))
                .lastActionCode(TRANSFER_ORDER_ACTION_CREATE)
                .lastActionOperator(operator)
                .lastActionTime(now)
                .build();
        transferOrderMapper.insert(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTransferOrder(ProductStoreSkuTransferOrderActionReqVO reqVO) {
        ProductStoreSkuTransferOrderDO order = validateTransferOrderStatus(
                reqVO.getId(), ProductStoreSkuTransferOrderStatusEnum.DRAFT.getStatus());
        LocalDateTime now = LocalDateTime.now();
        ProductStoreSkuTransferOrderDO updateObj = ProductStoreSkuTransferOrderDO.builder()
                .id(order.getId())
                .status(ProductStoreSkuTransferOrderStatusEnum.PENDING.getStatus())
                .lastActionCode(TRANSFER_ORDER_ACTION_SUBMIT)
                .lastActionOperator(resolveStockAdjustOperator())
                .lastActionTime(now)
                .build();
        transferOrderMapper.updateStatusByIdAndOldStatus(updateObj,
                ProductStoreSkuTransferOrderStatusEnum.DRAFT.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTransferOrder(ProductStoreSkuTransferOrderActionReqVO reqVO) {
        ProductStoreSkuTransferOrderDO order = validateTransferOrderStatus(
                reqVO.getId(), ProductStoreSkuTransferOrderStatusEnum.PENDING.getStatus());
        ProductStoreDO fromStore = validateStoreExistsAndGet(order.getFromStoreId());
        ProductStoreDO toStore = validateStoreExistsAndGet(order.getToStoreId());
        if (Objects.equals(fromStore.getId(), toStore.getId())) {
            throw exception(STORE_SKU_TRANSFER_ORDER_STORE_INVALID);
        }

        List<ProductStoreSkuTransferOrderCreateReqVO.Item> detailItems = parseTransferOrderDetailItems(order.getDetailJson());
        List<ProductStoreSkuUpdateStockReqDTO.Item> outItems = buildTransferStockUpdateItems(
                detailItems, ProductStoreSkuManualStockBizTypeEnum.TRANSFER_OUT);
        List<ProductStoreSkuUpdateStockReqDTO.Item> inItems = buildTransferStockUpdateItems(
                detailItems, ProductStoreSkuManualStockBizTypeEnum.TRANSFER_IN);

        ProductStoreSkuUpdateStockReqDTO outReq = new ProductStoreSkuUpdateStockReqDTO();
        outReq.setStoreId(order.getFromStoreId());
        outReq.setBizType(ProductStoreSkuManualStockBizTypeEnum.TRANSFER_OUT.getStockFlowBizType());
        outReq.setBizNo(order.getOrderNo());
        outReq.setItems(outItems);
        updateStoreSkuStock(outReq);

        ProductStoreSkuUpdateStockReqDTO inReq = new ProductStoreSkuUpdateStockReqDTO();
        inReq.setStoreId(order.getToStoreId());
        inReq.setBizType(ProductStoreSkuManualStockBizTypeEnum.TRANSFER_IN.getStockFlowBizType());
        inReq.setBizNo(order.getOrderNo());
        inReq.setItems(inItems);
        updateStoreSkuStock(inReq);

        LocalDateTime now = LocalDateTime.now();
        ProductStoreSkuTransferOrderDO updateObj = ProductStoreSkuTransferOrderDO.builder()
                .id(order.getId())
                .status(ProductStoreSkuTransferOrderStatusEnum.APPROVED.getStatus())
                .approveOperator(resolveStockAdjustOperator())
                .approveRemark(StrUtil.trimToEmpty(reqVO.getRemark()))
                .approveTime(now)
                .lastActionCode(TRANSFER_ORDER_ACTION_APPROVE)
                .lastActionOperator(resolveStockAdjustOperator())
                .lastActionTime(now)
                .build();
        transferOrderMapper.updateStatusByIdAndOldStatus(updateObj,
                ProductStoreSkuTransferOrderStatusEnum.PENDING.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTransferOrder(ProductStoreSkuTransferOrderActionReqVO reqVO) {
        ProductStoreSkuTransferOrderDO order = validateTransferOrderStatus(
                reqVO.getId(), ProductStoreSkuTransferOrderStatusEnum.PENDING.getStatus());
        LocalDateTime now = LocalDateTime.now();
        ProductStoreSkuTransferOrderDO updateObj = ProductStoreSkuTransferOrderDO.builder()
                .id(order.getId())
                .status(ProductStoreSkuTransferOrderStatusEnum.REJECTED.getStatus())
                .approveOperator(resolveStockAdjustOperator())
                .approveRemark(StrUtil.trimToEmpty(reqVO.getRemark()))
                .approveTime(now)
                .lastActionCode(TRANSFER_ORDER_ACTION_REJECT)
                .lastActionOperator(resolveStockAdjustOperator())
                .lastActionTime(now)
                .build();
        transferOrderMapper.updateStatusByIdAndOldStatus(updateObj,
                ProductStoreSkuTransferOrderStatusEnum.PENDING.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTransferOrder(ProductStoreSkuTransferOrderActionReqVO reqVO) {
        ProductStoreSkuTransferOrderDO order = getTransferOrder(reqVO.getId());
        Integer status = order.getStatus();
        if (!Objects.equals(status, ProductStoreSkuTransferOrderStatusEnum.DRAFT.getStatus())
                && !Objects.equals(status, ProductStoreSkuTransferOrderStatusEnum.PENDING.getStatus())) {
            throw exception(STORE_SKU_TRANSFER_ORDER_STATUS_INVALID, status,
                    ProductStoreSkuTransferOrderStatusEnum.DRAFT.getStatus() + "/"
                            + ProductStoreSkuTransferOrderStatusEnum.PENDING.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        ProductStoreSkuTransferOrderDO updateObj = ProductStoreSkuTransferOrderDO.builder()
                .id(order.getId())
                .status(ProductStoreSkuTransferOrderStatusEnum.CANCELLED.getStatus())
                .approveOperator(resolveStockAdjustOperator())
                .approveRemark(StrUtil.trimToEmpty(reqVO.getRemark()))
                .approveTime(now)
                .lastActionCode(TRANSFER_ORDER_ACTION_CANCEL)
                .lastActionOperator(resolveStockAdjustOperator())
                .lastActionTime(now)
                .build();
        transferOrderMapper.updateStatusByIdAndOldStatus(updateObj, status);
    }

    @Override
    public ProductStoreSkuTransferOrderDO getTransferOrder(Long id) {
        ProductStoreSkuTransferOrderDO order = transferOrderMapper.selectById(id);
        if (order == null) {
            throw exception(STORE_SKU_TRANSFER_ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public PageResult<ProductStoreSkuTransferOrderDO> getTransferOrderPage(ProductStoreSkuTransferOrderPageReqVO reqVO) {
        ProductStoreSkuTransferOrderPageReqVO normalizedReq = normalizeTransferOrderPageReq(reqVO);
        return transferOrderMapper.selectPage(normalizedReq);
    }

    @Override
    public int retryStoreSkuStockFlow(Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? STOCK_RETRY_DEFAULT_LIMIT : Math.min(limit, STOCK_RETRY_MAX_LIMIT);
        LocalDateTime now = LocalDateTime.now();
        storeSkuStockFlowMapper.markProcessingTimeoutAsFailed(now, "processing-lease-timeout");
        List<ProductStoreSkuStockFlowDO> retryableList = storeSkuStockFlowMapper.selectRetryableList(now, safeLimit);
        int successCount = 0;
        for (ProductStoreSkuStockFlowDO flow : retryableList) {
            RetryExecutionResult executionResult = executeRetryFlow(flow, null, null);
            if (executionResult.isSuccess()) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductStoreSkuStockFlowBatchRetryResult retryStoreSkuStockFlowByIds(List<Long> flowIds,
                                                                                 String retryOperator,
                                                                                 String retrySource) {
        List<Long> normalizedFlowIds = normalizeFlowIds(flowIds);
        if (normalizedFlowIds.isEmpty()) {
            return ProductStoreSkuStockFlowBatchRetryResult.empty();
        }
        String normalizedRetryOperator = normalizeRetryOperator(retryOperator);
        String normalizedRetrySource = normalizeRetrySource(retrySource);
        List<ProductStoreSkuStockFlowDO> flowList = storeSkuStockFlowMapper.selectBatchIds(normalizedFlowIds);
        if (flowList == null) {
            flowList = Collections.emptyList();
        }
        Map<Long, ProductStoreSkuStockFlowDO> flowMap = flowList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ProductStoreSkuStockFlowDO::getId, item -> item, (o1, o2) -> o1));
        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<ProductStoreSkuStockFlowBatchRetryResult.Item> items = new ArrayList<>(normalizedFlowIds.size());
        for (Long flowId : normalizedFlowIds) {
            ProductStoreSkuStockFlowDO flow = flowMap.get(flowId);
            RetryExecutionResult executionResult = executeRetryFlow(flow, normalizedRetryOperator, normalizedRetrySource);
            if (executionResult.isSuccess()) {
                successCount++;
            } else if (executionResult.isSkipped()) {
                skippedCount++;
            } else {
                failedCount++;
            }
            items.add(ProductStoreSkuStockFlowBatchRetryResult.Item.builder()
                    .id(flowId)
                    .storeId(flow == null ? null : flow.getStoreId())
                    .skuId(flow == null ? null : flow.getSkuId())
                    .resultType(executionResult.getResultType())
                    .reason(executionResult.getReason())
                    .status(executionResult.getStatus())
                    .retryOperator(normalizedRetryOperator)
                    .retrySource(normalizedRetrySource)
                    .build());
        }
        return ProductStoreSkuStockFlowBatchRetryResult.builder()
                .totalCount(normalizedFlowIds.size())
                .successCount(successCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .items(items)
                .build();
    }

    private RetryExecutionResult executeRetryFlow(ProductStoreSkuStockFlowDO flow, String retryOperator,
                                                  String retrySource) {
        if (flow == null) {
            return RetryExecutionResult.skipped("NOT_FOUND", null);
        }
        if (!ProductStoreSkuStockFlowStatusEnum.isRetryable(flow.getStatus())) {
            return RetryExecutionResult.skipped("STATUS_NOT_RETRYABLE", flow.getStatus());
        }
        Integer oldStatus = flow.getStatus() == null
                ? ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus() : flow.getStatus();
        int retryCount = normalizeRetryCount(flow.getRetryCount());
        int claimed = storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(flow.getId(), oldStatus,
                ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus(),
                retryCount, calculateProcessingLeaseTime(), "", retryOperator, retrySource);
        if (claimed == 0) {
            return RetryExecutionResult.skipped("CLAIM_CONFLICT", oldStatus);
        }
        try {
            ProductStoreSkuUpdateStockReqDTO.Item item = new ProductStoreSkuUpdateStockReqDTO.Item();
            item.setSkuId(flow.getSkuId());
            item.setIncrCount(flow.getIncrCount());
            applyStoreSkuStock(flow.getStoreId(), item);
            int updated = storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(flow.getId(),
                    ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus(),
                    ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus(),
                    retryCount, null, null, retryOperator, retrySource);
            if (updated > 0) {
                return RetryExecutionResult.success(ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus());
            }
            return RetryExecutionResult.failed("STATUS_UPDATE_CONFLICT",
                    ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus());
        } catch (RuntimeException ex) {
            int nextRetryCount = retryCount + 1;
            storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(flow.getId(),
                    ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus(),
                    ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus(),
                    nextRetryCount, calculateNextRetryTime(nextRetryCount), trimErrorMsg(ex.getMessage()),
                    retryOperator, retrySource);
            return RetryExecutionResult.failed(StrUtil.format("APPLY_FAILED:{}", trimErrorMsg(ex.getMessage())),
                    ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus());
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

    private ProductStoreDO validateStoreExistsAndGet(Long storeId) {
        productStoreService.validateStoreExists(storeId);
        ProductStoreDO store = productStoreService.getStore(storeId);
        if (store == null) {
            throw exception(STORE_NOT_EXISTS);
        }
        return store;
    }

    private ProductStoreSkuStockAdjustOrderDO validateStockAdjustOrderStatus(Long id, Integer expectStatus) {
        ProductStoreSkuStockAdjustOrderDO order = getStockAdjustOrder(id);
        if (!Objects.equals(order.getStatus(), expectStatus)) {
            throw exception(STORE_SKU_STOCK_ADJUST_ORDER_STATUS_INVALID, order.getStatus(), expectStatus);
        }
        return order;
    }

    private ProductStoreSkuTransferOrderDO validateTransferOrderStatus(Long id, Integer expectStatus) {
        ProductStoreSkuTransferOrderDO order = getTransferOrder(id);
        if (!Objects.equals(order.getStatus(), expectStatus)) {
            throw exception(STORE_SKU_TRANSFER_ORDER_STATUS_INVALID, order.getStatus(), expectStatus);
        }
        return order;
    }

    private List<ProductStoreSkuManualStockAdjustReqVO.Item> convertStockAdjustCreateItems(
            List<ProductStoreSkuStockAdjustOrderCreateReqVO.Item> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductStoreSkuManualStockAdjustReqVO.Item> converted = new ArrayList<>(items.size());
        for (ProductStoreSkuStockAdjustOrderCreateReqVO.Item item : items) {
            ProductStoreSkuManualStockAdjustReqVO.Item convertedItem = new ProductStoreSkuManualStockAdjustReqVO.Item();
            if (item != null) {
                convertedItem.setSkuId(item.getSkuId());
                convertedItem.setIncrCount(item.getIncrCount());
            }
            converted.add(convertedItem);
        }
        return converted;
    }

    private List<ProductStoreSkuManualStockAdjustReqVO.Item> parseStockAdjustDetailItems(String detailJson) {
        try {
            List<ProductStoreSkuManualStockAdjustReqVO.Item> items = JsonUtils.parseArray(detailJson,
                    ProductStoreSkuManualStockAdjustReqVO.Item.class);
            if (items == null || items.isEmpty()) {
                throw exception(STORE_SKU_STOCK_ADJUST_ORDER_DETAIL_INVALID);
            }
            return items;
        } catch (RuntimeException ex) {
            throw exception(STORE_SKU_STOCK_ADJUST_ORDER_DETAIL_INVALID);
        }
    }

    private List<ProductStoreSkuTransferOrderCreateReqVO.Item> normalizeTransferCreateItems(
            List<ProductStoreSkuTransferOrderCreateReqVO.Item> items) {
        if (items == null || items.isEmpty()) {
            throw exception(STORE_SKU_TRANSFER_ORDER_DETAIL_INVALID);
        }
        Set<Long> skuIdSet = new HashSet<>();
        List<ProductStoreSkuTransferOrderCreateReqVO.Item> normalizedItems = new ArrayList<>(items.size());
        for (ProductStoreSkuTransferOrderCreateReqVO.Item item : items) {
            if (item == null || item.getSkuId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw exception(STORE_SKU_TRANSFER_ORDER_DETAIL_INVALID);
            }
            if (!skuIdSet.add(item.getSkuId())) {
                throw exception(STORE_SKU_TRANSFER_ORDER_DETAIL_INVALID);
            }
            ProductStoreSkuTransferOrderCreateReqVO.Item normalizedItem = new ProductStoreSkuTransferOrderCreateReqVO.Item();
            normalizedItem.setSkuId(item.getSkuId());
            normalizedItem.setQuantity(item.getQuantity());
            normalizedItems.add(normalizedItem);
        }
        return normalizedItems;
    }

    private List<ProductStoreSkuTransferOrderCreateReqVO.Item> parseTransferOrderDetailItems(String detailJson) {
        try {
            List<ProductStoreSkuTransferOrderCreateReqVO.Item> items = JsonUtils.parseArray(detailJson,
                    ProductStoreSkuTransferOrderCreateReqVO.Item.class);
            return normalizeTransferCreateItems(items);
        } catch (RuntimeException ex) {
            throw exception(STORE_SKU_TRANSFER_ORDER_DETAIL_INVALID);
        }
    }

    private List<ProductStoreSkuUpdateStockReqDTO.Item> buildTransferStockUpdateItems(
            List<ProductStoreSkuTransferOrderCreateReqVO.Item> detailItems,
            ProductStoreSkuManualStockBizTypeEnum bizTypeEnum) {
        List<ProductStoreSkuManualStockAdjustReqVO.Item> manualItems = new ArrayList<>(detailItems.size());
        for (ProductStoreSkuTransferOrderCreateReqVO.Item detailItem : detailItems) {
            ProductStoreSkuManualStockAdjustReqVO.Item item = new ProductStoreSkuManualStockAdjustReqVO.Item();
            item.setSkuId(detailItem.getSkuId());
            int qty = detailItem.getQuantity();
            item.setIncrCount(bizTypeEnum == ProductStoreSkuManualStockBizTypeEnum.TRANSFER_OUT ? -qty : qty);
            manualItems.add(item);
        }
        return normalizeManualStockItems(manualItems, bizTypeEnum);
    }

    private ProductStoreSkuStockAdjustOrderPageReqVO normalizeStockAdjustOrderPageReq(
            ProductStoreSkuStockAdjustOrderPageReqVO reqVO) {
        if (reqVO == null) {
            return new ProductStoreSkuStockAdjustOrderPageReqVO();
        }
        reqVO.setOrderNo(normalizeStockFlowPageTrim(reqVO.getOrderNo()));
        reqVO.setApplyOperator(normalizeStockFlowPageTrim(reqVO.getApplyOperator()));
        reqVO.setLastActionOperator(normalizeStockFlowPageTrim(reqVO.getLastActionOperator()));
        reqVO.setBizType(normalizeStockFlowPageUppercase(reqVO.getBizType()));
        reqVO.setLastActionCode(normalizeStockFlowPageUppercase(reqVO.getLastActionCode()));
        return reqVO;
    }

    private ProductStoreSkuTransferOrderPageReqVO normalizeTransferOrderPageReq(
            ProductStoreSkuTransferOrderPageReqVO reqVO) {
        if (reqVO == null) {
            return new ProductStoreSkuTransferOrderPageReqVO();
        }
        reqVO.setOrderNo(normalizeStockFlowPageTrim(reqVO.getOrderNo()));
        reqVO.setApplyOperator(normalizeStockFlowPageTrim(reqVO.getApplyOperator()));
        reqVO.setLastActionOperator(normalizeStockFlowPageTrim(reqVO.getLastActionOperator()));
        reqVO.setLastActionCode(normalizeStockFlowPageUppercase(reqVO.getLastActionCode()));
        return reqVO;
    }

    private String resolveStockAdjustOperator() {
        String nickname = StrUtil.trim(getLoginUserNickname());
        if (StrUtil.isNotBlank(nickname)) {
            return nickname;
        }
        Long loginUserId = getLoginUserId();
        return loginUserId == null ? STOCK_RETRY_OPERATOR_DEFAULT : String.valueOf(loginUserId);
    }

    private String normalizeStockAdjustApplySource(String source) {
        String normalized = StrUtil.trim(source);
        if (StrUtil.isBlank(normalized)) {
            return STOCK_ADJUST_SOURCE_DEFAULT;
        }
        return StrUtil.maxLength(normalized.toUpperCase(Locale.ROOT), 32);
    }

    private String normalizeTransferApplySource(String source) {
        String normalized = StrUtil.trim(source);
        if (StrUtil.isBlank(normalized)) {
            return TRANSFER_ORDER_SOURCE_DEFAULT;
        }
        return StrUtil.maxLength(normalized.toUpperCase(Locale.ROOT), 32);
    }

    private String buildStockAdjustOrderNo() {
        String timePart = LocalDateTime.now().format(STOCK_ADJUST_ORDER_NO_FORMATTER);
        String random = StrUtil.sub(java.util.UUID.randomUUID().toString().replace("-", ""), 0, 8)
                .toUpperCase(Locale.ROOT);
        return "SAO-" + timePart + "-" + random;
    }

    private String buildTransferOrderNo() {
        String timePart = LocalDateTime.now().format(TRANSFER_ORDER_NO_FORMATTER);
        String random = StrUtil.sub(java.util.UUID.randomUUID().toString().replace("-", ""), 0, 8)
                .toUpperCase(Locale.ROOT);
        return "STO-" + timePart + "-" + random;
    }

    private void triggerStocktakeAuditTicketIfNeeded(ProductStoreSkuStockAdjustOrderDO order,
                                                     List<ProductStoreSkuUpdateStockReqDTO.Item> normalizedItems) {
        if (order == null || normalizedItems == null || normalizedItems.isEmpty()) {
            return;
        }
        if (!StrUtil.equalsIgnoreCase(order.getBizType(), ProductStoreSkuManualStockBizTypeEnum.STOCKTAKE.getCode())) {
            return;
        }
        if (!resolveStocktakeAuditEnabled()) {
            return;
        }
        int threshold = resolveStocktakeAuditThreshold();
        int diffCount = normalizedItems.stream()
                .map(ProductStoreSkuUpdateStockReqDTO.Item::getIncrCount)
                .filter(Objects::nonNull)
                .mapToInt(Math::abs)
                .sum();
        if (diffCount <= threshold) {
            return;
        }
        String sourceBizNo = STOCKTAKE_AUDIT_SOURCE_BIZ_PREFIX + order.getOrderNo();
        TradeReviewTicketUpsertReqDTO reqDTO = new TradeReviewTicketUpsertReqDTO()
                .setTicketType(STOCKTAKE_AUDIT_TICKET_TYPE)
                .setSourceBizNo(sourceBizNo)
                .setRuleCode(STOCKTAKE_AUDIT_RULE_CODE)
                .setSeverity(STOCKTAKE_AUDIT_SEVERITY)
                .setDecisionReason(StrUtil.format("盘点差异超阈值，diffCount={}，threshold={}", diffCount, threshold))
                .setRemark(buildStocktakeAuditRemark(order, diffCount, threshold, normalizedItems))
                .setActionCode(STOCKTAKE_AUDIT_ACTION_CODE);
        try {
            tradeReviewTicketApi.upsertReviewTicket(reqDTO);
        } catch (Exception ex) {
            // fail-open：工单联动失败不阻断库存审批主链路。
            log.warn("[triggerStocktakeAuditTicketIfNeeded][upsert failed, sourceBizNo={}]", sourceBizNo, ex);
        }
    }

    private String buildStocktakeAuditRemark(ProductStoreSkuStockAdjustOrderDO order, int diffCount, int threshold,
                                             List<ProductStoreSkuUpdateStockReqDTO.Item> normalizedItems) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("storeId", order.getStoreId());
        payload.put("storeName", order.getStoreName());
        payload.put("diffCount", diffCount);
        payload.put("threshold", threshold);
        payload.put("itemCount", normalizedItems.size());
        payload.put("items", normalizedItems);
        return JsonUtils.toJsonString(payload);
    }

    private boolean resolveStocktakeAuditEnabled() {
        String value = getConfigValueByKeyWithLegacy(STOCKTAKE_AUDIT_ENABLED_CONFIG_KEY,
                STOCKTAKE_AUDIT_ENABLED_CONFIG_KEY_LEGACY);
        if (StrUtil.isBlank(value)) {
            return STOCKTAKE_AUDIT_ENABLED_DEFAULT;
        }
        String normalized = StrUtil.trim(value).toLowerCase(Locale.ROOT);
        if ("1".equals(normalized) || "yes".equals(normalized) || "y".equals(normalized)) {
            return true;
        }
        if ("0".equals(normalized) || "no".equals(normalized) || "n".equals(normalized)) {
            return false;
        }
        return Boolean.parseBoolean(normalized);
    }

    private int resolveStocktakeAuditThreshold() {
        String value = getConfigValueByKeyWithLegacy(STOCKTAKE_AUDIT_THRESHOLD_CONFIG_KEY,
                STOCKTAKE_AUDIT_THRESHOLD_CONFIG_KEY_LEGACY);
        if (StrUtil.isBlank(value)) {
            return STOCKTAKE_AUDIT_THRESHOLD_DEFAULT;
        }
        try {
            int parsed = Integer.parseInt(StrUtil.trim(value));
            if (parsed < STOCKTAKE_AUDIT_THRESHOLD_MIN || parsed > STOCKTAKE_AUDIT_THRESHOLD_MAX) {
                return STOCKTAKE_AUDIT_THRESHOLD_DEFAULT;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return STOCKTAKE_AUDIT_THRESHOLD_DEFAULT;
        }
    }

    private String getConfigValueByKeyWithLegacy(String configKey, String legacyConfigKey) {
        try {
            String value = configApi.getConfigValueByKey(configKey);
            if (StrUtil.isNotBlank(value) || StrUtil.isBlank(legacyConfigKey)) {
                return value;
            }
        } catch (Exception ex) {
            log.warn("[getConfigValueByKeyWithLegacy][query configKey({}) failed]", configKey, ex);
        }
        if (StrUtil.isBlank(legacyConfigKey)) {
            return null;
        }
        try {
            return configApi.getConfigValueByKey(legacyConfigKey);
        } catch (Exception ex) {
            log.warn("[getConfigValueByKeyWithLegacy][query legacyConfigKey({}) failed]", legacyConfigKey, ex);
            return null;
        }
    }

    private ProductSpuDO validateSpuExists(Long spuId) {
        ProductSpuDO spu = productSpuService.getSpu(spuId);
        if (spu == null) {
            throw exception(SPU_NOT_EXISTS);
        }
        return spu;
    }

    private void normalizeStockFlowPageReq(ProductStoreSkuStockFlowPageReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        reqVO.setBizType(normalizeStockFlowPageUppercase(reqVO.getBizType()));
        reqVO.setSource(normalizeStockFlowPageUppercase(reqVO.getSource()));
        reqVO.setBizNo(normalizeStockFlowPageTrim(reqVO.getBizNo()));
        reqVO.setOperator(normalizeStockFlowPageTrim(reqVO.getOperator()));
    }

    private String normalizeStockFlowPageTrim(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private String normalizeStockFlowPageUppercase(String value) {
        String normalized = normalizeStockFlowPageTrim(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
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

    private static String normalizeRetryOperator(String retryOperator) {
        String normalized = StrUtil.trimToEmpty(retryOperator);
        if (StrUtil.isBlank(normalized)) {
            return STOCK_RETRY_OPERATOR_DEFAULT;
        }
        return StrUtil.maxLength(normalized, 64);
    }

    private static String normalizeRetrySource(String retrySource) {
        String normalized = StrUtil.trimToEmpty(retrySource);
        if (StrUtil.isBlank(normalized)) {
            return STOCK_RETRY_SOURCE_DEFAULT;
        }
        return StrUtil.maxLength(normalized.toUpperCase(), 32);
    }

    private static class RetryExecutionResult {
        private final String resultType;
        private final String reason;
        private final Integer status;

        private RetryExecutionResult(String resultType, String reason, Integer status) {
            this.resultType = resultType;
            this.reason = reason;
            this.status = status;
        }

        public static RetryExecutionResult success(Integer status) {
            return new RetryExecutionResult("SUCCESS", "SUCCESS", status);
        }

        public static RetryExecutionResult skipped(String reason, Integer status) {
            return new RetryExecutionResult("SKIPPED", reason, status);
        }

        public static RetryExecutionResult failed(String reason, Integer status) {
            return new RetryExecutionResult("FAILED", reason, status);
        }

        public boolean isSuccess() {
            return "SUCCESS".equals(resultType);
        }

        public boolean isSkipped() {
            return "SKIPPED".equals(resultType);
        }

        public String getResultType() {
            return resultType;
        }

        public String getReason() {
            return reason;
        }

        public Integer getStatus() {
            return status;
        }
    }
}
