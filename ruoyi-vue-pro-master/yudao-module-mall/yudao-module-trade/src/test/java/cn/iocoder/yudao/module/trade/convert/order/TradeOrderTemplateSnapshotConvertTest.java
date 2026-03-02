package cn.iocoder.yudao.module.trade.convert.order;

import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderSettlementReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateReqBO;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateRespBO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradeOrderTemplateSnapshotConvertTest {

    @Test
    void convertSettlementReq_shouldCarryTemplateSnapshotFields() {
        AppTradeOrderSettlementReqVO reqVO = new AppTradeOrderSettlementReqVO();
        reqVO.setPointStatus(false);
        reqVO.setItems(Collections.singletonList(buildSettlementItem()));

        TradePriceCalculateReqBO reqBO = TradeOrderConvert.INSTANCE.convert(7L, reqVO, Collections.emptyList());

        TradePriceCalculateReqBO.Item item = reqBO.getItems().get(0);
        assertEquals(2026030201L, item.getTemplateVersionId());
        assertEquals("{\"template\":\"v1\"}", item.getTemplateSnapshotJson());
        assertEquals("{\"source\":\"STORE_OVERRIDE\"}", item.getPriceSourceSnapshotJson());
    }

    @Test
    void convertOrderItems_shouldCarryTemplateSnapshotFields() {
        TradeOrderDO order = new TradeOrderDO();
        order.setUserId(9L);
        TradePriceCalculateRespBO.OrderItem calculateItem = new TradePriceCalculateRespBO.OrderItem();
        calculateItem.setSpuId(1001L);
        calculateItem.setSpuName("荷小悦肩颈服务");
        calculateItem.setSkuId(2001L);
        calculateItem.setCount(1);
        calculateItem.setPrice(9800);
        calculateItem.setPayPrice(9800);
        calculateItem.setDiscountPrice(0);
        calculateItem.setDeliveryPrice(0);
        calculateItem.setTemplateVersionId(2026030202L);
        calculateItem.setTemplateSnapshotJson("{\"template\":\"v2\"}");
        calculateItem.setPriceSourceSnapshotJson("{\"source\":\"HQ_BASE\"}");
        TradePriceCalculateRespBO respBO = new TradePriceCalculateRespBO();
        respBO.setItems(Collections.singletonList(calculateItem));

        List<TradeOrderItemDO> items = TradeOrderConvert.INSTANCE.convertList(order, respBO);

        TradeOrderItemDO orderItem = items.get(0);
        assertEquals(2026030202L, orderItem.getTemplateVersionId());
        assertEquals("{\"template\":\"v2\"}", orderItem.getTemplateSnapshotJson());
        assertEquals("{\"source\":\"HQ_BASE\"}", orderItem.getPriceSourceSnapshotJson());
    }

    private static AppTradeOrderSettlementReqVO.Item buildSettlementItem() {
        AppTradeOrderSettlementReqVO.Item item = new AppTradeOrderSettlementReqVO.Item();
        item.setSkuId(2001L);
        item.setCount(1);
        item.setTemplateVersionId(2026030201L);
        item.setTemplateSnapshotJson("{\"template\":\"v1\"}");
        item.setPriceSourceSnapshotJson("{\"source\":\"STORE_OVERRIDE\"}");
        return item;
    }
}
