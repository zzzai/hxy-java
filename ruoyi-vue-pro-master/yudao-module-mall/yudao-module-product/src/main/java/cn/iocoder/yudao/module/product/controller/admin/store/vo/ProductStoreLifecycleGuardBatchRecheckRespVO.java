package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 门店生命周期守卫批次复核 Response VO")
@Data
public class ProductStoreLifecycleGuardBatchRecheckRespVO {

    @Schema(description = "批次台账 ID", example = "1001")
    private Long logId;

    @Schema(description = "批次号", example = "LIFECYCLE-20260304202100-ABCD1234")
    private String batchNo;

    @Schema(description = "目标生命周期状态", example = "35")
    private Integer targetLifecycleStatus;

    @Schema(description = "总门店数", example = "10")
    private Integer totalCount;

    @Schema(description = "阻塞数", example = "1")
    private Integer blockedCount;

    @Schema(description = "告警数", example = "2")
    private Integer warningCount;

    @Schema(description = "明细快照解析是否失败", example = "false")
    private Boolean detailParseError;

    @Schema(description = "守卫规则版本", example = "GRV-9A17C2F1E8A3")
    private String guardRuleVersion;

    @Schema(description = "守卫配置快照 JSON")
    private String guardConfigSnapshotJson;

    @Schema(description = "逐店复核结果")
    private List<Detail> details = new ArrayList<>();

    @Schema(description = "管理后台 - 门店生命周期守卫批次复核明细")
    @Data
    public static class Detail {

        @Schema(description = "门店编号", example = "1001")
        private Long storeId;

        @Schema(description = "门店名称", example = "荷小悦-上海徐汇店")
        private String storeName;

        @Schema(description = "是否阻塞", example = "true")
        private Boolean blocked;

        @Schema(description = "阻塞错误码", example = "1008014005")
        private Integer blockedCode;

        @Schema(description = "阻塞原因")
        private String blockedMessage;

        @Schema(description = "告警列表")
        private List<String> warnings;

        @Schema(description = "守卫项详情")
        private List<ProductStoreLifecycleGuardRespVO.GuardItem> guardItems;
    }
}
