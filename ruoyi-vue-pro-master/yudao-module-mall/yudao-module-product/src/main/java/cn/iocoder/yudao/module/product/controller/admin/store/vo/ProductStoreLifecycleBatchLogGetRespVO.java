package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 门店生命周期批量执行台账详情 Response VO")
@Data
public class ProductStoreLifecycleBatchLogGetRespVO {

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

    @Schema(description = "守卫规则版本", example = "GRV-9A17C2F1E8A3")
    private String guardRuleVersion;

    @Schema(description = "守卫配置快照 JSON")
    private String guardConfigSnapshotJson;

    @Schema(description = "逐店明细快照 JSON 原文")
    private String detailJson;

    @Schema(description = "结构化明细视图（从 detailJson 解析）")
    private Map<String, Object> detailView;

    @Schema(description = "明细解析是否失败", example = "false")
    private Boolean detailParseError;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
