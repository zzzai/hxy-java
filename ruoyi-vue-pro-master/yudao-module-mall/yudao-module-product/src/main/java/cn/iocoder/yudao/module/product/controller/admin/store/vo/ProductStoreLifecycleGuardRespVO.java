package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 门店生命周期守卫详情 Response VO")
@Data
public class ProductStoreLifecycleGuardRespVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long storeId;

    @Schema(description = "目标生命周期状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "35")
    private Integer targetLifecycleStatus;

    @Schema(description = "是否阻塞", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean blocked;

    @Schema(description = "阻塞错误码", example = "1008014005")
    private Integer blockedCode;

    @Schema(description = "阻塞原因", example = "门店存在库存流水待处理，无法停业或闭店")
    private String blockedMessage;

    @Schema(description = "告警列表（WARN 模式）")
    private List<String> warnings;

    @Schema(description = "守卫项详情")
    private List<GuardItem> guardItems;

    @Schema(description = "守卫项")
    @Data
    public static class GuardItem {

        @Schema(description = "守卫键", requiredMode = Schema.RequiredMode.REQUIRED, example = "stock-flow")
        private String guardKey;

        @Schema(description = "命中数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
        private Long count;

        @Schema(description = "守卫模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "BLOCK")
        private String mode;

        @Schema(description = "是否阻塞", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        private Boolean blocked;
    }
}
