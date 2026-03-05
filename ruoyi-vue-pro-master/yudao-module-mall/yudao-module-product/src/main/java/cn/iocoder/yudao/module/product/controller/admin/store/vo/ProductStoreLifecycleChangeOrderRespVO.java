package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 门店生命周期变更单 Response VO")
@Data
public class ProductStoreLifecycleChangeOrderRespVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "变更单号", example = "LCO-20260305183000-ABCD1234")
    private String orderNo;

    @Schema(description = "门店 ID", example = "1001")
    private Long storeId;

    @Schema(description = "门店名称", example = "荷小悦-上海徐汇店")
    private String storeName;

    @Schema(description = "变更前生命周期状态", example = "30")
    private Integer fromLifecycleStatus;

    @Schema(description = "目标生命周期状态", example = "35")
    private Integer toLifecycleStatus;

    @Schema(description = "变更原因")
    private String reason;

    @Schema(description = "申请人")
    private String applyOperator;

    @Schema(description = "申请来源")
    private String applySource;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "守卫快照 JSON")
    private String guardSnapshotJson;

    @Schema(description = "守卫是否阻塞", example = "false")
    private Boolean guardBlocked;

    @Schema(description = "守卫告警")
    private String guardWarnings;

    @Schema(description = "审批人")
    private String approveOperator;

    @Schema(description = "审批备注")
    private String approveRemark;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "SLA 截止时间")
    private LocalDateTime slaDeadlineTime;

    @Schema(description = "最后动作编码")
    private String lastActionCode;

    @Schema(description = "最后动作操作人")
    private String lastActionOperator;

    @Schema(description = "最后动作时间")
    private LocalDateTime lastActionTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
