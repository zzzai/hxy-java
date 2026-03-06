package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - booking退款回调台账重放 Response VO")
@Data
public class BookingRefundNotifyLogReplayRespVO {

    @Schema(description = "成功数", example = "2")
    private Integer successCount;

    @Schema(description = "跳过数", example = "1")
    private Integer skipCount;

    @Schema(description = "失败数", example = "1")
    private Integer failCount;

    @Schema(description = "逐条结果")
    private List<ReplayDetail> details;

    @Schema(description = "单条重放结果")
    @Data
    public static class ReplayDetail {

        @Schema(description = "台账ID", example = "100")
        private Long id;

        @Schema(description = "预约订单ID", example = "1001")
        private Long orderId;

        @Schema(description = "支付退款单ID", example = "88001")
        private Long payRefundId;

        @Schema(description = "结果状态", example = "SUCCESS")
        private String resultStatus;

        @Schema(description = "结果码", example = "OK")
        private String resultCode;

        @Schema(description = "结果消息", example = "重放成功")
        private String resultMsg;

        @Schema(description = "结果消息（兼容字段）", example = "重放成功")
        private String resultMessage;
    }
}
