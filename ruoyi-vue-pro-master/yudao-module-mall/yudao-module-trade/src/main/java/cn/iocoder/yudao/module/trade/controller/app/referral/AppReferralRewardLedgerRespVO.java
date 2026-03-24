package cn.iocoder.yudao.module.trade.controller.app.referral;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 邀请奖励台账 Response VO")
@Data
@Accessors(chain = true)
public class AppReferralRewardLedgerRespVO {

    @Schema(description = "台账编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9001")
    private Long ledgerId;

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-1")
    private String orderId;

    @Schema(description = "来源业务号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-1")
    private String sourceBizNo;

    @Schema(description = "奖励金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "180")
    private Integer rewardAmount;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "运行批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private String runId;

    @Schema(description = "退款流水号", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private String payRefundId;
}
