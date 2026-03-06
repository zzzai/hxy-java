package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 四账退款提成巡检工单同步 Response VO")
@Data
public class FourAccountRefundCommissionAuditSyncRespVO {

    @Schema(description = "命中异常总数", example = "26")
    private Integer totalMismatchCount;

    @Schema(description = "本次尝试同步条数", example = "20")
    private Integer attemptedCount;

    @Schema(description = "同步成功条数", example = "18")
    private Integer successCount;

    @Schema(description = "同步失败条数", example = "2")
    private Integer failedCount;

    @Schema(description = "失败订单ID列表")
    private List<Long> failedOrderIds;

}
