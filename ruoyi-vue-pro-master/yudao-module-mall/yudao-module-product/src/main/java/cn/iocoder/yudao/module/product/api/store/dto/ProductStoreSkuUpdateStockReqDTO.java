package cn.iocoder.yudao.module.product.api.store.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 门店 SKU 库存更新请求 DTO
 */
@Data
public class ProductStoreSkuUpdateStockReqDTO {

    /**
     * 门店编号
     */
    @NotNull(message = "门店编号不能为空")
    private Long storeId;

    /**
     * 业务类型（用于库存流水幂等）
     */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    /**
     * 业务单号（用于库存流水幂等）
     */
    @NotBlank(message = "业务单号不能为空")
    private String bizNo;

    /**
     * 库存变更项
     */
    @NotNull(message = "库存变更项不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        /**
         * SKU 编号
         */
        @NotNull(message = "SKU 编号不能为空")
        private Long skuId;

        /**
         * 库存变化值
         * 正数：增加库存；负数：扣减库存
         */
        @NotNull(message = "库存变化值不能为空")
        private Integer incrCount;
    }
}
