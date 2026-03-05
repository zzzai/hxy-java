package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreBatchLifecycleExecuteRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreBatchLifecycleReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogGetRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleChangeOrderActionReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleChangeOrderCreateReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleChangeOrderPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleChangeOrderRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleGuardBatchRecheckReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleGuardBatchRecheckRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogGetRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleBatchLogDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleChangeOrderDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleRecheckLogDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreLifecycleBatchLogService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreLifecycleRecheckLogService;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStoreControllerTest {

    @Mock
    private ProductStoreService productStoreService;
    @Mock
    private ProductStoreLifecycleBatchLogService lifecycleBatchLogService;
    @Mock
    private ProductStoreLifecycleRecheckLogService lifecycleRecheckLogService;

    @InjectMocks
    private ProductStoreController productStoreController;
    @InjectMocks
    private ProductStoreLifecycleBatchLogController lifecycleBatchLogController;
    @InjectMocks
    private ProductStoreLifecycleRecheckLogController lifecycleRecheckLogController;

    @Test
    void batchUpdateLifecycleExecute_shouldReturnExecutionSummary() {
        ProductStoreBatchLifecycleReqVO reqVO = new ProductStoreBatchLifecycleReqVO();
        reqVO.setStoreIds(Collections.singletonList(1001L));
        reqVO.setLifecycleStatus(35);
        reqVO.setReason("批量停业");

        ProductStoreBatchLifecycleExecuteRespVO respVO = new ProductStoreBatchLifecycleExecuteRespVO();
        respVO.setBatchNo("LIFECYCLE-20260304000000-ABCD1234");
        respVO.setTargetLifecycleStatus(35);
        respVO.setTotalCount(1);
        respVO.setSuccessCount(1);
        respVO.setBlockedCount(0);
        respVO.setWarningCount(0);
        when(productStoreService.batchUpdateLifecycleWithResult(any(ProductStoreBatchLifecycleReqVO.class)))
                .thenReturn(respVO);

        CommonResult<ProductStoreBatchLifecycleExecuteRespVO> result = productStoreController.batchUpdateLifecycleExecute(reqVO);

        assertEquals("LIFECYCLE-20260304000000-ABCD1234", result.getData().getBatchNo());
        assertEquals(1, result.getData().getSuccessCount());
        verify(productStoreService).batchUpdateLifecycleWithResult(any(ProductStoreBatchLifecycleReqVO.class));
    }

    @Test
    void pageLifecycleBatchLog_shouldPassFiltersAndMapResponse() {
        ProductStoreLifecycleBatchLogPageReqVO reqVO = new ProductStoreLifecycleBatchLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setBatchNo("LIFECYCLE-20260304");
        reqVO.setTargetLifecycleStatus(35);
        reqVO.setOperator("运营同学");
        reqVO.setSource("ADMIN_UI");

        ProductStoreLifecycleBatchLogDO row = ProductStoreLifecycleBatchLogDO.builder()
                .id(1L)
                .batchNo("LIFECYCLE-20260304000000-ABCD1234")
                .targetLifecycleStatus(35)
                .totalCount(3)
                .successCount(1)
                .blockedCount(1)
                .warningCount(1)
                .auditSummary("total=3,success=1,blocked=1,warning=1")
                .detailJson("{\"blocked\":[],\"warnings\":[]}")
                .operator("运营同学")
                .source("ADMIN_UI")
                .build();
        when(lifecycleBatchLogService.getLifecycleBatchLogPage(any(ProductStoreLifecycleBatchLogPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        CommonResult<PageResult<ProductStoreLifecycleBatchLogRespVO>> result = lifecycleBatchLogController.pageLifecycleBatchLog(reqVO);

        assertEquals(1L, result.getData().getTotal());
        assertEquals("LIFECYCLE-20260304000000-ABCD1234", result.getData().getList().get(0).getBatchNo());
        ArgumentCaptor<ProductStoreLifecycleBatchLogPageReqVO> reqCaptor = ArgumentCaptor.forClass(ProductStoreLifecycleBatchLogPageReqVO.class);
        verify(lifecycleBatchLogService).getLifecycleBatchLogPage(reqCaptor.capture());
        assertEquals("LIFECYCLE-20260304", reqCaptor.getValue().getBatchNo());
        assertEquals(35, reqCaptor.getValue().getTargetLifecycleStatus());
    }

    @Test
    void getLifecycleBatchLog_shouldReturnDetailAndParseView() {
        ProductStoreLifecycleBatchLogDO row = ProductStoreLifecycleBatchLogDO.builder()
                .id(2L)
                .batchNo("LIFECYCLE-20260304000100-ABCD1234")
                .targetLifecycleStatus(35)
                .totalCount(2)
                .successCount(1)
                .blockedCount(1)
                .warningCount(0)
                .auditSummary("total=2,success=1,blocked=1,warning=0")
                .guardRuleVersion("GRV-A1B2C3D4E5F6")
                .guardConfigSnapshotJson("{\"stockMode\":\"BLOCK\"}")
                .detailJson("{\"details\":[{\"storeId\":1001}]}")
                .operator("运营同学")
                .source("ADMIN_UI")
                .build();
        when(lifecycleBatchLogService.getLifecycleBatchLog(2L)).thenReturn(row);

        CommonResult<ProductStoreLifecycleBatchLogGetRespVO> result = lifecycleBatchLogController.getLifecycleBatchLog(2L);

        assertEquals(2L, result.getData().getId());
        assertEquals("GRV-A1B2C3D4E5F6", result.getData().getGuardRuleVersion());
        assertEquals(false, result.getData().getDetailParseError());
        assertEquals(true, result.getData().getDetailView().containsKey("details"));
    }

    @Test
    void getLifecycleBatchLog_shouldDegradeWhenDetailJsonMalformed() {
        ProductStoreLifecycleBatchLogDO row = ProductStoreLifecycleBatchLogDO.builder()
                .id(3L)
                .batchNo("LIFECYCLE-20260304000100-ABCD1234")
                .targetLifecycleStatus(35)
                .detailJson("{\"details\":[INVALID]")
                .build();
        when(lifecycleBatchLogService.getLifecycleBatchLog(3L)).thenReturn(row);

        CommonResult<ProductStoreLifecycleBatchLogGetRespVO> result = lifecycleBatchLogController.getLifecycleBatchLog(3L);

        assertEquals(3L, result.getData().getId());
        assertEquals(true, result.getData().getDetailParseError());
        assertEquals(null, result.getData().getDetailView());
    }

    @Test
    void recheckLifecycleGuardByBatch_shouldReturnSummary() {
        ProductStoreLifecycleGuardBatchRecheckReqVO reqVO = new ProductStoreLifecycleGuardBatchRecheckReqVO();
        reqVO.setBatchNo("LIFECYCLE-20260304000000-ABCD1234");

        ProductStoreLifecycleGuardBatchRecheckRespVO respVO = new ProductStoreLifecycleGuardBatchRecheckRespVO();
        respVO.setLogId(9L);
        respVO.setBatchNo("LIFECYCLE-20260304000000-ABCD1234");
        respVO.setTargetLifecycleStatus(35);
        respVO.setTotalCount(2);
        respVO.setBlockedCount(1);
        respVO.setWarningCount(1);
        when(productStoreService.recheckLifecycleGuardByBatch(any(ProductStoreLifecycleGuardBatchRecheckReqVO.class)))
                .thenReturn(respVO);

        CommonResult<ProductStoreLifecycleGuardBatchRecheckRespVO> result =
                productStoreController.recheckLifecycleGuardByBatch(reqVO);

        assertEquals(9L, result.getData().getLogId());
        assertEquals(2, result.getData().getTotalCount());
        assertEquals(1, result.getData().getBlockedCount());
        verify(productStoreService).recheckLifecycleGuardByBatch(any(ProductStoreLifecycleGuardBatchRecheckReqVO.class));
    }

    @Test
    void executeLifecycleGuardRecheckByBatch_shouldReturnSummaryAndRecheckNo() {
        ProductStoreLifecycleGuardBatchRecheckReqVO reqVO = new ProductStoreLifecycleGuardBatchRecheckReqVO();
        reqVO.setLogId(9L);

        ProductStoreLifecycleGuardBatchRecheckRespVO respVO = new ProductStoreLifecycleGuardBatchRecheckRespVO();
        respVO.setRecheckNo("RECHECK-20260305152000-ABCD1234");
        respVO.setLogId(9L);
        respVO.setBatchNo("LIFECYCLE-20260304000000-ABCD1234");
        respVO.setTargetLifecycleStatus(35);
        respVO.setTotalCount(2);
        respVO.setBlockedCount(1);
        respVO.setWarningCount(1);
        when(productStoreService.executeLifecycleGuardRecheckByBatch(any(ProductStoreLifecycleGuardBatchRecheckReqVO.class)))
                .thenReturn(respVO);

        CommonResult<ProductStoreLifecycleGuardBatchRecheckRespVO> result =
                productStoreController.executeLifecycleGuardRecheckByBatch(reqVO);

        assertEquals("RECHECK-20260305152000-ABCD1234", result.getData().getRecheckNo());
        assertEquals(2, result.getData().getTotalCount());
        verify(productStoreService).executeLifecycleGuardRecheckByBatch(any(ProductStoreLifecycleGuardBatchRecheckReqVO.class));
    }

    @Test
    void createLifecycleChangeOrder_shouldReturnId() {
        ProductStoreLifecycleChangeOrderCreateReqVO reqVO = new ProductStoreLifecycleChangeOrderCreateReqVO();
        reqVO.setStoreId(1001L);
        reqVO.setToLifecycleStatus(35);
        reqVO.setReason("临时停业");
        reqVO.setApplySource("ADMIN_UI");
        when(productStoreService.createLifecycleChangeOrder(any(ProductStoreLifecycleChangeOrderCreateReqVO.class)))
                .thenReturn(11L);

        CommonResult<Long> result = productStoreController.createLifecycleChangeOrder(reqVO);

        assertEquals(11L, result.getData());
        verify(productStoreService).createLifecycleChangeOrder(any(ProductStoreLifecycleChangeOrderCreateReqVO.class));
    }

    @Test
    void submitLifecycleChangeOrder_shouldInvokeService() {
        ProductStoreLifecycleChangeOrderActionReqVO reqVO = new ProductStoreLifecycleChangeOrderActionReqVO();
        reqVO.setId(11L);

        CommonResult<Boolean> result = productStoreController.submitLifecycleChangeOrder(reqVO);

        assertEquals(true, result.getData());
        verify(productStoreService).submitLifecycleChangeOrder(any(ProductStoreLifecycleChangeOrderActionReqVO.class));
    }

    @Test
    void approveLifecycleChangeOrder_shouldInvokeService() {
        ProductStoreLifecycleChangeOrderActionReqVO reqVO = new ProductStoreLifecycleChangeOrderActionReqVO();
        reqVO.setId(11L);
        reqVO.setRemark("审批通过");

        CommonResult<Boolean> result = productStoreController.approveLifecycleChangeOrder(reqVO);

        assertEquals(true, result.getData());
        verify(productStoreService).approveLifecycleChangeOrder(any(ProductStoreLifecycleChangeOrderActionReqVO.class));
    }

    @Test
    void rejectLifecycleChangeOrder_shouldInvokeService() {
        ProductStoreLifecycleChangeOrderActionReqVO reqVO = new ProductStoreLifecycleChangeOrderActionReqVO();
        reqVO.setId(11L);
        reqVO.setRemark("库存未清");

        CommonResult<Boolean> result = productStoreController.rejectLifecycleChangeOrder(reqVO);

        assertEquals(true, result.getData());
        verify(productStoreService).rejectLifecycleChangeOrder(any(ProductStoreLifecycleChangeOrderActionReqVO.class));
    }

    @Test
    void cancelLifecycleChangeOrder_shouldInvokeService() {
        ProductStoreLifecycleChangeOrderActionReqVO reqVO = new ProductStoreLifecycleChangeOrderActionReqVO();
        reqVO.setId(11L);
        reqVO.setRemark("申请人撤回");

        CommonResult<Boolean> result = productStoreController.cancelLifecycleChangeOrder(reqVO);

        assertEquals(true, result.getData());
        verify(productStoreService).cancelLifecycleChangeOrder(any(ProductStoreLifecycleChangeOrderActionReqVO.class));
    }

    @Test
    void pageLifecycleChangeOrder_shouldPassFiltersAndMapResponse() {
        ProductStoreLifecycleChangeOrderPageReqVO reqVO = new ProductStoreLifecycleChangeOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setOrderNo("LCO-20260305");
        reqVO.setStoreId(1001L);
        reqVO.setStatus(10);
        reqVO.setOverdue(true);
        reqVO.setLastActionCode("SUBMIT");
        reqVO.setLastActionOperator("运营同学");
        ProductStoreLifecycleChangeOrderDO order = ProductStoreLifecycleChangeOrderDO.builder()
                .id(11L)
                .orderNo("LCO-20260305101010-ABCD1234")
                .storeId(1001L)
                .storeName("上海徐汇店")
                .fromLifecycleStatus(30)
                .toLifecycleStatus(35)
                .reason("临时停业")
                .applyOperator("运营同学")
                .applySource("ADMIN_UI")
                .status(10)
                .guardBlocked(false)
                .guardWarnings("pending-order")
                .submitTime(java.time.LocalDateTime.of(2026, 3, 5, 10, 10, 10))
                .slaDeadlineTime(java.time.LocalDateTime.of(2026, 3, 6, 10, 10, 10))
                .lastActionCode("SUBMIT")
                .lastActionOperator("运营同学")
                .lastActionTime(java.time.LocalDateTime.of(2026, 3, 5, 10, 11, 10))
                .build();
        when(productStoreService.getLifecycleChangeOrderPage(any(ProductStoreLifecycleChangeOrderPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(order), 1L));

        CommonResult<PageResult<ProductStoreLifecycleChangeOrderRespVO>> result =
                productStoreController.pageLifecycleChangeOrder(reqVO);

        assertEquals(1L, result.getData().getTotal());
        assertEquals("LCO-20260305101010-ABCD1234", result.getData().getList().get(0).getOrderNo());
        ArgumentCaptor<ProductStoreLifecycleChangeOrderPageReqVO> reqCaptor =
                ArgumentCaptor.forClass(ProductStoreLifecycleChangeOrderPageReqVO.class);
        verify(productStoreService).getLifecycleChangeOrderPage(reqCaptor.capture());
        assertEquals("LCO-20260305", reqCaptor.getValue().getOrderNo());
        assertEquals(1001L, reqCaptor.getValue().getStoreId());
        assertEquals(10, reqCaptor.getValue().getStatus());
        assertEquals(true, reqCaptor.getValue().getOverdue());
        assertEquals("SUBMIT", reqCaptor.getValue().getLastActionCode());
        assertEquals("运营同学", reqCaptor.getValue().getLastActionOperator());
        assertEquals("SUBMIT", result.getData().getList().get(0).getLastActionCode());
    }

    @Test
    void getLifecycleChangeOrder_shouldReturnOrder() {
        ProductStoreLifecycleChangeOrderDO order = ProductStoreLifecycleChangeOrderDO.builder()
                .id(12L)
                .orderNo("LCO-20260305112233-ABCD1234")
                .storeId(1002L)
                .storeName("杭州西湖店")
                .fromLifecycleStatus(35)
                .toLifecycleStatus(30)
                .status(20)
                .approveOperator("审批同学")
                .submitTime(java.time.LocalDateTime.of(2026, 3, 5, 11, 22, 33))
                .slaDeadlineTime(java.time.LocalDateTime.of(2026, 3, 6, 11, 22, 33))
                .lastActionCode("APPROVE")
                .lastActionOperator("审批同学")
                .lastActionTime(java.time.LocalDateTime.of(2026, 3, 5, 11, 23, 33))
                .build();
        when(productStoreService.getLifecycleChangeOrder(12L)).thenReturn(order);

        CommonResult<ProductStoreLifecycleChangeOrderRespVO> result =
                productStoreController.getLifecycleChangeOrder(12L);

        assertEquals(12L, result.getData().getId());
        assertEquals("LCO-20260305112233-ABCD1234", result.getData().getOrderNo());
        assertEquals(20, result.getData().getStatus());
        assertEquals("APPROVE", result.getData().getLastActionCode());
        assertEquals("审批同学", result.getData().getLastActionOperator());
        verify(productStoreService).getLifecycleChangeOrder(12L);
    }

    @Test
    void pageLifecycleRecheckLog_shouldPassFiltersAndMapResponse() {
        ProductStoreLifecycleRecheckLogPageReqVO reqVO = new ProductStoreLifecycleRecheckLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setRecheckNo("RECHECK-20260305");
        reqVO.setLogId(11L);
        reqVO.setBatchNo("LIFECYCLE-20260305");
        reqVO.setTargetLifecycleStatus(35);
        reqVO.setOperator("运营同学");
        reqVO.setSource("ADMIN_UI");

        ProductStoreLifecycleRecheckLogDO row = ProductStoreLifecycleRecheckLogDO.builder()
                .id(1L)
                .recheckNo("RECHECK-20260305152000-ABCD1234")
                .logId(11L)
                .batchNo("LIFECYCLE-20260305000000-ABCD1234")
                .targetLifecycleStatus(35)
                .totalCount(3)
                .blockedCount(1)
                .warningCount(1)
                .detailParseError(false)
                .operator("运营同学")
                .source("ADMIN_UI")
                .build();
        when(lifecycleRecheckLogService.getLifecycleRecheckLogPage(any(ProductStoreLifecycleRecheckLogPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        CommonResult<PageResult<ProductStoreLifecycleRecheckLogRespVO>> result =
                lifecycleRecheckLogController.pageLifecycleRecheckLog(reqVO);

        assertEquals(1L, result.getData().getTotal());
        assertEquals("RECHECK-20260305152000-ABCD1234", result.getData().getList().get(0).getRecheckNo());
        ArgumentCaptor<ProductStoreLifecycleRecheckLogPageReqVO> reqCaptor =
                ArgumentCaptor.forClass(ProductStoreLifecycleRecheckLogPageReqVO.class);
        verify(lifecycleRecheckLogService).getLifecycleRecheckLogPage(reqCaptor.capture());
        assertEquals("RECHECK-20260305", reqCaptor.getValue().getRecheckNo());
        assertEquals(11L, reqCaptor.getValue().getLogId());
    }

    @Test
    void getLifecycleRecheckLog_shouldDegradeWhenDetailJsonMalformed() {
        ProductStoreLifecycleRecheckLogDO row = ProductStoreLifecycleRecheckLogDO.builder()
                .id(3L)
                .recheckNo("RECHECK-20260305152000-ABCD1234")
                .batchNo("LIFECYCLE-20260305000000-ABCD1234")
                .targetLifecycleStatus(35)
                .detailJson("{\"details\":[INVALID]")
                .detailParseError(false)
                .build();
        when(lifecycleRecheckLogService.getLifecycleRecheckLog(3L)).thenReturn(row);

        CommonResult<ProductStoreLifecycleRecheckLogGetRespVO> result =
                lifecycleRecheckLogController.getLifecycleRecheckLog(3L);

        assertEquals(3L, result.getData().getId());
        assertEquals(true, result.getData().getDetailParseError());
        assertEquals(null, result.getData().getDetailView());
    }
}
