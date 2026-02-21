package cn.iocoder.yudao.module.trade.controller.app.compat.crmeb;

import lombok.Data;

import java.util.List;

/**
 * CRMEB 预下单缓存存储。
 */
public interface CrmebPreOrderStore {

    void save(Long userId, CrmebPreOrderContext context);

    CrmebPreOrderContext get(Long userId, String preOrderNo);

    void remove(Long userId, String preOrderNo);

    @Data
    class CrmebPreOrderContext {

        private String preOrderNo;
        private String preOrderType;

        private List<CrmebPreOrderItem> items;

        private Long seckillActivityId;
        private Long bargainActivityId;
        private Long combinationActivityId;
        private Long combinationHeadId;
        private Long bargainRecordId;
        private Long pointActivityId;

        private Integer shippingType;
        private Long addressId;
        private Long couponId;
        private Boolean useIntegral;

        private Long storeId;
        private String realName;
        private String phone;
        private String mark;
    }

    @Data
    class CrmebPreOrderItem {
        private Long cartId;
        private Long skuId;
        private Integer count;
    }
}
