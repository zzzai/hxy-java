package com.hxy.module.booking.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "用户端 - 时间槽 Response VO")
@Data
public class AppTimeSlotRespVO {

    @Schema(description = "时间槽编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "技师编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long technicianId;

    @Schema(description = "技师姓名", example = "张三")
    private String technicianName;

    @Schema(description = "技师头像", example = "https://xxx.com/avatar.jpg")
    private String technicianAvatar;

    @Schema(description = "日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-02-24")
    private LocalDate slotDate;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "09:00")
    private LocalTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "10:00")
    private LocalTime endTime;

    @Schema(description = "是否闲时", example = "true")
    private Boolean isOffpeak;

    @Schema(description = "闲时价格（分）", example = "8000")
    private Integer offpeakPrice;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
