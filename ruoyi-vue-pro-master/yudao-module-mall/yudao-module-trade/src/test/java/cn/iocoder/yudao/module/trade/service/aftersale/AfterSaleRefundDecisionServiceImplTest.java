package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleRefundDecisionServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleRefundDecisionServiceImpl service;

    private TradeAfterSaleRefundRuleProperties refundRuleProperties;

    @Mock
    private AfterSaleMapper afterSaleMapper;
    @Mock
    private AfterSaleLogService afterSaleLogService;

    @BeforeEach
    void setUp() {
        refundRuleProperties = new TradeAfterSaleRefundRuleProperties();
        ReflectionTestUtils.setField(service, "refundRuleProperties", refundRuleProperties);
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
        refundRuleProperties.setAutoRefundMaxPrice(5000);
        AfterSaleDO afterSale = buildAfterSale(2L, 8000, "ORDER_002");

        AfterSaleRefundDecisionBO decision = service.evaluate(afterSale);

        assertFalse(Boolean.TRUE.equals(decision.getAutoPass()));
        assertEquals("AMOUNT_OVER_LIMIT", decision.getRuleCode());
    }

    @Test
    void shouldThrowManualReviewWhenBlackUserAndNotForcePass() {
        refundRuleProperties.getBlacklistUserIds().add(66L);
        AfterSaleDO afterSale = buildAfterSale(66L, 100, "ORDER_003");
        afterSale.setId(99L);
        afterSale.setStatus(10);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.checkAndAuditForExecution(10L, UserTypeEnum.ADMIN.getValue(), afterSale, false));
        assertTrue(exception.getMessage().contains("人工复核"));

        ArgumentCaptor<AfterSaleLogCreateReqBO> captor = ArgumentCaptor.forClass(AfterSaleLogCreateReqBO.class);
        verify(afterSaleLogService).createAfterSaleLog(captor.capture());
        assertEquals(AfterSaleOperateTypeEnum.SYSTEM_REFUND_RULE_MANUAL_REVIEW.getType(), captor.getValue().getOperateType());
    }

    @Test
    void shouldAllowForcePassWhenManualReviewRuleMatched() {
        refundRuleProperties.getBlacklistUserIds().add(77L);
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
    }

    private static AfterSaleDO buildAfterSale(Long userId, Integer refundPrice, String orderNo) {
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setUserId(userId);
        afterSale.setRefundPrice(refundPrice);
        afterSale.setOrderNo(orderNo);
        return afterSale;
    }
}
