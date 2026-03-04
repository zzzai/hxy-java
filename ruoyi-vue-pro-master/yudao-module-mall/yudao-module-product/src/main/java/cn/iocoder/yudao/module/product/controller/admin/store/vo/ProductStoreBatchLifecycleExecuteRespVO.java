package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 门店生命周期批量执行结果 Response VO")
@Data
public class ProductStoreBatchLifecycleExecuteRespVO {

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

    @Schema(description = "逐店明细")
    private List<Detail> details = new ArrayList<>();

    @Schema(description = "管理后台 - 门店生命周期批量执行明细")
    @Data
    public static class Detail {

        @Schema(description = "门店编号", example = "1001")
        private Long storeId;

        @Schema(description = "门店名称", example = "荷小悦-上海徐汇店")
        private String storeName;

        @Schema(description = "执行结果：SUCCESS/BLOCKED/WARNING", example = "SUCCESS")
        private String result;

        @Schema(description = "结果说明", example = "执行成功")
        private String message;
    }
}
