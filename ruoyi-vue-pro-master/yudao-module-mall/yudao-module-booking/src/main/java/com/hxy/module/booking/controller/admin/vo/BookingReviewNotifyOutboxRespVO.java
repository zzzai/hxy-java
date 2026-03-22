package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 预约服务评价通知出站 Response VO")
@Data
public class BookingReviewNotifyOutboxRespVO {

    @Schema(description = "出站ID", example = "1")
    private Long id;

    @Schema(description = "评价ID", example = "1001")
    private Long reviewId;

    @Schema(description = "门店ID", example = "2001")
    private Long storeId;

    @Schema(description = "接收角色", example = "STORE_MANAGER")
    private String receiverRole;

    @Schema(description = "接收账号ID", example = "3001")
    private Long receiverUserId;

    @Schema(description = "接收账号", example = "wecom-manager-3001")
    private String receiverAccount;

    @Schema(description = "通知类型", example = "NEGATIVE_REVIEW_CREATED")
    private String notifyType;

    @Schema(description = "通知渠道", example = "IN_APP")
    private String channel;

    @Schema(description = "通知状态", example = "PENDING")
    private String status;

    @Schema(description = "标准化诊断码", example = "BLOCKED_NO_MANAGER_ROUTE")
    private String diagnosticCode;

    @Schema(description = "标准化诊断结论", example = "缺店长路由")
    private String diagnosticLabel;

    @Schema(description = "标准化诊断说明")
    private String diagnosticDetail;

    @Schema(description = "修复建议")
    private String repairHint;

    @Schema(description = "是否允许人工重试", example = "false")
    private Boolean manualRetryAllowed;

    @Schema(description = "重试次数", example = "0")
    private Integer retryCount;

    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "发送成功时间")
    private LocalDateTime sentTime;

    @Schema(description = "最后错误信息")
    private String lastErrorMsg;

    @Schema(description = "最近动作编码", example = "CREATE_OUTBOX")
    private String lastActionCode;

    @Schema(description = "最近动作业务号", example = "REVIEW#1001")
    private String lastActionBizNo;

    @Schema(description = "最近动作说明", example = "人工重新入队")
    private String actionLabel;

    @Schema(description = "最近动作执行人说明", example = "管理员#88")
    private String actionOperatorLabel;

    @Schema(description = "最近动作原因")
    private String actionReason;

    @Schema(description = "跨通道审计阶段", example = "MANUAL_RETRY_PENDING")
    private String reviewAuditStage;

    @Schema(description = "跨通道审计结论", example = "人工重试后待复核")
    private String reviewAuditLabel;

    @Schema(description = "跨通道审计说明")
    private String reviewAuditDetail;

    @Schema(description = "最近动作时间")
    private LocalDateTime lastActionTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
