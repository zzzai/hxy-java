package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 预约评价店长账号路由分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingReviewManagerAccountRoutingPageReqVO extends PageParam {

    @Schema(description = "门店ID", example = "3001")
    private Long storeId;

    @Schema(description = "门店名称", example = "朝阳门店")
    private String storeName;

    @Schema(description = "联系人手机号", example = "13900000000")
    private String contactMobile;

    @Schema(description = "是否只看缺任一绑定")
    private Boolean onlyMissingAny;

    @Schema(description = "路由状态", example = "ACTIVE_ROUTE")
    private String routingStatus;

    @Schema(description = "App 路由状态", example = "APP_MISSING")
    private String appRoutingStatus;

    @Schema(description = "企微路由状态", example = "WECOM_MISSING")
    private String wecomRoutingStatus;

    @Schema(description = "治理分组", example = "IMMEDIATE_FIX")
    private String governanceStage;

    @Schema(description = "核验状态", example = "STALE_VERIFY")
    private String verificationFreshnessStatus;

    @Schema(description = "来源闭环状态", example = "SOURCE_PENDING")
    private String sourceClosureStatus;
}
