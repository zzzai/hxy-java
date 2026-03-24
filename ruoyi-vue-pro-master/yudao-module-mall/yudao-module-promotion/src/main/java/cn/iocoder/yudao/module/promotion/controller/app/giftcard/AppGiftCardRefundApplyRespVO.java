package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 礼品卡退款申请 Response VO")
@Data
@Accessors(chain = true)
public class AppGiftCardRefundApplyRespVO {

    @Schema(description = "售后单编号", example = "6001")
    private Long afterSaleId;

    @Schema(description = "退款状态", example = "REFUND_PENDING")
    private String refundStatus;

    @Schema(description = "是否降级", example = "false")
    private Boolean degraded;

    @Schema(description = "降级原因", example = "")
    private String degradeReason;
}
