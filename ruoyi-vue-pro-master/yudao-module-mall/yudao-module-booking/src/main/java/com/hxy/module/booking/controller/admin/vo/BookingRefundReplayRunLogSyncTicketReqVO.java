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

    @Schema(description = "是否仅预演，不执行工单同步", example = "false")
    private Boolean dryRun;

    @Schema(description = "是否强制重同步，默认 false", example = "false")
    private Boolean forceResync;

    public boolean onlyFailEnabled() {
        return onlyFail == null || Boolean.TRUE.equals(onlyFail);
    }

    public boolean dryRunEnabled() {
        return Boolean.TRUE.equals(dryRun);
    }

    public boolean forceResyncEnabled() {
        return Boolean.TRUE.equals(forceResync);
    }
}
