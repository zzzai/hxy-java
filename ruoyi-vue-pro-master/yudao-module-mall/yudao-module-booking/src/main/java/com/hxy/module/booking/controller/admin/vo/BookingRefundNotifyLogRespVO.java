package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - booking退款回调台账 Response VO")
@Data
public class BookingRefundNotifyLogRespVO {

    @Schema(description = "台账ID", example = "1")
    private Long id;

    @Schema(description = "预约订单ID", example = "1001")
    private Long orderId;

    @Schema(description = "商户退款单号", example = "1001-refund")
    private String merchantRefundId;

    @Schema(description = "支付退款单ID", example = "88001")
    private Long payRefundId;

    @Schema(description = "状态(success/fail)", example = "fail")
    private String status;

    @Schema(description = "错误码", example = "1030004012")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "原始载荷")
    private String rawPayload;

    @Schema(description = "重试次数", example = "2")
    private Integer retryCount;

    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "最近重放操作人")
    private String lastReplayOperator;

    @Schema(description = "最近重放时间")
    private LocalDateTime lastReplayTime;

    @Schema(description = "最近重放结果", example = "SUCCESS")
    private String lastReplayResult;

    @Schema(description = "最近重放备注")
    private String lastReplayRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
