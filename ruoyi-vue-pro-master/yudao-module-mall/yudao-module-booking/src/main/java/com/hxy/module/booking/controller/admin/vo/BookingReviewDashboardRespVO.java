package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 预约服务评价看板汇总 Response VO")
@Data
public class BookingReviewDashboardRespVO {

    @Schema(description = "总评价数", example = "100")
    private Long totalCount;

    @Schema(description = "好评数", example = "80")
    private Long positiveCount;

    @Schema(description = "中评数", example = "15")
    private Long neutralCount;

    @Schema(description = "差评数", example = "5")
    private Long negativeCount;

    @Schema(description = "待处理数", example = "3")
    private Long pendingFollowCount;

    @Schema(description = "紧急数", example = "2")
    private Long urgentCount;

    @Schema(description = "已回复数", example = "60")
    private Long repliedCount;

    @Schema(description = "店长待认领数", example = "2")
    private Long managerTodoPendingCount;

    @Schema(description = "店长认领超时数", example = "1")
    private Long managerTodoClaimTimeoutCount;

    @Schema(description = "店长首次处理超时数", example = "1")
    private Long managerTodoFirstActionTimeoutCount;

    @Schema(description = "店长闭环超时数", example = "1")
    private Long managerTodoCloseTimeoutCount;

    @Schema(description = "店长已闭环数", example = "3")
    private Long managerTodoClosedCount;
}
