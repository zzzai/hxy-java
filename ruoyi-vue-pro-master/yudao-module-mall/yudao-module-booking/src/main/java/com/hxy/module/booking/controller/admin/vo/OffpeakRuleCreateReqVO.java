package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "管理后台 - 闲时规则创建 Request VO")
@Data
public class OffpeakRuleCreateReqVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "门店编号不能为空")
    private Long storeId;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "工作日午间闲时")
    @NotBlank(message = "规则名称不能为空")
    private String name;

    @Schema(description = "适用星期（逗号分隔，如：1,2,3,4,5）", example = "1,2,3,4,5")
    private String weekDays;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "13:00:00")
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "17:00:00")
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    @Schema(description = "折扣比例（如：80表示8折）", example = "80")
    private Integer discountRate;

    @Schema(description = "固定优惠价（分）", example = "8800")
    private Integer fixedPrice;

}
