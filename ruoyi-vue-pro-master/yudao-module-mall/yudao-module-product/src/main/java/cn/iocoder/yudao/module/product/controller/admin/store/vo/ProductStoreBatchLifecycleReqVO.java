package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 门店批量更新生命周期 Request VO")
@Data
public class ProductStoreBatchLifecycleReqVO {

    @Schema(description = "门店编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "门店编号列表不能为空")
    private List<Long> storeIds;

    @Schema(description = "生命周期状态：10筹备中 20试营业 30营业中 35停业 40闭店",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "生命周期状态不能为空")
    private Integer lifecycleStatus;

    @Schema(description = "操作原因", example = "新店达标上线")
    private String reason;
}
