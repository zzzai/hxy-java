package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 门店 SKU 映射新增/更新 Request VO")
@Data
public class ProductStoreSkuSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "门店编号不能为空")
    private Long storeId;

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

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "备注", example = "门店专享库存")
    private String remark;
}

