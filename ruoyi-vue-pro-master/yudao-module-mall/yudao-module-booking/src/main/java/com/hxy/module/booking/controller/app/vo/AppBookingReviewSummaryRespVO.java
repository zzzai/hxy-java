package com.hxy.module.booking.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户端 - 预约服务评价汇总 Response VO")
@Data
public class AppBookingReviewSummaryRespVO {

    @Schema(description = "总评价数", example = "12")
    private Long totalCount;

    @Schema(description = "好评数", example = "10")
    private Long positiveCount;

    @Schema(description = "中评数", example = "1")
    private Long neutralCount;

    @Schema(description = "差评数", example = "1")
    private Long negativeCount;

    @Schema(description = "平均分（四舍五入）", example = "4")
    private Integer averageScore;
}
