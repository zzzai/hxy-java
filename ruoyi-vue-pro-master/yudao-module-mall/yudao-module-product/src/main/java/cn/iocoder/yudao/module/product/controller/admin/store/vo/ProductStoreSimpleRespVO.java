package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 门店精简 Response VO")
@Data
public class ProductStoreSimpleRespVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;

    @Schema(description = "门店编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SH-001")
    private String code;

    @Schema(description = "门店名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "荷小悦-上海徐汇店")
    private String name;

    @Schema(description = "门店简称", example = "徐汇店")
    private String shortName;
}
