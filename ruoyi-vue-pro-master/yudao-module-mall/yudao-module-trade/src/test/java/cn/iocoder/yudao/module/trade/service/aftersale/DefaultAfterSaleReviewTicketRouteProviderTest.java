package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultAfterSaleReviewTicketRouteProviderTest {

    private final DefaultAfterSaleReviewTicketRouteProvider provider = new DefaultAfterSaleReviewTicketRouteProvider();

    @Test
    void shouldResolveByRuleCodeFirst() {
        ReviewTicketRoute route = provider.resolve(
                AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(),
                "P1",
                "BLACKLIST_USER");

        assertEquals("P0", route.getSeverity());
        assertEquals("HQ_RISK_FINANCE", route.getEscalateTo());
        assertEquals(30, route.getSlaMinutes());
    }

    @Test
    void shouldResolveByTicketTypeAndSeverityWhenRuleUnknown() {
        ReviewTicketRoute route = provider.resolve(
                AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(),
                "P0",
                "UNKNOWN_RULE");

        assertEquals("P0", route.getSeverity());
        assertEquals("HQ_SERVICE_OPS", route.getEscalateTo());
        assertEquals(30, route.getSlaMinutes());
    }

    @Test
    void shouldFallbackToTicketTypeDefaultRoute() {
        ReviewTicketRoute route = provider.resolve(
                AfterSaleReviewTicketTypeEnum.COMMISSION_DISPUTE.getType(),
                null,
                "UNKNOWN_RULE");

        assertEquals("P1", route.getSeverity());
        assertEquals("HQ_FINANCE", route.getEscalateTo());
        assertEquals(120, route.getSlaMinutes());
    }

    @Test
    void shouldFallbackToGlobalDefaultRoute() {
        ReviewTicketRoute route = provider.resolve(null, null, "UNKNOWN_RULE");

        assertEquals("P1", route.getSeverity());
        assertEquals("HQ_AFTER_SALE", route.getEscalateTo());
        assertEquals(120, route.getSlaMinutes());
    }

}
