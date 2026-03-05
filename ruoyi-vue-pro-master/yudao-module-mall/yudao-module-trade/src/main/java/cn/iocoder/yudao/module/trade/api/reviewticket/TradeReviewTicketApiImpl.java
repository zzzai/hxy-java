package cn.iocoder.yudao.module.trade.api.reviewticket;

import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketUpsertReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketResolveReqDTO;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketService;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

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

}
