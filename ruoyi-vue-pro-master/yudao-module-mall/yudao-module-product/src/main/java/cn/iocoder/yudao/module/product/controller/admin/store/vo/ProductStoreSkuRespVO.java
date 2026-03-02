package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 门店 SKU 映射 Response VO")
@Data
public class ProductStoreSkuRespVO {

    private Long id;
    private Long storeId;
    private String storeName;
    private Long spuId;
    private String spuName;
    private Long skuId;
    private String skuSpecText;
    private Integer saleStatus;
    private Integer salePrice;
    private Integer marketPrice;
    private Integer stock;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
}
