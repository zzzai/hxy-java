package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 技师佣金结算单创建 Request VO")
@Data
public class TechnicianCommissionSettlementCreateReqVO {

    @Schema(description = "佣金记录ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "佣金记录ID列表不能为空")
    private List<Long> commissionIds;

    @Schema(description = "备注", example = "2月技师结算")
    private String remark;
}
