package cn.iocoder.yudao.module.trade.service.order;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderItemMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderRefundStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.ORDER_DELIVERY_FAIL_CONTAINS_SERVICE_ITEM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeOrderUpdateServiceDeliveryGuardTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeOrderUpdateServiceImpl service;

    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tradeOrderHandlers", Collections.emptyList());
    }

    @Test
    void deliveryOrder_shouldRejectWhenContainsServiceItem() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(100L);
        order.setStatus(TradeOrderStatusEnum.UNDELIVERED.getStatus());
        order.setRefundStatus(TradeOrderRefundStatusEnum.NONE.getStatus());
        order.setDeliveryType(DeliveryTypeEnum.EXPRESS.getType());
        when(tradeOrderMapper.selectById(100L)).thenReturn(order);

        TradeOrderItemDO serviceItem = new TradeOrderItemDO();
        serviceItem.setOrderId(100L);
        serviceItem.setProductType(ProductTypeEnum.SERVICE.getType());
        when(tradeOrderItemMapper.selectListByOrderId(100L)).thenReturn(Collections.singletonList(serviceItem));

        TradeOrderDeliveryReqVO reqVO = new TradeOrderDeliveryReqVO();
        reqVO.setId(100L);
        reqVO.setLogisticsId(1L);
        reqVO.setLogisticsNo("SF123");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.deliveryOrder(reqVO));
        assertEquals(ORDER_DELIVERY_FAIL_CONTAINS_SERVICE_ITEM.getCode(), ex.getCode());
        verify(tradeOrderMapper, never()).updateByIdAndStatus(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }
}
