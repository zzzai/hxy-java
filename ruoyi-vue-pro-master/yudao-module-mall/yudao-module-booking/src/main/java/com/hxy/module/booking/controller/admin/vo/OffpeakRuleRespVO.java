package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "管理后台 - 闲时规则 Response VO")
@Data
public class OffpeakRuleRespVO {

    @Schema(description = "规则编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long storeId;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "工作日午间闲时")
    private String name;

    @Schema(description = "适用星期（逗号分隔，如：1,2,3,4,5）", example = "1,2,3,4,5")
    private String weekDays;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "13:00:00")
    private LocalTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "17:00:00")
    private LocalTime endTime;

    @Schema(description = "折扣比例（如：80表示8折）", example = "80")
    private Integer discountRate;

    @Schema(description = "固定优惠价（分）", example = "8800")
    private Integer fixedPrice;

    @Schema(description = "状态（0正常 1停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
