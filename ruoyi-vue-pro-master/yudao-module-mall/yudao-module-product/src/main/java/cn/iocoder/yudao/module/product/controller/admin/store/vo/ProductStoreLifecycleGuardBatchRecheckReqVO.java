package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 门店生命周期守卫批次复核 Request VO")
@Data
public class ProductStoreLifecycleGuardBatchRecheckReqVO {

    @Schema(description = "批次台账 ID（与 batchNo 二选一）", example = "1001")
    private Long logId;

    @Schema(description = "批次号（与 logId 二选一）", example = "LIFECYCLE-20260304202100-ABCD1234")
    private String batchNo;
}
