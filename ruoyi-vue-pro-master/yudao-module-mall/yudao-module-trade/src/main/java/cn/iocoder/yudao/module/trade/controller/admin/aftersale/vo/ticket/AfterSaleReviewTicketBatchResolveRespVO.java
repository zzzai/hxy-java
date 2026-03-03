package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 售后人工复核工单批量收口 Response VO")
@Data
public class AfterSaleReviewTicketBatchResolveRespVO {

    @Schema(description = "总处理数量（去重后）", example = "3")
    private Integer totalCount;

    @Schema(description = "成功数量", example = "2")
    private Integer successCount;

    @Schema(description = "跳过数量（不存在）", example = "1")
    private Integer skippedNotFoundCount;

    @Schema(description = "跳过数量（状态非待处理）", example = "0")
    private Integer skippedNotPendingCount;

    @Schema(description = "成功工单 ID 列表")
    private List<Long> successIds;

    @Schema(description = "跳过（不存在）工单 ID 列表")
    private List<Long> skippedNotFoundIds;

    @Schema(description = "跳过（状态非待处理）工单 ID 列表")
    private List<Long> skippedNotPendingIds;

}
