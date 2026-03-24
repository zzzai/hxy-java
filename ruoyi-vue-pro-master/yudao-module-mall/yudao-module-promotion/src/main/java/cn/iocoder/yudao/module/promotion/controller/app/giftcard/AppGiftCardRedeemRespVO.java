package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 礼品卡核销 Response VO")
@Data
@Accessors(chain = true)
public class AppGiftCardRedeemRespVO {

    @Schema(description = "核销记录编号", example = "7001")
    private Long redeemRecordId;

    @Schema(description = "会员编号", example = "66")
    private Long memberId;

    @Schema(description = "卡状态", example = "REDEEMED")
    private String cardStatus;

    @Schema(description = "是否降级", example = "false")
    private Boolean degraded;
}
