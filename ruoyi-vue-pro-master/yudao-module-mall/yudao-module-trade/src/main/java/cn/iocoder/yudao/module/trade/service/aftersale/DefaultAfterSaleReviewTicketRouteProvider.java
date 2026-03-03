package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketRouteMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认工单路由策略
 */
@Component
public class DefaultAfterSaleReviewTicketRouteProvider implements AfterSaleReviewTicketRouteProvider {

    private static final long CACHE_REFRESH_MILLIS = 30_000L;
    private static final ReviewTicketRoute GLOBAL_DEFAULT_ROUTE = new ReviewTicketRoute("P1", "HQ_AFTER_SALE", 120);

    private static final Map<String, ReviewTicketRoute> RULE_ROUTE_MAP = new HashMap<>();
    private static final Map<Integer, ReviewTicketRoute> TYPE_DEFAULT_ROUTE_MAP = new HashMap<>();
    private static final Map<String, ReviewTicketRoute> TYPE_SEVERITY_ROUTE_MAP = new HashMap<>();

    @Resource
    private AfterSaleReviewTicketRouteMapper routeMapper;

    private volatile RouteCache cache = RouteCache.empty();

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
        RouteCache snapshot = getCache();
        ReviewTicketRoute byRule = snapshot.ruleRouteMap.get(normalize(ruleKey(ruleCode)));
        if (byRule != null) {
            return byRule;
        }
        ReviewTicketRoute byTypeSeverity = snapshot.typeSeverityRouteMap.get(typeSeverityKey(ticketType, preferredSeverity));
        if (byTypeSeverity != null) {
            return byTypeSeverity;
        }
        ReviewTicketRoute byType = snapshot.typeDefaultRouteMap.get(ticketType);
        if (byType != null) {
            return byType;
        }
        if (snapshot.globalDefaultRoute != null) {
            return snapshot.globalDefaultRoute;
        }

        byRule = RULE_ROUTE_MAP.get(normalize(ruleKey(ruleCode)));
        if (byRule != null) {
            return byRule;
        }

        ReviewTicketRoute fallbackByTypeSeverity = TYPE_SEVERITY_ROUTE_MAP.get(typeSeverityKey(ticketType, preferredSeverity));
        if (fallbackByTypeSeverity != null) {
            return fallbackByTypeSeverity;
        }

        ReviewTicketRoute fallbackByType = TYPE_DEFAULT_ROUTE_MAP.get(ticketType);
        if (fallbackByType != null) {
            return fallbackByType;
        }
        return GLOBAL_DEFAULT_ROUTE;
    }

    @Override
    public void invalidateCache() {
        cache = RouteCache.empty();
    }

    private RouteCache getCache() {
        long now = System.currentTimeMillis();
        RouteCache current = this.cache;
        if (current.loaded && now - current.loadedAt < CACHE_REFRESH_MILLIS) {
            return current;
        }
        synchronized (this) {
            current = this.cache;
            if (current.loaded && now - current.loadedAt < CACHE_REFRESH_MILLIS) {
                return current;
            }
            List<AfterSaleReviewTicketRouteDO> dbList = routeMapper == null
                    ? Collections.emptyList() : routeMapper.selectListByEnabled(Boolean.TRUE);
            RouteCache refreshed = RouteCache.fromList(dbList, now);
            this.cache = refreshed;
            return refreshed;
        }
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

    private static final class RouteCache {

        private final boolean loaded;
        private final long loadedAt;
        private final Map<String, ReviewTicketRoute> ruleRouteMap;
        private final Map<Integer, ReviewTicketRoute> typeDefaultRouteMap;
        private final Map<String, ReviewTicketRoute> typeSeverityRouteMap;
        private final ReviewTicketRoute globalDefaultRoute;

        private RouteCache(boolean loaded, long loadedAt, Map<String, ReviewTicketRoute> ruleRouteMap,
                           Map<Integer, ReviewTicketRoute> typeDefaultRouteMap,
                           Map<String, ReviewTicketRoute> typeSeverityRouteMap, ReviewTicketRoute globalDefaultRoute) {
            this.loaded = loaded;
            this.loadedAt = loadedAt;
            this.ruleRouteMap = ruleRouteMap;
            this.typeDefaultRouteMap = typeDefaultRouteMap;
            this.typeSeverityRouteMap = typeSeverityRouteMap;
            this.globalDefaultRoute = globalDefaultRoute;
        }

        private static RouteCache empty() {
            return new RouteCache(false, 0L, new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
        }

        private static RouteCache fromList(List<AfterSaleReviewTicketRouteDO> list, long loadedAt) {
            Map<String, ReviewTicketRoute> ruleMap = new HashMap<>();
            Map<Integer, ReviewTicketRoute> typeMap = new HashMap<>();
            Map<String, ReviewTicketRoute> typeSeverityMap = new HashMap<>();
            ReviewTicketRoute global = null;
            for (AfterSaleReviewTicketRouteDO row : list) {
                if (row == null || !Boolean.TRUE.equals(row.getEnabled())) {
                    continue;
                }
                ReviewTicketRoute route = toRoute(row);
                if (route == null) {
                    continue;
                }
                String scope = normalize(row.getScope());
                if (StrUtil.equals(scope, AfterSaleReviewTicketRouteScopeEnum.RULE.getScope())) {
                    if (StrUtil.isBlank(row.getRuleCode())) {
                        continue;
                    }
                    ruleMap.put(normalize(row.getRuleCode()), route);
                    continue;
                }
                if (StrUtil.equals(scope, AfterSaleReviewTicketRouteScopeEnum.TYPE_SEVERITY.getScope())) {
                    if (row.getTicketType() == null || row.getTicketType() <= 0 || StrUtil.isBlank(row.getSeverity())) {
                        continue;
                    }
                    typeSeverityMap.put(typeSeverityKey(row.getTicketType(), row.getSeverity()), route);
                    continue;
                }
                if (StrUtil.equals(scope, AfterSaleReviewTicketRouteScopeEnum.TYPE_DEFAULT.getScope())) {
                    if (row.getTicketType() == null || row.getTicketType() <= 0) {
                        continue;
                    }
                    typeMap.put(row.getTicketType(), route);
                    continue;
                }
                if (StrUtil.equals(scope, AfterSaleReviewTicketRouteScopeEnum.GLOBAL_DEFAULT.getScope())) {
                    global = route;
                }
            }
            return new RouteCache(true, loadedAt, ruleMap, typeMap, typeSeverityMap, global);
        }

        private static ReviewTicketRoute toRoute(AfterSaleReviewTicketRouteDO row) {
            if (StrUtil.isBlank(row.getSeverity()) || StrUtil.isBlank(row.getEscalateTo())
                    || row.getSlaMinutes() == null || row.getSlaMinutes() <= 0) {
                return null;
            }
            return new ReviewTicketRoute(normalize(row.getSeverity()), StrUtil.trim(row.getEscalateTo()), row.getSlaMinutes());
        }

    }

}
