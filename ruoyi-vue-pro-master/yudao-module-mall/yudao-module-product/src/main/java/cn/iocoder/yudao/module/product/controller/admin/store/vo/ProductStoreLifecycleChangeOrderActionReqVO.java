package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 门店生命周期变更单动作 Request VO")
@Data
public class ProductStoreLifecycleChangeOrderActionReqVO {

    @Schema(description = "变更单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "变更单 ID 不能为空")
    private Long id;

    @Schema(description = "备注", example = "同意执行")
    private String remark;
}
