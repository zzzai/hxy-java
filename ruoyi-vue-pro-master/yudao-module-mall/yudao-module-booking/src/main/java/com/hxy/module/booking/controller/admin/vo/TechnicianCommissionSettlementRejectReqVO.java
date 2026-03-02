package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 技师佣金结算单驳回 Request VO")
@Data
public class TechnicianCommissionSettlementRejectReqVO {

    @Schema(description = "结算单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "结算单ID不能为空")
    private Long id;

    @Schema(description = "驳回原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "驳回原因不能为空")
    private String rejectReason;
}
