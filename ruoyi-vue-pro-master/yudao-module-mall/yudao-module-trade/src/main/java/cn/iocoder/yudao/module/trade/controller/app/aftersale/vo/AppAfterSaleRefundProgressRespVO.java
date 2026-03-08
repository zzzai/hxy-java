package cn.iocoder.yudao.module.trade.controller.app.aftersale.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 售后退款进度聚合 Response VO")
@Data
public class AppAfterSaleRefundProgressRespVO {

    @Schema(description = "售后编号", example = "1001")
    private Long afterSaleId;

    @Schema(description = "售后单号", example = "AS202603080001")
    private String afterSaleNo;

    @Schema(description = "交易订单编号", example = "2001")
    private Long orderId;

    @Schema(description = "交易订单号", example = "T202603080001")
    private String orderNo;

    @Schema(description = "售后状态", example = "40")
    private Integer afterSaleStatus;

    @Schema(description = "售后状态名", example = "等待平台退款")
    private String afterSaleStatusName;

    @Schema(description = "退款金额（分）", example = "1200")
    private Integer refundPrice;

    @Schema(description = "支付退款单编号", example = "3001")
    private Long payRefundId;

    @Schema(description = "支付退款状态", example = "10")
    private Integer payRefundStatus;

    @Schema(description = "支付退款状态名", example = "退款成功")
    private String payRefundStatusName;

    @Schema(description = "商户订单号", example = "2001")
    private String merchantOrderId;

    @Schema(description = "商户退款号", example = "2001-refund")
    private String merchantRefundId;

    @Schema(description = "退款完成时间")
    private LocalDateTime refundTime;

    @Schema(description = "聚合进度编码", example = "REFUND_PROCESSING")
    private String progressCode;

    @Schema(description = "聚合进度描述", example = "退款处理中")
    private String progressDesc;

    @Schema(description = "支付渠道错误码", example = "SYSTEM_ERROR")
    private String channelErrorCode;

    @Schema(description = "支付渠道错误信息", example = "系统繁忙")
    private String channelErrorMsg;

}
