package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 门店 SKU 跨店调拨单 Response VO")
@Data
public class ProductStoreSkuTransferOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;

    @Schema(description = "调拨单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "STO-20260306120000-ABCD1234")
    private String orderNo;

    @Schema(description = "源门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long fromStoreId;

    @Schema(description = "源门店名称", example = "上海徐汇店")
    private String fromStoreName;

    @Schema(description = "目标门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    private Long toStoreId;

    @Schema(description = "目标门店名称", example = "上海闵行店")
    private String toStoreName;

    @Schema(description = "原因", example = "跨店补货")
    private String reason;

    @Schema(description = "备注", example = "区域库存平衡")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "调拨明细 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
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
