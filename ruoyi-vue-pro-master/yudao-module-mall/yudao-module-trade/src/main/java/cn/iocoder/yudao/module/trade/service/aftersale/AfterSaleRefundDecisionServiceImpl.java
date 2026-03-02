package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderLogDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleOperateTypeEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleLogCreateReqBO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundRuleSnapshotBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_REFUND_NEED_MANUAL_REVIEW;

/**
 * 售后退款风控决策服务实现
 */
@Service
@Validated
@Slf4j
public class AfterSaleRefundDecisionServiceImpl implements AfterSaleRefundDecisionService {

    @Resource
    private AfterSaleMapper afterSaleMapper;
    @Resource
    private AfterSaleLogService afterSaleLogService;
    @Resource
    private AfterSaleRefundRuleResolver afterSaleRefundRuleResolver;
    @Resource
    private AfterSaleReviewTicketService afterSaleReviewTicketService;

    @Override
    public AfterSaleRefundDecisionBO evaluate(AfterSaleDO afterSale) {
        if (afterSale == null) {
            return AfterSaleRefundDecisionBO.manual("AFTER_SALE_NOT_FOUND", "售后单不存在");
        }
        AfterSaleRefundRuleSnapshotBO rule = afterSaleRefundRuleResolver.resolve();
        if (!Boolean.TRUE.equals(rule.getEnabled())) {
            return AfterSaleRefundDecisionBO.auto("RULE_DISABLED",
                    StrUtil.format("退款风控规则未启用（source={}）", rule.getSource()));
        }
        if (isBlackUser(afterSale.getUserId(), rule.getBlacklistUserIds())) {
            return AfterSaleRefundDecisionBO.manual("BLACKLIST_USER", "用户命中退款黑名单");
        }
        Integer refundPrice = ObjectUtil.defaultIfNull(afterSale.getRefundPrice(), 0);
        Integer maxAutoPrice = ObjectUtil.defaultIfNull(rule.getAutoRefundMaxPrice(), 0);
        if (refundPrice > maxAutoPrice) {
            return AfterSaleRefundDecisionBO.manual("AMOUNT_OVER_LIMIT",
                    StrUtil.format("退款金额{}元超过自动退款阈值{}元",
                            toYuan(refundPrice), toYuan(maxAutoPrice)));
        }
        String keyword = hitSuspiciousKeyword(afterSale.getOrderNo(), rule.getSuspiciousOrderKeywords());
        if (StrUtil.isNotBlank(keyword)) {
            return AfterSaleRefundDecisionBO.manual("SUSPICIOUS_ORDER",
                    StrUtil.format("订单号命中可疑关键字：{}", keyword));
        }
        Integer dailyLimit = ObjectUtil.defaultIfNull(rule.getUserDailyApplyLimit(), 0);
        if (dailyLimit > 0) {
            long todayCount = countTodayAfterSaleApply(afterSale.getUserId());
            if (todayCount > dailyLimit) {
                return AfterSaleRefundDecisionBO.manual("HIGH_FREQUENCY",
                        StrUtil.format("用户当日售后申请{}笔，超过阈值{}笔", todayCount, dailyLimit));
            }
        }
        return AfterSaleRefundDecisionBO.auto("AUTO_PASS", "命中自动退款规则");
    }

    @Override
    public AfterSaleRefundDecisionBO checkAndAuditForExecution(Long operatorId, Integer operatorType,
                                                               AfterSaleDO afterSale, boolean forcePass) {
        AfterSaleRefundDecisionBO decision = evaluate(afterSale);
        if (Boolean.TRUE.equals(decision.getAutoPass())) {
            auditDecision(operatorId, operatorType, afterSale, decision, false);
            return decision;
        }
        if (forcePass) {
            auditDecision(operatorId, operatorType, afterSale, decision, true);
            return decision;
        }
        auditDecision(operatorId, operatorType, afterSale, decision, false);
        throw exception(AFTER_SALE_REFUND_NEED_MANUAL_REVIEW, decision.getReason());
    }

    @Override
    public void auditDecision(Long operatorId, Integer operatorType, AfterSaleDO afterSale,
                              AfterSaleRefundDecisionBO decision, boolean forcePass) {
        if (afterSale == null || decision == null) {
            return;
        }
        AfterSaleOperateTypeEnum operateType = resolveOperateType(decision, forcePass);
        String content = StrUtil.format(operateType.getContent(), MapUtil.<String, Object>builder()
                .put("rule", decision.getRuleCode())
                .put("reason", decision.getReason())
                .build());
        AfterSaleLogCreateReqBO createReqBO = new AfterSaleLogCreateReqBO(
                ObjectUtil.defaultIfNull(operatorId, TradeOrderLogDO.USER_ID_SYSTEM),
                ObjectUtil.defaultIfNull(operatorType, TradeOrderLogDO.USER_TYPE_SYSTEM),
                afterSale.getId(),
                afterSale.getStatus(),
                afterSale.getStatus(),
                operateType.getType(),
                content);
        afterSaleLogService.createAfterSaleLog(createReqBO);
        if (Boolean.TRUE.equals(decision.getAutoPass())) {
            afterSaleReviewTicketService.resolveManualReviewTicket(afterSale.getId(), operatorId, operatorType,
                    "AUTO_PASS_RULE", "AFTER_SALE#" + afterSale.getId(), "auto-pass");
            return;
        }
        if (forcePass) {
            afterSaleReviewTicketService.resolveManualReviewTicket(afterSale.getId(), operatorId, operatorType,
                    "FORCE_PASS_MANUAL", "AFTER_SALE#" + afterSale.getId(), "force-pass");
            return;
        }
        afterSaleReviewTicketService.upsertManualReviewTicket(afterSale, decision);
    }

    private AfterSaleOperateTypeEnum resolveOperateType(AfterSaleRefundDecisionBO decision, boolean forcePass) {
        if (Boolean.TRUE.equals(decision.getAutoPass())) {
            return AfterSaleOperateTypeEnum.SYSTEM_REFUND_RULE_AUTO_PASS;
        }
        if (forcePass) {
            return AfterSaleOperateTypeEnum.ADMIN_REFUND_RULE_FORCE_PASS;
        }
        return AfterSaleOperateTypeEnum.SYSTEM_REFUND_RULE_MANUAL_REVIEW;
    }

    private boolean isBlackUser(Long userId, java.util.Set<Long> blacklistUserIds) {
        if (userId == null || blacklistUserIds == null || blacklistUserIds.isEmpty()) {
            return false;
        }
        return blacklistUserIds.contains(userId);
    }

    private String hitSuspiciousKeyword(String orderNo, java.util.Set<String> suspiciousOrderKeywords) {
        if (StrUtil.isBlank(orderNo) || suspiciousOrderKeywords == null || suspiciousOrderKeywords.isEmpty()) {
            return null;
        }
        for (String keyword : suspiciousOrderKeywords) {
            if (StrUtil.isNotBlank(keyword) && StrUtil.containsIgnoreCase(orderNo, keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private long countTodayAfterSaleApply(Long userId) {
        if (userId == null) {
            return 0L;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime begin = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        Long count = afterSaleMapper.selectCountByUserIdAndCreateTimeBetween(userId, begin, end);
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    private BigDecimal toYuan(Integer fen) {
        return MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(fen, 0));
    }

}
