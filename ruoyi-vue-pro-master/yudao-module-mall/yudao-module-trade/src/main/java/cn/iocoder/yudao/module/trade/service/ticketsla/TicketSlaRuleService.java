package cn.iocoder.yudao.module.trade.service.ticketsla;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchReqDTO;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchRespDTO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRulePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRulePreviewReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.ticketsla.TicketSlaRuleDO;

/**
 * SLA 工单规则 Service
 */
public interface TicketSlaRuleService {

    PageResult<TicketSlaRuleDO> getRulePage(TicketSlaRulePageReqVO pageReqVO);

    TicketSlaRuleDO getRule(Long id);

    Long createRule(TicketSlaRuleCreateReqVO reqVO);

    void updateRule(TicketSlaRuleUpdateReqVO reqVO);

    void updateRuleStatus(Long id, Boolean enabled);

    TradeTicketSlaRuleMatchRespDTO previewMatch(TicketSlaRulePreviewReqVO reqVO);

    TradeTicketSlaRuleMatchRespDTO matchRule(TradeTicketSlaRuleMatchReqDTO reqDTO);

}
