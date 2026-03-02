package com.hxy.module.booking.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 技师佣金通知出站批量重试结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianCommissionNotifyBatchRetryResult {

    /**
     * 入参总数（去重后）
     */
    private Integer totalCount;
    /**
     * 成功重试条数
     */
    private Integer retriedCount;
    /**
     * 跳过：记录不存在
     */
    private Integer skippedNotExistsCount;
    /**
     * 跳过：状态不允许重试/并发状态变化
     */
    private Integer skippedStatusInvalidCount;
    /**
     * 成功重试的出站 ID
     */
    private List<Long> retriedIds;
    /**
     * 跳过（不存在）的出站 ID
     */
    private List<Long> skippedNotExistsIds;
    /**
     * 跳过（状态非法）的出站 ID
     */
    private List<Long> skippedStatusInvalidIds;

    public static TechnicianCommissionNotifyBatchRetryResult empty() {
        return TechnicianCommissionNotifyBatchRetryResult.builder()
                .totalCount(0)
                .retriedCount(0)
                .skippedNotExistsCount(0)
                .skippedStatusInvalidCount(0)
                .retriedIds(Collections.emptyList())
                .skippedNotExistsIds(Collections.emptyList())
                .skippedStatusInvalidIds(Collections.emptyList())
                .build();
    }
}
