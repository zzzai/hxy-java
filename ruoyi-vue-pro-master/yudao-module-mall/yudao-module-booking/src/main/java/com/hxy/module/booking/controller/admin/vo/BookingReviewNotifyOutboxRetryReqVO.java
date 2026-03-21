package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 预约服务评价通知出站重试 Request VO")
@Data
public class BookingReviewNotifyOutboxRetryReqVO {

    @Schema(description = "通知出站记录 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1001]")
    @NotEmpty(message = "通知出站记录 ID 列表不能为空")
    private List<Long> ids;

    @Schema(description = "重试原因", example = "manual-retry")
    @Size(max = 255, message = "重试原因长度不能超过 255")
    private String reason;
}
