package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 门店批量更新分类 Request VO")
@Data
public class ProductStoreBatchCategoryReqVO {

    @Schema(description = "门店编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "门店编号列表不能为空")
    private List<Long> storeIds;

    @Schema(description = "分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "分类编号不能为空")
    private Long categoryId;

    @Schema(description = "操作原因", example = "区域重组")
    private String reason;
}
