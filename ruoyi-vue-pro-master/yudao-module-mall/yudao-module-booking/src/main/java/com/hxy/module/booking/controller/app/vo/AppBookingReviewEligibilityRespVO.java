package com.hxy.module.booking.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户端 - 预约服务评价资格 Response VO")
@Data
public class AppBookingReviewEligibilityRespVO {

    @Schema(description = "预约订单ID", example = "1001")
    private Long bookingOrderId;

    @Schema(description = "是否可评价", example = "true")
    private Boolean eligible;

    @Schema(description = "是否已评价", example = "false")
    private Boolean alreadyReviewed;

    @Schema(description = "已存在的评价ID", example = "9001")
    private Long reviewId;

    @Schema(description = "当前状态原因", example = "ORDER_NOT_COMPLETED")
    private String reason;
}
