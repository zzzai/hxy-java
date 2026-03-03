package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 售后人工复核工单 Response VO")
@Data
public class AfterSaleReviewTicketRespVO {

    @Schema(description = "工单 ID", example = "1")
    private Long id;

    @Schema(description = "工单类型", example = "10")
    private Integer ticketType;

    @Schema(description = "售后单 ID", example = "1001")
    private Long afterSaleId;

    @Schema(description = "来源业务单号", example = "BK202603010001")
    private String sourceBizNo;

    @Schema(description = "订单 ID", example = "2001")
    private Long orderId;

    @Schema(description = "订单项 ID", example = "3001")
    private Long orderItemId;

    @Schema(description = "用户 ID", example = "4001")
    private Long userId;

    @Schema(description = "规则编码", example = "BLACKLIST_USER")
    private String ruleCode;

    @Schema(description = "命中原因", example = "用户命中退款黑名单")
    private String decisionReason;

    @Schema(description = "严重级别", example = "P0")
    private String severity;

    @Schema(description = "升级对象", example = "HQ_RISK_FINANCE")
    private String escalateTo;

    @Schema(description = "命中路由 ID", example = "88")
    private Long routeId;

    @Schema(description = "命中路由作用域", example = "RULE")
    private String routeScope;

    @Schema(description = "路由决策顺序", example = "RULE>TYPE_SEVERITY>TYPE_DEFAULT>GLOBAL_DEFAULT")
    private String routeDecisionOrder;

    @Schema(description = "SLA 截止时间")
    private LocalDateTime slaDeadlineTime;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "是否逾期", example = "true")
    private Boolean overdue;

    @Schema(description = "首次触发时间")
    private LocalDateTime firstTriggerTime;

    @Schema(description = "最近触发时间")
    private LocalDateTime lastTriggerTime;

    @Schema(description = "触发次数", example = "2")
    private Integer triggerCount;

    @Schema(description = "收口时间")
    private LocalDateTime resolvedTime;

    @Schema(description = "收口人 ID", example = "1")
    private Long resolverId;

    @Schema(description = "收口人类型", example = "1")
    private Integer resolverType;

    @Schema(description = "收口动作编码", example = "MANUAL_RESOLVE")
    private String resolveActionCode;

    @Schema(description = "收口来源业务号", example = "OPS-202603020001")
    private String resolveBizNo;

    @Schema(description = "最近审计动作编码", example = "MANUAL_RESOLVE")
    private String lastActionCode;

    @Schema(description = "最近审计业务号", example = "OPS-202603020001")
    private String lastActionBizNo;

    @Schema(description = "最近审计动作时间")
    private LocalDateTime lastActionTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
