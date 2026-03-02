package cn.iocoder.yudao.module.product.enums.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 门店 SKU 库存流水状态
 */
@Getter
@AllArgsConstructor
public enum ProductStoreSkuStockFlowStatusEnum {

    PENDING(0, "待执行"),
    SUCCESS(1, "执行成功"),
    FAILED(2, "执行失败"),
    PROCESSING(3, "执行中");

    private final Integer status;
    private final String name;

    public static boolean isRetryable(Integer status) {
        return Arrays.asList(PENDING.status, FAILED.status).contains(status);
    }

}
