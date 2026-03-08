package cn.iocoder.yudao.module.trade.controller.app.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 交易订单支付结果聚合 Response VO")
@Data
public class AppTradeOrderPayResultRespVO {

    @Schema(description = "交易订单编号", example = "1024")
    private Long orderId;

    @Schema(description = "交易订单号", example = "T202603080001")
    private String orderNo;

    @Schema(description = "支付订单编号", example = "2048")
    private Long payOrderId;

    @Schema(description = "订单状态", example = "0")
    private Integer orderStatus;

    @Schema(description = "订单是否已支付", example = "false")
    private Boolean orderPayStatus;

    @Schema(description = "订单退款状态", example = "10")
    private Integer orderRefundStatus;

    @Schema(description = "订单累计退款金额（分）", example = "1000")
    private Integer orderRefundPrice;

    @Schema(description = "支付单状态", example = "10")
    private Integer payOrderStatus;

    @Schema(description = "支付单状态名", example = "支付成功")
    private String payOrderStatusName;

    @Schema(description = "支付成功时间")
    private LocalDateTime paySuccessTime;

    @Schema(description = "支付渠道编码", example = "wx_lite")
    private String payChannelCode;

    @Schema(description = "聚合支付结果编码", example = "SUCCESS")
    private String payResultCode;

    @Schema(description = "聚合支付结果描述", example = "支付成功")
    private String payResultDesc;

    @Schema(description = "是否降级", example = "false")
    private Boolean degraded;

    @Schema(description = "降级原因", example = "PAY_ORDER_NOT_FOUND")
    private String degradeReason;

}
