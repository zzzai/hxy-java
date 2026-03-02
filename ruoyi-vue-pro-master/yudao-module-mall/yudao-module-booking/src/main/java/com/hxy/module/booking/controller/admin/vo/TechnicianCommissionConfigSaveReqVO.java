package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 技师佣金配置 Save Request VO")
@Data
public class TechnicianCommissionConfigSaveReqVO {

    @Schema(description = "配置ID（更新时传）")
    private Long id;

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @Schema(description = "佣金类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "佣金类型不能为空")
    private Integer commissionType;

    @Schema(description = "佣金比例", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "佣金比例不能为空")
    private BigDecimal rate;

    @Schema(description = "固定金额（分）")
    private Integer fixedAmount;

}
