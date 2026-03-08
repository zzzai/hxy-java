package com.hxy.module.booking.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayRefundNotifyReqDTO;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import cn.iocoder.yudao.module.trade.api.reviewticket.TradeReviewTicketApi;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketUpsertReqDTO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunDetailPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunDetailRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogSummaryRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogSyncTicketRespVO;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunDetailDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayTicketSyncLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundRepairCandidateDO;
import com.hxy.module.booking.dal.mysql.BookingRefundNotifyLogMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReconcileQueryMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReplayRunDetailMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReplayRunDetailQueryMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReplayRunLogMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReplayTicketSyncLogMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.service.BookingOrderService;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import com.hxy.module.booking.service.FourAccountReconcileService;
import com.hxy.module.booking.service.support.FinanceLogFieldValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOT_FOUND;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_LOG_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_REPLAY_RUN_DETAIL_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_REPLAY_RUN_LOG_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_STATUS_INVALID;

@Service
@Validated
@Slf4j
public class BookingRefundNotifyLogServiceImpl implements BookingRefundNotifyLogService {

    private static final Pattern REFUND_BIZ_NO_PATTERN = Pattern.compile("^(\\d+)(?:-refund)?$");

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAIL = "fail";

    private static final String REPLAY_RESULT_SUCCESS = "SUCCESS";
    private static final String REPLAY_RESULT_SKIP = "SKIP";
    private static final String REPLAY_RESULT_FAIL = "FAIL";

    private static final String RESULT_CODE_OK = "OK";
    private static final String RESULT_CODE_SKIPPED = "SKIPPED";
    private static final String RESULT_CODE_DRY_RUN = "DRY_RUN_OK";

    private static final String DEFAULT_OPERATOR = "SYSTEM";
    private static final String DEFAULT_TRIGGER_SOURCE = "MANUAL";

    private static final String RUN_STATUS_STARTED = "started";
    private static final String RUN_STATUS_SUCCESS = "success";
    private static final String RUN_STATUS_PARTIAL_FAIL = "partial_fail";
    private static final String RUN_STATUS_FAIL = "fail";

    private static final String WARNING_TAG_FOUR_ACCOUNT_REFRESH_WARN = "FOUR_ACCOUNT_REFRESH_WARN";
    private static final String WARNING_TAG_TICKET_SYNC_DEGRADED = "TICKET_SYNC_DEGRADED";
    private static final String TICKET_SYNC_STATUS_SUCCESS = "SUCCESS";
    private static final String TICKET_SYNC_STATUS_SKIP = "SKIP";
    private static final String TICKET_SYNC_STATUS_FAIL = "FAIL";
    private static final String TICKET_SYNC_RESULT_CODE_OK = "OK";
    private static final String TICKET_SYNC_RESULT_CODE_DRY_RUN = "DRY_RUN";
    private static final String TICKET_SYNC_RESULT_CODE_SKIP_ALREADY_SUCCESS = "SKIP_ALREADY_SUCCESS";
    private static final String TICKET_SYNC_RESULT_CODE_TRADE_FAIL = "TRADE_UPSERT_FAIL";

    private static final Integer REVIEW_TICKET_TYPE = 40;
    private static final String REVIEW_TICKET_SOURCE_PREFIX = "REFUND_REPLAY_RUN:";
    private static final String REVIEW_TICKET_RULE_CODE_PREFIX = "REFUND_REPLAY_RUN_";
    private static final String REVIEW_TICKET_ACTION_CODE_PREFIX = "REFUND_REPLAY_RUN_";
    private static final String REVIEW_TICKET_DEFAULT_ESCALATE_TO = "HQ_AFTER_SALE";
    private static final int REVIEW_TICKET_P0_SLA_MINUTES = 30;
    private static final int REVIEW_TICKET_P1_SLA_MINUTES = 120;
    private static final int SUMMARY_TOP_N = 5;

    private static final int DEFAULT_RECONCILE_LIMIT = 200;
    private static final int MAX_RECONCILE_LIMIT = 1000;
    private static final int RETRY_BACKOFF_BASE_MINUTES = 5;
    private static final int RETRY_BACKOFF_MAX_MINUTES = 60;
    private static final int MAX_RUN_ERROR_MSG_LENGTH = 1024;

    @Resource
    private BookingRefundNotifyLogMapper refundNotifyLogMapper;
    @Resource
    private BookingRefundReconcileQueryMapper refundReconcileQueryMapper;
    @Resource
    private BookingRefundReplayRunDetailMapper refundReplayRunDetailMapper;
    @Resource
    private BookingRefundReplayRunDetailQueryMapper refundReplayRunDetailQueryMapper;
    @Resource
    private BookingRefundReplayRunLogMapper refundReplayRunLogMapper;
    @Resource
    private BookingRefundReplayTicketSyncLogMapper refundReplayTicketSyncLogMapper;
    @Resource
    private BookingOrderService bookingOrderService;
    @Resource
    private FourAccountReconcileService fourAccountReconcileService;
    @Resource
    private TradeReviewTicketApi tradeReviewTicketApi;

    @Override
    public void recordNotifySuccess(Long orderId, PayRefundNotifyReqDTO notifyReqDTO) {
        persistLogSafely(buildNotifyLog(orderId, notifyReqDTO, STATUS_SUCCESS,
                "", "", 0, null));
    }

    @Override
    public void recordNotifyFailure(Long orderId, PayRefundNotifyReqDTO notifyReqDTO, Throwable throwable) {
        persistLogSafely(buildNotifyLog(orderId, notifyReqDTO, STATUS_FAIL,
                resolveErrorCode(throwable), resolveErrorMsg(throwable), 0,
                LocalDateTime.now().plusMinutes(RETRY_BACKOFF_BASE_MINUTES)));
    }

    @Override
    public PageResult<BookingRefundNotifyLogDO> getNotifyLogPage(BookingRefundNotifyLogPageReqVO reqVO) {
        if (reqVO != null && StrUtil.isNotBlank(reqVO.getStatus())) {
            reqVO.setStatus(reqVO.getStatus().trim().toLowerCase(Locale.ROOT));
        }
        return refundNotifyLogMapper.selectPage(reqVO);
    }

    @Override
    public BookingRefundNotifyLogReplayRespVO replayFailedLogs(List<Long> ids, boolean dryRun,
                                                               Long operatorId, String operatorNickname) {
        BookingRefundNotifyLogReplayRespVO respVO = initReplayResp();
        List<Long> replayIds = normalizeReplayIds(ids);
        if (CollUtil.isEmpty(replayIds)) {
            return respVO;
        }
        String operator = resolveOperator(operatorId, operatorNickname);
        for (Long id : replayIds) {
            BookingRefundNotifyLogReplayRespVO.ReplayDetail detail = replaySingle(id, dryRun, operator);
            respVO.getDetails().add(detail);
            countReplayResult(respVO, detail.getResultStatus());
        }
        return respVO;
    }

