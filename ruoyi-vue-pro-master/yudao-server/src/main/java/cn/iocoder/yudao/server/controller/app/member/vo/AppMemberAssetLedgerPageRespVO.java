package cn.iocoder.yudao.server.controller.app.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "用户 App - 会员统一资产台账分页 Response VO")
@Data
public class AppMemberAssetLedgerPageRespVO {

    @Schema(description = "台账列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AppMemberAssetLedgerRespVO> list = new ArrayList<>();

    @Schema(description = "总量", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Long total = 0L;

    @Schema(description = "是否降级", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean degraded = Boolean.FALSE;

    @Schema(description = "降级原因", example = "miniapp.asset.ledger")
    private String degradeReason;
}
