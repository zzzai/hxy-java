package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 门店 SKU 库存调整单 Response VO")
@Data
public class ProductStoreSkuStockAdjustOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;

    @Schema(description = "调整单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SAO-20260305120000-ABCD1234")
    private String orderNo;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long storeId;

    @Schema(description = "门店名称", example = "上海徐汇店")
    private String storeName;

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "REPLENISH_IN")
    private String bizType;

    @Schema(description = "原因", example = "到货入库")
    private String reason;

    @Schema(description = "备注", example = "总部统一补货")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "调整明细 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    private String detailJson;

    @Schema(description = "申请人", example = "运营同学")
    private String applyOperator;

    @Schema(description = "申请来源", example = "ADMIN_UI")
    private String applySource;

    @Schema(description = "审批人", example = "审批同学")
    private String approveOperator;

    @Schema(description = "审批备注", example = "审批通过")
    private String approveRemark;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "最后动作编码", example = "APPROVE")
    private String lastActionCode;

    @Schema(description = "最后动作操作人", example = "审批同学")
    private String lastActionOperator;

    @Schema(description = "最后动作时间")
    private LocalDateTime lastActionTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
