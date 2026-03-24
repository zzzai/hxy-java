package cn.iocoder.yudao.module.trade.controller.app.referral;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 邀请绑定 Request VO")
@Data
public class AppReferralBindReqVO {

    @Schema(description = "邀请人会员编号", example = "88")
    private Long inviterMemberId;

    @Schema(description = "邀请码", example = "88")
    private String inviteCode;

    @Schema(description = "客户端幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "ref-bind-001")
    private String clientToken;
}
