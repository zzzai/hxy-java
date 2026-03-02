package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 技师佣金结算单 Response VO")
@Data
public class TechnicianCommissionSettlementRespVO {

    @Schema(description = "结算单ID", example = "1")
    private Long id;

    @Schema(description = "结算单号", example = "SET20260301120000ABC123")
    private String settlementNo;

    @Schema(description = "门店ID", example = "100")
    private Long storeId;

    @Schema(description = "技师ID", example = "200")
    private Long technicianId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "佣金条目数", example = "8")
    private Integer commissionCount;

    @Schema(description = "总佣金（分）", example = "19900")
    private Integer totalCommissionAmount;

    @Schema(description = "提审时间")
    private LocalDateTime reviewSubmitTime;

    @Schema(description = "审核SLA截止")
    private LocalDateTime reviewDeadlineTime;

    @Schema(description = "是否审核超时")
    private Boolean overdue;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedTime;

    @Schema(description = "审核人", example = "1")
    private Long reviewerId;

    @Schema(description = "审核备注")
    private String reviewRemark;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "打款时间")
    private LocalDateTime paidTime;

    @Schema(description = "打款人", example = "1")
    private Long payerId;

    @Schema(description = "打款凭证号")
    private String payVoucherNo;

    @Schema(description = "打款备注")
    private String payRemark;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
