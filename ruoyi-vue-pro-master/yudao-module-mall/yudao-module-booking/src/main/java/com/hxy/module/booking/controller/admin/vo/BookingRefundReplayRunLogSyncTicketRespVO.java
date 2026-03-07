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

    @Schema(description = "同步跳过数量", example = "1")
    private Integer skipCount;

    @Schema(description = "同步失败数量", example = "2")
    private Integer failedCount;

    @Schema(description = "同步失败的明细ID列表")
    private List<Long> failedIds;

    @Schema(description = "逐条同步结果")
    private List<SyncDetail> details;

    @Schema(description = "单条同步结果")
    @Data
    public static class SyncDetail {

        @Schema(description = "退款回调台账ID", example = "100")
        private Long notifyLogId;

        @Schema(description = "工单ID", example = "20001")
        private Long ticketId;

        @Schema(description = "结果码", example = "OK")
        private String resultCode;

        @Schema(description = "结果消息", example = "同步成功")
        private String resultMsg;
    }
}
