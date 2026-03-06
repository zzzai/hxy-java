package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - booking退款回调台账重放 Request VO")
@Data
public class BookingRefundNotifyLogReplayReqVO {

    @Schema(description = "台账ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "台账ID不能为空")
    private Long id;
}
