package cn.iocoder.yudao.module.trade.service.aftersale.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 售后人工复核工单通知出站批量重试结果
 */
@Data
@Builder
public class AfterSaleReviewTicketNotifyBatchRetryResult {

    /**
     * 去重后的总处理数量
     */
    private Integer totalCount;
    /**
     * 重试成功数量
     */
    private Integer successCount;
    /**
     * 跳过（记录不存在）数量
     */
    private Integer skippedNotFoundCount;
    /**
     * 跳过（状态不支持重试）数量
     */
    private Integer skippedStatusInvalidCount;
    /**
     * 成功记录 ID 列表
     */
    private List<Long> successIds;
    /**
     * 跳过（不存在）记录 ID 列表
     */
    private List<Long> skippedNotFoundIds;
    /**
     * 跳过（状态不支持重试）记录 ID 列表
     */
    private List<Long> skippedStatusInvalidIds;

    public static AfterSaleReviewTicketNotifyBatchRetryResult empty() {
        return AfterSaleReviewTicketNotifyBatchRetryResult.builder()
                .totalCount(0)
                .successCount(0)
                .skippedNotFoundCount(0)
                .skippedStatusInvalidCount(0)
                .successIds(Collections.emptyList())
                .skippedNotFoundIds(Collections.emptyList())
                .skippedStatusInvalidIds(Collections.emptyList())
                .build();
    }
}
