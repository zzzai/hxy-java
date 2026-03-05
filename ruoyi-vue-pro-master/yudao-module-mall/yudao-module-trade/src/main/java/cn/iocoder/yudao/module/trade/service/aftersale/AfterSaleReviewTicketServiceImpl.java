package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import cn.iocoder.yudao.module.trade.api.ticketsla.TradeTicketSlaRuleApi;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchReqDTO;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchRespDTO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketNotifyOutboxPageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketNotifyOutboxDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketNotifyOutboxMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleMatchLevelEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleTicketTypeEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;
import cn.iocoder.yudao.module.trade.service.aftersale.dto.AfterSaleReviewTicketBatchResolveResult;
import cn.iocoder.yudao.module.trade.service.aftersale.dto.AfterSaleReviewTicketNotifyBatchRetryResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_REVIEW_TICKET_NOT_FOUND;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_REVIEW_TICKET_STATUS_NOT_PENDING;

/**
 * 售后人工复核工单服务实现
 *
 * @author HXY
 */
@Service
@Validated
@Slf4j
public class AfterSaleReviewTicketServiceImpl implements AfterSaleReviewTicketService {

    private static final int DEFAULT_ESCALATE_BATCH_LIMIT = 200;
    private static final int MAX_ESCALATE_BATCH_LIMIT = 5000;
    private static final DateTimeFormatter ESCALATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_RESOLVE_ACTION_MANUAL = "MANUAL_RESOLVE";
    private static final String DEFAULT_RESOLVE_ACTION_AUTO = "AUTO_RESOLVE";
    private static final String ACTION_TICKET_CREATE = "TICKET_CREATE";
    private static final String ACTION_RULE_RETRIGGER = "RULE_RETRIGGER";
    private static final String ACTION_SLA_AUTO_ESCALATE = "SLA_AUTO_ESCALATE";
    private static final String ACTION_SLA_WARN_NOTIFY = "SLA_WARN_NOTIFY";
    private static final String ACTION_SLA_NOTIFY_DISPATCH = "SLA_NOTIFY_DISPATCH";
    private static final String ROUTE_DECISION_ORDER = "RULE>TYPE_SEVERITY>TYPE_DEFAULT>GLOBAL_DEFAULT";
    private static final String ROUTE_SCOPE_GLOBAL_FALLBACK = "GLOBAL_DEFAULT_FALLBACK";
    private static final String CONFIG_KEY_FALLBACK_P0_ESCALATE_TO = "hxy.trade.review-ticket.sla.fallback.p0.escalate-to";
    private static final String CONFIG_KEY_FALLBACK_P0_SLA_MINUTES = "hxy.trade.review-ticket.sla.fallback.p0.sla-minutes";
    private static final String CONFIG_KEY_FALLBACK_DEFAULT_ESCALATE_TO = "hxy.trade.review-ticket.sla.fallback.default.escalate-to";
    private static final String CONFIG_KEY_FALLBACK_DEFAULT_SLA_MINUTES = "hxy.trade.review-ticket.sla.fallback.default.sla-minutes";
    private static final String CONFIG_KEY_WARN_LEAD_DEFAULT = "hxy.trade.review-ticket.sla.warn.lead-minutes.default";
    private static final String CONFIG_KEY_WARN_LEAD_MAX = "hxy.trade.review-ticket.sla.warn.lead-minutes.max";
    private static final String CONFIG_KEY_NOTIFY_MAX_RETRY_COUNT = "hxy.trade.review-ticket.sla.notify.max-retry-count";
    private static final String FALLBACK_ESCALATE_TO_P0 = "HQ_RISK_FINANCE";
    private static final String FALLBACK_ESCALATE_TO_DEFAULT = "HQ_AFTER_SALE";
    private static final int FALLBACK_SLA_MINUTES_P0 = 30;
    private static final int FALLBACK_SLA_MINUTES_DEFAULT = 120;
    private static final int DEFAULT_WARN_BATCH_LIMIT = 200;
    private static final int MAX_WARN_BATCH_LIMIT = 5000;
    private static final int DEFAULT_WARN_LEAD_MINUTES = 30;
    private static final int MAX_WARN_LEAD_MINUTES = 24 * 60;
    private static final int DEFAULT_NOTIFY_DISPATCH_LIMIT = 200;
    private static final int MAX_NOTIFY_DISPATCH_LIMIT = 1000;
    private static final int DEFAULT_NOTIFY_MAX_RETRY_COUNT = 5;
    private static final int MAX_NOTIFY_MAX_RETRY_COUNT = 20;
    private static final int NOTIFY_BACKOFF_BASE_MINUTES = 5;
    private static final long DEFAULT_NOTIFY_ADMIN_USER_ID = 1L;

    private static final String NOTIFY_TYPE_SLA_WARN = "SLA_WARN";
    private static final String NOTIFY_TYPE_SLA_ESCALATE = "SLA_ESCALATE";
    private static final String NOTIFY_CHANNEL_IN_APP = "IN_APP";
    private static final int NOTIFY_STATUS_PENDING = 0;
    private static final int NOTIFY_STATUS_SENT = 1;
    private static final int NOTIFY_STATUS_FAILED = 2;
    private static final String NOTIFY_ACTION_CREATE = "CREATE";
    private static final String NOTIFY_ACTION_DISPATCH_SUCCESS = "DISPATCH_SUCCESS";
    private static final String NOTIFY_ACTION_DISPATCH_FAILED = "DISPATCH_FAILED";
    private static final String NOTIFY_ACTION_MANUAL_RETRY = "MANUAL_RETRY";
    private static final String NOTIFY_TEMPLATE_CODE_WARN = "hxy_trade_review_ticket_warn";
    private static final String NOTIFY_TEMPLATE_CODE_ESCALATE = "hxy_trade_review_ticket_escalate";

    @Resource
    private AfterSaleReviewTicketMapper afterSaleReviewTicketMapper;
    @Resource
    private AfterSaleReviewTicketNotifyOutboxMapper afterSaleReviewTicketNotifyOutboxMapper;
    @Resource
    private TradeTicketSlaRuleApi tradeTicketSlaRuleApi;
    @Resource
    private ConfigApi configApi;
    @Resource
    private NotifySendService notifySendService;
    @Resource
    private PermissionApi permissionApi;

    @Value("${hxy.trade.review-ticket.notify-target-role-ids:}")
    private String notifyTargetRoleIdsConfig;

