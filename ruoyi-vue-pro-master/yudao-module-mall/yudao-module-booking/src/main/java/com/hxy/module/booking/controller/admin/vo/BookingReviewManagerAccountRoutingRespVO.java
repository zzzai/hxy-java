package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 预约评价店长账号路由 Response VO")
@Data
public class BookingReviewManagerAccountRoutingRespVO {

    @Schema(description = "门店ID", example = "3001")
    private Long storeId;

    @Schema(description = "门店名称", example = "朝阳门店")
    private String storeName;

    @Schema(description = "门店联系人", example = "王店长")
    private String contactName;

    @Schema(description = "门店联系人手机号", example = "13900000000")
    private String contactMobile;

    @Schema(description = "店长后台账号ID", example = "7001")
    private Long managerAdminUserId;

    @Schema(description = "店长企微账号ID", example = "wecom-manager-7001")
    private String managerWecomUserId;

    @Schema(description = "路由绑定状态", example = "ACTIVE")
    private String bindingStatus;

    @Schema(description = "生效时间")
    private LocalDateTime effectiveTime;

    @Schema(description = "失效时间")
    private LocalDateTime expireTime;

    @Schema(description = "来源", example = "MANUAL_BIND")
    private String source;

    @Schema(description = "最近核验时间")
    private LocalDateTime lastVerifiedTime;

    @Schema(description = "路由状态", example = "NO_ROUTE")
    private String routingStatus;

    @Schema(description = "路由结论", example = "未绑定店长账号")
    private String routingLabel;

    @Schema(description = "路由说明")
    private String routingDetail;

    @Schema(description = "修复建议")
    private String repairHint;

    @Schema(description = "App 路由状态", example = "APP_READY")
    private String appRoutingStatus;

    @Schema(description = "App 路由结论", example = "App 路由有效")
    private String appRoutingLabel;

    @Schema(description = "App 修复建议")
    private String appRepairHint;

    @Schema(description = "企微路由状态", example = "WECOM_READY")
    private String wecomRoutingStatus;

    @Schema(description = "企微路由结论", example = "企微路由有效")
    private String wecomRoutingLabel;

    @Schema(description = "企微修复建议")
    private String wecomRepairHint;

    @Schema(description = "治理分组", example = "IMMEDIATE_FIX")
    private String governanceStage;

    @Schema(description = "治理分组标签", example = "立即治理")
    private String governanceStageLabel;

    @Schema(description = "治理优先级", example = "P0")
    private String governancePriority;

    @Schema(description = "治理优先级标签", example = "P0 立即治理")
    private String governancePriorityLabel;

    @Schema(description = "核验状态", example = "STALE_VERIFY")
    private String verificationFreshnessStatus;

    @Schema(description = "核验状态标签", example = "长期未核验")
    private String verificationFreshnessLabel;

    @Schema(description = "来源闭环状态", example = "SOURCE_PENDING")
    private String sourceClosureStatus;

    @Schema(description = "来源闭环状态标签", example = "来源待闭环")
    private String sourceClosureLabel;

    @Schema(description = "来源真值阶段", example = "ROUTE_CONFIRMED")
    private String sourceTruthStage;

    @Schema(description = "来源真值结论", example = "来源已确认")
    private String sourceTruthLabel;

    @Schema(description = "来源真值说明")
    private String sourceTruthDetail;

    @Schema(description = "来源真值下一步动作")
    private String sourceTruthActionHint;

    @Schema(description = "治理归口", example = "企微账号治理")
    private String governanceOwnerLabel;

    @Schema(description = "治理交接摘要")
    private String governanceActionSummary;
}
