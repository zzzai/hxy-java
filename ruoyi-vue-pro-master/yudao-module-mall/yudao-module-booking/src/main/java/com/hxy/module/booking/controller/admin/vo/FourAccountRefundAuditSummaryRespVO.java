package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 四账退款审计汇总 Response VO")
@Data
public class FourAccountRefundAuditSummaryRespVO {

    @Schema(description = "总笔数", example = "12")
    private Long totalCount;

    @Schema(description = "差异金额聚合（分）", example = "3500")
    private Long differenceAmountSum;

    @Schema(description = "未收口工单数", example = "2")
    private Long unresolvedTicketCount;

    @Schema(description = "工单摘要是否降级", example = "false")
    private Boolean ticketSummaryDegraded;

    @Schema(description = "按退款审计状态聚合")
    private List<CountItem> statusAgg;

    @Schema(description = "按退款异常类型聚合")
    private List<CountItem> exceptionTypeAgg;

    @Schema(description = "聚合项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountItem {

        @Schema(description = "键", example = "WARN")
        private String key;

        @Schema(description = "数量", example = "8")
        private Long count;
    }
}