    @Override
    public PageResult<AfterSaleReviewTicketDO> getReviewTicketPage(AfterSaleReviewTicketPageReqVO pageReqVO) {
        normalizePageReq(pageReqVO);
        return afterSaleReviewTicketMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<AfterSaleReviewTicketNotifyOutboxDO> getNotifyOutboxPage(
            AfterSaleReviewTicketNotifyOutboxPageReqVO pageReqVO) {
        normalizeNotifyOutboxPageReq(pageReqVO);
        return afterSaleReviewTicketNotifyOutboxMapper.selectPage(pageReqVO);
    }

    @Override
    public AfterSaleReviewTicketDO getReviewTicket(Long id) {
        if (id == null) {
            return null;
        }
        return afterSaleReviewTicketMapper.selectById(id);
    }

    @Override
    public Long createReviewTicket(AfterSaleReviewTicketCreateReqBO reqBO) {
        if (reqBO == null) {
            return null;
        }
        Integer ticketType = ObjUtil.defaultIfNull(reqBO.getTicketType(), AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType());
        TicketRoute route = buildRoute(ticketType, reqBO.getRuleCode(), reqBO.getSeverity(), null);
        LocalDateTime now = LocalDateTime.now();
        String severity = StrUtil.blankToDefault(reqBO.getSeverity(), route.getSeverity());
        String escalateTo = StrUtil.blankToDefault(reqBO.getEscalateTo(), route.getEscalateTo());
        Integer slaMinutes = resolveSlaMinutes(reqBO.getSlaMinutes(), route.getSlaMinutes());
        String sourceBizNo = StrUtil.blankToDefault(reqBO.getSourceBizNo(),
                reqBO.getAfterSaleId() == null ? "" : String.valueOf(reqBO.getAfterSaleId()));

        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO()
                .setTicketType(ticketType)
                .setAfterSaleId(reqBO.getAfterSaleId())
                .setOrderId(reqBO.getOrderId())
                .setOrderItemId(reqBO.getOrderItemId())
                .setUserId(reqBO.getUserId())
                .setSourceBizNo(sourceBizNo)
                .setRuleCode(StrUtil.blankToDefault(reqBO.getRuleCode(), "MANUAL_CREATE"))
                .setDecisionReason(abbreviate(reqBO.getDecisionReason(), 500))
                .setSeverity(severity)
                .setEscalateTo(escalateTo)
                .setSlaDeadlineTime(now.plusMinutes(slaMinutes))
                .setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus())
                .setFirstTriggerTime(now)
                .setLastTriggerTime(now)
                .setTriggerCount(1)
                .setLastActionCode(ACTION_TICKET_CREATE)
                .setLastActionBizNo(normalizeActionBizNo(sourceBizNo, reqBO.getAfterSaleId()))
                .setLastActionTime(now)
                .setRemark(abbreviate(reqBO.getRemark(), 255));
        applyRouteSnapshot(ticket, route);
        afterSaleReviewTicketMapper.insert(ticket);
        return ticket.getId();
    }

