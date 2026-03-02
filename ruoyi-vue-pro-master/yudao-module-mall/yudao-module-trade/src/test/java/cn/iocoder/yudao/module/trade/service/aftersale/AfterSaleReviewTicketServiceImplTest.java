package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleReviewTicketServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleReviewTicketServiceImpl service;

    @Mock
    private AfterSaleReviewTicketMapper afterSaleReviewTicketMapper;

    @Test
    void shouldCreateGenericReviewTicket() {
        AfterSaleReviewTicketCreateReqBO reqBO = new AfterSaleReviewTicketCreateReqBO();
        reqBO.setTicketType(AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType());
        reqBO.setOrderId(100L);
        reqBO.setOrderItemId(101L);
        reqBO.setUserId(102L);
        reqBO.setSourceBizNo("BK202603010001");
        reqBO.setRuleCode("SERVICE_DELAY");
        reqBO.setDecisionReason("服务履约超时");
        when(afterSaleReviewTicketMapper.insert(any(AfterSaleReviewTicketDO.class))).thenAnswer(invocation -> {
            AfterSaleReviewTicketDO ticket = invocation.getArgument(0);
            ticket.setId(777L);
            return 1;
        });

        Long id = service.createReviewTicket(reqBO);

        assertEquals(777L, id);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).insert(captor.capture());
        assertEquals(AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType(), captor.getValue().getTicketType());
        assertEquals(AfterSaleReviewTicketStatusEnum.PENDING.getStatus(), captor.getValue().getStatus());
        assertEquals("BK202603010001", captor.getValue().getSourceBizNo());
        assertEquals("TICKET_CREATE", captor.getValue().getLastActionCode());
        assertEquals("BK202603010001", captor.getValue().getLastActionBizNo());
        assertNotNull(captor.getValue().getLastActionTime());
    }

    @Test
    void shouldInsertTicketWhenNotExists() {
        AfterSaleDO afterSale = buildAfterSale(1L);
        AfterSaleRefundDecisionBO decision = AfterSaleRefundDecisionBO.manual("BLACKLIST_USER", "黑名单命中");
        when(afterSaleReviewTicketMapper.selectByAfterSaleId(1L)).thenReturn(null);

        service.upsertManualReviewTicket(afterSale, decision);

        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getAfterSaleId());
        assertEquals("P0", captor.getValue().getSeverity());
        assertEquals(AfterSaleReviewTicketStatusEnum.PENDING.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void shouldUpdateTicketWhenExists() {
        AfterSaleDO afterSale = buildAfterSale(2L);
        AfterSaleRefundDecisionBO decision = AfterSaleRefundDecisionBO.manual("AMOUNT_OVER_LIMIT", "金额超限");
        AfterSaleReviewTicketDO existed = new AfterSaleReviewTicketDO();
        existed.setId(88L);
        existed.setAfterSaleId(2L);
        existed.setTriggerCount(2);
        when(afterSaleReviewTicketMapper.selectByAfterSaleId(2L)).thenReturn(existed);

        service.upsertManualReviewTicket(afterSale, decision);

        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateById(captor.capture());
        assertEquals(88L, captor.getValue().getId());
        assertEquals(3, captor.getValue().getTriggerCount());
        assertEquals("P1", captor.getValue().getSeverity());
        assertEquals("RULE_RETRIGGER", captor.getValue().getLastActionCode());
        assertEquals("2", captor.getValue().getLastActionBizNo());
        assertNotNull(captor.getValue().getLastActionTime());
    }

    @Test
    void shouldResolvePendingTicketByAfterSaleId() {
        service.resolveManualReviewTicket(3L, 99L, 1, "FORCE_PASS_MANUAL",
                "AFTER_SALE#3", "force-pass");

        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateByAfterSaleIdAndStatus(
                eq(3L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()),
                captor.capture());
        assertEquals(AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus(), captor.getValue().getStatus());
        assertEquals(99L, captor.getValue().getResolverId());
        assertEquals("FORCE_PASS_MANUAL", captor.getValue().getResolveActionCode());
        assertEquals("AFTER_SALE#3", captor.getValue().getResolveBizNo());
        assertEquals("FORCE_PASS_MANUAL", captor.getValue().getLastActionCode());
        assertEquals("AFTER_SALE#3", captor.getValue().getLastActionBizNo());
        assertNotNull(captor.getValue().getLastActionTime());
        assertNotNull(captor.getValue().getResolvedTime());
    }

    @Test
    void shouldResolvePendingTicketById() {
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(4L);
        ticket.setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus());
        when(afterSaleReviewTicketMapper.selectById(4L)).thenReturn(ticket);
        when(afterSaleReviewTicketMapper.updateByIdAndStatus(eq(4L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any()))
                .thenReturn(1);

        service.resolveManualReviewTicketById(4L, 100L, 1, "MANUAL_RESOLVE",
                "OPS#4", "resolved-by-id");

        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateByIdAndStatus(eq(4L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), captor.capture());
        assertEquals(AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus(), captor.getValue().getStatus());
        assertEquals(100L, captor.getValue().getResolverId());
        assertEquals("MANUAL_RESOLVE", captor.getValue().getResolveActionCode());
        assertEquals("OPS#4", captor.getValue().getResolveBizNo());
        assertEquals("MANUAL_RESOLVE", captor.getValue().getLastActionCode());
        assertEquals("OPS#4", captor.getValue().getLastActionBizNo());
        assertNotNull(captor.getValue().getLastActionTime());
    }

    @Test
    void shouldThrowWhenResolveByIdNotPending() {
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(5L);
        ticket.setStatus(AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus());
        when(afterSaleReviewTicketMapper.selectById(5L)).thenReturn(ticket);

        assertThrows(ServiceException.class, () -> service.resolveManualReviewTicketById(5L, 100L, 1,
                "MANUAL_RESOLVE", "OPS#5", "ignore"));
        verify(afterSaleReviewTicketMapper, never()).updateByIdAndStatus(eq(5L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any());
    }

    @Test
    void shouldEscalateOverdueTicket() {
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(6L);
        ticket.setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus());
        ticket.setSeverity("P1");
        ticket.setEscalateTo("HQ_AFTER_SALE");
        ticket.setSlaDeadlineTime(LocalDateTime.now().minusMinutes(10));
        ticket.setTriggerCount(2);
        when(afterSaleReviewTicketMapper.selectListByStatusAndSlaDeadlineTimeBefore(
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()),
                any(LocalDateTime.class),
                eq(200)))
                .thenReturn(Collections.singletonList(ticket));
        when(afterSaleReviewTicketMapper.updateByIdAndStatus(eq(6L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any()))
                .thenReturn(1);

        int count = service.escalateOverduePendingTickets(null);

        assertEquals(1, count);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateByIdAndStatus(eq(6L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), captor.capture());
        assertEquals("P0", captor.getValue().getSeverity());
        assertEquals("HQ_RISK_FINANCE", captor.getValue().getEscalateTo());
        assertEquals(3, captor.getValue().getTriggerCount());
        assertEquals("SLA_AUTO_ESCALATE", captor.getValue().getLastActionCode());
        assertEquals("TICKET#6", captor.getValue().getLastActionBizNo());
        assertNotNull(captor.getValue().getLastActionTime());
    }

    private static AfterSaleDO buildAfterSale(Long id) {
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setId(id);
        afterSale.setOrderId(10L);
        afterSale.setOrderItemId(11L);
        afterSale.setUserId(12L);
        return afterSale;
    }

}
