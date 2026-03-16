package com.hxy.module.booking.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "用户端 - 创建预约服务评价 Request VO")
@Data
public class AppBookingReviewCreateReqVO {

    @Schema(description = "预约订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "预约订单ID不能为空")
    private Long bookingOrderId;

    @Schema(description = "总体评分 1-5", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "总体评分不能为空")
    @Min(value = 1, message = "总体评分不能低于 1")
    @Max(value = 5, message = "总体评分不能高于 5")
    private Integer overallScore;

    @Schema(description = "服务体验评分 1-5", example = "5")
    @Min(value = 1, message = "服务体验评分不能低于 1")
    @Max(value = 5, message = "服务体验评分不能高于 5")
    private Integer serviceScore;

    @Schema(description = "技师表现评分 1-5", example = "5")
    @Min(value = 1, message = "技师表现评分不能低于 1")
    @Max(value = 5, message = "技师表现评分不能高于 5")
    private Integer technicianScore;

    @Schema(description = "门店环境评分 1-5", example = "5")
    @Min(value = 1, message = "门店环境评分不能低于 1")
    @Max(value = 5, message = "门店环境评分不能高于 5")
    private Integer environmentScore;

    @Schema(description = "标签", example = "[\"服务专业\",\"沟通耐心\"]")
    @Size(max = 8, message = "标签数量不能超过 8 个")
    private List<String> tags;

    @Schema(description = "评价内容", example = "整体体验不错，下次还会再来")
    @Size(max = 1024, message = "评价内容不能超过 1024 个字符")
    private String content;

    @Schema(description = "评价图片", example = "[\"https://example.com/review-1.png\"]")
    @Size(max = 9, message = "评价图片不能超过 9 张")
    private List<String> picUrls;

    @Schema(description = "是否匿名", example = "false")
    private Boolean anonymous;

    @Schema(description = "来源", example = "order_detail")
    private String source;
}
