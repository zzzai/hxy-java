package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - booking退款回调重放批次同步工单 Request VO")
@Data
public class BookingRefundReplayRunLogSyncTicketReqVO {

    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "RR202603071200000001")
    @NotBlank(message = "runId 不能为空")
    private String runId;

    @Schema(description = "是否仅同步失败明细，默认 true", example = "true")
    private Boolean onlyFail;

    public boolean onlyFailEnabled() {
        return onlyFail == null || Boolean.TRUE.equals(onlyFail);
    }
}
