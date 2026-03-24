package cn.iocoder.yudao.module.trade.controller.app.referral;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 邀请总览 Response VO")
@Data
@Accessors(chain = true)
public class AppReferralOverviewRespVO {

    @Schema(description = "邀请码", requiredMode = Schema.RequiredMode.REQUIRED, example = "66")
    private String referralCode;

    @Schema(description = "总邀请数", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    private Long totalInvites;

    @Schema(description = "有效邀请数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Long effectiveInvites;

    @Schema(description = "待到账金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    private Integer pendingRewardAmount;

    @Schema(description = "奖励余额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "500")
    private Integer rewardBalance;

    @Schema(description = "是否降级", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean degraded;
}