    @Override
    public BookingRefundNotifyLogReplayRespVO replayDueFailedLogs(Integer limit, boolean dryRun,
                                                                   Long operatorId, String operatorNickname,
                                                                   String triggerSource) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, DEFAULT_RECONCILE_LIMIT), MAX_RECONCILE_LIMIT));
        String operator = resolveOperator(operatorId, operatorNickname);
        String normalizedTriggerSource = normalizeTriggerSource(triggerSource);

        String runId = generateRunId();
        LocalDateTime startTime = LocalDateTime.now();
        Long runLogId = insertReplayRunLogStartSafely(runId, normalizedTriggerSource, operator, dryRun, safeLimit, startTime);
        FinanceLogFieldValidator.FinanceLogFields startFields = validateFinanceLogFields(
                runId, -1L, -1L, "BOOKING_REFUND_REPLAY_RUN:" + runId, "RUN_START", "booking_refund_replay_due");
        log.info("[finance-audit][scene=booking_refund_replay_due_start][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}][triggerSource={}][dryRun={}]",
                startFields.getRunId(), startFields.getOrderId(), startFields.getPayRefundId(),
                startFields.getSourceBizNo(), startFields.getErrorCode(), normalizedTriggerSource, dryRun);

        BookingRefundNotifyLogReplayRespVO respVO = initReplayResp();
        respVO.setRunId(runId);
        try {
            List<Long> dueIds = refundNotifyLogMapper.selectDueFailIds(LocalDateTime.now(), safeLimit, STATUS_FAIL);
            respVO = replayFailedLogs(dueIds, dryRun, operatorId, operator);
            respVO.setRunId(runId);

            int scannedCount = calculateScannedCount(respVO);
            String runStatus = resolveRunStatus(respVO);
            String runErrorMsg = buildRunErrorMsg(respVO);
            persistReplayRunDetailsSafely(runId, runLogId, respVO);
            updateReplayRunLogResultSafely(runLogId, scannedCount, respVO.getSuccessCount(), respVO.getSkipCount(),
                    respVO.getFailCount(), runStatus, runErrorMsg, LocalDateTime.now());
            FinanceLogFieldValidator.FinanceLogFields doneFields = validateFinanceLogFields(
                    runId, -1L, -1L, "BOOKING_REFUND_REPLAY_RUN:" + runId, "RUN_" + runStatus.toUpperCase(Locale.ROOT),
                    "booking_refund_replay_due");
            log.info("[finance-audit][scene=booking_refund_replay_due_done][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}][scanned={}][success={}][skip={}][fail={}]",
                    doneFields.getRunId(), doneFields.getOrderId(), doneFields.getPayRefundId(),
                    doneFields.getSourceBizNo(), doneFields.getErrorCode(),
                    scannedCount, respVO.getSuccessCount(), respVO.getSkipCount(), respVO.getFailCount());
            return respVO;
        } catch (Exception ex) {
            String errorCode = resolveErrorCode(ex);
            String errorMsg = resolveErrorMsg(ex);
            persistReplayRunDetailsSafely(runId, runLogId, respVO);
            updateReplayRunLogResultSafely(runLogId, 0, 0, 0, 1,
                    RUN_STATUS_FAIL,
                    StrUtil.maxLength("RUN_FAIL:" + errorCode + ":" + errorMsg, MAX_RUN_ERROR_MSG_LENGTH),
                    LocalDateTime.now());
            respVO.setFailCount(ObjectUtil.defaultIfNull(respVO.getFailCount(), 0) + 1);
            respVO.getDetails().add(buildReplayDetail(null, null, null, REPLAY_RESULT_FAIL, errorCode, errorMsg));
            FinanceLogFieldValidator.FinanceLogFields failFields = validateFinanceLogFields(
                    runId, -1L, -1L, "BOOKING_REFUND_REPLAY_RUN:" + runId, errorCode, "booking_refund_replay_due");
            log.warn("[finance-audit][scene=booking_refund_replay_due_fail][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}][errorMsg={}]",
                    failFields.getRunId(), failFields.getOrderId(), failFields.getPayRefundId(),
                    failFields.getSourceBizNo(), failFields.getErrorCode(), errorMsg, ex);
            return respVO;
        }
    }

    @Override
    public PageResult<BookingRefundReplayRunLogDO> getReplayRunLogPage(BookingRefundReplayRunLogPageReqVO reqVO) {
        if (reqVO != null) {
            if (StrUtil.isNotBlank(reqVO.getRunId())) {
                reqVO.setRunId(reqVO.getRunId().trim());
            }
            if (StrUtil.isNotBlank(reqVO.getOperator())) {
                reqVO.setOperator(reqVO.getOperator().trim());
            }
            if (StrUtil.isNotBlank(reqVO.getTriggerSource())) {
                reqVO.setTriggerSource(reqVO.getTriggerSource().trim().toUpperCase(Locale.ROOT));
            }
            if (StrUtil.isNotBlank(reqVO.getStatus())) {
                reqVO.setStatus(reqVO.getStatus().trim().toLowerCase(Locale.ROOT));
            }
            if (reqVO.getMinFailCount() != null && reqVO.getMinFailCount() < 0) {
                reqVO.setMinFailCount(0);
            }
        }
        return refundReplayRunLogMapper.selectPage(reqVO);
    }

    @Override
    public BookingRefundReplayRunLogDO getReplayRunLog(Long id) {
        BookingRefundReplayRunLogDO runLogDO = refundReplayRunLogMapper.selectById(id);
        if (runLogDO == null) {
            throw exception(BOOKING_ORDER_REFUND_REPLAY_RUN_LOG_NOT_EXISTS);
        }
        return runLogDO;
    }

    @Override
    public PageResult<BookingRefundReplayRunDetailRespVO> getReplayRunDetailPage(BookingRefundReplayRunDetailPageReqVO reqVO) {
        normalizeReplayRunDetailPageReq(reqVO);
        Long total = refundReplayRunDetailQueryMapper.countDetailPage(reqVO);
        if (total == null || total <= 0) {
            return PageResult.empty();
        }
        int offset = (reqVO.getPageNo() - 1) * reqVO.getPageSize();
        List<BookingRefundReplayRunDetailRespVO> list =
                refundReplayRunDetailQueryMapper.selectDetailPage(reqVO, offset, reqVO.getPageSize());
        return new PageResult<>(list, total);
    }

    @Override
    public BookingRefundReplayRunDetailRespVO getReplayRunDetail(Long id) {
        BookingRefundReplayRunDetailRespVO detailRespVO = refundReplayRunDetailQueryMapper.selectDetailById(id);
        if (detailRespVO == null) {
            throw exception(BOOKING_ORDER_REFUND_REPLAY_RUN_DETAIL_NOT_EXISTS);
        }
        return detailRespVO;
    }

    @Override
    public BookingRefundReplayRunLogSummaryRespVO getReplayRunLogSummary(String runId) {
        String normalizedRunId = normalizeRunId(runId);
        BookingRefundReplayRunLogDO runLogDO = refundReplayRunLogMapper.selectByRunId(normalizedRunId);
        if (runLogDO == null) {
            throw exception(BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS);
        }
        List<BookingRefundReplayRunDetailDO> detailList = refundReplayRunDetailMapper.selectByRunId(normalizedRunId);
        BookingRefundReplayRunLogSummaryRespVO respVO = new BookingRefundReplayRunLogSummaryRespVO();
        respVO.setRunId(runLogDO.getRunId());
        respVO.setRunStatus(runLogDO.getStatus());
        respVO.setTriggerSource(runLogDO.getTriggerSource());
        respVO.setOperator(runLogDO.getOperator());
        respVO.setDryRun(runLogDO.getDryRun());
        respVO.setStartTime(runLogDO.getStartTime());
        respVO.setEndTime(runLogDO.getEndTime());
        respVO.setScannedCount(ObjectUtil.defaultIfNull(runLogDO.getScannedCount(), 0));
        respVO.setSuccessCount(ObjectUtil.defaultIfNull(runLogDO.getSuccessCount(), 0));
        respVO.setSkipCount(ObjectUtil.defaultIfNull(runLogDO.getSkipCount(), 0));
        respVO.setFailCount(ObjectUtil.defaultIfNull(runLogDO.getFailCount(), 0));
        applyTicketSyncSummary(normalizedRunId, respVO);
        if (CollUtil.isEmpty(detailList)) {
            respVO.setWarningCount(StrUtil.containsIgnoreCase(runLogDO.getErrorMsg(), "WARN#") ? 1 : 0);
            respVO.setTopFailCodes(Collections.emptyList());
            respVO.setTopWarningTags(Collections.emptyList());
            return respVO;
        }
        Map<String, Integer> failCodeCounter = new HashMap<>();
        Map<String, Integer> warningTagCounter = new HashMap<>();
        int warningCount = 0;
        for (BookingRefundReplayRunDetailDO detailDO : detailList) {
            if (detailDO == null) {
                continue;
            }
            if (StrUtil.equalsIgnoreCase(REPLAY_RESULT_FAIL, detailDO.getResultStatus())) {
                String failCode = StrUtil.blankToDefault(detailDO.getResultCode(), "UNKNOWN");
                failCodeCounter.put(failCode, failCodeCounter.getOrDefault(failCode, 0) + 1);
            }
            if (StrUtil.isNotBlank(detailDO.getWarningTag())) {
                warningCount++;
                String warningTag = detailDO.getWarningTag().trim().toUpperCase(Locale.ROOT);
                warningTagCounter.put(warningTag, warningTagCounter.getOrDefault(warningTag, 0) + 1);
            }
        }
        respVO.setWarningCount(warningCount);
        respVO.setTopFailCodes(buildTopBuckets(failCodeCounter));
        respVO.setTopWarningTags(buildTopBuckets(warningTagCounter));
        return respVO;
    }

    @Override
    public BookingRefundReplayRunLogSyncTicketRespVO syncReplayRunLogTickets(String runId, boolean onlyFail,
                                                                              boolean dryRun, boolean forceResync,
                                                                              Long operatorId, String operatorNickname) {
        String normalizedRunId = normalizeRunId(runId);
        BookingRefundReplayRunLogDO runLogDO = refundReplayRunLogMapper.selectByRunId(normalizedRunId);
        if (runLogDO == null) {
            throw exception(BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS);
        }
        List<BookingRefundReplayRunDetailDO> detailList = onlyFail
                ? refundReplayRunDetailMapper.selectByRunIdAndResultStatus(normalizedRunId, REPLAY_RESULT_FAIL)
                : refundReplayRunDetailMapper.selectByRunId(normalizedRunId);
        if (CollUtil.isEmpty(detailList)) {
            return buildSyncTicketResp(normalizedRunId, 0, 0, 0, Collections.emptyList(), Collections.emptyList());
        }
        List<Long> notifyLogIds = detailList.stream().map(BookingRefundReplayRunDetailDO::getNotifyLogId)
                .filter(Objects::nonNull).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        Map<Long, BookingRefundReplayTicketSyncLogDO> latestSyncLogMap =
                buildLatestSyncLogMap(normalizedRunId, notifyLogIds);
        String operator = resolveOperator(operatorId, operatorNickname);
        int successCount = 0;
        int skipCount = 0;
        boolean ticketSyncDegraded = false;
        List<Long> failedIds = new ArrayList<>();
        List<BookingRefundReplayRunLogSyncTicketRespVO.SyncDetail> syncDetails = new ArrayList<>();
        for (BookingRefundReplayRunDetailDO detailDO : detailList) {
            Long failedId = detailDO == null ? null : detailDO.getNotifyLogId();
            Long notifyLogId = detailDO == null ? null : detailDO.getNotifyLogId();
            if (notifyLogId == null) {
                skipCount++;
                syncDetails.add(buildSyncDetail(null, null, TICKET_SYNC_RESULT_CODE_SKIP_ALREADY_SUCCESS, "明细缺少 notifyLogId，跳过同步"));
                continue;
            }
            BookingRefundReplayTicketSyncLogDO latestSyncLog = latestSyncLogMap.get(notifyLogId);
            if (!forceResync && isTicketAlreadySuccess(latestSyncLog)) {
                skipCount++;
                String skipMsg = "工单已同步成功，跳过";
                persistTicketSyncLogSafely(buildTicketSyncLog(normalizedRunId, notifyLogId,
                        buildReplayRunTicketSourceBizNo(normalizedRunId, notifyLogId), latestSyncLog.getTicketId(),
                        TICKET_SYNC_STATUS_SKIP, TICKET_SYNC_RESULT_CODE_SKIP_ALREADY_SUCCESS, skipMsg, operator));
                syncDetails.add(buildSyncDetail(notifyLogId, latestSyncLog.getTicketId(),
                        TICKET_SYNC_RESULT_CODE_SKIP_ALREADY_SUCCESS, skipMsg));
                continue;
            }
            if (dryRun) {
                skipCount++;
                String dryRunMsg = "预演通过（未执行工单同步）";
                persistTicketSyncLogSafely(buildTicketSyncLog(normalizedRunId, notifyLogId,
                        buildReplayRunTicketSourceBizNo(normalizedRunId, notifyLogId), null,
                        TICKET_SYNC_STATUS_SKIP, TICKET_SYNC_RESULT_CODE_DRY_RUN, dryRunMsg, operator));
                syncDetails.add(buildSyncDetail(notifyLogId, null, TICKET_SYNC_RESULT_CODE_DRY_RUN, dryRunMsg));
                continue;
            }
            try {
                Long ticketId = tradeReviewTicketApi.upsertReviewTicket(buildReplayRunTicketReq(normalizedRunId, detailDO, operator));
                persistTicketSyncLogSafely(buildTicketSyncLog(normalizedRunId, notifyLogId,
                        buildReplayRunTicketSourceBizNo(normalizedRunId, notifyLogId), ticketId,
                        TICKET_SYNC_STATUS_SUCCESS, TICKET_SYNC_RESULT_CODE_OK, "工单同步成功", operator));
                successCount++;
                syncDetails.add(buildSyncDetail(notifyLogId, ticketId, TICKET_SYNC_RESULT_CODE_OK, "工单同步成功"));
            } catch (Exception ex) {
                ticketSyncDegraded = true;
                if (failedId != null) {
                    failedIds.add(failedId);
                }
                String errorCode = resolveErrorCode(ex);
                String errorMsg = resolveErrorMsg(ex);
                persistTicketSyncLogSafely(buildTicketSyncLog(normalizedRunId, notifyLogId,
                        buildReplayRunTicketSourceBizNo(normalizedRunId, notifyLogId), null,
                        TICKET_SYNC_STATUS_FAIL, errorCode, errorMsg, operator));
                syncDetails.add(buildSyncDetail(notifyLogId, null, errorCode, errorMsg));
                FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                        normalizedRunId, detailDO.getOrderId(), detailDO.getPayRefundId(),
                        buildReplayRunTicketSourceBizNo(normalizedRunId, notifyLogId), errorCode,
                        "booking_refund_sync_tickets");
                log.warn("[syncReplayRunLogTickets][{}][upsert ticket fail, runId={}, notifyLogId={}]",
                        WARNING_TAG_TICKET_SYNC_DEGRADED, normalizedRunId, failedId, ex);
                log.warn("[finance-audit][scene=booking_refund_sync_ticket_fail][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                        fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                        fields.getSourceBizNo(), fields.getErrorCode());
            }
        }
        if (ticketSyncDegraded) {
            log.warn("[syncReplayRunLogTickets][{}][runId={}, attempted={}, success={}, failed={}, fail-open continue]",
                    WARNING_TAG_TICKET_SYNC_DEGRADED, normalizedRunId, detailList.size(), successCount, failedIds.size());
        }
        return buildSyncTicketResp(normalizedRunId, detailList.size(), successCount, skipCount, failedIds, syncDetails);
    }

    @Override
    public int reconcileRefundedOrders(Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, DEFAULT_RECONCILE_LIMIT), MAX_RECONCILE_LIMIT));
        List<BookingRefundRepairCandidateDO> candidates = refundReconcileQueryMapper.selectRepairCandidates(
                PayRefundStatusEnum.SUCCESS.getStatus(),
                BookingOrderStatusEnum.REFUNDED.getStatus(),
                safeLimit);
        if (candidates.isEmpty()) {
            return 0;
        }
        int successCount = 0;
        for (BookingRefundRepairCandidateDO candidate : candidates) {
            try {
                bookingOrderService.updateOrderRefunded(candidate.getOrderId(), candidate.getPayRefundId());
                assertOrderRefundSynced(candidate.getOrderId(), candidate.getPayRefundId());
                persistLogSafely(buildRepairLog(candidate, STATUS_SUCCESS, null));
                successCount++;
            } catch (Exception ex) {
                persistLogSafely(buildRepairLog(candidate, STATUS_FAIL, ex));
                FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                        "NO_RUN", candidate.getOrderId(), candidate.getPayRefundId(),
                        candidate.getMerchantRefundId(), resolveErrorCode(ex), "booking_refund_reconcile_repair");
                log.error("[reconcileRefundedOrders][repair failed][orderId={}, payRefundId={}]",
                        candidate.getOrderId(), candidate.getPayRefundId(), ex);
                log.error("[finance-audit][scene=booking_refund_reconcile_repair_fail][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                        fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                        fields.getSourceBizNo(), fields.getErrorCode());
            }
        }
        return successCount;
    }

    private BookingRefundNotifyLogReplayRespVO.ReplayDetail replaySingle(Long id, boolean dryRun, String operator) {
        BookingRefundNotifyLogDO logDO = refundNotifyLogMapper.selectById(id);
        if (logDO == null) {
            return buildReplayDetail(id, null, null, REPLAY_RESULT_FAIL,
                    String.valueOf(BOOKING_ORDER_REFUND_NOTIFY_LOG_NOT_EXISTS.getCode()),
                    BOOKING_ORDER_REFUND_NOTIFY_LOG_NOT_EXISTS.getMsg());
        }
        if (dryRun) {
            return dryRunSingle(logDO, operator);
        }
        if (StrUtil.equalsIgnoreCase(STATUS_SUCCESS, logDO.getStatus())) {
            return skipSingle(logDO, operator, "已成功记录，跳过重放");
        }
        if (!StrUtil.equalsIgnoreCase(STATUS_FAIL, logDO.getStatus())) {
            String code = String.valueOf(BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID.getCode());
            String msg = BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID.getMsg();
            updateReplayAuditSafely(logDO.getId(), operator, REPLAY_RESULT_FAIL,
                    "REPLAY_FAIL_INVALID_STATUS:" + StrUtil.blankToDefault(logDO.getStatus(), "NULL"));
            return buildReplayDetail(logDO.getId(), logDO.getOrderId(), logDO.getPayRefundId(),
                    REPLAY_RESULT_FAIL, code, msg);
        }

        Long orderId = logDO.getOrderId();
        Long payRefundId = logDO.getPayRefundId();
        int nextRetryCount = ObjectUtil.defaultIfNull(logDO.getRetryCount(), 0) + 1;
        try {
            orderId = resolveOrderId(logDO);
            payRefundId = resolvePayRefundId(logDO);
            bookingOrderService.updateOrderRefunded(orderId, payRefundId);
            assertOrderRefundSynced(orderId, payRefundId);
            String warningRemark = refreshFourAccountSafely("NO_RUN", orderId, payRefundId,
                    logDO.getMerchantRefundId(), operator);
            String resultMsg = StrUtil.isBlank(warningRemark)
                    ? "重放成功"
                    : "重放成功（四账刷新降级:" + warningRemark + "）";
            String replayRemark = StrUtil.isBlank(warningRemark)
                    ? "REPLAY_SUCCESS"
                    : "REPLAY_SUCCESS|" + warningRemark;
            refundNotifyLogMapper.updateReplaySuccess(logDO.getId(), STATUS_SUCCESS, nextRetryCount,
                    operator, LocalDateTime.now(), REPLAY_RESULT_SUCCESS, StrUtil.maxLength(replayRemark, 512));
            FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                    "NO_RUN", orderId, payRefundId,
                    StrUtil.blankToDefault(logDO.getMerchantRefundId(), ""), RESULT_CODE_OK,
                    "booking_refund_replay_single");
            log.info("[finance-audit][scene=booking_refund_replay_single_success][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                    fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                    fields.getSourceBizNo(), fields.getErrorCode());
            return buildReplayDetail(logDO.getId(), orderId, payRefundId,
                    REPLAY_RESULT_SUCCESS, RESULT_CODE_OK, resultMsg);
        } catch (Exception ex) {
            String errorCode = resolveErrorCode(ex);
            String errorMsg = resolveErrorMsg(ex);
            String replayRemark = StrUtil.maxLength("REPLAY_FAIL:" + errorCode + ":" + errorMsg, 512);
            refundNotifyLogMapper.updateReplayFailure(logDO.getId(), STATUS_FAIL, nextRetryCount,
                    LocalDateTime.now().plusMinutes(resolveRetryBackoffMinutes(nextRetryCount)),
                    errorCode, errorMsg,
                    operator, LocalDateTime.now(), REPLAY_RESULT_FAIL, replayRemark);
            FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                    "NO_RUN", orderId, payRefundId,
                    StrUtil.blankToDefault(logDO.getMerchantRefundId(), ""), errorCode,
                    "booking_refund_replay_single");
            log.warn("[finance-audit][scene=booking_refund_replay_single_fail][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                    fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                    fields.getSourceBizNo(), fields.getErrorCode());
            return buildReplayDetail(logDO.getId(), orderId, payRefundId,
                    REPLAY_RESULT_FAIL, errorCode, errorMsg);
        }
    }

    private BookingRefundNotifyLogReplayRespVO.ReplayDetail dryRunSingle(BookingRefundNotifyLogDO logDO, String operator) {
        if (StrUtil.equalsIgnoreCase(STATUS_SUCCESS, logDO.getStatus())) {
            updateReplayAuditSafely(logDO.getId(), operator, REPLAY_RESULT_SKIP,
                    "DRY_RUN_SKIP_ALREADY_SUCCESS");
            return buildReplayDetail(logDO.getId(), logDO.getOrderId(), logDO.getPayRefundId(),
                    REPLAY_RESULT_SKIP, RESULT_CODE_SKIPPED, "预演跳过：已成功记录");
        }
        if (!StrUtil.equalsIgnoreCase(STATUS_FAIL, logDO.getStatus())) {
            String code = String.valueOf(BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID.getCode());
            String msg = BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID.getMsg();
            updateReplayAuditSafely(logDO.getId(), operator, REPLAY_RESULT_FAIL,
                    "DRY_RUN_FAIL_INVALID_STATUS:" + StrUtil.blankToDefault(logDO.getStatus(), "NULL"));
            return buildReplayDetail(logDO.getId(), logDO.getOrderId(), logDO.getPayRefundId(),
                    REPLAY_RESULT_FAIL, code, msg);
        }

        Long orderId = logDO.getOrderId();
        Long payRefundId = logDO.getPayRefundId();
        try {
            orderId = resolveOrderId(logDO);
            payRefundId = resolvePayRefundId(logDO);
            BookingOrderDO order = bookingOrderService.getOrder(orderId);
            if (order == null) {
                throw exception(BOOKING_ORDER_NOT_EXISTS);
            }
            if (Objects.equals(BookingOrderStatusEnum.REFUNDED.getStatus(), order.getStatus())) {
                if (Objects.equals(order.getPayRefundId(), payRefundId)) {
                    updateReplayAuditSafely(logDO.getId(), operator, REPLAY_RESULT_SKIP,
                            "DRY_RUN_SKIP_ALREADY_REFUNDED");
                    return buildReplayDetail(logDO.getId(), orderId, payRefundId,
                            REPLAY_RESULT_SKIP, RESULT_CODE_SKIPPED, "预演跳过：订单已退款且退款单一致");
                }
                if (order.getPayRefundId() != null) {
                    throw exception(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT);
                }
            }
            updateReplayAuditSafely(logDO.getId(), operator, REPLAY_RESULT_SUCCESS,
                    "DRY_RUN_PASS");
            return buildReplayDetail(logDO.getId(), orderId, payRefundId,
                    REPLAY_RESULT_SUCCESS, RESULT_CODE_DRY_RUN, "预演通过");
        } catch (Exception ex) {
            String errorCode = resolveErrorCode(ex);
            String errorMsg = resolveErrorMsg(ex);
            updateReplayAuditSafely(logDO.getId(), operator, REPLAY_RESULT_FAIL,
                    StrUtil.maxLength("DRY_RUN_FAIL:" + errorCode + ":" + errorMsg, 512));
            return buildReplayDetail(logDO.getId(), orderId, payRefundId,
                    REPLAY_RESULT_FAIL, errorCode, errorMsg);
        }
    }

    private BookingRefundNotifyLogReplayRespVO.ReplayDetail skipSingle(BookingRefundNotifyLogDO logDO,
                                                                        String operator,
                                                                        String message) {
        updateReplayAuditSafely(logDO.getId(), operator, REPLAY_RESULT_SKIP,
                "REPLAY_SKIP_ALREADY_SUCCESS");
        return buildReplayDetail(logDO.getId(), logDO.getOrderId(), logDO.getPayRefundId(),
                REPLAY_RESULT_SKIP, RESULT_CODE_SKIPPED, message);
    }

    private Long insertReplayRunLogStartSafely(String runId, String triggerSource, String operator,
                                               boolean dryRun, Integer limitSize, LocalDateTime startTime) {
        try {
            BookingRefundReplayRunLogDO runLogDO = new BookingRefundReplayRunLogDO()
                    .setRunId(runId)
                    .setTriggerSource(triggerSource)
                    .setOperator(operator)
                    .setDryRun(dryRun)
                    .setLimitSize(limitSize)
                    .setScannedCount(0)
                    .setSuccessCount(0)
                    .setSkipCount(0)
                    .setFailCount(0)
                    .setStatus(RUN_STATUS_STARTED)
                    .setErrorMsg("")
                    .setStartTime(startTime)
                    .setEndTime(null);
            refundReplayRunLogMapper.insert(runLogDO);
            return runLogDO.getId();
        } catch (Exception ex) {
            log.error("[insertReplayRunLogStartSafely][runId={}]", runId, ex);
            return null;
        }
    }

    private void updateReplayRunLogResultSafely(Long runLogId, Integer scannedCount,
                                                Integer successCount, Integer skipCount, Integer failCount,
                                                String status, String errorMsg, LocalDateTime endTime) {
        if (runLogId == null) {
            return;
        }
        try {
            refundReplayRunLogMapper.updateRunResult(runLogId, scannedCount, successCount, skipCount,
                    failCount, status, StrUtil.maxLength(StrUtil.blankToDefault(errorMsg, ""), MAX_RUN_ERROR_MSG_LENGTH), endTime);
        } catch (Exception ex) {
            log.error("[updateReplayRunLogResultSafely][runLogId={}]", runLogId, ex);
        }
    }

    private String resolveRunStatus(BookingRefundNotifyLogReplayRespVO respVO) {
        int success = ObjectUtil.defaultIfNull(respVO.getSuccessCount(), 0);
        int skip = ObjectUtil.defaultIfNull(respVO.getSkipCount(), 0);
        int fail = ObjectUtil.defaultIfNull(respVO.getFailCount(), 0);
        if (fail <= 0) {
            return RUN_STATUS_SUCCESS;
        }
        if (success + skip <= 0) {
            return RUN_STATUS_FAIL;
        }
        return RUN_STATUS_PARTIAL_FAIL;
    }

    private int calculateScannedCount(BookingRefundNotifyLogReplayRespVO respVO) {
        return ObjectUtil.defaultIfNull(respVO.getSuccessCount(), 0)
                + ObjectUtil.defaultIfNull(respVO.getSkipCount(), 0)
                + ObjectUtil.defaultIfNull(respVO.getFailCount(), 0);
    }

    private String buildRunErrorMsg(BookingRefundNotifyLogReplayRespVO respVO) {
        if (respVO == null || CollUtil.isEmpty(respVO.getDetails())) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (BookingRefundNotifyLogReplayRespVO.ReplayDetail detail : respVO.getDetails()) {
            if (detail == null) {
                continue;
            }
            if (StrUtil.equalsIgnoreCase(REPLAY_RESULT_FAIL, detail.getResultStatus())) {
                appendRunIssue(builder, String.format("FAIL#id=%s#code=%s#msg=%s",
                        String.valueOf(detail.getId()),
                        StrUtil.blankToDefault(detail.getResultCode(), ""),
                        StrUtil.blankToDefault(detail.getResultMsg(), "")));
            } else if (StrUtil.containsIgnoreCase(detail.getResultMsg(), "FOUR_ACCOUNT_REFRESH_WARN")) {
                appendRunIssue(builder, String.format("WARN#id=%s#msg=%s",
                        String.valueOf(detail.getId()),
                        StrUtil.blankToDefault(detail.getResultMsg(), "")));
            }
            if (builder.length() >= MAX_RUN_ERROR_MSG_LENGTH) {
                break;
            }
        }
        return StrUtil.maxLength(builder.toString(), MAX_RUN_ERROR_MSG_LENGTH);
    }

    private void appendRunIssue(StringBuilder builder, String issue) {
        if (StrUtil.isBlank(issue)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("; ");
        }
        builder.append(issue);
    }

    private void persistReplayRunDetailsSafely(String runId, Long runLogId, BookingRefundNotifyLogReplayRespVO respVO) {
        if (StrUtil.isBlank(runId) || respVO == null || CollUtil.isEmpty(respVO.getDetails())) {
            return;
        }
        for (BookingRefundNotifyLogReplayRespVO.ReplayDetail detail : respVO.getDetails()) {
            if (detail == null || detail.getId() == null) {
                continue;
            }
            BookingRefundReplayRunDetailDO detailDO = new BookingRefundReplayRunDetailDO()
                    .setRunId(runId)
                    .setRunLogId(runLogId)
                    .setNotifyLogId(detail.getId())
                    .setOrderId(detail.getOrderId())
                    .setPayRefundId(detail.getPayRefundId())
                    .setResultStatus(StrUtil.blankToDefault(detail.getResultStatus(), REPLAY_RESULT_FAIL).toUpperCase(Locale.ROOT))
                    .setResultCode(StrUtil.blankToDefault(detail.getResultCode(), ""))
                    .setResultMsg(StrUtil.maxLength(StrUtil.blankToDefault(detail.getResultMsg(), ""), 512))
                    .setWarningTag(extractWarningTag(detail.getResultMsg()));
            try {
                refundReplayRunDetailMapper.insert(detailDO);
            } catch (DuplicateKeyException duplicateKeyException) {
                log.debug("[persistReplayRunDetailsSafely][duplicate detail skipped][runId={}, notifyLogId={}]",
                        runId, detail.getId());
            } catch (Exception ex) {
                log.warn("[persistReplayRunDetailsSafely][insert run detail fail][runId={}, notifyLogId={}]",
                        runId, detail.getId(), ex);
            }
        }
    }

    private String extractWarningTag(String resultMsg) {
        if (StrUtil.containsIgnoreCase(resultMsg, WARNING_TAG_FOUR_ACCOUNT_REFRESH_WARN)) {
            return WARNING_TAG_FOUR_ACCOUNT_REFRESH_WARN;
        }
        return "";
    }

    private String normalizeRunId(String runId) {
        String normalized = runId == null ? "" : runId.trim();
        if (StrUtil.isBlank(normalized)) {
            throw exception(BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS);
        }
        return normalized;
    }

    private void normalizeReplayRunDetailPageReq(BookingRefundReplayRunDetailPageReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        if (StrUtil.isNotBlank(reqVO.getRunId())) {
            reqVO.setRunId(reqVO.getRunId().trim());
        }
        if (StrUtil.isNotBlank(reqVO.getResultStatus())) {
            reqVO.setResultStatus(reqVO.getResultStatus().trim().toUpperCase(Locale.ROOT));
        }
        if (StrUtil.isNotBlank(reqVO.getResultCode())) {
            reqVO.setResultCode(reqVO.getResultCode().trim());
        }
        if (StrUtil.isNotBlank(reqVO.getWarningTag())) {
            reqVO.setWarningTag(reqVO.getWarningTag().trim().toUpperCase(Locale.ROOT));
        }
        if (StrUtil.isNotBlank(reqVO.getTicketSyncStatus())) {
            reqVO.setTicketSyncStatus(reqVO.getTicketSyncStatus().trim().toUpperCase(Locale.ROOT));
        }
    }

    private void applyTicketSyncSummary(String runId, BookingRefundReplayRunLogSummaryRespVO respVO) {
        respVO.setTicketSyncSuccessCount(0);
        respVO.setTicketSyncSkipCount(0);
        respVO.setTicketSyncFailCount(0);
        if (StrUtil.isBlank(runId)) {
            return;
        }
        List<BookingRefundReplayTicketSyncLogDO> latestSyncLogList = refundReplayTicketSyncLogMapper.selectLatestByRunId(runId);
        if (CollUtil.isEmpty(latestSyncLogList)) {
            return;
        }
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;
        for (BookingRefundReplayTicketSyncLogDO syncLogDO : latestSyncLogList) {
            if (syncLogDO == null || StrUtil.isBlank(syncLogDO.getStatus())) {
                continue;
            }
            if (StrUtil.equalsIgnoreCase(TICKET_SYNC_STATUS_SUCCESS, syncLogDO.getStatus())) {
                successCount++;
                continue;
            }
            if (StrUtil.equalsIgnoreCase(TICKET_SYNC_STATUS_SKIP, syncLogDO.getStatus())) {
                skipCount++;
                continue;
            }
            if (StrUtil.equalsIgnoreCase(TICKET_SYNC_STATUS_FAIL, syncLogDO.getStatus())) {
                failCount++;
            }
        }
        respVO.setTicketSyncSuccessCount(successCount);
        respVO.setTicketSyncSkipCount(skipCount);
        respVO.setTicketSyncFailCount(failCount);
    }

    private List<BookingRefundReplayRunLogSummaryRespVO.SummaryBucket> buildTopBuckets(Map<String, Integer> counter) {
        if (counter == null || counter.isEmpty()) {
            return Collections.emptyList();
        }
        return counter.entrySet().stream()
                .filter(entry -> StrUtil.isNotBlank(entry.getKey()))
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(SUMMARY_TOP_N)
                .collect(ArrayList::new, (list, entry) -> {
                    BookingRefundReplayRunLogSummaryRespVO.SummaryBucket bucket =
                            new BookingRefundReplayRunLogSummaryRespVO.SummaryBucket();
                    bucket.setKey(entry.getKey());
                    bucket.setCount(entry.getValue());
                    list.add(bucket);
                }, ArrayList::addAll);
    }

    private BookingRefundReplayRunLogSyncTicketRespVO buildSyncTicketResp(String runId, int attemptedCount,
                                                                           int successCount, int skipCount,
                                                                           List<Long> failedIds,
                                                                           List<BookingRefundReplayRunLogSyncTicketRespVO.SyncDetail> details) {
        BookingRefundReplayRunLogSyncTicketRespVO respVO = new BookingRefundReplayRunLogSyncTicketRespVO();
        respVO.setRunId(runId);
        respVO.setAttemptedCount(attemptedCount);
        respVO.setSuccessCount(successCount);
        respVO.setSkipCount(skipCount);
        respVO.setFailedCount(Math.max(attemptedCount - successCount - skipCount, 0));
        respVO.setFailedIds(failedIds == null ? Collections.emptyList() : failedIds);
        respVO.setDetails(details == null ? Collections.emptyList() : details);
        return respVO;
    }

    private BookingRefundReplayRunLogSyncTicketRespVO.SyncDetail buildSyncDetail(Long notifyLogId, Long ticketId,
                                                                                  String resultCode, String resultMsg) {
        BookingRefundReplayRunLogSyncTicketRespVO.SyncDetail detail =
                new BookingRefundReplayRunLogSyncTicketRespVO.SyncDetail();
        detail.setNotifyLogId(notifyLogId);
        detail.setTicketId(ticketId);
        detail.setResultCode(StrUtil.blankToDefault(resultCode, ""));
        detail.setResultMsg(StrUtil.blankToDefault(resultMsg, ""));
        return detail;
    }

    private Map<Long, BookingRefundReplayTicketSyncLogDO> buildLatestSyncLogMap(String runId, List<Long> notifyLogIds) {
        if (StrUtil.isBlank(runId) || CollUtil.isEmpty(notifyLogIds)) {
            return Collections.emptyMap();
        }
        List<BookingRefundReplayTicketSyncLogDO> latestList =
                refundReplayTicketSyncLogMapper.selectLatestByRunIdAndNotifyLogIds(runId, notifyLogIds);
        if (CollUtil.isEmpty(latestList)) {
            return Collections.emptyMap();
        }
        Map<Long, BookingRefundReplayTicketSyncLogDO> latestMap = new HashMap<>();
        for (BookingRefundReplayTicketSyncLogDO syncLogDO : latestList) {
            if (syncLogDO == null || syncLogDO.getNotifyLogId() == null) {
                continue;
            }
            latestMap.put(syncLogDO.getNotifyLogId(), syncLogDO);
        }
        return latestMap;
    }

    private boolean isTicketAlreadySuccess(BookingRefundReplayTicketSyncLogDO syncLogDO) {
        return syncLogDO != null && StrUtil.equalsIgnoreCase(TICKET_SYNC_STATUS_SUCCESS, syncLogDO.getStatus());
    }

    private String buildReplayRunTicketSourceBizNo(String runId, Long notifyLogId) {
        Long sourceNotifyId = notifyLogId == null ? 0L : notifyLogId;
        return REVIEW_TICKET_SOURCE_PREFIX + runId + ":" + sourceNotifyId;
    }

    private BookingRefundReplayTicketSyncLogDO buildTicketSyncLog(String runId, Long notifyLogId, String sourceBizNo,
                                                                   Long ticketId, String status,
                                                                   String errorCode, String errorMsg,
                                                                   String operator) {
        return new BookingRefundReplayTicketSyncLogDO()
                .setRunId(runId)
                .setNotifyLogId(notifyLogId)
                .setSourceBizNo(StrUtil.blankToDefault(sourceBizNo, ""))
                .setTicketId(ticketId)
                .setStatus(StrUtil.blankToDefault(status, TICKET_SYNC_STATUS_FAIL))
                .setErrorCode(StrUtil.blankToDefault(errorCode, ""))
                .setErrorMsg(StrUtil.maxLength(StrUtil.blankToDefault(errorMsg, ""), 512))
                .setOperator(StrUtil.blankToDefault(operator, DEFAULT_OPERATOR))
                .setSyncTime(LocalDateTime.now());
    }

    private void persistTicketSyncLogSafely(BookingRefundReplayTicketSyncLogDO syncLogDO) {
        if (syncLogDO == null) {
            return;
        }
        try {
            refundReplayTicketSyncLogMapper.insert(syncLogDO);
        } catch (Exception ex) {
            log.error("[persistTicketSyncLogSafely][runId={}, notifyLogId={}, status={}]",
                    syncLogDO.getRunId(), syncLogDO.getNotifyLogId(), syncLogDO.getStatus(), ex);
        }
    }

    private TradeReviewTicketUpsertReqDTO buildReplayRunTicketReq(String runId,
                                                                   BookingRefundReplayRunDetailDO detailDO,
                                                                   String operator) {
        String resultCode = detailDO == null ? "" : StrUtil.blankToDefault(detailDO.getResultCode(), "");
        String normalizedCode = StrUtil.isBlank(resultCode) ? "UNKNOWN" : resultCode.trim().toUpperCase(Locale.ROOT);
        String resultStatus = detailDO == null ? REPLAY_RESULT_FAIL
                : StrUtil.blankToDefault(detailDO.getResultStatus(), REPLAY_RESULT_FAIL).trim().toUpperCase(Locale.ROOT);
        String severity = resolveReplayTicketSeverity(normalizedCode, resultStatus);
        int slaMinutes = "P0".equals(severity) ? REVIEW_TICKET_P0_SLA_MINUTES : REVIEW_TICKET_P1_SLA_MINUTES;
        Long notifyLogId = detailDO == null ? null : detailDO.getNotifyLogId();
        Long sourceNotifyId = notifyLogId == null ? 0L : notifyLogId;
        String sourceBizNo = REVIEW_TICKET_SOURCE_PREFIX + runId + ":" + sourceNotifyId;
        return new TradeReviewTicketUpsertReqDTO()
                .setTicketType(REVIEW_TICKET_TYPE)
                .setOrderId(detailDO == null ? null : detailDO.getOrderId())
                .setSourceBizNo(sourceBizNo)
                .setRuleCode(REVIEW_TICKET_RULE_CODE_PREFIX + normalizedCode)
                .setDecisionReason(detailDO == null ? "" : StrUtil.blankToDefault(detailDO.getResultMsg(), ""))
                .setSeverity(severity)
                .setEscalateTo(REVIEW_TICKET_DEFAULT_ESCALATE_TO)
                .setSlaMinutes(slaMinutes)
                .setRemark(buildReplayRunTicketRemark(runId, detailDO, operator))
                .setActionCode(REVIEW_TICKET_ACTION_CODE_PREFIX + resultStatus);
    }

    private String resolveReplayTicketSeverity(String resultCode, String resultStatus) {
        if (!StrUtil.equalsIgnoreCase(REPLAY_RESULT_FAIL, resultStatus)) {
            return "P1";
        }
        if (Objects.equals(resultCode, String.valueOf(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT.getCode()))
                || Objects.equals(resultCode, String.valueOf(BOOKING_ORDER_REFUND_STATUS_INVALID.getCode()))) {
            return "P0";
        }
        return "P1";
    }

    private String buildReplayRunTicketRemark(String runId, BookingRefundReplayRunDetailDO detailDO, String operator) {
        if (detailDO == null) {
            return "runId=" + runId + ",detail=null,operator=" + StrUtil.blankToDefault(operator, DEFAULT_OPERATOR);
        }
        return String.format(Locale.ROOT,
                "runId=%s,notifyLogId=%s,status=%s,code=%s,warning=%s,operator=%s,msg=%s",
                runId,
                String.valueOf(detailDO.getNotifyLogId()),
                StrUtil.blankToDefault(detailDO.getResultStatus(), ""),
                StrUtil.blankToDefault(detailDO.getResultCode(), ""),
                StrUtil.blankToDefault(detailDO.getWarningTag(), ""),
                StrUtil.blankToDefault(operator, DEFAULT_OPERATOR),
                StrUtil.maxLength(StrUtil.blankToDefault(detailDO.getResultMsg(), ""), 200));
    }

    private String generateRunId() {
        return "RR" + UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizeTriggerSource(String triggerSource) {
        if (StrUtil.isBlank(triggerSource)) {
            return DEFAULT_TRIGGER_SOURCE;
        }
        String normalized = triggerSource.trim().toUpperCase(Locale.ROOT);
        if ("JOB".equals(normalized)) {
            return "JOB";
        }
        return "MANUAL";
    }

    private void updateReplayAuditSafely(Long id, String operator, String replayResult, String replayRemark) {
        try {
            refundNotifyLogMapper.updateReplayAudit(id, operator, LocalDateTime.now(), replayResult,
                    StrUtil.maxLength(StrUtil.blankToDefault(replayRemark, ""), 512));
        } catch (Exception ex) {
            log.error("[updateReplayAuditSafely][id={}, result={}]", id, replayResult, ex);
        }
    }

    private String refreshFourAccountSafely(String runId, Long orderId, Long payRefundId,
                                            String sourceBizNo, String operator) {
        try {
            BookingOrderDO order = bookingOrderService.getOrder(orderId);
            if (order == null || order.getRefundTime() == null) {
                return "FOUR_ACCOUNT_REFRESH_SKIPPED_NO_REFUND_TIME";
            }
            fourAccountReconcileService.runReconcile(order.getRefundTime().toLocalDate(), "REFUND_NOTIFY_REPLAY",
                    StrUtil.blankToDefault(operator, DEFAULT_OPERATOR));
            return "";
        } catch (Exception ex) {
            String errorCode = resolveErrorCode(ex);
            FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                    runId, orderId, payRefundId, sourceBizNo, errorCode, "booking_refund_refresh_four_account");
            log.warn("[refreshFourAccountSafely][orderId={}] refresh failed, degrade continue", orderId, ex);
            log.warn("[finance-audit][scene=booking_refund_refresh_four_account_fail][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                    fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                    fields.getSourceBizNo(), fields.getErrorCode());
            return "FOUR_ACCOUNT_REFRESH_WARN:" + errorCode;
        }
    }

    private void countReplayResult(BookingRefundNotifyLogReplayRespVO respVO, String resultStatus) {
        if (StrUtil.equalsIgnoreCase(REPLAY_RESULT_SUCCESS, resultStatus)) {
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
            return;
        }
        if (StrUtil.equalsIgnoreCase(REPLAY_RESULT_SKIP, resultStatus)) {
            respVO.setSkipCount(respVO.getSkipCount() + 1);
            return;
        }
        respVO.setFailCount(respVO.getFailCount() + 1);
    }

    private BookingRefundNotifyLogReplayRespVO initReplayResp() {
        BookingRefundNotifyLogReplayRespVO respVO = new BookingRefundNotifyLogReplayRespVO();
        respVO.setSuccessCount(0);
        respVO.setSkipCount(0);
        respVO.setFailCount(0);
        respVO.setDetails(new ArrayList<>());
        return respVO;
    }

    private List<Long> normalizeReplayIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                uniqueIds.add(id);
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private String resolveOperator(Long operatorId, String operatorNickname) {
        if (StrUtil.isNotBlank(operatorNickname)) {
            return operatorNickname.trim();
        }
        if (operatorId != null && operatorId > 0) {
            return String.valueOf(operatorId);
        }
        return DEFAULT_OPERATOR;
    }

    private BookingRefundNotifyLogReplayRespVO.ReplayDetail buildReplayDetail(Long id, Long orderId, Long payRefundId,
                                                                               String resultStatus,
                                                                               String resultCode,
                                                                               String resultMsg) {
        BookingRefundNotifyLogReplayRespVO.ReplayDetail detail = new BookingRefundNotifyLogReplayRespVO.ReplayDetail();
        detail.setId(id);
        detail.setOrderId(orderId);
        detail.setPayRefundId(payRefundId);
        detail.setResultStatus(resultStatus);
        detail.setResultCode(StrUtil.blankToDefault(resultCode, ""));
        detail.setResultMsg(StrUtil.blankToDefault(resultMsg, ""));
        detail.setResultMessage(StrUtil.blankToDefault(resultMsg, ""));
        return detail;
    }

    private BookingRefundNotifyLogDO buildNotifyLog(Long orderId, PayRefundNotifyReqDTO notifyReqDTO, String status,
                                                    String errorCode, String errorMsg, Integer retryCount,
                                                    LocalDateTime nextRetryTime) {
        return new BookingRefundNotifyLogDO()
                .setOrderId(orderId)
                .setMerchantRefundId(StrUtil.blankToDefault(notifyReqDTO == null ? null : notifyReqDTO.getMerchantRefundId(), ""))
                .setPayRefundId(notifyReqDTO == null ? null : notifyReqDTO.getPayRefundId())
                .setStatus(status)
                .setErrorCode(StrUtil.blankToDefault(errorCode, ""))
                .setErrorMsg(StrUtil.blankToDefault(errorMsg, ""))
                .setRawPayload(toRawPayload(notifyReqDTO))
                .setRetryCount(ObjectUtil.defaultIfNull(retryCount, 0))
                .setNextRetryTime(nextRetryTime);
    }

    private BookingRefundNotifyLogDO buildRepairLog(BookingRefundRepairCandidateDO candidate, String status, Throwable throwable) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("source", "RECONCILE_JOB");
        payload.put("orderId", candidate.getOrderId());
        payload.put("merchantRefundId", candidate.getMerchantRefundId());
        payload.put("payRefundId", candidate.getPayRefundId());
        return new BookingRefundNotifyLogDO()
                .setOrderId(candidate.getOrderId())
                .setMerchantRefundId(StrUtil.blankToDefault(candidate.getMerchantRefundId(), ""))
                .setPayRefundId(candidate.getPayRefundId())
                .setStatus(status)
                .setErrorCode(resolveErrorCode(throwable))
                .setErrorMsg(resolveErrorMsg(throwable))
                .setRawPayload(toRawPayload(payload))
                .setRetryCount(0)
                .setNextRetryTime(StrUtil.equals(status, STATUS_FAIL)
                        ? LocalDateTime.now().plusMinutes(RETRY_BACKOFF_BASE_MINUTES)
                        : null);
    }

    private void assertOrderRefundSynced(Long orderId, Long payRefundId) {
        BookingOrderDO latest = bookingOrderService.getOrder(orderId);
        if (latest == null) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        if (!Objects.equals(BookingOrderStatusEnum.REFUNDED.getStatus(), latest.getStatus())
                || !Objects.equals(payRefundId, latest.getPayRefundId())
                || latest.getRefundTime() == null) {
            throw exception(BOOKING_ORDER_REFUND_STATUS_INVALID);
        }
    }

    private Long resolveOrderId(BookingRefundNotifyLogDO logDO) {
        if (logDO.getOrderId() != null && logDO.getOrderId() > 0) {
            return logDO.getOrderId();
        }
        return parseOrderId(logDO.getMerchantRefundId());
    }

    private Long resolvePayRefundId(BookingRefundNotifyLogDO logDO) {
        if (logDO.getPayRefundId() == null || logDO.getPayRefundId() <= 0) {
            throw exception(BOOKING_ORDER_REFUND_NOT_FOUND);
        }
        return logDO.getPayRefundId();
    }

    private Long parseOrderId(String merchantRefundId) {
        Matcher matcher = REFUND_BIZ_NO_PATTERN.matcher(merchantRefundId == null ? "" : merchantRefundId.trim());
        if (!matcher.matches()) {
            throw exception(BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID);
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw exception(BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID);
        }
    }

    private int resolveRetryBackoffMinutes(int retryCount) {
        int candidate = Math.max(1, retryCount) * RETRY_BACKOFF_BASE_MINUTES;
        return Math.min(candidate, RETRY_BACKOFF_MAX_MINUTES);
    }

    private String resolveErrorCode(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        if (throwable instanceof ServiceException) {
            return String.valueOf(((ServiceException) throwable).getCode());
        }
        return throwable.getClass().getSimpleName();
    }

    private String resolveErrorMsg(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        String message = throwable.getMessage();
        if (StrUtil.isBlank(message)) {
            message = throwable.getClass().getSimpleName();
        }
        return StrUtil.maxLength(message, 512);
    }

    private String toRawPayload(Object payload) {
        if (payload == null) {
            return "";
        }
        try {
            return JsonUtils.toJsonString(payload);
        } catch (Exception ex) {
            return String.valueOf(payload);
        }
    }

    private void persistLogSafely(BookingRefundNotifyLogDO logDO) {
        try {
            refundNotifyLogMapper.insert(logDO);
        } catch (Exception ex) {
            log.error("[persistLogSafely][insert refund notify log failed][orderId={}, merchantRefundId={}, payRefundId={}]",
                    logDO.getOrderId(), logDO.getMerchantRefundId(), logDO.getPayRefundId(), ex);
        }
    }

    private FinanceLogFieldValidator.FinanceLogFields validateFinanceLogFields(String runId, Long orderId,
                                                                                Long payRefundId, String sourceBizNo,
                                                                                String errorCode, String scene) {
        FinanceLogFieldValidator.FinanceLogFields fields = FinanceLogFieldValidator.validate(
                runId, orderId, payRefundId, sourceBizNo, errorCode);
        if (!fields.isComplete()) {
            log.warn("[finance-log-validate][scene={}][missingFields={}]", scene, fields.getMissingFields());
        }
        return fields;
    }
}
