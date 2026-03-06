package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 四账对账汇总 Request VO")
@Data
public class FourAccountReconcileSummaryReqVO {

    @Schema(description = "业务日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] bizDate;

    @Schema(description = "状态（10通过 20告警）", example = "20")
    private Integer status;

    @Schema(description = "是否已关联工单", example = "true")
    private Boolean relatedTicketLinked;
}
