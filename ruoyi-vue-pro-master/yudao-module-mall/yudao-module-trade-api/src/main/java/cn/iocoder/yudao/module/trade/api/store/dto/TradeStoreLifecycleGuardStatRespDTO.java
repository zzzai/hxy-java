package cn.iocoder.yudao.module.trade.api.store.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 门店生命周期守卫统计 Response DTO
 */
@Data
@Accessors(chain = true)
public class TradeStoreLifecycleGuardStatRespDTO {

    /**
     * 未结订单数
     */
    private Long pendingOrderCount;
    /**
     * 在途售后工单数
     */
    private Long inflightTicketCount;

}
