package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementPageReqVO;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementNotifyOutboxPageReqVO;
import com.hxy.module.booking.service.dto.TechnicianCommissionNotifyBatchRetryResult;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementLogDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementNotifyOutboxDO;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionMapper;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionSettlementLogMapper;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionSettlementMapper;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionSettlementNotifyOutboxMapper;
import com.hxy.module.booking.enums.CommissionSettlementStatusEnum;
import com.hxy.module.booking.enums.CommissionStatusEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.hxy.module.booking.enums.ErrorCodeConstants.COMMISSION_SETTLEMENT_PAY_REMARK_REQUIRED;
import static com.hxy.module.booking.enums.ErrorCodeConstants.COMMISSION_SETTLEMENT_PAY_VOUCHER_REQUIRED;
import static com.hxy.module.booking.enums.ErrorCodeConstants.COMMISSION_SETTLEMENT_NOTIFY_OUTBOX_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.COMMISSION_SETTLEMENT_NOTIFY_OUTBOX_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicianCommissionSettlementServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TechnicianCommissionSettlementServiceImpl service;

    @Mock
    private TechnicianCommissionSettlementMapper settlementMapper;
    @Mock
    private TechnicianCommissionMapper commissionMapper;
    @Mock
    private TechnicianCommissionSettlementLogMapper settlementLogMapper;
    @Mock
    private TechnicianCommissionSettlementNotifyOutboxMapper notifyOutboxMapper;
    @Mock
    private NotifySendService notifySendService;
    @Mock
    private PermissionApi permissionApi;

    @Test
    void shouldCreateDraftSettlementAndBindCommissions() {
        List<Long> commissionIds = Arrays.asList(11L, 12L);
        when(commissionMapper.selectListByIds(commissionIds)).thenReturn(Arrays.asList(
                buildCommission(11L, 1001L, 2001L, 1000),
                buildCommission(12L, 1001L, 2001L, 1500)
        ));
        when(settlementMapper.insert(any(TechnicianCommissionSettlementDO.class))).thenAnswer(invocation -> {
            TechnicianCommissionSettlementDO settlement = invocation.getArgument(0);
            settlement.setId(9001L);
            return 1;
        });
        when(commissionMapper.bindSettlementByIds(commissionIds, 9001L, CommissionStatusEnum.PENDING.getStatus()))
                .thenReturn(2);

        Long id = service.createSettlement(commissionIds, "2月技师结算");

        assertEquals(9001L, id);
        ArgumentCaptor<TechnicianCommissionSettlementDO> captor = ArgumentCaptor.forClass(TechnicianCommissionSettlementDO.class);
        verify(settlementMapper).insert(captor.capture());
        assertEquals(CommissionSettlementStatusEnum.DRAFT.getStatus(), captor.getValue().getStatus());
        assertEquals(2, captor.getValue().getCommissionCount());
        assertEquals(2500, captor.getValue().getTotalCommissionAmount());
        verify(commissionMapper).bindSettlementByIds(commissionIds, 9001L,
                CommissionStatusEnum.PENDING.getStatus());
    }

    @Test
    void shouldCreateDraftSettlementWithReversalAndUseNetAmount() {
        List<Long> commissionIds = Arrays.asList(21L, 22L);
        TechnicianCommissionDO positive = buildCommission(21L, 1001L, 2001L, 1800);
        TechnicianCommissionDO reversal = buildCommission(22L, 1001L, 2001L, -500);
        when(commissionMapper.selectListByIds(commissionIds)).thenReturn(Arrays.asList(positive, reversal));
        when(settlementMapper.insert(any(TechnicianCommissionSettlementDO.class))).thenAnswer(invocation -> {
            TechnicianCommissionSettlementDO settlement = invocation.getArgument(0);
            settlement.setId(9002L);
            return 1;
        });
        when(commissionMapper.bindSettlementByIds(commissionIds, 9002L, CommissionStatusEnum.PENDING.getStatus()))
                .thenReturn(2);

        Long id = service.createSettlement(commissionIds, "冲正并表结算");

        assertEquals(9002L, id);
        ArgumentCaptor<TechnicianCommissionSettlementDO> captor = ArgumentCaptor.forClass(TechnicianCommissionSettlementDO.class);
        verify(settlementMapper).insert(captor.capture());
        assertEquals(2, captor.getValue().getCommissionCount());
        assertEquals(1300, captor.getValue().getTotalCommissionAmount());
        verify(commissionMapper).bindSettlementByIds(commissionIds, 9002L, CommissionStatusEnum.PENDING.getStatus());
    }

    @Test
    void shouldSubmitDraftSettlementWithDeadline() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(1L, CommissionSettlementStatusEnum.DRAFT.getStatus());
        when(settlementMapper.selectById(1L)).thenReturn(settlement);
        when(settlementMapper.updateByIdAndStatus(eq(1L), eq(CommissionSettlementStatusEnum.DRAFT.getStatus()),
                any(TechnicianCommissionSettlementDO.class))).thenReturn(1);

        service.submitForReview(1L, 120, "提审");

        ArgumentCaptor<TechnicianCommissionSettlementDO> captor = ArgumentCaptor.forClass(TechnicianCommissionSettlementDO.class);
        verify(settlementMapper).updateByIdAndStatus(eq(1L), eq(CommissionSettlementStatusEnum.DRAFT.getStatus()),
                captor.capture());
        assertEquals(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), captor.getValue().getStatus());
        assertNotNull(captor.getValue().getReviewDeadlineTime());
        verify(settlementLogMapper).insert(any(TechnicianCommissionSettlementLogDO.class));
    }

    @Test
    void shouldApprovePendingSettlement() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(2L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        when(settlementMapper.selectById(2L)).thenReturn(settlement);
        when(settlementMapper.updateByIdAndStatus(eq(2L), eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                any(TechnicianCommissionSettlementDO.class))).thenReturn(1);

        service.approve(2L, 99L, "审核通过");

        ArgumentCaptor<TechnicianCommissionSettlementDO> captor = ArgumentCaptor.forClass(TechnicianCommissionSettlementDO.class);
        verify(settlementMapper).updateByIdAndStatus(eq(2L), eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                captor.capture());
        assertEquals(CommissionSettlementStatusEnum.APPROVED.getStatus(), captor.getValue().getStatus());
        assertEquals(99L, captor.getValue().getReviewerId());
    }

    @Test
    void shouldRejectPendingSettlementAndReleaseBinding() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(3L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        when(settlementMapper.selectById(3L)).thenReturn(settlement);
        when(settlementMapper.updateByIdAndStatus(eq(3L), eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                any(TechnicianCommissionSettlementDO.class))).thenReturn(1);

        service.reject(3L, 88L, "资料不完整");

        verify(commissionMapper).clearSettlementBindingBySettlementId(3L, CommissionStatusEnum.PENDING.getStatus());
    }

    @Test
    void shouldPayApprovedSettlementAndSettleCommissions() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(4L, CommissionSettlementStatusEnum.APPROVED.getStatus());
        when(settlementMapper.selectById(4L)).thenReturn(settlement);
        when(settlementMapper.updateByIdAndStatus(eq(4L), eq(CommissionSettlementStatusEnum.APPROVED.getStatus()),
                any(TechnicianCommissionSettlementDO.class))).thenReturn(1);

        service.markPaid(4L, 77L, "TXN202603010001", "出纳打款");

        verify(commissionMapper).settleBySettlementId(eq(4L), eq(CommissionStatusEnum.PENDING.getStatus()), any(LocalDateTime.class));
    }

    @Test
    void shouldThrowWhenPayWithoutVoucherNo() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(41L, CommissionSettlementStatusEnum.APPROVED.getStatus());
        when(settlementMapper.selectById(41L)).thenReturn(settlement);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.markPaid(41L, 77L, " ", "出纳打款"));
        assertEquals(COMMISSION_SETTLEMENT_PAY_VOUCHER_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void shouldThrowWhenPayWithoutRemark() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(42L, CommissionSettlementStatusEnum.APPROVED.getStatus());
        when(settlementMapper.selectById(42L)).thenReturn(settlement);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.markPaid(42L, 77L, "TXN202603010002", " "));
        assertEquals(COMMISSION_SETTLEMENT_PAY_REMARK_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void shouldExpireOverduePendingSettlementAndReleaseBinding() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(5L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewDeadlineTime(LocalDateTime.now().minusMinutes(5));
        when(settlementMapper.selectListByStatusAndReviewDeadlineBefore(
                eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()), any(LocalDateTime.class), eq(200)))
                .thenReturn(Collections.singletonList(settlement));
        when(settlementMapper.updateByIdAndStatus(eq(5L), eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                any(TechnicianCommissionSettlementDO.class))).thenReturn(1);

        int count = service.expireOverduePending(200);

        assertEquals(1, count);
        ArgumentCaptor<TechnicianCommissionSettlementDO> settlementCaptor =
                ArgumentCaptor.forClass(TechnicianCommissionSettlementDO.class);
        verify(settlementMapper).updateByIdAndStatus(eq(5L), eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                settlementCaptor.capture());
        assertEquals(CommissionSettlementStatusEnum.VOIDED.getStatus(), settlementCaptor.getValue().getStatus());
        verify(commissionMapper).clearSettlementBindingBySettlementId(5L, CommissionStatusEnum.PENDING.getStatus());
    }

    @Test
    void shouldListSlaOverduePendingSettlement() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(51L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewDeadlineTime(LocalDateTime.now().minusMinutes(3));
        when(settlementMapper.selectListByStatusAndReviewDeadlineBefore(
                eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()), any(LocalDateTime.class), eq(50)))
                .thenReturn(Collections.singletonList(settlement));

        List<TechnicianCommissionSettlementDO> result = service.getSlaOverduePendingList(50);

        assertEquals(1, result.size());
        assertEquals(51L, result.get(0).getId());
    }

    @Test
    void shouldWarnNearDeadlinePendingSettlement() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(61L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewDeadlineTime(LocalDateTime.now().plusMinutes(10));
        settlement.setReviewWarned(Boolean.FALSE);
        when(settlementMapper.selectListByStatusAndReviewDeadlineBetweenAndWarned(
                eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                any(LocalDateTime.class), any(LocalDateTime.class), eq(Boolean.FALSE), eq(80)))
                .thenReturn(Collections.singletonList(settlement));
        when(settlementMapper.updateWarnedByIdAndStatusAndWarned(
                eq(61L), eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                eq(Boolean.FALSE), any(TechnicianCommissionSettlementDO.class)))
                .thenReturn(1);

        int count = service.warnNearDeadlinePending(20, 80);

        assertEquals(1, count);
        verify(settlementLogMapper).insert(any(TechnicianCommissionSettlementLogDO.class));
        verify(notifyOutboxMapper).insert(any(TechnicianCommissionSettlementNotifyOutboxDO.class));
    }

    @Test
    void shouldEscalateOverduePendingSettlementToP0() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(62L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewDeadlineTime(LocalDateTime.now().minusMinutes(31));
        settlement.setReviewEscalated(Boolean.FALSE);
        when(settlementMapper.selectListByStatusAndReviewDeadlineBeforeAndEscalated(
                eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                any(LocalDateTime.class), eq(Boolean.FALSE), eq(50)))
                .thenReturn(Collections.singletonList(settlement));
        when(settlementMapper.updateEscalatedByIdAndStatusAndEscalated(
                eq(62L), eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                eq(Boolean.FALSE), any(TechnicianCommissionSettlementDO.class)))
                .thenReturn(1);

        int count = service.escalateOverduePendingToP0(30, 50);

        assertEquals(1, count);
        ArgumentCaptor<TechnicianCommissionSettlementDO> captor =
                ArgumentCaptor.forClass(TechnicianCommissionSettlementDO.class);
        verify(settlementMapper).updateEscalatedByIdAndStatusAndEscalated(
                eq(62L), eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                eq(Boolean.FALSE), captor.capture());
        assertEquals(CommissionSettlementStatusEnum.ESCALATED.getStatus(), captor.getValue().getStatus());
        assertEquals("SLA_OVERDUE_ESCALATE_P0", captor.getValue().getReviewEscalateReason());
        assertNotNull(captor.getValue().getReviewEscalateTime());
        verify(settlementLogMapper).insert(any(TechnicianCommissionSettlementLogDO.class));
        verify(notifyOutboxMapper).insert(any(TechnicianCommissionSettlementNotifyOutboxDO.class));
    }

    @Test
    void shouldSkipEscalateWhenConcurrentStatusChanged() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(63L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewDeadlineTime(LocalDateTime.now().minusMinutes(40));
        settlement.setReviewEscalated(Boolean.FALSE);
        when(settlementMapper.selectListByStatusAndReviewDeadlineBeforeAndEscalated(
                eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                any(LocalDateTime.class), eq(Boolean.FALSE), eq(20)))
                .thenReturn(Collections.singletonList(settlement));
        when(settlementMapper.updateEscalatedByIdAndStatusAndEscalated(
                eq(63L), eq(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus()),
                eq(Boolean.FALSE), any(TechnicianCommissionSettlementDO.class)))
                .thenReturn(0);

        int count = service.escalateOverduePendingToP0(30, 20);

        assertEquals(0, count);
        verify(settlementLogMapper, never()).insert(any(TechnicianCommissionSettlementLogDO.class));
        verify(notifyOutboxMapper, never()).insert(any(TechnicianCommissionSettlementNotifyOutboxDO.class));
    }

    @Test
    void shouldDispatchPendingNotifyOutbox() {
        TechnicianCommissionSettlementNotifyOutboxDO outbox = new TechnicianCommissionSettlementNotifyOutboxDO();
        outbox.setId(91L);
        outbox.setSettlementId(62L);
        outbox.setNotifyType("P0_ESCALATE");
        outbox.setChannel("IN_APP");
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        TechnicianCommissionSettlementDO settlement = buildSettlement(62L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewerId(99L);
        settlement.setSettlementNo("SET202603010091");
        when(notifyOutboxMapper.selectDispatchableList(any(LocalDateTime.class), eq(20), eq(5)))
                .thenReturn(Collections.singletonList(outbox));
        when(settlementMapper.selectById(62L)).thenReturn(settlement);
        when(notifySendService.sendSingleNotifyToAdmin(eq(99L), eq("hxy_booking_commission_p0_escalate"), any()))
                .thenReturn(9001L);
        when(notifyOutboxMapper.updateByIdAndStatus(eq(91L), eq(0), any(TechnicianCommissionSettlementNotifyOutboxDO.class)))
                .thenReturn(1);

        int count = service.dispatchPendingNotifyOutbox(20);

        assertEquals(1, count);
        verify(notifySendService).sendSingleNotifyToAdmin(eq(99L), eq("hxy_booking_commission_p0_escalate"), any());
        verify(settlementLogMapper).insert(any(TechnicianCommissionSettlementLogDO.class));
        ArgumentCaptor<TechnicianCommissionSettlementNotifyOutboxDO> captor =
                ArgumentCaptor.forClass(TechnicianCommissionSettlementNotifyOutboxDO.class);
        verify(notifyOutboxMapper).updateByIdAndStatus(eq(91L), eq(0), captor.capture());
        TechnicianCommissionSettlementNotifyOutboxDO updateObj = captor.getValue();
        assertEquals("DISPATCH_SUCCESS", updateObj.getLastActionCode());
        assertNotNull(updateObj.getLastActionBizNo());
        assertNotNull(updateObj.getLastActionTime());
    }

    @Test
    void shouldDispatchPendingNotifyOutboxToRoleUserWhenReviewerMissing() {
        ReflectionTestUtils.setField(service, "notifyTargetRoleIdsConfig", "201,202");
        TechnicianCommissionSettlementNotifyOutboxDO outbox = new TechnicianCommissionSettlementNotifyOutboxDO();
        outbox.setId(94L);
        outbox.setSettlementId(65L);
        outbox.setNotifyType("P1_WARN");
        outbox.setChannel("IN_APP");
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        TechnicianCommissionSettlementDO settlement = buildSettlement(65L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewerId(null);
        settlement.setCreator("ops");
        when(notifyOutboxMapper.selectDispatchableList(any(LocalDateTime.class), eq(20), eq(5)))
                .thenReturn(Collections.singletonList(outbox));
        when(settlementMapper.selectById(65L)).thenReturn(settlement);
        Set<Long> roleUserIds = new LinkedHashSet<>(Arrays.asList(7002L, 7001L));
        when(permissionApi.getUserRoleIdListByRoleIds(eq(Arrays.asList(201L, 202L))))
                .thenReturn(roleUserIds);
        when(notifySendService.sendSingleNotifyToAdmin(eq(7001L), eq("hxy_booking_commission_p1_warn"), any()))
                .thenReturn(9101L);
        when(notifySendService.sendSingleNotifyToAdmin(eq(7002L), eq("hxy_booking_commission_p1_warn"), any()))
                .thenReturn(9102L);
        when(notifyOutboxMapper.updateByIdAndStatus(eq(94L), eq(0), any(TechnicianCommissionSettlementNotifyOutboxDO.class)))
                .thenReturn(1);

        int count = service.dispatchPendingNotifyOutbox(20);

        assertEquals(1, count);
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notifySendService, times(2))
                .sendSingleNotifyToAdmin(userIdCaptor.capture(), eq("hxy_booking_commission_p1_warn"), any());
        assertIterableEquals(Arrays.asList(7001L, 7002L), new ArrayList<>(userIdCaptor.getAllValues()));
        verify(settlementLogMapper).insert(any(TechnicianCommissionSettlementLogDO.class));
    }

    @Test
    void shouldRetryNotifyOutboxWhenDispatchFailAndBelowMax() {
        TechnicianCommissionSettlementNotifyOutboxDO outbox = new TechnicianCommissionSettlementNotifyOutboxDO();
        outbox.setId(92L);
        outbox.setSettlementId(63L);
        outbox.setNotifyType("P1_WARN");
        outbox.setChannel("IN_APP");
        outbox.setStatus(0);
        outbox.setRetryCount(1);
        TechnicianCommissionSettlementDO settlement = buildSettlement(63L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewerId(100L);
        when(notifyOutboxMapper.selectDispatchableList(any(LocalDateTime.class), eq(20), eq(5)))
                .thenReturn(Collections.singletonList(outbox));
        when(settlementMapper.selectById(63L)).thenReturn(settlement);
        when(notifySendService.sendSingleNotifyToAdmin(eq(100L), eq("hxy_booking_commission_p1_warn"), any()))
                .thenThrow(new RuntimeException("mock-send-failed"));
        when(notifyOutboxMapper.updateByIdAndStatus(eq(92L), eq(0), any(TechnicianCommissionSettlementNotifyOutboxDO.class)))
                .thenReturn(1);

        int count = service.dispatchPendingNotifyOutbox(20);

        assertEquals(0, count);
        ArgumentCaptor<TechnicianCommissionSettlementNotifyOutboxDO> captor =
                ArgumentCaptor.forClass(TechnicianCommissionSettlementNotifyOutboxDO.class);
        verify(notifyOutboxMapper)
                .updateByIdAndStatus(eq(92L), eq(0), captor.capture());
        TechnicianCommissionSettlementNotifyOutboxDO retryUpdate = captor.getValue();
        assertEquals(0, retryUpdate.getStatus());
        assertEquals(2, retryUpdate.getRetryCount());
        assertNotNull(retryUpdate.getNextRetryTime());
        assertEquals("DISPATCH_FAILED", retryUpdate.getLastActionCode());
        assertNotNull(retryUpdate.getLastActionBizNo());
        assertNotNull(retryUpdate.getLastActionTime());
    }

    @Test
    void shouldMarkNotifyOutboxFailedWhenRetryReachMax() {
        TechnicianCommissionSettlementNotifyOutboxDO outbox = new TechnicianCommissionSettlementNotifyOutboxDO();
        outbox.setId(93L);
        outbox.setSettlementId(64L);
        outbox.setNotifyType("P0_ESCALATE");
        outbox.setChannel("IN_APP");
        outbox.setStatus(2);
        outbox.setRetryCount(4);
        TechnicianCommissionSettlementDO settlement = buildSettlement(64L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        settlement.setReviewerId(101L);
        when(notifyOutboxMapper.selectDispatchableList(any(LocalDateTime.class), eq(20), eq(5)))
                .thenReturn(Collections.singletonList(outbox));
        when(settlementMapper.selectById(64L)).thenReturn(settlement);
        when(notifySendService.sendSingleNotifyToAdmin(eq(101L), eq("hxy_booking_commission_p0_escalate"), any()))
                .thenThrow(new RuntimeException("mock-send-failed"));
        when(notifyOutboxMapper.updateByIdAndStatus(eq(93L), eq(2), any(TechnicianCommissionSettlementNotifyOutboxDO.class)))
                .thenReturn(1);

        int count = service.dispatchPendingNotifyOutbox(20);

        assertEquals(0, count);
        ArgumentCaptor<TechnicianCommissionSettlementNotifyOutboxDO> captor =
                ArgumentCaptor.forClass(TechnicianCommissionSettlementNotifyOutboxDO.class);
        verify(notifyOutboxMapper)
                .updateByIdAndStatus(eq(93L), eq(2), captor.capture());
        TechnicianCommissionSettlementNotifyOutboxDO failedUpdate = captor.getValue();
        assertEquals(2, failedUpdate.getStatus());
        assertEquals(5, failedUpdate.getRetryCount());
        assertNull(failedUpdate.getNextRetryTime());
        assertEquals("DISPATCH_FAILED", failedUpdate.getLastActionCode());
        assertNotNull(failedUpdate.getLastActionBizNo());
        assertNotNull(failedUpdate.getLastActionTime());
    }

    @Test
    void shouldGetNotifyOutboxList() {
        TechnicianCommissionSettlementNotifyOutboxDO outbox = new TechnicianCommissionSettlementNotifyOutboxDO();
        outbox.setId(101L);
        outbox.setSettlementId(66L);
        outbox.setStatus(0);
        outbox.setNotifyType("P1_WARN");
        when(notifyOutboxMapper.selectListBySettlementAndStatus(66L, 0, 50))
                .thenReturn(Collections.singletonList(outbox));

        List<TechnicianCommissionSettlementNotifyOutboxDO> result = service.getNotifyOutboxList(66L, 0, 50);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getId());
        verify(notifyOutboxMapper).selectListBySettlementAndStatus(66L, 0, 50);
    }

    @Test
    void shouldGetNotifyOutboxPage() {
        TechnicianCommissionSettlementNotifyOutboxDO outbox = new TechnicianCommissionSettlementNotifyOutboxDO();
        outbox.setId(102L);
        outbox.setSettlementId(67L);
        outbox.setStatus(2);
        TechnicianCommissionSettlementNotifyOutboxPageReqVO reqVO = new TechnicianCommissionSettlementNotifyOutboxPageReqVO();
        reqVO.setSettlementId(67L);
        reqVO.setStatus(2);
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        when(notifyOutboxMapper.selectPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(outbox), 1L));

        PageResult<TechnicianCommissionSettlementNotifyOutboxDO> result = service.getNotifyOutboxPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(102L, result.getList().get(0).getId());
        verify(notifyOutboxMapper).selectPage(reqVO);
    }

    @Test
    void shouldGetSettlementPage() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(103L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        TechnicianCommissionSettlementPageReqVO reqVO = new TechnicianCommissionSettlementPageReqVO();
        reqVO.setSettlementNo("SET20260302");
        reqVO.setStatus(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(settlementMapper.selectPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(settlement), 1L));

        PageResult<TechnicianCommissionSettlementDO> result = service.getSettlementPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(103L, result.getList().get(0).getId());
        verify(settlementMapper).selectPage(reqVO);
    }

    @Test
    void shouldAttachLastActionDimensionsWhenGetSettlementPage() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(104L, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        TechnicianCommissionSettlementPageReqVO reqVO = new TechnicianCommissionSettlementPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(settlementMapper.selectPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(settlement), 1L));
        TechnicianCommissionSettlementLogDO latestLog = new TechnicianCommissionSettlementLogDO();
        latestLog.setSettlementId(104L);
        latestLog.setAction("SUBMIT_REVIEW");
        latestLog.setOperateRemark("BIZ#SET104");
        latestLog.setActionTime(LocalDateTime.now());
        when(settlementLogMapper.selectLatestListBySettlementIds(List.of(104L)))
                .thenReturn(Collections.singletonList(latestLog));

        PageResult<TechnicianCommissionSettlementDO> result = service.getSettlementPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("SUBMIT_REVIEW", result.getList().get(0).getLastActionCode());
        assertEquals("BIZ#SET104", result.getList().get(0).getLastActionBizNo());
        assertNotNull(result.getList().get(0).getLastActionTime());
    }

    @Test
    void shouldRetryFailedNotifyOutbox() {
        TechnicianCommissionSettlementNotifyOutboxDO outbox = new TechnicianCommissionSettlementNotifyOutboxDO();
        outbox.setId(201L);
        outbox.setSettlementId(68L);
        outbox.setStatus(2);
        outbox.setRetryCount(5);
        when(notifyOutboxMapper.selectById(201L)).thenReturn(outbox);
        when(notifyOutboxMapper.updateByIdAndStatus(eq(201L), eq(2), any(TechnicianCommissionSettlementNotifyOutboxDO.class)))
                .thenReturn(1);

        int count = service.retryNotifyOutbox(Collections.singletonList(201L), 9001L, "manual-retry");

        assertEquals(1, count);
        ArgumentCaptor<TechnicianCommissionSettlementNotifyOutboxDO> captor =
                ArgumentCaptor.forClass(TechnicianCommissionSettlementNotifyOutboxDO.class);
        verify(notifyOutboxMapper).updateByIdAndStatus(eq(201L), eq(2), captor.capture());
        TechnicianCommissionSettlementNotifyOutboxDO updateObj = captor.getValue();
        assertEquals(0, updateObj.getStatus());
        assertNotNull(updateObj.getNextRetryTime());
        assertEquals("MANUAL_RETRY", updateObj.getLastActionCode());
        assertNotNull(updateObj.getLastActionBizNo());
        assertNotNull(updateObj.getLastActionTime());
        verify(settlementLogMapper).insert(any(TechnicianCommissionSettlementLogDO.class));
    }

    @Test
    void shouldBatchRetryNotifyOutboxWithSkipDetails() {
        TechnicianCommissionSettlementNotifyOutboxDO retryable = new TechnicianCommissionSettlementNotifyOutboxDO();
        retryable.setId(301L);
        retryable.setSettlementId(88L);
        retryable.setStatus(2);
        retryable.setRetryCount(2);
        TechnicianCommissionSettlementNotifyOutboxDO sent = new TechnicianCommissionSettlementNotifyOutboxDO();
        sent.setId(302L);
        sent.setSettlementId(88L);
        sent.setStatus(1);

        when(notifyOutboxMapper.selectById(301L)).thenReturn(retryable);
        when(notifyOutboxMapper.selectById(302L)).thenReturn(sent);
        when(notifyOutboxMapper.selectById(303L)).thenReturn(null);
        when(notifyOutboxMapper.updateByIdAndStatus(eq(301L), eq(2), any(TechnicianCommissionSettlementNotifyOutboxDO.class)))
                .thenReturn(1);

        TechnicianCommissionNotifyBatchRetryResult result =
                service.retryNotifyOutboxBatch(Arrays.asList(301L, 302L, 303L), 9002L, "ops-batch-retry");

        assertEquals(3, result.getTotalCount());
        assertEquals(1, result.getRetriedCount());
        assertEquals(1, result.getSkippedNotExistsCount());
        assertEquals(1, result.getSkippedStatusInvalidCount());
        assertIterableEquals(List.of(301L), result.getRetriedIds());
        assertTrue(result.getSkippedNotExistsIds().contains(303L));
        assertTrue(result.getSkippedStatusInvalidIds().contains(302L));
        verify(settlementLogMapper).insert(any(TechnicianCommissionSettlementLogDO.class));
    }

    @Test
    void shouldThrowWhenRetryNotifyOutboxNotExists() {
        when(notifyOutboxMapper.selectById(202L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.retryNotifyOutbox(Collections.singletonList(202L), "manual-retry"));
        assertEquals(COMMISSION_SETTLEMENT_NOTIFY_OUTBOX_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void shouldThrowWhenRetryNotifyOutboxStatusInvalid() {
        TechnicianCommissionSettlementNotifyOutboxDO outbox = new TechnicianCommissionSettlementNotifyOutboxDO();
        outbox.setId(203L);
        outbox.setSettlementId(69L);
        outbox.setStatus(1);
        when(notifyOutboxMapper.selectById(203L)).thenReturn(outbox);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.retryNotifyOutbox(Collections.singletonList(203L), "manual-retry"));
        assertEquals(COMMISSION_SETTLEMENT_NOTIFY_OUTBOX_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void shouldThrowWhenApproveFromIllegalStatus() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(6L, CommissionSettlementStatusEnum.DRAFT.getStatus());
        when(settlementMapper.selectById(6L)).thenReturn(settlement);

        assertThrows(ServiceException.class, () -> service.approve(6L, 1L, "非法审批"));
    }

    @Test
    void shouldThrowWhenSubmitFromIllegalStatus() {
        TechnicianCommissionSettlementDO settlement = buildSettlement(7L, CommissionSettlementStatusEnum.REJECTED.getStatus());
        when(settlementMapper.selectById(7L)).thenReturn(settlement);

        assertThrows(ServiceException.class, () -> service.submitForReview(7L, 60, "重复提审"));
    }

    private static TechnicianCommissionDO buildCommission(Long id, Long technicianId, Long storeId, Integer amount) {
        return TechnicianCommissionDO.builder()
                .id(id)
                .technicianId(technicianId)
                .orderId(id + 1000)
                .userId(3001L)
                .commissionType(1)
                .baseAmount(amount * 2)
                .commissionRate(new BigDecimal("0.5"))
                .commissionAmount(amount)
                .status(CommissionStatusEnum.PENDING.getStatus())
                .settlementId(null)
                .build();
    }

    private static TechnicianCommissionSettlementDO buildSettlement(Long id, Integer status) {
        TechnicianCommissionSettlementDO settlement = new TechnicianCommissionSettlementDO();
        settlement.setId(id);
        settlement.setStatus(status);
        settlement.setStoreId(2001L);
        settlement.setTechnicianId(1001L);
        settlement.setCommissionCount(2);
        settlement.setTotalCommissionAmount(2500);
        return settlement;
    }
}
