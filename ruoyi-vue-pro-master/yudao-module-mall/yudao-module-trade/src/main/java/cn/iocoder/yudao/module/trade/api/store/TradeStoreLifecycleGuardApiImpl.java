package cn.iocoder.yudao.module.trade.api.store;

import cn.iocoder.yudao.module.trade.api.store.dto.TradeStoreLifecycleGuardStatRespDTO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * 门店生命周期守卫统计 API 实现
 */
@Service
@Validated
public class TradeStoreLifecycleGuardApiImpl implements TradeStoreLifecycleGuardApi {

    @Resource
    private TradeOrderMapper tradeOrderMapper;
    @Resource
    private AfterSaleMapper afterSaleMapper;

    @Override
    public TradeStoreLifecycleGuardStatRespDTO getStoreLifecycleGuardStat(Long storeId) {
        Long pendingOrderCount = tradeOrderMapper.selectCountByPickUpStoreIdAndStatuses(storeId, Arrays.asList(
                TradeOrderStatusEnum.UNPAID.getStatus(),
                TradeOrderStatusEnum.UNDELIVERED.getStatus(),
                TradeOrderStatusEnum.DELIVERED.getStatus()));
        Long inflightTicketCount = afterSaleMapper.selectCountByPickUpStoreIdAndStatuses(
                storeId, AfterSaleStatusEnum.APPLYING_STATUSES);
        return new TradeStoreLifecycleGuardStatRespDTO()
                .setPendingOrderCount(pendingOrderCount == null ? 0L : pendingOrderCount)
                .setInflightTicketCount(inflightTicketCount == null ? 0L : inflightTicketCount);
    }

}
