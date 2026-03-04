package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 四账对账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FourAccountReconcilePageReqVO extends PageParam {

    @Schema(description = "对账流水号", example = "FAR20260304ABCD12")
    private String reconcileNo;

    @Schema(description = "业务日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] bizDate;

    @Schema(description = "状态（10通过 20告警）", example = "20")
    private Integer status;

    @Schema(description = "触发来源", example = "JOB_DAILY")
    private String source;

    @Schema(description = "问题编码（模糊匹配）", example = "FULFILLMENT_GT_TRADE")
    private String issueCode;
}

