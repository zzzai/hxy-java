package cn.iocoder.yudao.module.trade.service.aftersale.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 售后人工复核工单批量收口结果
 */
@Data
@Builder
public class AfterSaleReviewTicketBatchResolveResult {

    /**
     * 去重后的总处理数量
     */
    private Integer totalCount;
    /**
     * 收口成功数量
     */
    private Integer successCount;
    /**
     * 跳过（工单不存在）数量
     */
    private Integer skippedNotFoundCount;
    /**
     * 跳过（状态非待处理）数量
     */
    private Integer skippedNotPendingCount;
    /**
     * 收口成功工单 ID 列表
     */
    private List<Long> successIds;
    /**
     * 跳过（不存在）工单 ID 列表
     */
    private List<Long> skippedNotFoundIds;
    /**
     * 跳过（状态非待处理）工单 ID 列表
     */
    private List<Long> skippedNotPendingIds;

    public static AfterSaleReviewTicketBatchResolveResult empty() {
        return AfterSaleReviewTicketBatchResolveResult.builder()
                .totalCount(0)
                .successCount(0)
                .skippedNotFoundCount(0)
                .skippedNotPendingCount(0)
                .successIds(Collections.emptyList())
                .skippedNotFoundIds(Collections.emptyList())
                .skippedNotPendingIds(Collections.emptyList())
                .build();
    }

}
