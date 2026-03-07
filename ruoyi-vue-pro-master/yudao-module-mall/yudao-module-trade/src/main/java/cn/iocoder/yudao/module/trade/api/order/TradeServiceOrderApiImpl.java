package cn.iocoder.yudao.module.trade.api.order;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.trade.api.order.dto.TradeServiceOrderTraceRespDTO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeServiceOrderMapper;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务履约单 API 实现
 *
 * @author HXY
 */
@Service
@Validated
public class TradeServiceOrderApiImpl implements TradeServiceOrderApi {

    @Resource
    private TradeServiceOrderMapper tradeServiceOrderMapper;
    @Resource
    private TradeServiceOrderService tradeServiceOrderService;

    @Override
    public int markBookedByPayOrderId(Long payOrderId, String bookingNo, String remark) {
        return syncByPayOrderId(payOrderId, TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus(), operation ->
                tradeServiceOrderService.markBooked(operation.getId(), bookingNo, remark));
    }

    @Override
    public int startServingByPayOrderId(Long payOrderId, String remark) {
        return syncByPayOrderId(payOrderId, TradeServiceOrderStatusEnum.BOOKED.getStatus(), operation ->
                tradeServiceOrderService.startServing(operation.getId(), remark));
    }

    @Override
    public int finishServingByPayOrderId(Long payOrderId, String remark) {
        return syncByPayOrderId(payOrderId, TradeServiceOrderStatusEnum.SERVING.getStatus(), operation ->
                tradeServiceOrderService.finishServing(operation.getId(), remark));
    }

    @Override
    public int cancelByPayOrderId(Long payOrderId, String remark) {
        if (payOrderId == null || payOrderId <= 0) {
            return 0;
        }
        List<TradeServiceOrderDO> serviceOrders = tradeServiceOrderMapper.selectListByPayOrderId(payOrderId);
        if (CollUtil.isEmpty(serviceOrders)) {
            return 0;
        }
        int synced = 0;
        for (TradeServiceOrderDO serviceOrder : serviceOrders) {
            if (serviceOrder == null || serviceOrder.getId() == null) {
                continue;
            }
            Integer status = serviceOrder.getStatus();
            if (TradeServiceOrderStatusEnum.FINISHED.getStatus().equals(status)
                    || TradeServiceOrderStatusEnum.CANCELLED.getStatus().equals(status)) {
                continue;
            }
            tradeServiceOrderService.cancelServiceOrder(serviceOrder.getId(), remark);
            synced++;
        }
        return synced;
    }

    @Override
    public List<TradeServiceOrderTraceRespDTO> listTraceByPayOrderId(Long payOrderId) {
        if (payOrderId == null || payOrderId <= 0) {
            return Collections.emptyList();
        }
        List<TradeServiceOrderDO> serviceOrders = tradeServiceOrderMapper.selectListByPayOrderId(payOrderId);
        if (CollUtil.isEmpty(serviceOrders)) {
            return Collections.emptyList();
        }
        return serviceOrders.stream()
                .filter(order -> order != null && order.getId() != null)
                .map(order -> new TradeServiceOrderTraceRespDTO()
                        .setServiceOrderId(order.getId())
                        .setOrderItemId(order.getOrderItemId())
                        .setPayOrderId(order.getPayOrderId())
                        .setSpuId(order.getSpuId())
                        .setSkuId(order.getSkuId()))
                .collect(Collectors.toList());
    }

    private int syncByPayOrderId(Long payOrderId, Integer expectedStatus, SyncOperation operation) {
        if (payOrderId == null || payOrderId <= 0) {
            return 0;
        }
        List<TradeServiceOrderDO> serviceOrders = tradeServiceOrderMapper.selectListByPayOrderId(payOrderId);
        if (CollUtil.isEmpty(serviceOrders)) {
            return 0;
        }
        int synced = 0;
        for (TradeServiceOrderDO serviceOrder : serviceOrders) {
            if (serviceOrder == null || serviceOrder.getId() == null) {
                continue;
            }
            if (!expectedStatus.equals(serviceOrder.getStatus())) {
                continue;
            }
            operation.apply(serviceOrder);
            synced++;
        }
        return synced;
    }

    @FunctionalInterface
    private interface SyncOperation {

        void apply(TradeServiceOrderDO serviceOrder);

    }

}
