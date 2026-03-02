package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 门店映射可选 SPU Response VO")
@Data
public class ProductStoreSpuOptionRespVO {

    @Schema(description = "SPU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long id;

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "60分钟足疗")
    private String name;

    @Schema(description = "商品类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer productType;

    @Schema(description = "商品状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;
}

