package cn.iocoder.yudao.module.trade.service.order;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemBundleChildDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradeOrderCreateServiceImplTest extends BaseMockitoUnitTest {

    private final TradeOrderUpdateServiceImpl service = new TradeOrderUpdateServiceImpl();

    @SuppressWarnings("unchecked")
    @Test
    void shouldBuildBundleChildLedgerRowsFromBundleSnapshot() {
        TradeOrderItemDO orderItem = new TradeOrderItemDO();
        orderItem.setId(1001L);
        orderItem.setOrderId(9001L);
        orderItem.setSpuId(2001L);
        orderItem.setSkuId(3001L);
        orderItem.setSpuName("套餐A");
        orderItem.setBundleItemSnapshotJson("{\"bundleChildren\":[{\"childCode\":\"A\",\"refundCapPrice\":1200,\"quantity\":1},{\"childCode\":\"B\",\"refundCapPrice\":800,\"quantity\":2}]}");

        List<TradeOrderItemBundleChildDO> rows = (List<TradeOrderItemBundleChildDO>) ReflectionTestUtils
                .invokeMethod(service, "buildBundleChildLedgerRows", orderItem);

        assertEquals(2, rows.size());
        assertEquals("A", rows.get(0).getChildCode());
        assertEquals(1200, rows.get(0).getPayPrice());
        assertEquals(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus(), rows.get(0).getFulfillmentStatus());
        assertEquals("B", rows.get(1).getChildCode());
        assertEquals(2, rows.get(1).getQuantity());
        assertEquals(800, rows.get(1).getPayPrice());
    }
}