    @Override
    public Long upsertReviewTicketBySourceBizNo(AfterSaleReviewTicketCreateReqBO reqBO, String actionCode) {
        if (reqBO == null) {
            return null;
        }
        Integer ticketType = ObjUtil.defaultIfNull(reqBO.getTicketType(), AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType());
        String sourceBizNo = resolveSourceBizNo(reqBO, null);
        if (StrUtil.isBlank(sourceBizNo)) {
            return null;
        }
        String normalizedActionCode = normalizeActionCode(actionCode, ACTION_RULE_RETRIGGER);
        AfterSaleReviewTicketDO existed = afterSaleReviewTicketMapper.selectByTicketTypeAndSourceBizNo(ticketType, sourceBizNo);
        if (existed == null) {
            TicketRoute route = buildRoute(ticketType, reqBO.getRuleCode(), reqBO.getSeverity(), null);
            LocalDateTime now = LocalDateTime.now();
            String severity = StrUtil.blankToDefault(reqBO.getSeverity(), route.getSeverity());
            String escalateTo = StrUtil.blankToDefault(reqBO.getEscalateTo(), route.getEscalateTo());
            Integer slaMinutes = resolveSlaMinutes(reqBO.getSlaMinutes(), route.getSlaMinutes());
            AfterSaleReviewTicketDO createObj = new AfterSaleReviewTicketDO()
                    .setTicketType(ticketType)
                    .setAfterSaleId(reqBO.getAfterSaleId())
                    .setOrderId(reqBO.getOrderId())
                    .setOrderItemId(reqBO.getOrderItemId())
                    .setUserId(reqBO.getUserId())
                    .setSourceBizNo(sourceBizNo)
                    .setRuleCode(StrUtil.blankToDefault(reqBO.getRuleCode(), "MANUAL_CREATE"))
                    .setDecisionReason(abbreviate(reqBO.getDecisionReason(), 500))
                    .setSeverity(severity)
                    .setEscalateTo(escalateTo)
                    .setSlaDeadlineTime(now.plusMinutes(slaMinutes))
                    .setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus())
                    .setFirstTriggerTime(now)
                    .setLastTriggerTime(now)
                    .setTriggerCount(1)
                    .setLastActionCode(normalizedActionCode)
                    .setLastActionBizNo(normalizeActionBizNo(sourceBizNo, reqBO.getAfterSaleId()))
                    .setLastActionTime(now)
                    .setRemark(abbreviate(reqBO.getRemark(), 255));
            applyRouteSnapshot(createObj, route);
            try {
                afterSaleReviewTicketMapper.insert(createObj);
                return createObj.getId();
            } catch (DuplicateKeyException ex) {
                existed = afterSaleReviewTicketMapper.selectByTicketTypeAndSourceBizNo(ticketType, sourceBizNo);
                if (existed == null) {
                    throw ex;
                }
            }
        }
        TicketRoute route = buildRoute(ticketType, reqBO.getRuleCode(),
                StrUtil.blankToDefault(reqBO.getSeverity(), existed.getSeverity()), null);
        LocalDateTime now = LocalDateTime.now();
        String severity = StrUtil.blankToDefault(reqBO.getSeverity(), route.getSeverity());
        String escalateTo = StrUtil.blankToDefault(reqBO.getEscalateTo(), route.getEscalateTo());
        Integer slaMinutes = resolveSlaMinutes(reqBO.getSlaMinutes(), route.getSlaMinutes());
        String ruleCode = StrUtil.blankToDefault(reqBO.getRuleCode(), StrUtil.blankToDefault(existed.getRuleCode(), "UNKNOWN"));
        AfterSaleReviewTicketDO updateObj = new AfterSaleReviewTicketDO()
                .setId(existed.getId())
                .setTicketType(ticketType)
                .setAfterSaleId(ObjUtil.defaultIfNull(reqBO.getAfterSaleId(), existed.getAfterSaleId()))
                .setOrderId(ObjUtil.defaultIfNull(reqBO.getOrderId(), existed.getOrderId()))
                .setOrderItemId(ObjUtil.defaultIfNull(reqBO.getOrderItemId(), existed.getOrderItemId()))
                .setUserId(ObjUtil.defaultIfNull(reqBO.getUserId(), existed.getUserId()))
                .setSourceBizNo(sourceBizNo)
                .setRuleCode(ruleCode)
                .setDecisionReason(abbreviate(StrUtil.blankToDefault(reqBO.getDecisionReason(), existed.getDecisionReason()), 500))
                .setSeverity(severity)
                .setEscalateTo(escalateTo)
                .setSlaDeadlineTime(now.plusMinutes(slaMinutes))
                .setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus())
                .setFirstTriggerTime(ObjUtil.defaultIfNull(existed.getFirstTriggerTime(), now))
                .setLastTriggerTime(now)
                .setTriggerCount(ObjUtil.defaultIfNull(existed.getTriggerCount(), 0) + 1)
                .setResolvedTime(null)
                .setResolverId(null)
                .setResolverType(null)
                .setResolveActionCode("")
                .setResolveBizNo("")
                .setLastActionCode(normalizedActionCode)
                .setLastActionBizNo(normalizeActionBizNo(sourceBizNo, reqBO.getAfterSaleId()))
                .setLastActionTime(now)
                .setRemark(abbreviate(StrUtil.blankToDefault(reqBO.getRemark(), existed.getRemark()), 255));
        applyRouteSnapshot(updateObj, route);
        afterSaleReviewTicketMapper.updateById(updateObj);
        return existed.getId();
    }

    @Override
    public List<AfterSaleReviewTicketDO> listLatestByTicketTypeAndSourceBizNos(Integer ticketType,
                                                                                List<String> sourceBizNos) {
        if (ticketType == null || CollectionUtils.isEmpty(sourceBizNos)) {
            return Collections.emptyList();
        }
        List<String> normalizedSourceBizNos = sourceBizNos.stream()
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(normalizedSourceBizNos)) {
            return Collections.emptyList();
        }
        List<AfterSaleReviewTicketDO> tickets =
                afterSaleReviewTicketMapper.selectListByTicketTypeAndSourceBizNos(ticketType, normalizedSourceBizNos);
        if (CollectionUtils.isEmpty(tickets)) {
            return Collections.emptyList();
        }
        Map<String, AfterSaleReviewTicketDO> latestBySourceBizNo = new HashMap<>();
        for (AfterSaleReviewTicketDO ticket : tickets) {
            if (ticket == null || StrUtil.isBlank(ticket.getSourceBizNo())
                    || latestBySourceBizNo.containsKey(ticket.getSourceBizNo())) {
                continue;
            }
            latestBySourceBizNo.put(ticket.getSourceBizNo(), ticket);
        }
        return new ArrayList<>(latestBySourceBizNo.values());
    }

    @Override
    public boolean resolveReviewTicketBySourceBizNo(Integer ticketType, String sourceBizNo, Long resolverId,
                                                    Integer resolverType, String resolveActionCode,
                                                    String resolveBizNo, String resolveRemark) {
        if (ticketType == null || StrUtil.isBlank(sourceBizNo)) {
            return false;
        }
        AfterSaleReviewTicketDO ticket =
                afterSaleReviewTicketMapper.selectByTicketTypeAndSourceBizNo(ticketType, sourceBizNo);
        if (ticket == null || !AfterSaleReviewTicketStatusEnum.isPending(ticket.getStatus())) {
            return false;
        }
        String normalizedActionCode = normalizeResolveActionCode(resolveActionCode, DEFAULT_RESOLVE_ACTION_AUTO);
        String normalizedBizNo = normalizeResolveBizNo(resolveBizNo, ticket.getId());
        int updateCount = afterSaleReviewTicketMapper.updateByIdAndStatus(ticket.getId(),
                AfterSaleReviewTicketStatusEnum.PENDING.getStatus(),
                buildResolvedTicketUpdate(resolverId, resolverType, normalizedActionCode, normalizedBizNo, resolveRemark));
        return updateCount > 0;
    }

    @Override
    public void upsertManualReviewTicket(AfterSaleDO afterSale, AfterSaleRefundDecisionBO decision) {
        if (afterSale == null || decision == null || afterSale.getId() == null) {
            return;
        }
        AfterSaleReviewTicketDO existed = afterSaleReviewTicketMapper.selectByAfterSaleId(afterSale.getId());
        TicketRoute route = buildRoute(
                AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType(),
                decision.getRuleCode(),
                existed == null ? null : existed.getSeverity(),
                null);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slaDeadlineTime = now.plusMinutes(route.getSlaMinutes());
        if (existed == null) {
            afterSaleReviewTicketMapper.insert(new AfterSaleReviewTicketDO()
                    .setTicketType(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType())
                    .setAfterSaleId(afterSale.getId())
                    .setOrderId(afterSale.getOrderId())
                    .setOrderItemId(afterSale.getOrderItemId())
                    .setUserId(afterSale.getUserId())
                    .setSourceBizNo(StrUtil.blankToDefault(afterSale.getNo(), String.valueOf(afterSale.getId())))
                    .setRuleCode(StrUtil.blankToDefault(decision.getRuleCode(), "UNKNOWN"))
                    .setDecisionReason(abbreviate(decision.getReason(), 500))
                    .setSeverity(route.getSeverity())
                    .setEscalateTo(route.getEscalateTo())
                    .setSlaDeadlineTime(slaDeadlineTime)
                    .setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus())
                    .setFirstTriggerTime(now)
                    .setLastTriggerTime(now)
                    .setTriggerCount(1)
                    .setLastActionCode(ACTION_TICKET_CREATE)
                    .setLastActionBizNo(normalizeActionBizNo(afterSale.getNo(), afterSale.getId()))
                    .setLastActionTime(now)
                    .setRemark(abbreviate(decision.getReason(), 255))
                    .setRouteId(route.getRouteId())
                    .setRouteScope(resolveRouteScope(route))
                    .setRouteDecisionOrder(resolveRouteDecisionOrder(route)));
            return;
        }
        afterSaleReviewTicketMapper.updateById(new AfterSaleReviewTicketDO()
                .setId(existed.getId())
                .setTicketType(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType())
                .setRuleCode(StrUtil.blankToDefault(decision.getRuleCode(), "UNKNOWN"))
                .setDecisionReason(abbreviate(decision.getReason(), 500))
                .setSeverity(route.getSeverity())
                .setEscalateTo(route.getEscalateTo())
                .setSourceBizNo(StrUtil.blankToDefault(afterSale.getNo(), String.valueOf(afterSale.getId())))
                .setSlaDeadlineTime(slaDeadlineTime)
                .setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus())
                .setFirstTriggerTime(ObjUtil.defaultIfNull(existed.getFirstTriggerTime(), now))
                .setLastTriggerTime(now)
                .setTriggerCount(ObjUtil.defaultIfNull(existed.getTriggerCount(), 0) + 1)
                .setLastActionCode(ACTION_RULE_RETRIGGER)
                .setLastActionBizNo(normalizeActionBizNo(afterSale.getNo(), afterSale.getId()))
                .setLastActionTime(now)
                .setRemark(abbreviate(decision.getReason(), 255))
                .setRouteId(route.getRouteId())
                .setRouteScope(resolveRouteScope(route))
                .setRouteDecisionOrder(resolveRouteDecisionOrder(route)));
    }

    @Override
    public void resolveManualReviewTicket(Long afterSaleId, Long resolverId, Integer resolverType,
                                          String resolveActionCode, String resolveBizNo, String resolveRemark) {
        if (afterSaleId == null) {
            return;
        }
        String normalizedActionCode = normalizeResolveActionCode(resolveActionCode, DEFAULT_RESOLVE_ACTION_AUTO);
        String normalizedBizNo = normalizeResolveBizNo(resolveBizNo, afterSaleId);
        afterSaleReviewTicketMapper.updateByAfterSaleIdAndStatus(afterSaleId,
                AfterSaleReviewTicketStatusEnum.PENDING.getStatus(),
                buildResolvedTicketUpdate(resolverId, resolverType, normalizedActionCode, normalizedBizNo, resolveRemark));
    }

    @Override
    public void resolveManualReviewTicketById(Long id, Long resolverId, Integer resolverType,
                                              String resolveActionCode, String resolveBizNo, String resolveRemark) {
        AfterSaleReviewTicketDO ticket = afterSaleReviewTicketMapper.selectById(id);
        if (ticket == null) {
            throw exception(AFTER_SALE_REVIEW_TICKET_NOT_FOUND);
        }
        if (!AfterSaleReviewTicketStatusEnum.isPending(ticket.getStatus())) {
            throw exception(AFTER_SALE_REVIEW_TICKET_STATUS_NOT_PENDING);
        }
        String normalizedActionCode = normalizeResolveActionCode(resolveActionCode, DEFAULT_RESOLVE_ACTION_MANUAL);
        String normalizedBizNo = normalizeResolveBizNo(resolveBizNo, id);
        int updateCount = afterSaleReviewTicketMapper.updateByIdAndStatus(id,
                AfterSaleReviewTicketStatusEnum.PENDING.getStatus(),
                buildResolvedTicketUpdate(resolverId, resolverType, normalizedActionCode, normalizedBizNo, resolveRemark));
        if (updateCount == 0) {
            throw exception(AFTER_SALE_REVIEW_TICKET_STATUS_NOT_PENDING);
        }
    }

    @Override
    public AfterSaleReviewTicketBatchResolveResult batchResolveManualReviewTicketByIds(List<Long> ids, Long resolverId,
                                                                                        Integer resolverType,
                                                                                        String resolveActionCode,
                                                                                        String resolveBizNo,
                                                                                        String resolveRemark) {
        if (ids == null || ids.isEmpty()) {
            return AfterSaleReviewTicketBatchResolveResult.empty();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                uniqueIds.add(id);
            }
        }
        if (uniqueIds.isEmpty()) {
            return AfterSaleReviewTicketBatchResolveResult.empty();
        }
        String normalizedActionCode = normalizeResolveActionCode(resolveActionCode, DEFAULT_RESOLVE_ACTION_MANUAL);
        String normalizedBizNo = normalizeResolveBizNo(resolveBizNo, null);
        List<Long> successIds = new ArrayList<>();
        List<Long> skippedNotFoundIds = new ArrayList<>();
        List<Long> skippedNotPendingIds = new ArrayList<>();
        for (Long id : uniqueIds) {
            AfterSaleReviewTicketDO ticket = afterSaleReviewTicketMapper.selectById(id);
            if (ticket == null) {
                skippedNotFoundIds.add(id);
                continue;
            }
            if (!AfterSaleReviewTicketStatusEnum.isPending(ticket.getStatus())) {
                skippedNotPendingIds.add(id);
                continue;
            }
            int updateCount = afterSaleReviewTicketMapper.updateByIdAndStatus(id,
                    AfterSaleReviewTicketStatusEnum.PENDING.getStatus(),
                    buildResolvedTicketUpdate(resolverId, resolverType, normalizedActionCode,
                            buildBatchResolveBizNo(normalizedBizNo, id),
                            resolveRemark));
            if (updateCount > 0) {
                successIds.add(id);
            } else {
                skippedNotPendingIds.add(id);
            }
        }
        return AfterSaleReviewTicketBatchResolveResult.builder()
                .totalCount(uniqueIds.size())
                .successCount(successIds.size())
                .skippedNotFoundCount(skippedNotFoundIds.size())
                .skippedNotPendingCount(skippedNotPendingIds.size())
                .successIds(successIds)
                .skippedNotFoundIds(skippedNotFoundIds)
                .skippedNotPendingIds(skippedNotPendingIds)
                .build();
    }

    private String buildBatchResolveBizNo(String baseBizNo, Long ticketId) {
        String fallback = String.valueOf(ticketId);
        if (StrUtil.isBlank(baseBizNo)) {
            return fallback;
        }
        String suffix = "#" + fallback;
        int maxBaseLength = Math.max(0, 64 - suffix.length());
        String normalizedBase = StrUtil.maxLength(baseBizNo, maxBaseLength);
        return normalizedBase + suffix;
    }

    private AfterSaleReviewTicketDO buildResolvedTicketUpdate(Long resolverId, Integer resolverType,
                                                              String resolveActionCode, String resolveBizNo,
                                                              String resolveRemark) {
        LocalDateTime now = LocalDateTime.now();
        return new AfterSaleReviewTicketDO()
                .setStatus(AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus())
                .setResolverId(resolverId)
                .setResolverType(resolverType)
                .setResolveActionCode(resolveActionCode)
                .setResolveBizNo(resolveBizNo)
                .setLastActionCode(resolveActionCode)
                .setLastActionBizNo(resolveBizNo)
                .setLastActionTime(now)
                .setResolvedTime(now)
                .setRemark(abbreviate(resolveRemark, 255));
    }

    @Override
    public int escalateOverduePendingTickets(Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjUtil.defaultIfNull(limit, DEFAULT_ESCALATE_BATCH_LIMIT), MAX_ESCALATE_BATCH_LIMIT));
        LocalDateTime now = LocalDateTime.now();
        List<AfterSaleReviewTicketDO> overdueTickets = afterSaleReviewTicketMapper
                .selectListByStatusAndSlaDeadlineTimeBefore(
                        AfterSaleReviewTicketStatusEnum.PENDING.getStatus(), now, safeLimit);
        if (overdueTickets == null || overdueTickets.isEmpty()) {
            return 0;
        }
        int affectedRows = 0;
        for (AfterSaleReviewTicketDO ticket : overdueTickets) {
            String newSeverity = nextSeverity(ticket.getSeverity());
            TicketRoute route = buildRoute(ticket.getTicketType(), ticket.getRuleCode(), newSeverity, null);
            if (!isEscalateDelaySatisfied(ticket, route, now)) {
                continue;
            }
            String newEscalateTo = nextEscalateTo(ticket.getEscalateTo(), newSeverity, route.getEscalateTo());
            String newRemark = buildEscalateRemark(ticket, now, newSeverity);
            int updateCount = afterSaleReviewTicketMapper.updateByIdAndStatus(ticket.getId(),
                    AfterSaleReviewTicketStatusEnum.PENDING.getStatus(),
                    new AfterSaleReviewTicketDO()
                            .setSeverity(newSeverity)
                            .setEscalateTo(newEscalateTo)
                            .setRouteId(route.getRouteId())
                            .setRouteScope(resolveRouteScope(route))
                            .setRouteDecisionOrder(resolveRouteDecisionOrder(route))
                            .setSlaDeadlineTime(now.plusMinutes(resolveEscalatedSlaMinutes(newSeverity, route.getSlaMinutes())))
                            .setLastTriggerTime(now)
                            .setTriggerCount(ObjUtil.defaultIfNull(ticket.getTriggerCount(), 0) + 1)
                            .setLastActionCode(ACTION_SLA_AUTO_ESCALATE)
                            .setLastActionBizNo(normalizeActionBizNo("TICKET#" + ticket.getId(), ticket.getId()))
                            .setLastActionTime(now)
                            .setRemark(abbreviate(newRemark, 255)));
            if (updateCount > 0) {
                createNotifyOutboxIfAbsent(ticket, now, NOTIFY_TYPE_SLA_ESCALATE, newSeverity, newEscalateTo,
                        buildEscalateNotifyBizKey(ticket));
                affectedRows += 1;
            }
        }
        return affectedRows;
    }

    @Override
    public int warnNearDeadlinePendingTickets(Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjUtil.defaultIfNull(limit, DEFAULT_WARN_BATCH_LIMIT), MAX_WARN_BATCH_LIMIT));
        LocalDateTime now = LocalDateTime.now();
        int maxLeadMinutes = resolveConfigPositiveInt(CONFIG_KEY_WARN_LEAD_MAX,
                MAX_WARN_LEAD_MINUTES, 1, MAX_WARN_LEAD_MINUTES);
        List<AfterSaleReviewTicketDO> candidateList = afterSaleReviewTicketMapper.selectListByStatusAndSlaDeadlineTimeBefore(
                AfterSaleReviewTicketStatusEnum.PENDING.getStatus(), now.plusMinutes(maxLeadMinutes), safeLimit);
        if (candidateList == null || candidateList.isEmpty()) {
            return 0;
        }
        int affectedRows = 0;
        for (AfterSaleReviewTicketDO ticket : candidateList) {
            if (ticket == null || ticket.getId() == null || ticket.getSlaDeadlineTime() == null) {
                continue;
            }
            if (ticket.getSlaDeadlineTime().isBefore(now)) {
                continue;
            }
            TicketRoute route = buildRoute(ticket.getTicketType(), ticket.getRuleCode(), ticket.getSeverity(), null);
            int warnLeadMinutes = resolveWarnLeadMinutes(route == null ? null : route.getWarnLeadMinutes());
            if (now.isBefore(ticket.getSlaDeadlineTime().minusMinutes(warnLeadMinutes))) {
                continue;
            }
            String bizKey = buildWarnNotifyBizKey(ticket);
            if (!createNotifyOutboxIfAbsent(ticket, now, NOTIFY_TYPE_SLA_WARN, ticket.getSeverity(),
                    ticket.getEscalateTo(), bizKey)) {
                continue;
            }
            int updateCount = afterSaleReviewTicketMapper.updateByIdAndStatus(ticket.getId(),
                    AfterSaleReviewTicketStatusEnum.PENDING.getStatus(),
                    new AfterSaleReviewTicketDO()
                            .setLastActionCode(ACTION_SLA_WARN_NOTIFY)
                            .setLastActionBizNo(bizKey)
                            .setLastActionTime(now)
                            .setRemark(abbreviate(buildWarnRemark(ticket, now), 255)));
            affectedRows += updateCount > 0 ? 1 : 0;
        }
        return affectedRows;
    }

    @Override
    public int dispatchPendingNotifyOutbox(Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjUtil.defaultIfNull(limit, DEFAULT_NOTIFY_DISPATCH_LIMIT),
                MAX_NOTIFY_DISPATCH_LIMIT));
        int maxRetryCount = resolveConfigPositiveInt(CONFIG_KEY_NOTIFY_MAX_RETRY_COUNT,
                DEFAULT_NOTIFY_MAX_RETRY_COUNT, 1, MAX_NOTIFY_MAX_RETRY_COUNT);
        LocalDateTime now = LocalDateTime.now();
        List<AfterSaleReviewTicketNotifyOutboxDO> dispatchableList =
                afterSaleReviewTicketNotifyOutboxMapper.selectDispatchableList(now, safeLimit, maxRetryCount);
        if (dispatchableList == null || dispatchableList.isEmpty()) {
            return 0;
        }
        int successCount = 0;
        for (AfterSaleReviewTicketNotifyOutboxDO outbox : dispatchableList) {
            if (outbox == null || outbox.getId() == null) {
                continue;
            }
            Integer currentStatus = ObjUtil.defaultIfNull(outbox.getStatus(), NOTIFY_STATUS_PENDING);
            try {
                dispatchNotify(outbox);
                int updated = afterSaleReviewTicketNotifyOutboxMapper.updateByIdAndStatus(outbox.getId(), currentStatus,
                        new AfterSaleReviewTicketNotifyOutboxDO()
                                .setStatus(NOTIFY_STATUS_SENT)
                                .setRetryCount(ObjUtil.defaultIfNull(outbox.getRetryCount(), 0))
                                .setNextRetryTime(null)
                                .setSentTime(now)
                                .setLastErrorMsg("")
                                .setLastActionCode(NOTIFY_ACTION_DISPATCH_SUCCESS)
                                .setLastActionBizNo(buildDispatchAuditBizNo(outbox.getId(), now))
                                .setLastActionTime(now));
                if (updated > 0) {
                    afterSaleReviewTicketMapper.updateByIdAndStatus(outbox.getTicketId(),
                            AfterSaleReviewTicketStatusEnum.PENDING.getStatus(),
                            new AfterSaleReviewTicketDO()
                                    .setLastActionCode(ACTION_SLA_NOTIFY_DISPATCH)
                                    .setLastActionBizNo(buildDispatchAuditBizNo(outbox.getId(), now))
                                    .setLastActionTime(now));
                    successCount++;
                }
            } catch (Exception ex) {
                int nextRetryCount = ObjUtil.defaultIfNull(outbox.getRetryCount(), 0) + 1;
                afterSaleReviewTicketNotifyOutboxMapper.updateByIdAndStatus(outbox.getId(), currentStatus,
                        new AfterSaleReviewTicketNotifyOutboxDO()
                                .setStatus(NOTIFY_STATUS_FAILED)
                                .setRetryCount(nextRetryCount)
                                .setNextRetryTime(calculateNextRetryTime(now, nextRetryCount))
                                .setLastErrorMsg(abbreviate(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), 255))
                                .setLastActionCode(NOTIFY_ACTION_DISPATCH_FAILED)
                                .setLastActionBizNo(buildDispatchAuditBizNo(outbox.getId(), now))
                                .setLastActionTime(now));
                log.warn("[dispatchPendingNotifyOutbox][outboxId({}) dispatch failed]", outbox.getId(), ex);
            }
        }
        return successCount;
    }

    @Override
    public AfterSaleReviewTicketNotifyBatchRetryResult retryNotifyOutboxBatch(List<Long> ids, Long operatorId, String reason) {
        List<Long> normalizedIds = normalizePositiveIds(ids);
        if (normalizedIds.isEmpty()) {
            return AfterSaleReviewTicketNotifyBatchRetryResult.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> successIds = new ArrayList<>();
        List<Long> skippedNotFoundIds = new ArrayList<>();
        List<Long> skippedStatusInvalidIds = new ArrayList<>();
        for (Long id : normalizedIds) {
            AfterSaleReviewTicketNotifyOutboxDO outbox = afterSaleReviewTicketNotifyOutboxMapper.selectById(id);
            if (outbox == null) {
                skippedNotFoundIds.add(id);
                continue;
            }
            Integer currentStatus = ObjUtil.defaultIfNull(outbox.getStatus(), NOTIFY_STATUS_PENDING);
            if (!Objects.equals(currentStatus, NOTIFY_STATUS_FAILED)) {
                skippedStatusInvalidIds.add(id);
                continue;
            }
            int updateCount = afterSaleReviewTicketNotifyOutboxMapper.updateByIdAndStatus(id, currentStatus,
                    new AfterSaleReviewTicketNotifyOutboxDO()
                            .setStatus(NOTIFY_STATUS_PENDING)
                            .setRetryCount(0)
                            .setNextRetryTime(now)
                            .setLastErrorMsg("")
                            .setLastActionCode(NOTIFY_ACTION_MANUAL_RETRY)
                            .setLastActionBizNo(buildManualRetryAuditBizNo(operatorId, id, reason))
                            .setLastActionTime(now));
            if (updateCount > 0) {
                successIds.add(id);
            } else {
                skippedStatusInvalidIds.add(id);
            }
        }
        return AfterSaleReviewTicketNotifyBatchRetryResult.builder()
                .totalCount(normalizedIds.size())
                .successCount(successIds.size())
                .skippedNotFoundCount(skippedNotFoundIds.size())
                .skippedStatusInvalidCount(skippedStatusInvalidIds.size())
                .successIds(successIds)
                .skippedNotFoundIds(skippedNotFoundIds)
                .skippedStatusInvalidIds(skippedStatusInvalidIds)
                .build();
    }

    private TicketRoute buildRoute(Integer ticketType, String ruleCode, String severity, Long storeId) {
        TradeTicketSlaRuleMatchRespDTO matchResp = null;
        try {
            TradeTicketSlaRuleMatchReqDTO reqDTO = new TradeTicketSlaRuleMatchReqDTO();
            reqDTO.setTicketType(ObjUtil.defaultIfNull(ticketType, TicketSlaRuleTicketTypeEnum.AFTER_SALE_REVIEW.getType()));
            reqDTO.setRuleCode(ruleCode);
            reqDTO.setSeverity(severity);
            reqDTO.setStoreId(storeId);
            matchResp = tradeTicketSlaRuleApi.matchRule(reqDTO);
        } catch (Exception ignore) {
            // 规则中心不可用时走默认兜底，避免创建/升级流程中断
        }
        if (matchResp != null && Boolean.TRUE.equals(matchResp.getMatched())) {
            String routeSeverity = StrUtil.blankToDefault(matchResp.getSeverity(), StrUtil.blankToDefault(severity, "P1"));
            String routeScope = resolveRouteScope(matchResp.getMatchLevel());
            return new TicketRoute(routeSeverity,
                    StrUtil.blankToDefault(matchResp.getEscalateTo(), nextEscalateTo("", routeSeverity, null)),
                    resolveSlaMinutes(matchResp.getSlaMinutes(), 120),
                    resolveWarnLeadMinutes(matchResp.getWarnLeadMinutes()),
                    resolveEscalateDelayMinutes(matchResp.getEscalateDelayMinutes()),
                    matchResp.getRuleId(),
                    routeScope,
                    ROUTE_DECISION_ORDER);
        }
        return buildFallbackRoute(severity);
    }

    private TicketRoute buildFallbackRoute(String requestedSeverity) {
        String severity = normalizeSeverity(requestedSeverity);
        if (StrUtil.equalsIgnoreCase("P0", severity)) {
            return new TicketRoute("P0",
                    resolveConfigString(CONFIG_KEY_FALLBACK_P0_ESCALATE_TO, FALLBACK_ESCALATE_TO_P0, 64),
                    resolveConfigPositiveInt(CONFIG_KEY_FALLBACK_P0_SLA_MINUTES, FALLBACK_SLA_MINUTES_P0, 1, 30),
                    resolveWarnLeadMinutes(null),
                    0,
                    null,
                    ROUTE_SCOPE_GLOBAL_FALLBACK, ROUTE_DECISION_ORDER);
        }
        return new TicketRoute(severity,
                resolveConfigString(CONFIG_KEY_FALLBACK_DEFAULT_ESCALATE_TO, FALLBACK_ESCALATE_TO_DEFAULT, 64),
                resolveConfigPositiveInt(CONFIG_KEY_FALLBACK_DEFAULT_SLA_MINUTES, FALLBACK_SLA_MINUTES_DEFAULT, 1, 7 * 24 * 60),
                resolveWarnLeadMinutes(null),
                0,
                null,
                ROUTE_SCOPE_GLOBAL_FALLBACK, ROUTE_DECISION_ORDER);
    }

    private String abbreviate(String text, int maxLength) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return StrUtil.maxLength(text.trim(), maxLength);
    }

    private String nextSeverity(String severity) {
        if (StrUtil.equalsIgnoreCase("P2", severity)) {
            return "P1";
        }
        if (StrUtil.equalsIgnoreCase("P1", severity)) {
            return "P0";
        }
        if (StrUtil.equalsIgnoreCase("P0", severity)) {
            return "P0";
        }
        return "P1";
    }

    private String normalizeSeverity(String severity) {
        String normalized = StrUtil.blankToDefault(StrUtil.trim(severity), "P1").toUpperCase(Locale.ROOT);
        return StrUtil.equalsAny(normalized, "P0", "P1", "P2") ? normalized : "P1";
    }

    private String resolveConfigString(String key, String defaultValue, int maxLength) {
        String value = StrUtil.trim(configApi.getConfigValueByKey(key));
        return StrUtil.maxLength(StrUtil.blankToDefault(value, defaultValue), maxLength);
    }

    private Integer resolveConfigPositiveInt(String key, Integer defaultValue, int min, int max) {
        String value = configApi.getConfigValueByKey(key);
        if (StrUtil.isBlank(value) || !StrUtil.isNumeric(value.trim())) {
            return clamp(defaultValue, min, max);
        }
        return clamp(Integer.parseInt(value.trim()), min, max);
    }

    private Integer resolveEscalateDelayMinutes(Integer delayMinutes) {
        return clamp(delayMinutes, 0, 7 * 24 * 60);
    }

    private Integer resolveWarnLeadMinutes(Integer warnLeadMinutes) {
        Integer defaultValue = resolveConfigPositiveInt(CONFIG_KEY_WARN_LEAD_DEFAULT,
                DEFAULT_WARN_LEAD_MINUTES, 1, MAX_WARN_LEAD_MINUTES);
        return clamp(ObjUtil.defaultIfNull(warnLeadMinutes, defaultValue), 1, MAX_WARN_LEAD_MINUTES);
    }

    private Integer clamp(Integer value, int min, int max) {
        int safe = ObjUtil.defaultIfNull(value, min);
        return Math.max(min, Math.min(safe, max));
    }

    private String nextEscalateTo(String currentEscalateTo, String severity, String routeEscalateTo) {
        if (StrUtil.equalsIgnoreCase("P0", severity)) {
            return StrUtil.blankToDefault(routeEscalateTo, "HQ_RISK_FINANCE");
        }
        if (StrUtil.isNotBlank(routeEscalateTo)) {
            return routeEscalateTo;
        }
        if (StrUtil.isNotBlank(currentEscalateTo)) {
            return currentEscalateTo;
        }
        return "HQ_AFTER_SALE";
    }

    private Integer resolveEscalatedSlaMinutes(String severity, Integer routeSlaMinutes) {
        int fallback = StrUtil.equalsIgnoreCase("P0", severity) ? 30 : 120;
        int resolved = ObjUtil.defaultIfNull(routeSlaMinutes, fallback);
        if (resolved <= 0) {
            resolved = fallback;
        }
        if (StrUtil.equalsIgnoreCase("P0", severity)) {
            return Math.min(resolved, 30);
        }
        return Math.min(resolved, 7 * 24 * 60);
    }

    private boolean isEscalateDelaySatisfied(AfterSaleReviewTicketDO ticket, TicketRoute route, LocalDateTime now) {
        if (ticket == null || route == null || ticket.getSlaDeadlineTime() == null) {
            return true;
        }
        Integer delayMinutes = ObjUtil.defaultIfNull(route.getEscalateDelayMinutes(), 0);
        if (delayMinutes <= 0) {
            return true;
        }
        return !now.isBefore(ticket.getSlaDeadlineTime().plusMinutes(delayMinutes));
    }

    private boolean createNotifyOutboxIfAbsent(AfterSaleReviewTicketDO ticket, LocalDateTime now,
                                               String notifyType, String severity,
                                               String escalateTo, String bizKey) {
        if (ticket == null || ticket.getId() == null || StrUtil.isBlank(bizKey)) {
            return false;
        }
        if (afterSaleReviewTicketNotifyOutboxMapper.selectByBizKey(bizKey) != null) {
            return false;
        }
        try {
            afterSaleReviewTicketNotifyOutboxMapper.insert(new AfterSaleReviewTicketNotifyOutboxDO()
                    .setTicketId(ticket.getId())
                    .setNotifyType(StrUtil.maxLength(StrUtil.blankToDefault(notifyType, NOTIFY_TYPE_SLA_WARN), 32))
                    .setChannel(NOTIFY_CHANNEL_IN_APP)
                    .setSeverity(StrUtil.maxLength(StrUtil.blankToDefault(severity, normalizeSeverity(ticket.getSeverity())), 16))
                    .setEscalateTo(StrUtil.maxLength(StrUtil.blankToDefault(escalateTo, ticket.getEscalateTo()), 64))
                    .setBizKey(StrUtil.maxLength(bizKey, 64))
                    .setStatus(NOTIFY_STATUS_PENDING)
                    .setRetryCount(0)
                    .setNextRetryTime(now)
                    .setSentTime(null)
                    .setLastErrorMsg("")
                    .setLastActionCode(NOTIFY_ACTION_CREATE)
                    .setLastActionBizNo(StrUtil.maxLength("BIZ#" + bizKey, 64))
                    .setLastActionTime(now));
            return true;
        } catch (DuplicateKeyException ex) {
            log.info("[createNotifyOutboxIfAbsent][bizKey({}) duplicated]", bizKey);
            return false;
        }
    }

    private String buildWarnNotifyBizKey(AfterSaleReviewTicketDO ticket) {
        if (ticket == null || ticket.getId() == null) {
            return "";
        }
        String deadlineSlot = ticket.getSlaDeadlineTime() == null ? "0"
                : ticket.getSlaDeadlineTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        return StrUtil.maxLength(StrUtil.format("WARN#{}#{}", ticket.getId(), deadlineSlot), 64);
    }

    private String buildEscalateNotifyBizKey(AfterSaleReviewTicketDO ticket) {
        if (ticket == null || ticket.getId() == null) {
            return "";
        }
        int triggerCount = ObjUtil.defaultIfNull(ticket.getTriggerCount(), 0) + 1;
        return StrUtil.maxLength(StrUtil.format("ESCALATE#{}#{}", ticket.getId(), triggerCount), 64);
    }

    private String buildWarnRemark(AfterSaleReviewTicketDO ticket, LocalDateTime now) {
        String baseRemark = StrUtil.blankToDefault(ticket.getRemark(), "");
        String deadline = ticket.getSlaDeadlineTime() == null ? "unknown"
                : ticket.getSlaDeadlineTime().format(ESCALATE_TIME_FORMATTER);
        String append = StrUtil.format("SLA_WARN_NEAR_DEADLINE@{} deadline={}",
                now.format(ESCALATE_TIME_FORMATTER), deadline);
        if (StrUtil.isBlank(baseRemark)) {
            return append;
        }
        return baseRemark + " | " + append;
    }

    private LocalDateTime calculateNextRetryTime(LocalDateTime now, int retryCount) {
        int multiplier = 1 << Math.max(0, Math.min(retryCount - 1, 6));
        int delayMinutes = Math.min(60, NOTIFY_BACKOFF_BASE_MINUTES * multiplier);
        return now.plusMinutes(delayMinutes);
    }

    private void dispatchNotify(AfterSaleReviewTicketNotifyOutboxDO outbox) {
        if (StrUtil.equalsIgnoreCase(outbox.getChannel(), NOTIFY_CHANNEL_IN_APP)) {
            sendInAppNotify(outbox);
            return;
        }
        throw new IllegalArgumentException("unsupported notify channel: " + outbox.getChannel());
    }

    private void sendInAppNotify(AfterSaleReviewTicketNotifyOutboxDO outbox) {
        AfterSaleReviewTicketDO ticket = afterSaleReviewTicketMapper.selectById(outbox.getTicketId());
        if (ticket == null) {
            throw new IllegalStateException("review ticket not exists: " + outbox.getTicketId());
        }
        String templateCode = resolveNotifyTemplateCode(outbox.getNotifyType());
        Map<String, Object> templateParams = buildInAppNotifyParams(outbox, ticket);
        for (Long adminUserId : resolveNotifyAdminUserIds(ticket)) {
            Long messageId = notifySendService.sendSingleNotifyToAdmin(adminUserId, templateCode, templateParams);
            if (messageId == null) {
                throw new IllegalStateException("notify skipped due template status: " + templateCode);
            }
        }
    }

    private Map<String, Object> buildInAppNotifyParams(AfterSaleReviewTicketNotifyOutboxDO outbox,
                                                        AfterSaleReviewTicketDO ticket) {
        Map<String, Object> params = new HashMap<>();
        params.put("ticketId", ticket.getId());
        params.put("notifyType", StrUtil.blankToDefault(outbox.getNotifyType(), ""));
        params.put("severity", StrUtil.blankToDefault(outbox.getSeverity(), ""));
        params.put("escalateTo", StrUtil.blankToDefault(outbox.getEscalateTo(), ""));
        params.put("deadlineTime", ticket.getSlaDeadlineTime() == null ? "" : ticket.getSlaDeadlineTime().toString());
        params.put("ruleCode", StrUtil.blankToDefault(ticket.getRuleCode(), ""));
        return params;
    }

    private String resolveNotifyTemplateCode(String notifyType) {
        if (StrUtil.equalsIgnoreCase(notifyType, NOTIFY_TYPE_SLA_ESCALATE)) {
            return NOTIFY_TEMPLATE_CODE_ESCALATE;
        }
        return NOTIFY_TEMPLATE_CODE_WARN;
    }

    private List<Long> resolveNotifyAdminUserIds(AfterSaleReviewTicketDO ticket) {
        List<Long> roleUserIds = resolveRoleNotifyAdminUserIds();
        if (!roleUserIds.isEmpty()) {
            return roleUserIds;
        }
        if (ticket != null && StrUtil.isNotBlank(ticket.getCreator()) && StrUtil.isNumeric(ticket.getCreator())) {
            Long creatorId = Long.parseLong(ticket.getCreator());
            if (creatorId > 0) {
                return Collections.singletonList(creatorId);
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

    private String buildManualRetryAuditBizNo(Long operatorId, Long outboxId, String reason) {
        String operator = operatorId == null ? "SYSTEM" : String.valueOf(operatorId);
        String suffix = StrUtil.blankToDefault(StrUtil.trim(reason), "");
        if (StrUtil.isBlank(suffix)) {
            return StrUtil.maxLength(StrUtil.format("ADMIN#{}/OUTBOX#{}", operator, outboxId), 64);
        }
        return StrUtil.maxLength(StrUtil.format("ADMIN#{}/OUTBOX#{}#{}", operator, outboxId, suffix), 64);
    }

    private List<Long> normalizePositiveIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                uniqueIds.add(id);
            }
        }
        if (uniqueIds.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(uniqueIds);
    }

    private Integer resolveSlaMinutes(Integer requestSlaMinutes, Integer defaultSlaMinutes) {
        int value = ObjUtil.defaultIfNull(requestSlaMinutes, ObjUtil.defaultIfNull(defaultSlaMinutes, 120));
        if (value <= 0) {
            return ObjUtil.defaultIfNull(defaultSlaMinutes, 120);
        }
        return Math.min(value, 7 * 24 * 60);
    }

    private String buildEscalateRemark(AfterSaleReviewTicketDO ticket, LocalDateTime now, String newSeverity) {
        String baseRemark = StrUtil.blankToDefault(ticket.getRemark(), "");
        String deadline = ticket.getSlaDeadlineTime() == null ? "unknown"
                : ticket.getSlaDeadlineTime().format(ESCALATE_TIME_FORMATTER);
        String append = StrUtil.format("AUTO_ESCALATE_OVERDUE@{} from {} -> {}",
                now.format(ESCALATE_TIME_FORMATTER), deadline, newSeverity);
        if (StrUtil.isBlank(baseRemark)) {
            return append;
        }
        return baseRemark + " | " + append;
    }

    private String normalizeResolveActionCode(String resolveActionCode, String defaultCode) {
        return StrUtil.maxLength(StrUtil.blankToDefault(StrUtil.trim(resolveActionCode), defaultCode), 64);
    }

    private String normalizeActionCode(String actionCode, String defaultCode) {
        return StrUtil.maxLength(StrUtil.blankToDefault(StrUtil.trim(actionCode), defaultCode), 64);
    }

    private String normalizeResolveBizNo(String resolveBizNo, Long fallbackId) {
        return StrUtil.maxLength(StrUtil.blankToDefault(StrUtil.trim(resolveBizNo),
                fallbackId == null ? "" : String.valueOf(fallbackId)), 64);
    }

    private String normalizeActionBizNo(String actionBizNo, Long fallbackId) {
        return StrUtil.maxLength(StrUtil.blankToDefault(StrUtil.trim(actionBizNo),
                fallbackId == null ? "" : String.valueOf(fallbackId)), 64);
    }

    private String resolveSourceBizNo(AfterSaleReviewTicketCreateReqBO reqBO, Long fallbackId) {
        if (reqBO == null) {
            return "";
        }
        Long safeFallbackId = ObjUtil.defaultIfNull(reqBO.getAfterSaleId(), fallbackId);
        return StrUtil.blankToDefault(reqBO.getSourceBizNo(),
                safeFallbackId == null ? "" : String.valueOf(safeFallbackId));
    }

    private void applyRouteSnapshot(AfterSaleReviewTicketDO ticket, TicketRoute route) {
        if (ticket == null || route == null) {
            return;
        }
        ticket.setRouteId(route.getRouteId());
        ticket.setRouteScope(resolveRouteScope(route));
        ticket.setRouteDecisionOrder(resolveRouteDecisionOrder(route));
    }

    private String resolveRouteScope(TicketRoute route) {
        if (route == null) {
            return ROUTE_SCOPE_GLOBAL_FALLBACK;
        }
        return StrUtil.maxLength(StrUtil.blankToDefault(route.getMatchedScope(), ROUTE_SCOPE_GLOBAL_FALLBACK), 32);
    }

    private String resolveRouteDecisionOrder(TicketRoute route) {
        if (route == null) {
            return ROUTE_DECISION_ORDER;
        }
        return StrUtil.maxLength(StrUtil.blankToDefault(route.getDecisionOrder(),
                ROUTE_DECISION_ORDER), 128);
    }

    private String resolveRouteScope(Integer matchLevel) {
        if (TicketSlaRuleMatchLevelEnum.RULE.getCode().equals(matchLevel)) {
            return "RULE";
        }
        if (TicketSlaRuleMatchLevelEnum.TYPE_SEVERITY.getCode().equals(matchLevel)) {
            return "TYPE_SEVERITY";
        }
        if (TicketSlaRuleMatchLevelEnum.TYPE_DEFAULT.getCode().equals(matchLevel)) {
            return "TYPE_DEFAULT";
        }
        if (TicketSlaRuleMatchLevelEnum.GLOBAL_DEFAULT.getCode().equals(matchLevel)) {
            return "GLOBAL_DEFAULT";
        }
        return ROUTE_SCOPE_GLOBAL_FALLBACK;
    }

    private void normalizePageReq(AfterSaleReviewTicketPageReqVO pageReqVO) {
        if (pageReqVO == null) {
            return;
        }
        pageReqVO.setRouteScope(normalizeUpper(pageReqVO.getRouteScope()));
        pageReqVO.setSeverity(normalizeUpper(pageReqVO.getSeverity()));
        pageReqVO.setRuleCode(normalizeUpper(pageReqVO.getRuleCode()));
        pageReqVO.setEscalateTo(normalizeUpper(pageReqVO.getEscalateTo()));
        pageReqVO.setLastActionCode(normalizeUpper(pageReqVO.getLastActionCode()));
        pageReqVO.setResolveActionCode(normalizeUpper(pageReqVO.getResolveActionCode()));
        pageReqVO.setSourceBizNo(normalizeTrim(pageReqVO.getSourceBizNo()));
        pageReqVO.setLastActionBizNo(normalizeTrim(pageReqVO.getLastActionBizNo()));
        pageReqVO.setResolveBizNo(normalizeTrim(pageReqVO.getResolveBizNo()));
    }

    private void normalizeNotifyOutboxPageReq(AfterSaleReviewTicketNotifyOutboxPageReqVO pageReqVO) {
        if (pageReqVO == null) {
            return;
        }
        pageReqVO.setNotifyType(normalizeUpper(pageReqVO.getNotifyType()));
        pageReqVO.setChannel(normalizeUpper(pageReqVO.getChannel()));
        pageReqVO.setLastActionCode(normalizeUpper(pageReqVO.getLastActionCode()));
        pageReqVO.setLastActionBizNo(normalizeTrim(pageReqVO.getLastActionBizNo()));
    }

    private String normalizeUpper(String value) {
        String normalized = StrUtil.trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeTrim(String value) {
        return StrUtil.trimToNull(value);
    }

    @Data
    @AllArgsConstructor
    private static class TicketRoute {
        private String severity;
        private String escalateTo;
        private Integer slaMinutes;
        private Integer warnLeadMinutes;
        private Integer escalateDelayMinutes;
        private Long routeId;
        private String matchedScope;
        private String decisionOrder;
    }

}
