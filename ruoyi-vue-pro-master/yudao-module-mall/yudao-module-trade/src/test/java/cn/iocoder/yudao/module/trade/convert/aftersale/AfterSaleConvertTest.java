package cn.iocoder.yudao.module.trade.convert.aftersale;

import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.AfterSaleDetailRespVO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AfterSaleConvertTest {

    @Test
    void shouldParseRefundLimitDetailWhenJsonValid() {
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setRefundLimitDetailJson("{\"upperBound\":2600,\"bundleChildren\":[{\"childCode\":\"A\"}]}");

        Map<String, Object> detail = AfterSaleConvert.INSTANCE
                .convert(afterSale, null, null, null, Collections.emptyList())
                .getRefundLimitDetail();

        assertNotNull(detail);
        assertEquals(2600, ((Number) detail.get("upperBound")).intValue());
        assertNotNull(detail.get("bundleChildren"));
    }

    @Test
    void shouldReturnNullWhenRefundLimitDetailJsonInvalid() {
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setRefundLimitDetailJson("{\"upperBound\":");

        Map<String, Object> detail = AfterSaleConvert.INSTANCE
                .convert(afterSale, null, null, null, Collections.emptyList())
                .getRefundLimitDetail();

        assertNull(detail);
    }

    @Test
    void shouldSetRefundLimitSourceLabelAndRuleHint() {
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setRefundLimitSource("SERVICE_ORDER_SNAPSHOT");

        AfterSaleDetailRespVO resp = AfterSaleConvert.INSTANCE.convert(afterSale, null, null, null, Collections.emptyList());

        assertEquals("服务履约快照口径", resp.getRefundLimitSourceLabel());
        assertEquals("按服务履约快照中的可退上限执行退款校验", resp.getRefundLimitRuleHint());
    }

    @Test
    void shouldFallbackLabelAndRuleHintWhenSourceUnknown() {
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setRefundLimitSource("UNKNOWN_SOURCE");

        AfterSaleDetailRespVO resp = AfterSaleConvert.INSTANCE.convert(afterSale, null, null, null, Collections.emptyList());

        assertEquals("UNKNOWN_SOURCE", resp.getRefundLimitSourceLabel());
        assertEquals("按退款上限来源字段校验", resp.getRefundLimitRuleHint());
    }
}
