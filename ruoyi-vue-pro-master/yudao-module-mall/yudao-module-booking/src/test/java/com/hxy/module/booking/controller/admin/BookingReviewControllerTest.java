package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import com.hxy.module.booking.controller.admin.vo.BookingReviewDashboardRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewFollowUpdateReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanItemRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanSummaryRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerTodoClaimReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerTodoCloseReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerTodoFirstActionReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewReplyReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.service.BookingReviewService;
import com.hxy.module.booking.service.TechnicianService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingReviewControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingReviewController controller;

    @Mock
    private BookingReviewService bookingReviewService;

    @Mock
    private ProductStoreService productStoreService;

    @Mock
    private TechnicianService technicianService;

    @Mock
    private MemberUserApi memberUserApi;

    @Test
    void shouldGetReviewPageForLowScoreQueue() {
        BookingReviewPageReqVO reqVO = new BookingReviewPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setRiskLevel(2);
        BookingReviewDO row = BookingReviewDO.builder()
                .id(1001L)
                .bookingOrderId(2001L)
                .storeId(3001L)
                .technicianId(4001L)
                .memberId(5001L)
                .riskLevel(2)
                .followStatus(1)
                .overallScore(2)
                .submitTime(LocalDateTime.now().withNano(0))
                .build();
        when(bookingReviewService.getAdminReviewPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));
        Map<Long, ProductStoreDO> storeMap = new HashMap<>();
        storeMap.put(3001L, ProductStoreDO.builder().id(3001L).name("朝阳门店").build());
        when(productStoreService.getStoreMap(Collections.singleton(3001L))).thenReturn(storeMap);
        when(technicianService.getTechnician(4001L)).thenReturn(TechnicianDO.builder().id(4001L).name("李技师").build());
        Map<Long, MemberUserRespDTO> userMap = new HashMap<>();
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(5001L);
        member.setNickname("安心会员");
        userMap.put(5001L, member);
        when(memberUserApi.getUserMap(Collections.singleton(5001L))).thenReturn(userMap);

        CommonResult<PageResult<BookingReviewRespVO>> result = controller.page(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(1001L, result.getData().getList().get(0).getId());
        assertEquals("朝阳门店", result.getData().getList().get(0).getStoreName());
        assertEquals("李技师", result.getData().getList().get(0).getTechnicianName());
        assertEquals("安心会员", result.getData().getList().get(0).getMemberNickname());
        verify(bookingReviewService).getAdminReviewPage(reqVO);
    }

    @Test
    void shouldGetReviewDetail() {
        BookingReviewDO review = BookingReviewDO.builder()
                .id(1002L)
                .bookingOrderId(2002L)
                .storeId(3002L)
                .technicianId(4002L)
                .memberId(5002L)
                .replyContent("已联系用户")
                .followStatus(2)
                .build();
        when(bookingReviewService.getAdminReview(1002L)).thenReturn(review);
        when(productStoreService.getStoreMap(Collections.singleton(3002L)))
                .thenReturn(Collections.singletonMap(3002L, ProductStoreDO.builder().id(3002L).name("望京门店").build()));
        when(technicianService.getTechnician(4002L)).thenReturn(TechnicianDO.builder().id(4002L).name("张技师").build());
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(5002L);
        member.setNickname("高频会员");
        when(memberUserApi.getUserMap(Collections.singleton(5002L)))
                .thenReturn(Collections.singletonMap(5002L, member));

        CommonResult<BookingReviewRespVO> result = controller.get(1002L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1002L, result.getData().getId());
        assertEquals("已联系用户", result.getData().getReplyContent());
        assertEquals("望京门店", result.getData().getStoreName());
        assertEquals("张技师", result.getData().getTechnicianName());
        assertEquals("高频会员", result.getData().getMemberNickname());
        verify(bookingReviewService).getAdminReview(1002L);
    }

    @Test
    void shouldReplyReview() {
        BookingReviewReplyReqVO reqVO = new BookingReviewReplyReqVO();
        reqVO.setReviewId(1003L);
        reqVO.setReplyContent("已联系用户，安排店长回访");

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);

            CommonResult<Boolean> result = controller.reply(reqVO);

            assertTrue(result.isSuccess());
            assertTrue(Boolean.TRUE.equals(result.getData()));
        }

        verify(bookingReviewService).replyReview(1003L, 88L, "已联系用户，安排店长回访");
    }

    @Test
    void shouldUpdateFollowStatus() {
        BookingReviewFollowUpdateReqVO reqVO = new BookingReviewFollowUpdateReqVO();
        reqVO.setReviewId(1004L);
        reqVO.setFollowStatus(2);
        reqVO.setFollowResult("已同步门店店长处理");

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<Boolean> result = controller.updateFollowStatus(reqVO);

            assertTrue(result.isSuccess());
            assertTrue(Boolean.TRUE.equals(result.getData()));
        }

        verify(bookingReviewService).updateFollowStatus(1004L, 99L, reqVO);
    }

    @Test
    void shouldGetDashboardSummary() {
        BookingReviewDashboardRespVO respVO = new BookingReviewDashboardRespVO();
        respVO.setTotalCount(10L);
        respVO.setNegativeCount(2L);
        respVO.setPendingFollowCount(1L);
        respVO.setUrgentCount(1L);
        when(bookingReviewService.getDashboardSummary()).thenReturn(respVO);

        CommonResult<BookingReviewDashboardRespVO> result = controller.dashboardSummary();

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(10L, result.getData().getTotalCount());
        assertEquals(2L, result.getData().getNegativeCount());
        assertEquals(1L, result.getData().getPendingFollowCount());
        verify(bookingReviewService).getDashboardSummary();
    }

    @Test
    void shouldGetHistoryScanSummaryAndReadableNames() {
        BookingReviewHistoryScanReqVO reqVO = new BookingReviewHistoryScanReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        BookingReviewHistoryScanItemRespVO item = new BookingReviewHistoryScanItemRespVO();
        item.setReviewId(1101L);
        item.setBookingOrderId(2101L);
        item.setStoreId(3101L);
        item.setTechnicianId(4101L);
        item.setMemberId(5101L);
        item.setRiskCategory("MANUAL_READY");
        BookingReviewHistoryScanSummaryRespVO summary = new BookingReviewHistoryScanSummaryRespVO();
        summary.setScannedCount(1L);
        summary.setManualReadyCount(1L);
        BookingReviewHistoryScanRespVO respVO = new BookingReviewHistoryScanRespVO();
        respVO.setSummary(summary);
        respVO.setList(Collections.singletonList(item));
        respVO.setTotal(1L);

        when(bookingReviewService.scanAdminHistoryCandidates(reqVO)).thenReturn(respVO);
        when(productStoreService.getStoreMap(Collections.singleton(3101L)))
                .thenReturn(Collections.singletonMap(3101L, ProductStoreDO.builder().id(3101L).name("国贸门店").build()));
        when(technicianService.getTechnician(4101L)).thenReturn(TechnicianDO.builder().id(4101L).name("周技师").build());
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(5101L);
        member.setNickname("高净值会员");
        when(memberUserApi.getUserMap(Collections.singleton(5101L)))
                .thenReturn(Collections.singletonMap(5101L, member));

        CommonResult<BookingReviewHistoryScanRespVO> result = controller.historyScan(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getSummary().getScannedCount());
        assertEquals(1L, result.getData().getTotal());
        assertEquals("国贸门店", result.getData().getList().get(0).getStoreName());
        assertEquals("周技师", result.getData().getList().get(0).getTechnicianName());
        assertEquals("高净值会员", result.getData().getList().get(0).getMemberNickname());
        verify(bookingReviewService).scanAdminHistoryCandidates(reqVO);
    }

    @Test
    void shouldClaimManagerTodo() {
        BookingReviewManagerTodoClaimReqVO reqVO = new BookingReviewManagerTodoClaimReqVO();
        reqVO.setReviewId(1010L);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<Boolean> result = controller.claimManagerTodo(reqVO);

            assertTrue(result.isSuccess());
            assertTrue(Boolean.TRUE.equals(result.getData()));
        }

        verify(bookingReviewService).claimManagerTodo(1010L, 66L);
    }

    @Test
    void shouldRecordManagerFirstAction() {
        BookingReviewManagerTodoFirstActionReqVO reqVO = new BookingReviewManagerTodoFirstActionReqVO();
        reqVO.setReviewId(1011L);
        reqVO.setRemark("已电话联系店长确认处理");

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(67L);

            CommonResult<Boolean> result = controller.recordManagerFirstAction(reqVO);

            assertTrue(result.isSuccess());
            assertTrue(Boolean.TRUE.equals(result.getData()));
        }

        verify(bookingReviewService).recordManagerFirstAction(1011L, 67L, "已电话联系店长确认处理");
    }

    @Test
    void shouldCloseManagerTodo() {
        BookingReviewManagerTodoCloseReqVO reqVO = new BookingReviewManagerTodoCloseReqVO();
        reqVO.setReviewId(1012L);
        reqVO.setRemark("店长已完成闭环");

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(68L);

            CommonResult<Boolean> result = controller.closeManagerTodo(reqVO);

            assertTrue(result.isSuccess());
            assertTrue(Boolean.TRUE.equals(result.getData()));
        }

        verify(bookingReviewService).closeManagerTodo(1012L, 68L, "店长已完成闭环");
    }
}
