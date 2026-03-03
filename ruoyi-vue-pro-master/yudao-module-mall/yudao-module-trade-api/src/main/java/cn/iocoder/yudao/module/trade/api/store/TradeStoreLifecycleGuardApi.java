package cn.iocoder.yudao.module.trade.api.store;

import cn.iocoder.yudao.module.trade.api.store.dto.TradeStoreLifecycleGuardStatRespDTO;

/**
 * 门店生命周期守卫统计 API
 */
public interface TradeStoreLifecycleGuardApi {

    /**
     * 获得门店生命周期守卫统计
     *
     * @param storeId 门店编号
     * @return 守卫统计
     */
    TradeStoreLifecycleGuardStatRespDTO getStoreLifecycleGuardStat(Long storeId);

}
