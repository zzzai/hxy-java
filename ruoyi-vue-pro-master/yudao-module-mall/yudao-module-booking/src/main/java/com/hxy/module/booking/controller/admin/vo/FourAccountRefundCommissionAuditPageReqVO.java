package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 四账退款提成巡检分页 Request VO")
@Data
public class FourAccountRefundCommissionAuditPageReqVO extends PageParam {

    @Schema(description = "开始业务日", example = "2026-03-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate beginBizDate;

    @Schema(description = "结束业务日", example = "2026-03-06")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endBizDate;

    @Schema(description = "异常类型", example = "REFUND_WITHOUT_REVERSAL")
    private String mismatchType;

    @Schema(description = "交易订单号关键词", example = "20260306")
    private String keyword;

    @Schema(description = "交易订单ID", example = "10001")
    private Long orderId;

}
