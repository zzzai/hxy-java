package cn.iocoder.yudao.module.product.controller.admin.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - SKU 生成预览 Response VO")
@Data
public class ProductSkuGeneratePreviewRespVO {

    private String taskNo;

    private Integer combinationCount;

    private Boolean truncated;

    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private String specHash;
        private String specSummary;
        private Long existsSkuId;
        private SuggestedSku suggestedSku;
    }

    @Data
    public static class SuggestedSku {
        private Integer price;
        private Integer marketPrice;
        private Integer stock;
    }
}
