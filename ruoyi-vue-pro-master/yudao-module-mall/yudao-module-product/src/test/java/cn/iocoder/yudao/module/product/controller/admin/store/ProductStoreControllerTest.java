package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreBatchLifecycleExecuteRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreBatchLifecycleReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogGetRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleGuardBatchRecheckReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleGuardBatchRecheckRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleBatchLogDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreLifecycleBatchLogService;
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

    @InjectMocks
    private ProductStoreController productStoreController;
    @InjectMocks
    private ProductStoreLifecycleBatchLogController lifecycleBatchLogController;

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
}
