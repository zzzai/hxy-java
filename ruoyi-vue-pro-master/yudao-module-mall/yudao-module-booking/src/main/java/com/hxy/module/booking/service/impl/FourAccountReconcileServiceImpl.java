package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.trade.api.reviewticket.TradeReviewTicketApi;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketResolveReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryQueryReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryRespDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketUpsertReqDTO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcilePageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundAuditSummaryReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundAuditSummaryRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditPageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditSyncReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditSyncRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;
import com.hxy.module.booking.dal.dataobject.FourAccountRefundCommissionAuditRow;
import com.hxy.module.booking.dal.mysql.FourAccountReconcileMapper;
import com.hxy.module.booking.dal.mysql.FourAccountReconcileQueryMapper;
import com.hxy.module.booking.enums.FourAccountReconcileStatusEnum;
import com.hxy.module.booking.service.FourAccountReconcileService;
import com.hxy.module.booking.service.support.FinanceLogFieldValidator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
@Slf4j
public class FourAccountReconcileServiceImpl implements FourAccountReconcileService {

    private static final DateTimeFormatter RECONCILE_NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DEFAULT_SOURCE = "JOB_DAILY";
    private static final String DEFAULT_OPERATOR = "SYSTEM";
    private static final Integer REVIEW_TICKET_TYPE = 40;
    private static final String REVIEW_TICKET_SOURCE_PREFIX = "FOUR_ACCOUNT_RECONCILE:";
    private static final String REVIEW_TICKET_RULE_CODE = "FOUR_ACCOUNT_RECONCILE_WARN";
    private static final String REVIEW_TICKET_SEVERITY = "P1";
    private static final String REVIEW_TICKET_ACTION_CODE = "FOUR_ACCOUNT_RECONCILE_WARN";
    private static final String REVIEW_TICKET_RESOLVE_ACTION_CODE = "FOUR_ACCOUNT_RECONCILE_PASS";
    private static final String REVIEW_TICKET_RESOLVE_REMARK = "四账对账通过自动收口";
    private static final String MISMATCH_REFUND_WITHOUT_REVERSAL = "REFUND_WITHOUT_REVERSAL";
    private static final String MISMATCH_REVERSAL_WITHOUT_REFUND = "REVERSAL_WITHOUT_REFUND";
    private static final String MISMATCH_REVERSAL_AMOUNT_MISMATCH = "REVERSAL_AMOUNT_MISMATCH";
    private static final List<String> MISMATCH_TYPE_PRIORITY = Arrays.asList(
            MISMATCH_REFUND_WITHOUT_REVERSAL, MISMATCH_REVERSAL_WITHOUT_REFUND, MISMATCH_REVERSAL_AMOUNT_MISMATCH);
    private static final String REFUND_AUDIT_STATUS_PASS = "PASS";
    private static final String REFUND_AUDIT_STATUS_WARN = "WARN";
    private static final String REFUND_AUDIT_EXCEPTION_NONE = "NONE";
    private static final String REFUND_COMMISSION_TICKET_SOURCE_PREFIX = "REFUND_COMMISSION_AUDIT:";
    private static final String REFUND_COMMISSION_RULE_CODE_PREFIX = "REFUND_COMMISSION_AUDIT_";
    private static final String REFUND_COMMISSION_TICKET_ACTION_CODE_PREFIX = "REFUND_COMMISSION_AUDIT_";
    private static final String REFUND_COMMISSION_TICKET_DEFAULT_ESCALATE_TO = "HQ_RISK_FINANCE";
    private static final int REFUND_COMMISSION_TICKET_P0_SLA_MINUTES = 30;
    private static final int REFUND_COMMISSION_TICKET_P1_SLA_MINUTES = 120;

