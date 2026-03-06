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
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;
import com.hxy.module.booking.dal.mysql.FourAccountReconcileMapper;
import com.hxy.module.booking.dal.mysql.FourAccountReconcileQueryMapper;
import com.hxy.module.booking.enums.FourAccountReconcileStatusEnum;
import com.hxy.module.booking.service.FourAccountReconcileService;
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
import java.util.Collections;
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
        long unresolvedTicketCount = countUnresolvedTickets(filteredRows, ticketLoadResult.ticketMap);

        FourAccountReconcileSummaryRespVO respVO = new FourAccountReconcileSummaryRespVO();
        respVO.setTotalCount(totalCount);
        respVO.setPassCount(passCount);
        respVO.setWarnCount(warnCount);
        respVO.setTradeMinusFulfillmentSum(tradeMinusFulfillmentSum);
        respVO.setTradeMinusCommissionSplitSum(tradeMinusCommissionSplitSum);
        respVO.setUnresolvedTicketCount(unresolvedTicketCount);
        respVO.setTicketSummaryDegraded(ticketLoadResult.degraded);
        return respVO;
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
        respVO.setUnresolvedTicketCount(0L);
        respVO.setTicketSummaryDegraded(degraded);
        return respVO;
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
            log.warn("[upsertWarnReviewTicket][bizDate({}) sourceBizNo({}) failed]",
                    bizDate, reqDTO.getSourceBizNo(), ex);
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
            log.warn("[resolvePassReviewTicket][bizDate({}) sourceBizNo({}) failed]",
                    bizDate, sourceBizNo, ex);
        }
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
