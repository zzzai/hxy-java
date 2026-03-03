package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 门店 SKU 库存流水批量重试 Request VO")
@Data
public class ProductStoreSkuStockFlowBatchRetryReqVO {

    @Schema(description = "库存流水编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "库存流水编号列表不能为空")
    private List<Long> ids;
}
