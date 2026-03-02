package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 门店生命周期更新 Request VO")
@Data
public class ProductStoreLifecycleUpdateReqVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "门店编号不能为空")
    private Long id;

    @Schema(description = "生命周期状态：10筹备中 20试营业 30营业中 35停业 40闭店",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "生命周期状态不能为空")
    private Integer lifecycleStatus;

    @Schema(description = "变更原因", example = "完成验收")
    private String reason;
}
