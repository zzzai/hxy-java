package cn.iocoder.yudao.module.trade.service.order.booking;

import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认预约网关（占位实现）
 *
 * @author HXY
 */
@Component
@ConditionalOnMissingBean(TradeServiceBookingGateway.class)
@Slf4j
public class NoopTradeServiceBookingGateway implements TradeServiceBookingGateway {

    @Override
    public void createPendingBooking(TradeServiceOrderDO serviceOrder) {
        log.info("[createPendingBooking][serviceOrderId({}) 预约创建接口占位，待接入预约域服务]",
                serviceOrder == null ? null : serviceOrder.getId());
    }

}
