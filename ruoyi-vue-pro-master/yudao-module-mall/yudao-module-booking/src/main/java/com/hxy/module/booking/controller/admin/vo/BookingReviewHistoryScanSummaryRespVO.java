package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 预约服务评价历史治理扫描汇总 Response VO")
@Data
public class BookingReviewHistoryScanSummaryRespVO {

    @Schema(description = "扫描总量", example = "16")
    private Long scannedCount;

    @Schema(description = "可人工推进数量", example = "8")
    private Long manualReadyCount;

    @Schema(description = "高风险待核实数量", example = "4")
    private Long highRiskCount;

    @Schema(description = "不在本轮范围数量", example = "4")
    private Long outOfScopeCount;
}
