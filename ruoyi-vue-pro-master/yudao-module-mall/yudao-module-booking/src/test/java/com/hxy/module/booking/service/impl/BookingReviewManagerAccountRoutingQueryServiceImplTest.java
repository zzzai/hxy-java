package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStorePageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import com.hxy.module.booking.dal.mysql.BookingReviewManagerAccountRoutingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
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
        assertEquals("CONTACT_ONLY_PENDING_BIND", respVO.getSourceTruthStage());
        assertEquals("联系人待转绑定", respVO.getSourceTruthLabel());
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
        assertEquals("ROUTE_CONFIRMED", respVO.getSourceTruthStage());
        assertEquals("来源已确认", respVO.getSourceTruthLabel());
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
    void shouldKeepSourceTruthConfirmedWhenInactiveRouteAlreadyHasSource() {
        ProductStoreDO store = ProductStoreDO.builder()
                .id(30031L)
                .name("三里屯门店")
                .contactName("吴店长")
                .contactMobile("13600000000")
                .build();
        BookingReviewManagerAccountRoutingDO routing = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(30031L)
                .setManagerAdminUserId(7031L)
                .setManagerWecomUserId("wecom-7031")
                .setBindingStatus("INACTIVE")
                .setSource("SYNC")
                .setLastVerifiedTime(LocalDateTime.now().minusHours(2).withNano(0));
        when(productStoreService.getStore(30031L)).thenReturn(store);
        when(bookingReviewManagerAccountRoutingMapper.selectLatestByStoreId(30031L)).thenReturn(routing);

        BookingReviewManagerAccountRoutingRespVO respVO = service.getRouting(30031L);

        assertEquals("INACTIVE_ROUTE", respVO.getRoutingStatus());
        assertEquals("ROUTE_CONFIRMED", respVO.getSourceTruthStage());
        assertEquals("来源已确认", respVO.getSourceTruthLabel());
    }

    @Test
    void shouldKeepSourceTruthMissingWhenExpiredRouteHasNoSource() {
        ProductStoreDO store = ProductStoreDO.builder()
                .id(30032L)
                .name("亚运村门店")
                .contactName("郑店长")
                .contactMobile("13500000000")
                .build();
        BookingReviewManagerAccountRoutingDO routing = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(30032L)
                .setManagerAdminUserId(7032L)
                .setManagerWecomUserId("wecom-7032")
                .setBindingStatus("ACTIVE")
                .setExpireTime(LocalDateTime.now().minusHours(1).withNano(0))
                .setLastVerifiedTime(LocalDateTime.now().withNano(0));
        when(productStoreService.getStore(30032L)).thenReturn(store);
        when(bookingReviewManagerAccountRoutingMapper.selectLatestByStoreId(30032L)).thenReturn(routing);

        BookingReviewManagerAccountRoutingRespVO respVO = service.getRouting(30032L);

        assertEquals("EXPIRED_ROUTE", respVO.getRoutingStatus());
        assertEquals("SOURCE_MISSING", respVO.getSourceTruthStage());
        assertEquals("来源缺失", respVO.getSourceTruthLabel());
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
        when(bookingReviewManagerAccountRoutingMapper.selectLatestListByStoreIds(anyCollection()))
                .thenReturn(Collections.singletonList(routing));

        PageResult<BookingReviewManagerAccountRoutingRespVO> result = service.getRoutingPage(reqVO);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("国贸门店", result.getList().get(0).getStoreName());
        assertEquals("双通道路由有效", result.getList().get(0).getRoutingLabel());
        assertEquals("wecom-manager-7004", result.getList().get(0).getManagerWecomUserId());
        verify(productStoreService).getStorePage(any(ProductStorePageReqVO.class));
    }

    @Test
    void shouldBuildCoverageSummaryForMissingBindingOpsView() {
        BookingReviewManagerAccountRoutingPageReqVO reqVO = new BookingReviewManagerAccountRoutingPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        ProductStoreDO store1 = ProductStoreDO.builder().id(3101L).name("门店A").build();
        ProductStoreDO store2 = ProductStoreDO.builder().id(3102L).name("门店B").contactMobile("13900000002").build();
        ProductStoreDO store3 = ProductStoreDO.builder().id(3103L).name("门店C").contactMobile("13900000003").build();
        ProductStoreDO store4 = ProductStoreDO.builder().id(3104L).name("门店D").contactMobile("13900000004").build();
        when(productStoreService.getStorePage(any(ProductStorePageReqVO.class)))
                .thenReturn(new PageResult<>(Arrays.asList(store1, store2, store3, store4), 4L));

        BookingReviewManagerAccountRoutingDO dualReady = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3102L)
                .setManagerAdminUserId(7102L)
                .setManagerWecomUserId("wecom-7102")
                .setBindingStatus("ACTIVE")
                .setSource("MANUAL_BIND")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0));
        BookingReviewManagerAccountRoutingDO appOnly = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3103L)
                .setManagerAdminUserId(7103L)
                .setBindingStatus("ACTIVE")
                .setSource("MANUAL_BIND")
                .setLastVerifiedTime(LocalDateTime.now().minusDays(10).withNano(0));
        BookingReviewManagerAccountRoutingDO dualReadySourcePending = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3104L)
                .setManagerAdminUserId(7104L)
                .setManagerWecomUserId("wecom-7104")
                .setBindingStatus("ACTIVE")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0));
        when(bookingReviewManagerAccountRoutingMapper.selectLatestListByStoreIds(anyCollection()))
                .thenReturn(Arrays.asList(dualReady, appOnly, dualReadySourcePending));

        BookingReviewManagerAccountRoutingSummaryRespVO summary = service.getRoutingCoverageSummary(reqVO);

        assertEquals(4L, summary.getTotalStoreCount());
        assertEquals(2L, summary.getDualReadyCount());
        assertEquals(3L, summary.getAppReadyCount());
        assertEquals(2L, summary.getWecomReadyCount());
        assertEquals(2L, summary.getMissingAnyCount());
        assertEquals(1L, summary.getMissingAppCount());
        assertEquals(2L, summary.getMissingWecomCount());
        assertEquals(1L, summary.getMissingBothCount());
        assertEquals(2L, summary.getImmediateFixCount());
        assertEquals(1L, summary.getVerifySourceCount());
        assertEquals(2L, summary.getSourcePendingCount());
        assertEquals(2L, summary.getStaleVerifyCount());
        assertEquals(1L, summary.getObserveReadyCount());
        assertEquals(1L, summary.getRouteConfirmedCount());
        assertEquals(1L, summary.getSourceMissingCount());
        assertEquals(0L, summary.getContactOnlyPendingBindCount());
        assertEquals(1L, summary.getContactMissingCount());
        assertEquals(1L, summary.getVerifyStaleCount());
    }

    @Test
    void shouldFilterRoutingPageByWecomRoutingStatus() {
        BookingReviewManagerAccountRoutingPageReqVO reqVO = new BookingReviewManagerAccountRoutingPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setWecomRoutingStatus("WECOM_MISSING");

        ProductStoreDO appOnlyStore = ProductStoreDO.builder().id(3201L).name("门店E").contactMobile("13900000005").build();
        ProductStoreDO dualReadyStore = ProductStoreDO.builder().id(3202L).name("门店F").contactMobile("13900000006").build();
        when(productStoreService.getStorePage(any(ProductStorePageReqVO.class)))
                .thenReturn(new PageResult<>(Arrays.asList(appOnlyStore, dualReadyStore), 2L));

        BookingReviewManagerAccountRoutingDO appOnly = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3201L)
                .setManagerAdminUserId(7201L)
                .setBindingStatus("ACTIVE")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0));
        BookingReviewManagerAccountRoutingDO dualReady = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3202L)
                .setManagerAdminUserId(7202L)
                .setManagerWecomUserId("wecom-7202")
                .setBindingStatus("ACTIVE")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0));
        when(bookingReviewManagerAccountRoutingMapper.selectLatestListByStoreIds(anyCollection()))
                .thenReturn(Arrays.asList(appOnly, dualReady));

        PageResult<BookingReviewManagerAccountRoutingRespVO> result = service.getRoutingPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("门店E", result.getList().get(0).getStoreName());
        assertEquals("缺店长企微账号", result.getList().get(0).getWecomRoutingLabel());
    }

    @Test
    void shouldDeriveGovernanceFieldsAndFilterByGovernanceStage() {
        BookingReviewManagerAccountRoutingPageReqVO reqVO = new BookingReviewManagerAccountRoutingPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setGovernanceStage("VERIFY_SOURCE");

        ProductStoreDO store1 = ProductStoreDO.builder().id(3301L).name("门店G").contactMobile("13900000007").build();
        ProductStoreDO store2 = ProductStoreDO.builder().id(3302L).name("门店H").contactMobile("13900000008").build();
        ProductStoreDO store3 = ProductStoreDO.builder().id(3303L).name("门店I").contactMobile("13900000009").build();
        when(productStoreService.getStorePage(any(ProductStorePageReqVO.class)))
                .thenReturn(new PageResult<>(Arrays.asList(store1, store2, store3), 3L));

        BookingReviewManagerAccountRoutingDO recentReady = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3301L)
                .setManagerAdminUserId(7301L)
                .setManagerWecomUserId("wecom-7301")
                .setBindingStatus("ACTIVE")
                .setSource("MANUAL_BIND")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0));
        BookingReviewManagerAccountRoutingDO staleReady = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3302L)
                .setManagerAdminUserId(7302L)
                .setManagerWecomUserId("wecom-7302")
                .setBindingStatus("ACTIVE")
                .setSource("SYNC")
                .setLastVerifiedTime(LocalDateTime.now().minusDays(14).withNano(0));
        BookingReviewManagerAccountRoutingDO appMissing = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3303L)
                .setManagerWecomUserId("wecom-7303")
                .setBindingStatus("ACTIVE")
                .setSource("SYNC")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0));
        when(bookingReviewManagerAccountRoutingMapper.selectLatestListByStoreIds(anyCollection()))
                .thenReturn(Arrays.asList(recentReady, staleReady, appMissing));

        PageResult<BookingReviewManagerAccountRoutingRespVO> result = service.getRoutingPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        BookingReviewManagerAccountRoutingRespVO item = result.getList().get(0);
        assertEquals("门店H", item.getStoreName());
        assertEquals("VERIFY_SOURCE", item.getGovernanceStage());
        assertEquals("待核来源闭环", item.getGovernanceStageLabel());
        assertEquals("STALE_VERIFY", item.getVerificationFreshnessStatus());
        assertEquals("长期未核验", item.getVerificationFreshnessLabel());
        assertEquals("SOURCE_READY", item.getSourceClosureStatus());
        assertEquals("来源已登记", item.getSourceClosureLabel());
        assertEquals("P1", item.getGovernancePriority());
        assertEquals("P1 待核来源", item.getGovernancePriorityLabel());
    }

    @Test
    void shouldFilterRoutingPageBySourceTruthStage() {
        BookingReviewManagerAccountRoutingPageReqVO reqVO = new BookingReviewManagerAccountRoutingPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSourceTruthStage("VERIFY_STALE");

        ProductStoreDO store1 = ProductStoreDO.builder()
                .id(3401L).name("门店J").contactName("赵店长").contactMobile("13900000010").build();
        ProductStoreDO store2 = ProductStoreDO.builder()
                .id(3402L).name("门店K").contactName("钱店长").contactMobile("13900000011").build();
        ProductStoreDO store3 = ProductStoreDO.builder()
                .id(3403L).name("门店L").contactName("孙店长").contactMobile("13900000012").build();
        when(productStoreService.getStorePage(any(ProductStorePageReqVO.class)))
                .thenReturn(new PageResult<>(Arrays.asList(store1, store2, store3), 3L));

        BookingReviewManagerAccountRoutingDO verifiedStale = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3401L)
                .setManagerAdminUserId(7401L)
                .setManagerWecomUserId("wecom-7401")
                .setBindingStatus("ACTIVE")
                .setSource("MANUAL_BIND")
                .setLastVerifiedTime(LocalDateTime.now().minusDays(30).withNano(0));
        BookingReviewManagerAccountRoutingDO sourceMissing = new BookingReviewManagerAccountRoutingDO()
                .setStoreId(3402L)
                .setManagerAdminUserId(7402L)
                .setManagerWecomUserId("wecom-7402")
                .setBindingStatus("ACTIVE")
                .setLastVerifiedTime(LocalDateTime.now().minusDays(1).withNano(0));
        when(bookingReviewManagerAccountRoutingMapper.selectLatestListByStoreIds(anyCollection()))
                .thenReturn(Arrays.asList(verifiedStale, sourceMissing));

        PageResult<BookingReviewManagerAccountRoutingRespVO> result = service.getRoutingPage(reqVO);

        assertEquals(1L, result.getTotal());
        BookingReviewManagerAccountRoutingRespVO item = result.getList().get(0);
        assertEquals("门店J", item.getStoreName());
        assertEquals("VERIFY_STALE", item.getSourceTruthStage());
        assertEquals("来源待复核", item.getSourceTruthLabel());
        assertTrue(item.getSourceTruthDetail().contains("lastVerifiedTime"));
    }
}
