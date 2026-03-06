package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - booking退款回调重放批次台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingRefundReplayRunLogPageReqVO extends PageParam {

    @Schema(description = "批次号", example = "RR202603061800001234")
    private String runId;

    @Schema(description = "触发来源", example = "MANUAL")
    private String triggerSource;

    @Schema(description = "操作人", example = "admin")
    private String operator;

    @Schema(description = "批次状态(started/success/partial_fail/fail)", example = "partial_fail")
    private String status;

    @Schema(description = "是否预演", example = "false")
    private Boolean dryRun;

    @Schema(description = "开始时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] startTime;
}
