package cn.iocoder.yudao.module.trade.api.ticketsla;

import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchReqDTO;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchRespDTO;

/**
 * SLA 工单规则 API
 */
public interface TradeTicketSlaRuleApi {

    /**
     * 按优先级匹配 SLA 工单规则。
     */
    TradeTicketSlaRuleMatchRespDTO matchRule(TradeTicketSlaRuleMatchReqDTO reqDTO);

}
