package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleRefundRuleConfigDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleRefundRuleConfigMapper;
import cn.iocoder.yudao.module.trade.framework.aftersale.config.TradeAfterSaleRefundRuleProperties;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundRuleSnapshotBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AfterSaleRefundRuleResolverTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleRefundRuleResolver resolver;

    @Mock
    private AfterSaleRefundRuleConfigMapper afterSaleRefundRuleConfigMapper;

    @BeforeEach
    void setUp() {
        TradeAfterSaleRefundRuleProperties yaml = new TradeAfterSaleRefundRuleProperties();
        yaml.setEnabled(Boolean.TRUE);
        yaml.setAutoRefundMaxPrice(6000);
        yaml.setUserDailyApplyLimit(4);
        yaml.setBlacklistUserIds(new LinkedHashSet<>(Arrays.asList(100L, 200L)));
        yaml.setSuspiciousOrderKeywords(new LinkedHashSet<>(Arrays.asList("MOCK", "TEST")));
        ReflectionTestUtils.setField(resolver, "refundRuleProperties", yaml);
    }

    @Test
    void shouldUseDbRuleFirst() {
        AfterSaleRefundRuleConfigDO dbRule = new AfterSaleRefundRuleConfigDO();
        dbRule.setEnabled(Boolean.FALSE);
        dbRule.setAutoRefundMaxPrice(1000);
        dbRule.setUserDailyApplyLimit(1);
        dbRule.setBlacklistUserIds(Arrays.asList(9L, 10L));
        dbRule.setSuspiciousOrderKeywords(Arrays.asList("RISK", "FRAUD"));
        when(afterSaleRefundRuleConfigMapper.selectLatest()).thenReturn(dbRule);

        AfterSaleRefundRuleSnapshotBO result = resolver.resolve();

        assertEquals("DB", result.getSource());
        assertEquals(Boolean.FALSE, result.getEnabled());
        assertEquals(1000, result.getAutoRefundMaxPrice());
        assertTrue(result.getBlacklistUserIds().contains(9L));
        assertTrue(result.getSuspiciousOrderKeywords().contains("RISK"));
    }

    @Test
    void shouldFallbackToYamlWhenDbEmpty() {
        when(afterSaleRefundRuleConfigMapper.selectLatest()).thenReturn(null);

        AfterSaleRefundRuleSnapshotBO result = resolver.resolve();

        assertEquals("YAML", result.getSource());
        assertEquals(Boolean.TRUE, result.getEnabled());
        assertEquals(6000, result.getAutoRefundMaxPrice());
        assertEquals(4, result.getUserDailyApplyLimit());
        assertTrue(result.getBlacklistUserIds().contains(100L));
        assertTrue(result.getSuspiciousOrderKeywords().contains("MOCK"));
    }

}
