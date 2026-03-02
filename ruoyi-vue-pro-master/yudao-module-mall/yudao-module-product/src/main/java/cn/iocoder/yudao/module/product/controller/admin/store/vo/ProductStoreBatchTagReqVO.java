package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 门店批量更新标签 Request VO")
@Data
public class ProductStoreBatchTagReqVO {

    @Schema(description = "门店编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "门店编号列表不能为空")
    private List<Long> storeIds;

    @Schema(description = "标签编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "标签编号列表不能为空")
    private List<Long> tagIds;

    @Schema(description = "操作原因", example = "区域运营策略调整")
    private String reason;
}
