package cn.iocoder.yudao.module.trade.api.reviewticket.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 统一工单摘要批量查询请求 DTO。
 */
@Data
@Accessors(chain = true)
public class TradeReviewTicketSummaryQueryReqDTO implements Serializable {

    /**
     * 工单类型
     */
    private Integer ticketType;
    /**
     * 来源业务号列表
     */
    private List<String> sourceBizNos;
}

