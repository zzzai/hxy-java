package cn.iocoder.yudao.module.product.controller.admin.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - SKU 生成预览 Request VO")
@Data
public class ProductSkuGeneratePreviewReqVO {

    @NotNull(message = "SPU 不能为空")
    private Long spuId;

    @NotNull(message = "类目不能为空")
    private Long categoryId;

    private Long templateVersionId;

    @Valid
    @NotNull(message = "基础 SKU 不能为空")
    private BaseSku baseSku;

    @Valid
    @NotEmpty(message = "规格选择不能为空")
    private List<SpecSelection> specSelections;

    @Data
    public static class BaseSku {
        @NotNull(message = "销售价不能为空")
        private Integer price;
        @NotNull(message = "划线价不能为空")
        private Integer marketPrice;
        @NotNull(message = "成本价不能为空")
        private Integer costPrice;
        @NotNull(message = "库存不能为空")
        private Integer stock;
    }

    @Data
    public static class SpecSelection {
        @NotNull(message = "属性ID不能为空")
        private Long attributeId;
        @NotEmpty(message = "规格选项不能为空")
        private List<Long> optionIds;
    }
}
