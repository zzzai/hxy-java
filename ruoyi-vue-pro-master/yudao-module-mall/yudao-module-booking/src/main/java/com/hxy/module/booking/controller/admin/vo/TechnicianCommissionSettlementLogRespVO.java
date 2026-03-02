package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 技师佣金结算单操作日志 Response VO")
@Data
public class TechnicianCommissionSettlementLogRespVO {

    @Schema(description = "日志ID", example = "1")
    private Long id;

    @Schema(description = "结算单ID", example = "1001")
    private Long settlementId;

    @Schema(description = "动作", example = "SUBMIT_REVIEW")
    private String action;

    @Schema(description = "源状态", example = "0")
    private Integer fromStatus;

    @Schema(description = "目标状态", example = "10")
    private Integer toStatus;

    @Schema(description = "操作人ID", example = "1")
    private Long operatorId;

    @Schema(description = "操作人类型", example = "ADMIN")
    private String operatorType;

    @Schema(description = "操作备注")
    private String operateRemark;

    @Schema(description = "操作时间")
    private LocalDateTime actionTime;
}

