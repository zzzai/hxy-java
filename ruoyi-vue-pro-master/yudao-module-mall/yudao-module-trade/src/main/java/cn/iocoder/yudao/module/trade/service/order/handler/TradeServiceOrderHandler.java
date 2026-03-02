package cn.iocoder.yudao.module.trade.service.order.handler;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务型商品订单处理器
 *
 * 支付成功后识别服务型订单，预留预约单/核销单创建入口。
 */
@Component
@Slf4j
public class TradeServiceOrderHandler implements TradeOrderHandler {

    @Resource
    private TradeServiceOrderService tradeServiceOrderService;

    @Override
    public void afterPayOrder(TradeOrderDO order, List<TradeOrderItemDO> orderItems) {
        List<TradeOrderItemDO> serviceItems = orderItems.stream()
                .filter(item -> ProductTypeEnum.isService(item.getProductType()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(serviceItems)) {
            return;
        }
        int createdCount = tradeServiceOrderService.createByPaidOrder(order, serviceItems);
        log.info("[afterPayOrder][order({}) 服务履约单入队完成，serviceItems={} created={} itemIds={}]",
                order.getId(), serviceItems.size(),
                createdCount,
                serviceItems.stream().map(TradeOrderItemDO::getId).collect(Collectors.toList()));
    }

}
