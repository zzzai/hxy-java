package cn.iocoder.yudao.module.product.service.store.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 门店 SKU 库存流水批量重试结果
 */
@Data
@Builder
public class ProductStoreSkuStockFlowBatchRetryResult {

    /**
     * 去重后的总处理数量
     */
    private Integer totalCount;
    /**
     * 成功数量
     */
    private Integer successCount;
    /**
     * 跳过数量
     */
    private Integer skippedCount;
    /**
     * 失败数量
     */
    private Integer failedCount;
    /**
     * 明细
     */
    private List<Item> items;

    @Data
    @Builder
    public static class Item {
        /**
         * 流水 ID
         */
        private Long id;
        /**
         * 结果类型：SUCCESS/SKIPPED/FAILED
         */
        private String resultType;
        /**
         * 结果原因：NOT_FOUND/STATUS_NOT_RETRYABLE/CLAIM_CONFLICT/APPLY_FAILED/STATUS_UPDATE_CONFLICT
         */
        private String reason;
        /**
         * 处理后状态
         */
        private Integer status;
        /**
         * 审计：操作人
         */
        private String retryOperator;
        /**
         * 审计：操作来源
         */
        private String retrySource;
    }

    public static ProductStoreSkuStockFlowBatchRetryResult empty() {
        return ProductStoreSkuStockFlowBatchRetryResult.builder()
                .totalCount(0)
                .successCount(0)
                .skippedCount(0)
                .failedCount(0)
                .items(Collections.emptyList())
                .build();
    }
}
