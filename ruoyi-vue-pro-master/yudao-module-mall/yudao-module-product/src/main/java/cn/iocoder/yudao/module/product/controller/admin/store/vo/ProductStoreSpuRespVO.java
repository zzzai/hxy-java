package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 门店 SPU 映射 Response VO")
@Data
public class ProductStoreSpuRespVO {

    private Long id;
    private Long storeId;
    private String storeName;
    private Long spuId;
    private String spuName;
    private Integer productType;
    private Integer saleStatus;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
}
