package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "用户 App - 礼品卡核销 Request VO")
@Data
public class AppGiftCardRedeemReqVO {

    @Schema(description = "礼品卡卡号", requiredMode = Schema.RequiredMode.REQUIRED, example = "GC1001")
    @NotBlank(message = "卡号不能为空")
    private String cardNo;

    @Schema(description = "核销码", requiredMode = Schema.RequiredMode.REQUIRED, example = "8888")
    @NotBlank(message = "核销码不能为空")
    private String redeemCode;

    @Schema(description = "客户端幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "gift-redeem-001")
    @NotBlank(message = "客户端幂等键不能为空")
    private String clientToken;
}
