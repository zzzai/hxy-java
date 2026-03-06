package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 四账对账汇总 Response VO")
@Data
public class FourAccountReconcileSummaryRespVO {

    @Schema(description = "总笔数", example = "31")
    private Long totalCount;

    @Schema(description = "通过数", example = "28")
    private Long passCount;

    @Schema(description = "告警数", example = "3")
    private Long warnCount;

    @Schema(description = "差额聚合：交易-履约（分）", example = "1200")
    private Long tradeMinusFulfillmentSum;

    @Schema(description = "差额聚合：交易-(提成+分账)（分）", example = "5400")
    private Long tradeMinusCommissionSplitSum;

    @Schema(description = "未收口工单数", example = "2")
    private Long unresolvedTicketCount;

    @Schema(description = "工单摘要是否降级", example = "false")
    private Boolean ticketSummaryDegraded;
}
