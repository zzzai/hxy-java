package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 门店批量生命周期执行结果 Response VO")
@Data
public class ProductStoreBatchLifecycleResultRespVO {

    @Schema(description = "总门店数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer totalCount;

    @Schema(description = "执行成功数", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    private Integer successCount;

    @Schema(description = "阻塞门店数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer blockedCount;

    @Schema(description = "告警门店数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer warningCount;

    @Schema(description = "汇总审计摘要")
    private String auditSummary;

    @Schema(description = "逐门店执行明细")
    private List<StoreResult> storeResults;

    @Schema(description = "逐门店执行结果")
    @Data
    public static class StoreResult {

        @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
        private Long storeId;

        @Schema(description = "是否阻塞", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
        private Boolean blocked;

        @Schema(description = "是否执行成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        private Boolean executed;

        @Schema(description = "阻塞错误码", example = "1008014005")
        private Integer blockedCode;

        @Schema(description = "阻塞原因", example = "门店存在库存流水待处理，无法停业或闭店")
        private String blockedMessage;

        @Schema(description = "告警列表（WARN 模式）")
        private List<String> warnings;
    }
}
