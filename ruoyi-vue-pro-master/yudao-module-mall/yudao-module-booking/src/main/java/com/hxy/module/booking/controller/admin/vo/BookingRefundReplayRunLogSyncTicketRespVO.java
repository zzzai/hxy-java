package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - booking退款回调重放批次同步工单 Response VO")
@Data
public class BookingRefundReplayRunLogSyncTicketRespVO {

    @Schema(description = "批次号", example = "RR202603071200000001")
    private String runId;

    @Schema(description = "尝试同步数量", example = "10")
    private Integer attemptedCount;

    @Schema(description = "同步成功数量", example = "8")
    private Integer successCount;

    @Schema(description = "同步失败数量", example = "2")
    private Integer failedCount;

    @Schema(description = "同步失败的明细ID列表")
    private List<Long> failedIds;
}
