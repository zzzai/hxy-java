package cn.iocoder.yudao.module.trade.service.order.handler;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeServiceOrderHandlerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeServiceOrderHandler handler;

    @Mock
    private TradeServiceOrderService tradeServiceOrderService;

    @Test
    void shouldSkipWhenNoServiceItems() {
        TradeOrderDO order = buildOrder(100L);
        TradeOrderItemDO physical = buildItem(11L, ProductTypeEnum.PHYSICAL.getType());

        handler.afterPayOrder(order, Collections.singletonList(physical));

        verify(tradeServiceOrderService, never()).createByPaidOrder(any(), anyList());
    }

    @Test
    void shouldCreateServiceOrderWhenHasServiceItems() {
        TradeOrderDO order = buildOrder(101L);
        TradeOrderItemDO physical = buildItem(21L, ProductTypeEnum.PHYSICAL.getType());
        TradeOrderItemDO serviceA = buildItem(22L, ProductTypeEnum.SERVICE.getType());
        TradeOrderItemDO serviceB = buildItem(23L, ProductTypeEnum.SERVICE.getType());
        when(tradeServiceOrderService.createByPaidOrder(order, Arrays.asList(serviceA, serviceB)))
                .thenReturn(2);

        handler.afterPayOrder(order, Arrays.asList(physical, serviceA, serviceB));

        ArgumentCaptor<List<TradeOrderItemDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(tradeServiceOrderService).createByPaidOrder(org.mockito.ArgumentMatchers.eq(order), captor.capture());
        List<TradeOrderItemDO> captured = captor.getValue();
        assertEquals(2, captured.size());
        assertEquals(22L, captured.get(0).getId());
        assertEquals(23L, captured.get(1).getId());
    }

    private static TradeOrderDO buildOrder(Long id) {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(id);
        return order;
    }

    private static TradeOrderItemDO buildItem(Long id, Integer productType) {
        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setId(id);
        item.setProductType(productType);
        return item;
    }

}
