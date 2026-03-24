package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 礼品卡下单 Response VO")
@Data
@Accessors(chain = true)
public class AppGiftCardOrderCreateRespVO {

    @Schema(description = "订单编号", example = "9001")
    private Long orderId;

    @Schema(description = "礼品卡批次号", example = "GIFT-20260324-001")
    private String giftCardBatchNo;

    @Schema(description = "总金额（分）", example = "19800")
    private Integer amountTotal;

    @Schema(description = "是否降级", example = "false")
    private Boolean degraded;

    @Schema(description = "降级原因", example = "")
    private String degradeReason;
}
