package com.hxy.server.config.booking;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.service.BookingOrderService;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import cn.iocoder.yudao.module.trade.service.order.booking.TradeServiceBookingGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 服务端预约网关实现
 *
 * 通过支付单号尝试对齐 booking_order 与 trade_service_order 的预约关系。
 * 若 booking 模块尚未生成预约单，则保持待预约状态，交由后续重试任务收敛。
 *
 * @author HXY
 */
@Component
@Slf4j
public class ServerTradeServiceBookingGateway implements TradeServiceBookingGateway {

    private static final String SYNC_REMARK = "SYNC_FROM_BOOKING_MODULE_BY_PAY_ORDER";

    @Resource
    private BookingOrderService bookingOrderService;
    @Resource
    private TradeServiceOrderService tradeServiceOrderService;

    @Override
    public void createPendingBooking(TradeServiceOrderDO serviceOrder) {
        if (serviceOrder == null || serviceOrder.getId() == null) {
            return;
        }
        if (!TradeServiceOrderStatusEnum.isWaitBooking(serviceOrder.getStatus())) {
            return;
        }
        if (serviceOrder.getPayOrderId() == null) {
            log.info("[createPendingBooking][serviceOrderId({}) 无 payOrderId，跳过 booking 对齐]", serviceOrder.getId());
            return;
        }

        BookingOrderDO bookingOrder = bookingOrderService.getOrderByPayOrderId(serviceOrder.getPayOrderId());
        if (bookingOrder == null) {
            log.info("[createPendingBooking][serviceOrderId({}) payOrderId({}) 未找到 booking_order，保持待预约]",
                    serviceOrder.getId(), serviceOrder.getPayOrderId());
            return;
        }
        try {
            tradeServiceOrderService.markBooked(serviceOrder.getId(), bookingOrder.getOrderNo(), SYNC_REMARK);
        } catch (ServiceException ex) {
            // 并发场景下可能已被其它流程更新为已预约，按幂等忽略
            log.warn("[createPendingBooking][serviceOrderId({}) bookingNo({}) 标记已预约失败，按幂等忽略：{}]",
                    serviceOrder.getId(), bookingOrder.getOrderNo(), ex.getMessage());
        }
    }

}
