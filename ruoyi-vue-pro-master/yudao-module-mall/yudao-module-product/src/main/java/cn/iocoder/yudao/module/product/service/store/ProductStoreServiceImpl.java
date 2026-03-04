package cn.iocoder.yudao.module.product.service.store;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.*;
import cn.iocoder.yudao.module.product.dal.dataobject.store.*;
import cn.iocoder.yudao.module.product.dal.mysql.store.*;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreLifecycleStatusEnum;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuStockFlowStatusEnum;
import cn.iocoder.yudao.module.trade.api.store.TradeStoreLifecycleGuardApi;
import cn.iocoder.yudao.module.trade.api.store.dto.TradeStoreLifecycleGuardStatRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ProductStoreServiceImpl implements ProductStoreService {

    private static final Integer STATUS_ENABLE = 1;
    private static final Integer STATUS_DISABLE = 0;
    private static final Integer FLAG_TRUE = 1;
    private static final Integer DEFAULT_LIFECYCLE = 10;
    private static final String GUARD_MODE_BLOCK = "BLOCK";
    private static final String GUARD_MODE_WARN = "WARN";
    private static final String GUARD_WARN_REASON_PREFIX = "LIFECYCLE_GUARD_WARN:";
    private static final String GUARD_MODE_MAPPING = "hxy.store.lifecycle.guard.mapping.mode";
    private static final String GUARD_MODE_STOCK = "hxy.store.lifecycle.guard.stock.mode";
    private static final String GUARD_MODE_STOCK_FLOW = "hxy.store.lifecycle.guard.stock-flow.mode";
    private static final String GUARD_MODE_STOCK_FLOW_PENDING = "hxy.store.lifecycle.guard.stock-flow.pending.mode";
    private static final String GUARD_MODE_STOCK_FLOW_PROCESSING = "hxy.store.lifecycle.guard.stock-flow.processing.mode";
    private static final String GUARD_MODE_STOCK_FLOW_FAILED = "hxy.store.lifecycle.guard.stock-flow.failed.mode";
    private static final String GUARD_MODE_PENDING_ORDER = "hxy.store.lifecycle.guard.pending-order.mode";
    private static final String GUARD_MODE_INFLIGHT_TICKET = "hxy.store.lifecycle.guard.inflight-ticket.mode";

    private static final String DOMAIN_STORE = "STORE";
    private static final String DOMAIN_CATEGORY = "CATEGORY";
    private static final String DOMAIN_TAG = "TAG";
    private static final String DOMAIN_TAG_GROUP = "TAG_GROUP";
    private static final String SOURCE_ADMIN_UI = "ADMIN_UI";
    private static final DateTimeFormatter BATCH_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private ProductStoreMapper storeMapper;
    @Resource
    private ProductStoreCategoryMapper storeCategoryMapper;
    @Resource
    private ProductStoreTagMapper storeTagMapper;
    @Resource
    private ProductStoreTagRelMapper storeTagRelMapper;
    @Resource
    private ProductStoreSpuMapper storeSpuMapper;
    @Resource
    private ProductStoreSkuMapper storeSkuMapper;
    @Resource
    private ProductStoreSkuStockFlowMapper storeSkuStockFlowMapper;
    @Resource
    private ProductStoreTagGroupMapper storeTagGroupMapper;
    @Resource
    private ProductStoreAuditLogMapper storeAuditLogMapper;
    @Resource
    private TradeStoreLifecycleGuardApi tradeStoreLifecycleGuardApi;
    @Resource
    private ConfigApi configApi;
    private ProductStoreLifecycleBatchLogService lifecycleBatchLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveStore(ProductStoreSaveReqVO reqVO) {
        Long categoryId = reqVO.getCategoryId();
        validateCategoryExists(categoryId);
        validateStoreCodeUnique(reqVO.getId(), reqVO.getCode());
        List<ProductStoreTagDO> tags = validateTagIds(reqVO.getTagIds());
        validateTagGroupRules(tags);
        if (reqVO.getId() == null) {
            ProductStoreDO store = buildStoreDO(reqVO);
            storeMapper.insert(store);
            syncStoreTags(store.getId(), tags.stream().map(ProductStoreTagDO::getId).collect(Collectors.toList()));
            saveAuditLog(DOMAIN_STORE, store.getId(), "CREATE", null, store, reqVO.getRemark());
            return store.getId();
        }
        ProductStoreDO existing = validateStoreExistsAndGet(reqVO.getId());
        ProductStoreDO updateObj = buildStoreDO(reqVO);
        updateObj.setId(reqVO.getId());
        LifecycleGuardEvaluation guardEvaluation = evaluateDisableOrCloseAllowed(
                updateObj.getStatus(), updateObj.getLifecycleStatus(), reqVO.getId());
        throwIfLifecycleGuardBlocked(guardEvaluation);
        validateLifecycleTransition(existing, updateObj.getLifecycleStatus());
        validateLifecycleReasonRequired(updateObj.getLifecycleStatus(), reqVO.getRemark());
        storeMapper.updateById(updateObj);
        syncStoreTags(reqVO.getId(), tags.stream().map(ProductStoreTagDO::getId).collect(Collectors.toList()));
        saveAuditLog(DOMAIN_STORE, reqVO.getId(), "UPDATE", existing, updateObj,
                appendGuardWarnReason(reqVO.getRemark(), guardEvaluation.getWarnings()));
        return reqVO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStore(Long id) {
        ProductStoreDO old = validateStoreExistsAndGet(id);
        if (storeSpuMapper.selectCountByStoreId(id) > 0 || storeSkuMapper.selectCountByStoreId(id) > 0) {
            throw exception(STORE_HAS_PRODUCT_MAPPING);
        }
        storeTagRelMapper.deleteByStoreId(id);
        storeMapper.deleteById(id);
        saveAuditLog(DOMAIN_STORE, id, "DELETE", old, null, "删除门店");
    }

    @Override
    public ProductStoreDO getStore(Long id) {
        return storeMapper.selectById(id);
    }

    @Override
    public Map<Long, ProductStoreDO> getStoreMap(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<ProductStoreDO> stores = storeMapper.selectBatchIds(ids);
        if (CollUtil.isEmpty(stores)) {
            return Collections.emptyMap();
        }
        return stores.stream().collect(Collectors.toMap(ProductStoreDO::getId, store -> store));
    }

    @Override
    public PageResult<ProductStoreDO> getStorePage(ProductStorePageReqVO reqVO) {
        return storeMapper.selectPage(reqVO);
    }

    @Override
    public List<ProductStoreOptionRespVO> getStoreOptions(String keyword) {
        List<ProductStoreDO> list = storeMapper.selectSimpleList(keyword, STATUS_ENABLE);
        return list.stream()
                .map(store -> {
                    ProductStoreOptionRespVO option = new ProductStoreOptionRespVO();
                    option.setId(store.getId());
                    option.setName(store.getName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductStoreSimpleRespVO> getStoreSimpleList(String keyword) {
        List<ProductStoreDO> list = storeMapper.selectSimpleList(keyword, STATUS_ENABLE);
        return BeanUtils.toBean(list, ProductStoreSimpleRespVO.class);
    }

    @Override
    public List<Long> getStoreTagIds(Long storeId) {
        List<ProductStoreTagRelDO> relations = storeTagRelMapper.selectListByStoreId(storeId);
        if (CollUtil.isEmpty(relations)) {
            return Collections.emptyList();
        }
        return relations.stream()
                .map(ProductStoreTagRelDO::getTagId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void validateStoreExists(Long id) {
        validateStoreExistsAndGet(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCategory(ProductStoreCategorySaveReqVO reqVO) {
        validateCategoryCodeUnique(reqVO.getId(), reqVO.getCode());
        validateCategoryNameUnique(reqVO.getId(), reqVO.getName());

        ProductStoreCategoryDO obj = BeanUtils.toBean(reqVO, ProductStoreCategoryDO.class);
        normalizeCategoryHierarchy(reqVO, obj);
        obj.setCode(normalizeCode(reqVO.getCode()));
        if (obj.getStatus() == null) {
            obj.setStatus(STATUS_ENABLE);
        }
        if (obj.getSort() == null) {
            obj.setSort(0);
        }
        if (!StringUtils.hasText(obj.getRemark())) {
            obj.setRemark("");
        }
        if (reqVO.getId() == null) {
            storeCategoryMapper.insert(obj);
            saveAuditLog(DOMAIN_CATEGORY, obj.getId(), "CREATE", null, obj, reqVO.getRemark());
            return obj.getId();
        }
        validateCategoryExists(reqVO.getId());
        ProductStoreCategoryDO old = storeCategoryMapper.selectById(reqVO.getId());
        obj.setId(reqVO.getId());
        storeCategoryMapper.updateById(obj);
        saveAuditLog(DOMAIN_CATEGORY, obj.getId(), "UPDATE", old, obj, reqVO.getRemark());
        return obj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        ProductStoreCategoryDO old = storeCategoryMapper.selectById(id);
        validateCategoryExists(id);
        if (storeMapper.selectCount(ProductStoreDO::getCategoryId, id) > 0) {
            throw exception(STORE_CATEGORY_HAS_STORE);
        }
        if (storeCategoryMapper.selectCount(ProductStoreCategoryDO::getParentId, id) > 0) {
            throw exception(STORE_CATEGORY_HAS_STORE);
        }
        storeCategoryMapper.deleteById(id);
        saveAuditLog(DOMAIN_CATEGORY, id, "DELETE", old, null, "删除分类");
    }

    @Override
    public ProductStoreCategoryDO getCategory(Long id) {
        return storeCategoryMapper.selectById(id);
    }

    @Override
    public List<ProductStoreCategoryDO> getCategoryList(ProductStoreCategoryListReqVO reqVO) {
        return storeCategoryMapper.selectList(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTag(ProductStoreTagSaveReqVO reqVO) {
        validateTagCodeUnique(reqVO.getId(), reqVO.getCode());
        validateTagNameUnique(reqVO.getId(), reqVO.getName());

        ProductStoreTagGroupDO group = resolveTagGroup(reqVO.getGroupId(), reqVO.getGroupName());
        ProductStoreTagDO obj = BeanUtils.toBean(reqVO, ProductStoreTagDO.class);
        obj.setCode(normalizeCode(reqVO.getCode()));
        obj.setGroupId(group.getId());
        obj.setGroupName(group.getName());
        if (obj.getStatus() == null) {
            obj.setStatus(STATUS_ENABLE);
        }
        if (obj.getSort() == null) {
            obj.setSort(0);
        }
        if (!StringUtils.hasText(obj.getRemark())) {
            obj.setRemark("");
        }
        if (reqVO.getId() == null) {
            storeTagMapper.insert(obj);
            saveAuditLog(DOMAIN_TAG, obj.getId(), "CREATE", null, obj, reqVO.getRemark());
            return obj.getId();
        }
        validateTagExists(reqVO.getId());
        ProductStoreTagDO old = storeTagMapper.selectById(reqVO.getId());
        obj.setId(reqVO.getId());
        storeTagMapper.updateById(obj);
        saveAuditLog(DOMAIN_TAG, obj.getId(), "UPDATE", old, obj, reqVO.getRemark());
        return obj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        ProductStoreTagDO old = storeTagMapper.selectById(id);
        validateTagExists(id);
        if (storeTagRelMapper.selectCount(ProductStoreTagRelDO::getTagId, id) > 0) {
            throw exception(STORE_TAG_HAS_STORE);
        }
        storeTagMapper.deleteById(id);
        saveAuditLog(DOMAIN_TAG, id, "DELETE", old, null, "删除标签");
    }

    @Override
    public ProductStoreTagDO getTag(Long id) {
        return storeTagMapper.selectById(id);
    }

    @Override
    public List<ProductStoreTagDO> getTagList(ProductStoreTagListReqVO reqVO) {
        return storeTagMapper.selectList(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTagGroup(ProductStoreTagGroupSaveReqVO reqVO) {
        validateTagGroupCodeUnique(reqVO.getId(), reqVO.getCode());
        validateTagGroupNameUnique(reqVO.getId(), reqVO.getName());

        ProductStoreTagGroupDO obj = BeanUtils.toBean(reqVO, ProductStoreTagGroupDO.class);
        obj.setCode(normalizeCode(reqVO.getCode()));
        if (obj.getRequired() == null) {
            obj.setRequired(0);
        }
        if (obj.getMutex() == null) {
            obj.setMutex(0);
        }
        if (obj.getEditableByStore() == null) {
            obj.setEditableByStore(0);
        }
        if (obj.getStatus() == null) {
            obj.setStatus(STATUS_ENABLE);
        }
        if (obj.getSort() == null) {
            obj.setSort(0);
        }
        if (!StringUtils.hasText(obj.getRemark())) {
            obj.setRemark("");
        }
        if (reqVO.getId() == null) {
            storeTagGroupMapper.insert(obj);
            saveAuditLog(DOMAIN_TAG_GROUP, obj.getId(), "CREATE", null, obj, reqVO.getRemark());
            return obj.getId();
        }
        ProductStoreTagGroupDO old = validateTagGroupExistsAndGet(reqVO.getId());
        obj.setId(reqVO.getId());
        storeTagGroupMapper.updateById(obj);
        saveAuditLog(DOMAIN_TAG_GROUP, obj.getId(), "UPDATE", old, obj, reqVO.getRemark());
        return obj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTagGroup(Long id) {
        ProductStoreTagGroupDO old = validateTagGroupExistsAndGet(id);
        if (storeTagMapper.selectCount(ProductStoreTagDO::getGroupId, id) > 0) {
            throw exception(STORE_TAG_GROUP_HAS_TAG);
        }
        storeTagGroupMapper.deleteById(id);
        saveAuditLog(DOMAIN_TAG_GROUP, id, "DELETE", old, null, "删除标签组");
    }

    @Override
    public ProductStoreTagGroupDO getTagGroup(Long id) {
        return storeTagGroupMapper.selectById(id);
    }

    @Override
    public List<ProductStoreTagGroupDO> getTagGroupList(ProductStoreTagGroupListReqVO reqVO) {
        return storeTagGroupMapper.selectList(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStoreLifecycle(Long id, Integer lifecycleStatus, String reason) {
        ProductStoreDO store = validateStoreExistsAndGet(id);
        validateLifecycleStatus(lifecycleStatus);
        LifecycleGuardEvaluation guardEvaluation = evaluateDisableOrCloseAllowed(store.getStatus(), lifecycleStatus, id);
        throwIfLifecycleGuardBlocked(guardEvaluation);
        validateLifecycleTransition(store, lifecycleStatus);
        validateLifecycleReasonRequired(lifecycleStatus, reason);
        ProductStoreDO updateObj = ProductStoreDO.builder()
                .id(id)
                .lifecycleStatus(lifecycleStatus)
                .build();
        storeMapper.updateById(updateObj);
        ProductStoreDO after = storeMapper.selectById(id);
        saveAuditLog(DOMAIN_STORE, id, "LIFECYCLE", store, after, appendGuardWarnReason(reason, guardEvaluation.getWarnings()));
    }

    @Override
    public ProductStoreLifecycleGuardRespVO getLifecycleGuard(Long id, Integer lifecycleStatus) {
        ProductStoreDO store = validateStoreExistsAndGet(id);
        validateLifecycleStatus(lifecycleStatus);
        LifecycleGuardEvaluation guardEvaluation = evaluateDisableOrCloseAllowed(store.getStatus(), lifecycleStatus, id);
        ErrorCode transitionErrorCode = resolveLifecycleTransitionError(store, lifecycleStatus);
        if (transitionErrorCode != null && guardEvaluation.getBlockedErrorCode() == null) {
            guardEvaluation.setBlockedErrorCode(transitionErrorCode);
        }
        ProductStoreLifecycleGuardRespVO respVO = new ProductStoreLifecycleGuardRespVO();
        respVO.setStoreId(id);
        respVO.setTargetLifecycleStatus(lifecycleStatus);
        respVO.setBlocked(guardEvaluation.getBlockedErrorCode() != null);
        if (guardEvaluation.getBlockedErrorCode() != null) {
            respVO.setBlockedCode(guardEvaluation.getBlockedErrorCode().getCode());
            respVO.setBlockedMessage(guardEvaluation.getBlockedErrorCode().getMsg());
        }
        respVO.setWarnings(guardEvaluation.getWarnings());
        respVO.setGuardItems(guardEvaluation.getGuardItems());
        return respVO;
    }

    @Override
    public List<ProductStoreLifecycleGuardRespVO> getLifecycleGuardBatch(List<Long> storeIds, Integer lifecycleStatus) {
        validateLifecycleStatus(lifecycleStatus);
        List<Long> normalizedIds = normalizeIds(storeIds);
        if (CollUtil.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        List<ProductStoreLifecycleGuardRespVO> result = new ArrayList<>(normalizedIds.size());
        for (Long storeId : normalizedIds) {
            result.add(getLifecycleGuard(storeId, lifecycleStatus));
        }
        return result;
    }

    @Override
    public ProductStoreLaunchReadinessRespVO getLaunchReadiness(Long id) {
        ProductStoreDO store = validateStoreExistsAndGet(id);
        List<String> reasons = new ArrayList<>();
        if (!Objects.equals(store.getStatus(), STATUS_ENABLE)) {
            reasons.add("门店状态不是启用");
        }
        if (!Objects.equals(store.getLifecycleStatus(), ProductStoreLifecycleStatusEnum.OPERATING.getStatus())) {
            reasons.add("门店生命周期不是营业中");
        }
        ProductStoreCategoryDO category = storeCategoryMapper.selectById(store.getCategoryId());
        if (category == null) {
            reasons.add("门店分类不存在");
        } else if (!Objects.equals(category.getStatus(), STATUS_ENABLE)) {
            reasons.add("门店分类未启用");
        }
        if (!StringUtils.hasText(store.getContactMobile())) {
            reasons.add("联系人手机号缺失");
        }
        if (!StringUtils.hasText(store.getAddress())) {
            reasons.add("详细地址缺失");
        }
        if (!StringUtils.hasText(store.getOpeningTime()) || !StringUtils.hasText(store.getClosingTime())) {
            reasons.add("营业时间缺失");
        }
        try {
            List<ProductStoreTagDO> tags = validateTagIds(getStoreTagIds(id));
            validateTagGroupRules(tags);
        } catch (ServiceException ex) {
            reasons.add(ex.getMessage());
        }
        if (storeSpuMapper.selectCountByStoreId(id) <= 0 && storeSkuMapper.selectCountByStoreId(id) <= 0) {
            reasons.add("门店未配置商品映射");
        }
        ProductStoreLaunchReadinessRespVO respVO = new ProductStoreLaunchReadinessRespVO();
        respVO.setStoreId(id);
        respVO.setReasons(reasons);
        respVO.setReady(reasons.isEmpty());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateCategory(ProductStoreBatchCategoryReqVO reqVO) {
        validateCategoryExists(reqVO.getCategoryId());
        List<Long> storeIds = normalizeIds(reqVO.getStoreIds());
        for (Long storeId : storeIds) {
            ProductStoreDO old = validateStoreExistsAndGet(storeId);
            ProductStoreDO updateObj = ProductStoreDO.builder().id(storeId).categoryId(reqVO.getCategoryId()).build();
            storeMapper.updateById(updateObj);
            ProductStoreDO after = storeMapper.selectById(storeId);
            saveAuditLog(DOMAIN_STORE, storeId, "BATCH_CATEGORY", old, after, reqVO.getReason());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateTags(ProductStoreBatchTagReqVO reqVO) {
        List<Long> storeIds = normalizeIds(reqVO.getStoreIds());
        List<ProductStoreTagDO> tags = validateTagIds(reqVO.getTagIds());
        validateTagGroupRules(tags);
        List<Long> tagIds = tags.stream().map(ProductStoreTagDO::getId).collect(Collectors.toList());
        for (Long storeId : storeIds) {
            validateStoreExists(storeId);
            List<Long> beforeTagIds = getStoreTagIds(storeId);
            syncStoreTags(storeId, tagIds);
            Map<String, Object> before = Collections.singletonMap("tagIds", beforeTagIds);
            Map<String, Object> after = Collections.singletonMap("tagIds", tagIds);
            saveAuditLog(DOMAIN_STORE, storeId, "BATCH_TAG", before, after, reqVO.getReason());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateLifecycle(ProductStoreBatchLifecycleReqVO reqVO) {
        batchUpdateLifecycleWithResult(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductStoreBatchLifecycleExecuteRespVO batchUpdateLifecycleWithResult(ProductStoreBatchLifecycleReqVO reqVO) {
        validateLifecycleStatus(reqVO.getLifecycleStatus());
        List<Long> storeIds = normalizeIds(reqVO.getStoreIds());
        String batchNo = buildBatchNo();

        ProductStoreBatchLifecycleExecuteRespVO respVO = new ProductStoreBatchLifecycleExecuteRespVO();
        respVO.setBatchNo(batchNo);
        respVO.setTargetLifecycleStatus(reqVO.getLifecycleStatus());
        respVO.setTotalCount(storeIds.size());
        respVO.setSuccessCount(0);
        respVO.setBlockedCount(0);
        respVO.setWarningCount(0);

        List<ProductStoreBatchLifecycleExecuteRespVO.Detail> details = new ArrayList<>();
        for (Long storeId : storeIds) {
            ProductStoreDO store = storeMapper.selectById(storeId);
            if (store == null) {
                ProductStoreBatchLifecycleExecuteRespVO.Detail detail = buildDetail(storeId, null, "BLOCKED", "门店不存在");
                details.add(detail);
                respVO.setBlockedCount(respVO.getBlockedCount() + 1);
                continue;
            }
            if (Objects.equals(store.getLifecycleStatus(), reqVO.getLifecycleStatus())) {
                ProductStoreBatchLifecycleExecuteRespVO.Detail detail = buildDetail(storeId, store.getName(),
                        "WARNING", "门店已是目标生命周期状态");
                details.add(detail);
                respVO.setWarningCount(respVO.getWarningCount() + 1);
                continue;
            }
            ProductStoreLifecycleGuardRespVO guardResp = getLifecycleGuard(storeId, reqVO.getLifecycleStatus());
            String warningText = buildGuardWarningText(guardResp.getWarnings());
            if (Boolean.TRUE.equals(guardResp.getBlocked())) {
                String blockedMessage = StringUtils.hasText(guardResp.getBlockedMessage())
                        ? guardResp.getBlockedMessage()
                        : "门店生命周期守卫阻塞";
                ProductStoreBatchLifecycleExecuteRespVO.Detail detail = buildDetail(storeId, store.getName(),
                        "BLOCKED", blockedMessage);
                details.add(detail);
                respVO.setBlockedCount(respVO.getBlockedCount() + 1);
                continue;
            }
            try {
                updateStoreLifecycle(storeId, reqVO.getLifecycleStatus(), reqVO.getReason());
                String result = StringUtils.hasText(warningText) ? "WARNING" : "SUCCESS";
                String message = StringUtils.hasText(warningText)
                        ? "执行成功，守卫告警: " + warningText
                        : "执行成功";
                ProductStoreBatchLifecycleExecuteRespVO.Detail detail = buildDetail(storeId, store.getName(), result, message);
                details.add(detail);
                respVO.setSuccessCount(respVO.getSuccessCount() + 1);
                if (StringUtils.hasText(warningText)) {
                    respVO.setWarningCount(respVO.getWarningCount() + 1);
                }
            } catch (ServiceException ex) {
                ProductStoreBatchLifecycleExecuteRespVO.Detail detail = buildDetail(storeId, store.getName(),
                        "BLOCKED", ex.getMessage());
                details.add(detail);
                respVO.setBlockedCount(respVO.getBlockedCount() + 1);
            }
        }
        respVO.setDetails(details);

        String detailJson = buildLifecycleBatchDetailJson(details);
        ProductStoreLifecycleBatchLogDO batchLog = ProductStoreLifecycleBatchLogDO.builder()
                .batchNo(batchNo)
                .targetLifecycleStatus(reqVO.getLifecycleStatus())
                .totalCount(respVO.getTotalCount())
                .successCount(respVO.getSuccessCount())
                .blockedCount(respVO.getBlockedCount())
                .warningCount(respVO.getWarningCount())
                .auditSummary(buildAuditSummary(respVO))
                .detailJson(detailJson)
                .operator(resolveOperator())
                .source(SOURCE_ADMIN_UI)
                .build();
        lifecycleBatchLogService.createLifecycleBatchLog(batchLog);
        return respVO;
    }

    private String buildGuardWarningText(List<String> warnings) {
        if (CollUtil.isEmpty(warnings)) {
            return null;
        }
        return String.join(";", warnings);
    }

    private ProductStoreDO buildStoreDO(ProductStoreSaveReqVO reqVO) {
        ProductStoreDO store = BeanUtils.toBean(reqVO, ProductStoreDO.class);
        store.setCode(normalizeCode(reqVO.getCode()));
        if (store.getStatus() == null) {
            store.setStatus(STATUS_ENABLE);
        }
        if (store.getLifecycleStatus() == null) {
            store.setLifecycleStatus(DEFAULT_LIFECYCLE);
        }
        if (!ProductStoreLifecycleStatusEnum.isValid(store.getLifecycleStatus())) {
            throw exception(STORE_LIFECYCLE_STATUS_INVALID);
        }
        if (store.getSort() == null) {
            store.setSort(0);
        }
        if (!StringUtils.hasText(store.getRemark())) {
            store.setRemark("");
        }
        if (!StringUtils.hasText(store.getShortName())) {
            store.setShortName(store.getName());
        }
        return store;
    }

    private void syncStoreTags(Long storeId, List<Long> tagIds) {
        storeTagRelMapper.deleteByStoreId(storeId);
        if (CollUtil.isEmpty(tagIds)) {
            return;
        }
        for (Long tagId : tagIds) {
            ProductStoreTagRelDO relation = ProductStoreTagRelDO.builder()
                    .storeId(storeId)
                    .tagId(tagId)
                    .build();
            storeTagRelMapper.insert(relation);
        }
    }

    private List<ProductStoreTagDO> validateTagIds(List<Long> tagIds) {
        if (CollUtil.isEmpty(tagIds)) {
            return Collections.emptyList();
        }
        List<Long> normalized = normalizeIds(tagIds);
        if (CollUtil.isEmpty(normalized)) {
            return Collections.emptyList();
        }
        List<ProductStoreTagDO> tags = storeTagMapper.selectBatchIds(normalized);
        if (tags.size() != normalized.size()) {
            throw exception(STORE_TAG_NOT_EXISTS);
        }
        boolean hasInvalid = tags.stream().anyMatch(tag -> tag.getGroupId() == null || !Objects.equals(tag.getStatus(), STATUS_ENABLE));
        if (hasInvalid) {
            throw exception(STORE_TAG_NOT_EXISTS);
        }
        return tags;
    }

    private void validateTagGroupRules(List<ProductStoreTagDO> tags) {
        Map<Long, List<ProductStoreTagDO>> tagGroupMap = tags.stream()
                .filter(tag -> tag.getGroupId() != null)
                .collect(Collectors.groupingBy(ProductStoreTagDO::getGroupId));

        List<Long> selectedGroupIds = new ArrayList<>(tagGroupMap.keySet());
        List<ProductStoreTagGroupDO> selectedGroups = CollUtil.isEmpty(selectedGroupIds)
                ? Collections.emptyList()
                : Optional.ofNullable(storeTagGroupMapper.selectByIds(selectedGroupIds)).orElse(Collections.emptyList());
        if (selectedGroups.size() != selectedGroupIds.size()) {
            throw exception(STORE_TAG_GROUP_NOT_EXISTS);
        }
        Map<Long, ProductStoreTagGroupDO> selectedGroupMap = selectedGroups.stream()
                .collect(Collectors.toMap(ProductStoreTagGroupDO::getId, g -> g));

        List<ProductStoreTagGroupDO> requiredGroups = Optional.ofNullable(
                storeTagGroupMapper.selectRequiredGroups(STATUS_ENABLE)).orElse(Collections.emptyList());
        List<String> missingRequired = requiredGroups.stream()
                .filter(group -> !tagGroupMap.containsKey(group.getId()))
                .map(ProductStoreTagGroupDO::getName)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(missingRequired)) {
            throw exception(STORE_TAG_GROUP_REQUIRED_MISSING, String.join("、", missingRequired));
        }

        List<String> mutexConflicts = new ArrayList<>();
        for (Map.Entry<Long, List<ProductStoreTagDO>> entry : tagGroupMap.entrySet()) {
            ProductStoreTagGroupDO group = selectedGroupMap.get(entry.getKey());
            if (group != null && Objects.equals(group.getMutex(), FLAG_TRUE) && entry.getValue().size() > 1) {
                mutexConflicts.add(group.getName());
            }
        }
        if (CollUtil.isNotEmpty(mutexConflicts)) {
            throw exception(STORE_TAG_GROUP_MUTEX_CONFLICT, String.join("、", mutexConflicts));
        }
    }

    private void validateStoreCodeUnique(Long id, String code) {
        ProductStoreDO exists = storeMapper.selectByCode(normalizeCode(code));
        if (exists == null) {
            return;
        }
        if (id != null && Objects.equals(exists.getId(), id)) {
            return;
        }
        throw exception(STORE_CODE_EXISTS);
    }

    private void validateCategoryCodeUnique(Long id, String code) {
        ProductStoreCategoryDO exists = storeCategoryMapper.selectByCode(normalizeCode(code));
        if (exists == null) {
            return;
        }
        if (id != null && Objects.equals(exists.getId(), id)) {
            return;
        }
        throw exception(STORE_CATEGORY_CODE_EXISTS);
    }

    private void validateCategoryNameUnique(Long id, String name) {
        ProductStoreCategoryDO exists = storeCategoryMapper.selectOne(ProductStoreCategoryDO::getName, name);
        if (exists == null) {
            return;
        }
        if (id != null && Objects.equals(exists.getId(), id)) {
            return;
        }
        throw exception(STORE_CATEGORY_NAME_EXISTS);
    }

    private void validateCategoryExists(Long categoryId) {
        ProductStoreCategoryDO category = storeCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw exception(STORE_CATEGORY_NOT_EXISTS);
        }
    }

    private void normalizeCategoryHierarchy(ProductStoreCategorySaveReqVO reqVO, ProductStoreCategoryDO category) {
        Long parentId = reqVO.getParentId();
        if (parentId == null || parentId <= 0) {
            category.setParentId(0L);
            category.setLevel(1);
            return;
        }
        ProductStoreCategoryDO parent = storeCategoryMapper.selectById(parentId);
        if (parent == null || Objects.equals(parent.getId(), reqVO.getId())) {
            throw exception(STORE_CATEGORY_NOT_EXISTS);
        }
        if (parent.getLevel() != null && parent.getLevel() >= 2) {
            throw exception(STORE_CATEGORY_NOT_EXISTS);
        }
        category.setParentId(parentId);
        category.setLevel(2);
    }

    private void validateTagCodeUnique(Long id, String code) {
        ProductStoreTagDO exists = storeTagMapper.selectByCode(normalizeCode(code));
        if (exists == null) {
            return;
        }
        if (id != null && Objects.equals(exists.getId(), id)) {
            return;
        }
        throw exception(STORE_TAG_CODE_EXISTS);
    }

    private void validateTagNameUnique(Long id, String name) {
        ProductStoreTagDO exists = storeTagMapper.selectOne(ProductStoreTagDO::getName, name);
        if (exists == null) {
            return;
        }
        if (id != null && Objects.equals(exists.getId(), id)) {
            return;
        }
        throw exception(STORE_TAG_NAME_EXISTS);
    }

    private void validateTagExists(Long id) {
        ProductStoreTagDO tag = storeTagMapper.selectById(id);
        if (tag == null) {
            throw exception(STORE_TAG_NOT_EXISTS);
        }
    }

    private ProductStoreTagGroupDO resolveTagGroup(Long groupId, String groupName) {
        if (groupId != null) {
            ProductStoreTagGroupDO group = storeTagGroupMapper.selectById(groupId);
            if (group == null) {
                throw exception(STORE_TAG_GROUP_NOT_EXISTS);
            }
            return group;
        }
        if (StringUtils.hasText(groupName)) {
            ProductStoreTagGroupDO group = storeTagGroupMapper.selectOne(ProductStoreTagGroupDO::getName, groupName);
            if (group != null) {
                return group;
            }
        }
        throw exception(STORE_TAG_GROUP_NOT_EXISTS);
    }

    private void validateTagGroupCodeUnique(Long id, String code) {
        ProductStoreTagGroupDO exists = storeTagGroupMapper.selectByCode(normalizeCode(code));
        if (exists == null) {
            return;
        }
        if (id != null && Objects.equals(exists.getId(), id)) {
            return;
        }
        throw exception(STORE_TAG_GROUP_CODE_EXISTS);
    }

    private void validateTagGroupNameUnique(Long id, String name) {
        ProductStoreTagGroupDO exists = storeTagGroupMapper.selectOne(ProductStoreTagGroupDO::getName, name);
        if (exists == null) {
            return;
        }
        if (id != null && Objects.equals(exists.getId(), id)) {
            return;
        }
        throw exception(STORE_TAG_GROUP_NAME_EXISTS);
    }

    private ProductStoreTagGroupDO validateTagGroupExistsAndGet(Long id) {
        ProductStoreTagGroupDO group = storeTagGroupMapper.selectById(id);
        if (group == null) {
            throw exception(STORE_TAG_GROUP_NOT_EXISTS);
        }
        return group;
    }

    private ProductStoreDO validateStoreExistsAndGet(Long id) {
        ProductStoreDO store = storeMapper.selectById(id);
        if (store == null) {
            throw exception(STORE_NOT_EXISTS);
        }
        return store;
    }

    private void validateLifecycleStatus(Integer lifecycleStatus) {
        if (!ProductStoreLifecycleStatusEnum.isValid(lifecycleStatus)) {
            throw exception(STORE_LIFECYCLE_STATUS_INVALID);
        }
    }

    private void validateLifecycleTransition(ProductStoreDO store, Integer targetLifecycleStatus) {
        if (targetLifecycleStatus == null) {
            return;
        }
        Integer current = store.getLifecycleStatus();
        if (Objects.equals(current, targetLifecycleStatus) || current == null) {
            return;
        }
        Set<Integer> allowed = ProductStoreLifecycleStatusEnum.nextStatuses(current);
        if (!allowed.contains(targetLifecycleStatus)) {
            throw exception(STORE_LIFECYCLE_TRANSITION_NOT_ALLOWED, current, targetLifecycleStatus);
        }
    }

    private ErrorCode resolveLifecycleTransitionError(ProductStoreDO store, Integer targetLifecycleStatus) {
        try {
            validateLifecycleTransition(store, targetLifecycleStatus);
            return null;
        } catch (ServiceException ex) {
            return new ErrorCode(ex.getCode(), ex.getMessage());
        }
    }

    private LifecycleGuardEvaluation evaluateDisableOrCloseAllowed(Integer targetStatus, Integer targetLifecycleStatus,
                                                                   Long storeId) {
        LifecycleGuardEvaluation evaluation = new LifecycleGuardEvaluation();
        boolean disabling = Objects.equals(targetStatus, STATUS_DISABLE);
        boolean suspending = Objects.equals(targetLifecycleStatus, ProductStoreLifecycleStatusEnum.SUSPENDED.getStatus());
        boolean closing = Objects.equals(targetLifecycleStatus, ProductStoreLifecycleStatusEnum.CLOSED.getStatus());
        if (!(disabling || suspending || closing)) {
            return evaluation;
        }
        applyLifecycleGuardWithConfig("mapping",
                storeSpuMapper.selectCountByStoreId(storeId) + storeSkuMapper.selectCountByStoreId(storeId),
                GUARD_MODE_MAPPING,
                STORE_LIFECYCLE_CLOSE_BLOCKED_BY_MAPPING,
                evaluation);
        String stockMode = resolveGuardMode(GUARD_MODE_STOCK);
        applyLifecycleGuard("stock",
                storeSkuMapper.selectNonZeroStockCountByStoreId(storeId),
                stockMode,
                STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK,
                evaluation);
        long pendingFlowCount = countStoreStockFlowByStatuses(storeId,
                Collections.singletonList(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()));
        long processingFlowCount = countStoreStockFlowByStatuses(storeId,
                Collections.singletonList(ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus()));
        long failedFlowCount = countStoreStockFlowByStatuses(storeId,
                Collections.singletonList(ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus()));
        String pendingFlowMode = resolveGuardModeWithFallback(GUARD_MODE_STOCK_FLOW_PENDING, GUARD_MODE_STOCK_FLOW);
        String processingFlowMode = resolveGuardModeWithFallback(GUARD_MODE_STOCK_FLOW_PROCESSING, GUARD_MODE_STOCK_FLOW);
        String failedFlowMode = resolveGuardModeWithFallback(GUARD_MODE_STOCK_FLOW_FAILED, GUARD_MODE_STOCK_FLOW);
        long totalFlowCount = pendingFlowCount + processingFlowCount + failedFlowCount;
        appendLifecycleGuardDetail(evaluation, "stock-flow", totalFlowCount,
                resolveStockFlowAggregateMode(pendingFlowCount, pendingFlowMode,
                        processingFlowCount, processingFlowMode,
                        failedFlowCount, failedFlowMode));
        applyLifecycleGuard("stock-flow-pending",
                pendingFlowCount,
                pendingFlowMode,
                STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW,
                evaluation);
        applyLifecycleGuard("stock-flow-processing",
                processingFlowCount,
                processingFlowMode,
                STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW,
                evaluation);
        applyLifecycleGuard("stock-flow-failed",
                failedFlowCount,
                failedFlowMode,
                STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW,
                evaluation);
        TradeStoreLifecycleGuardStatRespDTO tradeStat = Optional.ofNullable(
                tradeStoreLifecycleGuardApi.getStoreLifecycleGuardStat(storeId)).orElse(new TradeStoreLifecycleGuardStatRespDTO());
        applyLifecycleGuardWithConfig("pending-order",
                Optional.ofNullable(tradeStat.getPendingOrderCount()).orElse(0L),
                GUARD_MODE_PENDING_ORDER,
                STORE_LIFECYCLE_CLOSE_BLOCKED_BY_PENDING_ORDER,
                evaluation);
        applyLifecycleGuardWithConfig("inflight-ticket",
                Optional.ofNullable(tradeStat.getInflightTicketCount()).orElse(0L),
                GUARD_MODE_INFLIGHT_TICKET,
                STORE_LIFECYCLE_CLOSE_BLOCKED_BY_INFLIGHT_TICKET,
                evaluation);
        return evaluation;
    }

    private void applyLifecycleGuardWithConfig(String guardKey, Long count, String configKey,
                                               ErrorCode errorCode,
                                               LifecycleGuardEvaluation evaluation) {
        applyLifecycleGuard(guardKey, count, resolveGuardMode(configKey), errorCode, evaluation);
    }

    private void applyLifecycleGuard(String guardKey, Long count, String mode,
                                     ErrorCode errorCode,
                                     LifecycleGuardEvaluation evaluation) {
        long guardCount = Optional.ofNullable(count).orElse(0L);
        boolean blocked = guardCount > 0 && GUARD_MODE_BLOCK.equals(mode);
        ProductStoreLifecycleGuardRespVO.GuardItem guardItem = new ProductStoreLifecycleGuardRespVO.GuardItem();
        guardItem.setGuardKey(guardKey);
        guardItem.setCount(guardCount);
        guardItem.setMode(mode);
        guardItem.setBlocked(blocked);
        evaluation.getGuardItems().add(guardItem);
        if (guardCount <= 0) {
            return;
        }
        if (GUARD_MODE_WARN.equals(mode)) {
            evaluation.getWarnings().add(GUARD_WARN_REASON_PREFIX + guardKey + ":count=" + guardCount);
            return;
        }
        if (evaluation.getBlockedErrorCode() == null) {
            evaluation.setBlockedErrorCode(errorCode);
        }
    }

    private void appendLifecycleGuardDetail(LifecycleGuardEvaluation evaluation, String guardKey,
                                            Long count, String mode) {
        long guardCount = Optional.ofNullable(count).orElse(0L);
        ProductStoreLifecycleGuardRespVO.GuardItem guardItem = new ProductStoreLifecycleGuardRespVO.GuardItem();
        guardItem.setGuardKey(guardKey);
        guardItem.setCount(guardCount);
        guardItem.setMode(mode);
        guardItem.setBlocked(guardCount > 0 && GUARD_MODE_BLOCK.equals(mode));
        evaluation.getGuardItems().add(guardItem);
    }

    private long countStoreStockFlowByStatuses(Long storeId, List<Integer> statuses) {
        return Optional.ofNullable(storeSkuStockFlowMapper.selectCountByStoreIdAndStatuses(storeId, statuses)).orElse(0L);
    }

    private void throwIfLifecycleGuardBlocked(LifecycleGuardEvaluation guardEvaluation) {
        if (guardEvaluation.getBlockedErrorCode() == null) {
            return;
        }
        throw exception(guardEvaluation.getBlockedErrorCode());
    }

    private String resolveGuardMode(String configKey) {
        return resolveGuardModeWithFallback(configKey, null);
    }

    private String resolveGuardModeWithFallback(String configKey, String fallbackConfigKey) {
        String mode = configApi.getConfigValueByKey(configKey);
        if (!StringUtils.hasText(mode) && StringUtils.hasText(fallbackConfigKey)) {
            mode = configApi.getConfigValueByKey(fallbackConfigKey);
        }
        return normalizeGuardMode(mode);
    }

    private String normalizeGuardMode(String mode) {
        String normalizedMode = Optional.ofNullable(mode).orElse(GUARD_MODE_BLOCK).trim().toUpperCase(Locale.ROOT);
        return GUARD_MODE_WARN.equals(normalizedMode) ? GUARD_MODE_WARN : GUARD_MODE_BLOCK;
    }

    private String resolveStockFlowAggregateMode(Long pendingCount, String pendingMode,
                                                 Long processingCount, String processingMode,
                                                 Long failedCount, String failedMode) {
        long pending = Optional.ofNullable(pendingCount).orElse(0L);
        long processing = Optional.ofNullable(processingCount).orElse(0L);
        long failed = Optional.ofNullable(failedCount).orElse(0L);
        boolean blocked = (pending > 0 && GUARD_MODE_BLOCK.equals(pendingMode))
                || (processing > 0 && GUARD_MODE_BLOCK.equals(processingMode))
                || (failed > 0 && GUARD_MODE_BLOCK.equals(failedMode));
        if (blocked) {
            return GUARD_MODE_BLOCK;
        }
        return pending + processing + failed > 0 ? GUARD_MODE_WARN : GUARD_MODE_BLOCK;
    }

    private String appendGuardWarnReason(String reason, List<String> warnings) {
        if (CollUtil.isEmpty(warnings)) {
            return reason;
        }
        String warnText = String.join(";", warnings);
        if (!StringUtils.hasText(reason)) {
            return warnText;
        }
        return reason + ";" + warnText;
    }

    private void validateLifecycleReasonRequired(Integer targetLifecycleStatus, String reason) {
        if (!Objects.equals(targetLifecycleStatus, ProductStoreLifecycleStatusEnum.SUSPENDED.getStatus())
                && !Objects.equals(targetLifecycleStatus, ProductStoreLifecycleStatusEnum.CLOSED.getStatus())) {
            return;
        }
        if (!StringUtils.hasText(reason)) {
            throw exception(STORE_LIFECYCLE_REASON_REQUIRED);
        }
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private static ProductStoreBatchLifecycleExecuteRespVO.Detail buildDetail(Long storeId, String storeName,
                                                                              String result, String message) {
        ProductStoreBatchLifecycleExecuteRespVO.Detail detail = new ProductStoreBatchLifecycleExecuteRespVO.Detail();
        detail.setStoreId(storeId);
        detail.setStoreName(storeName);
        detail.setResult(result);
        detail.setMessage(message);
        return detail;
    }

    private String buildLifecycleBatchDetailJson(List<ProductStoreBatchLifecycleExecuteRespVO.Detail> details) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("details", details);
        payload.put("blocked", details.stream()
                .filter(detail -> Objects.equals(detail.getResult(), "BLOCKED"))
                .collect(Collectors.toList()));
        payload.put("warnings", details.stream()
                .filter(detail -> Objects.equals(detail.getResult(), "WARNING"))
                .collect(Collectors.toList()));
        return JsonUtils.toJsonString(payload);
    }

    private static String buildBatchNo() {
        return "LIFECYCLE-" + LocalDateTime.now().format(BATCH_NO_TIME_FORMAT)
                + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String buildAuditSummary(ProductStoreBatchLifecycleExecuteRespVO respVO) {
        return String.format("total=%d,success=%d,blocked=%d,warning=%d",
                respVO.getTotalCount(), respVO.getSuccessCount(), respVO.getBlockedCount(), respVO.getWarningCount());
    }

    private String resolveOperator() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId == null ? "SYSTEM" : String.valueOf(userId);
    }

    private void saveAuditLog(String domain, Long domainId, String action,
                              Object beforeSnapshot, Object afterSnapshot, String reason) {
        ProductStoreAuditLogDO log = ProductStoreAuditLogDO.builder()
                .domain(domain)
                .domainId(domainId)
                .action(action)
                .beforeSnapshot(beforeSnapshot == null ? "" : JsonUtils.toJsonString(beforeSnapshot))
                .afterSnapshot(afterSnapshot == null ? "" : JsonUtils.toJsonString(afterSnapshot))
                .reason(StringUtils.hasText(reason) ? reason : "")
                .build();
        storeAuditLogMapper.insert(log);
    }

    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private static final class LifecycleGuardEvaluation {

        private final List<String> warnings = new ArrayList<>();
        private final List<ProductStoreLifecycleGuardRespVO.GuardItem> guardItems = new ArrayList<>();
        private ErrorCode blockedErrorCode;

        public List<String> getWarnings() {
            return warnings;
        }

        public List<ProductStoreLifecycleGuardRespVO.GuardItem> getGuardItems() {
            return guardItems;
        }

        public ErrorCode getBlockedErrorCode() {
            return blockedErrorCode;
        }

        public void setBlockedErrorCode(ErrorCode blockedErrorCode) {
            this.blockedErrorCode = blockedErrorCode;
        }
    }
}
