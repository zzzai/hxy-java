package cn.iocoder.yudao.module.trade.service.order.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.store.ProductStoreSkuApi;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuUpdateStockReqDTO;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.trade.convert.order.TradeOrderConvert;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * 商品 SKU 库存的 {@link TradeOrderHandler} 实现类
 *
 * @author 芋道源码
 */
@Component
public class TradeProductSkuOrderHandler implements TradeOrderHandler {

    private static final String STOCK_BIZ_TYPE_ORDER_RESERVE = "TRADE_ORDER_RESERVE";
    private static final String STOCK_BIZ_TYPE_ORDER_CANCEL = "TRADE_ORDER_CANCEL";
    private static final String STOCK_BIZ_TYPE_ORDER_CANCEL_ITEM = "TRADE_ORDER_CANCEL_ITEM";

    @Resource
    private ProductSkuApi productSkuApi;
    @Resource
    private ProductStoreSkuApi productStoreSkuApi;

    @Override
    public void beforeOrderCreate(TradeOrderDO order, List<TradeOrderItemDO> orderItems) {
        orderItems = filterStockAffectOrderItems(orderItems);
        if (CollUtil.isEmpty(orderItems)) {
            return;
        }
        if (useStoreSkuStock(order)) {
            productStoreSkuApi.updateStoreSkuStock(buildStoreStockReq(order, orderItems, true,
                    STOCK_BIZ_TYPE_ORDER_RESERVE, resolveOrderBizNo(order)));
            return;
        }
        productSkuApi.updateSkuStock(TradeOrderConvert.INSTANCE.convertNegative(orderItems));
    }

    @Override
    public void afterCancelOrder(TradeOrderDO order, List<TradeOrderItemDO> orderItems) {
        // 售后的订单项，已经在 afterCancelOrderItem 回滚库存，所以这里不需要重复回滚
        orderItems = filterOrderItemListByNoneAfterSale(orderItems);
        orderItems = filterStockAffectOrderItems(orderItems);
        if (CollUtil.isEmpty(orderItems)) {
            return;
        }
        if (useStoreSkuStock(order)) {
            productStoreSkuApi.updateStoreSkuStock(buildStoreStockReq(order, orderItems, false,
                    STOCK_BIZ_TYPE_ORDER_CANCEL, resolveOrderBizNo(order)));
            return;
        }
        productSkuApi.updateSkuStock(TradeOrderConvert.INSTANCE.convert(orderItems));
    }

    @Override
    public void afterCancelOrderItem(TradeOrderDO order, TradeOrderItemDO orderItem) {
        if (!affectSkuStock(orderItem)) {
            return;
        }
        if (useStoreSkuStock(order)) {
            productStoreSkuApi.updateStoreSkuStock(buildStoreStockReq(order, singletonList(orderItem), false,
                    STOCK_BIZ_TYPE_ORDER_CANCEL_ITEM,
                    resolveOrderBizNo(order) + ":" + resolveOrderItemBizKey(orderItem)));
            return;
        }
        productSkuApi.updateSkuStock(TradeOrderConvert.INSTANCE.convert(singletonList(orderItem)));
    }

    private static boolean useStoreSkuStock(TradeOrderDO order) {
        return Objects.equals(order.getDeliveryType(), DeliveryTypeEnum.PICK_UP.getType())
                && order.getPickUpStoreId() != null;
    }

    private static List<TradeOrderItemDO> filterStockAffectOrderItems(List<TradeOrderItemDO> orderItems) {
        if (CollUtil.isEmpty(orderItems)) {
            return orderItems;
        }
        return orderItems.stream()
                .filter(TradeProductSkuOrderHandler::affectSkuStock)
                .collect(Collectors.toList());
    }

    private static boolean affectSkuStock(TradeOrderItemDO orderItem) {
        return orderItem != null && !ProductTypeEnum.isService(orderItem.getProductType());
    }

    private static ProductStoreSkuUpdateStockReqDTO buildStoreStockReq(TradeOrderDO order, List<TradeOrderItemDO> orderItems,
                                                                        boolean decrease, String bizType, String bizNo) {
        ProductStoreSkuUpdateStockReqDTO reqDTO = new ProductStoreSkuUpdateStockReqDTO();
        reqDTO.setStoreId(order.getPickUpStoreId());
        reqDTO.setBizType(bizType);
        reqDTO.setBizNo(bizNo);
        Map<Long, Integer> skuCountMap = orderItems.stream().collect(Collectors.toMap(
                TradeOrderItemDO::getSkuId,
                orderItem -> decrease ? -orderItem.getCount() : orderItem.getCount(),
                Integer::sum,
                LinkedHashMap::new
        ));
        reqDTO.setItems(skuCountMap.entrySet().stream().map(entry -> {
            ProductStoreSkuUpdateStockReqDTO.Item item = new ProductStoreSkuUpdateStockReqDTO.Item();
            item.setSkuId(entry.getKey());
            item.setIncrCount(entry.getValue());
            return item;
        }).collect(Collectors.toList()));
        return reqDTO;
    }

    private static String resolveOrderBizNo(TradeOrderDO order) {
        return StrUtil.blankToDefault(order.getNo(), String.valueOf(order.getId()));
    }

    private static String resolveOrderItemBizKey(TradeOrderItemDO orderItem) {
        if (orderItem.getId() != null) {
            return String.valueOf(orderItem.getId());
        }
        return String.valueOf(orderItem.getSkuId());
    }

}
