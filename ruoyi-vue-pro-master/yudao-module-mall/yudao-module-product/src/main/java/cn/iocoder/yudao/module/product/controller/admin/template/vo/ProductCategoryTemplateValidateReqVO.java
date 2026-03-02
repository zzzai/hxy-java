package cn.iocoder.yudao.module.product.controller.admin.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 类目模板校验 Request VO")
@Data
public class ProductCategoryTemplateValidateReqVO {

    @Schema(description = "类目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "类目不能为空")
    private Long categoryId;

    @Schema(description = "模板版本ID", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "12")
    private Long templateVersionId;

    @Schema(description = "模板项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "模板项不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "属性ID不能为空")
        private Long attributeId;

        @NotNull(message = "属性角色不能为空")
        private Integer attrRole;

        @NotNull(message = "是否必填不能为空")
        private Boolean required;

        @NotNull(message = "是否影响价格不能为空")
        private Boolean affectsPrice;

        @NotNull(message = "是否影响库存不能为空")
        private Boolean affectsStock;
    }
}
