package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 技师佣金结算单提审 Request VO")
@Data
public class TechnicianCommissionSettlementSubmitReqVO {

    @Schema(description = "结算单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "结算单ID不能为空")
    private Long id;

    @Schema(description = "SLA分钟", example = "120")
    private Integer slaMinutes;

    @Schema(description = "提审备注", example = "请财务审核")
    private String remark;
}
