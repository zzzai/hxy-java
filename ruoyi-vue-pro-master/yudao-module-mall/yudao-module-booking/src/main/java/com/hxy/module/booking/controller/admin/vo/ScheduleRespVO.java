package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "管理后台 - 排班 Response VO")
@Data
public class ScheduleRespVO {

    @Schema(description = "排班编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "技师编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long technicianId;

    @Schema(description = "技师姓名", example = "张三")
    private String technicianName;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long storeId;

    @Schema(description = "排班日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-02-24")
    private LocalDate scheduleDate;

    @Schema(description = "星期几", example = "1")
    private Integer weekDay;

    @Schema(description = "是否休息日", example = "false")
    private Boolean isRestDay;

    @Schema(description = "上班时间", example = "09:00")
    private LocalTime workStartTime;

    @Schema(description = "下班时间", example = "21:00")
    private LocalTime workEndTime;

    @Schema(description = "备注", example = "")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
