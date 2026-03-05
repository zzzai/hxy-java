package cn.iocoder.yudao.module.trade.api.reviewticket;

import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketUpsertReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketResolveReqDTO;

/**
 * 统一工单跨模块 API。
 */
public interface TradeReviewTicketApi {

    /**
     * 按 {@code ticketType + sourceBizNo} 幂等创建或刷新工单。
     *
     * @param reqDTO 请求参数
     * @return 工单 ID
     */
    Long upsertReviewTicket(TradeReviewTicketUpsertReqDTO reqDTO);

    /**
     * 按 {@code ticketType + sourceBizNo} 收口工单。
     *
     * @param reqDTO 请求参数
     * @return 是否成功收口
     */
    boolean resolveReviewTicketBySourceBizNo(TradeReviewTicketResolveReqDTO reqDTO);

}
