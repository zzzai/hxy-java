package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStorePageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import com.hxy.module.booking.dal.mysql.BookingReviewManagerAccountRoutingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingReviewManagerAccountRoutingQueryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingReviewManagerAccountRoutingQueryServiceImpl service;

    @Mock
    private ProductStoreService productStoreService;

    @Mock
    private BookingReviewManagerAccountRoutingMapper bookingReviewManagerAccountRoutingMapper;

    @Test
    void shouldReturnMissingBothChannelSnapshotWhenStoreHasNoRouting() {
        ProductStoreDO store = ProductStoreDO.builder()
                .id(3001L)
                .name("朝阳门店")
                .contactName("王店长")
                .contactMobile("13900000000")
                .build();
        when(productStoreService.getStore(3001L)).thenReturn(store);
        when(bookingReviewManagerAccountRoutingMapper.selectLatestByStoreId(3001L)).thenReturn(null);

        BookingReviewManagerAccountRoutingRespVO respVO = service.getRouting(3001L);

        assertNotNull(respVO);
        assertEquals(3001L, respVO.getStoreId());
        assertEquals("朝阳门店", respVO.getStoreName());
        assertEquals("NO_ROUTE", respVO.getRoutingStatus());
        assertEquals("未绑定店长双通道路由", respVO.getRoutingLabel());
        assertEquals("缺店长 App 账号", respVO.getAppRoutingLabel());
        assertEquals("缺店长企微账号", respVO.getWecomRoutingLabel());
        assertTrue(respVO.getRepairHint().contains("managerAdminUserId"));
        assertTrue(respVO.getRepairHint().contains("managerWecomUserId"));
        verify(productStoreService).getStore(3001L);
        verify(bookingReviewManagerAccountRoutingMapper).selectLatestByStoreId(3001L);
    }

    @Test
    void shouldReturnDualChannelReadySnapshotWhenRoutingEffective() {
        ProductStoreDO store = ProductStoreDO.builder()
                .id(3002L)
                .name("望京门店")
                .contactName("李店长")
                .contactMobile("13800000000")
                .build();
        BookingReviewManagerAccountRoutingDO routing = new BookingReviewManagerAccountRoutingDO()
                .setId(9001L)
                .setStoreId(3002L)
                .setManagerAdminUserId(7001L)
                .setManagerWecomUserId("wecom-manager-7001")
                .setBindingStatus("ACTIVE")
                .setSource("MANUAL_BIND")
                .setLastVerifiedTime(LocalDateTime.now().minusMinutes(5).withNano(0));
        when(productStoreService.getStore(3002L)).thenReturn(store);
        when(bookingReviewManagerAccountRoutingMapper.selectLatestByStoreId(3002L)).thenReturn(routing);

        BookingReviewManagerAccountRoutingRespVO respVO = service.getRouting(3002L);

        assertNotNull(respVO);
        assertEquals("ACTIVE_ROUTE", respVO.getRoutingStatus());
        assertEquals("双通道路由有效", respVO.getRoutingLabel());
        assertEquals(7001L, respVO.getManagerAdminUserId());
        assertEquals("wecom-manager-7001", respVO.getManagerWecomUserId());
        assertEquals("App 路由有效", respVO.getAppRoutingLabel());
        assertEquals("企微路由有效", respVO.getWecomRoutingLabel());
        assertEquals("MANUAL_BIND", respVO.getSource());
    }

    @Test
    void shouldReturnPartialSnapshotWhenWecomMissing() {
        ProductStoreDO store = ProductStoreDO.builder()
                .id(3003L)
                .name("国贸门店")
                .contactName("周店长")
                .contactMobile("13700000000")
                .build();
        BookingReviewManagerAccountRoutingDO routing = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3003L)
                .setManagerAdminUserId(7003L)
                .setManagerWecomUserId(null)
                .setBindingStatus("ACTIVE")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0));
        when(productStoreService.getStore(3003L)).thenReturn(store);
        when(bookingReviewManagerAccountRoutingMapper.selectLatestByStoreId(3003L)).thenReturn(routing);

        BookingReviewManagerAccountRoutingRespVO respVO = service.getRouting(3003L);

        assertEquals("PARTIAL_ROUTE", respVO.getRoutingStatus());
        assertEquals("App 已就绪，企微待补齐", respVO.getRoutingLabel());
        assertEquals("App 路由有效", respVO.getAppRoutingLabel());
        assertEquals("缺店长企微账号", respVO.getWecomRoutingLabel());
    }

    @Test
    void shouldGetRoutingPageAndEnrichStoreRows() {
        BookingReviewManagerAccountRoutingPageReqVO reqVO = new BookingReviewManagerAccountRoutingPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setStoreName("门店");

        ProductStoreDO store = ProductStoreDO.builder()
                .id(3004L)
                .name("国贸门店")
                .contactName("周店长")
                .contactMobile("13700000000")
                .build();
        BookingReviewManagerAccountRoutingDO routing = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3004L)
                .setManagerAdminUserId(7004L)
                .setManagerWecomUserId("wecom-manager-7004")
                .setBindingStatus("ACTIVE")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0));
        when(productStoreService.getStorePage(any(ProductStorePageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(store), 1L));
        when(bookingReviewManagerAccountRoutingMapper.selectLatestByStoreId(3004L)).thenReturn(routing);

        PageResult<BookingReviewManagerAccountRoutingRespVO> result = service.getRoutingPage(reqVO);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("国贸门店", result.getList().get(0).getStoreName());
        assertEquals("双通道路由有效", result.getList().get(0).getRoutingLabel());
        assertEquals("wecom-manager-7004", result.getList().get(0).getManagerWecomUserId());
        verify(productStoreService).getStorePage(any(ProductStorePageReqVO.class));
    }
}
