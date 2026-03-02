package cn.iocoder.yudao.module.trade.service.order;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeServiceOrderMapper;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.order.booking.TradeServiceBookingGateway;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeServiceOrderServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeServiceOrderServiceImpl service;

    @Mock
    private TradeServiceOrderMapper tradeServiceOrderMapper;
    @Mock
    private TradeServiceBookingGateway tradeServiceBookingGateway;

    @Test
    void shouldCreateServiceOrderForServiceItem() {
        TradeOrderDO order = buildOrder();
        TradeOrderItemDO serviceItem = buildItem(11L, ProductTypeEnum.SERVICE.getType());
        serviceItem.setSpuName("60分钟肩颈放松");
        serviceItem.setCount(2);
        serviceItem.setPrice(9800);
        serviceItem.setPayPrice(18800);
        serviceItem.setAddonType(3);
        serviceItem.setAddonSnapshotJson("{\"addonCode\":\"ADD_ON_HOT_STONE\",\"addonName\":\"热石加项\"}");
        serviceItem.setTemplateVersionId(20260302L);
        serviceItem.setTemplateSnapshotJson("{\"templateVersion\":\"v3\"}");
        serviceItem.setPriceSourceSnapshotJson("{\"source\":\"STORE_SKU_OVERRIDE\"}");
        TradeOrderItemDO physicalItem = buildItem(12L, ProductTypeEnum.PHYSICAL.getType());
        when(tradeServiceOrderMapper.selectByOrderItemId(11L)).thenReturn(null);

        int count = service.createByPaidOrder(order, Arrays.asList(serviceItem, physicalItem));

        assertEquals(1, count);
        ArgumentCaptor<TradeServiceOrderDO> captor = ArgumentCaptor.forClass(TradeServiceOrderDO.class);
        verify(tradeServiceOrderMapper).insert(captor.capture());
        assertEquals(order.getId(), captor.getValue().getOrderId());
        assertEquals(serviceItem.getId(), captor.getValue().getOrderItemId());
        assertEquals(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus(), captor.getValue().getStatus());
        assertEquals(serviceItem.getAddonType(), captor.getValue().getAddonType());
        assertEquals(serviceItem.getAddonSnapshotJson(), captor.getValue().getAddonSnapshotJson());
        JsonNode snapshot = JsonUtils.parseTree(captor.getValue().getOrderItemSnapshotJson());
        assertEquals(11L, snapshot.path("orderItemId").asLong());
        assertEquals(3, snapshot.path("addonType").asInt());
        assertEquals(20260302L, snapshot.path("templateVersionId").asLong());
        assertEquals("{\"templateVersion\":\"v3\"}", snapshot.path("templateSnapshotJson").asText());
        assertEquals("{\"source\":\"STORE_SKU_OVERRIDE\"}", snapshot.path("priceSourceSnapshotJson").asText());
        assertEquals("{\"source\":\"STORE_SKU_OVERRIDE\"}", snapshot.path("bundleRefundSnapshotJson").asText());
    }

    @Test
    void shouldGetServiceOrderPage() {
        TradeServiceOrderPageReqVO reqVO = new TradeServiceOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        PageResult<TradeServiceOrderDO> page = new PageResult<>(Collections.singletonList(new TradeServiceOrderDO()), 1L);
        when(tradeServiceOrderMapper.selectPage(reqVO)).thenReturn(page);

        PageResult<TradeServiceOrderDO> result = service.getServiceOrderPage(reqVO);

        assertEquals(1L, result.getTotal());
        verify(tradeServiceOrderMapper).selectPage(reqVO);
    }

    @Test
    void shouldRetryCreateBookingPlaceholder() {
        TradeServiceOrderDO first = new TradeServiceOrderDO();
        first.setId(11L);
        TradeServiceOrderDO second = new TradeServiceOrderDO();
        second.setId(12L);
        when(tradeServiceOrderMapper.selectListForBookingPlaceholderRetry(
                TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus(), 50))
                .thenReturn(Arrays.asList(first, second));

        int count = service.retryCreateBookingPlaceholder(50);

        assertEquals(2, count);
        verify(tradeServiceOrderMapper).selectListForBookingPlaceholderRetry(
                TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus(), 50);
        verify(tradeServiceBookingGateway).createPendingBooking(first);
        verify(tradeServiceBookingGateway).createPendingBooking(second);
    }

    @Test
    void shouldContinueRetryWhenGatewayThrows() {
        TradeServiceOrderDO first = new TradeServiceOrderDO();
        first.setId(21L);
        TradeServiceOrderDO second = new TradeServiceOrderDO();
        second.setId(22L);
        when(tradeServiceOrderMapper.selectListForBookingPlaceholderRetry(
                TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus(), 80))
                .thenReturn(Arrays.asList(first, second));
        doThrow(new RuntimeException("booking down")).when(tradeServiceBookingGateway).createPendingBooking(first);

        int count = service.retryCreateBookingPlaceholder(80);

        assertEquals(1, count);
        verify(tradeServiceBookingGateway).createPendingBooking(first);
        verify(tradeServiceBookingGateway).createPendingBooking(second);
    }

    @Test
    void shouldMarkBooked() {
        TradeServiceOrderDO existed = new TradeServiceOrderDO();
        existed.setId(1L);
        existed.setStatus(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus());
        when(tradeServiceOrderMapper.selectById(1L)).thenReturn(existed);
        when(tradeServiceOrderMapper.updateByIdAndStatus(eq(1L),
                eq(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus()), any()))
                .thenReturn(1);

        service.markBooked(1L, "BOOK_001", "from booking");

        ArgumentCaptor<TradeServiceOrderDO> captor = ArgumentCaptor.forClass(TradeServiceOrderDO.class);
        verify(tradeServiceOrderMapper).updateByIdAndStatus(eq(1L),
                eq(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus()), captor.capture());
        assertEquals(TradeServiceOrderStatusEnum.BOOKED.getStatus(), captor.getValue().getStatus());
        assertEquals("BOOK_001", captor.getValue().getBookingNo());
        assertTrue(captor.getValue().getRemark().contains("BOOKED"));
    }

    @Test
    void shouldThrowWhenMarkBookedWithInvalidStatus() {
        TradeServiceOrderDO existed = new TradeServiceOrderDO();
        existed.setId(2L);
        existed.setStatus(TradeServiceOrderStatusEnum.SERVING.getStatus());
        when(tradeServiceOrderMapper.selectById(2L)).thenReturn(existed);

        assertThrows(ServiceException.class, () -> service.markBooked(2L, "B2", null));
        verify(tradeServiceOrderMapper, never())
                .updateByIdAndStatus(eq(2L), eq(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus()), any());
    }

    @Test
    void shouldStartServing() {
        TradeServiceOrderDO existed = new TradeServiceOrderDO();
        existed.setId(3L);
        existed.setStatus(TradeServiceOrderStatusEnum.BOOKED.getStatus());
        when(tradeServiceOrderMapper.selectById(3L)).thenReturn(existed);
        when(tradeServiceOrderMapper.updateByIdAndStatus(eq(3L),
                eq(TradeServiceOrderStatusEnum.BOOKED.getStatus()), any()))
                .thenReturn(1);

        service.startServing(3L, "tech accepted");

        ArgumentCaptor<TradeServiceOrderDO> captor = ArgumentCaptor.forClass(TradeServiceOrderDO.class);
        verify(tradeServiceOrderMapper).updateByIdAndStatus(eq(3L),
                eq(TradeServiceOrderStatusEnum.BOOKED.getStatus()), captor.capture());
        assertEquals(TradeServiceOrderStatusEnum.SERVING.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void shouldFinishServing() {
        TradeServiceOrderDO existed = new TradeServiceOrderDO();
        existed.setId(4L);
        existed.setStatus(TradeServiceOrderStatusEnum.SERVING.getStatus());
        when(tradeServiceOrderMapper.selectById(4L)).thenReturn(existed);
        when(tradeServiceOrderMapper.updateByIdAndStatus(eq(4L),
                eq(TradeServiceOrderStatusEnum.SERVING.getStatus()), any()))
                .thenReturn(1);

        service.finishServing(4L, "done");

        ArgumentCaptor<TradeServiceOrderDO> captor = ArgumentCaptor.forClass(TradeServiceOrderDO.class);
        verify(tradeServiceOrderMapper).updateByIdAndStatus(eq(4L),
                eq(TradeServiceOrderStatusEnum.SERVING.getStatus()), captor.capture());
        assertEquals(TradeServiceOrderStatusEnum.FINISHED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void shouldFreezeBundleRefundSnapshotWhenFinishServing() {
        TradeServiceOrderDO existed = new TradeServiceOrderDO();
        existed.setId(41L);
        existed.setStatus(TradeServiceOrderStatusEnum.SERVING.getStatus());
        existed.setOrderItemSnapshotJson("{\"snapshotVersion\":\"v1\",\"bundleRefundSnapshotJson\":\"{\\\"bundleRefundablePrice\\\":3000,\\\"bundleChildren\\\":[{\\\"childCode\\\":\\\"A\\\",\\\"refundCapPrice\\\":2000,\\\"fulfilled\\\":false,\\\"refundable\\\":true},{\\\"childCode\\\":\\\"B\\\",\\\"refundCapPrice\\\":1000,\\\"fulfilled\\\":false,\\\"refundable\\\":true}]}\"}");
        when(tradeServiceOrderMapper.selectById(41L)).thenReturn(existed);
        when(tradeServiceOrderMapper.updateByIdAndStatus(eq(41L),
                eq(TradeServiceOrderStatusEnum.SERVING.getStatus()), any()))
                .thenReturn(1);

        service.finishServing(41L, "done");

        ArgumentCaptor<TradeServiceOrderDO> captor = ArgumentCaptor.forClass(TradeServiceOrderDO.class);
        verify(tradeServiceOrderMapper).updateByIdAndStatus(eq(41L),
                eq(TradeServiceOrderStatusEnum.SERVING.getStatus()), captor.capture());
        JsonNode frozenSnapshotRoot = JsonUtils.parseTree(captor.getValue().getOrderItemSnapshotJson());
        JsonNode bundleSnapshot = JsonUtils.parseTree(frozenSnapshotRoot.path("bundleRefundSnapshotJson").asText());
        assertEquals(0, bundleSnapshot.path("bundleRefundablePrice").asInt());
        JsonNode firstChild = bundleSnapshot.path("bundleChildren").path(0);
        JsonNode secondChild = bundleSnapshot.path("bundleChildren").path(1);
        assertTrue(firstChild.path("fulfilled").asBoolean());
        assertTrue(secondChild.path("fulfilled").asBoolean());
        assertTrue(!firstChild.path("refundable").asBoolean());
        assertTrue(!secondChild.path("refundable").asBoolean());
        assertEquals(0, firstChild.path("refundCapPrice").asInt());
        assertEquals(0, secondChild.path("refundCapPrice").asInt());
    }

    @Test
    void shouldCancelServiceOrderIdempotentWhenAlreadyCancelled() {
        TradeServiceOrderDO existed = new TradeServiceOrderDO();
        existed.setId(5L);
        existed.setStatus(TradeServiceOrderStatusEnum.CANCELLED.getStatus());
        when(tradeServiceOrderMapper.selectById(5L)).thenReturn(existed);

        service.cancelServiceOrder(5L, "duplicate-cancel");

        verify(tradeServiceOrderMapper, never()).updateByIdAndStatus(eq(5L), any(), any());
    }

    @Test
    void shouldCreateBookingPlaceholderViaGateway() {
        TradeServiceOrderDO existed = new TradeServiceOrderDO();
        existed.setId(6L);
        when(tradeServiceOrderMapper.selectById(6L)).thenReturn(existed);

        service.createBookingPlaceholder(6L);

        verify(tradeServiceBookingGateway).createPendingBooking(existed);
    }

    @Test
    void shouldSkipBookingPlaceholderWhenServiceOrderMissing() {
        when(tradeServiceOrderMapper.selectById(7L)).thenReturn(null);

        service.createBookingPlaceholder(7L);

        verify(tradeServiceBookingGateway, never()).createPendingBooking(any());
    }

    @Test
    void shouldNotFailWhenBookingGatewayThrowsDuringCreate() {
        TradeOrderDO order = buildOrder();
        TradeOrderItemDO serviceItem = buildItem(31L, ProductTypeEnum.SERVICE.getType());
        when(tradeServiceOrderMapper.selectByOrderItemId(31L)).thenReturn(null);
        doAnswer(invocation -> {
            TradeServiceOrderDO arg = invocation.getArgument(0);
            arg.setId(9001L);
            return null;
        }).when(tradeServiceOrderMapper).insert(any(TradeServiceOrderDO.class));
        when(tradeServiceOrderMapper.selectById(9001L)).thenReturn(TradeServiceOrderDO.builder()
                .id(9001L)
                .status(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus())
                .build());
        doThrow(new RuntimeException("booking down")).when(tradeServiceBookingGateway).createPendingBooking(any());

        int count = service.createByPaidOrder(order, Arrays.asList(serviceItem));

        assertEquals(1, count);
        verify(tradeServiceOrderMapper).insert(any(TradeServiceOrderDO.class));
        verify(tradeServiceBookingGateway).createPendingBooking(any());
    }

    private static TradeOrderDO buildOrder() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(100L);
        order.setNo("ORDER_100");
        order.setUserId(200L);
        order.setPayOrderId(300L);
        return order;
    }

    private static TradeOrderItemDO buildItem(Long id, Integer productType) {
        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setId(id);
        item.setSpuId(1000L + id);
        item.setSkuId(2000L + id);
        item.setProductType(productType);
        return item;
    }

}
