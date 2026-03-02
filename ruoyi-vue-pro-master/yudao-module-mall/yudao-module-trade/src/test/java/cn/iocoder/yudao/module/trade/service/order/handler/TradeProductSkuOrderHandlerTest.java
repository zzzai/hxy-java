package cn.iocoder.yudao.module.trade.service.order.handler;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuUpdateStockReqDTO;
import cn.iocoder.yudao.module.product.api.store.ProductStoreSkuApi;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuUpdateStockReqDTO;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderItemAfterSaleStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TradeProductSkuOrderHandlerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeProductSkuOrderHandler handler;

    @Mock
    private ProductSkuApi productSkuApi;
    @Mock
    private ProductStoreSkuApi productStoreSkuApi;

    @Test
    void beforeOrderCreate_shouldReserveStoreSkuStockWhenPickup() {
        TradeOrderDO order = buildOrder(DeliveryTypeEnum.PICK_UP.getType(), 11L);
        List<TradeOrderItemDO> items = Arrays.asList(
                buildItem(101L, 2, TradeOrderItemAfterSaleStatusEnum.NONE.getStatus(), ProductTypeEnum.PHYSICAL.getType()),
                buildItem(102L, 1, TradeOrderItemAfterSaleStatusEnum.NONE.getStatus(), ProductTypeEnum.PHYSICAL.getType())
        );

        handler.beforeOrderCreate(order, items);

        ArgumentCaptor<ProductStoreSkuUpdateStockReqDTO> captor = ArgumentCaptor.forClass(ProductStoreSkuUpdateStockReqDTO.class);
        verify(productStoreSkuApi).updateStoreSkuStock(captor.capture());
        ProductStoreSkuUpdateStockReqDTO req = captor.getValue();
        assertEquals(11L, req.getStoreId());
        assertEquals("TRADE_ORDER_RESERVE", req.getBizType());
        assertEquals("NO-PICKUP-11", req.getBizNo());
        assertEquals(2, req.getItems().size());
        assertEquals(101L, req.getItems().get(0).getSkuId());
        assertEquals(-2, req.getItems().get(0).getIncrCount());
        assertEquals(102L, req.getItems().get(1).getSkuId());
        assertEquals(-1, req.getItems().get(1).getIncrCount());
        verify(productSkuApi, never()).updateSkuStock(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void beforeOrderCreate_shouldReserveHeadSkuStockWhenExpress() {
        TradeOrderDO order = buildOrder(DeliveryTypeEnum.EXPRESS.getType(), null);
        List<TradeOrderItemDO> items = Collections.singletonList(
                buildItem(101L, 2, TradeOrderItemAfterSaleStatusEnum.NONE.getStatus(), ProductTypeEnum.PHYSICAL.getType())
        );

        handler.beforeOrderCreate(order, items);

        ArgumentCaptor<ProductSkuUpdateStockReqDTO> captor = ArgumentCaptor.forClass(ProductSkuUpdateStockReqDTO.class);
        verify(productSkuApi).updateSkuStock(captor.capture());
        ProductSkuUpdateStockReqDTO req = captor.getValue();
        assertEquals(1, req.getItems().size());
        assertEquals(101L, req.getItems().get(0).getId());
        assertEquals(-2, req.getItems().get(0).getIncrCount());
        verify(productStoreSkuApi, never()).updateStoreSkuStock(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void afterCancelOrder_shouldRollbackStoreSkuStockWhenPickup() {
        TradeOrderDO order = buildOrder(DeliveryTypeEnum.PICK_UP.getType(), 12L);
        List<TradeOrderItemDO> items = Arrays.asList(
                buildItem(201L, 3, TradeOrderItemAfterSaleStatusEnum.NONE.getStatus(), ProductTypeEnum.PHYSICAL.getType()),
                buildItem(202L, 1, TradeOrderItemAfterSaleStatusEnum.APPLY.getStatus(), ProductTypeEnum.PHYSICAL.getType())
        );

        handler.afterCancelOrder(order, items);

        ArgumentCaptor<ProductStoreSkuUpdateStockReqDTO> captor = ArgumentCaptor.forClass(ProductStoreSkuUpdateStockReqDTO.class);
        verify(productStoreSkuApi).updateStoreSkuStock(captor.capture());
        ProductStoreSkuUpdateStockReqDTO req = captor.getValue();
        assertEquals(12L, req.getStoreId());
        assertEquals("TRADE_ORDER_CANCEL", req.getBizType());
        assertEquals("NO-PICKUP-12", req.getBizNo());
        assertEquals(1, req.getItems().size());
        assertEquals(201L, req.getItems().get(0).getSkuId());
        assertEquals(3, req.getItems().get(0).getIncrCount());
        verify(productSkuApi, never()).updateSkuStock(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void afterCancelOrderItem_shouldRollbackStoreSkuStockWhenPickup() {
        TradeOrderDO order = buildOrder(DeliveryTypeEnum.PICK_UP.getType(), 13L);
        TradeOrderItemDO item = buildItem(301L, 4, TradeOrderItemAfterSaleStatusEnum.NONE.getStatus(), ProductTypeEnum.PHYSICAL.getType());

        handler.afterCancelOrderItem(order, item);

        ArgumentCaptor<ProductStoreSkuUpdateStockReqDTO> captor = ArgumentCaptor.forClass(ProductStoreSkuUpdateStockReqDTO.class);
        verify(productStoreSkuApi).updateStoreSkuStock(captor.capture());
        ProductStoreSkuUpdateStockReqDTO req = captor.getValue();
        assertEquals(13L, req.getStoreId());
        assertEquals("TRADE_ORDER_CANCEL_ITEM", req.getBizType());
        assertEquals("NO-PICKUP-13:301", req.getBizNo());
        assertEquals(1, req.getItems().size());
        assertEquals(301L, req.getItems().get(0).getSkuId());
        assertEquals(4, req.getItems().get(0).getIncrCount());
        verify(productSkuApi, never()).updateSkuStock(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void beforeOrderCreate_shouldIgnoreServiceItemsForStoreStock() {
        TradeOrderDO order = buildOrder(DeliveryTypeEnum.PICK_UP.getType(), 21L);
        List<TradeOrderItemDO> items = Arrays.asList(
                buildItem(401L, 2, TradeOrderItemAfterSaleStatusEnum.NONE.getStatus(), ProductTypeEnum.SERVICE.getType()),
                buildItem(402L, 1, TradeOrderItemAfterSaleStatusEnum.NONE.getStatus(), ProductTypeEnum.PHYSICAL.getType())
        );

        handler.beforeOrderCreate(order, items);

        ArgumentCaptor<ProductStoreSkuUpdateStockReqDTO> captor = ArgumentCaptor.forClass(ProductStoreSkuUpdateStockReqDTO.class);
        verify(productStoreSkuApi).updateStoreSkuStock(captor.capture());
        ProductStoreSkuUpdateStockReqDTO req = captor.getValue();
        assertEquals(1, req.getItems().size());
        assertEquals(402L, req.getItems().get(0).getSkuId());
        assertEquals(-1, req.getItems().get(0).getIncrCount());
        verify(productSkuApi, never()).updateSkuStock(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void beforeOrderCreate_shouldSkipWhenOnlyServiceItems() {
        TradeOrderDO order = buildOrder(DeliveryTypeEnum.PICK_UP.getType(), 22L);
        List<TradeOrderItemDO> items = Collections.singletonList(
                buildItem(501L, 1, TradeOrderItemAfterSaleStatusEnum.NONE.getStatus(), ProductTypeEnum.SERVICE.getType())
        );

        handler.beforeOrderCreate(order, items);

        verify(productStoreSkuApi, never()).updateStoreSkuStock(org.mockito.ArgumentMatchers.any());
        verify(productSkuApi, never()).updateSkuStock(org.mockito.ArgumentMatchers.any());
    }

    private static TradeOrderDO buildOrder(Integer deliveryType, Long pickUpStoreId) {
        TradeOrderDO order = new TradeOrderDO();
        order.setDeliveryType(deliveryType);
        order.setPickUpStoreId(pickUpStoreId);
        if (pickUpStoreId != null) {
            order.setNo("NO-PICKUP-" + pickUpStoreId);
        } else {
            order.setNo("NO-EXPRESS");
        }
        return order;
    }

    private static TradeOrderItemDO buildItem(Long skuId, Integer count, Integer afterSaleStatus, Integer productType) {
        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setSkuId(skuId);
        item.setCount(count);
        item.setAfterSaleStatus(afterSaleStatus);
        item.setProductType(productType);
        return item;
    }

}
