package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchRespDTO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRulePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRulePreviewReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.ticketsla.TicketSlaRuleDO;
import cn.iocoder.yudao.module.trade.service.ticketsla.TicketSlaRuleService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketSlaRuleControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TicketSlaRuleController controller;

    @Mock
    private TicketSlaRuleService ticketSlaRuleService;

    @Test
    void shouldCreateRule() {
        TicketSlaRuleCreateReqVO reqVO = new TicketSlaRuleCreateReqVO();
        reqVO.setTicketType(10);
        reqVO.setEnabled(true);
        reqVO.setPriority(100);
        reqVO.setSlaMinutes(30);
        when(ticketSlaRuleService.createRule(any(TicketSlaRuleCreateReqVO.class))).thenReturn(1001L);

        CommonResult<Long> result = controller.createRule(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1001L, result.getData());
        verify(ticketSlaRuleService).createRule(any(TicketSlaRuleCreateReqVO.class));
    }

    @Test
    void shouldUpdateRule() {
        TicketSlaRuleUpdateReqVO reqVO = new TicketSlaRuleUpdateReqVO();
        reqVO.setId(2001L);
        reqVO.setTicketType(10);
        reqVO.setEnabled(true);
        reqVO.setPriority(100);
        reqVO.setSlaMinutes(30);

        CommonResult<Boolean> result = controller.updateRule(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(ticketSlaRuleService).updateRule(reqVO);
    }

    @Test
    void shouldUpdateRuleStatus() {
        CommonResult<Boolean> result = controller.updateRuleStatus(3001L, false);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(ticketSlaRuleService).updateRuleStatus(3001L, false);
    }

    @Test
    void shouldGetRule() {
        TicketSlaRuleDO dataObject = new TicketSlaRuleDO();
        dataObject.setId(4001L);
        dataObject.setTicketType(10);
        dataObject.setSlaMinutes(30);
        when(ticketSlaRuleService.getRule(4001L)).thenReturn(dataObject);

        CommonResult<TicketSlaRuleRespVO> result = controller.getRule(4001L);

        assertTrue(result.isSuccess());
        assertEquals(4001L, result.getData().getId());
        assertEquals(10, result.getData().getTicketType());
    }

    @Test
    void shouldGetRulePage() {
        TicketSlaRulePageReqVO reqVO = new TicketSlaRulePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        TicketSlaRuleDO dataObject = new TicketSlaRuleDO();
        dataObject.setId(5001L);
        dataObject.setTicketType(10);
        dataObject.setSlaMinutes(30);
        when(ticketSlaRuleService.getRulePage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(dataObject), 1L));

        CommonResult<PageResult<TicketSlaRuleRespVO>> result = controller.getRulePage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(5001L, result.getData().getList().get(0).getId());
    }

    @Test
    void shouldPreviewMatch() {
        TicketSlaRulePreviewReqVO reqVO = new TicketSlaRulePreviewReqVO();
        reqVO.setTicketType(10);
        reqVO.setRuleCode("BLACKLIST_USER");
        TradeTicketSlaRuleMatchRespDTO respDTO = new TradeTicketSlaRuleMatchRespDTO();
        respDTO.setMatched(true);
        respDTO.setRuleId(6001L);
        respDTO.setMatchLevel(1);
        when(ticketSlaRuleService.previewMatch(any())).thenReturn(respDTO);

        CommonResult<TradeTicketSlaRuleMatchRespDTO> result = controller.previewMatch(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().getMatched());
        assertEquals(6001L, result.getData().getRuleId());
    }

}
