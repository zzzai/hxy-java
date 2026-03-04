package cn.iocoder.yudao.module.trade.service.ticketsla;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchReqDTO;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchRespDTO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.ticketsla.TicketSlaRuleDO;
import cn.iocoder.yudao.module.trade.dal.mysql.ticketsla.TicketSlaRuleMapper;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleMatchLevelEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleScopeTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.TICKET_SLA_RULE_SCOPE_DUPLICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketSlaRuleServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TicketSlaRuleServiceImpl service;

    @Mock
    private TicketSlaRuleMapper ticketSlaRuleMapper;

    @Test
    void shouldMatchByPrecedenceRuleThenSeverityThenTypeThenGlobal() {
        TradeTicketSlaRuleMatchReqDTO reqDTO = new TradeTicketSlaRuleMatchReqDTO();
        reqDTO.setTicketType(10);
        reqDTO.setRuleCode("BLACKLIST_USER");
        reqDTO.setSeverity("P1");

        TicketSlaRuleDO rule = buildRule(1L, 10, "BLACKLIST_USER", "P0", 1, true, 100, 30);
        TicketSlaRuleDO typeSeverity = buildRule(2L, 10, "", "P1", 1, true, 90, 120);
        TicketSlaRuleDO typeDefault = buildRule(3L, 10, "", "", 1, true, 80, 180);
        TicketSlaRuleDO globalDefault = buildRule(4L, 0, "GLOBAL_DEFAULT", "", 1, true, 10, 240);

        when(ticketSlaRuleMapper.selectListByTicketTypeAndScope(eq(10), eq(0L)))
                .thenReturn(Arrays.asList(rule, typeSeverity, typeDefault, globalDefault));

        TradeTicketSlaRuleMatchRespDTO resp = service.matchRule(reqDTO);
        assertTrue(resp.getMatched());
        assertEquals(TicketSlaRuleMatchLevelEnum.RULE.getCode(), resp.getMatchLevel());
        assertEquals(1L, resp.getRuleId());

        when(ticketSlaRuleMapper.selectListByTicketTypeAndScope(eq(10), eq(0L)))
                .thenReturn(Arrays.asList(typeSeverity, typeDefault, globalDefault));
        resp = service.matchRule(reqDTO);
        assertEquals(TicketSlaRuleMatchLevelEnum.TYPE_SEVERITY.getCode(), resp.getMatchLevel());
        assertEquals(2L, resp.getRuleId());

        when(ticketSlaRuleMapper.selectListByTicketTypeAndScope(eq(10), eq(0L)))
                .thenReturn(Arrays.asList(typeDefault, globalDefault));
        resp = service.matchRule(reqDTO);
        assertEquals(TicketSlaRuleMatchLevelEnum.TYPE_DEFAULT.getCode(), resp.getMatchLevel());
        assertEquals(3L, resp.getRuleId());

        when(ticketSlaRuleMapper.selectListByTicketTypeAndScope(eq(10), eq(0L)))
                .thenReturn(Collections.singletonList(globalDefault));
        resp = service.matchRule(reqDTO);
        assertEquals(TicketSlaRuleMatchLevelEnum.GLOBAL_DEFAULT.getCode(), resp.getMatchLevel());
        assertEquals(4L, resp.getRuleId());
    }

    @Test
    void shouldFallbackWhenRuleDisabled() {
        TradeTicketSlaRuleMatchReqDTO reqDTO = new TradeTicketSlaRuleMatchReqDTO();
        reqDTO.setTicketType(10);
        reqDTO.setRuleCode("BLACKLIST_USER");

        TicketSlaRuleDO disabledRule = buildRule(11L, 10, "BLACKLIST_USER", "P0", 1, false, 100, 30);
        TicketSlaRuleDO typeDefault = buildRule(12L, 10, "", "", 1, true, 80, 180);

        when(ticketSlaRuleMapper.selectListByTicketTypeAndScope(eq(10), eq(0L)))
                .thenReturn(Arrays.asList(disabledRule, typeDefault));

        TradeTicketSlaRuleMatchRespDTO resp = service.matchRule(reqDTO);

        assertTrue(resp.getMatched());
        assertEquals(TicketSlaRuleMatchLevelEnum.TYPE_DEFAULT.getCode(), resp.getMatchLevel());
        assertEquals(12L, resp.getRuleId());
    }

    @Test
    void shouldBlockCreateWhenScopeConflict() {
        TicketSlaRuleCreateReqVO reqVO = new TicketSlaRuleCreateReqVO();
        reqVO.setTicketType(10);
        reqVO.setRuleCode("BLACKLIST_USER");
        reqVO.setSeverity("P0");
        reqVO.setScopeType(TicketSlaRuleScopeTypeEnum.GLOBAL.getCode());
        reqVO.setScopeStoreId(0L);
        reqVO.setEnabled(true);
        reqVO.setPriority(100);
        reqVO.setSlaMinutes(30);

        when(ticketSlaRuleMapper.selectByScope(10, "BLACKLIST_USER", "P0",
                TicketSlaRuleScopeTypeEnum.GLOBAL.getCode(), 0L, null))
                .thenReturn(buildRule(99L, 10, "BLACKLIST_USER", "P0", 1, true, 100, 30));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.createRule(reqVO));
        assertEquals(TICKET_SLA_RULE_SCOPE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void shouldUpdateStatusWithAuditAction() {
        TicketSlaRuleDO existed = buildRule(41L, 10, "", "", 1, true, 10, 120);
        when(ticketSlaRuleMapper.selectById(41L)).thenReturn(existed);

        service.updateRuleStatus(41L, false);

        ArgumentCaptor<TicketSlaRuleDO> captor = ArgumentCaptor.forClass(TicketSlaRuleDO.class);
        verify(ticketSlaRuleMapper).updateById(captor.capture());
        assertEquals(41L, captor.getValue().getId());
        assertEquals(Boolean.FALSE, captor.getValue().getEnabled());
        assertEquals("DISABLE", captor.getValue().getLastAction());
        assertTrue(captor.getValue().getLastActionAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void shouldBlockUpdateWhenScopeConflict() {
        TicketSlaRuleUpdateReqVO reqVO = new TicketSlaRuleUpdateReqVO();
        reqVO.setId(52L);
        reqVO.setTicketType(10);
        reqVO.setRuleCode("BLACKLIST_USER");
        reqVO.setSeverity("P0");
        reqVO.setScopeType(TicketSlaRuleScopeTypeEnum.GLOBAL.getCode());
        reqVO.setScopeStoreId(0L);
        reqVO.setEnabled(true);
        reqVO.setPriority(100);
        reqVO.setSlaMinutes(30);

        when(ticketSlaRuleMapper.selectById(52L)).thenReturn(buildRule(52L, 10, "", "", 1, true, 100, 30));
        when(ticketSlaRuleMapper.selectByScope(10, "BLACKLIST_USER", "P0",
                TicketSlaRuleScopeTypeEnum.GLOBAL.getCode(), 0L, 52L))
                .thenReturn(buildRule(53L, 10, "BLACKLIST_USER", "P0", 1, true, 90, 60));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateRule(reqVO));
        assertEquals(TICKET_SLA_RULE_SCOPE_DUPLICATE.getCode(), ex.getCode());
    }

    private static TicketSlaRuleDO buildRule(Long id, Integer ticketType, String ruleCode, String severity,
                                             Integer scopeType, Boolean enabled, Integer priority, Integer slaMinutes) {
        TicketSlaRuleDO ruleDO = new TicketSlaRuleDO();
        ruleDO.setId(id);
        ruleDO.setTicketType(ticketType);
        ruleDO.setRuleCode(ruleCode);
        ruleDO.setSeverity(severity);
        ruleDO.setScopeType(scopeType);
        ruleDO.setScopeStoreId(scopeType.equals(TicketSlaRuleScopeTypeEnum.STORE.getCode()) ? 1001L : 0L);
        ruleDO.setEnabled(enabled);
        ruleDO.setPriority(priority);
        ruleDO.setEscalateTo("HQ_AFTER_SALE");
        ruleDO.setSlaMinutes(slaMinutes);
        ruleDO.setWarnLeadMinutes(30);
        ruleDO.setEscalateDelayMinutes(30);
        return ruleDO;
    }

}
