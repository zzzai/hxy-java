package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 预约服务评价历史治理扫描 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingReviewHistoryScanReqVO extends PageParam {

    @Schema(description = "门店ID", example = "3001")
    private Long storeId;

    @Schema(description = "预约订单ID", example = "2001")
    private Long bookingOrderId;

    @Schema(description = "风险分类", example = "MANUAL_READY")
    private String riskCategory;

    @Schema(description = "提交时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] submitTime;
}
