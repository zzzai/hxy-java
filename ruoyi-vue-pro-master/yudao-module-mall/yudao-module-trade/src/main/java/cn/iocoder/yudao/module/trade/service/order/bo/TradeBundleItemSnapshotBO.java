package cn.iocoder.yudao.module.trade.service.order.bo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * 套餐子项退款快照
 *
 * 用于在服务履约单中固化套餐子项可退口径，避免运行时反查实时配置。
 */
@Data
public class TradeBundleItemSnapshotBO {

    /**
     * 套餐可退上限（分）
     */
    @JsonAlias({"bundleRefundablePrice", "refundablePrice", "maxRefundPrice"})
    private Integer bundleRefundablePrice;

    /**
     * 套餐子项
     */
    @JsonAlias({"bundleChildren", "children", "items"})
    private List<TradeBundleItemChildSnapshotBO> bundleChildren;

    @Data
    public static class TradeBundleItemChildSnapshotBO {

        @JsonAlias({"childCode", "code", "itemCode", "skuCode"})
        private String childCode;

        @JsonAlias({"refundCapPrice", "refundablePrice", "maxRefundPrice"})
        private Integer refundCapPrice;

        @JsonAlias({"fulfilled", "served", "completed", "serviceFulfilled"})
        private Boolean fulfilled;

        @JsonAlias({"refundable", "allowRefund"})
        private Boolean refundable;
    }

}
