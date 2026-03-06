package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 四账退款提成巡检 Response VO")
@Data
public class FourAccountRefundCommissionAuditRespVO {

    @Schema(description = "交易订单ID", example = "10001")
    private Long orderId;

    @Schema(description = "交易订单号", example = "T202603060001")
    private String tradeOrderNo;

    @Schema(description = "用户ID", example = "20001")
    private Long userId;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "退款金额（分）", example = "8800")
    private Integer refundPrice;

    @Schema(description = "已结算正向提成金额（分）", example = "1200")
    private Integer settledCommissionAmount;

    @Schema(description = "有效冲正提成金额（绝对值，分）", example = "1200")
    private Integer reversalCommissionAmountAbs;

    @Schema(description = "有效提成净额（分，status in pending/settled）", example = "0")
    private Integer activeCommissionAmount;

    @Schema(description = "期望冲正金额（分）", example = "1200")
    private Integer expectedReversalAmount;

    @Schema(description = "退款单编号", example = "90001")
    private Long payRefundId;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "退款上限来源", example = "CHILD_LEDGER")
    private String refundLimitSource;

    @Schema(description = "退款证据 JSON")
    private String refundEvidenceJson;

    @Schema(description = "异常类型", example = "REVERSAL_AMOUNT_MISMATCH")
    private String mismatchType;

    @Schema(description = "异常原因说明")
    private String mismatchReason;

}
