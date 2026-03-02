package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 技师佣金结算单打款 Request VO")
@Data
public class TechnicianCommissionSettlementPayReqVO {

    @Schema(description = "结算单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "结算单ID不能为空")
    private Long id;

    @Schema(description = "打款凭证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TXN20260301001")
    @NotBlank(message = "打款凭证号不能为空")
    private String payVoucherNo;

    @Schema(description = "打款原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "出纳已打款并复核通过")
    @NotBlank(message = "打款原因不能为空")
    private String payRemark;
}
