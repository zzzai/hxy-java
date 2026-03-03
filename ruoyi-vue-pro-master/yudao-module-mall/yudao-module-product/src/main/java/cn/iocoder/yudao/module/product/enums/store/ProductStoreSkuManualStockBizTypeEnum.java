package cn.iocoder.yudao.module.product.enums.store;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

/**
 * 门店库存人工调整业务类型
 */
public enum ProductStoreSkuManualStockBizTypeEnum {

    REPLENISH_IN("REPLENISH_IN", 1),
    TRANSFER_IN("TRANSFER_IN", 1),
    TRANSFER_OUT("TRANSFER_OUT", -1),
    STOCKTAKE("STOCKTAKE", 0),
    LOSS("LOSS", -1),
    SCRAP("SCRAP", -1);

    /**
     * 业务类型编码
     */
    private final String code;
    /**
     * 库存变化方向：1 仅允许增加；-1 仅允许减少；0 双向允许
     */
    private final int direction;

    ProductStoreSkuManualStockBizTypeEnum(String code, int direction) {
        this.code = code;
        this.direction = direction;
    }

    public String getCode() {
        return code;
    }

    public String getStockFlowBizType() {
        return "MANUAL_" + code;
    }

    public boolean supportsIncrCount(Integer incrCount) {
        if (incrCount == null || incrCount == 0) {
            return false;
        }
        if (direction == 0) {
            return true;
        }
        return direction > 0 ? incrCount > 0 : incrCount < 0;
    }

    public static ProductStoreSkuManualStockBizTypeEnum valueOfCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        String normalized = StrUtil.trim(code).toUpperCase();
        return Arrays.stream(values())
                .filter(item -> item.code.equals(normalized))
                .findFirst()
                .orElse(null);
    }
}

