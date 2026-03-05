package cn.iocoder.yudao.module.trade.api.reviewticket;

import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketUpsertReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketResolveReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryQueryReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryRespDTO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketService;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一工单跨模块 API 实现。
 */
@Service
@Validated
public class TradeReviewTicketApiImpl implements TradeReviewTicketApi {

    @Resource
    private AfterSaleReviewTicketService afterSaleReviewTicketService;

    @Override
    public Long upsertReviewTicket(TradeReviewTicketUpsertReqDTO reqDTO) {
        if (reqDTO == null) {
            return null;
        }
        AfterSaleReviewTicketCreateReqBO reqBO = new AfterSaleReviewTicketCreateReqBO();
        reqBO.setTicketType(reqDTO.getTicketType());
        reqBO.setAfterSaleId(reqDTO.getAfterSaleId());
        reqBO.setOrderId(reqDTO.getOrderId());
        reqBO.setOrderItemId(reqDTO.getOrderItemId());
        reqBO.setUserId(reqDTO.getUserId());
        reqBO.setSourceBizNo(reqDTO.getSourceBizNo());
        reqBO.setRuleCode(reqDTO.getRuleCode());
        reqBO.setDecisionReason(reqDTO.getDecisionReason());
        reqBO.setSeverity(reqDTO.getSeverity());
        reqBO.setEscalateTo(reqDTO.getEscalateTo());
        reqBO.setSlaMinutes(reqDTO.getSlaMinutes());
        reqBO.setRemark(reqDTO.getRemark());
        return afterSaleReviewTicketService.upsertReviewTicketBySourceBizNo(reqBO, reqDTO.getActionCode());
    }

    @Override
    public boolean resolveReviewTicketBySourceBizNo(TradeReviewTicketResolveReqDTO reqDTO) {
        if (reqDTO == null) {
            return false;
        }
        return afterSaleReviewTicketService.resolveReviewTicketBySourceBizNo(
                reqDTO.getTicketType(), reqDTO.getSourceBizNo(), reqDTO.getResolverId(), reqDTO.getResolverType(),
                reqDTO.getResolveActionCode(), reqDTO.getResolveBizNo(), reqDTO.getResolveRemark());
    }

    @Override
    public List<TradeReviewTicketSummaryRespDTO> listLatestTicketSummaryBySourceBizNos(
            TradeReviewTicketSummaryQueryReqDTO reqDTO) {
        if (reqDTO == null || reqDTO.getTicketType() == null || CollectionUtils.isEmpty(reqDTO.getSourceBizNos())) {
            return Collections.emptyList();
        }
        List<AfterSaleReviewTicketDO> tickets = afterSaleReviewTicketService.listLatestByTicketTypeAndSourceBizNos(
                reqDTO.getTicketType(), reqDTO.getSourceBizNos());
        if (CollectionUtils.isEmpty(tickets)) {
            return Collections.emptyList();
        }
        return tickets.stream().map(ticket -> new TradeReviewTicketSummaryRespDTO()
                        .setId(ticket.getId())
                        .setTicketType(ticket.getTicketType())
                        .setSourceBizNo(ticket.getSourceBizNo())
                        .setStatus(ticket.getStatus())
                        .setSeverity(ticket.getSeverity())
                        .setResolvedTime(ticket.getResolvedTime()))
                .collect(Collectors.toList());
    }

}
