package cn.iocoder.yudao.module.product.service.store;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreBatchLifecycleReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreBatchLifecycleResultRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleGuardRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLaunchReadinessRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSaveReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreAuditLogDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreCategoryDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
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
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuStockFlowStatusEnum;
import cn.iocoder.yudao.module.trade.api.store.TradeStoreLifecycleGuardApi;
import cn.iocoder.yudao.module.trade.api.store.dto.TradeStoreLifecycleGuardStatRespDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_LIFECYCLE_CLOSE_BLOCKED_BY_PENDING_ORDER;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_LIFECYCLE_CLOSE_BLOCKED_BY_INFLIGHT_TICKET;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_LIFECYCLE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_LIFECYCLE_TRANSITION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_TAG_GROUP_MUTEX_CONFLICT;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_TAG_GROUP_REQUIRED_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
    private TradeStoreLifecycleGuardApi tradeStoreLifecycleGuardApi;
    @Mock
    private ConfigApi configApi;

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
    void updateStoreLifecycle_shouldThrowWhenClosedAndHasFailedStockFlow() {
        ProductStoreDO before = ProductStoreDO.builder().id(1008L).status(1).lifecycleStatus(35).build();
        when(storeMapper.selectById(1008L)).thenReturn(before);
        when(storeSpuMapper.selectCountByStoreId(1008L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1008L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(1008L)).thenReturn(0L);
        doAnswer(invocation -> {
            List<Integer> statuses = invocation.getArgument(1);
            return statuses.contains(ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus()) ? 1L : 0L;
        }).when(storeSkuStockFlowMapper).selectCountByStoreIdAndStatuses(eq(1008L), any());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.updateStoreLifecycle(1008L, 40, "闭店"));
        assertEquals(STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW.getCode(), ex.getCode());
    }

    @Test
    void updateStoreLifecycle_shouldThrowWhenSuspendedAndHasFailedStockFlow() {
        ProductStoreDO before = ProductStoreDO.builder().id(1009L).status(1).lifecycleStatus(30).build();
        when(storeMapper.selectById(1009L)).thenReturn(before);
        when(storeSpuMapper.selectCountByStoreId(1009L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1009L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(1009L)).thenReturn(0L);
        doAnswer(invocation -> {
            List<Integer> statuses = invocation.getArgument(1);
            return statuses.contains(ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus()) ? 1L : 0L;
        }).when(storeSkuStockFlowMapper).selectCountByStoreIdAndStatuses(eq(1009L), any());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.updateStoreLifecycle(1009L, 35, "临时停业"));
        assertEquals(STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW.getCode(), ex.getCode());
    }

    @Test
    void updateStoreLifecycle_shouldThrowWhenSuspendedAndHasPendingOrder() {
        ProductStoreDO before = ProductStoreDO.builder().id(1010L).status(1).lifecycleStatus(30).build();
        when(storeMapper.selectById(1010L)).thenReturn(before);
        when(storeSpuMapper.selectCountByStoreId(1010L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1010L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(1010L)).thenReturn(0L);
        when(storeSkuStockFlowMapper.selectCountByStoreIdAndStatuses(eq(1010L), any())).thenReturn(0L);
        when(tradeStoreLifecycleGuardApi.getStoreLifecycleGuardStat(1010L))
                .thenReturn(new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(1L).setInflightTicketCount(0L));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.updateStoreLifecycle(1010L, 35, "临时停业"));
        assertEquals(STORE_LIFECYCLE_CLOSE_BLOCKED_BY_PENDING_ORDER.getCode(), ex.getCode());
    }

    @Test
    void updateStoreLifecycle_shouldThrowWhenClosedAndHasInflightTicket() {
        ProductStoreDO before = ProductStoreDO.builder().id(1011L).status(1).lifecycleStatus(35).build();
        when(storeMapper.selectById(1011L)).thenReturn(before);
        when(storeSpuMapper.selectCountByStoreId(1011L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1011L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(1011L)).thenReturn(0L);
        when(storeSkuStockFlowMapper.selectCountByStoreIdAndStatuses(eq(1011L), any())).thenReturn(0L);
        when(tradeStoreLifecycleGuardApi.getStoreLifecycleGuardStat(1011L))
                .thenReturn(new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(0L).setInflightTicketCount(2L));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.updateStoreLifecycle(1011L, 40, "闭店"));
        assertEquals(STORE_LIFECYCLE_CLOSE_BLOCKED_BY_INFLIGHT_TICKET.getCode(), ex.getCode());
    }

    @Test
    void updateStoreLifecycle_shouldAllowWarnModeWhenPendingOrderExists() {
        ProductStoreDO before = ProductStoreDO.builder().id(1012L).status(1).lifecycleStatus(30).build();
        ProductStoreDO after = ProductStoreDO.builder().id(1012L).status(1).lifecycleStatus(35).build();
        when(storeMapper.selectById(1012L)).thenReturn(before, after);
        when(storeSpuMapper.selectCountByStoreId(1012L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1012L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(1012L)).thenReturn(0L);
        when(storeSkuStockFlowMapper.selectCountByStoreIdAndStatuses(eq(1012L), any())).thenReturn(0L);
        when(tradeStoreLifecycleGuardApi.getStoreLifecycleGuardStat(1012L))
                .thenReturn(new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(3L).setInflightTicketCount(0L));
        when(configApi.getConfigValueByKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(configApi.getConfigValueByKey("hxy.store.lifecycle.guard.pending-order.mode")).thenReturn("WARN");

        assertDoesNotThrow(() -> productStoreService.updateStoreLifecycle(1012L, 35, "临时停业"));
        verify(storeAuditLogMapper).insert(argThat((ProductStoreAuditLogDO log) ->
                log != null
                        && "LIFECYCLE".equals(log.getAction())
                        && log.getReason() != null
                        && log.getReason().contains("LIFECYCLE_GUARD_WARN:pending-order:count=3")));
    }

    @Test
    void getLifecycleGuard_shouldReturnBlockedWhenStockFlowExists() {
        ProductStoreDO store = ProductStoreDO.builder().id(1013L).status(1).lifecycleStatus(35).build();
        when(storeMapper.selectById(1013L)).thenReturn(store);
        when(storeSpuMapper.selectCountByStoreId(1013L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1013L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(1013L)).thenReturn(0L);
        when(storeSkuStockFlowMapper.selectCountByStoreIdAndStatuses(eq(1013L), any())).thenReturn(2L);

        ProductStoreLifecycleGuardRespVO respVO = productStoreService.getLifecycleGuard(1013L, 40);

        assertTrue(respVO.getBlocked());
        assertEquals(STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW.getCode(), respVO.getBlockedCode());
        assertTrue(respVO.getWarnings().isEmpty());
    }

    @Test
    void getLifecycleGuard_shouldReturnWarnWhenPendingOrderWarnMode() {
        ProductStoreDO store = ProductStoreDO.builder().id(1014L).status(1).lifecycleStatus(30).build();
        when(storeMapper.selectById(1014L)).thenReturn(store);
        when(storeSpuMapper.selectCountByStoreId(1014L)).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(1014L)).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(1014L)).thenReturn(0L);
        when(storeSkuStockFlowMapper.selectCountByStoreIdAndStatuses(eq(1014L), any())).thenReturn(0L);
        when(tradeStoreLifecycleGuardApi.getStoreLifecycleGuardStat(1014L))
                .thenReturn(new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(5L).setInflightTicketCount(0L));
        when(configApi.getConfigValueByKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(configApi.getConfigValueByKey("hxy.store.lifecycle.guard.pending-order.mode")).thenReturn("WARN");

        ProductStoreLifecycleGuardRespVO respVO = productStoreService.getLifecycleGuard(1014L, 35);

        assertFalse(respVO.getBlocked());
        assertTrue(respVO.getWarnings().contains("LIFECYCLE_GUARD_WARN:pending-order:count=5"));
    }

    @Test
    void getLifecycleGuardBatch_shouldReturnDeduplicatedResults() {
        ProductStoreDO blockedStore = ProductStoreDO.builder().id(1015L).status(1).lifecycleStatus(35).build();
        ProductStoreDO warnStore = ProductStoreDO.builder().id(1016L).status(1).lifecycleStatus(30).build();
        when(storeMapper.selectById(1015L)).thenReturn(blockedStore);
        when(storeMapper.selectById(1016L)).thenReturn(warnStore);
        when(storeSpuMapper.selectCountByStoreId(any(Long.class))).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(any(Long.class))).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(any(Long.class))).thenReturn(0L);
        doAnswer(invocation -> {
            Long storeId = invocation.getArgument(0);
            return storeId != null && storeId.equals(1015L) ? 1L : 0L;
        }).when(storeSkuStockFlowMapper).selectCountByStoreIdAndStatuses(any(Long.class), any());
        doAnswer(invocation -> {
            Long storeId = invocation.getArgument(0);
            if (storeId != null && storeId.equals(1016L)) {
                return new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(2L).setInflightTicketCount(0L);
            }
            return new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(0L).setInflightTicketCount(0L);
        }).when(tradeStoreLifecycleGuardApi).getStoreLifecycleGuardStat(any(Long.class));
        when(configApi.getConfigValueByKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(configApi.getConfigValueByKey("hxy.store.lifecycle.guard.pending-order.mode")).thenReturn("WARN");

        List<ProductStoreLifecycleGuardRespVO> results = productStoreService.getLifecycleGuardBatch(
                Arrays.asList(1015L, 1015L, 1016L), 35);

        assertEquals(2, results.size());
        ProductStoreLifecycleGuardRespVO blocked = results.stream()
                .filter(item -> item.getStoreId().equals(1015L))
                .findFirst().orElseThrow();
        ProductStoreLifecycleGuardRespVO warned = results.stream()
                .filter(item -> item.getStoreId().equals(1016L))
                .findFirst().orElseThrow();
        assertTrue(blocked.getBlocked());
        assertEquals(STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW.getCode(), blocked.getBlockedCode());
        assertFalse(warned.getBlocked());
        assertTrue(warned.getWarnings().contains("LIFECYCLE_GUARD_WARN:pending-order:count=2"));
    }

    @Test
    void getLifecycleGuardBatch_shouldThrowWhenStoreMissing() {
        when(storeMapper.selectById(1099L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.getLifecycleGuardBatch(Collections.singletonList(1099L), 35));
        assertEquals(cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void batchUpdateLifecycleWithResult_shouldExecuteUnblockedAndReturnSummary() {
        ProductStoreDO blockedStore = ProductStoreDO.builder().id(1021L).status(1).lifecycleStatus(30).build();
        ProductStoreDO warnStoreBefore = ProductStoreDO.builder().id(1022L).status(1).lifecycleStatus(30).build();
        ProductStoreDO warnStoreAfter = ProductStoreDO.builder().id(1022L).status(1).lifecycleStatus(35).build();
        ProductStoreDO cleanStoreBefore = ProductStoreDO.builder().id(1023L).status(1).lifecycleStatus(30).build();
        ProductStoreDO cleanStoreAfter = ProductStoreDO.builder().id(1023L).status(1).lifecycleStatus(35).build();
        when(storeMapper.selectById(1021L)).thenReturn(blockedStore);
        when(storeMapper.selectById(1022L)).thenReturn(warnStoreBefore, warnStoreBefore, warnStoreAfter);
        when(storeMapper.selectById(1023L)).thenReturn(cleanStoreBefore, cleanStoreBefore, cleanStoreAfter);
        when(storeSpuMapper.selectCountByStoreId(any(Long.class))).thenReturn(0L);
        when(storeSkuMapper.selectCountByStoreId(any(Long.class))).thenReturn(0L);
        when(storeSkuMapper.selectPositiveStockCountByStoreId(any(Long.class))).thenReturn(0L);
        doAnswer(invocation -> {
            Long storeId = invocation.getArgument(0);
            return storeId != null && storeId.equals(1021L) ? 1L : 0L;
        }).when(storeSkuStockFlowMapper).selectCountByStoreIdAndStatuses(any(Long.class), any());
        doAnswer(invocation -> {
            Long storeId = invocation.getArgument(0);
            if (storeId != null && storeId.equals(1022L)) {
                return new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(2L).setInflightTicketCount(0L);
            }
            return new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(0L).setInflightTicketCount(0L);
        }).when(tradeStoreLifecycleGuardApi).getStoreLifecycleGuardStat(any(Long.class));
        when(configApi.getConfigValueByKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(configApi.getConfigValueByKey("hxy.store.lifecycle.guard.pending-order.mode")).thenReturn("WARN");
        ProductStoreBatchLifecycleReqVO reqVO = new ProductStoreBatchLifecycleReqVO();
        reqVO.setStoreIds(Arrays.asList(1021L, 1022L, 1023L));
        reqVO.setLifecycleStatus(35);
        reqVO.setReason("批量停业");

        ProductStoreBatchLifecycleResultRespVO result = productStoreService.batchUpdateLifecycleWithResult(reqVO);

        assertEquals(3, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getBlockedCount());
        assertEquals(1, result.getWarningCount());
        assertEquals("LIFECYCLE_BATCH_EXEC:target=35,total=3,success=2,blocked=1,warn=1", result.getAuditSummary());
        verify(storeMapper, times(2)).updateById(any(ProductStoreDO.class));
    }

    @Test
    void batchUpdateLifecycleWithResult_shouldThrowWhenStoreNotExists() {
        when(storeMapper.selectById(1040L)).thenReturn(null);
        ProductStoreBatchLifecycleReqVO reqVO = new ProductStoreBatchLifecycleReqVO();
        reqVO.setStoreIds(Collections.singletonList(1040L));
        reqVO.setLifecycleStatus(35);
        reqVO.setReason("批量停业");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreService.batchUpdateLifecycleWithResult(reqVO));
        assertEquals(cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_NOT_EXISTS.getCode(), ex.getCode());
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
}
