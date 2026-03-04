package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 售后人工复核工单通知出站批量重试 Response VO")
@Data
public class AfterSaleReviewTicketNotifyOutboxBatchRetryRespVO {

    @Schema(description = "入参总数（去重后）", example = "3")
    private Integer totalCount;

    @Schema(description = "成功重试条数", example = "1")
    private Integer successCount;

    @Schema(description = "跳过数量-记录不存在", example = "1")
    private Integer skippedNotFoundCount;

    @Schema(description = "跳过数量-状态非法/并发冲突", example = "1")
    private Integer skippedStatusInvalidCount;

    @Schema(description = "成功重试的出站ID")
    private List<Long> successIds;

    @Schema(description = "跳过（不存在）的出站ID")
    private List<Long> skippedNotFoundIds;

    @Schema(description = "跳过（状态非法）的出站ID")
    private List<Long> skippedStatusInvalidIds;
}
