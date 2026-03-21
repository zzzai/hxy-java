package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 预约服务评价历史治理扫描 Response VO")
@Data
public class BookingReviewHistoryScanRespVO {

    @Schema(description = "扫描汇总")
    private BookingReviewHistoryScanSummaryRespVO summary;

    @Schema(description = "结果列表")
    private List<BookingReviewHistoryScanItemRespVO> list;

    @Schema(description = "结果总数", example = "8")
    private Long total;
}
