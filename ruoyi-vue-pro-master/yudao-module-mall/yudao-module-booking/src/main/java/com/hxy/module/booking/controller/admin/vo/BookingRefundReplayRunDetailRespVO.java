package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - booking退款回调重放批次明细 Response VO")
@Data
public class BookingRefundReplayRunDetailRespVO {

    @Schema(description = "明细ID", example = "1")
    private Long id;

    @Schema(description = "批次号", example = "RR202603071200000001")
    private String runId;

    @Schema(description = "退款回调台账ID", example = "100")
    private Long notifyLogId;

    @Schema(description = "预约订单ID", example = "1001")
    private Long orderId;

    @Schema(description = "支付退款单ID", example = "9001")
    private Long payRefundId;

    @Schema(description = "重放结果状态", example = "FAIL")
    private String resultStatus;

    @Schema(description = "重放结果码", example = "1030004012")
    private String resultCode;

    @Schema(description = "重放结果消息", example = "预约订单退款回调幂等冲突")
    private String resultMsg;

    @Schema(description = "告警标签", example = "FOUR_ACCOUNT_REFRESH_WARN")
    private String warningTag;

    @Schema(description = "工单同步状态", example = "SUCCESS")
    private String ticketSyncStatus;

    @Schema(description = "工单ID", example = "20001")
    private Long ticketId;

    @Schema(description = "工单同步时间")
    private LocalDateTime ticketSyncTime;

    @Schema(description = "工单同步错误码", example = "TradeApiException")
    private String ticketSyncErrorCode;

    @Schema(description = "工单同步错误信息")
    private String ticketSyncErrorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
