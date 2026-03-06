package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - booking退款回调台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingRefundNotifyLogPageReqVO extends PageParam {

    @Schema(description = "预约订单ID", example = "1001")
    private Long orderId;

    @Schema(description = "商户退款单号", example = "1001-refund")
    private String merchantRefundId;

    @Schema(description = "支付退款单ID", example = "88801")
    private Long payRefundId;

    @Schema(description = "状态(success/fail)", example = "fail")
    private String status;

    @Schema(description = "错误码", example = "1030004012")
    private String errorCode;

    @Schema(description = "创建时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
}
