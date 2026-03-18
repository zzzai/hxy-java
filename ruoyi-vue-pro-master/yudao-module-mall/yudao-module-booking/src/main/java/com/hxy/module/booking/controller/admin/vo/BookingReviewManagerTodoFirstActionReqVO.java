package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 预约服务评价店长待办首次处理 Request VO")
@Data
public class BookingReviewManagerTodoFirstActionReqVO {

    @Schema(description = "评价ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "评价ID不能为空")
    private Long reviewId;

    @Schema(description = "首次处理说明", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "首次处理说明不能为空")
    private String remark;
}
