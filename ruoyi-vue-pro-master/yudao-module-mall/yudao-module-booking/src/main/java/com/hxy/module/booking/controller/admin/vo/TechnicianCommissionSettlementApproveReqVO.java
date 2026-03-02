package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 技师佣金结算单审批通过 Request VO")
@Data
public class TechnicianCommissionSettlementApproveReqVO {

    @Schema(description = "结算单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "结算单ID不能为空")
    private Long id;

    @Schema(description = "审批备注", example = "审核通过")
    private String remark;
}
