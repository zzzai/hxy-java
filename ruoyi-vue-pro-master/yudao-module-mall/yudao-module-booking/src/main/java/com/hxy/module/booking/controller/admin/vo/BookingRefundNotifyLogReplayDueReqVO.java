package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - booking退款回调到期待重放 Request VO")
@Data
public class BookingRefundNotifyLogReplayDueReqVO {

    @Schema(description = "扫描条数上限，默认 200", example = "200")
    private Integer limit;

    @Schema(description = "是否仅预演，不写业务数据与台账状态", example = "false")
    private Boolean dryRun;

    public boolean dryRunEnabled() {
        return Boolean.TRUE.equals(dryRun);
    }
}
