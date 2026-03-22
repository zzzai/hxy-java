package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 预约服务评价通知跨通道审计摘要 Response VO")
@Data
public class BookingReviewNotifyOutboxSummaryRespVO {

    @Schema(description = "评价总数", example = "100")
    private Long totalReviewCount;

    @Schema(description = "双通道已发送评价数", example = "50")
    private Long dualSentReviewCount;

    @Schema(description = "存在阻断评价数", example = "12")
    private Long blockedReviewCount;

    @Schema(description = "存在失败评价数", example = "6")
    private Long failedReviewCount;

    @Schema(description = "人工重试待复核评价数", example = "3")
    private Long manualRetryPendingReviewCount;

    @Schema(description = "跨通道分裂评价数", example = "10")
    private Long divergedReviewCount;
}
