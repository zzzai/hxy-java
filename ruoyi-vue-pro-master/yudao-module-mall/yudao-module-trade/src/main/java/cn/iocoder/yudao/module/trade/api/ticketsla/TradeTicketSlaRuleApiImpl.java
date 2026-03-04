package cn.iocoder.yudao.module.trade.api.ticketsla;

import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchReqDTO;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchRespDTO;
import cn.iocoder.yudao.module.trade.service.ticketsla.TicketSlaRuleService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * SLA 工单规则 API 实现
 */
@Service
@Validated
public class TradeTicketSlaRuleApiImpl implements TradeTicketSlaRuleApi {

    @Resource
    private TicketSlaRuleService ticketSlaRuleService;

    @Override
    public TradeTicketSlaRuleMatchRespDTO matchRule(TradeTicketSlaRuleMatchReqDTO reqDTO) {
        return ticketSlaRuleService.matchRule(reqDTO);
    }

}
