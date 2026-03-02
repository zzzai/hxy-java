package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 设置休息日 Request VO")
@Data
public class ScheduleSetRestDayReqVO {

    @Schema(description = "排班编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "排班编号不能为空")
    private Long scheduleId;

    @Schema(description = "是否休息日", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否休息日不能为空")
    private Boolean isRestDay;

    @Schema(description = "备注", example = "临时请假")
    private String remark;

}
