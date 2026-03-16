package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 预约服务评价跟进更新 Request VO")
@Data
public class BookingReviewFollowUpdateReqVO {

    @Schema(description = "评价ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "评价ID不能为空")
    private Long reviewId;

    @Schema(description = "跟进状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "跟进状态不能为空")
    private Integer followStatus;

    @Schema(description = "跟进结果")
    private String followResult;
}
