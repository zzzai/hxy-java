package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketRouteMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DefaultAfterSaleReviewTicketRouteProviderTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DefaultAfterSaleReviewTicketRouteProvider provider;

    @Mock
    private AfterSaleReviewTicketRouteMapper routeMapper;

    @Test
    void shouldResolveByDbRuleCodeFirst() {
        when(routeMapper.selectListByEnabled(Boolean.TRUE)).thenReturn(List.of(
                buildRoute(1L, AfterSaleReviewTicketRouteScopeEnum.RULE.getScope(), "BLACKLIST_USER",
                        0, "P0", "HQ_CUSTOM_RISK", 25),
                buildRoute(2L, AfterSaleReviewTicketRouteScopeEnum.TYPE_DEFAULT.getScope(), "",
                        AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(), "P1", "HQ_SERVICE_OPS", 90)
        ));
        ReviewTicketRoute route = provider.resolve(
                AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(),
                "P1",
                "BLACKLIST_USER");

        assertEquals("P0", route.getSeverity());
        assertEquals("HQ_CUSTOM_RISK", route.getEscalateTo());
        assertEquals(25, route.getSlaMinutes());
    }

    @Test
    void shouldResolveByDbTicketTypeAndSeverityWhenRuleUnknown() {
        when(routeMapper.selectListByEnabled(Boolean.TRUE)).thenReturn(List.of(
                buildRoute(3L, AfterSaleReviewTicketRouteScopeEnum.TYPE_SEVERITY.getScope(), "",
                        AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(), "P0", "HQ_SERVICE_P0", 20)
        ));
        ReviewTicketRoute route = provider.resolve(
                AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(),
                "P0",
                "UNKNOWN_RULE");

        assertEquals("P0", route.getSeverity());
        assertEquals("HQ_SERVICE_P0", route.getEscalateTo());
        assertEquals(20, route.getSlaMinutes());
    }

    @Test
    void shouldFallbackToDbTicketTypeDefaultRoute() {
        when(routeMapper.selectListByEnabled(Boolean.TRUE)).thenReturn(List.of(
                buildRoute(4L, AfterSaleReviewTicketRouteScopeEnum.TYPE_DEFAULT.getScope(), "",
                        AfterSaleReviewTicketTypeEnum.COMMISSION_DISPUTE.getType(), "P1", "HQ_FINANCE_SLA", 95)
        ));
        ReviewTicketRoute route = provider.resolve(
                AfterSaleReviewTicketTypeEnum.COMMISSION_DISPUTE.getType(),
                null,
                "UNKNOWN_RULE");

        assertEquals("P1", route.getSeverity());
        assertEquals("HQ_FINANCE_SLA", route.getEscalateTo());
        assertEquals(95, route.getSlaMinutes());
    }

    @Test
    void shouldFallbackToBuiltInDefaultRouteWhenDbEmpty() {
        when(routeMapper.selectListByEnabled(Boolean.TRUE)).thenReturn(Collections.emptyList());
        ReviewTicketRoute route = provider.resolve(null, null, "UNKNOWN_RULE");

        assertEquals("P1", route.getSeverity());
        assertEquals("HQ_AFTER_SALE", route.getEscalateTo());
        assertEquals(120, route.getSlaMinutes());
    }

    private static AfterSaleReviewTicketRouteDO buildRoute(Long id, String scope, String ruleCode, Integer ticketType,
                                                           String severity, String escalateTo, Integer slaMinutes) {
        AfterSaleReviewTicketRouteDO route = new AfterSaleReviewTicketRouteDO();
        route.setId(id);
        route.setScope(scope);
        route.setRuleCode(ruleCode);
        route.setTicketType(ticketType);
        route.setSeverity(severity);
        route.setEscalateTo(escalateTo);
        route.setSlaMinutes(slaMinutes);
        route.setEnabled(Boolean.TRUE);
        route.setSort(0);
        route.setRemark("");
        return route;
    }

}
