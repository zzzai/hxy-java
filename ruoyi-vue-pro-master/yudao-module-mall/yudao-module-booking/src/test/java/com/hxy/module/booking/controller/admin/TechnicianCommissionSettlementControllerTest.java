package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementLogRespVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementNotifyOutboxPageReqVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementNotifyOutboxRetryReqVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementNotifyOutboxBatchRetryRespVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementNotifyOutboxRespVO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementLogDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementNotifyOutboxDO;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementRespVO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementDO;
import com.hxy.module.booking.enums.CommissionSettlementStatusEnum;
import com.hxy.module.booking.service.dto.TechnicianCommissionNotifyBatchRetryResult;
import com.hxy.module.booking.service.TechnicianCommissionSettlementService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicianCommissionSettlementControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TechnicianCommissionSettlementController controller;

    @Mock
    private TechnicianCommissionSettlementService settlementService;

    @Test
    void shouldGetSlaOverduePendingList() {
        TechnicianCommissionSettlementDO settlement = new TechnicianCommissionSettlementDO();
        settlement.setId(1001L);
        settlement.setStatus(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewDeadlineTime(LocalDateTime.now().minusMinutes(10));
        when(settlementService.getSlaOverduePendingList(20)).thenReturn(Collections.singletonList(settlement));

        CommonResult<List<TechnicianCommissionSettlementRespVO>> result = controller.slaOverdueList(20);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(1001L, result.getData().get(0).getId());
        assertTrue(Boolean.TRUE.equals(result.getData().get(0).getOverdue()));
        verify(settlementService).getSlaOverduePendingList(20);
    }

    @Test
    void shouldMarkGetResultAsNotOverdueWhenNotPending() {
        TechnicianCommissionSettlementDO settlement = new TechnicianCommissionSettlementDO();
        settlement.setId(1002L);
        settlement.setStatus(CommissionSettlementStatusEnum.PAID.getStatus());
        settlement.setReviewDeadlineTime(LocalDateTime.now().minusMinutes(30));
        when(settlementService.getSettlement(1002L)).thenReturn(settlement);

        CommonResult<TechnicianCommissionSettlementRespVO> result = controller.get(1002L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertFalse(Boolean.TRUE.equals(result.getData().getOverdue()));
        verify(settlementService).getSettlement(1002L);
    }

    @Test
    void shouldGetSettlementOperationLogs() {
        TechnicianCommissionSettlementLogDO logDO = new TechnicianCommissionSettlementLogDO();
        logDO.setId(3001L);
        logDO.setSettlementId(1001L);
        logDO.setAction("SUBMIT_REVIEW");
        when(settlementService.getOperationLogList(1001L)).thenReturn(Collections.singletonList(logDO));

        CommonResult<List<TechnicianCommissionSettlementLogRespVO>> result = controller.logList(1001L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("SUBMIT_REVIEW", result.getData().get(0).getAction());
        verify(settlementService).getOperationLogList(1001L);
    }

    @Test
    void shouldGetNotifyOutboxList() {
        TechnicianCommissionSettlementNotifyOutboxDO outboxDO = new TechnicianCommissionSettlementNotifyOutboxDO();
        outboxDO.setId(5001L);
        outboxDO.setSettlementId(1001L);
        outboxDO.setNotifyType("P1_WARN");
        outboxDO.setStatus(0);
        when(settlementService.getNotifyOutboxList(1001L, 0, 30))
                .thenReturn(Collections.singletonList(outboxDO));

        CommonResult<List<TechnicianCommissionSettlementNotifyOutboxRespVO>> result =
                controller.notifyOutboxList(1001L, 0, 30);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(5001L, result.getData().get(0).getId());
        verify(settlementService).getNotifyOutboxList(1001L, 0, 30);
    }

    @Test
    void shouldGetNotifyOutboxPage() {
        TechnicianCommissionSettlementNotifyOutboxDO outboxDO = new TechnicianCommissionSettlementNotifyOutboxDO();
        outboxDO.setId(5002L);
        outboxDO.setSettlementId(1002L);
        outboxDO.setNotifyType("P0_ESCALATE");
        outboxDO.setStatus(2);
        TechnicianCommissionSettlementNotifyOutboxPageReqVO reqVO = new TechnicianCommissionSettlementNotifyOutboxPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSettlementId(1002L);
        reqVO.setStatus(2);
        when(settlementService.getNotifyOutboxPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(outboxDO), 1L));

        CommonResult<PageResult<TechnicianCommissionSettlementNotifyOutboxRespVO>> result =
                controller.notifyOutboxPage(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(5002L, result.getData().getList().get(0).getId());
        verify(settlementService).getNotifyOutboxPage(reqVO);
    }

    @Test
    void shouldRetryNotifyOutbox() {
        TechnicianCommissionSettlementNotifyOutboxRetryReqVO reqVO = new TechnicianCommissionSettlementNotifyOutboxRetryReqVO();
        reqVO.setIds(List.of(5001L, 5002L));
        reqVO.setReason("manual-retry");
        when(settlementService.retryNotifyOutbox(reqVO.getIds(), null, reqVO.getReason())).thenReturn(2);

        CommonResult<Integer> result = controller.retryNotifyOutbox(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getData());
        verify(settlementService).retryNotifyOutbox(reqVO.getIds(), null, reqVO.getReason());
    }

    @Test
    void shouldBatchRetryNotifyOutboxAndReturnSummary() {
        TechnicianCommissionSettlementNotifyOutboxRetryReqVO reqVO = new TechnicianCommissionSettlementNotifyOutboxRetryReqVO();
        reqVO.setIds(List.of(5001L, 5002L, 5003L));
        reqVO.setReason("ops-batch-retry");
        TechnicianCommissionNotifyBatchRetryResult serviceResp = TechnicianCommissionNotifyBatchRetryResult.builder()
                .totalCount(3)
                .retriedCount(1)
                .skippedNotExistsCount(1)
                .skippedStatusInvalidCount(1)
                .retriedIds(List.of(5001L))
                .skippedNotExistsIds(List.of(5002L))
                .skippedStatusInvalidIds(List.of(5003L))
                .build();
        when(settlementService.retryNotifyOutboxBatch(reqVO.getIds(), null, reqVO.getReason())).thenReturn(serviceResp);

        CommonResult<TechnicianCommissionSettlementNotifyOutboxBatchRetryRespVO> result =
                controller.batchRetryNotifyOutbox(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getTotalCount());
        assertEquals(1, result.getData().getRetriedCount());
        assertEquals(1, result.getData().getSkippedNotExistsCount());
        assertEquals(1, result.getData().getSkippedStatusInvalidCount());
        verify(settlementService).retryNotifyOutboxBatch(reqVO.getIds(), null, reqVO.getReason());
    }

    @Test
    void shouldGetSettlementPageAndExposeLastActionDimensions() {
        TechnicianCommissionSettlementDO settlement = new TechnicianCommissionSettlementDO();
        settlement.setId(1003L);
        settlement.setStatus(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewDeadlineTime(LocalDateTime.now().plusMinutes(15));
        settlement.setLastActionCode("SUBMIT_REVIEW");
        settlement.setLastActionBizNo("BIZ#SET1003");
        settlement.setLastActionTime(LocalDateTime.now().minusMinutes(2));

        com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementPageReqVO reqVO =
                new com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(settlementService.getSettlementPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(settlement), 1L));

        CommonResult<PageResult<TechnicianCommissionSettlementRespVO>> result = controller.page(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        TechnicianCommissionSettlementRespVO vo = result.getData().getList().get(0);
        assertEquals("SUBMIT_REVIEW", vo.getLastActionCode());
        assertEquals("BIZ#SET1003", vo.getLastActionBizNo());
        assertNotNull(vo.getLastActionTime());
        assertFalse(Boolean.TRUE.equals(vo.getOverdue()));
        verify(settlementService).getSettlementPage(reqVO);
    }
}
