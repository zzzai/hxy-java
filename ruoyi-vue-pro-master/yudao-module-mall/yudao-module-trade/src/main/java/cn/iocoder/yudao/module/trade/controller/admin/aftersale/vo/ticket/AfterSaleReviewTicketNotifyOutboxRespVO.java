package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 售后人工复核工单通知出站 Response VO")
@Data
public class AfterSaleReviewTicketNotifyOutboxRespVO {

    @Schema(description = "主键", example = "1001")
    private Long id;

    @Schema(description = "工单 ID", example = "2001")
    private Long ticketId;

    @Schema(description = "通知类型", example = "SLA_WARN")
    private String notifyType;

    @Schema(description = "通知渠道", example = "IN_APP")
    private String channel;

    @Schema(description = "严重级别", example = "P1")
    private String severity;

    @Schema(description = "升级对象快照", example = "HQ_AFTER_SALE")
    private String escalateTo;

    @Schema(description = "幂等业务键", example = "WARN#2001#202603041530")
    private String bizKey;

    @Schema(description = "状态（0 待发送 1 已发送 2 失败）", example = "0")
    private Integer status;

    @Schema(description = "重试次数", example = "1")
    private Integer retryCount;

    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "发送成功时间")
    private LocalDateTime sentTime;

    @Schema(description = "最后错误信息")
    private String lastErrorMsg;

    @Schema(description = "最近审计动作编码")
    private String lastActionCode;

    @Schema(description = "最近审计业务号")
    private String lastActionBizNo;

    @Schema(description = "最近审计动作时间")
    private LocalDateTime lastActionTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
