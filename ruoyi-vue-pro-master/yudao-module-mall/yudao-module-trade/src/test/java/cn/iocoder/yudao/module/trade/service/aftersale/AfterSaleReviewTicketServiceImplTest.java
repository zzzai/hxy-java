package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import cn.iocoder.yudao.module.trade.api.ticketsla.TradeTicketSlaRuleApi;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchRespDTO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketNotifyOutboxDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketNotifyOutboxMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleMatchLevelEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;
import cn.iocoder.yudao.module.trade.service.aftersale.dto.AfterSaleReviewTicketBatchResolveResult;
import cn.iocoder.yudao.module.trade.service.aftersale.dto.AfterSaleReviewTicketNotifyBatchRetryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.anyString;

class AfterSaleReviewTicketServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleReviewTicketServiceImpl service;

    @Mock
    private AfterSaleReviewTicketMapper afterSaleReviewTicketMapper;

    @Mock
    private TradeTicketSlaRuleApi tradeTicketSlaRuleApi;
    @Mock
    private ConfigApi configApi;
    @Mock
    private AfterSaleReviewTicketNotifyOutboxMapper afterSaleReviewTicketNotifyOutboxMapper;
    @Mock
    private NotifySendService notifySendService;
    @Mock
    private PermissionApi permissionApi;

    @BeforeEach
    void setUpRouteProvider() {
        lenient().when(tradeTicketSlaRuleApi.matchRule(any()))
                .thenReturn(matchedRule(11L, TicketSlaRuleMatchLevelEnum.TYPE_DEFAULT.getCode(),
                        "P1", "HQ_AFTER_SALE", 120));
        lenient().when(configApi.getConfigValueByKey(anyString())).thenReturn(null);
        lenient().when(permissionApi.getUserRoleIdListByRoleIds(any())).thenReturn(Collections.emptySet());
    }

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
        assertEquals(11L, captor.getValue().getRouteId());
        assertEquals("TYPE_DEFAULT", captor.getValue().getRouteScope());
        assertEquals("RULE>TYPE_SEVERITY>TYPE_DEFAULT>GLOBAL_DEFAULT", captor.getValue().getRouteDecisionOrder());
    }

    @Test
    void shouldInsertWhenUpsertBySourceTicketNotExists() {
        AfterSaleReviewTicketCreateReqBO reqBO = new AfterSaleReviewTicketCreateReqBO();
        reqBO.setTicketType(AfterSaleReviewTicketTypeEnum.BOOKING_SETTLEMENT.getType());
        reqBO.setSourceBizNo("FOUR_ACCOUNT_RECONCILE:2026-03-04");
        reqBO.setRuleCode("FOUR_ACCOUNT_RECONCILE_WARN");
        reqBO.setDecisionReason("四账对账告警");
        when(afterSaleReviewTicketMapper.selectByTicketTypeAndSourceBizNo(
                AfterSaleReviewTicketTypeEnum.BOOKING_SETTLEMENT.getType(),
                "FOUR_ACCOUNT_RECONCILE:2026-03-04")).thenReturn(null);
        when(afterSaleReviewTicketMapper.insert(any(AfterSaleReviewTicketDO.class))).thenAnswer(invocation -> {
            AfterSaleReviewTicketDO ticket = invocation.getArgument(0);
            ticket.setId(2001L);
            return 1;
        });

        Long id = service.upsertReviewTicketBySourceBizNo(reqBO, "FOUR_ACCOUNT_RECONCILE_WARN");

        assertEquals(2001L, id);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).insert(captor.capture());
        assertEquals(AfterSaleReviewTicketTypeEnum.BOOKING_SETTLEMENT.getType(), captor.getValue().getTicketType());
        assertEquals("FOUR_ACCOUNT_RECONCILE:2026-03-04", captor.getValue().getSourceBizNo());
        assertEquals("FOUR_ACCOUNT_RECONCILE_WARN", captor.getValue().getLastActionCode());
        assertEquals(1, captor.getValue().getTriggerCount());
    }

    @Test
    void shouldUpdateWhenUpsertBySourceTicketExists() {
        AfterSaleReviewTicketCreateReqBO reqBO = new AfterSaleReviewTicketCreateReqBO();
        reqBO.setTicketType(AfterSaleReviewTicketTypeEnum.BOOKING_SETTLEMENT.getType());
        reqBO.setSourceBizNo("FOUR_ACCOUNT_RECONCILE:2026-03-04");
        reqBO.setRuleCode("FOUR_ACCOUNT_RECONCILE_WARN");
        reqBO.setDecisionReason("四账对账告警");
        reqBO.setSeverity("P1");
        AfterSaleReviewTicketDO existed = new AfterSaleReviewTicketDO();
        existed.setId(2002L);
        existed.setTriggerCount(2);
        existed.setStatus(AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus());
        when(afterSaleReviewTicketMapper.selectByTicketTypeAndSourceBizNo(
                AfterSaleReviewTicketTypeEnum.BOOKING_SETTLEMENT.getType(),
                "FOUR_ACCOUNT_RECONCILE:2026-03-04")).thenReturn(existed);

        Long id = service.upsertReviewTicketBySourceBizNo(reqBO, "FOUR_ACCOUNT_RECONCILE_WARN");

        assertEquals(2002L, id);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateById(captor.capture());
        assertEquals(2002L, captor.getValue().getId());
        assertEquals(AfterSaleReviewTicketStatusEnum.PENDING.getStatus(), captor.getValue().getStatus());
        assertEquals(3, captor.getValue().getTriggerCount());
        assertEquals("FOUR_ACCOUNT_RECONCILE_WARN", captor.getValue().getLastActionCode());
        assertEquals("", captor.getValue().getResolveActionCode());
    }

    @Test
    void shouldFallbackToUpdateWhenUpsertInsertDuplicate() {
        AfterSaleReviewTicketCreateReqBO reqBO = new AfterSaleReviewTicketCreateReqBO();
        reqBO.setTicketType(AfterSaleReviewTicketTypeEnum.BOOKING_SETTLEMENT.getType());
        reqBO.setSourceBizNo("FOUR_ACCOUNT_RECONCILE:2026-03-05");
        reqBO.setRuleCode("FOUR_ACCOUNT_RECONCILE_WARN");
        reqBO.setDecisionReason("四账对账告警");
        AfterSaleReviewTicketDO existed = new AfterSaleReviewTicketDO();
        existed.setId(2003L);
        existed.setTriggerCount(1);
        when(afterSaleReviewTicketMapper.selectByTicketTypeAndSourceBizNo(
                AfterSaleReviewTicketTypeEnum.BOOKING_SETTLEMENT.getType(),
                "FOUR_ACCOUNT_RECONCILE:2026-03-05"))
                .thenReturn(null)
                .thenReturn(existed);
        when(afterSaleReviewTicketMapper.insert(any(AfterSaleReviewTicketDO.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        Long id = service.upsertReviewTicketBySourceBizNo(reqBO, "FOUR_ACCOUNT_RECONCILE_WARN");

        assertEquals(2003L, id);
        verify(afterSaleReviewTicketMapper).updateById(
                org.mockito.ArgumentMatchers.<AfterSaleReviewTicketDO>argThat(update -> update.getId().equals(2003L)));
    }

    @Test
    void shouldNormalizeQueryFiltersWhenQueryPage() {
        AfterSaleReviewTicketPageReqVO reqVO = new AfterSaleReviewTicketPageReqVO();
        reqVO.setRouteScope(" rule ");
        reqVO.setSeverity(" p1 ");
        reqVO.setRuleCode(" blacklist_user ");
        reqVO.setEscalateTo(" hq_after_sale ");
        reqVO.setLastActionCode(" manual_resolve ");
        reqVO.setResolveActionCode(" auto_resolve ");
        reqVO.setSourceBizNo(" BK202603030001 ");
        reqVO.setLastActionBizNo(" OPS-202603030001 ");
        reqVO.setResolveBizNo(" OPS-BATCH-20260303#1 ");
        when(afterSaleReviewTicketMapper.selectPage(any(AfterSaleReviewTicketPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        service.getReviewTicketPage(reqVO);

        assertEquals("RULE", reqVO.getRouteScope());
        assertEquals("P1", reqVO.getSeverity());
        assertEquals("BLACKLIST_USER", reqVO.getRuleCode());
        assertEquals("HQ_AFTER_SALE", reqVO.getEscalateTo());
        assertEquals("MANUAL_RESOLVE", reqVO.getLastActionCode());
        assertEquals("AUTO_RESOLVE", reqVO.getResolveActionCode());
        assertEquals("BK202603030001", reqVO.getSourceBizNo());
        assertEquals("OPS-202603030001", reqVO.getLastActionBizNo());
        assertEquals("OPS-BATCH-20260303#1", reqVO.getResolveBizNo());
        verify(afterSaleReviewTicketMapper).selectPage(reqVO);
    }

    @Test
    void shouldClearBlankQueryFiltersWhenQueryPage() {
        AfterSaleReviewTicketPageReqVO reqVO = new AfterSaleReviewTicketPageReqVO();
        reqVO.setRouteScope("   ");
        reqVO.setSeverity("   ");
        reqVO.setRuleCode("   ");
        reqVO.setEscalateTo("   ");
        reqVO.setLastActionCode("   ");
        reqVO.setResolveActionCode("   ");
        reqVO.setSourceBizNo("   ");
        reqVO.setLastActionBizNo("   ");
        reqVO.setResolveBizNo("   ");
        when(afterSaleReviewTicketMapper.selectPage(any(AfterSaleReviewTicketPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        service.getReviewTicketPage(reqVO);

        assertNull(reqVO.getRouteScope());
        assertNull(reqVO.getSeverity());
        assertNull(reqVO.getRuleCode());
        assertNull(reqVO.getEscalateTo());
        assertNull(reqVO.getLastActionCode());
        assertNull(reqVO.getResolveActionCode());
        assertNull(reqVO.getSourceBizNo());
        assertNull(reqVO.getLastActionBizNo());
        assertNull(reqVO.getResolveBizNo());
        verify(afterSaleReviewTicketMapper).selectPage(reqVO);
    }

    @Test
    void shouldInsertTicketWhenNotExists() {
        AfterSaleDO afterSale = buildAfterSale(1L);
        AfterSaleRefundDecisionBO decision = AfterSaleRefundDecisionBO.manual("BLACKLIST_USER", "黑名单命中");
        when(afterSaleReviewTicketMapper.selectByAfterSaleId(1L)).thenReturn(null);
        when(tradeTicketSlaRuleApi.matchRule(any())).thenReturn(
                matchedRule(21L, TicketSlaRuleMatchLevelEnum.RULE.getCode(),
                        "P0", "HQ_RISK_FINANCE", 30));

        service.upsertManualReviewTicket(afterSale, decision);

        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getAfterSaleId());
        assertEquals("P0", captor.getValue().getSeverity());
        assertEquals(21L, captor.getValue().getRouteId());
        assertEquals("RULE", captor.getValue().getRouteScope());
        assertEquals(AfterSaleReviewTicketStatusEnum.PENDING.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void shouldUpdateTicketWhenExists() {
        AfterSaleDO afterSale = buildAfterSale(2L);
        AfterSaleRefundDecisionBO decision = AfterSaleRefundDecisionBO.manual("AMOUNT_OVER_LIMIT", "金额超限");
        AfterSaleReviewTicketDO existed = new AfterSaleReviewTicketDO();
        existed.setId(88L);
        existed.setAfterSaleId(2L);
        existed.setSeverity("P2");
        existed.setTriggerCount(2);
        when(afterSaleReviewTicketMapper.selectByAfterSaleId(2L)).thenReturn(existed);
        when(tradeTicketSlaRuleApi.matchRule(any())).thenReturn(
                matchedRule(22L, TicketSlaRuleMatchLevelEnum.TYPE_SEVERITY.getCode(),
                        "P1", "HQ_FINANCE", 120));

        service.upsertManualReviewTicket(afterSale, decision);

        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateById(captor.capture());
        assertEquals(88L, captor.getValue().getId());
        assertEquals(3, captor.getValue().getTriggerCount());
        assertEquals("P1", captor.getValue().getSeverity());
        assertEquals(22L, captor.getValue().getRouteId());
        assertEquals("TYPE_SEVERITY", captor.getValue().getRouteScope());
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
    void shouldBatchResolveReviewTicketsWithSummary() {
        AfterSaleReviewTicketDO pending1 = new AfterSaleReviewTicketDO();
        pending1.setId(21L);
        pending1.setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus());
        AfterSaleReviewTicketDO resolved = new AfterSaleReviewTicketDO();
        resolved.setId(23L);
        resolved.setStatus(AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus());
        AfterSaleReviewTicketDO pendingRace = new AfterSaleReviewTicketDO();
        pendingRace.setId(24L);
        pendingRace.setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus());

        when(afterSaleReviewTicketMapper.selectById(21L)).thenReturn(pending1);
        when(afterSaleReviewTicketMapper.selectById(22L)).thenReturn(null);
        when(afterSaleReviewTicketMapper.selectById(23L)).thenReturn(resolved);
        when(afterSaleReviewTicketMapper.selectById(24L)).thenReturn(pendingRace);
        when(afterSaleReviewTicketMapper.updateByIdAndStatus(eq(21L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any())).thenReturn(1);
        when(afterSaleReviewTicketMapper.updateByIdAndStatus(eq(24L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any())).thenReturn(0);

        AfterSaleReviewTicketBatchResolveResult result = service.batchResolveManualReviewTicketByIds(
                Arrays.asList(21L, 22L, 23L, 24L, 21L, null), 99L, 1,
                " MANUAL_RESOLVE ", " OPS-BATCH-20260303 ", "batch-close");

        assertEquals(4, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getSkippedNotFoundCount());
        assertEquals(2, result.getSkippedNotPendingCount());
        assertEquals(Collections.singletonList(21L), result.getSuccessIds());
        assertEquals(Collections.singletonList(22L), result.getSkippedNotFoundIds());
        assertEquals(Arrays.asList(23L, 24L), result.getSkippedNotPendingIds());

        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateByIdAndStatus(eq(21L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), captor.capture());
        assertEquals("OPS-BATCH-20260303#21", captor.getValue().getResolveBizNo());
        assertEquals("OPS-BATCH-20260303#21", captor.getValue().getLastActionBizNo());
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
        ticket.setTicketType(AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType());
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
        when(tradeTicketSlaRuleApi.matchRule(any())).thenReturn(
                matchedRule(33L, TicketSlaRuleMatchLevelEnum.TYPE_SEVERITY.getCode(),
                        "P0", "HQ_RISK_FINANCE", 30));

        int count = service.escalateOverduePendingTickets(null);

        assertEquals(1, count);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateByIdAndStatus(eq(6L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), captor.capture());
        assertEquals("P0", captor.getValue().getSeverity());
        assertEquals("HQ_RISK_FINANCE", captor.getValue().getEscalateTo());
        assertEquals(33L, captor.getValue().getRouteId());
        assertEquals("TYPE_SEVERITY", captor.getValue().getRouteScope());
        assertEquals(3, captor.getValue().getTriggerCount());
        assertEquals("SLA_AUTO_ESCALATE", captor.getValue().getLastActionCode());
        assertEquals("TICKET#6", captor.getValue().getLastActionBizNo());
        assertNotNull(captor.getValue().getLastActionTime());
    }

    @Test
    void shouldCapEscalateBatchLimitAtFiveThousand() {
        when(afterSaleReviewTicketMapper.selectListByStatusAndSlaDeadlineTimeBefore(
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()),
                any(LocalDateTime.class),
                eq(5000)))
                .thenReturn(Collections.emptyList());

        int count = service.escalateOverduePendingTickets(99999);

        assertEquals(0, count);
        verify(afterSaleReviewTicketMapper).selectListByStatusAndSlaDeadlineTimeBefore(
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()),
                any(LocalDateTime.class),
                eq(5000));
    }

    @Test
    void shouldCreateTicketUseRouteFallbackWhenSeverityMissing() {
        AfterSaleReviewTicketCreateReqBO reqBO = new AfterSaleReviewTicketCreateReqBO();
        reqBO.setTicketType(AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType());
        reqBO.setRuleCode("UNKNOWN_RULE");
        when(tradeTicketSlaRuleApi.matchRule(any())).thenReturn(
                matchedRule(45L, TicketSlaRuleMatchLevelEnum.TYPE_DEFAULT.getCode(),
                        "P0", "HQ_SERVICE_OPS", 45));
        when(afterSaleReviewTicketMapper.insert(any(AfterSaleReviewTicketDO.class))).thenAnswer(invocation -> {
            AfterSaleReviewTicketDO ticket = invocation.getArgument(0);
            ticket.setId(1001L);
            return 1;
        });

        Long id = service.createReviewTicket(reqBO);

        assertEquals(1001L, id);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).insert(captor.capture());
        assertEquals("P0", captor.getValue().getSeverity());
        assertEquals("HQ_SERVICE_OPS", captor.getValue().getEscalateTo());
    }

    @Test
    void shouldEscalateUseResolvedRouteSlaAndEscalateTarget() {
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(18L);
        ticket.setTicketType(AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType());
        ticket.setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus());
        ticket.setSeverity("P2");
        ticket.setEscalateTo("HQ_SERVICE_OPS");
        ticket.setRuleCode("UNKNOWN_RULE");
        ticket.setSlaDeadlineTime(LocalDateTime.now().minusMinutes(20));
        ticket.setTriggerCount(1);
        when(afterSaleReviewTicketMapper.selectListByStatusAndSlaDeadlineTimeBefore(
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any(LocalDateTime.class), eq(200)))
                .thenReturn(Collections.singletonList(ticket));
        when(tradeTicketSlaRuleApi.matchRule(any())).thenReturn(
                matchedRule(44L, TicketSlaRuleMatchLevelEnum.RULE.getCode(),
                        "P1", "HQ_SERVICE_OPS", 90));
        when(afterSaleReviewTicketMapper.updateByIdAndStatus(eq(18L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any()))
                .thenReturn(1);

        int count = service.escalateOverduePendingTickets(200);

        assertEquals(1, count);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper, times(1)).updateByIdAndStatus(eq(18L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), captor.capture());
        assertEquals("P1", captor.getValue().getSeverity());
        assertEquals("HQ_SERVICE_OPS", captor.getValue().getEscalateTo());
        assertEquals(44L, captor.getValue().getRouteId());
        assertEquals("RULE", captor.getValue().getRouteScope());
    }

    @Test
    void shouldSkipEscalateWhenEscalateDelayNotReached() {
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(20L);
        ticket.setTicketType(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType());
        ticket.setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus());
        ticket.setSeverity("P1");
        ticket.setEscalateTo("HQ_AFTER_SALE");
        ticket.setRuleCode("UNKNOWN_RULE");
        ticket.setSlaDeadlineTime(LocalDateTime.now().minusMinutes(1));
        ticket.setTriggerCount(1);
        when(afterSaleReviewTicketMapper.selectListByStatusAndSlaDeadlineTimeBefore(
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any(LocalDateTime.class), eq(200)))
                .thenReturn(Collections.singletonList(ticket));
        when(tradeTicketSlaRuleApi.matchRule(any())).thenReturn(
                matchedRule(55L, TicketSlaRuleMatchLevelEnum.TYPE_DEFAULT.getCode(),
                        "P0", "HQ_RISK_FINANCE", 30, 10));

        int count = service.escalateOverduePendingTickets(null);

        assertEquals(0, count);
        verify(afterSaleReviewTicketMapper, never()).updateByIdAndStatus(eq(20L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any());
    }

    @Test
    void shouldCreateTicketFallbackToGlobalDefaultWhenRuleCenterUnavailable() {
        AfterSaleReviewTicketCreateReqBO reqBO = new AfterSaleReviewTicketCreateReqBO();
        reqBO.setTicketType(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType());
        reqBO.setRuleCode("BLACKLIST_USER");
        when(tradeTicketSlaRuleApi.matchRule(any())).thenThrow(new RuntimeException("down"));
        when(afterSaleReviewTicketMapper.insert(any(AfterSaleReviewTicketDO.class))).thenAnswer(invocation -> {
            AfterSaleReviewTicketDO ticket = invocation.getArgument(0);
            ticket.setId(1002L);
            return 1;
        });

        Long id = service.createReviewTicket(reqBO);

        assertEquals(1002L, id);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).insert(captor.capture());
        assertEquals("P1", captor.getValue().getSeverity());
        assertEquals("HQ_AFTER_SALE", captor.getValue().getEscalateTo());
        assertEquals("GLOBAL_DEFAULT_FALLBACK", captor.getValue().getRouteScope());
        assertNull(captor.getValue().getRouteId());
    }

    @Test
    void shouldEscalateKeepP0WhenRuleCenterUnavailable() {
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(19L);
        ticket.setTicketType(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType());
        ticket.setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus());
        ticket.setSeverity("P1");
        ticket.setEscalateTo("HQ_AFTER_SALE");
        ticket.setRuleCode("UNKNOWN_RULE");
        ticket.setSlaDeadlineTime(LocalDateTime.now().minusMinutes(5));
        ticket.setTriggerCount(1);
        when(afterSaleReviewTicketMapper.selectListByStatusAndSlaDeadlineTimeBefore(
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any(LocalDateTime.class), eq(200)))
                .thenReturn(Collections.singletonList(ticket));
        when(afterSaleReviewTicketMapper.updateByIdAndStatus(eq(19L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any()))
                .thenReturn(1);
        when(tradeTicketSlaRuleApi.matchRule(any())).thenThrow(new RuntimeException("down"));

        int count = service.escalateOverduePendingTickets(null);

        assertEquals(1, count);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateByIdAndStatus(eq(19L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), captor.capture());
        assertEquals("P0", captor.getValue().getSeverity());
        assertEquals("HQ_RISK_FINANCE", captor.getValue().getEscalateTo());
        assertEquals("GLOBAL_DEFAULT_FALLBACK", captor.getValue().getRouteScope());
        assertNull(captor.getValue().getRouteId());
    }

    @Test
    void shouldCreateTicketFallbackUseConfiguredP0Values() {
        AfterSaleReviewTicketCreateReqBO reqBO = new AfterSaleReviewTicketCreateReqBO();
        reqBO.setTicketType(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType());
        reqBO.setSeverity("P0");
        when(tradeTicketSlaRuleApi.matchRule(any())).thenThrow(new RuntimeException("down"));
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.fallback.p0.escalate-to"))
                .thenReturn("HQ_CUSTOM_P0");
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.fallback.p0.sla-minutes"))
                .thenReturn("25");
        when(afterSaleReviewTicketMapper.insert(any(AfterSaleReviewTicketDO.class))).thenAnswer(invocation -> {
            AfterSaleReviewTicketDO ticket = invocation.getArgument(0);
            ticket.setId(1003L);
            return 1;
        });

        Long id = service.createReviewTicket(reqBO);

        assertEquals(1003L, id);
        ArgumentCaptor<AfterSaleReviewTicketDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).insert(captor.capture());
        assertEquals("P0", captor.getValue().getSeverity());
        assertEquals("HQ_CUSTOM_P0", captor.getValue().getEscalateTo());
        assertEquals(25,
                java.time.temporal.ChronoUnit.MINUTES.between(
                        captor.getValue().getFirstTriggerTime(), captor.getValue().getSlaDeadlineTime()));
    }

    @Test
    void shouldWarnNearDeadlinePendingTicketsAndCreateNotifyOutbox() {
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(31L);
        ticket.setTicketType(AfterSaleReviewTicketTypeEnum.AFTER_SALE.getType());
        ticket.setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus());
        ticket.setRuleCode("BLACKLIST_USER");
        ticket.setSeverity("P1");
        ticket.setEscalateTo("HQ_AFTER_SALE");
        ticket.setSlaDeadlineTime(LocalDateTime.now().plusMinutes(10));
        when(afterSaleReviewTicketMapper.selectListByStatusAndSlaDeadlineTimeBefore(
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any(LocalDateTime.class), eq(200)))
                .thenReturn(Collections.singletonList(ticket));
        when(tradeTicketSlaRuleApi.matchRule(any())).thenReturn(
                matchedRule(61L, TicketSlaRuleMatchLevelEnum.TYPE_SEVERITY.getCode(),
                        "P1", "HQ_AFTER_SALE", 120, 30, 0));
        when(afterSaleReviewTicketMapper.updateByIdAndStatus(eq(31L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), any()))
                .thenReturn(1);

        int count = service.warnNearDeadlinePendingTickets(null);

        assertEquals(1, count);
        ArgumentCaptor<AfterSaleReviewTicketNotifyOutboxDO> outboxCaptor =
                ArgumentCaptor.forClass(AfterSaleReviewTicketNotifyOutboxDO.class);
        verify(afterSaleReviewTicketNotifyOutboxMapper).insert(outboxCaptor.capture());
        assertEquals(31L, outboxCaptor.getValue().getTicketId());
        assertEquals("SLA_WARN", outboxCaptor.getValue().getNotifyType());
        assertEquals("IN_APP", outboxCaptor.getValue().getChannel());
        assertEquals(0, outboxCaptor.getValue().getStatus());
        assertEquals(0, outboxCaptor.getValue().getRetryCount());

        ArgumentCaptor<AfterSaleReviewTicketDO> ticketUpdateCaptor = ArgumentCaptor.forClass(AfterSaleReviewTicketDO.class);
        verify(afterSaleReviewTicketMapper).updateByIdAndStatus(eq(31L),
                eq(AfterSaleReviewTicketStatusEnum.PENDING.getStatus()), ticketUpdateCaptor.capture());
        assertEquals("SLA_WARN_NOTIFY", ticketUpdateCaptor.getValue().getLastActionCode());
        assertNotNull(ticketUpdateCaptor.getValue().getLastActionTime());
    }

    @Test
    void shouldDispatchNotifyOutboxAndMarkSentWhenNotifySuccess() {
        AfterSaleReviewTicketNotifyOutboxDO outbox = new AfterSaleReviewTicketNotifyOutboxDO();
        outbox.setId(41L);
        outbox.setTicketId(32L);
        outbox.setNotifyType("SLA_WARN");
        outbox.setChannel("IN_APP");
        outbox.setSeverity("P1");
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        when(afterSaleReviewTicketNotifyOutboxMapper.selectDispatchableList(any(LocalDateTime.class), eq(200), eq(5)))
                .thenReturn(Collections.singletonList(outbox));
        when(afterSaleReviewTicketNotifyOutboxMapper.updateByIdAndStatus(eq(41L), eq(0), any()))
                .thenReturn(1);
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(32L);
        ticket.setEscalateTo("HQ_AFTER_SALE");
        ticket.setSeverity("P1");
        ticket.setSlaDeadlineTime(LocalDateTime.now().plusMinutes(20));
        when(afterSaleReviewTicketMapper.selectById(32L)).thenReturn(ticket);
        when(notifySendService.sendSingleNotifyToAdmin(eq(1L), eq("hxy_trade_review_ticket_warn"), any()))
                .thenReturn(10001L);

        int count = service.dispatchPendingNotifyOutbox(null);

        assertEquals(1, count);
        ArgumentCaptor<AfterSaleReviewTicketNotifyOutboxDO> outboxCaptor =
                ArgumentCaptor.forClass(AfterSaleReviewTicketNotifyOutboxDO.class);
        verify(afterSaleReviewTicketNotifyOutboxMapper).updateByIdAndStatus(eq(41L), eq(0), outboxCaptor.capture());
        assertEquals(1, outboxCaptor.getValue().getStatus());
        assertEquals(0, outboxCaptor.getValue().getRetryCount());
        assertEquals("DISPATCH_SUCCESS", outboxCaptor.getValue().getLastActionCode());
        assertNotNull(outboxCaptor.getValue().getSentTime());
    }

    @Test
    void shouldDispatchNotifyOutboxAndMarkFailedWhenNotifyThrows() {
        AfterSaleReviewTicketNotifyOutboxDO outbox = new AfterSaleReviewTicketNotifyOutboxDO();
        outbox.setId(42L);
        outbox.setTicketId(33L);
        outbox.setNotifyType("SLA_WARN");
        outbox.setChannel("IN_APP");
        outbox.setSeverity("P1");
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        when(afterSaleReviewTicketNotifyOutboxMapper.selectDispatchableList(any(LocalDateTime.class), eq(200), eq(5)))
                .thenReturn(Collections.singletonList(outbox));
        when(afterSaleReviewTicketNotifyOutboxMapper.updateByIdAndStatus(eq(42L), eq(0), any()))
                .thenReturn(1);
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(33L);
        ticket.setEscalateTo("HQ_AFTER_SALE");
        ticket.setSeverity("P1");
        ticket.setSlaDeadlineTime(LocalDateTime.now().plusMinutes(20));
        when(afterSaleReviewTicketMapper.selectById(33L)).thenReturn(ticket);
        when(notifySendService.sendSingleNotifyToAdmin(eq(1L), eq("hxy_trade_review_ticket_warn"), any()))
                .thenThrow(new IllegalStateException("template disabled"));

        int count = service.dispatchPendingNotifyOutbox(null);

        assertEquals(0, count);
        ArgumentCaptor<AfterSaleReviewTicketNotifyOutboxDO> outboxCaptor =
                ArgumentCaptor.forClass(AfterSaleReviewTicketNotifyOutboxDO.class);
        verify(afterSaleReviewTicketNotifyOutboxMapper).updateByIdAndStatus(eq(42L), eq(0), outboxCaptor.capture());
        assertEquals(2, outboxCaptor.getValue().getStatus());
        assertEquals(1, outboxCaptor.getValue().getRetryCount());
        assertEquals("DISPATCH_FAILED", outboxCaptor.getValue().getLastActionCode());
        assertNotNull(outboxCaptor.getValue().getNextRetryTime());
    }

    @Test
    void shouldBatchRetryNotifyOutboxWithSummary() {
        AfterSaleReviewTicketNotifyOutboxDO failed = new AfterSaleReviewTicketNotifyOutboxDO();
        failed.setId(51L);
        failed.setStatus(2);
        AfterSaleReviewTicketNotifyOutboxDO sent = new AfterSaleReviewTicketNotifyOutboxDO();
        sent.setId(53L);
        sent.setStatus(1);
        when(afterSaleReviewTicketNotifyOutboxMapper.selectById(51L)).thenReturn(failed);
        when(afterSaleReviewTicketNotifyOutboxMapper.selectById(52L)).thenReturn(null);
        when(afterSaleReviewTicketNotifyOutboxMapper.selectById(53L)).thenReturn(sent);
        when(afterSaleReviewTicketNotifyOutboxMapper.updateByIdAndStatus(eq(51L), eq(2), any())).thenReturn(1);

        AfterSaleReviewTicketNotifyBatchRetryResult result = service.retryNotifyOutboxBatch(
                Arrays.asList(51L, 52L, 53L, 51L, null), 99L, "manual-retry");

        assertEquals(3, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getSkippedNotFoundCount());
        assertEquals(1, result.getSkippedStatusInvalidCount());
        assertEquals(Collections.singletonList(51L), result.getSuccessIds());
        assertEquals(Collections.singletonList(52L), result.getSkippedNotFoundIds());
        assertEquals(Collections.singletonList(53L), result.getSkippedStatusInvalidIds());

        ArgumentCaptor<AfterSaleReviewTicketNotifyOutboxDO> outboxCaptor =
                ArgumentCaptor.forClass(AfterSaleReviewTicketNotifyOutboxDO.class);
        verify(afterSaleReviewTicketNotifyOutboxMapper).updateByIdAndStatus(eq(51L), eq(2), outboxCaptor.capture());
        assertEquals(0, outboxCaptor.getValue().getStatus());
        assertEquals(0, outboxCaptor.getValue().getRetryCount());
        assertEquals("MANUAL_RETRY", outboxCaptor.getValue().getLastActionCode());
        assertNotNull(outboxCaptor.getValue().getNextRetryTime());
    }

    private static AfterSaleDO buildAfterSale(Long id) {
        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setId(id);
        afterSale.setOrderId(10L);
        afterSale.setOrderItemId(11L);
        afterSale.setUserId(12L);
        return afterSale;
    }

    private static TradeTicketSlaRuleMatchRespDTO matchedRule(Long ruleId, Integer matchLevel,
                                                              String severity, String escalateTo, Integer slaMinutes) {
        return matchedRule(ruleId, matchLevel, severity, escalateTo, slaMinutes, null, null);
    }

    private static TradeTicketSlaRuleMatchRespDTO matchedRule(Long ruleId, Integer matchLevel,
                                                              String severity, String escalateTo, Integer slaMinutes,
                                                              Integer escalateDelayMinutes) {
        return matchedRule(ruleId, matchLevel, severity, escalateTo, slaMinutes, null, escalateDelayMinutes);
    }

    private static TradeTicketSlaRuleMatchRespDTO matchedRule(Long ruleId, Integer matchLevel,
                                                              String severity, String escalateTo, Integer slaMinutes,
                                                              Integer warnLeadMinutes,
                                                              Integer escalateDelayMinutes) {
        TradeTicketSlaRuleMatchRespDTO respDTO = new TradeTicketSlaRuleMatchRespDTO();
        respDTO.setMatched(true);
        respDTO.setRuleId(ruleId);
        respDTO.setMatchLevel(matchLevel);
        respDTO.setSeverity(severity);
        respDTO.setEscalateTo(escalateTo);
        respDTO.setSlaMinutes(slaMinutes);
        respDTO.setWarnLeadMinutes(warnLeadMinutes);
        respDTO.setEscalateDelayMinutes(escalateDelayMinutes);
        return respDTO;
    }

}
