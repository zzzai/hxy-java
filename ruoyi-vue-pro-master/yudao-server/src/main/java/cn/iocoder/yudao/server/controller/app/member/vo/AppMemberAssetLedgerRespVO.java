package cn.iocoder.yudao.server.controller.app.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 会员统一资产台账条目 Response VO")
@Data
public class AppMemberAssetLedgerRespVO {

    @Schema(description = "台账编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long ledgerId;

    @Schema(description = "资产类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "WALLET")
    private String assetType;

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private String bizType;

    @Schema(description = "标题", example = "钱包充值")
    private String title;

    @Schema(description = "描述", example = "交易后余额 188.00 元")
    private String description;

    @Schema(description = "金额", example = "8800")
    private Long amount;

    @Schema(description = "变动后余额", example = "18800")
    private Long balanceAfter;

    @Schema(description = "来源业务号", example = "PAY-11")
    private String sourceBizNo;

    @Schema(description = "运行标识", example = "WALLET-11")
    private String runId;

    @Schema(description = "发生时间")
    private LocalDateTime createTime;
}
