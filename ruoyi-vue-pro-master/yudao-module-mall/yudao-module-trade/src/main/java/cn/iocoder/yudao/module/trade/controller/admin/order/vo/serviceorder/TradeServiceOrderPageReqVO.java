package cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 服务履约单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TradeServiceOrderPageReqVO extends PageParam {

    @Schema(description = "交易订单编号", example = "1001")
    private Long orderId;

    @Schema(description = "交易订单号", example = "T202602240001")
    private String orderNo;

    @Schema(description = "交易订单项编号", example = "2001")
    private Long orderItemId;

    @Schema(description = "用户编号", example = "3001")
    private Long userId;

    @Schema(description = "支付单编号", example = "4001")
    private Long payOrderId;

    @Schema(description = "履约状态", example = "10")
    private Integer status;

    @Schema(description = "预约单号", example = "BOOK202602240001")
    private String bookingNo;

    @Schema(description = "来源", example = "PAY_CALLBACK")
    private String source;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
