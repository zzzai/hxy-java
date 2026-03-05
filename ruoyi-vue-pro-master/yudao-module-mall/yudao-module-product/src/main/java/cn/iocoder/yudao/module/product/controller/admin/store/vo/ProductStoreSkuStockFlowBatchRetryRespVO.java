package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 门店 SKU 库存流水批量重试 Response VO")
@Data
public class ProductStoreSkuStockFlowBatchRetryRespVO {

    @Schema(description = "总处理数量（去重后）", example = "3")
    private Integer totalCount;

    @Schema(description = "成功数量", example = "1")
    private Integer successCount;

    @Schema(description = "跳过数量", example = "1")
    private Integer skippedCount;

    @Schema(description = "失败数量", example = "1")
    private Integer failedCount;

    @Schema(description = "明细")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "流水编号", example = "1001")
        private Long id;

        @Schema(description = "门店编号", example = "1001")
        private Long storeId;

        @Schema(description = "SKU 编号", example = "3001")
        private Long skuId;

        @Schema(description = "结果类型：SUCCESS/SKIPPED/FAILED", example = "SUCCESS")
        private String resultType;

        @Schema(description = "结果原因", example = "STATUS_NOT_RETRYABLE")
        private String reason;

        @Schema(description = "处理后状态：0待执行 1成功 2失败 3执行中", example = "1")
        private Integer status;

        @Schema(description = "操作人", example = "admin")
        private String retryOperator;

        @Schema(description = "操作来源", example = "ADMIN_UI")
        private String retrySource;
    }
}
