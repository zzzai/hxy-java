package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 门店 SKU 库存调整单动作 Request VO")
@Data
public class ProductStoreSkuStockAdjustOrderActionReqVO {

    @Schema(description = "调整单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "调整单编号不能为空")
    private Long id;

    @Schema(description = "备注", example = "审批通过")
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
