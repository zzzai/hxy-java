package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 门店映射可选 SKU Response VO")
@Data
public class ProductStoreSkuOptionRespVO {

    @Schema(description = "SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long id;

    @Schema(description = "SPU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long spuId;

    @Schema(description = "规格描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "时长:60分钟; 力度:中")
    private String specText;

    @Schema(description = "销售价（分）", requiredMode = Schema.RequiredMode.REQUIRED, example = "9800")
    private Integer price;

    @Schema(description = "划线价（分）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12800")
    private Integer marketPrice;

    @Schema(description = "库存", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer stock;
}

