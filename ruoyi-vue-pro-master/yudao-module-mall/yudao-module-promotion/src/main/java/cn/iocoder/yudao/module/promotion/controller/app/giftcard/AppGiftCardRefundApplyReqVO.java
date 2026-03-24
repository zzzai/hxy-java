package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "用户 App - 礼品卡退款申请 Request VO")
@Data
public class AppGiftCardRefundApplyReqVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9001")
    @NotNull(message = "订单编号不能为空")
    private Long orderId;

    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "未使用")
    @NotBlank(message = "退款原因不能为空")
    private String reason;

    @Schema(description = "支付退款单号", example = "PAY-REFUND-001")
    private String payRefundId;

    @Schema(description = "客户端幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "gift-refund-001")
    @NotBlank(message = "客户端幂等键不能为空")
    private String clientToken;
}
