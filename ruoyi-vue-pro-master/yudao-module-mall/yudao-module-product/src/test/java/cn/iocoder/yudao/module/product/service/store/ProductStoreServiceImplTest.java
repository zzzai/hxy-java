package cn.iocoder.yudao.module.product.service.store;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLaunchReadinessRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreBatchLifecycleReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreBatchLifecycleExecuteRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreCategoryDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleBatchLogDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagGroupDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagRelDO;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreAuditLogMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreCategoryMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuStockFlowMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSpuMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreTagGroupMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreTagMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreTagRelMapper;
import cn.iocoder.yudao.module.product.service.store.ProductStoreLifecycleBatchLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_HAS_PRODUCT_MAPPING;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_LIFECYCLE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_LIFECYCLE_TRANSITION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_TAG_GROUP_MUTEX_CONFLICT;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_TAG_GROUP_REQUIRED_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStoreServiceImplTest {

    @Mock
    private ProductStoreMapper storeMapper;
    @Mock
    private ProductStoreCategoryMapper storeCategoryMapper;
    @Mock
    private ProductStoreTagMapper storeTagMapper;
    @Mock
    private ProductStoreTagRelMapper storeTagRelMapper;
    @Mock
    private ProductStoreSpuMapper storeSpuMapper;
    @Mock
    private ProductStoreSkuMapper storeSkuMapper;
    @Mock
    private ProductStoreSkuStockFlowMapper storeSkuStockFlowMapper;
    @Mock
    private ProductStoreTagGroupMapper storeTagGroupMapper;
    @Mock
    private ProductStoreAuditLogMapper storeAuditLogMapper;
    @Mock
    private ProductStoreLifecycleBatchLogService lifecycleBatchLogService;

    @InjectMocks
    private ProductStoreServiceImpl productStoreService;

    @Test
    void saveStore_shouldCreateAndSyncTagRelations() {
        ProductStoreSaveReqVO reqVO = new ProductStoreSaveReqVO();
        reqVO.setCode("SH-001");
        reqVO.setName("荷小悦-上海徐汇店");
        reqVO.setCategoryId(10L);
        reqVO.setTagIds(Arrays.asList(101L, 102L, 102L));

        ProductStoreCategoryDO category = new ProductStoreCategoryDO();
        category.setId(10L);
        when(storeCategoryMapper.selectById(10L)).thenReturn(category);
        when(storeMapper.selectByCode("SH-001")).thenReturn(null);

        ProductStoreTagDO tag1 = ProductStoreTagDO.builder().id(101L).groupId(1L).status(1).build();
        ProductStoreTagDO tag2 = ProductStoreTagDO.builder().id(102L).groupId(2L).status(1).build();
        when(storeTagMapper.selectBatchIds(Arrays.asList(101L, 102L))).thenReturn(Arrays.asList(tag1, tag2));
        when(storeTagGroupMapper.selectByIds(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(
                ProductStoreTagGroupDO.builder().id(1L).name("经营属性").mutex(0).build(),
                ProductStoreTagGroupDO.builder().id(2L).name("服务能力").mutex(0).build()
        ));
        when(storeTagGroupMapper.selectRequiredGroups(1)).thenReturn(Arrays.asList(
                ProductStoreTagGroupDO.builder().id(1L).name("经营属性").required(1).build(),
                ProductStoreTagGroupDO.builder().id(2L).name("服务能力").required(1).build()
        ));

        doAnswer(invocation -> {
            ProductStoreDO store = invocation.getArgument(0);
            store.setId(1001L);
            return 1;
        }).when(storeMapper).insert(any(ProductStoreDO.class));

        Long id = productStoreService.saveStore(reqVO);

        assertEquals(1001L, id);
        verify(storeTagRelMapper).deleteByStoreId(1001L);
        verify(storeTagRelMapper, times(2)).insert(any(ProductStoreTagRelDO.class));
    }

    @Test
    void saveStore_shouldThrowWhenCategoryMissing() {
        ProductStoreSaveReqVO reqVO = new ProductStoreSaveReqVO();
        reqVO.setCode("SZ-001");
        reqVO.setName("荷小悦-深圳南山店");
        reqVO.setCategoryId(66L);
        reqVO.setTagIds(Collections.emptyList());

        when(storeCategoryMapper.selectById(66L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> productStoreService.saveStore(reqVO));
        assertEquals(STORE_CATEGORY_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void saveStore_shouldThrowWhenRequiredTagGroupMissing() {
        ProductStoreSaveReqVO reqVO = new ProductStoreSaveReqVO();
        reqVO.setCode("BJ-001");
        reqVO.setName("荷小悦-北京朝阳店");
        reqVO.setCategoryId(10L);
        reqVO.setTagIds(Collections.emptyList());

        ProductStoreCategoryDO category = new ProductStoreCategoryDO();
        category.setId(10L);
        when(storeCategoryMapper.selectById(10L)).thenReturn(category);
        when(storeMapper.selectByCode("BJ-001")).thenReturn(null);
        when(storeTagGroupMapper.selectRequiredGroups(1)).thenReturn(Collections.singletonList(
                ProductStoreTagGroupDO.builder().id(1L).name("经营属性").required(1).build()
        ));

        ServiceException ex = assertThrows(ServiceException.class, () -> productStoreService.saveStore(reqVO));
        assertEquals(STORE_TAG_GROUP_REQUIRED_MISSING.getCode(), ex.getCode());
    }

    @Test
    void saveStore_shouldThrowWhenMutexTagGroupConflict() {
        ProductStoreSaveReqVO reqVO = new ProductStoreSaveReqVO();
        reqVO.setCode("GZ-001");
        reqVO.setName("荷小悦-广州天河店");
        reqVO.setCategoryId(10L);
        reqVO.setTagIds(Arrays.asList(101L, 102L));

        ProductStoreCategoryDO category = new ProductStoreCategoryDO();
        category.setId(10L);
        when(storeCategoryMapper.selectById(10L)).thenReturn(category);
        when(storeMapper.selectByCode("GZ-001")).thenReturn(null);

        ProductStoreTagDO tag1 = ProductStoreTagDO.builder().id(101L).groupId(1L).status(1).build();
        ProductStoreTagDO tag2 = ProductStoreTagDO.builder().id(102L).groupId(1L).status(1).build();
        when(storeTagMapper.selectBatchIds(Arrays.asList(101L, 102L))).thenReturn(Arrays.asList(tag1, tag2));
        when(storeTagGroupMapper.selectByIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(
                ProductStoreTagGroupDO.builder().id(1L).name("经营属性").mutex(1).build()
        ));
        when(storeTagGroupMapper.selectRequiredGroups(1)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> productStoreService.saveStore(reqVO));
        assertEquals(STORE_TAG_GROUP_MUTEX_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void updateStoreLifecycle_shouldThrowWhenTransitionNotAllowed() {
        ProductStoreDO store = ProductStoreDO.builder().id(1001L).status(1).lifecycleStatus(40).build();
        when(storeMapper.selectById(1001L)).thenReturn(store);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.updateStoreLifecycle(1001L, 30, "恢复营业"));
        assertEquals(STORE_LIFECYCLE_TRANSITION_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void updateStoreLifecycle_shouldAllowOperatingToSuspended() {
        ProductStoreDO before = ProductStoreDO.builder().id(1002L).status(1).lifecycleStatus(30).build();
        ProductStoreDO after = ProductStoreDO.builder().id(1002L).status(1).lifecycleStatus(35).build();
        when(storeMapper.selectById(1002L)).thenReturn(before, after);

        assertDoesNotThrow(() -> productStoreService.updateStoreLifecycle(1002L, 35, "临时停业"));
        verify(storeMapper).updateById(any(ProductStoreDO.class));
    }

    @Test
    void updateStoreLifecycle_shouldAllowSuspendedToOperating() {
        ProductStoreDO before = ProductStoreDO.builder().id(1003L).status(1).lifecycleStatus(35).build();
        ProductStoreDO after = ProductStoreDO.builder().id(1003L).status(1).lifecycleStatus(30).build();
        when(storeMapper.selectById(1003L)).thenReturn(before, after);

        assertDoesNotThrow(() -> productStoreService.updateStoreLifecycle(1003L, 30, "恢复营业"));
        verify(storeMapper).updateById(any(ProductStoreDO.class));
    }

    @Test
    void updateStoreLifecycle_shouldRequireReasonWhenSuspended() {
        ProductStoreDO before = ProductStoreDO.builder().id(1004L).status(1).lifecycleStatus(30).build();
        when(storeMapper.selectById(1004L)).thenReturn(before);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.updateStoreLifecycle(1004L, 35, " "));
        assertEquals(STORE_LIFECYCLE_REASON_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void updateStoreLifecycle_shouldRequireReasonWhenClosed() {
        ProductStoreDO before = ProductStoreDO.builder().id(1005L).status(1).lifecycleStatus(35).build();
        when(storeMapper.selectById(1005L)).thenReturn(before);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.updateStoreLifecycle(1005L, 40, null));
        assertEquals(STORE_LIFECYCLE_REASON_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void updateStoreLifecycle_shouldThrowWhenSuspendedAndHasPositiveStock() {
        ProductStoreDO before = ProductStoreDO.builder().id(1006L).status(1).lifecycleStatus(30).build();
        when(storeMapper.selectById(1006L)).thenReturn(before);
        when(storeSpuMapper.selectCountByStoreId(1006L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1006L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(1006L)).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.updateStoreLifecycle(1006L, 35, "临时停业"));
        assertEquals(STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK.getCode(), ex.getCode());
    }

    @Test
    void updateStoreLifecycle_shouldThrowWhenClosedAndHasPendingStockFlow() {
        ProductStoreDO before = ProductStoreDO.builder().id(1007L).status(1).lifecycleStatus(35).build();
        when(storeMapper.selectById(1007L)).thenReturn(before);
        when(storeSpuMapper.selectCountByStoreId(1007L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1007L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(1007L)).thenReturn(0L);
        when(storeSkuStockFlowMapper.selectCountByStoreIdAndStatuses(eq(1007L), any())).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.updateStoreLifecycle(1007L, 40, "闭店"));
        assertEquals(STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW.getCode(), ex.getCode());
    }

    @Test
    void getLaunchReadiness_shouldReturnNotReadyWhenMissingRequiredFields() {
        ProductStoreDO store = ProductStoreDO.builder()
                .id(1001L)
                .categoryId(10L)
                .status(0)
                .lifecycleStatus(20)
                .contactMobile("")
                .address("")
                .openingTime("")
                .closingTime("")
                .build();
        when(storeMapper.selectById(1001L)).thenReturn(store);
        when(storeCategoryMapper.selectById(10L)).thenReturn(null);
        when(storeTagRelMapper.selectListByStoreId(1001L)).thenReturn(Collections.emptyList());
        when(storeTagGroupMapper.selectRequiredGroups(1)).thenReturn(Collections.singletonList(
                ProductStoreTagGroupDO.builder().id(1L).name("经营属性").required(1).build()
        ));
        when(storeSpuMapper.selectCountByStoreId(1001L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1001L)).thenReturn(0L);

        ProductStoreLaunchReadinessRespVO readiness = productStoreService.getLaunchReadiness(1001L);

        assertFalse(readiness.getReady());
        assertFalse(readiness.getReasons().isEmpty());
    }

    @Test
    void deleteStore_shouldThrowWhenHasProductMapping() {
        ProductStoreDO store = new ProductStoreDO();
        store.setId(1001L);
        when(storeMapper.selectById(1001L)).thenReturn(store);
        when(storeSpuMapper.selectCountByStoreId(1001L)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> productStoreService.deleteStore(1001L));
        assertEquals(STORE_HAS_PRODUCT_MAPPING.getCode(), ex.getCode());
    }

    @Test
    void batchUpdateLifecycleWithResult_shouldPersistBatchLogAndContainBlockedWarnings() {
        ProductStoreBatchLifecycleReqVO reqVO = new ProductStoreBatchLifecycleReqVO();
        reqVO.setStoreIds(Arrays.asList(2001L, 2002L, 2003L));
        reqVO.setLifecycleStatus(35);
        reqVO.setReason("批量停业巡检");

        ProductStoreDO store1Before = ProductStoreDO.builder().id(2001L).name("上海徐汇店").status(1).lifecycleStatus(30).build();
        ProductStoreDO store1After = ProductStoreDO.builder().id(2001L).name("上海徐汇店").status(1).lifecycleStatus(35).build();
        ProductStoreDO store2 = ProductStoreDO.builder().id(2002L).name("杭州西湖店").status(1).lifecycleStatus(30).build();
        ProductStoreDO store3 = ProductStoreDO.builder().id(2003L).name("苏州工业园店").status(1).lifecycleStatus(35).build();
        when(storeMapper.selectById(2001L)).thenReturn(store1Before, store1Before, store1After);
        when(storeMapper.selectById(2002L)).thenReturn(store2, store2);
        when(storeMapper.selectById(2003L)).thenReturn(store3);
        when(storeSpuMapper.selectCountByStoreId(2001L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(2001L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(2001L)).thenReturn(0L);
        when(storeSkuStockFlowMapper.selectCountByStoreIdAndStatuses(eq(2001L), any())).thenReturn(0L);
        when(storeSpuMapper.selectCountByStoreId(2002L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(2002L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(2002L)).thenReturn(3L);

        ProductStoreBatchLifecycleExecuteRespVO respVO = productStoreService.batchUpdateLifecycleWithResult(reqVO);

        assertEquals(3, respVO.getTotalCount());
        assertEquals(1, respVO.getSuccessCount());
        assertEquals(1, respVO.getBlockedCount());
        assertEquals(1, respVO.getWarningCount());

        ArgumentCaptor<ProductStoreLifecycleBatchLogDO> logCaptor = ArgumentCaptor.forClass(ProductStoreLifecycleBatchLogDO.class);
        verify(lifecycleBatchLogService).createLifecycleBatchLog(logCaptor.capture());
        String detailJson = logCaptor.getValue().getDetailJson();
        assertEquals("ADMIN_UI", logCaptor.getValue().getSource());
        assertFalse(detailJson.isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(detailJson.contains("\"blocked\""));
        org.junit.jupiter.api.Assertions.assertTrue(detailJson.contains("\"warnings\""));
        verify(storeMapper, times(1)).updateById(any(ProductStoreDO.class));
        verifyNoMoreInteractions(lifecycleBatchLogService);
    }
}
