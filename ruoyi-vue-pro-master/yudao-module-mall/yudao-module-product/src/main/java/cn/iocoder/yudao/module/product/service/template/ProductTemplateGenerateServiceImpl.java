package cn.iocoder.yudao.module.product.service.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductCategoryTemplateValidateReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductCategoryTemplateValidateRespVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGenerateCommitReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGenerateCommitRespVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGeneratePreviewReqVO;
import cn.iocoder.yudao.module.product.controller.admin.template.vo.ProductSkuGeneratePreviewRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.sku.ProductSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductAttributeDefinitionDO;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductAttributeOptionDO;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductCategoryAttrTplItemDO;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductCategoryAttrTplVersionDO;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductCategoryExtDO;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductSkuGenerateTaskDO;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductSkuGenerateTaskItemDO;
import cn.iocoder.yudao.module.product.dal.mysql.sku.ProductSkuMapper;
import cn.iocoder.yudao.module.product.dal.mysql.template.ProductAttributeDefinitionMapper;
import cn.iocoder.yudao.module.product.dal.mysql.template.ProductAttributeOptionMapper;
import cn.iocoder.yudao.module.product.dal.mysql.template.ProductCategoryAttrTplItemMapper;
import cn.iocoder.yudao.module.product.dal.mysql.template.ProductCategoryAttrTplVersionMapper;
import cn.iocoder.yudao.module.product.dal.mysql.template.ProductCategoryExtMapper;
import cn.iocoder.yudao.module.product.dal.mysql.template.ProductSkuGenerateTaskItemMapper;
import cn.iocoder.yudao.module.product.dal.mysql.template.ProductSkuGenerateTaskMapper;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.product.enums.template.ProductTemplateConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class ProductTemplateGenerateServiceImpl implements ProductTemplateGenerateService {

    private static final int DEFAULT_PREVIEW_COMBINATION_LIMIT = 200;
    private static final int DEFAULT_RETRY_LIMIT = 100;

    @Resource
    private ProductCategoryExtMapper categoryExtMapper;
    @Resource
    private ProductAttributeDefinitionMapper attributeDefinitionMapper;
    @Resource
    private ProductAttributeOptionMapper attributeOptionMapper;
    @Resource
    private ProductCategoryAttrTplVersionMapper templateVersionMapper;
    @Resource
    private ProductCategoryAttrTplItemMapper templateItemMapper;
    @Resource
    private ProductSkuGenerateTaskMapper skuGenerateTaskMapper;
    @Resource
    private ProductSkuGenerateTaskItemMapper skuGenerateTaskItemMapper;
    @Resource
    private ProductSkuMapper productSkuMapper;

    private int previewCombinationLimit = DEFAULT_PREVIEW_COMBINATION_LIMIT;

    // for unit test
    void setPreviewCombinationLimit(int previewCombinationLimit) {
        this.previewCombinationLimit = previewCombinationLimit;
    }

    @Override
    public ProductCategoryTemplateValidateRespVO validateTemplate(ProductCategoryTemplateValidateReqVO reqVO) {
        ProductCategoryTemplateValidateRespVO respVO = new ProductCategoryTemplateValidateRespVO();
        if (CollUtil.isEmpty(reqVO.getItems())) {
            respVO.setPass(false);
            respVO.getErrors().add(new ProductCategoryTemplateValidateRespVO.Message(
                    "EMPTY_ITEMS", "模板项不能为空"));
            return respVO;
        }
        Set<Long> duplicateCheck = new HashSet<>();
        Set<Long> attributeIds = reqVO.getItems().stream()
                .map(ProductCategoryTemplateValidateReqVO.Item::getAttributeId)
                .collect(Collectors.toSet());
        Map<Long, ProductAttributeDefinitionDO> definitionMap = attributeDefinitionMapper.selectListByIds(attributeIds)
                .stream().collect(Collectors.toMap(ProductAttributeDefinitionDO::getId, d -> d));

        ProductCategoryExtDO categoryExt = categoryExtMapper.selectByCategoryId(reqVO.getCategoryId());
        boolean serviceCategory = categoryExt != null
                && Objects.equals(categoryExt.getProductType(), ProductTypeEnum.SERVICE.getType());

        for (ProductCategoryTemplateValidateReqVO.Item item : reqVO.getItems()) {
            if (!duplicateCheck.add(item.getAttributeId())) {
                addError(respVO, "DUPLICATE_ATTRIBUTE", "模板内属性重复：" + item.getAttributeId());
                continue;
            }
            boolean skuSpecRole = Objects.equals(item.getAttrRole(), ProductTemplateConstants.ATTR_ROLE_SKU_SPEC);
            boolean affectsPrice = Boolean.TRUE.equals(item.getAffectsPrice());
            boolean affectsStock = Boolean.TRUE.equals(item.getAffectsStock());
            if (skuSpecRole && !affectsPrice && !affectsStock) {
                addError(respVO, CATEGORY_TEMPLATE_SKU_SPEC_AFFECT_FLAG_INVALID.getCode().toString(),
                        "SKU 规格属性必须影响价格或库存，属性：" + item.getAttributeId());
            }
            if (!skuSpecRole && (affectsPrice || affectsStock)) {
                addError(respVO, CATEGORY_TEMPLATE_NON_SPEC_AFFECT_FORBIDDEN.getCode().toString(),
                        "非 SKU 规格属性不允许影响价格或库存，属性：" + item.getAttributeId());
            }
            ProductAttributeDefinitionDO definition = definitionMap.get(item.getAttributeId());
            if (definition == null) {
                addError(respVO, "ATTRIBUTE_NOT_EXISTS", "属性不存在：" + item.getAttributeId());
                continue;
            }
            if (Objects.equals(item.getAttrRole(), ProductTemplateConstants.ATTR_ROLE_SKU_SPEC)
                    && !isSpecDataType(definition.getDataType())) {
                addError(respVO, CATEGORY_TEMPLATE_SKU_SPEC_DATA_TYPE_INVALID.getCode().toString(),
                        "SKU 规格属性仅支持单选或多选，属性：" + definition.getName());
            }
            if (serviceCategory && Boolean.TRUE.equals(item.getAffectsStock())) {
                addError(respVO, CATEGORY_TEMPLATE_SERVICE_STOCK_AFFECT_FORBIDDEN.getCode().toString(),
                        "服务类目不允许影响库存，属性：" + definition.getName());
            }
        }
        respVO.setPass(respVO.getErrors().isEmpty());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductSkuGeneratePreviewRespVO previewSkuGenerate(ProductSkuGeneratePreviewReqVO reqVO) {
        ProductCategoryAttrTplVersionDO templateVersion = getTemplateVersion(reqVO.getCategoryId(), reqVO.getTemplateVersionId());
        List<ProductCategoryAttrTplItemDO> templateItems = templateItemMapper.selectListByTemplateVersionId(templateVersion.getId());
        List<ProductCategoryAttrTplItemDO> skuSpecItems = templateItems.stream()
                .filter(item -> Objects.equals(item.getAttrRole(), ProductTemplateConstants.ATTR_ROLE_SKU_SPEC))
                .collect(Collectors.toList());

        Map<Long, ProductSkuGeneratePreviewReqVO.SpecSelection> selectionMap = reqVO.getSpecSelections().stream()
                .collect(Collectors.toMap(ProductSkuGeneratePreviewReqVO.SpecSelection::getAttributeId, s -> s, (a, b) -> a));
        for (ProductCategoryAttrTplItemDO skuSpecItem : skuSpecItems) {
            ProductSkuGeneratePreviewReqVO.SpecSelection selection = selectionMap.get(skuSpecItem.getAttributeId());
            if (Boolean.TRUE.equals(skuSpecItem.getIsRequired())
                    && (selection == null || CollUtil.isEmpty(selection.getOptionIds()))) {
                throw exception(SKU_GENERATE_SPEC_SELECTION_EMPTY);
            }
        }
        for (ProductSkuGeneratePreviewReqVO.SpecSelection selection : reqVO.getSpecSelections()) {
            if (CollUtil.isEmpty(selection.getOptionIds())) {
                throw exception(SKU_GENERATE_SPEC_SELECTION_EMPTY);
            }
        }

        List<ProductSkuGeneratePreviewReqVO.SpecSelection> orderedSelections = new ArrayList<>(reqVO.getSpecSelections());
        orderedSelections.sort(Comparator.comparing(ProductSkuGeneratePreviewReqVO.SpecSelection::getAttributeId));

        long combinationCount = 1L;
        for (ProductSkuGeneratePreviewReqVO.SpecSelection selection : orderedSelections) {
            combinationCount *= selection.getOptionIds().size();
        }
        boolean truncated = combinationCount > previewCombinationLimit;

        List<List<SpecPair>> combinations = generateCombinations(orderedSelections, previewCombinationLimit);
        Map<Long, String> optionLabelMap = attributeOptionMapper.selectListByIds(
                orderedSelections.stream().flatMap(s -> s.getOptionIds().stream()).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(ProductAttributeOptionDO::getId, ProductAttributeOptionDO::getLabel, (a, b) -> a));
        Map<Long, String> attrNameMap = attributeDefinitionMapper.selectListByIds(
                orderedSelections.stream().map(ProductSkuGeneratePreviewReqVO.SpecSelection::getAttributeId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(ProductAttributeDefinitionDO::getId, ProductAttributeDefinitionDO::getName, (a, b) -> a));

        Map<String, Long> existingSkuMap = buildExistingSkuMap(reqVO.getSpuId());
        ProductSkuGenerateTaskDO previewTask = ProductSkuGenerateTaskDO.builder()
                .taskNo(generateTaskNo("SKU_PREVIEW"))
                .spuId(reqVO.getSpuId())
                .categoryId(reqVO.getCategoryId())
                .templateVersionId(templateVersion.getId())
                .mode(ProductTemplateConstants.TASK_MODE_PREVIEW)
                .status(ProductTemplateConstants.TASK_STATUS_SUCCESS)
                .idempotencyKey("")
                .requestJson(JsonUtils.toJsonString(reqVO))
                .resultJson("")
                .errorMsg("")
                .retryCount(0)
                .build();
        skuGenerateTaskMapper.insert(previewTask);

        ProductSkuGeneratePreviewRespVO respVO = new ProductSkuGeneratePreviewRespVO();
        respVO.setTaskNo(previewTask.getTaskNo());
        respVO.setCombinationCount((int) Math.min(Integer.MAX_VALUE, combinationCount));
        respVO.setTruncated(truncated);

        for (List<SpecPair> combination : combinations) {
            String optionKey = buildOptionKey(combination.stream().map(SpecPair::getOptionId).collect(Collectors.toList()));
            String specHash = buildSpecHash(reqVO.getSpuId(), optionKey);
            Long existsSkuId = existingSkuMap.get(optionKey);
            String specSummary = buildSpecSummary(combination, attrNameMap, optionLabelMap);

            ProductSkuGenerateTaskItemDO taskItem = ProductSkuGenerateTaskItemDO.builder()
                    .taskId(previewTask.getId())
                    .spuId(reqVO.getSpuId())
                    .specHash(specHash)
                    .specJson(JsonUtils.toJsonString(combination))
                    .targetSkuId(existsSkuId)
                    .status(ProductTemplateConstants.TASK_ITEM_STATUS_PENDING)
                    .errorMsg("")
                    .build();
            skuGenerateTaskItemMapper.insert(taskItem);

            ProductSkuGeneratePreviewRespVO.Item item = new ProductSkuGeneratePreviewRespVO.Item();
            item.setSpecHash(specHash);
            item.setSpecSummary(specSummary);
            item.setExistsSkuId(existsSkuId);
            ProductSkuGeneratePreviewRespVO.SuggestedSku suggestedSku = new ProductSkuGeneratePreviewRespVO.SuggestedSku();
            suggestedSku.setPrice(reqVO.getBaseSku().getPrice());
            suggestedSku.setMarketPrice(reqVO.getBaseSku().getMarketPrice());
            suggestedSku.setStock(reqVO.getBaseSku().getStock());
            item.setSuggestedSku(suggestedSku);
            respVO.getItems().add(item);
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductSkuGenerateCommitRespVO commitSkuGenerate(ProductSkuGenerateCommitReqVO reqVO) {
        ProductSkuGenerateTaskDO previewTask = skuGenerateTaskMapper.selectByTaskNo(reqVO.getTaskNo());
        if (previewTask == null || !Objects.equals(previewTask.getMode(), ProductTemplateConstants.TASK_MODE_PREVIEW)) {
            throw exception(SKU_GENERATE_PREVIEW_TASK_REQUIRED);
        }
        ProductSkuGenerateTaskDO existed = skuGenerateTaskMapper.selectByIdempotency(previewTask.getSpuId(),
                ProductTemplateConstants.TASK_MODE_COMMIT, reqVO.getIdempotencyKey());
        if (existed != null) {
            return buildIdempotentHitResp(existed);
        }

        ProductSkuGeneratePreviewReqVO previewReq = JsonUtils.parseObject(previewTask.getRequestJson(), ProductSkuGeneratePreviewReqVO.class);
        if (previewReq == null) {
            throw exception(SKU_GENERATE_TASK_STATUS_INVALID);
        }
        ProductSkuGenerateTaskDO commitTask = ProductSkuGenerateTaskDO.builder()
                .taskNo(generateTaskNo("SKU_COMMIT"))
                .spuId(previewTask.getSpuId())
                .categoryId(previewTask.getCategoryId())
                .templateVersionId(previewTask.getTemplateVersionId())
                .mode(ProductTemplateConstants.TASK_MODE_COMMIT)
                .idempotencyKey(reqVO.getIdempotencyKey())
                .status(ProductTemplateConstants.TASK_STATUS_RUNNING)
                .requestJson(previewTask.getRequestJson())
                .resultJson("")
                .errorMsg("")
                .retryCount(0)
                .build();
        try {
            skuGenerateTaskMapper.insert(commitTask);
        } catch (DuplicateKeyException ex) {
            // 并发提交同一幂等键时，返回已存在任务，避免重复执行或 500
            ProductSkuGenerateTaskDO idempotentTask = skuGenerateTaskMapper.selectByIdempotency(previewTask.getSpuId(),
                    ProductTemplateConstants.TASK_MODE_COMMIT, reqVO.getIdempotencyKey());
            if (idempotentTask != null) {
                return buildIdempotentHitResp(idempotentTask);
            }
            throw ex;
        }

        List<ProductSkuGenerateTaskItemDO> previewItems = skuGenerateTaskItemMapper.selectListByTaskId(previewTask.getId());
        CommitSummary summary = processCommitItems(commitTask.getId(), previewReq, previewItems, false);
        finishCommitTask(commitTask, summary);

        ProductSkuGenerateCommitRespVO respVO = new ProductSkuGenerateCommitRespVO();
        respVO.setTaskNo(commitTask.getTaskNo());
        respVO.setStatus(commitTask.getStatus());
        respVO.setAccepted(true);
        respVO.setIdempotentHit(false);
        return respVO;
    }

    private static ProductSkuGenerateCommitRespVO buildIdempotentHitResp(ProductSkuGenerateTaskDO existed) {
        ProductSkuGenerateCommitRespVO hitResp = new ProductSkuGenerateCommitRespVO();
        hitResp.setTaskNo(existed.getTaskNo());
        hitResp.setStatus(existed.getStatus());
        hitResp.setAccepted(true);
        hitResp.setIdempotentHit(true);
        return hitResp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int retryFailedCommitTasks(int limit) {
        int actualLimit = limit <= 0 ? DEFAULT_RETRY_LIMIT : Math.min(1000, limit);
        List<ProductSkuGenerateTaskDO> retryableList = skuGenerateTaskMapper.selectRetryableList(LocalDateTime.now(), actualLimit)
                .stream()
                .filter(task -> Objects.equals(task.getMode(), ProductTemplateConstants.TASK_MODE_COMMIT))
                .collect(Collectors.toList());
        int processed = 0;
        for (ProductSkuGenerateTaskDO task : retryableList) {
            try {
                retryCommitTask(task);
                processed++;
            } catch (Exception ex) {
                log.error("[retryFailedCommitTasks][task({}) retry failed]", task.getTaskNo(), ex);
            }
        }
        return processed;
    }

    private void retryCommitTask(ProductSkuGenerateTaskDO task) {
        ProductSkuGeneratePreviewReqVO previewReq = JsonUtils.parseObject(task.getRequestJson(), ProductSkuGeneratePreviewReqVO.class);
        if (previewReq == null) {
            task.setErrorMsg("request_json parse failed");
            task.setRetryCount(task.getRetryCount() + 1);
            task.setNextRetryTime(LocalDateTime.now().plusMinutes(5));
            skuGenerateTaskMapper.updateById(task);
            return;
        }
        List<ProductSkuGenerateTaskItemDO> failedItems = skuGenerateTaskItemMapper.selectListByTaskIdAndStatus(task.getId(),
                ProductTemplateConstants.TASK_ITEM_STATUS_FAIL);
        if (CollUtil.isEmpty(failedItems)) {
            task.setStatus(ProductTemplateConstants.TASK_STATUS_SUCCESS);
            task.setErrorMsg("");
            task.setNextRetryTime(null);
            skuGenerateTaskMapper.updateById(task);
            return;
        }
        processCommitItems(task.getId(), previewReq, failedItems, true);

        List<ProductSkuGenerateTaskItemDO> allItems = skuGenerateTaskItemMapper.selectListByTaskId(task.getId());
        long failCount = allItems.stream().filter(i -> Objects.equals(i.getStatus(), ProductTemplateConstants.TASK_ITEM_STATUS_FAIL)).count();
        if (failCount > 0) {
            task.setStatus(ProductTemplateConstants.TASK_STATUS_PARTIAL_SUCCESS);
            task.setRetryCount(task.getRetryCount() + 1);
            task.setNextRetryTime(LocalDateTime.now().plusMinutes(5));
        } else {
            task.setStatus(ProductTemplateConstants.TASK_STATUS_SUCCESS);
            task.setErrorMsg("");
            task.setNextRetryTime(null);
        }
        skuGenerateTaskMapper.updateById(task);
    }

    private CommitSummary processCommitItems(Long taskId, ProductSkuGeneratePreviewReqVO previewReq,
                                             List<ProductSkuGenerateTaskItemDO> sourceItems, boolean retryMode) {
        Map<String, Long> existingSkuMap = buildExistingSkuMap(previewReq.getSpuId());
        Set<Long> allAttrIds = new HashSet<>();
        Set<Long> allOptionIds = new HashSet<>();
        List<List<SpecPair>> parsedPairsList = new ArrayList<>();
        for (ProductSkuGenerateTaskItemDO sourceItem : sourceItems) {
            List<SpecPair> pairs = parseSpecPairs(sourceItem.getSpecJson());
            parsedPairsList.add(pairs);
            for (SpecPair pair : pairs) {
                allAttrIds.add(pair.getAttributeId());
                allOptionIds.add(pair.getOptionId());
            }
        }
        Map<Long, ProductAttributeDefinitionDO> attrMap = attributeDefinitionMapper.selectListByIds(allAttrIds)
                .stream().collect(Collectors.toMap(ProductAttributeDefinitionDO::getId, d -> d, (a, b) -> a));
        Map<Long, ProductAttributeOptionDO> optionMap = attributeOptionMapper.selectListByIds(allOptionIds)
                .stream().collect(Collectors.toMap(ProductAttributeOptionDO::getId, o -> o, (a, b) -> a));

        CommitSummary summary = new CommitSummary();
        for (int i = 0; i < sourceItems.size(); i++) {
            ProductSkuGenerateTaskItemDO sourceItem = sourceItems.get(i);
            List<SpecPair> pairs = parsedPairsList.get(i);
            if (CollUtil.isEmpty(pairs)) {
                sourceItem.setStatus(ProductTemplateConstants.TASK_ITEM_STATUS_FAIL);
                sourceItem.setErrorMsg("spec_json invalid");
                if (retryMode) {
                    skuGenerateTaskItemMapper.updateById(sourceItem);
                } else {
                    ProductSkuGenerateTaskItemDO failItem = copyToNewTaskItem(sourceItem, taskId);
                    failItem.setStatus(ProductTemplateConstants.TASK_ITEM_STATUS_FAIL);
                    failItem.setErrorMsg("spec_json invalid");
                    skuGenerateTaskItemMapper.insert(failItem);
                }
                summary.fail++;
                continue;
            }

            String optionKey = buildOptionKey(pairs.stream().map(SpecPair::getOptionId).collect(Collectors.toList()));
            Long existsSkuId = existingSkuMap.get(optionKey);
            if (existsSkuId != null) {
                if (retryMode) {
                    sourceItem.setStatus(ProductTemplateConstants.TASK_ITEM_STATUS_SKIPPED);
                    sourceItem.setTargetSkuId(existsSkuId);
                    sourceItem.setErrorMsg("");
                    skuGenerateTaskItemMapper.updateById(sourceItem);
                } else {
                    ProductSkuGenerateTaskItemDO skipItem = copyToNewTaskItem(sourceItem, taskId);
                    skipItem.setStatus(ProductTemplateConstants.TASK_ITEM_STATUS_SKIPPED);
                    skipItem.setTargetSkuId(existsSkuId);
                    skipItem.setErrorMsg("");
                    skuGenerateTaskItemMapper.insert(skipItem);
                }
                summary.skipped++;
                continue;
            }

            try {
                ProductSkuDO sku = new ProductSkuDO();
                sku.setSpuId(previewReq.getSpuId());
                sku.setProperties(buildProperties(pairs, attrMap, optionMap));
                sku.setPrice(previewReq.getBaseSku().getPrice());
                sku.setMarketPrice(previewReq.getBaseSku().getMarketPrice());
                sku.setCostPrice(previewReq.getBaseSku().getCostPrice());
                sku.setBarCode("");
                sku.setPicUrl("");
                sku.setStock(previewReq.getBaseSku().getStock());
                sku.setWeight(0D);
                sku.setVolume(0D);
                sku.setFirstBrokeragePrice(0);
                sku.setSecondBrokeragePrice(0);
                sku.setSalesCount(0);
                productSkuMapper.insert(sku);
                existingSkuMap.put(optionKey, sku.getId());

                if (retryMode) {
                    sourceItem.setStatus(ProductTemplateConstants.TASK_ITEM_STATUS_SUCCESS);
                    sourceItem.setTargetSkuId(sku.getId());
                    sourceItem.setErrorMsg("");
                    skuGenerateTaskItemMapper.updateById(sourceItem);
                } else {
                    ProductSkuGenerateTaskItemDO successItem = copyToNewTaskItem(sourceItem, taskId);
                    successItem.setStatus(ProductTemplateConstants.TASK_ITEM_STATUS_SUCCESS);
                    successItem.setTargetSkuId(sku.getId());
                    successItem.setErrorMsg("");
                    skuGenerateTaskItemMapper.insert(successItem);
                }
                summary.success++;
            } catch (Exception ex) {
                if (retryMode) {
                    sourceItem.setStatus(ProductTemplateConstants.TASK_ITEM_STATUS_FAIL);
                    sourceItem.setErrorMsg(cutError(ex.getMessage()));
                    skuGenerateTaskItemMapper.updateById(sourceItem);
                } else {
                    ProductSkuGenerateTaskItemDO failItem = copyToNewTaskItem(sourceItem, taskId);
                    failItem.setStatus(ProductTemplateConstants.TASK_ITEM_STATUS_FAIL);
                    failItem.setErrorMsg(cutError(ex.getMessage()));
                    skuGenerateTaskItemMapper.insert(failItem);
                }
                summary.fail++;
            }
        }
        return summary;
    }

    private static ProductSkuGenerateTaskItemDO copyToNewTaskItem(ProductSkuGenerateTaskItemDO sourceItem, Long taskId) {
        return ProductSkuGenerateTaskItemDO.builder()
                .taskId(taskId)
                .spuId(sourceItem.getSpuId())
                .specHash(sourceItem.getSpecHash())
                .specJson(sourceItem.getSpecJson())
                .targetSkuId(sourceItem.getTargetSkuId())
                .status(sourceItem.getStatus())
                .errorMsg(sourceItem.getErrorMsg())
                .build();
    }

    private void finishCommitTask(ProductSkuGenerateTaskDO commitTask, CommitSummary summary) {
        if (summary.fail == 0) {
            commitTask.setStatus(ProductTemplateConstants.TASK_STATUS_SUCCESS);
            commitTask.setErrorMsg("");
            commitTask.setNextRetryTime(null);
        } else if (summary.success > 0 || summary.skipped > 0) {
            commitTask.setStatus(ProductTemplateConstants.TASK_STATUS_PARTIAL_SUCCESS);
            commitTask.setErrorMsg("存在失败明细");
            commitTask.setNextRetryTime(LocalDateTime.now().plusMinutes(5));
        } else {
            commitTask.setStatus(ProductTemplateConstants.TASK_STATUS_FAIL);
            commitTask.setErrorMsg("全部明细失败");
            commitTask.setNextRetryTime(LocalDateTime.now().plusMinutes(5));
        }
        commitTask.setResultJson(JsonUtils.toJsonString(summary));
        skuGenerateTaskMapper.updateById(commitTask);
    }

    private ProductCategoryAttrTplVersionDO getTemplateVersion(Long categoryId, Long templateVersionId) {
        ProductCategoryAttrTplVersionDO templateVersion;
        if (templateVersionId != null) {
            templateVersion = templateVersionMapper.selectById(templateVersionId);
        } else {
            templateVersion = templateVersionMapper.selectPublishedByCategoryId(categoryId);
        }
        if (templateVersion == null) {
            throw exception(CATEGORY_TEMPLATE_NOT_EXISTS);
        }
        if (!Objects.equals(templateVersion.getCategoryId(), categoryId)) {
            throw exception(CATEGORY_TEMPLATE_NOT_EXISTS);
        }
        if (StrUtil.isBlank(templateVersion.getSnapshotJson())) {
            throw exception(CATEGORY_TEMPLATE_VERSION_SNAPSHOT_REQUIRED);
        }
        return templateVersion;
    }

    private static boolean isSpecDataType(Integer dataType) {
        return Objects.equals(dataType, ProductTemplateConstants.DATA_TYPE_ENUM)
                || Objects.equals(dataType, ProductTemplateConstants.DATA_TYPE_MULTI_ENUM);
    }

    private static void addError(ProductCategoryTemplateValidateRespVO respVO, String code, String message) {
        respVO.getErrors().add(new ProductCategoryTemplateValidateRespVO.Message(code, message));
    }

    private static String cutError(String message) {
        if (StrUtil.isBlank(message)) {
            return "unknown";
        }
        return StrUtil.maxLength(message, 500);
    }

    private static String generateTaskNo(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + RandomUtil.randomNumbers(6);
    }

    private Map<String, Long> buildExistingSkuMap(Long spuId) {
        List<ProductSkuDO> existingSkus = productSkuMapper.selectListBySpuId(spuId);
        Map<String, Long> map = new HashMap<>();
        for (ProductSkuDO sku : existingSkus) {
            if (CollUtil.isEmpty(sku.getProperties())) {
                continue;
            }
            String key = buildOptionKey(sku.getProperties().stream()
                    .map(ProductSkuDO.Property::getValueId)
                    .collect(Collectors.toList()));
            map.putIfAbsent(key, sku.getId());
        }
        return map;
    }

    private static String buildOptionKey(List<Long> optionIds) {
        List<Long> sorted = new ArrayList<>(optionIds);
        sorted.sort(Comparator.naturalOrder());
        return sorted.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String buildSpecHash(Long spuId, String optionKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((spuId + ":" + optionKey).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static List<List<SpecPair>> generateCombinations(List<ProductSkuGeneratePreviewReqVO.SpecSelection> selections, int limit) {
        List<List<SpecPair>> result = new ArrayList<>();
        backtrackCombinations(selections, 0, new ArrayList<>(), result, limit);
        return result;
    }

    private static void backtrackCombinations(List<ProductSkuGeneratePreviewReqVO.SpecSelection> selections,
                                              int index, List<SpecPair> current, List<List<SpecPair>> result, int limit) {
        if (result.size() >= limit) {
            return;
        }
        if (index >= selections.size()) {
            result.add(new ArrayList<>(current));
            return;
        }
        ProductSkuGeneratePreviewReqVO.SpecSelection selection = selections.get(index);
        for (Long optionId : selection.getOptionIds()) {
            current.add(new SpecPair(selection.getAttributeId(), optionId));
            backtrackCombinations(selections, index + 1, current, result, limit);
            current.remove(current.size() - 1);
            if (result.size() >= limit) {
                return;
            }
        }
    }

    private static String buildSpecSummary(List<SpecPair> combination, Map<Long, String> attrNameMap, Map<Long, String> optionLabelMap) {
        return combination.stream()
                .map(pair -> {
                    String attr = attrNameMap.getOrDefault(pair.getAttributeId(), String.valueOf(pair.getAttributeId()));
                    String option = optionLabelMap.getOrDefault(pair.getOptionId(), String.valueOf(pair.getOptionId()));
                    return attr + ":" + option;
                })
                .collect(Collectors.joining("/"));
    }

    private static List<SpecPair> parseSpecPairs(String specJson) {
        if (StrUtil.isBlank(specJson)) {
            return Collections.emptyList();
        }
        List<SpecPair> list = JsonUtils.parseObjectQuietly(specJson, new TypeReference<List<SpecPair>>() {});
        return list == null ? Collections.emptyList() : list;
    }

    private static List<ProductSkuDO.Property> buildProperties(List<SpecPair> pairs,
                                                               Map<Long, ProductAttributeDefinitionDO> attrMap,
                                                               Map<Long, ProductAttributeOptionDO> optionMap) {
        List<ProductSkuDO.Property> properties = new ArrayList<>(pairs.size());
        for (SpecPair pair : pairs) {
            ProductAttributeDefinitionDO attr = attrMap.get(pair.getAttributeId());
            ProductAttributeOptionDO option = optionMap.get(pair.getOptionId());
            ProductSkuDO.Property property = new ProductSkuDO.Property();
            property.setPropertyId(pair.getAttributeId());
            property.setPropertyName(attr != null ? attr.getName() : String.valueOf(pair.getAttributeId()));
            property.setValueId(pair.getOptionId());
            property.setValueName(option != null ? option.getLabel() : String.valueOf(pair.getOptionId()));
            properties.add(property);
        }
        return properties;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SpecPair {
        private Long attributeId;
        private Long optionId;
    }

    @Data
    private static class CommitSummary {
        private int success;
        private int skipped;
        private int fail;
    }
}
