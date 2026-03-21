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
}