    @Resource
    private FourAccountReconcileMapper reconcileMapper;
    @Resource
    private FourAccountReconcileQueryMapper queryMapper;
    @Resource
    private TradeReviewTicketApi tradeReviewTicketApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long runReconcile(LocalDate bizDate, String source, String operator) {
        LocalDate targetDate = bizDate != null ? bizDate : LocalDate.now().minusDays(1);
        String normalizedSource = normalizeSource(source);
        String normalizedOperator = normalizeOperator(operator);

        LocalDateTime beginTime = targetDate.atStartOfDay();
        LocalDateTime endTime = targetDate.plusDays(1).atStartOfDay();
        int tradeAmount = defaultZero(queryMapper.selectTradeNetAmount(beginTime, endTime));
        int fulfillmentAmount = defaultZero(queryMapper.selectFulfillmentAmount(beginTime, endTime));
        int commissionAmount = defaultZero(queryMapper.selectCommissionAmount(beginTime, endTime));
        int splitAmount = defaultZero(queryMapper.selectSplitAmount(beginTime, endTime));
        int tradeMinusFulfillment = tradeAmount - fulfillmentAmount;
        int tradeMinusCommissionSplit = tradeAmount - (commissionAmount + splitAmount);

        List<String> issueCodes = evaluateIssueCodes(tradeAmount, fulfillmentAmount, commissionAmount, splitAmount);
        int status = issueCodes.isEmpty() ? FourAccountReconcileStatusEnum.PASS.getStatus()
                : FourAccountReconcileStatusEnum.WARN.getStatus();
        String issueCodeText = issueCodes.isEmpty() ? "" : String.join(",", issueCodes);
        String issueDetailJson = buildIssueDetailJson(tradeAmount, fulfillmentAmount, commissionAmount, splitAmount,
                tradeMinusFulfillment, tradeMinusCommissionSplit, issueCodes);
        RefundAuditSnapshot refundAuditSnapshot = buildRefundAuditSnapshot(beginTime, endTime);

        Long reconcileId;
        FourAccountReconcileDO existing = reconcileMapper.selectByBizDate(targetDate);
        if (existing == null) {
            FourAccountReconcileDO createObj = FourAccountReconcileDO.builder()
                    .reconcileNo(generateReconcileNo(targetDate))
                    .bizDate(targetDate)
                    .tradeAmount(tradeAmount)
                    .fulfillmentAmount(fulfillmentAmount)
                    .commissionAmount(commissionAmount)
                    .splitAmount(splitAmount)
                    .tradeMinusFulfillment(tradeMinusFulfillment)
                    .tradeMinusCommissionSplit(tradeMinusCommissionSplit)
                    .status(status)
                    .issueCount(issueCodes.size())
                    .issueCodes(issueCodeText)
                    .issueDetailJson(issueDetailJson)
                    .payRefundId(refundAuditSnapshot.getPayRefundId())
                    .refundTime(refundAuditSnapshot.getRefundTime())
                    .refundLimitSource(refundAuditSnapshot.getRefundLimitSource())
                    .refundExceptionType(refundAuditSnapshot.getRefundExceptionType())
                    .refundAuditStatus(refundAuditSnapshot.getRefundAuditStatus())
                    .refundAuditRemark(refundAuditSnapshot.getRefundAuditRemark())
                    .refundEvidenceJson(refundAuditSnapshot.getRefundEvidenceJson())
                    .source(normalizedSource)
                    .operator(normalizedOperator)
                    .reconciledAt(LocalDateTime.now())
                    .build();
            try {
                reconcileMapper.insert(createObj);
                reconcileId = createObj.getId();
            } catch (DuplicateKeyException ignore) {
                // 并发重复触发按业务日幂等收敛到更新路径
                existing = reconcileMapper.selectByBizDate(targetDate);
                reconcileId = null;
            }
        } else {
            reconcileId = null;
        }
        if (reconcileId == null && existing != null) {
            FourAccountReconcileDO updateObj = FourAccountReconcileDO.builder()
                    .id(existing.getId())
                    .tradeAmount(tradeAmount)
                    .fulfillmentAmount(fulfillmentAmount)
                    .commissionAmount(commissionAmount)
                    .splitAmount(splitAmount)
                    .tradeMinusFulfillment(tradeMinusFulfillment)
                    .tradeMinusCommissionSplit(tradeMinusCommissionSplit)
                    .status(status)
                    .issueCount(issueCodes.size())
                    .issueCodes(issueCodeText)
                    .issueDetailJson(issueDetailJson)
                    .payRefundId(refundAuditSnapshot.getPayRefundId())
                    .refundTime(refundAuditSnapshot.getRefundTime())
                    .refundLimitSource(refundAuditSnapshot.getRefundLimitSource())
                    .refundExceptionType(refundAuditSnapshot.getRefundExceptionType())
                    .refundAuditStatus(refundAuditSnapshot.getRefundAuditStatus())
                    .refundAuditRemark(refundAuditSnapshot.getRefundAuditRemark())
                    .refundEvidenceJson(refundAuditSnapshot.getRefundEvidenceJson())
                    .source(normalizedSource)
                    .operator(normalizedOperator)
                    .reconciledAt(LocalDateTime.now())
                    .build();
            reconcileMapper.updateById(updateObj);
            reconcileId = existing.getId();
        }
        if (reconcileId == null) {
            FourAccountReconcileDO latest = reconcileMapper.selectByBizDate(targetDate);
            if (latest != null) {
                reconcileId = latest.getId();
            }
        }
        if (FourAccountReconcileStatusEnum.WARN.getStatus().equals(status)) {
            upsertWarnReviewTicket(targetDate, issueCodeText, issueDetailJson);
        } else {
            resolvePassReviewTicket(targetDate);
        }
        return reconcileId;
    }

    @Override
    public PageResult<FourAccountReconcileDO> getReconcilePage(FourAccountReconcilePageReqVO reqVO) {
        normalizePageReq(reqVO);
        return reconcileMapper.selectPage(reqVO);
    }

    @Override
    public FourAccountReconcileDO getReconcile(Long id) {
        if (id == null) {
            return null;
        }
        return reconcileMapper.selectById(id);
    }

    @Override
    public FourAccountReconcileSummaryRespVO getReconcileSummary(FourAccountReconcileSummaryReqVO reqVO) {
        FourAccountReconcileSummaryReqVO safeReq = reqVO == null ? new FourAccountReconcileSummaryReqVO() : reqVO;
        List<FourAccountReconcileDO> allRows = reconcileMapper.selectSummaryList(safeReq);
        if (allRows == null || allRows.isEmpty()) {
            return emptySummary(false);
        }

        TicketSummaryLoadResult ticketLoadResult = loadTicketSummaryMap(allRows);
        List<FourAccountReconcileDO> filteredRows = filterSummaryRowsByTicketLinked(allRows, safeReq.getRelatedTicketLinked(),
                ticketLoadResult.ticketMap, ticketLoadResult.degraded);

        long totalCount = filteredRows.size();
        long passCount = filteredRows.stream()
                .filter(row -> Objects.equals(row.getStatus(), FourAccountReconcileStatusEnum.PASS.getStatus()))
                .count();
        long warnCount = filteredRows.stream()
                .filter(row -> Objects.equals(row.getStatus(), FourAccountReconcileStatusEnum.WARN.getStatus()))
                .count();
        long tradeMinusFulfillmentSum = filteredRows.stream()
                .map(FourAccountReconcileDO::getTradeMinusFulfillment)
                .mapToLong(this::defaultZeroLong)
                .sum();
        long tradeMinusCommissionSplitSum = filteredRows.stream()
                .map(FourAccountReconcileDO::getTradeMinusCommissionSplit)
                .mapToLong(this::defaultZeroLong)
                .sum();
        long commissionAmountSum = filteredRows.stream()
                .map(FourAccountReconcileDO::getCommissionAmount)
                .mapToLong(this::defaultZeroLong)
                .sum();
        long commissionDifferenceAbsSum = filteredRows.stream()
                .map(FourAccountReconcileDO::getTradeMinusCommissionSplit)
                .mapToLong(this::absoluteZeroLong)
                .sum();
        long unresolvedTicketCount = countUnresolvedTickets(filteredRows, ticketLoadResult.ticketMap);

        FourAccountReconcileSummaryRespVO respVO = new FourAccountReconcileSummaryRespVO();
        respVO.setTotalCount(totalCount);
        respVO.setPassCount(passCount);
        respVO.setWarnCount(warnCount);
        respVO.setTradeMinusFulfillmentSum(tradeMinusFulfillmentSum);
        respVO.setTradeMinusCommissionSplitSum(tradeMinusCommissionSplitSum);
        respVO.setCommissionAmountSum(commissionAmountSum);
        respVO.setCommissionDifferenceAbsSum(commissionDifferenceAbsSum);
        respVO.setUnresolvedTicketCount(unresolvedTicketCount);
        respVO.setTicketSummaryDegraded(ticketLoadResult.degraded);
        return respVO;
    }

