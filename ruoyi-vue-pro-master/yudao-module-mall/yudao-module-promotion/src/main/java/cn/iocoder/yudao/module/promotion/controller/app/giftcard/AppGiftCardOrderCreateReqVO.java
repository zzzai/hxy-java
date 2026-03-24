package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "用户 App - 礼品卡下单 Request VO")
@Data
public class AppGiftCardOrderCreateReqVO {

    @Schema(description = "模板编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "模板编号不能为空")
    private Long templateId;

    @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer quantity;

    @Schema(description = "送礼场景 SELF/GIFT", requiredMode = Schema.RequiredMode.REQUIRED, example = "SELF")
    @NotBlank(message = "送礼场景不能为空")
    private String sendScene;

    @Schema(description = "受赠人会员编号", example = "66")
    private Long receiverMemberId;

    @Schema(description = "客户端幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "gift-create-001")
    @NotBlank(message = "客户端幂等键不能为空")
    private String clientToken;
}
