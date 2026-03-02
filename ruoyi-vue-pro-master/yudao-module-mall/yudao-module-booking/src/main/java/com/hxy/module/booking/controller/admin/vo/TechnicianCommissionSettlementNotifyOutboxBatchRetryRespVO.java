package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 技师佣金结算通知出站批量重试 Response VO")
@Data
public class TechnicianCommissionSettlementNotifyOutboxBatchRetryRespVO {

    @Schema(description = "入参总数（去重后）", example = "3")
    private Integer totalCount;

    @Schema(description = "成功重试条数", example = "1")
    private Integer retriedCount;

    @Schema(description = "跳过数量-记录不存在", example = "1")
    private Integer skippedNotExistsCount;

    @Schema(description = "跳过数量-状态非法/并发冲突", example = "1")
    private Integer skippedStatusInvalidCount;

    @Schema(description = "成功重试的出站ID")
    private List<Long> retriedIds;

    @Schema(description = "跳过（不存在）的出站ID")
    private List<Long> skippedNotExistsIds;

    @Schema(description = "跳过（状态非法）的出站ID")
    private List<Long> skippedStatusInvalidIds;
}
