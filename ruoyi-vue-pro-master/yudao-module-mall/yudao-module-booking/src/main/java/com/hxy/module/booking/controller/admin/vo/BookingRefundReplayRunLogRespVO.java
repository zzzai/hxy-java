package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - booking退款回调重放批次台账 Response VO")
@Data
public class BookingRefundReplayRunLogRespVO {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "批次号", example = "RR202603061800001234")
    private String runId;

    @Schema(description = "触发来源", example = "MANUAL")
    private String triggerSource;

    @Schema(description = "操作人", example = "admin")
    private String operator;

    @Schema(description = "是否预演", example = "false")
    private Boolean dryRun;

    @Schema(description = "扫描上限", example = "200")
    private Integer limitSize;

    @Schema(description = "扫描总数", example = "180")
    private Integer scannedCount;

    @Schema(description = "成功数", example = "160")
    private Integer successCount;

    @Schema(description = "跳过数", example = "10")
    private Integer skipCount;

    @Schema(description = "失败数", example = "10")
    private Integer failCount;

    @Schema(description = "批次状态(started/success/partial_fail/fail)", example = "partial_fail")
    private String status;

    @Schema(description = "错误/告警摘要")
    private String errorMsg;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
