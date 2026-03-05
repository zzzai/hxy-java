package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 门店生命周期变更单创建 Request VO")
@Data
public class ProductStoreLifecycleChangeOrderCreateReqVO {

    @Schema(description = "门店 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "门店 ID 不能为空")
    private Long storeId;

    @Schema(description = "目标生命周期状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "35")
    @NotNull(message = "目标生命周期状态不能为空")
    private Integer toLifecycleStatus;

    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "门店临时停业整修")
    @NotBlank(message = "变更原因不能为空")
    private String reason;

    @Schema(description = "申请来源", example = "ADMIN_UI")
    private String applySource;
}
