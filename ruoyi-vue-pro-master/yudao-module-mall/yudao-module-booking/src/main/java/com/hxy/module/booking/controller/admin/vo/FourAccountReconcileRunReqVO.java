package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 四账对账执行 Request VO")
@Data
public class FourAccountReconcileRunReqVO {

    @Schema(description = "业务日期，不传则默认昨天")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate bizDate;

    @Schema(description = "触发来源", example = "MANUAL")
    private String source;
}