    @Override
    public FourAccountRefundAuditSummaryRespVO getRefundAuditSummary(FourAccountRefundAuditSummaryReqVO reqVO) {
        FourAccountRefundAuditSummaryReqVO safeReq = reqVO == null ? new FourAccountRefundAuditSummaryReqVO() : reqVO;
        normalizeRefundAuditSummaryReq(safeReq);
        List<FourAccountReconcileDO> allRows = reconcileMapper.selectRefundAuditSummaryList(safeReq);
        if (allRows == null || allRows.isEmpty()) {
            return emptyRefundAuditSummary(false);
        }
        TicketSummaryLoadResult ticketLoadResult = loadTicketSummaryMap(allRows);
        List<FourAccountReconcileDO> filteredRows = filterSummaryRowsByTicketLinked(allRows, safeReq.getRelatedTicketLinked(),
                ticketLoadResult.ticketMap, ticketLoadResult.degraded);
        if (filteredRows.isEmpty()) {
            return emptyRefundAuditSummary(ticketLoadResult.degraded);
        }

        long totalCount = filteredRows.size();
        long differenceAmountSum = filteredRows.stream()
                .map(FourAccountReconcileDO::getTradeMinusCommissionSplit)
                .mapToLong(this::absoluteZeroLong)
                .sum();
        long unresolvedTicketCount = countUnresolvedTickets(filteredRows, ticketLoadResult.ticketMap);
        List<FourAccountRefundAuditSummaryRespVO.CountItem> statusAgg = toCountItems(filteredRows.stream()
                .collect(Collectors.groupingBy(this::resolveRefundAuditStatus, LinkedHashMap::new, Collectors.counting())));
        List<FourAccountRefundAuditSummaryRespVO.CountItem> exceptionTypeAgg = toCountItems(filteredRows.stream()
                .map(this::resolveRefundExceptionTypeKeys)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting())));

        FourAccountRefundAuditSummaryRespVO respVO = new FourAccountRefundAuditSummaryRespVO();
        respVO.setTotalCount(totalCount);
        respVO.setDifferenceAmountSum(differenceAmountSum);
        respVO.setUnresolvedTicketCount(unresolvedTicketCount);
        respVO.setTicketSummaryDegraded(ticketLoadResult.degraded);
        respVO.setStatusAgg(statusAgg);
        respVO.setExceptionTypeAgg(exceptionTypeAgg);
        return respVO;
    }

    @Override
    public PageResult<FourAccountRefundCommissionAuditRespVO> getRefundCommissionAuditPage(
            FourAccountRefundCommissionAuditPageReqVO reqVO) {
        FourAccountRefundCommissionAuditPageReqVO safeReq = reqVO == null
                ? new FourAccountRefundCommissionAuditPageReqVO() : reqVO;
        normalizeRefundCommissionAuditReq(safeReq);
        LocalDateTime beginTime = resolveAuditBeginTime(safeReq.getBeginBizDate(), safeReq.getEndBizDate());
        LocalDateTime endTime = resolveAuditEndTime(safeReq.getBeginBizDate(), safeReq.getEndBizDate());
        if (endTime.isBefore(beginTime)) {
            LocalDateTime swap = beginTime;
            beginTime = endTime;
            endTime = swap;
        }
        List<FourAccountRefundCommissionAuditRow> candidateRows =
                queryMapper.selectRefundCommissionAuditCandidates(beginTime, endTime);
        if (candidateRows == null || candidateRows.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<FourAccountRefundCommissionAuditRespVO> mismatchRows = candidateRows.stream()
                .map(this::buildAuditMismatchRow)
                .filter(Objects::nonNull)
                .filter(row -> filterByRequest(row, safeReq))
                .collect(Collectors.toList());
        if (mismatchRows.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        int pageNo = safeReq.getPageNo() == null ? 1 : Math.max(safeReq.getPageNo(), 1);
        int pageSize = safeReq.getPageSize() == null ? 10 : Math.max(safeReq.getPageSize(), 1);
        int fromIndex = (pageNo - 1) * pageSize;
        if (fromIndex >= mismatchRows.size()) {
            return new PageResult<>(Collections.emptyList(), (long) mismatchRows.size());
        }
        int toIndex = Math.min(fromIndex + pageSize, mismatchRows.size());
        List<FourAccountRefundCommissionAuditRespVO> pageList = mismatchRows.subList(fromIndex, toIndex);
        return new PageResult<>(pageList, (long) mismatchRows.size());
    }

    @Override
    public FourAccountRefundCommissionAuditSyncRespVO syncRefundCommissionAuditTickets(
            FourAccountRefundCommissionAuditSyncReqVO reqVO) {
        FourAccountRefundCommissionAuditSyncReqVO safeReq = reqVO == null
                ? new FourAccountRefundCommissionAuditSyncReqVO() : reqVO;
        normalizeRefundCommissionAuditSyncReq(safeReq);
        LocalDateTime beginTime = resolveAuditBeginTime(safeReq.getBeginBizDate(), safeReq.getEndBizDate());
        LocalDateTime endTime = resolveAuditEndTime(safeReq.getBeginBizDate(), safeReq.getEndBizDate());
        if (endTime.isBefore(beginTime)) {
            LocalDateTime swap = beginTime;
            beginTime = endTime;
            endTime = swap;
        }
        List<FourAccountRefundCommissionAuditRow> candidateRows =
                queryMapper.selectRefundCommissionAuditCandidates(beginTime, endTime);
        if (candidateRows == null || candidateRows.isEmpty()) {
            return buildSyncResp(0, 0, 0, Collections.emptyList());
        }
        List<FourAccountRefundCommissionAuditRespVO> mismatchRows = candidateRows.stream()
                .map(this::buildAuditMismatchRow)
                .filter(Objects::nonNull)
                .filter(row -> filterBySyncRequest(row, safeReq))
                .sorted(Comparator.comparing(FourAccountRefundCommissionAuditRespVO::getPayTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FourAccountRefundCommissionAuditRespVO::getOrderId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        if (mismatchRows.isEmpty()) {
            return buildSyncResp(0, 0, 0, Collections.emptyList());
        }
        int limit = safeReq.getLimit() == null ? 200 : safeReq.getLimit();
        int attemptedCount = Math.min(limit, mismatchRows.size());
        int successCount = 0;
        List<Long> failedOrderIds = new ArrayList<>();
        for (int i = 0; i < attemptedCount; i++) {
            FourAccountRefundCommissionAuditRespVO row = mismatchRows.get(i);
            try {
                tradeReviewTicketApi.upsertReviewTicket(buildRefundCommissionAuditTicketReq(row));
                successCount++;
            } catch (Exception ex) {
                failedOrderIds.add(row.getOrderId());
                String sourceBizNo = REFUND_COMMISSION_TICKET_SOURCE_PREFIX + row.getOrderId();
                FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                        "NO_RUN", row.getOrderId(), row.getPayRefundId(), sourceBizNo, resolveErrorCode(ex),
                        "four_account_refund_commission_sync_tickets");
                log.warn("[syncRefundCommissionAuditTickets][upsert ticket fail, orderId={}, mismatchType={}]",
                        row.getOrderId(), row.getMismatchType(), ex);
                log.warn("[finance-audit][scene=four_account_refund_commission_sync_ticket_fail][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                        fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                        fields.getSourceBizNo(), fields.getErrorCode());
            }
        }
        return buildSyncResp(mismatchRows.size(), attemptedCount, successCount, failedOrderIds);
    }

    private void normalizePageReq(FourAccountReconcilePageReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        if (StringUtils.hasText(reqVO.getSource())) {
            reqVO.setSource(reqVO.getSource().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(reqVO.getIssueCode())) {
            reqVO.setIssueCode(reqVO.getIssueCode().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(reqVO.getRefundAuditStatus())) {
            reqVO.setRefundAuditStatus(reqVO.getRefundAuditStatus().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(reqVO.getRefundExceptionType())) {
            reqVO.setRefundExceptionType(reqVO.getRefundExceptionType().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(reqVO.getRefundLimitSource())) {
            reqVO.setRefundLimitSource(reqVO.getRefundLimitSource().trim().toUpperCase(Locale.ROOT));
        }
    }

    private void normalizeRefundAuditSummaryReq(FourAccountRefundAuditSummaryReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        if (StringUtils.hasText(reqVO.getRefundAuditStatus())) {
            reqVO.setRefundAuditStatus(reqVO.getRefundAuditStatus().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(reqVO.getRefundExceptionType())) {
            reqVO.setRefundExceptionType(reqVO.getRefundExceptionType().trim().toUpperCase(Locale.ROOT));
        }
    }

    private void normalizeRefundCommissionAuditReq(FourAccountRefundCommissionAuditPageReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        if (StringUtils.hasText(reqVO.getMismatchType())) {
            reqVO.setMismatchType(reqVO.getMismatchType().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            reqVO.setKeyword(reqVO.getKeyword().trim());
        }
    }

    private void normalizeRefundCommissionAuditSyncReq(FourAccountRefundCommissionAuditSyncReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        if (StringUtils.hasText(reqVO.getMismatchType())) {
            reqVO.setMismatchType(reqVO.getMismatchType().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            reqVO.setKeyword(reqVO.getKeyword().trim());
        }
    }

    private LocalDateTime resolveAuditBeginTime(LocalDate beginBizDate, LocalDate endBizDate) {
        if (beginBizDate != null) {
            return beginBizDate.atStartOfDay();
        }
        if (endBizDate != null) {
            return endBizDate.minusDays(6).atStartOfDay();
        }
        return LocalDate.now().minusDays(7).atStartOfDay();
    }

    private LocalDateTime resolveAuditEndTime(LocalDate beginBizDate, LocalDate endBizDate) {
        if (endBizDate != null) {
            return endBizDate.plusDays(1).atStartOfDay();
        }
        if (beginBizDate != null) {
            return beginBizDate.plusDays(7).atStartOfDay();
        }
        return LocalDate.now().plusDays(1).atStartOfDay();
    }

    private FourAccountRefundCommissionAuditRespVO buildAuditMismatchRow(FourAccountRefundCommissionAuditRow row) {
        if (row == null) {
            return null;
        }
        int refundPrice = defaultZero(row.getRefundPrice());
        int settledAmount = defaultZero(row.getSettledCommissionAmount());
        int reversalAmount = defaultZero(row.getReversalCommissionAmountAbs());
        boolean refunded = refundPrice > 0;
        String mismatchType = null;
        String mismatchReason = null;
        if (refunded && settledAmount > 0 && reversalAmount == 0) {
            mismatchType = MISMATCH_REFUND_WITHOUT_REVERSAL;
            mismatchReason = "订单已退款且存在已结算正向提成，但未找到有效冲正记录";
        } else if (!refunded && reversalAmount > 0) {
            mismatchType = MISMATCH_REVERSAL_WITHOUT_REFUND;
            mismatchReason = "订单未退款，但存在有效冲正提成记录";
        } else if (refunded && settledAmount > 0 && reversalAmount > 0 && reversalAmount != settledAmount) {
            mismatchType = MISMATCH_REVERSAL_AMOUNT_MISMATCH;
            mismatchReason = "订单已退款，冲正金额与已结算提成金额不一致";
        }
        if (mismatchType == null) {
            return null;
        }
        FourAccountRefundCommissionAuditRespVO respVO = new FourAccountRefundCommissionAuditRespVO();
        respVO.setOrderId(row.getOrderId());
        respVO.setTradeOrderNo(row.getTradeOrderNo());
        respVO.setUserId(row.getUserId());
        respVO.setPayTime(row.getPayTime());
        respVO.setRefundPrice(refundPrice);
        respVO.setSettledCommissionAmount(settledAmount);
        respVO.setReversalCommissionAmountAbs(reversalAmount);
        respVO.setActiveCommissionAmount(defaultZero(row.getActiveCommissionAmount()));
        respVO.setExpectedReversalAmount(refunded ? settledAmount : 0);
        respVO.setPayRefundId(row.getPayRefundId());
        respVO.setRefundTime(row.getRefundTime());
        respVO.setRefundLimitSource(normalizeUpper(row.getRefundLimitSource()));
        respVO.setRefundEvidenceJson(row.getRefundLimitDetailJson());
        respVO.setMismatchType(mismatchType);
        respVO.setMismatchReason(mismatchReason);
        return respVO;
    }

    private boolean filterByRequest(FourAccountRefundCommissionAuditRespVO row,
                                    FourAccountRefundCommissionAuditPageReqVO reqVO) {
        if (row == null) {
            return false;
        }
        if (reqVO == null) {
            return true;
        }
        if (reqVO.getOrderId() != null && !Objects.equals(reqVO.getOrderId(), row.getOrderId())) {
            return false;
        }
        if (StringUtils.hasText(reqVO.getMismatchType())
                && !reqVO.getMismatchType().equalsIgnoreCase(row.getMismatchType())) {
            return false;
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String orderNo = row.getTradeOrderNo();
            return orderNo != null && orderNo.contains(reqVO.getKeyword());
        }
        return true;
    }

    private boolean filterBySyncRequest(FourAccountRefundCommissionAuditRespVO row,
                                        FourAccountRefundCommissionAuditSyncReqVO reqVO) {
        if (row == null) {
            return false;
        }
        if (reqVO == null) {
            return true;
        }
        if (reqVO.getOrderId() != null && !Objects.equals(reqVO.getOrderId(), row.getOrderId())) {
            return false;
        }
        if (StringUtils.hasText(reqVO.getMismatchType())
                && !reqVO.getMismatchType().equalsIgnoreCase(row.getMismatchType())) {
            return false;
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String orderNo = row.getTradeOrderNo();
            return orderNo != null && orderNo.contains(reqVO.getKeyword());
        }
        return true;
    }

    private TradeReviewTicketUpsertReqDTO buildRefundCommissionAuditTicketReq(
            FourAccountRefundCommissionAuditRespVO row) {
        String mismatchType = row.getMismatchType() == null ? "" : row.getMismatchType().trim().toUpperCase(Locale.ROOT);
        boolean highRisk = MISMATCH_REFUND_WITHOUT_REVERSAL.equals(mismatchType)
                || MISMATCH_REVERSAL_WITHOUT_REFUND.equals(mismatchType);
        String severity = highRisk ? "P0" : "P1";
        int slaMinutes = highRisk ? REFUND_COMMISSION_TICKET_P0_SLA_MINUTES : REFUND_COMMISSION_TICKET_P1_SLA_MINUTES;
        return new TradeReviewTicketUpsertReqDTO()
                .setTicketType(REVIEW_TICKET_TYPE)
                .setOrderId(row.getOrderId())
                .setUserId(row.getUserId())
                .setSourceBizNo(REFUND_COMMISSION_TICKET_SOURCE_PREFIX + row.getOrderId())
                .setRuleCode(REFUND_COMMISSION_RULE_CODE_PREFIX + mismatchType)
                .setDecisionReason(row.getMismatchReason())
                .setSeverity(severity)
                .setEscalateTo(REFUND_COMMISSION_TICKET_DEFAULT_ESCALATE_TO)
                .setSlaMinutes(slaMinutes)
                .setRemark(buildRefundCommissionTicketRemark(row))
                .setActionCode(REFUND_COMMISSION_TICKET_ACTION_CODE_PREFIX + mismatchType);
    }

    private String buildRefundCommissionTicketRemark(FourAccountRefundCommissionAuditRespVO row) {
        String tradeOrderNo = row.getTradeOrderNo() == null ? "-" : row.getTradeOrderNo();
        int refundPrice = defaultZero(row.getRefundPrice());
        int settled = defaultZero(row.getSettledCommissionAmount());
        int reversal = defaultZero(row.getReversalCommissionAmountAbs());
        int expected = defaultZero(row.getExpectedReversalAmount());
        return String.format(Locale.ROOT,
                "orderNo=%s,refund=%d,settled=%d,reversal=%d,expected=%d,reason=%s",
                tradeOrderNo, refundPrice, settled, reversal, expected,
                row.getMismatchReason() == null ? "-" : row.getMismatchReason());
    }

    private FourAccountRefundCommissionAuditSyncRespVO buildSyncResp(int totalMismatchCount,
                                                                     int attemptedCount,
                                                                     int successCount,
                                                                     List<Long> failedOrderIds) {
        FourAccountRefundCommissionAuditSyncRespVO respVO = new FourAccountRefundCommissionAuditSyncRespVO();
        respVO.setTotalMismatchCount(totalMismatchCount);
        respVO.setAttemptedCount(attemptedCount);
        respVO.setSuccessCount(successCount);
        respVO.setFailedCount(Math.max(attemptedCount - successCount, 0));
        respVO.setFailedOrderIds(failedOrderIds == null ? Collections.emptyList() : failedOrderIds);
        return respVO;
    }

    private TicketSummaryLoadResult loadTicketSummaryMap(List<FourAccountReconcileDO> reconcileRows) {
        List<String> sourceBizNos = reconcileRows.stream()
                .filter(Objects::nonNull)
                .map(FourAccountReconcileDO::getBizDate)
                .filter(Objects::nonNull)
                .map(this::buildReviewTicketSourceBizNo)
                .distinct()
                .collect(Collectors.toList());
        if (sourceBizNos.isEmpty()) {
            return new TicketSummaryLoadResult(Collections.emptyMap(), false);
        }
        try {
            List<TradeReviewTicketSummaryRespDTO> summaryList = tradeReviewTicketApi.listLatestTicketSummaryBySourceBizNos(
                    new TradeReviewTicketSummaryQueryReqDTO()
                            .setTicketType(REVIEW_TICKET_TYPE)
                            .setSourceBizNos(sourceBizNos));
            Map<String, TradeReviewTicketSummaryRespDTO> ticketMap = summaryList == null ? Collections.emptyMap()
                    : summaryList.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> StrUtil.isNotBlank(item.getSourceBizNo()))
                    .collect(Collectors.toMap(TradeReviewTicketSummaryRespDTO::getSourceBizNo, Function.identity(),
                            (left, right) -> left));
            return new TicketSummaryLoadResult(ticketMap, false);
        } catch (Exception ex) {
            log.warn("[getReconcileSummary][load ticket summary degrade]", ex);
            return new TicketSummaryLoadResult(Collections.emptyMap(), true);
        }
    }

    private List<FourAccountReconcileDO> filterSummaryRowsByTicketLinked(List<FourAccountReconcileDO> rows,
                                                                          Boolean relatedTicketLinked,
                                                                          Map<String, TradeReviewTicketSummaryRespDTO> ticketMap,
                                                                          boolean ticketSummaryDegraded) {
        if (relatedTicketLinked == null || ticketSummaryDegraded) {
            return rows;
        }
        return rows.stream().filter(row -> {
            String sourceBizNo = buildReviewTicketSourceBizNo(row.getBizDate());
            boolean linked = sourceBizNo != null && ticketMap.containsKey(sourceBizNo);
            return Objects.equals(linked, relatedTicketLinked);
        }).collect(Collectors.toList());
    }

    private long countUnresolvedTickets(List<FourAccountReconcileDO> rows,
                                        Map<String, TradeReviewTicketSummaryRespDTO> ticketMap) {
        if (rows == null || rows.isEmpty() || ticketMap == null || ticketMap.isEmpty()) {
            return 0L;
        }
        return rows.stream()
                .map(FourAccountReconcileDO::getBizDate)
                .filter(Objects::nonNull)
                .map(this::buildReviewTicketSourceBizNo)
                .map(ticketMap::get)
                .filter(Objects::nonNull)
                .filter(ticket -> AfterSaleReviewTicketStatusEnum.isPending(ticket.getStatus()))
                .count();
    }

    private long defaultZeroLong(Integer value) {
        return value == null ? 0L : value;
    }

    private String buildReviewTicketSourceBizNo(LocalDate bizDate) {
        return bizDate == null ? null : REVIEW_TICKET_SOURCE_PREFIX + bizDate;
    }

    private FourAccountReconcileSummaryRespVO emptySummary(boolean degraded) {
        FourAccountReconcileSummaryRespVO respVO = new FourAccountReconcileSummaryRespVO();
        respVO.setTotalCount(0L);
        respVO.setPassCount(0L);
        respVO.setWarnCount(0L);
        respVO.setTradeMinusFulfillmentSum(0L);
        respVO.setTradeMinusCommissionSplitSum(0L);
        respVO.setCommissionAmountSum(0L);
        respVO.setCommissionDifferenceAbsSum(0L);
        respVO.setUnresolvedTicketCount(0L);
        respVO.setTicketSummaryDegraded(degraded);
        return respVO;
    }

    private FourAccountRefundAuditSummaryRespVO emptyRefundAuditSummary(boolean degraded) {
        FourAccountRefundAuditSummaryRespVO respVO = new FourAccountRefundAuditSummaryRespVO();
        respVO.setTotalCount(0L);
        respVO.setDifferenceAmountSum(0L);
        respVO.setUnresolvedTicketCount(0L);
        respVO.setTicketSummaryDegraded(degraded);
        respVO.setStatusAgg(Collections.emptyList());
        respVO.setExceptionTypeAgg(Collections.emptyList());
        return respVO;
    }

    private RefundAuditSnapshot buildRefundAuditSnapshot(LocalDateTime beginTime, LocalDateTime endTime) {
        List<FourAccountRefundCommissionAuditRow> candidateRows =
                queryMapper.selectRefundCommissionAuditCandidates(beginTime, endTime);
        if (candidateRows == null || candidateRows.isEmpty()) {
            return RefundAuditSnapshot.pass("refund-audit mismatch=0", buildRefundEvidence(0, Collections.emptyList(), null));
        }
        List<FourAccountRefundCommissionAuditRespVO> mismatchRows = candidateRows.stream()
                .map(this::buildAuditMismatchRow)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (mismatchRows.isEmpty()) {
            return RefundAuditSnapshot.pass("refund-audit mismatch=0",
                    buildRefundEvidence(candidateRows.size(), Collections.emptyList(), null));
        }
        FourAccountRefundCommissionAuditRespVO topMismatch = mismatchRows.stream()
                .sorted(Comparator.comparingInt((FourAccountRefundCommissionAuditRespVO item) ->
                                mismatchTypePriority(item.getMismatchType()))
                        .thenComparing(FourAccountRefundCommissionAuditRespVO::getPayTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FourAccountRefundCommissionAuditRespVO::getOrderId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
        String remark = String.format(Locale.ROOT, "refund-audit mismatch=%d", mismatchRows.size());
        return RefundAuditSnapshot.warn(topMismatch == null ? null : topMismatch.getPayRefundId(),
                topMismatch == null ? null : topMismatch.getRefundTime(),
                topMismatch == null ? null : topMismatch.getRefundLimitSource(),
                topMismatch == null ? null : topMismatch.getMismatchType(),
                remark,
                buildRefundEvidence(candidateRows.size(), mismatchRows, topMismatch));
    }

    private String buildRefundEvidence(int candidateCount,
                                       List<FourAccountRefundCommissionAuditRespVO> mismatchRows,
                                       FourAccountRefundCommissionAuditRespVO topMismatch) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("candidateCount", candidateCount);
        payload.put("mismatchCount", mismatchRows == null ? 0 : mismatchRows.size());
        Map<String, Long> mismatchTypeAgg = mismatchRows == null ? Collections.emptyMap()
                : mismatchRows.stream()
                .map(FourAccountRefundCommissionAuditRespVO::getMismatchType)
                .map(this::normalizeUpper)
                .map(value -> value == null ? REFUND_AUDIT_EXCEPTION_NONE : value)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        payload.put("mismatchTypeAgg", mismatchTypeAgg);
        payload.put("sampleOrderIds", mismatchRows == null ? Collections.emptyList() : mismatchRows.stream()
                .map(FourAccountRefundCommissionAuditRespVO::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .limit(10)
                .collect(Collectors.toList()));
        if (topMismatch != null) {
            Map<String, Object> top = new LinkedHashMap<>();
            top.put("orderId", topMismatch.getOrderId());
            top.put("tradeOrderNo", topMismatch.getTradeOrderNo());
            top.put("mismatchType", topMismatch.getMismatchType());
            top.put("mismatchReason", topMismatch.getMismatchReason());
            top.put("refundPrice", topMismatch.getRefundPrice());
            top.put("settledCommissionAmount", topMismatch.getSettledCommissionAmount());
            top.put("reversalCommissionAmountAbs", topMismatch.getReversalCommissionAmountAbs());
            top.put("expectedReversalAmount", topMismatch.getExpectedReversalAmount());
            top.put("payRefundId", topMismatch.getPayRefundId());
            top.put("refundTime", topMismatch.getRefundTime());
            top.put("refundLimitSource", topMismatch.getRefundLimitSource());
            top.put("refundEvidenceJson", topMismatch.getRefundEvidenceJson());
            payload.put("topMismatch", top);
        }
        return JsonUtils.toJsonString(payload);
    }

    private int mismatchTypePriority(String mismatchType) {
        String normalized = normalizeUpper(mismatchType);
        int index = MISMATCH_TYPE_PRIORITY.indexOf(normalized);
        return index >= 0 ? index : MISMATCH_TYPE_PRIORITY.size();
    }

    private long absoluteZeroLong(Integer value) {
        return value == null ? 0L : Math.abs((long) value);
    }

    private List<FourAccountRefundAuditSummaryRespVO.CountItem> toCountItems(Map<String, Long> data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        return data.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new FourAccountRefundAuditSummaryRespVO.CountItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private String resolveRefundAuditStatus(FourAccountReconcileDO row) {
        if (row == null) {
            return REFUND_AUDIT_STATUS_PASS;
        }
        String status = normalizeUpper(row.getRefundAuditStatus());
        if (status != null) {
            return status;
        }
        return StringUtils.hasText(row.getRefundExceptionType()) ? REFUND_AUDIT_STATUS_WARN : REFUND_AUDIT_STATUS_PASS;
    }

    private List<String> resolveRefundExceptionTypeKeys(FourAccountReconcileDO row) {
        if (row == null || !StringUtils.hasText(row.getRefundExceptionType())) {
            return Collections.singletonList(REFUND_AUDIT_EXCEPTION_NONE);
        }
        return Arrays.stream(row.getRefundExceptionType().split(","))
                .map(this::normalizeUpper)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        list -> list.isEmpty() ? Collections.singletonList(REFUND_AUDIT_EXCEPTION_NONE) : list));
    }

    private String normalizeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> evaluateIssueCodes(int tradeAmount, int fulfillmentAmount, int commissionAmount, int splitAmount) {
        List<String> issueCodes = new ArrayList<>();
        if (fulfillmentAmount > tradeAmount) {
            issueCodes.add("FULFILLMENT_GT_TRADE");
        }
        if (commissionAmount > fulfillmentAmount) {
            issueCodes.add("COMMISSION_GT_FULFILLMENT");
        }
        if (splitAmount > tradeAmount) {
            issueCodes.add("SPLIT_GT_TRADE");
        }
        if (commissionAmount + splitAmount > tradeAmount) {
            issueCodes.add("COMMISSION_SPLIT_GT_TRADE");
        }
        return issueCodes;
    }

    private String buildIssueDetailJson(int tradeAmount, int fulfillmentAmount, int commissionAmount, int splitAmount,
                                        int tradeMinusFulfillment, int tradeMinusCommissionSplit,
                                        List<String> issueCodes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tradeAmount", tradeAmount);
        payload.put("fulfillmentAmount", fulfillmentAmount);
        payload.put("commissionAmount", commissionAmount);
        payload.put("splitAmount", splitAmount);
        payload.put("tradeMinusFulfillment", tradeMinusFulfillment);
        payload.put("tradeMinusCommissionSplit", tradeMinusCommissionSplit);
        payload.put("issues", issueCodes);
        return JsonUtils.toJsonString(payload);
    }

    private String normalizeSource(String source) {
        if (!StringUtils.hasText(source)) {
            return DEFAULT_SOURCE;
        }
        return source.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOperator(String operator) {
        if (!StringUtils.hasText(operator)) {
            return DEFAULT_OPERATOR;
        }
        return operator.trim();
    }

    private String generateReconcileNo(LocalDate bizDate) {
        return "FAR" + bizDate.format(RECONCILE_NO_DATE_FORMAT) + RandomUtil.randomStringUpper(6);
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    @Data
    @AllArgsConstructor
    private static class RefundAuditSnapshot {
        private Long payRefundId;
        private LocalDateTime refundTime;
        private String refundLimitSource;
        private String refundExceptionType;
        private String refundAuditStatus;
        private String refundAuditRemark;
        private String refundEvidenceJson;

        private static RefundAuditSnapshot pass(String remark, String evidenceJson) {
            return new RefundAuditSnapshot(null, null, null, null, REFUND_AUDIT_STATUS_PASS, remark, evidenceJson);
        }

        private static RefundAuditSnapshot warn(Long payRefundId, LocalDateTime refundTime, String refundLimitSource,
                                                String refundExceptionType, String remark, String evidenceJson) {
            return new RefundAuditSnapshot(payRefundId, refundTime, refundLimitSource, refundExceptionType,
                    REFUND_AUDIT_STATUS_WARN, remark, evidenceJson);
        }
    }

    private void upsertWarnReviewTicket(LocalDate bizDate, String issueCodeText, String issueDetailJson) {
        if (bizDate == null) {
            return;
        }
        TradeReviewTicketUpsertReqDTO reqDTO = new TradeReviewTicketUpsertReqDTO()
                .setTicketType(REVIEW_TICKET_TYPE)
                .setSourceBizNo(REVIEW_TICKET_SOURCE_PREFIX + bizDate)
                .setRuleCode(REVIEW_TICKET_RULE_CODE)
                .setDecisionReason(buildWarnDecisionReason(issueCodeText))
                .setSeverity(REVIEW_TICKET_SEVERITY)
                .setRemark(issueDetailJson)
                .setActionCode(REVIEW_TICKET_ACTION_CODE);
        try {
            tradeReviewTicketApi.upsertReviewTicket(reqDTO);
        } catch (Exception ex) {
            FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                    "NO_RUN", null, null, reqDTO.getSourceBizNo(), resolveErrorCode(ex), "four_account_warn_upsert");
            log.warn("[upsertWarnReviewTicket][bizDate({}) sourceBizNo({}) failed]",
                    bizDate, reqDTO.getSourceBizNo(), ex);
            log.warn("[finance-audit][scene=four_account_warn_ticket_upsert_fail][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                    fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                    fields.getSourceBizNo(), fields.getErrorCode());
        }
    }

    private String buildWarnDecisionReason(String issueCodeText) {
        String reason = "四账差异告警";
        if (StringUtils.hasText(issueCodeText)) {
            reason = reason + ":" + issueCodeText;
        }
        return StrUtil.maxLength(reason, 500);
    }

    private void resolvePassReviewTicket(LocalDate bizDate) {
        if (bizDate == null) {
            return;
        }
        String sourceBizNo = REVIEW_TICKET_SOURCE_PREFIX + bizDate;
        TradeReviewTicketResolveReqDTO reqDTO = new TradeReviewTicketResolveReqDTO()
                .setTicketType(REVIEW_TICKET_TYPE)
                .setSourceBizNo(sourceBizNo)
                .setResolveActionCode(REVIEW_TICKET_RESOLVE_ACTION_CODE)
                .setResolveBizNo(sourceBizNo)
                .setResolveRemark(REVIEW_TICKET_RESOLVE_REMARK);
        try {
            tradeReviewTicketApi.resolveReviewTicketBySourceBizNo(reqDTO);
        } catch (Exception ex) {
            FinanceLogFieldValidator.FinanceLogFields fields = validateFinanceLogFields(
                    "NO_RUN", null, null, sourceBizNo, resolveErrorCode(ex), "four_account_pass_resolve");
            log.warn("[resolvePassReviewTicket][bizDate({}) sourceBizNo({}) failed]",
                    bizDate, sourceBizNo, ex);
            log.warn("[finance-audit][scene=four_account_pass_ticket_resolve_fail][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                    fields.getRunId(), fields.getOrderId(), fields.getPayRefundId(),
                    fields.getSourceBizNo(), fields.getErrorCode());
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

    private String resolveErrorCode(Exception ex) {
        if (ex == null) {
            return "UNKNOWN";
        }
        return ex.getClass().getSimpleName();
    }

    private static final class TicketSummaryLoadResult {
        private final Map<String, TradeReviewTicketSummaryRespDTO> ticketMap;
        private final boolean degraded;

        private TicketSummaryLoadResult(Map<String, TradeReviewTicketSummaryRespDTO> ticketMap, boolean degraded) {
            this.ticketMap = ticketMap;
            this.degraded = degraded;
        }
    }
}
