package cn.iocoder.yudao.module.product.api.store.dto;

import lombok.Data;

/**
 * 门店 SKU 映射信息 DTO
 */
@Data
public class ProductStoreSkuRespDTO {

    private Long id;

    private Long storeId;

    private Long spuId;

    private Long skuId;

    private Integer saleStatus;

    private Integer salePrice;

    private Integer marketPrice;

    private Integer stock;

    private Integer sort;

    private String remark;
}

