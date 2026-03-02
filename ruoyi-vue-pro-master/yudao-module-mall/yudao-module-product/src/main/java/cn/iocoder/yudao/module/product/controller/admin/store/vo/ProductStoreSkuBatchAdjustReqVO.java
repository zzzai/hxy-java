package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 门店 SKU 批量调价/调库存 Request VO")
@Data
public class ProductStoreSkuBatchAdjustReqVO {

    @Schema(description = "门店编号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1001,1002]")
    @NotEmpty(message = "门店编号列表不能为空")
    private List<Long> storeIds;

    @Schema(description = "总部 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    @NotNull(message = "SKU 编号不能为空")
    private Long skuId;

    @Schema(description = "销售状态：0 上架 1 下架", example = "0")
    private Integer saleStatus;

    @Schema(description = "门店销售价（分）", example = "9800")
    private Integer salePrice;

    @Schema(description = "门店划线价（分）", example = "10800")
    private Integer marketPrice;

    @Schema(description = "门店库存", example = "9")
    private Integer stock;

    @Schema(description = "备注（可选，传值时覆盖）", example = "总部统一调价")
    private String remark;
}

