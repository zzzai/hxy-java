package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认工单路由策略
 */
@Component
public class DefaultAfterSaleReviewTicketRouteProvider implements AfterSaleReviewTicketRouteProvider {

    private static final ReviewTicketRoute GLOBAL_DEFAULT_ROUTE = new ReviewTicketRoute("P1", "HQ_AFTER_SALE", 120);

    private static final Map<String, ReviewTicketRoute> RULE_ROUTE_MAP = new HashMap<>();
    private static final Map<Integer, ReviewTicketRoute> TYPE_DEFAULT_ROUTE_MAP = new HashMap<>();
    private static final Map<String, ReviewTicketRoute> TYPE_SEVERITY_ROUTE_MAP = new HashMap<>();

    static {
        RULE_ROUTE_MAP.put(normalize(ruleKey("BLACKLIST_USER")), new ReviewTicketRoute("P0", "HQ_RISK_FINANCE", 30));
        RULE_ROUTE_MAP.put(normalize(ruleKey("SUSPICIOUS_ORDER")), new ReviewTicketRoute("P0", "HQ_RISK_FINANCE", 30));
        RULE_ROUTE_MAP.put(normalize(ruleKey("AMOUNT_OVER_LIMIT")), new ReviewTicketRoute("P1", "HQ_FINANCE", 120));
        RULE_ROUTE_MAP.put(normalize(ruleKey("HIGH_FREQUENCY")), new ReviewTicketRoute("P1", "HQ_AFTER_SALE", 120));
        RULE_ROUTE_MAP.put(normalize(ruleKey("AUTO_REFUND_EXECUTE_FAIL")), new ReviewTicketRoute("P0", "PAY_DEVOPS", 15));

        TYPE_DEFAULT_ROUTE_MAP.put(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType(),
                new ReviewTicketRoute("P1", "HQ_AFTER_SALE", 120));
        TYPE_DEFAULT_ROUTE_MAP.put(AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(),
                new ReviewTicketRoute("P1", "HQ_SERVICE_OPS", 90));
        TYPE_DEFAULT_ROUTE_MAP.put(AfterSaleReviewTicketTypeEnum.COMMISSION_DISPUTE.getType(),
                new ReviewTicketRoute("P1", "HQ_FINANCE", 120));

        TYPE_SEVERITY_ROUTE_MAP.put(typeSeverityKey(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType(), "P0"),
                new ReviewTicketRoute("P0", "HQ_RISK_FINANCE", 30));
        TYPE_SEVERITY_ROUTE_MAP.put(typeSeverityKey(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType(), "P1"),
                new ReviewTicketRoute("P1", "HQ_AFTER_SALE", 120));
        TYPE_SEVERITY_ROUTE_MAP.put(typeSeverityKey(AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(), "P0"),
                new ReviewTicketRoute("P0", "HQ_SERVICE_OPS", 30));
        TYPE_SEVERITY_ROUTE_MAP.put(typeSeverityKey(AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(), "P1"),
                new ReviewTicketRoute("P1", "HQ_SERVICE_OPS", 90));
        TYPE_SEVERITY_ROUTE_MAP.put(typeSeverityKey(AfterSaleReviewTicketTypeEnum.COMMISSION_DISPUTE.getType(), "P0"),
                new ReviewTicketRoute("P0", "HQ_FINANCE", 30));
        TYPE_SEVERITY_ROUTE_MAP.put(typeSeverityKey(AfterSaleReviewTicketTypeEnum.COMMISSION_DISPUTE.getType(), "P1"),
                new ReviewTicketRoute("P1", "HQ_FINANCE", 120));
    }

    @Override
    public ReviewTicketRoute resolve(Integer ticketType, String preferredSeverity, String ruleCode) {
        ReviewTicketRoute byRule = RULE_ROUTE_MAP.get(normalize(ruleKey(ruleCode)));
        if (byRule != null) {
            return byRule;
        }

        ReviewTicketRoute byTypeSeverity = TYPE_SEVERITY_ROUTE_MAP.get(typeSeverityKey(ticketType, preferredSeverity));
        if (byTypeSeverity != null) {
            return byTypeSeverity;
        }

        ReviewTicketRoute byType = TYPE_DEFAULT_ROUTE_MAP.get(ticketType);
        if (byType != null) {
            return byType;
        }
        return GLOBAL_DEFAULT_ROUTE;
    }

    private static String ruleKey(String ruleCode) {
        return StrUtil.blankToDefault(ruleCode, "");
    }

    private static String typeSeverityKey(Integer ticketType, String severity) {
        Integer safeTicketType = ObjUtil.defaultIfNull(ticketType, 0);
        String safeSeverity = normalize(StrUtil.blankToDefault(severity, ""));
        return safeTicketType + "#" + safeSeverity;
    }

    private static String normalize(String value) {
        return StrUtil.trim(ObjUtil.defaultIfNull(value, "")).toUpperCase(Locale.ROOT);
    }

}
