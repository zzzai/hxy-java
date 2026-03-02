package cn.iocoder.yudao.module.trade.service.order.booking;

import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;

/**
 * 服务履约单预约网关
 *
 * 用于隔离交易域与预约域，后续可替换为真实预约实现。
 *
 * @author HXY
 */
public interface TradeServiceBookingGateway {

    /**
     * 为服务履约单创建预约占位
     *
     * @param serviceOrder 服务履约单
     */
    void createPendingBooking(TradeServiceOrderDO serviceOrder);

}
