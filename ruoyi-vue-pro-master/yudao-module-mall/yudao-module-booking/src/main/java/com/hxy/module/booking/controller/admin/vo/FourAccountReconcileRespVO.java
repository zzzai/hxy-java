package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 四账对账 Response VO")
@Data
public class FourAccountReconcileRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "对账流水号", example = "FAR20260304ABCD12")
    private String reconcileNo;

    @Schema(description = "业务日期")
    private LocalDate bizDate;

    @Schema(description = "来源业务号", example = "FOUR_ACCOUNT_RECONCILE:2026-03-05")
    private String sourceBizNo;

    @Schema(description = "交易账净额（分）", example = "10000")
    private Integer tradeAmount;

    @Schema(description = "履约账金额（分）", example = "9800")
    private Integer fulfillmentAmount;

    @Schema(description = "提成账金额（分）", example = "2200")
    private Integer commissionAmount;

    @Schema(description = "分账账金额（分）", example = "500")
    private Integer splitAmount;

    @Schema(description = "差额：交易-履约（分）", example = "200")
    private Integer tradeMinusFulfillment;

    @Schema(description = "差额：交易-(提成+分账)（分）", example = "7300")
    private Integer tradeMinusCommissionSplit;

    @Schema(description = "状态（10通过 20告警）", example = "10")
    private Integer status;

    @Schema(description = "问题数量", example = "0")
    private Integer issueCount;

    @Schema(description = "问题编码（逗号分隔）")
    private String issueCodes;

    @Schema(description = "问题明细 JSON")
    private String issueDetailJson;

    @Schema(description = "退款单编号", example = "10001")
    private Long payRefundId;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "退款上限来源", example = "CHILD_LEDGER")
    private String refundLimitSource;

    @Schema(description = "退款异常类型", example = "REFUND_WITHOUT_REVERSAL")
    private String refundExceptionType;

    @Schema(description = "退款审计状态（PASS/WARN）", example = "WARN")
    private String refundAuditStatus;

    @Schema(description = "退款审计备注")
    private String refundAuditRemark;

    @Schema(description = "退款审计证据 JSON 原文")
    private String refundEvidenceJson;

    @Schema(description = "退款审计证据（结构化）")
    private Object refundEvidence;

    @Schema(description = "退款审计证据是否解析失败", example = "false")
    private Boolean refundEvidenceJsonParseError;

    @Schema(description = "触发来源", example = "JOB_DAILY")
    private String source;

    @Schema(description = "操作人", example = "SYSTEM")
    private String operator;

    @Schema(description = "对账执行时间")
    private LocalDateTime reconciledAt;

    @Schema(description = "关联工单 ID", example = "1001")
    private Long relatedTicketId;

    @Schema(description = "关联工单状态（10待处理 20已收口）", example = "10")
    private Integer relatedTicketStatus;

    @Schema(description = "关联工单严重级别", example = "P1")
    private String relatedTicketSeverity;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
