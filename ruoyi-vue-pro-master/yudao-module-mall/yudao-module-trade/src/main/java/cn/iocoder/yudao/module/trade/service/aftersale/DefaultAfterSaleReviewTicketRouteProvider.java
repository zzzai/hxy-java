package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketRouteMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
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
    private static final ReviewTicketRoute GLOBAL_DEFAULT_ROUTE = new ReviewTicketRoute(
            "P1", "HQ_AFTER_SALE", 120, null, "GLOBAL_DEFAULT_FALLBACK", ReviewTicketRoute.DECISION_ORDER);

    @Resource
    private AfterSaleReviewTicketRouteMapper routeMapper;

    private volatile RouteCache cache = RouteCache.empty();

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
            return new ReviewTicketRoute(normalize(row.getSeverity()), StrUtil.trim(row.getEscalateTo()),
                    row.getSlaMinutes(), row.getId(), normalize(row.getScope()), ReviewTicketRoute.DECISION_ORDER);
        }

    }

}
