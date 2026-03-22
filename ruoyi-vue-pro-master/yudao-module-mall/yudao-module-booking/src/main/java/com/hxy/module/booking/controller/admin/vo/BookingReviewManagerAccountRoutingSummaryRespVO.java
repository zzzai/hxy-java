package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 预约评价店长账号路由覆盖率摘要 Response VO")
@Data
public class BookingReviewManagerAccountRoutingSummaryRespVO {

    @Schema(description = "总门店数", example = "1000")
    private Long totalStoreCount;

    @Schema(description = "双通道就绪数", example = "820")
    private Long dualReadyCount;

    @Schema(description = "App 覆盖数", example = "910")
    private Long appReadyCount;

    @Schema(description = "企微覆盖数", example = "860")
    private Long wecomReadyCount;

    @Schema(description = "缺任一绑定数", example = "180")
    private Long missingAnyCount;

    @Schema(description = "缺 App 数", example = "90")
    private Long missingAppCount;

    @Schema(description = "缺企微数", example = "140")
    private Long missingWecomCount;

    @Schema(description = "双缺失数", example = "50")
    private Long missingBothCount;

    @Schema(description = "立即治理数", example = "120")
    private Long immediateFixCount;

    @Schema(description = "来源待闭环数", example = "80")
    private Long verifySourceCount;

    @Schema(description = "长期未核验数", example = "90")
    private Long staleVerifyCount;

    @Schema(description = "来源待登记数", example = "60")
    private Long sourcePendingCount;

    @Schema(description = "可观察就绪数", example = "700")
    private Long observeReadyCount;
}
