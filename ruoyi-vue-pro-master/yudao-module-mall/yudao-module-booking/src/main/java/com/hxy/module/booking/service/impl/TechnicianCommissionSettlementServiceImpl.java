package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementPageReqVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementNotifyOutboxPageReqVO;
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
import com.hxy.module.booking.service.TechnicianCommissionSettlementService;
import com.hxy.module.booking.service.dto.TechnicianCommissionNotifyBatchRetryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class TechnicianCommissionSettlementServiceImpl implements TechnicianCommissionSettlementService {

    private static final int DEFAULT_SLA_MINUTES = 120;
    private static final int DEFAULT_WARN_LEAD_MINUTES = 30;
    private static final int DEFAULT_ESCALATE_DELAY_MINUTES = 30;

    private static final String LOG_ACTION_CREATE = "CREATE";
    private static final String LOG_ACTION_SUBMIT_REVIEW = "SUBMIT_REVIEW";
    private static final String LOG_ACTION_APPROVE = "APPROVE";
    private static final String LOG_ACTION_REJECT = "REJECT";
    private static final String LOG_ACTION_PAY = "PAY";
    private static final String LOG_ACTION_EXPIRE = "EXPIRE";
    private static final String LOG_ACTION_SLA_WARN = "SLA_WARN_P1";
    private static final String LOG_ACTION_SLA_ESCALATE = "SLA_ESCALATE_P0";
    private static final String LOG_ACTION_NOTIFY_SENT = "NOTIFY_SENT";
    private static final String LOG_ACTION_NOTIFY_RETRY = "NOTIFY_RETRY";

    private static final String OPERATOR_TYPE_SYSTEM = "SYSTEM";
    private static final String OPERATOR_TYPE_ADMIN = "ADMIN";

    private static final String NOTIFY_TYPE_P1_WARN = "P1_WARN";
    private static final String NOTIFY_TYPE_P0_ESCALATE = "P0_ESCALATE";
    private static final String NOTIFY_CHANNEL_IN_APP = "IN_APP";
    private static final int NOTIFY_STATUS_PENDING = 0;
    private static final int NOTIFY_STATUS_SENT = 1;
    private static final int NOTIFY_STATUS_FAILED = 2;
    private static final String NOTIFY_ACTION_CREATE = "CREATE";
    private static final String NOTIFY_ACTION_DISPATCH_SUCCESS = "DISPATCH_SUCCESS";
    private static final String NOTIFY_ACTION_DISPATCH_FAILED = "DISPATCH_FAILED";
    private static final String NOTIFY_ACTION_MANUAL_RETRY = "MANUAL_RETRY";
    private static final int NOTIFY_MAX_RETRY_COUNT = 5;
    private static final int NOTIFY_BACKOFF_BASE_MINUTES = 5;
    private static final long DEFAULT_NOTIFY_ADMIN_USER_ID = 1L;
    private static final String NOTIFY_TEMPLATE_CODE_P1_WARN = "hxy_booking_commission_p1_warn";
    private static final String NOTIFY_TEMPLATE_CODE_P0_ESCALATE = "hxy_booking_commission_p0_escalate";

    @Resource
    private TechnicianCommissionSettlementMapper settlementMapper;
    @Resource
    private TechnicianCommissionMapper commissionMapper;
    @Resource
    private TechnicianCommissionSettlementLogMapper settlementLogMapper;
    @Resource
    private TechnicianCommissionSettlementNotifyOutboxMapper notifyOutboxMapper;
    @Resource
    private NotifySendService notifySendService;
    @Resource
    private PermissionApi permissionApi;

    @Value("${hxy.booking.commission.notify-target-role-ids:}")
    private String notifyTargetRoleIdsConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSettlement(List<Long> commissionIds, String remark) {
        List<Long> normalizedIds = normalizeIds(commissionIds);
        if (normalizedIds.isEmpty()) {
            throw exception(COMMISSION_SETTLEMENT_COMMISSION_EMPTY);
        }

        List<TechnicianCommissionDO> commissions = commissionMapper.selectListByIds(normalizedIds);
        if (commissions.size() != normalizedIds.size()) {
            throw exception(COMMISSION_SETTLEMENT_COMMISSION_INVALID);
        }
        validateCommissionScope(commissions);

        int totalAmount = commissions.stream().mapToInt(c -> ObjUtil.defaultIfNull(c.getCommissionAmount(), 0)).sum();
        TechnicianCommissionDO first = commissions.get(0);
        TechnicianCommissionSettlementDO settlement = TechnicianCommissionSettlementDO.builder()
                .settlementNo(generateSettlementNo())
                .storeId(first.getStoreId())
                .technicianId(first.getTechnicianId())
                .status(CommissionSettlementStatusEnum.DRAFT.getStatus())
                .commissionCount(commissions.size())
                .totalCommissionAmount(totalAmount)
                .remark(StrUtil.blankToDefault(remark, ""))
                .build();
        settlementMapper.insert(settlement);

        int bindRows = commissionMapper.bindSettlementByIds(normalizedIds, settlement.getId(),
                CommissionStatusEnum.PENDING.getStatus());
        if (bindRows != normalizedIds.size()) {
            throw exception(COMMISSION_SETTLEMENT_COMMISSION_INVALID);
        }
        appendLog(settlement.getId(), LOG_ACTION_CREATE, null,
                CommissionSettlementStatusEnum.DRAFT.getStatus(), null, OPERATOR_TYPE_SYSTEM, remark);
        return settlement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForReview(Long settlementId, Integer slaMinutes, String submitRemark) {
        TechnicianCommissionSettlementDO settlement = getRequiredSettlement(settlementId);
        ensureStatus(settlement, CommissionSettlementStatusEnum.DRAFT);

        LocalDateTime now = LocalDateTime.now();
        int safeSlaMinutes = resolveSlaMinutes(slaMinutes);
        int updated = settlementMapper.updateByIdAndStatus(settlementId, CommissionSettlementStatusEnum.DRAFT.getStatus(),
                new TechnicianCommissionSettlementDO()
                        .setStatus(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus())
                        .setReviewSubmitTime(now)
                        .setReviewDeadlineTime(now.plusMinutes(safeSlaMinutes))
                        .setReviewWarned(Boolean.FALSE)
                        .setReviewWarnTime(null)
                        .setReviewEscalated(Boolean.FALSE)
                        .setReviewEscalateTime(null)
                        .setReviewRemark(StrUtil.maxLength(StrUtil.blankToDefault(submitRemark, ""), 255)));
        if (updated == 0) {
            throw exception(COMMISSION_SETTLEMENT_STATUS_INVALID, settlement.getStatus(), CommissionSettlementStatusEnum.DRAFT.getStatus());
        }
        appendLog(settlementId, LOG_ACTION_SUBMIT_REVIEW, CommissionSettlementStatusEnum.DRAFT.getStatus(),
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), null, OPERATOR_TYPE_SYSTEM, submitRemark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long settlementId, Long reviewerId, String reviewRemark) {
        TechnicianCommissionSettlementDO settlement = getRequiredSettlement(settlementId);
        ensureStatus(settlement, CommissionSettlementStatusEnum.PENDING_REVIEW);

        int updated = settlementMapper.updateByIdAndStatus(settlementId,
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                new TechnicianCommissionSettlementDO()
                        .setStatus(CommissionSettlementStatusEnum.APPROVED.getStatus())
                        .setReviewedTime(LocalDateTime.now())
                        .setReviewerId(reviewerId)
                        .setReviewRemark(StrUtil.maxLength(StrUtil.blankToDefault(reviewRemark, ""), 255))
                        .setRejectReason(""));
        if (updated == 0) {
            throw exception(COMMISSION_SETTLEMENT_STATUS_INVALID, settlement.getStatus(),
                    CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        }
        appendLog(settlementId, LOG_ACTION_APPROVE, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                CommissionSettlementStatusEnum.APPROVED.getStatus(), reviewerId, OPERATOR_TYPE_ADMIN, reviewRemark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long settlementId, Long reviewerId, String rejectReason) {
        TechnicianCommissionSettlementDO settlement = getRequiredSettlement(settlementId);
        ensureStatus(settlement, CommissionSettlementStatusEnum.PENDING_REVIEW);

        int updated = settlementMapper.updateByIdAndStatus(settlementId,
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                new TechnicianCommissionSettlementDO()
                        .setStatus(CommissionSettlementStatusEnum.REJECTED.getStatus())
                        .setReviewedTime(LocalDateTime.now())
                        .setReviewerId(reviewerId)
                        .setRejectReason(StrUtil.maxLength(StrUtil.blankToDefault(rejectReason, ""), 255)));
        if (updated == 0) {
            throw exception(COMMISSION_SETTLEMENT_STATUS_INVALID, settlement.getStatus(),
                    CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        }
        commissionMapper.clearSettlementBindingBySettlementId(settlementId, CommissionStatusEnum.PENDING.getStatus());
        appendLog(settlementId, LOG_ACTION_REJECT, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                CommissionSettlementStatusEnum.REJECTED.getStatus(), reviewerId, OPERATOR_TYPE_ADMIN, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long settlementId, Long payerId, String payVoucherNo, String payRemark) {
        TechnicianCommissionSettlementDO settlement = getRequiredSettlement(settlementId);
        ensureStatus(settlement, CommissionSettlementStatusEnum.APPROVED);
        validatePayEvidence(payVoucherNo, payRemark);

        LocalDateTime now = LocalDateTime.now();
        int updated = settlementMapper.updateByIdAndStatus(settlementId,
                CommissionSettlementStatusEnum.APPROVED.getStatus(),
                new TechnicianCommissionSettlementDO()
                        .setStatus(CommissionSettlementStatusEnum.PAID.getStatus())
                        .setPaidTime(now)
                        .setPayerId(payerId)
                        .setPayVoucherNo(StrUtil.maxLength(payVoucherNo.trim(), 64))
                        .setPayRemark(StrUtil.maxLength(StrUtil.blankToDefault(payRemark, ""), 255)));
        if (updated == 0) {
            throw exception(COMMISSION_SETTLEMENT_STATUS_INVALID, settlement.getStatus(),
                    CommissionSettlementStatusEnum.APPROVED.getStatus());
        }
        commissionMapper.settleBySettlementId(settlementId, CommissionStatusEnum.PENDING.getStatus(), now);
        appendLog(settlementId, LOG_ACTION_PAY, CommissionSettlementStatusEnum.APPROVED.getStatus(),
                CommissionSettlementStatusEnum.PAID.getStatus(), payerId, OPERATOR_TYPE_ADMIN, payRemark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireOverduePending(Integer limit) {
        LocalDateTime now = LocalDateTime.now();
        List<TechnicianCommissionSettlementDO> overdueList = settlementMapper.selectListByStatusAndReviewDeadlineBefore(
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), now, limit);
        if (overdueList.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (TechnicianCommissionSettlementDO settlement : overdueList) {
            int updated = settlementMapper.updateByIdAndStatus(settlement.getId(),
                    CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                    new TechnicianCommissionSettlementDO()
                            .setStatus(CommissionSettlementStatusEnum.VOIDED.getStatus())
                            .setRejectReason("SLA_EXPIRED"));
            if (updated > 0) {
                commissionMapper.clearSettlementBindingBySettlementId(settlement.getId(), CommissionStatusEnum.PENDING.getStatus());
                appendLog(settlement.getId(), LOG_ACTION_EXPIRE, CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                        CommissionSettlementStatusEnum.VOIDED.getStatus(), null, OPERATOR_TYPE_SYSTEM, "SLA_EXPIRED");
                affected++;
            }
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int warnNearDeadlinePending(Integer leadMinutes, Integer limit) {
        LocalDateTime now = LocalDateTime.now();
        int safeLeadMinutes = resolveWarnLeadMinutes(leadMinutes);
        List<TechnicianCommissionSettlementDO> nearDeadlineList =
                settlementMapper.selectListByStatusAndReviewDeadlineBetweenAndWarned(
                        CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                        now, now.plusMinutes(safeLeadMinutes), Boolean.FALSE, limit);
        if (nearDeadlineList.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (TechnicianCommissionSettlementDO settlement : nearDeadlineList) {
            int updated = settlementMapper.updateWarnedByIdAndStatusAndWarned(
                    settlement.getId(), CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), Boolean.FALSE,
                    new TechnicianCommissionSettlementDO()
                            .setReviewWarned(Boolean.TRUE)
                            .setReviewWarnTime(now));
            if (updated > 0) {
                appendLog(settlement.getId(), LOG_ACTION_SLA_WARN,
                        CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                        CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                        null, OPERATOR_TYPE_SYSTEM, "SLA_NEAR_DEADLINE");
                createNotifyOutboxIfAbsent(settlement.getId(), NOTIFY_TYPE_P1_WARN, "P1");
                affected++;
            }
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int escalateOverduePendingToP0(Integer delayMinutes, Integer limit) {
        LocalDateTime now = LocalDateTime.now();
        int safeDelayMinutes = resolveEscalateDelayMinutes(delayMinutes);
        LocalDateTime cutoff = now.minusMinutes(safeDelayMinutes);
        List<TechnicianCommissionSettlementDO> overdueList =
                settlementMapper.selectListByStatusAndReviewDeadlineBeforeAndEscalated(
                        CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), cutoff, Boolean.FALSE, limit);
        if (overdueList.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (TechnicianCommissionSettlementDO settlement : overdueList) {
            int updated = settlementMapper.updateEscalatedByIdAndStatusAndEscalated(
                    settlement.getId(), CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), Boolean.FALSE,
                    new TechnicianCommissionSettlementDO()
                            .setReviewEscalated(Boolean.TRUE)
                            .setReviewEscalateTime(now));
            if (updated > 0) {
                appendLog(settlement.getId(), LOG_ACTION_SLA_ESCALATE,
                        CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                        CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                        null, OPERATOR_TYPE_SYSTEM, "SLA_OVERDUE_ESCALATE_P0");
                createNotifyOutboxIfAbsent(settlement.getId(), NOTIFY_TYPE_P0_ESCALATE, "P0");
                affected++;
            }
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int dispatchPendingNotifyOutbox(Integer limit) {
        List<TechnicianCommissionSettlementNotifyOutboxDO> pendingList = notifyOutboxMapper
                .selectDispatchableList(LocalDateTime.now(), limit, NOTIFY_MAX_RETRY_COUNT);
        if (pendingList.isEmpty()) {
            return 0;
        }
        int affected = 0;
        LocalDateTime now = LocalDateTime.now();
        for (TechnicianCommissionSettlementNotifyOutboxDO outbox : pendingList) {
            Integer currentStatus = ObjUtil.defaultIfNull(outbox.getStatus(), NOTIFY_STATUS_PENDING);
            try {
                dispatchNotify(outbox);
                int updated = notifyOutboxMapper.updateByIdAndStatus(outbox.getId(), currentStatus,
                        new TechnicianCommissionSettlementNotifyOutboxDO()
                                .setStatus(NOTIFY_STATUS_SENT)
                                .setRetryCount(ObjUtil.defaultIfNull(outbox.getRetryCount(), 0))
                                .setNextRetryTime(null)
                                .setSentTime(now)
                                .setLastErrorMsg("")
                                .setLastActionCode(NOTIFY_ACTION_DISPATCH_SUCCESS)
                                .setLastActionBizNo(buildDispatchAuditBizNo(outbox.getId(), now))
                                .setLastActionTime(now));
                if (updated > 0) {
                    appendLog(outbox.getSettlementId(), LOG_ACTION_NOTIFY_SENT,
                            CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                            CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(),
                            null, OPERATOR_TYPE_SYSTEM, outbox.getNotifyType() + ":" + outbox.getChannel());
                    affected++;
                }
            } catch (Exception ex) {
                int nextRetryCount = ObjUtil.defaultIfNull(outbox.getRetryCount(), 0) + 1;
                boolean reachRetryLimit = nextRetryCount >= NOTIFY_MAX_RETRY_COUNT;
                notifyOutboxMapper.updateByIdAndStatus(outbox.getId(), currentStatus,
                        new TechnicianCommissionSettlementNotifyOutboxDO()
                                .setStatus(reachRetryLimit ? NOTIFY_STATUS_FAILED : NOTIFY_STATUS_PENDING)
                                .setRetryCount(nextRetryCount)
                                .setNextRetryTime(reachRetryLimit ? null : calculateNextRetryTime(now, nextRetryCount))
                                .setLastErrorMsg(StrUtil.maxLength(StrUtil.blankToDefault(ex.getMessage(),
                                        ex.getClass().getSimpleName()), 255))
                                .setLastActionCode(NOTIFY_ACTION_DISPATCH_FAILED)
                                .setLastActionBizNo(buildDispatchAuditBizNo(outbox.getId(), now))
                                .setLastActionTime(now));
            }
        }
        return affected;
    }

    @Override
    public TechnicianCommissionSettlementDO getSettlement(Long id) {
        return settlementMapper.selectById(id);
    }

    @Override
    public PageResult<TechnicianCommissionSettlementDO> getSettlementPage(TechnicianCommissionSettlementPageReqVO pageReqVO) {
        return settlementMapper.selectPage(pageReqVO);
    }

    @Override
    public List<TechnicianCommissionSettlementDO> getSettlementList(Long technicianId, Integer status) {
        return settlementMapper.selectListByTechnicianAndStatus(technicianId, status);
    }

    @Override
    public List<TechnicianCommissionSettlementDO> getSlaOverduePendingList(Integer limit) {
        return settlementMapper.selectListByStatusAndReviewDeadlineBefore(
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), LocalDateTime.now(), limit);
    }

    @Override
    public List<TechnicianCommissionSettlementLogDO> getOperationLogList(Long settlementId) {
        if (settlementId == null) {
            return Collections.emptyList();
        }
        return settlementLogMapper.selectListBySettlementId(settlementId);
    }

    @Override
    public List<TechnicianCommissionSettlementNotifyOutboxDO> getNotifyOutboxList(Long settlementId, Integer status, Integer limit) {
        return notifyOutboxMapper.selectListBySettlementAndStatus(settlementId, status, limit);
    }

    @Override
    public PageResult<TechnicianCommissionSettlementNotifyOutboxDO> getNotifyOutboxPage(
            TechnicianCommissionSettlementNotifyOutboxPageReqVO pageReqVO) {
        return notifyOutboxMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int retryNotifyOutbox(List<Long> ids, Long operatorId, String reason) {
        List<Long> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return 0;
        }
        int affected = 0;
        LocalDateTime now = LocalDateTime.now();
        String retryReason = StrUtil.maxLength(StrUtil.blankToDefault(reason, "manual-retry"), 120);
        for (Long id : normalizedIds) {
            TechnicianCommissionSettlementNotifyOutboxDO outbox = notifyOutboxMapper.selectById(id);
            if (outbox == null) {
                throw exception(COMMISSION_SETTLEMENT_NOTIFY_OUTBOX_NOT_EXISTS);
            }
            Integer currentStatus = ObjUtil.defaultIfNull(outbox.getStatus(), NOTIFY_STATUS_PENDING);
            if (!isRetryableNotifyStatus(currentStatus)) {
                throw exception(COMMISSION_SETTLEMENT_NOTIFY_OUTBOX_STATUS_INVALID, currentStatus);
            }
            int updated = notifyOutboxMapper.updateByIdAndStatus(id, currentStatus,
                    new TechnicianCommissionSettlementNotifyOutboxDO()
                            .setStatus(NOTIFY_STATUS_PENDING)
                            .setNextRetryTime(now)
                            .setSentTime(null)
                            .setLastErrorMsg("manual-retry:" + retryReason)
                            .setLastActionCode(NOTIFY_ACTION_MANUAL_RETRY)
                            .setLastActionBizNo(buildManualRetryAuditBizNo(operatorId, id))
                            .setLastActionTime(now));
            if (updated == 0) {
                throw exception(COMMISSION_SETTLEMENT_NOTIFY_OUTBOX_STATUS_INVALID, currentStatus);
            }
            appendLog(outbox.getSettlementId(), LOG_ACTION_NOTIFY_RETRY, null, null, operatorId,
                    resolveOperatorType(operatorId), "outbox#" + id + ":" + retryReason);
            affected += updated;
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechnicianCommissionNotifyBatchRetryResult retryNotifyOutboxBatch(List<Long> ids, Long operatorId, String reason) {
        List<Long> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return TechnicianCommissionNotifyBatchRetryResult.empty();
        }
        List<Long> retriedIds = new ArrayList<>();
        List<Long> skippedNotExistsIds = new ArrayList<>();
        List<Long> skippedStatusInvalidIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String retryReason = StrUtil.maxLength(StrUtil.blankToDefault(reason, "manual-retry"), 120);
        for (Long id : normalizedIds) {
            TechnicianCommissionSettlementNotifyOutboxDO outbox = notifyOutboxMapper.selectById(id);
            if (outbox == null) {
                skippedNotExistsIds.add(id);
                continue;
            }
            Integer currentStatus = ObjUtil.defaultIfNull(outbox.getStatus(), NOTIFY_STATUS_PENDING);
            if (!isRetryableNotifyStatus(currentStatus)) {
                skippedStatusInvalidIds.add(id);
                continue;
            }
            int updated = notifyOutboxMapper.updateByIdAndStatus(id, currentStatus,
                    new TechnicianCommissionSettlementNotifyOutboxDO()
                            .setStatus(NOTIFY_STATUS_PENDING)
                            .setNextRetryTime(now)
                            .setSentTime(null)
                            .setLastErrorMsg("manual-retry:" + retryReason)
                            .setLastActionCode(NOTIFY_ACTION_MANUAL_RETRY)
                            .setLastActionBizNo(buildManualRetryAuditBizNo(operatorId, id))
                            .setLastActionTime(now));
            if (updated == 0) {
                skippedStatusInvalidIds.add(id);
                continue;
            }
            appendLog(outbox.getSettlementId(), LOG_ACTION_NOTIFY_RETRY, null, null, operatorId,
                    resolveOperatorType(operatorId), "outbox#" + id + ":" + retryReason);
            retriedIds.add(id);
        }
        return TechnicianCommissionNotifyBatchRetryResult.builder()
                .totalCount(normalizedIds.size())
                .retriedCount(retriedIds.size())
                .skippedNotExistsCount(skippedNotExistsIds.size())
                .skippedStatusInvalidCount(skippedStatusInvalidIds.size())
                .retriedIds(retriedIds)
                .skippedNotExistsIds(skippedNotExistsIds)
                .skippedStatusInvalidIds(skippedStatusInvalidIds)
                .build();
    }

    private TechnicianCommissionSettlementDO getRequiredSettlement(Long settlementId) {
        TechnicianCommissionSettlementDO settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw exception(COMMISSION_SETTLEMENT_NOT_EXISTS);
        }
        return settlement;
    }

    private void ensureStatus(TechnicianCommissionSettlementDO settlement, CommissionSettlementStatusEnum expectedStatus) {
        if (!Objects.equals(settlement.getStatus(), expectedStatus.getStatus())) {
            throw exception(COMMISSION_SETTLEMENT_STATUS_INVALID, settlement.getStatus(), expectedStatus.getStatus());
        }
    }

    private void validateCommissionScope(List<TechnicianCommissionDO> commissions) {
        if (commissions.isEmpty()) {
            throw exception(COMMISSION_SETTLEMENT_COMMISSION_EMPTY);
        }
        TechnicianCommissionDO first = commissions.get(0);
        for (TechnicianCommissionDO commission : commissions) {
            if (!Objects.equals(commission.getStatus(), CommissionStatusEnum.PENDING.getStatus())
                    || commission.getSettlementId() != null) {
                throw exception(COMMISSION_SETTLEMENT_COMMISSION_INVALID);
            }
            if (!Objects.equals(first.getTechnicianId(), commission.getTechnicianId())
                    || !Objects.equals(first.getStoreId(), commission.getStoreId())) {
                throw exception(COMMISSION_SETTLEMENT_COMMISSION_SCOPE_INVALID);
            }
        }
    }

    private int resolveSlaMinutes(Integer slaMinutes) {
        int value = ObjUtil.defaultIfNull(slaMinutes, DEFAULT_SLA_MINUTES);
        if (value <= 0) {
            return DEFAULT_SLA_MINUTES;
        }
        return Math.min(value, 10080);
    }

    private int resolveWarnLeadMinutes(Integer leadMinutes) {
        int value = ObjUtil.defaultIfNull(leadMinutes, DEFAULT_WARN_LEAD_MINUTES);
        if (value <= 0) {
            return DEFAULT_WARN_LEAD_MINUTES;
        }
        return Math.min(value, 1440);
    }

    private int resolveEscalateDelayMinutes(Integer delayMinutes) {
        int value = ObjUtil.defaultIfNull(delayMinutes, DEFAULT_ESCALATE_DELAY_MINUTES);
        if (value <= 0) {
            return DEFAULT_ESCALATE_DELAY_MINUTES;
        }
        return Math.min(value, 1440);
    }

    private void validatePayEvidence(String payVoucherNo, String payRemark) {
        if (StrUtil.isBlank(payVoucherNo)) {
            throw exception(COMMISSION_SETTLEMENT_PAY_VOUCHER_REQUIRED);
        }
        if (StrUtil.isBlank(payRemark)) {
            throw exception(COMMISSION_SETTLEMENT_PAY_REMARK_REQUIRED);
        }
    }

    private boolean isRetryableNotifyStatus(Integer status) {
        return Objects.equals(status, NOTIFY_STATUS_PENDING)
                || Objects.equals(status, NOTIFY_STATUS_FAILED);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> normalized = new ArrayList<>();
        for (Long id : ids) {
            if (id != null && !normalized.contains(id)) {
                normalized.add(id);
            }
        }
        return normalized;
    }

    private String generateSettlementNo() {
        return "SET" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    private void appendLog(Long settlementId, String action, Integer fromStatus, Integer toStatus,
                           Long operatorId, String operatorType, String remark) {
        settlementLogMapper.insert(new TechnicianCommissionSettlementLogDO()
                .setSettlementId(settlementId)
                .setAction(action)
                .setFromStatus(fromStatus)
                .setToStatus(toStatus)
                .setOperatorId(operatorId)
                .setOperatorType(StrUtil.blankToDefault(operatorType, OPERATOR_TYPE_SYSTEM))
                .setOperateRemark(StrUtil.maxLength(StrUtil.blankToDefault(remark, ""), 255))
                .setActionTime(LocalDateTime.now()));
    }

    private void createNotifyOutboxIfAbsent(Long settlementId, String notifyType, String severity) {
        String bizKey = settlementId + ":" + notifyType;
        if (notifyOutboxMapper.selectByBizKey(bizKey) != null) {
            return;
        }
        notifyOutboxMapper.insert(new TechnicianCommissionSettlementNotifyOutboxDO()
                .setSettlementId(settlementId)
                .setNotifyType(notifyType)
                .setChannel(NOTIFY_CHANNEL_IN_APP)
                .setSeverity(severity)
                .setBizKey(bizKey)
                .setStatus(NOTIFY_STATUS_PENDING)
                .setRetryCount(0)
                .setNextRetryTime(LocalDateTime.now())
                .setLastErrorMsg("")
                .setLastActionCode(NOTIFY_ACTION_CREATE)
                .setLastActionBizNo(StrUtil.maxLength("BIZ#" + bizKey, 64))
                .setLastActionTime(LocalDateTime.now()));
    }

    private LocalDateTime calculateNextRetryTime(LocalDateTime now, int retryCount) {
        int multiplier = 1 << Math.max(0, Math.min(retryCount - 1, 6));
        int delayMinutes = Math.min(60, NOTIFY_BACKOFF_BASE_MINUTES * multiplier);
        return now.plusMinutes(delayMinutes);
    }

    private void dispatchNotify(TechnicianCommissionSettlementNotifyOutboxDO outbox) {
        if (StrUtil.equalsIgnoreCase(outbox.getChannel(), NOTIFY_CHANNEL_IN_APP)) {
            sendInAppNotify(outbox);
            return;
        }
        throw new IllegalArgumentException("unsupported notify channel: " + outbox.getChannel());
    }

    private void sendInAppNotify(TechnicianCommissionSettlementNotifyOutboxDO outbox) {
        TechnicianCommissionSettlementDO settlement = settlementMapper.selectById(outbox.getSettlementId());
        if (settlement == null) {
            throw new IllegalStateException("settlement not exists: " + outbox.getSettlementId());
        }
        Map<String, Object> params = buildInAppNotifyParams(outbox, settlement);
        String templateCode = resolveNotifyTemplateCode(outbox.getNotifyType());
        for (Long recipientId : resolveNotifyAdminUserIds(settlement)) {
            Long messageId = notifySendService.sendSingleNotifyToAdmin(recipientId, templateCode, params);
            if (messageId == null) {
                throw new IllegalStateException("notify message skipped due template status: " + templateCode);
            }
        }
    }

    private Map<String, Object> buildInAppNotifyParams(TechnicianCommissionSettlementNotifyOutboxDO outbox,
                                                        TechnicianCommissionSettlementDO settlement) {
        Map<String, Object> params = new HashMap<>();
        params.put("settlementId", settlement.getId());
        params.put("settlementNo", StrUtil.blankToDefault(settlement.getSettlementNo(), "SET#" + settlement.getId()));
        params.put("notifyType", StrUtil.blankToDefault(outbox.getNotifyType(), ""));
        params.put("severity", StrUtil.blankToDefault(outbox.getSeverity(), ""));
        params.put("deadlineTime", settlement.getReviewDeadlineTime() == null ? "" : settlement.getReviewDeadlineTime().toString());
        return params;
    }

    private String resolveNotifyTemplateCode(String notifyType) {
        if (StrUtil.equals(notifyType, NOTIFY_TYPE_P1_WARN)) {
            return NOTIFY_TEMPLATE_CODE_P1_WARN;
        }
        if (StrUtil.equals(notifyType, NOTIFY_TYPE_P0_ESCALATE)) {
            return NOTIFY_TEMPLATE_CODE_P0_ESCALATE;
        }
        throw new IllegalArgumentException("unsupported notify type: " + notifyType);
    }

    private List<Long> resolveNotifyAdminUserIds(TechnicianCommissionSettlementDO settlement) {
        if (settlement.getReviewerId() != null && settlement.getReviewerId() > 0) {
            return Collections.singletonList(settlement.getReviewerId());
        }
        List<Long> roleUserIds = resolveRoleNotifyAdminUserIds();
        if (!roleUserIds.isEmpty()) {
            return roleUserIds;
        }
        if (StrUtil.isNotBlank(settlement.getCreator())) {
            try {
                long creatorId = Long.parseLong(settlement.getCreator());
                if (creatorId > 0) {
                    return Collections.singletonList(creatorId);
                }
            } catch (NumberFormatException ignored) {
                // creator 可能是用户名，不是用户 ID
            }
        }
        return Collections.singletonList(DEFAULT_NOTIFY_ADMIN_USER_ID);
    }

    private List<Long> resolveRoleNotifyAdminUserIds() {
        List<Long> roleIds = parseNotifyTargetRoleIds(notifyTargetRoleIdsConfig);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = permissionApi.getUserRoleIdListByRoleIds(roleIds);
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> parseNotifyTargetRoleIds(String roleIdsConfig) {
        if (StrUtil.isBlank(roleIdsConfig)) {
            return Collections.emptyList();
        }
        return Arrays.stream(roleIdsConfig.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .filter(StrUtil::isNumeric)
                .map(Long::parseLong)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private String buildDispatchAuditBizNo(Long outboxId, LocalDateTime now) {
        return StrUtil.maxLength(StrUtil.format("OUTBOX#{}@{}",
                ObjUtil.defaultIfNull(outboxId, 0L),
                now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))), 64);
    }

    private String buildManualRetryAuditBizNo(Long operatorId, Long outboxId) {
        String operator = operatorId == null ? "SYSTEM" : String.valueOf(operatorId);
        return StrUtil.maxLength(StrUtil.format("ADMIN#{}/OUTBOX#{}", operator, outboxId), 64);
    }

    private String resolveOperatorType(Long operatorId) {
        return operatorId == null ? OPERATOR_TYPE_SYSTEM : OPERATOR_TYPE_ADMIN;
    }
}
