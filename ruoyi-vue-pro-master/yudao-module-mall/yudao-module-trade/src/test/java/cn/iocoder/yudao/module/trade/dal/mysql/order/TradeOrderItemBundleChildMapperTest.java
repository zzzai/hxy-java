package cn.iocoder.yudao.module.trade.dal.mysql.order;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemBundleChildDO;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeOrderItemBundleChildMapperTest extends BaseDbUnitTest {

    @Resource
    private TradeOrderItemBundleChildMapper mapper;

    @Test
    void shouldSelectByOrderItemIdInOrder() {
        mapper.insert(buildRow(1001L, "B"));
        mapper.insert(buildRow(1001L, "A"));
        mapper.insert(buildRow(1002L, "C"));

        List<TradeOrderItemBundleChildDO> rows = mapper.selectListByOrderItemId(1001L);

        assertEquals(2, rows.size());
        assertEquals("B", rows.get(0).getChildCode());
        assertEquals("A", rows.get(1).getChildCode());
    }

    @Test
    void shouldCheckExistsByOrderItemId() {
        mapper.insert(buildRow(2001L, "X"));

        assertTrue(mapper.existsByOrderItemId(2001L));
        assertFalse(mapper.existsByOrderItemId(2999L));
    }

    private TradeOrderItemBundleChildDO buildRow(Long orderItemId, String childCode) {
        return TradeOrderItemBundleChildDO.builder()
                .orderId(900000L + orderItemId)
                .orderItemId(orderItemId)
                .spuId(30001L)
                .skuId(40001L)
                .childCode(childCode)
                .skuName("子项-" + childCode)
                .quantity(1)
                .payPrice(1200)
                .refundedPrice(0)
                .fulfillmentStatus(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus())
                .snapshotJson("{\"childCode\":\"" + childCode + "\"}")
                .build();
    }
}
