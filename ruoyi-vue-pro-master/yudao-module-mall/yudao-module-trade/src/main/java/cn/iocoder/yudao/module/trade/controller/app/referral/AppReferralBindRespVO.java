package cn.iocoder.yudao.module.trade.controller.app.referral;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 邀请绑定 Response VO")
@Data
@Accessors(chain = true)
public class AppReferralBindRespVO {

    @Schema(description = "被邀请人会员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "66")
    private Long refereeMemberId;

    @Schema(description = "邀请人会员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "88")
    private Long inviterMemberId;

    @Schema(description = "绑定状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOUND")
    private String bindStatus;

    @Schema(description = "是否命中幂等", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean idempotentHit;
}
