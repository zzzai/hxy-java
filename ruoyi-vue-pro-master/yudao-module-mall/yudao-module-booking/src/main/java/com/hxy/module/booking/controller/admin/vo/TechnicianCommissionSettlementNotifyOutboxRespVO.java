package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 技师佣金结算通知出站 Response VO")
@Data
public class TechnicianCommissionSettlementNotifyOutboxRespVO {

    @Schema(description = "出站ID", example = "1")
    private Long id;

    @Schema(description = "结算单ID", example = "1001")
    private Long settlementId;

    @Schema(description = "通知类型", example = "P1_WARN")
    private String notifyType;

    @Schema(description = "通知渠道", example = "IN_APP")
    private String channel;

    @Schema(description = "优先级", example = "P1")
    private String severity;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "重试次数", example = "2")
    private Integer retryCount;

    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "发送成功时间")
    private LocalDateTime sentTime;

    @Schema(description = "最后错误信息")
    private String lastErrorMsg;

    @Schema(description = "最近审计动作编码", example = "MANUAL_RETRY")
    private String lastActionCode;

    @Schema(description = "最近审计业务号", example = "ADMIN#1/OUTBOX#1001")
    private String lastActionBizNo;

    @Schema(description = "最近审计动作时间")
    private LocalDateTime lastActionTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
