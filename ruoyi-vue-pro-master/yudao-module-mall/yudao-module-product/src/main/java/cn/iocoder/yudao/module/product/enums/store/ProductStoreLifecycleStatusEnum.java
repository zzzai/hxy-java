package cn.iocoder.yudao.module.product.enums.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 门店生命周期状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProductStoreLifecycleStatusEnum {

    PREPARING(10, "筹备中"),
    TRIAL(20, "试营业"),
    OPERATING(30, "营业中"),
    SUSPENDED(35, "停业"),
    CLOSED(40, "闭店");

    private final Integer status;
    private final String name;

    public static Optional<ProductStoreLifecycleStatusEnum> of(Integer status) {
        return Arrays.stream(values()).filter(e -> e.status.equals(status)).findFirst();
    }

    public static boolean isValid(Integer status) {
        return of(status).isPresent();
    }

    public static Set<Integer> nextStatuses(Integer currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        if (PREPARING.status.equals(currentStatus)) {
            return new HashSet<>(Arrays.asList(TRIAL.status, OPERATING.status, CLOSED.status));
        }
        if (TRIAL.status.equals(currentStatus)) {
            return new HashSet<>(Arrays.asList(OPERATING.status, SUSPENDED.status, CLOSED.status));
        }
        if (OPERATING.status.equals(currentStatus)) {
            return new HashSet<>(Arrays.asList(SUSPENDED.status, CLOSED.status));
        }
        if (SUSPENDED.status.equals(currentStatus)) {
            return new HashSet<>(Arrays.asList(OPERATING.status, CLOSED.status));
        }
        return Collections.emptySet();
    }

}
