package cn.iocoder.yudao.module.product.enums.store;

import java.util.Arrays;

/**
 * 门店 SKU 库存调整单状态
 */
public enum ProductStoreSkuStockAdjustOrderStatusEnum {

    DRAFT(0),
    PENDING(10),
    APPROVED(20),
    REJECTED(30),
    CANCELLED(40);

    private final Integer status;

    ProductStoreSkuStockAdjustOrderStatusEnum(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

    public static boolean isValid(Integer status) {
        return Arrays.stream(values()).anyMatch(v -> v.status.equals(status));
    }
}
