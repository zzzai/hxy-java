package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 预约服务评价历史治理扫描项 Response VO")
@Data
public class BookingReviewHistoryScanItemRespVO {

    @Schema(description = "评价ID", example = "1001")
    private Long reviewId;

    @Schema(description = "预约订单ID", example = "2001")
    private Long bookingOrderId;

    @Schema(description = "门店ID", example = "3001")
    private Long storeId;

    @Schema(description = "门店名称", example = "朝阳门店")
    private String storeName;

    @Schema(description = "技师ID", example = "4001")
    private Long technicianId;

    @Schema(description = "技师名称", example = "李技师")
    private String technicianName;

    @Schema(description = "会员ID", example = "5001")
    private Long memberId;

    @Schema(description = "会员昵称", example = "安心会员")
    private String memberNickname;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "当前店长待办状态", example = "1")
    private Integer managerTodoStatus;

    @Schema(description = "风险分类", example = "MANUAL_READY")
    private String riskCategory;

    @Schema(description = "风险原因")
    private List<String> riskReasons;

    @Schema(description = "风险摘要", example = "可人工进入详情页推进")
    private String riskSummary;
}
