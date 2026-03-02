package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleRefundRuleConfigDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleRefundRuleConfigMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleOperateTypeEnum;
import cn.iocoder.yudao.module.trade.framework.aftersale.config.TradeAfterSaleRefundRuleProperties;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleLogCreateReqBO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleRefundDecisionServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleRefundDecisionServiceImpl service;

    @Mock
    private AfterSaleMapper afterSaleMapper;
    @Mock
    private AfterSaleLogService afterSaleLogService;
    @Mock
    private AfterSaleRefundRuleConfigMapper afterSaleRefundRuleConfigMapper;
    @Mock
    private AfterSaleReviewTicketService afterSaleReviewTicketService;

    private TradeAfterSaleRefundRuleProperties refundRuleProperties;

    @BeforeEach
    void setUp() {
        refundRuleProperties = new TradeAfterSaleRefundRuleProperties();
        refundRuleProperties.setEnabled(Boolean.TRUE);
        refundRuleProperties.setAutoRefundMaxPrice(5000);
        refundRuleProperties.setUserDailyApplyLimit(3);
        refundRuleProperties.setBlacklistUserIds(new LinkedHashSet<>());
        refundRuleProperties.setSuspiciousOrderKeywords(new LinkedHashSet<>());
        AfterSaleRefundRuleResolver resolver = new AfterSaleRefundRuleResolver();
        ReflectionTestUtils.setField(resolver, "afterSaleRefundRuleConfigMapper", afterSaleRefundRuleConfigMapper);
        ReflectionTestUtils.setField(resolver, "refundRuleProperties", refundRuleProperties);
        ReflectionTestUtils.setField(service, "afterSaleRefundRuleResolver", resolver);
        when(afterSaleRefundRuleConfigMapper.selectLatest()).thenReturn(null);
    }

    @Test
    void shouldAutoPassWhenAmountWithinLimit() {
        when(afterSaleMapper.selectCountByUserIdAndCreateTimeBetween(anyLong(), any(), any())).thenReturn(1L);
        AfterSaleDO afterSale = buildAfterSale(1L, 1000, "ORDER_001");

        AfterSaleRefundDecisionBO decision = service.evaluate(afterSale);

        assertTrue(Boolean.TRUE.equals(decision.getAutoPass()));
        assertEquals("AUTO_PASS", decision.getRuleCode());
    }

    @Test
    void shouldNeedManualReviewWhenAmountOverLimit() {
        when(afterSaleRefundRuleConfigMapper.selectLatest()).thenReturn(buildDbRule(true, 5000, 3,
                Collections.emptyList(), Collections.emptyList()));
        AfterSaleDO afterSale = buildAfterSale(2L, 8000, "ORDER_002");

        AfterSaleRefundDecisionBO decision = service.evaluate(afterSale);

        assertFalse(Boolean.TRUE.equals(decision.getAutoPass()));
        assertEquals("AMOUNT_OVER_LIMIT", decision.getRuleCode());
    }

    @Test
    void shouldThrowManualReviewWhenBlackUserAndNotForcePass() {
        Set<Long> blacklist = new LinkedHashSet<>();
        blacklist.add(66L);
        when(afterSaleRefundRuleConfigMapper.selectLatest()).thenReturn(buildDbRule(true, 5000, 3,
                new ArrayList<>(blacklist), Collections.emptyList()));
        AfterSaleDO afterSale = buildAfterSale(66L, 100, "ORDER_003");
        afterSale.setId(99L);
        afterSale.setStatus(10);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.checkAndAuditForExecution(10L, UserTypeEnum.ADMIN.getValue(), afterSale, false));
        assertTrue(exception.getMessage().contains("人工复核"));

        ArgumentCaptor<AfterSaleLogCreateReqBO> captor = ArgumentCaptor.forClass(AfterSaleLogCreateReqBO.class);
        verify(afterSaleLogService).createAfterSaleLog(captor.capture());
        assertEquals(AfterSaleOperateTypeEnum.SYSTEM_REFUND_RULE_MANUAL_REVIEW.getType(), captor.getValue().getOperateType());
        verify(afterSaleReviewTicketService).upsertManualReviewTicket(
                org.mockito.ArgumentMatchers.same(afterSale),
                org.mockito.ArgumentMatchers.argThat(decision ->
                        decision != null && "BLACKLIST_USER".equals(decision.getRuleCode())));
    }

    @Test
    void shouldAllowForcePassWhenManualReviewRuleMatched() {
        Set<Long> blacklist = new LinkedHashSet<>();
        blacklist.add(77L);
        when(afterSaleRefundRuleConfigMapper.selectLatest()).thenReturn(buildDbRule(true, 5000, 3,
                new ArrayList<>(blacklist), Collections.emptyList()));
        AfterSaleDO afterSale = buildAfterSale(77L, 100, "ORDER_004");
        afterSale.setId(100L);
        afterSale.setStatus(10);

        AfterSaleRefundDecisionBO decision = service.checkAndAuditForExecution(
                11L, UserTypeEnum.ADMIN.getValue(), afterSale, true);

        assertFalse(Boolean.TRUE.equals(decision.getAutoPass()));
        assertEquals("BLACKLIST_USER", decision.getRuleCode());

        ArgumentCaptor<AfterSaleLogCreateReqBO> captor = ArgumentCaptor.forClass(AfterSaleLogCreateReqBO.class);
        verify(afterSaleLogService).createAfterSaleLog(captor.capture());
        assertEquals(AfterSaleOperateTypeEnum.ADMIN_REFUND_RULE_FORCE_PASS.getType(), captor.getValue().getOperateType());
        verify(afterSaleReviewTicketService).resolveManualReviewTicket(100L, 11L, UserTypeEnum.ADMIN.getValue(),
                "FORCE_PASS_MANUAL", "AFTER_SALE#100", "force-pass");
    }

    @Test
    void shouldFallbackWhenRuleDisabled() {
        when(afterSaleRefundRuleConfigMapper.selectLatest()).thenReturn(buildDbRule(false, 5000, 3,
                Collections.emptyList(), Collections.emptyList()));
        AfterSaleDO afterSale = buildAfterSale(9L, 10000, "ORDER_DISABLED");

        AfterSaleRefundDecisionBO decision = service.evaluate(afterSale);

        assertTrue(Boolean.TRUE.equals(decision.getAutoPass()));
        assertEquals("RULE_DISABLED", decision.getRuleCode());
    }

    private static AfterSaleDO buildAfterSale(Long userId, Integer refundPrice, String orderNo) {
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setUserId(userId);
        afterSale.setRefundPrice(refundPrice);
        afterSale.setOrderNo(orderNo);
        return afterSale;
    }

    private static AfterSaleRefundRuleConfigDO buildDbRule(Boolean enabled, Integer maxPrice, Integer dailyLimit,
                                                           List<Long> blacklist, List<String> keywords) {
        AfterSaleRefundRuleConfigDO dbRule = new AfterSaleRefundRuleConfigDO();
        dbRule.setEnabled(enabled);
        dbRule.setAutoRefundMaxPrice(maxPrice);
        dbRule.setUserDailyApplyLimit(dailyLimit);
        dbRule.setBlacklistUserIds(blacklist);
        dbRule.setSuspiciousOrderKeywords(keywords);
        dbRule.setRuleVersion("v1");
        return dbRule;
    }
}
