package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 技师佣金类型枚举
 */
@Getter
@AllArgsConstructor
public enum CommissionTypeEnum {

    BASE(1, "基础服务", 0.15),
    DESIGNATED(2, "点钟加成", 0.20),
    EXTEND(3, "加钟服务", 0.20),
    CARD_SALE(4, "卡项销售", 0.05),
    PRODUCT(5, "商品推荐", 0.10),
    GOOD_REVIEW(6, "好评奖励", 0);

    private final Integer type;
    private final String name;
    /** 默认佣金比例 */
    private final double defaultRate;

}
