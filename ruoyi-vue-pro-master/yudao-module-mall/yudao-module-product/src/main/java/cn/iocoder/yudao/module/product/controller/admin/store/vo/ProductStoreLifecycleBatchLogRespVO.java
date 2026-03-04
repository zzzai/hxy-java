package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 门店生命周期批量执行台账 Response VO")
@Data
public class ProductStoreLifecycleBatchLogRespVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "批次号", example = "LIFECYCLE-20260304202100-ABCD1234")
    private String batchNo;

    @Schema(description = "目标生命周期状态", example = "35")
    private Integer targetLifecycleStatus;

    @Schema(description = "总门店数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功数", example = "8")
    private Integer successCount;

    @Schema(description = "阻塞数", example = "1")
    private Integer blockedCount;

    @Schema(description = "告警数", example = "1")
    private Integer warningCount;

    @Schema(description = "审计摘要")
    private String auditSummary;

    @Schema(description = "逐店明细快照 JSON")
    private String detailJson;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
