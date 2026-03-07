package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - booking退款回调重放批次汇总 Response VO")
@Data
public class BookingRefundReplayRunLogSummaryRespVO {

    @Schema(description = "批次号", example = "RR202603071200000001")
    private String runId;

    @Schema(description = "批次状态(started/success/partial_fail/fail)", example = "partial_fail")
    private String runStatus;

    @Schema(description = "触发来源", example = "MANUAL")
    private String triggerSource;

    @Schema(description = "操作人", example = "admin")
    private String operator;

    @Schema(description = "是否预演", example = "false")
    private Boolean dryRun;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "扫描总数", example = "100")
    private Integer scannedCount;

    @Schema(description = "成功数", example = "80")
    private Integer successCount;

    @Schema(description = "跳过数", example = "10")
    private Integer skipCount;

    @Schema(description = "失败数", example = "10")
    private Integer failCount;

    @Schema(description = "工单同步成功数", example = "6")
    private Integer ticketSyncSuccessCount;

    @Schema(description = "工单同步跳过数", example = "2")
    private Integer ticketSyncSkipCount;

    @Schema(description = "工单同步失败数", example = "2")
    private Integer ticketSyncFailCount;

    @Schema(description = "告警数（四账刷新降级）", example = "3")
    private Integer warningCount;

    @Schema(description = "失败错误码 TopN 聚合")
    private List<SummaryBucket> topFailCodes;

    @Schema(description = "告警标签 TopN 聚合")
    private List<SummaryBucket> topWarningTags;

    @Data
    public static class SummaryBucket {

        @Schema(description = "聚合键", example = "1030004012")
        private String key;

        @Schema(description = "数量", example = "5")
        private Integer count;
    }
}
