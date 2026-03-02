package cn.iocoder.yudao.module.trade.convert.aftersale;

import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
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
}

