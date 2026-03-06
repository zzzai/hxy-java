package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 门店 SKU 跨店调拨单创建 Request VO")
@Data
public class ProductStoreSkuTransferOrderCreateReqVO {

    @Schema(description = "源门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    @NotNull(message = "源门店编号不能为空")
    private Long fromStoreId;

    @Schema(description = "目标门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10002")
    @NotNull(message = "目标门店编号不能为空")
    private Long toStoreId;

    @Schema(description = "原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "跨店补货")
    @NotBlank(message = "原因不能为空")
    @Size(max = 255, message = "原因长度不能超过 255")
    private String reason;

    @Schema(description = "备注", example = "区域库存平衡")
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    @Schema(description = "来源", example = "ADMIN_UI")
    @Size(max = 32, message = "来源长度不能超过 32")
    private String applySource;

    @Schema(description = "调拨明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "调拨明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "20001")
        @NotNull(message = "SKU 编号不能为空")
        private Long skuId;

        @Schema(description = "调拨数量（正整数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull(message = "调拨数量不能为空")
        @Min(value = 1, message = "调拨数量必须大于 0")
        private Integer quantity;
    }
}
