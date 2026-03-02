package cn.iocoder.yudao.module.product.service.template;

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
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductTemplateGenerateServiceImplTest {

    @Mock
    private ProductCategoryExtMapper categoryExtMapper;
    @Mock
    private ProductAttributeDefinitionMapper attributeDefinitionMapper;
    @Mock
    private ProductAttributeOptionMapper attributeOptionMapper;
    @Mock
    private ProductCategoryAttrTplVersionMapper templateVersionMapper;
    @Mock
    private ProductCategoryAttrTplItemMapper templateItemMapper;
    @Mock
    private ProductSkuGenerateTaskMapper skuGenerateTaskMapper;
    @Mock
    private ProductSkuGenerateTaskItemMapper skuGenerateTaskItemMapper;
    @Mock
    private ProductSkuMapper productSkuMapper;

    @InjectMocks
    private ProductTemplateGenerateServiceImpl service;

    @Test
    void validateTemplate_shouldReturnErrorsWhenInvalidSpecAndServiceStockAffect() {
        ProductCategoryTemplateValidateReqVO reqVO = new ProductCategoryTemplateValidateReqVO();
        reqVO.setCategoryId(101L);
        ProductCategoryTemplateValidateReqVO.Item item = new ProductCategoryTemplateValidateReqVO.Item();
        item.setAttributeId(1L);
        item.setAttrRole(ProductTemplateConstants.ATTR_ROLE_SKU_SPEC);
        item.setRequired(true);
        item.setAffectsPrice(true);
        item.setAffectsStock(true);
        reqVO.setItems(Collections.singletonList(item));

        ProductCategoryExtDO categoryExtDO = ProductCategoryExtDO.builder()
                .categoryId(101L)
                .productType(ProductTypeEnum.SERVICE.getType())
                .build();
        when(categoryExtMapper.selectByCategoryId(101L)).thenReturn(categoryExtDO);

        ProductAttributeDefinitionDO definitionDO = ProductAttributeDefinitionDO.builder()
                .id(1L)
                .name("时长")
                .dataType(ProductTemplateConstants.DATA_TYPE_NUMBER)
                .build();
        when(attributeDefinitionMapper.selectListByIds(anySet())).thenReturn(Collections.singletonList(definitionDO));

        ProductCategoryTemplateValidateRespVO respVO = service.validateTemplate(reqVO);

        assertFalse(respVO.getPass());
        assertEquals(2, respVO.getErrors().size());
    }

    @Test
    void validateTemplate_shouldReturnErrorWhenSkuSpecNotAffectPriceOrStock() {
        ProductCategoryTemplateValidateReqVO reqVO = new ProductCategoryTemplateValidateReqVO();
        reqVO.setCategoryId(101L);
        ProductCategoryTemplateValidateReqVO.Item item = new ProductCategoryTemplateValidateReqVO.Item();
        item.setAttributeId(1L);
        item.setAttrRole(ProductTemplateConstants.ATTR_ROLE_SKU_SPEC);
        item.setRequired(true);
        item.setAffectsPrice(false);
        item.setAffectsStock(false);
        reqVO.setItems(Collections.singletonList(item));

        when(categoryExtMapper.selectByCategoryId(101L)).thenReturn(null);
        ProductAttributeDefinitionDO definitionDO = ProductAttributeDefinitionDO.builder()
                .id(1L)
                .name("时长")
                .dataType(ProductTemplateConstants.DATA_TYPE_ENUM)
                .build();
        when(attributeDefinitionMapper.selectListByIds(anySet())).thenReturn(Collections.singletonList(definitionDO));

        ProductCategoryTemplateValidateRespVO respVO = service.validateTemplate(reqVO);

        assertFalse(respVO.getPass());
        assertEquals(1, respVO.getErrors().size());
    }

    @Test
    void validateTemplate_shouldReturnErrorWhenNonSkuSpecAffectPriceOrStock() {
        ProductCategoryTemplateValidateReqVO reqVO = new ProductCategoryTemplateValidateReqVO();
        reqVO.setCategoryId(101L);
        ProductCategoryTemplateValidateReqVO.Item item = new ProductCategoryTemplateValidateReqVO.Item();
        item.setAttributeId(1L);
        item.setAttrRole(ProductTemplateConstants.ATTR_ROLE_SPU_ATTR);
        item.setRequired(true);
        item.setAffectsPrice(true);
        item.setAffectsStock(false);
        reqVO.setItems(Collections.singletonList(item));

        when(categoryExtMapper.selectByCategoryId(101L)).thenReturn(null);
        ProductAttributeDefinitionDO definitionDO = ProductAttributeDefinitionDO.builder()
                .id(1L)
                .name("产地")
                .dataType(ProductTemplateConstants.DATA_TYPE_STRING)
                .build();
        when(attributeDefinitionMapper.selectListByIds(anySet())).thenReturn(Collections.singletonList(definitionDO));

        ProductCategoryTemplateValidateRespVO respVO = service.validateTemplate(reqVO);

        assertFalse(respVO.getPass());
        assertEquals(1, respVO.getErrors().size());
    }

    @Test
    void previewSkuGenerate_shouldTruncateByCombinationLimit() {
        service.setPreviewCombinationLimit(2);
        ProductSkuGeneratePreviewReqVO reqVO = buildPreviewReq();
        reqVO.setTemplateVersionId(12L);

        ProductCategoryAttrTplVersionDO versionDO = ProductCategoryAttrTplVersionDO.builder()
                .id(12L).categoryId(101L).snapshotJson("{\"v\":12}").build();
        when(templateVersionMapper.selectById(12L)).thenReturn(versionDO);
        when(templateItemMapper.selectListByTemplateVersionId(12L)).thenReturn(Arrays.asList(
                ProductCategoryAttrTplItemDO.builder()
                        .attributeId(1L).attrRole(ProductTemplateConstants.ATTR_ROLE_SKU_SPEC).isRequired(true).build(),
                ProductCategoryAttrTplItemDO.builder()
                        .attributeId(2L).attrRole(ProductTemplateConstants.ATTR_ROLE_SKU_SPEC).isRequired(true).build()
        ));
        when(attributeOptionMapper.selectListByIds(anySet())).thenReturn(Arrays.asList(
                ProductAttributeOptionDO.builder().id(11L).label("60分钟").build(),
                ProductAttributeOptionDO.builder().id(12L).label("90分钟").build(),
                ProductAttributeOptionDO.builder().id(21L).label("标准").build(),
                ProductAttributeOptionDO.builder().id(22L).label("高级").build()
        ));
        when(attributeDefinitionMapper.selectListByIds(anySet())).thenReturn(Arrays.asList(
                ProductAttributeDefinitionDO.builder().id(1L).name("时长").build(),
                ProductAttributeDefinitionDO.builder().id(2L).name("等级").build()
        ));
        when(productSkuMapper.selectListBySpuId(30001L)).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            ProductSkuGenerateTaskDO taskDO = invocation.getArgument(0);
            taskDO.setId(100L);
            return 1;
        }).when(skuGenerateTaskMapper).insert(any(ProductSkuGenerateTaskDO.class));

        ProductSkuGeneratePreviewRespVO respVO = service.previewSkuGenerate(reqVO);

        assertEquals(4, respVO.getCombinationCount());
        assertTrue(respVO.getTruncated());
        assertEquals(2, respVO.getItems().size());
        verify(skuGenerateTaskItemMapper, times(2)).insert(any(ProductSkuGenerateTaskItemDO.class));
    }

    @Test
    void previewSkuGenerate_shouldThrowWhenTemplateSnapshotMissing() {
        ProductSkuGeneratePreviewReqVO reqVO = buildPreviewReq();
        reqVO.setTemplateVersionId(12L);
        ProductCategoryAttrTplVersionDO versionDO = ProductCategoryAttrTplVersionDO.builder()
                .id(12L).categoryId(101L).snapshotJson("").build();
        when(templateVersionMapper.selectById(12L)).thenReturn(versionDO);

        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.previewSkuGenerate(reqVO));
    }

    @Test
    void commitSkuGenerate_shouldCreateSkuAndHitIdempotency() {
        ProductSkuGeneratePreviewReqVO previewReq = buildPreviewReq();
        ProductSkuGenerateTaskDO previewTask = ProductSkuGenerateTaskDO.builder()
                .id(10L)
                .taskNo("SKU_PREVIEW_10")
                .spuId(30001L)
                .categoryId(101L)
                .templateVersionId(12L)
                .mode(ProductTemplateConstants.TASK_MODE_PREVIEW)
                .requestJson(JsonUtils.toJsonString(previewReq))
                .build();
        when(skuGenerateTaskMapper.selectByTaskNo("SKU_PREVIEW_10")).thenReturn(previewTask);
        when(skuGenerateTaskMapper.selectByIdempotency(30001L, ProductTemplateConstants.TASK_MODE_COMMIT, "KEY_1"))
                .thenReturn(null);
        doAnswer(invocation -> {
            ProductSkuGenerateTaskDO taskDO = invocation.getArgument(0);
            taskDO.setId(20L);
            return 1;
        }).when(skuGenerateTaskMapper).insert(any(ProductSkuGenerateTaskDO.class));

        ProductSkuGenerateTaskItemDO previewItem = ProductSkuGenerateTaskItemDO.builder()
                .id(1001L)
                .taskId(10L)
                .spuId(30001L)
                .specHash("hash-1")
                .specJson(JsonUtils.toJsonString(Collections.singletonList(
                        new ProductTemplateGenerateServiceImpl.SpecPair(1L, 11L))))
                .status(ProductTemplateConstants.TASK_ITEM_STATUS_PENDING)
                .build();
        when(skuGenerateTaskItemMapper.selectListByTaskId(10L)).thenReturn(Collections.singletonList(previewItem));
        when(productSkuMapper.selectListBySpuId(30001L)).thenReturn(Collections.emptyList());
        when(attributeDefinitionMapper.selectListByIds(anySet())).thenReturn(
                Collections.singletonList(ProductAttributeDefinitionDO.builder().id(1L).name("时长").build()));
        when(attributeOptionMapper.selectListByIds(anySet())).thenReturn(
                Collections.singletonList(ProductAttributeOptionDO.builder().id(11L).label("60分钟").build()));
        doAnswer(invocation -> {
            ProductSkuDO skuDO = invocation.getArgument(0);
            skuDO.setId(9001L);
            return 1;
        }).when(productSkuMapper).insert(any(ProductSkuDO.class));

        ProductSkuGenerateCommitReqVO commitReq = new ProductSkuGenerateCommitReqVO();
        commitReq.setTaskNo("SKU_PREVIEW_10");
        commitReq.setIdempotencyKey("KEY_1");
        ProductSkuGenerateCommitRespVO firstResp = service.commitSkuGenerate(commitReq);

        assertFalse(firstResp.getIdempotentHit());
        assertTrue(firstResp.getAccepted());
        verify(productSkuMapper, times(1)).insert(any(ProductSkuDO.class));

        ProductSkuGenerateTaskDO idempotentTask = ProductSkuGenerateTaskDO.builder()
                .taskNo("SKU_COMMIT_20")
                .status(ProductTemplateConstants.TASK_STATUS_SUCCESS)
                .build();
        when(skuGenerateTaskMapper.selectByIdempotency(30001L, ProductTemplateConstants.TASK_MODE_COMMIT, "KEY_2"))
                .thenReturn(idempotentTask);
        ProductSkuGenerateCommitReqVO secondReq = new ProductSkuGenerateCommitReqVO();
        secondReq.setTaskNo("SKU_PREVIEW_10");
        secondReq.setIdempotencyKey("KEY_2");

        ProductSkuGenerateCommitRespVO secondResp = service.commitSkuGenerate(secondReq);

        assertTrue(secondResp.getIdempotentHit());
        assertEquals("SKU_COMMIT_20", secondResp.getTaskNo());
    }

    @Test
    void retryFailedCommitTasks_shouldRetryFailedItems() {
        ProductSkuGeneratePreviewReqVO previewReq = buildPreviewReq();
        ProductSkuGenerateTaskDO task = ProductSkuGenerateTaskDO.builder()
                .id(30L)
                .taskNo("SKU_COMMIT_30")
                .mode(ProductTemplateConstants.TASK_MODE_COMMIT)
                .status(ProductTemplateConstants.TASK_STATUS_FAIL)
                .spuId(30001L)
                .requestJson(JsonUtils.toJsonString(previewReq))
                .retryCount(0)
                .build();
        when(skuGenerateTaskMapper.selectRetryableList(any(LocalDateTime.class), eq(10)))
                .thenReturn(Collections.singletonList(task));

        ProductSkuGenerateTaskItemDO failedItem = ProductSkuGenerateTaskItemDO.builder()
                .id(3001L)
                .taskId(30L)
                .spuId(30001L)
                .specHash("hash-1")
                .specJson(JsonUtils.toJsonString(Collections.singletonList(
                        new ProductTemplateGenerateServiceImpl.SpecPair(1L, 11L))))
                .status(ProductTemplateConstants.TASK_ITEM_STATUS_FAIL)
                .build();
        when(skuGenerateTaskItemMapper.selectListByTaskIdAndStatus(30L, ProductTemplateConstants.TASK_ITEM_STATUS_FAIL))
                .thenReturn(Collections.singletonList(failedItem));
        when(productSkuMapper.selectListBySpuId(30001L)).thenReturn(Collections.emptyList());
        when(attributeDefinitionMapper.selectListByIds(anySet())).thenReturn(
                Collections.singletonList(ProductAttributeDefinitionDO.builder().id(1L).name("时长").build()));
        when(attributeOptionMapper.selectListByIds(anySet())).thenReturn(
                Collections.singletonList(ProductAttributeOptionDO.builder().id(11L).label("60分钟").build()));
        doAnswer(invocation -> {
            ProductSkuDO skuDO = invocation.getArgument(0);
            skuDO.setId(9010L);
            return 1;
        }).when(productSkuMapper).insert(any(ProductSkuDO.class));
        doAnswer(invocation -> {
            ProductSkuGenerateTaskItemDO itemDO = invocation.getArgument(0);
            failedItem.setStatus(itemDO.getStatus());
            failedItem.setTargetSkuId(itemDO.getTargetSkuId());
            failedItem.setErrorMsg(itemDO.getErrorMsg());
            return 1;
        }).when(skuGenerateTaskItemMapper).updateById(any(ProductSkuGenerateTaskItemDO.class));
        when(skuGenerateTaskItemMapper.selectListByTaskId(30L)).thenReturn(Collections.singletonList(failedItem));

        int count = service.retryFailedCommitTasks(10);

        assertEquals(1, count);
        verify(skuGenerateTaskMapper, atLeastOnce()).updateById(any(ProductSkuGenerateTaskDO.class));
    }

    @Test
    void commitSkuGenerate_shouldReturnIdempotentHitWhenInsertDuplicateKey() {
        ProductSkuGeneratePreviewReqVO previewReq = buildPreviewReq();
        ProductSkuGenerateTaskDO previewTask = ProductSkuGenerateTaskDO.builder()
                .id(10L)
                .taskNo("SKU_PREVIEW_10")
                .spuId(30001L)
                .categoryId(101L)
                .templateVersionId(12L)
                .mode(ProductTemplateConstants.TASK_MODE_PREVIEW)
                .requestJson(JsonUtils.toJsonString(previewReq))
                .build();
        when(skuGenerateTaskMapper.selectByTaskNo("SKU_PREVIEW_10")).thenReturn(previewTask);
        when(skuGenerateTaskMapper.selectByIdempotency(30001L, ProductTemplateConstants.TASK_MODE_COMMIT, "KEY_DUP"))
                .thenReturn(null)
                .thenReturn(ProductSkuGenerateTaskDO.builder()
                        .id(21L)
                        .taskNo("SKU_COMMIT_21")
                        .status(ProductTemplateConstants.TASK_STATUS_RUNNING)
                        .build());
        doThrow(new DuplicateKeyException("duplicate")).when(skuGenerateTaskMapper).insert(any(ProductSkuGenerateTaskDO.class));

        ProductSkuGenerateCommitReqVO commitReq = new ProductSkuGenerateCommitReqVO();
        commitReq.setTaskNo("SKU_PREVIEW_10");
        commitReq.setIdempotencyKey("KEY_DUP");
        ProductSkuGenerateCommitRespVO respVO = service.commitSkuGenerate(commitReq);

        assertTrue(respVO.getAccepted());
        assertTrue(respVO.getIdempotentHit());
        assertEquals("SKU_COMMIT_21", respVO.getTaskNo());
        verify(skuGenerateTaskItemMapper, never()).selectListByTaskId(any(Long.class));
        verify(productSkuMapper, never()).insert(any(ProductSkuDO.class));
    }

    private static ProductSkuGeneratePreviewReqVO buildPreviewReq() {
        ProductSkuGeneratePreviewReqVO reqVO = new ProductSkuGeneratePreviewReqVO();
        reqVO.setSpuId(30001L);
        reqVO.setCategoryId(101L);
        ProductSkuGeneratePreviewReqVO.BaseSku baseSku = new ProductSkuGeneratePreviewReqVO.BaseSku();
        baseSku.setPrice(9800);
        baseSku.setMarketPrice(12800);
        baseSku.setCostPrice(5200);
        baseSku.setStock(100);
        reqVO.setBaseSku(baseSku);

        ProductSkuGeneratePreviewReqVO.SpecSelection a = new ProductSkuGeneratePreviewReqVO.SpecSelection();
        a.setAttributeId(1L);
        a.setOptionIds(Arrays.asList(11L, 12L));
        ProductSkuGeneratePreviewReqVO.SpecSelection b = new ProductSkuGeneratePreviewReqVO.SpecSelection();
        b.setAttributeId(2L);
        b.setOptionIds(Arrays.asList(21L, 22L));
        reqVO.setSpecSelections(Arrays.asList(a, b));
        return reqVO;
    }
}
