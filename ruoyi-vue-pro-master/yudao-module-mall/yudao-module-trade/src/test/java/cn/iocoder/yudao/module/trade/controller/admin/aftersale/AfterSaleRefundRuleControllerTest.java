package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule.AfterSaleRefundRuleRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule.AfterSaleRefundRuleSaveReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleRefundRuleConfigDO;
import cn.iocoder.yudao.module.trade.framework.aftersale.config.TradeAfterSaleRefundRuleProperties;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleRefundRuleConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleRefundRuleControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleRefundRuleController controller;

    @Mock
    private AfterSaleRefundRuleConfigService afterSaleRefundRuleConfigService;

    @BeforeEach
    void setUp() {
        TradeAfterSaleRefundRuleProperties properties = new TradeAfterSaleRefundRuleProperties();
        properties.setEnabled(Boolean.TRUE);
        properties.setAutoRefundMaxPrice(8000);
        properties.setUserDailyApplyLimit(5);
        properties.setBlacklistUserIds(new LinkedHashSet<>(Arrays.asList(1001L, 1002L)));
        properties.setSuspiciousOrderKeywords(new LinkedHashSet<>(Arrays.asList("TEST", "MOCK")));
        ReflectionTestUtils.setField(controller, "refundRuleProperties", properties);
    }

    @Test
    void shouldGetRuleFromDb() {
        AfterSaleRefundRuleConfigDO dbRule = new AfterSaleRefundRuleConfigDO();
        dbRule.setId(1L);
        dbRule.setEnabled(Boolean.FALSE);
        dbRule.setAutoRefundMaxPrice(3000);
        dbRule.setUserDailyApplyLimit(2);
        dbRule.setBlacklistUserIds(Arrays.asList(9L, 10L));
        dbRule.setSuspiciousOrderKeywords(Arrays.asList("RISK", "HIGH_FREQ"));
        dbRule.setRuleVersion("v2");
        dbRule.setRemark("db-rule");
        when(afterSaleRefundRuleConfigService.getLatestDbRule()).thenReturn(dbRule);

        CommonResult<AfterSaleRefundRuleRespVO> result = controller.getRule();

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("DB", result.getData().getSource());
        assertEquals(1L, result.getData().getId());
        assertEquals(3000, result.getData().getAutoRefundMaxPrice());
        assertEquals("v2", result.getData().getRuleVersion());
        verify(afterSaleRefundRuleConfigService).getLatestDbRule();
    }

    @Test
    void shouldFallbackToYamlWhenDbRuleMissing() {
        when(afterSaleRefundRuleConfigService.getLatestDbRule()).thenReturn(null);

        CommonResult<AfterSaleRefundRuleRespVO> result = controller.getRule();

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("YAML", result.getData().getSource());
        assertEquals(Boolean.TRUE, result.getData().getEnabled());
        assertEquals(8000, result.getData().getAutoRefundMaxPrice());
        assertEquals(5, result.getData().getUserDailyApplyLimit());
        assertEquals("yaml-default", result.getData().getRuleVersion());
        assertEquals("当前无 DB 规则，使用 YAML 兜底规则", result.getData().getRemark());
        assertTrue(result.getData().getBlacklistUserIds().contains(1001L));
        assertTrue(result.getData().getSuspiciousOrderKeywords().contains("TEST"));
        verify(afterSaleRefundRuleConfigService).getLatestDbRule();
    }

    @Test
    void shouldSaveRule() {
        AfterSaleRefundRuleSaveReqVO reqVO = new AfterSaleRefundRuleSaveReqVO();
        reqVO.setEnabled(Boolean.TRUE);
        reqVO.setAutoRefundMaxPrice(5000);
        reqVO.setUserDailyApplyLimit(3);
        reqVO.setBlacklistUserIds(Arrays.asList(11L, 12L));
        reqVO.setSuspiciousOrderKeywords(Arrays.asList("MOCK"));
        reqVO.setRuleVersion("v3");
        reqVO.setRemark("manual-adjust");
        when(afterSaleRefundRuleConfigService.saveRule(reqVO)).thenReturn(100L);

        CommonResult<Long> result = controller.saveRule(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(100L, result.getData());
        verify(afterSaleRefundRuleConfigService).saveRule(reqVO);
    }

}
