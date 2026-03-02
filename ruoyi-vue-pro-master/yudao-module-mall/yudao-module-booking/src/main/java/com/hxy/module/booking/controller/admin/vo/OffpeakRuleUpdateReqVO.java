package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "管理后台 - 闲时规则更新 Request VO")
@Data
public class OffpeakRuleUpdateReqVO {

    @Schema(description = "规则编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "规则编号不能为空")
    private Long id;

    @Schema(description = "规则名称", example = "工作日午间闲时")
    private String name;

    @Schema(description = "适用星期（逗号分隔，如：1,2,3,4,5）", example = "1,2,3,4,5")
    private String weekDays;

    @Schema(description = "开始时间", example = "13:00:00")
    private LocalTime startTime;

    @Schema(description = "结束时间", example = "17:00:00")
    private LocalTime endTime;

    @Schema(description = "折扣比例（如：80表示8折）", example = "80")
    private Integer discountRate;

    @Schema(description = "固定优惠价（分）", example = "8800")
    private Integer fixedPrice;

    @Schema(description = "状态（0正常 1停用）", example = "0")
    private Integer status;

}
