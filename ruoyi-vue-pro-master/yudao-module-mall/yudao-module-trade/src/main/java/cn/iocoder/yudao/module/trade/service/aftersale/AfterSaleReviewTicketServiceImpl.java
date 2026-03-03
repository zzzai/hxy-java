package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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
public class AfterSaleReviewTicketServiceImpl implements AfterSaleReviewTicketService {

    private static final int DEFAULT_ESCALATE_BATCH_LIMIT = 200;
    private static final DateTimeFormatter ESCALATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_RESOLVE_ACTION_MANUAL = "MANUAL_RESOLVE";
    private static final String DEFAULT_RESOLVE_ACTION_AUTO = "AUTO_RESOLVE";
    private static final String ACTION_TICKET_CREATE = "TICKET_CREATE";
    private static final String ACTION_RULE_RETRIGGER = "RULE_RETRIGGER";
    private static final String ACTION_SLA_AUTO_ESCALATE = "SLA_AUTO_ESCALATE";

    @Resource
    private AfterSaleReviewTicketMapper afterSaleReviewTicketMapper;
    @Resource
    private AfterSaleReviewTicketRouteProvider reviewTicketRouteProvider;

    @Override
    public PageResult<AfterSaleReviewTicketDO> getReviewTicketPage(AfterSaleReviewTicketPageReqVO pageReqVO) {
        normalizePageReq(pageReqVO);
        return afterSaleReviewTicketMapper.selectPage(pageReqVO);
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
        ReviewTicketRoute route = reviewTicketRouteProvider.resolve(ticketType, reqBO.getSeverity(), reqBO.getRuleCode());
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
    public void upsertManualReviewTicket(AfterSaleDO afterSale, AfterSaleRefundDecisionBO decision) {
        if (afterSale == null || decision == null || afterSale.getId() == null) {
            return;
        }
        AfterSaleReviewTicketDO existed = afterSaleReviewTicketMapper.selectByAfterSaleId(afterSale.getId());
        ReviewTicketRoute route = reviewTicketRouteProvider.resolve(
                AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType(),
                existed == null ? null : existed.getSeverity(),
                decision.getRuleCode());
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
        int safeLimit = Math.max(1, Math.min(ObjUtil.defaultIfNull(limit, DEFAULT_ESCALATE_BATCH_LIMIT), 1000));
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
            ReviewTicketRoute route = reviewTicketRouteProvider.resolve(
                    ticket.getTicketType(), newSeverity, ticket.getRuleCode());
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
            affectedRows += updateCount;
        }
        return affectedRows;
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
        return StrUtil.maxLength(StrUtil.blankToDefault(resolveActionCode, defaultCode), 64);
    }

    private String normalizeResolveBizNo(String resolveBizNo, Long fallbackId) {
        return StrUtil.maxLength(StrUtil.blankToDefault(resolveBizNo,
                fallbackId == null ? "" : String.valueOf(fallbackId)), 64);
    }

    private String normalizeActionBizNo(String actionBizNo, Long fallbackId) {
        return StrUtil.maxLength(StrUtil.blankToDefault(actionBizNo,
                fallbackId == null ? "" : String.valueOf(fallbackId)), 64);
    }

    private void applyRouteSnapshot(AfterSaleReviewTicketDO ticket, ReviewTicketRoute route) {
        if (ticket == null || route == null) {
            return;
        }
        ticket.setRouteId(route.getRouteId());
        ticket.setRouteScope(resolveRouteScope(route));
        ticket.setRouteDecisionOrder(resolveRouteDecisionOrder(route));
    }

    private String resolveRouteScope(ReviewTicketRoute route) {
        if (route == null) {
            return "GLOBAL_DEFAULT_FALLBACK";
        }
        return StrUtil.maxLength(StrUtil.blankToDefault(route.getMatchedScope(), "GLOBAL_DEFAULT_FALLBACK"), 32);
    }

    private String resolveRouteDecisionOrder(ReviewTicketRoute route) {
        if (route == null) {
            return ReviewTicketRoute.DECISION_ORDER;
        }
        return StrUtil.maxLength(StrUtil.blankToDefault(route.getDecisionOrder(),
                ReviewTicketRoute.DECISION_ORDER), 128);
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
        pageReqVO.setSourceBizNo(normalizeTrim(pageReqVO.getSourceBizNo()));
        pageReqVO.setLastActionBizNo(normalizeTrim(pageReqVO.getLastActionBizNo()));
    }

    private String normalizeUpper(String value) {
        String normalized = StrUtil.trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeTrim(String value) {
        return StrUtil.trimToNull(value);
    }

}
