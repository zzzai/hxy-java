package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 门店 SKU 人工库存调整 Request VO")
@Data
public class ProductStoreSkuManualStockAdjustReqVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    @NotNull(message = "门店编号不能为空")
    private Long storeId;

    @Schema(description = "人工调整业务类型：REPLENISH_IN/TRANSFER_IN/TRANSFER_OUT/STOCKTAKE/LOSS/SCRAP",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "REPLENISH_IN")
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 32, message = "业务类型长度不能超过 32")
    private String bizType;

    @Schema(description = "业务单号（用于幂等）", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUPPLY-20260303-001")
    @NotBlank(message = "业务单号不能为空")
    @Size(max = 64, message = "业务单号长度不能超过 64")
    private String bizNo;

    @Schema(description = "备注", example = "总部盘点修正")
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    @Schema(description = "库存调整明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "库存调整明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "20001")
        @NotNull(message = "SKU 编号不能为空")
        private Long skuId;

        @Schema(description = "库存变化值：正数增加，负数减少，禁止 0", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull(message = "库存变化值不能为空")
        private Integer incrCount;
    }
}

