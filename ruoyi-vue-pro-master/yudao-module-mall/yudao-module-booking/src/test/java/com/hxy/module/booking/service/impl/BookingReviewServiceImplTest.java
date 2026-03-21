package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.admin.vo.BookingReviewDashboardRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewFollowUpdateReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanItemRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewPageReqVO;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewCreateReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewEligibilityRespVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewPageReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.mysql.BookingOrderMapper;
import com.hxy.module.booking.dal.mysql.BookingReviewMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.enums.BookingReviewFollowStatusEnum;
import com.hxy.module.booking.enums.BookingReviewLevelEnum;
import com.hxy.module.booking.enums.BookingReviewManagerTodoStatusEnum;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import com.hxy.module.booking.service.BookingReviewService;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import cn.iocoder.yudao.module.trade.api.order.TradeServiceOrderApi;
import cn.iocoder.yudao.module.trade.api.order.dto.TradeServiceOrderTraceRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.mockito.ArgumentCaptor;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_ALREADY_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOT_ELIGIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(BookingReviewServiceImpl.class)
class BookingReviewServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BookingReviewService bookingReviewService;

    @Resource
    private BookingOrderMapper bookingOrderMapper;

    @Resource
    private BookingReviewMapper bookingReviewMapper;

    @MockBean
    private TradeServiceOrderApi tradeServiceOrderApi;

    @MockBean
    private ProductStoreService productStoreService;

    @MockBean
    private BookingReviewNotifyOutboxService bookingReviewNotifyOutboxService;

    @Test
    void shouldRejectCreateReviewWhenOrderNotCompleted() {
        BookingOrderDO order = buildOrder(1001L, 10L, BookingOrderStatusEnum.PAID.getStatus());
        bookingOrderMapper.insert(order);

        AppBookingReviewCreateReqVO reqVO = new AppBookingReviewCreateReqVO();
        reqVO.setBookingOrderId(order.getId());
        reqVO.setOverallScore(2);

        assertServiceException(() -> bookingReviewService.createReview(10L, reqVO), BOOKING_REVIEW_NOT_ELIGIBLE);
    }

    @Test
    void shouldCreateNegativeReviewAndDeriveRecoveryFlags() {
        BookingOrderDO order = buildOrder(1002L, 20L, BookingOrderStatusEnum.COMPLETED.getStatus());
        order.setPayOrderId(88001L);
        bookingOrderMapper.insert(order);
        when(productStoreService.getStore(eq(order.getStoreId()))).thenReturn(ProductStoreDO.builder()
                .id(order.getStoreId())
                .contactName("王店长")
                .contactMobile("13900000000")
                .build());
        when(tradeServiceOrderApi.listTraceByPayOrderId(eq(88001L))).thenReturn(Collections.singletonList(
                new TradeServiceOrderTraceRespDTO().setServiceOrderId(99001L).setOrderItemId(77001L)
                        .setSpuId(order.getSpuId()).setSkuId(order.getSkuId())));

        AppBookingReviewCreateReqVO reqVO = new AppBookingReviewCreateReqVO();
        reqVO.setBookingOrderId(order.getId());
        reqVO.setOverallScore(2);
        reqVO.setServiceScore(2);
        reqVO.setTechnicianScore(1);
        reqVO.setEnvironmentScore(3);
        reqVO.setTags(Arrays.asList("服务敷衍", "沟通不清楚"));
        reqVO.setContent("服务体验明显低于预期");
        reqVO.setPicUrls(Arrays.asList("https://example.com/review-negative-1.png"));
        reqVO.setAnonymous(Boolean.TRUE);
        reqVO.setSource("order_detail");

        Long reviewId = bookingReviewService.createReview(20L, reqVO);

        BookingReviewDO actual = bookingReviewMapper.selectById(reviewId);
        assertNotNull(actual);
        assertEquals(BookingReviewLevelEnum.NEGATIVE.getLevel(), actual.getReviewLevel());
        assertEquals(BookingReviewFollowStatusEnum.PENDING.getStatus(), actual.getFollowStatus());
        assertEquals(order.getId(), actual.getBookingOrderId());
        assertEquals(99001L, actual.getServiceOrderId());
        assertEquals(order.getStoreId(), actual.getStoreId());
        assertEquals(order.getTechnicianId(), actual.getTechnicianId());
        assertEquals(order.getSpuId(), actual.getServiceSpuId());
        assertEquals(order.getSkuId(), actual.getServiceSkuId());
        assertEquals(reqVO.getTags(), actual.getTags());
        assertEquals(reqVO.getPicUrls(), actual.getPicUrls());
        assertEquals("REVIEW_LEVEL_NEGATIVE", actual.getNegativeTriggerType());
        assertEquals("王店长", actual.getManagerContactName());
        assertEquals("13900000000", actual.getManagerContactMobile());
        assertEquals(1, actual.getManagerTodoStatus());
        assertNotNull(actual.getManagerClaimDeadlineAt());
        assertNotNull(actual.getManagerFirstActionDeadlineAt());
        assertNotNull(actual.getManagerCloseDeadlineAt());
        ArgumentCaptor<BookingReviewDO> captor = ArgumentCaptor.forClass(BookingReviewDO.class);
        verify(bookingReviewNotifyOutboxService).createNegativeReviewCreatedOutbox(captor.capture());
        assertEquals(reviewId, captor.getValue().getId());
        assertEquals(order.getStoreId(), captor.getValue().getStoreId());
        assertEquals(BookingReviewLevelEnum.NEGATIVE.getLevel(), captor.getValue().getReviewLevel());
    }

    @Test
    void shouldRejectDuplicateReviewForSameBookingOrder() {
        BookingOrderDO order = buildOrder(1003L, 30L, BookingOrderStatusEnum.COMPLETED.getStatus());
        bookingOrderMapper.insert(order);

        BookingReviewDO review = BookingReviewDO.builder()
                .bookingOrderId(order.getId())
                .storeId(order.getStoreId())
                .technicianId(order.getTechnicianId())
                .memberId(order.getUserId())
                .serviceSpuId(order.getSpuId())
                .serviceSkuId(order.getSkuId())
                .overallScore(5)
                .reviewLevel(BookingReviewLevelEnum.POSITIVE.getLevel())
                .followStatus(BookingReviewFollowStatusEnum.NONE.getStatus())
                .completedTime(order.getServiceEndTime())
                .submitTime(LocalDateTime.now().withNano(0))
                .build();
        bookingReviewMapper.insert(review);

        AppBookingReviewCreateReqVO reqVO = new AppBookingReviewCreateReqVO();
        reqVO.setBookingOrderId(order.getId());
        reqVO.setOverallScore(4);

        assertServiceException(() -> bookingReviewService.createReview(30L, reqVO), BOOKING_REVIEW_ALREADY_EXISTS);
    }

    @Test
    void shouldCreateReviewWhenTraceLookupFails() {
        BookingOrderDO order = buildOrder(10031L, 301L, BookingOrderStatusEnum.COMPLETED.getStatus());
        order.setPayOrderId(88031L);
        bookingOrderMapper.insert(order);
        when(tradeServiceOrderApi.listTraceByPayOrderId(eq(88031L))).thenThrow(new RuntimeException("trade trace down"));

        AppBookingReviewCreateReqVO reqVO = new AppBookingReviewCreateReqVO();
        reqVO.setBookingOrderId(order.getId());
        reqVO.setOverallScore(5);

        Long reviewId = bookingReviewService.createReview(301L, reqVO);

        BookingReviewDO actual = bookingReviewMapper.selectById(reviewId);
        assertNotNull(actual);
        assertEquals(order.getId(), actual.getBookingOrderId());
        assertNull(actual.getServiceOrderId());
        verify(bookingReviewNotifyOutboxService, never()).createNegativeReviewCreatedOutbox(any());
    }

    @Test
    void shouldNotCreateNotifyIntentForNeutralReview() {
        BookingOrderDO order = buildOrder(10032L, 302L, BookingOrderStatusEnum.COMPLETED.getStatus());
        bookingOrderMapper.insert(order);

        AppBookingReviewCreateReqVO reqVO = new AppBookingReviewCreateReqVO();
        reqVO.setBookingOrderId(order.getId());
        reqVO.setOverallScore(3);

        Long reviewId = bookingReviewService.createReview(302L, reqVO);

        BookingReviewDO actual = bookingReviewMapper.selectById(reviewId);
        assertNotNull(actual);
        assertEquals(BookingReviewLevelEnum.NEUTRAL.getLevel(), actual.getReviewLevel());
        verify(bookingReviewNotifyOutboxService, never()).createNegativeReviewCreatedOutbox(any());
    }

    @Test
    void shouldReturnEligibilityAndSummaryForMember() {
        BookingOrderDO completed = buildOrder(1004L, 40L, BookingOrderStatusEnum.COMPLETED.getStatus());
        bookingOrderMapper.insert(completed);
        bookingReviewMapper.insert(BookingReviewDO.builder()
                .bookingOrderId(2004L)
                .storeId(3001L)
                .technicianId(4001L)
                .memberId(40L)
                .serviceSpuId(5001L)
                .serviceSkuId(6001L)
                .overallScore(5)
                .reviewLevel(BookingReviewLevelEnum.POSITIVE.getLevel())
                .followStatus(BookingReviewFollowStatusEnum.NONE.getStatus())
                .completedTime(LocalDateTime.now().minusDays(2).withNano(0))
                .submitTime(LocalDateTime.now().minusDays(1).withNano(0))
                .build());
        bookingReviewMapper.insert(BookingReviewDO.builder()
                .bookingOrderId(2005L)
                .storeId(3001L)
                .technicianId(4002L)
                .memberId(40L)
                .serviceSpuId(5002L)
                .serviceSkuId(6002L)
                .overallScore(3)
                .reviewLevel(BookingReviewLevelEnum.NEUTRAL.getLevel())
                .followStatus(BookingReviewFollowStatusEnum.NONE.getStatus())
                .completedTime(LocalDateTime.now().minusDays(1).withNano(0))
                .submitTime(LocalDateTime.now().minusHours(10).withNano(0))
                .build());

        AppBookingReviewEligibilityRespVO eligibility = bookingReviewService.getEligibility(40L, completed.getId());
        AppBookingReviewSummaryRespVO summary = bookingReviewService.getSummary(40L);
        AppBookingReviewPageReqVO pageReqVO = new AppBookingReviewPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        PageResult<BookingReviewDO> pageResult = bookingReviewService.getReviewPage(40L, pageReqVO);

        assertTrue(Boolean.TRUE.equals(eligibility.getEligible()));
        assertFalse(Boolean.TRUE.equals(eligibility.getAlreadyReviewed()));
        assertEquals(2L, summary.getTotalCount());
        assertEquals(1L, summary.getPositiveCount());
        assertEquals(1L, summary.getNeutralCount());
        assertEquals(0L, summary.getNegativeCount());
        assertEquals(2L, pageResult.getTotal());
        assertEquals(2, pageResult.getList().size());
    }

    @Test
    void shouldReplyReviewAndRecordFirstResponse() {
        BookingReviewDO review = BookingReviewDO.builder()
                .bookingOrderId(3001L)
                .storeId(4001L)
                .technicianId(5001L)
                .memberId(60L)
                .overallScore(2)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .followStatus(BookingReviewFollowStatusEnum.PENDING.getStatus())
                .replyStatus(Boolean.FALSE)
                .submitTime(LocalDateTime.now().minusHours(1).withNano(0))
                .build();
        bookingReviewMapper.insert(review);

        bookingReviewService.replyReview(review.getId(), 9001L, "店长已电话回访并致歉");

        BookingReviewDO actual = bookingReviewMapper.selectById(review.getId());
        assertNotNull(actual);
        assertTrue(Boolean.TRUE.equals(actual.getReplyStatus()));
        assertEquals(9001L, actual.getReplyUserId());
        assertEquals("店长已电话回访并致歉", actual.getReplyContent());
        assertNotNull(actual.getReplyTime());
        assertNotNull(actual.getFirstResponseAt());
    }

    @Test
    void shouldUpdateFollowStatusAndOwnerForNegativeReview() {
        BookingReviewDO review = BookingReviewDO.builder()
                .bookingOrderId(3002L)
                .storeId(4002L)
                .technicianId(5002L)
                .memberId(61L)
                .overallScore(1)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .followStatus(BookingReviewFollowStatusEnum.PENDING.getStatus())
                .submitTime(LocalDateTime.now().minusMinutes(30).withNano(0))
                .build();
        bookingReviewMapper.insert(review);
        BookingReviewFollowUpdateReqVO reqVO = new BookingReviewFollowUpdateReqVO();
        reqVO.setReviewId(review.getId());
        reqVO.setFollowStatus(BookingReviewFollowStatusEnum.PROCESSING.getStatus());
        reqVO.setFollowResult("已分派门店店长跟进");

        bookingReviewService.updateFollowStatus(review.getId(), 9002L, reqVO);

        BookingReviewDO actual = bookingReviewMapper.selectById(review.getId());
        assertNotNull(actual);
        assertEquals(BookingReviewFollowStatusEnum.PROCESSING.getStatus(), actual.getFollowStatus());
        assertEquals(9002L, actual.getFollowOwnerId());
        assertEquals("已分派门店店长跟进", actual.getFollowResult());
    }

    @Test
    void shouldClaimAndCloseManagerTodoForNegativeReview() {
        BookingReviewDO review = BookingReviewDO.builder()
                .bookingOrderId(3012L)
                .storeId(4012L)
                .technicianId(5012L)
                .memberId(611L)
                .overallScore(1)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .managerTodoStatus(1)
                .managerContactName("王店长")
                .managerContactMobile("13900000000")
                .submitTime(LocalDateTime.now().minusMinutes(5).withNano(0))
                .build();
        bookingReviewMapper.insert(review);

        bookingReviewService.claimManagerTodo(review.getId(), 9101L);
        bookingReviewService.recordManagerFirstAction(review.getId(), 9101L, "已电话同步门店店长");
        bookingReviewService.closeManagerTodo(review.getId(), 9101L, "店长确认完成回访");

        BookingReviewDO actual = bookingReviewMapper.selectById(review.getId());
        assertNotNull(actual);
        assertEquals(4, actual.getManagerTodoStatus());
        assertEquals(9101L, actual.getManagerClaimedByUserId());
        assertNotNull(actual.getManagerClaimedAt());
        assertNotNull(actual.getManagerFirstActionAt());
        assertNotNull(actual.getManagerClosedAt());
        assertEquals("店长确认完成回访", actual.getManagerLatestActionRemark());
        assertEquals(9101L, actual.getManagerLatestActionByUserId());
    }

    @Test
    void shouldRejectInvalidManagerTodoTransitions() {
        BookingReviewDO review = BookingReviewDO.builder()
                .bookingOrderId(3013L)
                .storeId(4013L)
                .technicianId(5013L)
                .memberId(612L)
                .overallScore(1)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .managerTodoStatus(BookingReviewManagerTodoStatusEnum.PENDING_CLAIM.getStatus())
                .submitTime(LocalDateTime.now().minusMinutes(8).withNano(0))
                .build();
        bookingReviewMapper.insert(review);

        assertServiceException(() ->
                bookingReviewService.recordManagerFirstAction(review.getId(), 9201L, "越权首响"), BOOKING_REVIEW_NOT_ELIGIBLE);

        bookingReviewService.claimManagerTodo(review.getId(), 9201L);
        bookingReviewService.closeManagerTodo(review.getId(), 9201L, "已闭环");

        assertServiceException(() ->
                bookingReviewService.claimManagerTodo(review.getId(), 9202L), BOOKING_REVIEW_NOT_ELIGIBLE);
    }

    @Test
    void shouldLazyInitHistoricalNegativeManagerTodoWithoutOrder() {
        BookingReviewDO review = BookingReviewDO.builder()
                .bookingOrderId(999991L)
                .storeId(4991L)
                .technicianId(5991L)
                .memberId(6991L)
                .overallScore(1)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .submitTime(LocalDateTime.now().minusHours(2).withNano(0))
                .build();
        bookingReviewMapper.insert(review);

        bookingReviewService.claimManagerTodo(review.getId(), 9301L);

        BookingReviewDO actual = bookingReviewMapper.selectById(review.getId());
        assertNotNull(actual);
        assertEquals(BookingReviewManagerTodoStatusEnum.CLAIMED.getStatus(), actual.getManagerTodoStatus());
        assertEquals("REVIEW_LEVEL_NEGATIVE", actual.getNegativeTriggerType());
        assertNull(actual.getManagerContactName());
        assertNull(actual.getManagerContactMobile());
        assertNotNull(actual.getManagerClaimDeadlineAt());
        assertNotNull(actual.getManagerFirstActionDeadlineAt());
        assertNotNull(actual.getManagerCloseDeadlineAt());
        assertNotNull(actual.getManagerClaimedAt());
        assertEquals(9301L, actual.getManagerClaimedByUserId());
    }

    @Test
    void shouldNotBackfillHistoricalNegativeManagerTodoOnReadPath() {
        BookingReviewDO review = BookingReviewDO.builder()
                .bookingOrderId(999992L)
                .storeId(4992L)
                .technicianId(5992L)
                .memberId(6992L)
                .overallScore(1)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .riskLevel(2)
                .followStatus(BookingReviewFollowStatusEnum.PENDING.getStatus())
                .replyStatus(Boolean.FALSE)
                .submitTime(LocalDateTime.now().minusHours(3).withNano(0))
                .build();
        bookingReviewMapper.insert(review);

        BookingReviewPageReqVO pageReqVO = new BookingReviewPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);

        PageResult<BookingReviewDO> pageResult = bookingReviewService.getAdminReviewPage(pageReqVO);
        BookingReviewDashboardRespVO dashboard = bookingReviewService.getDashboardSummary();
        BookingReviewDO actual = bookingReviewMapper.selectById(review.getId());

        assertTrue(pageResult.getList().stream().anyMatch(item -> item.getId().equals(review.getId())));
        assertNotNull(actual);
        assertNull(actual.getManagerTodoStatus());
        assertNull(actual.getNegativeTriggerType());
        assertEquals(0L, dashboard.getManagerTodoPendingCount());
        assertEquals(0L, dashboard.getManagerTodoClaimTimeoutCount());
        assertEquals(0L, dashboard.getManagerTodoFirstActionTimeoutCount());
        assertEquals(0L, dashboard.getManagerTodoCloseTimeoutCount());
        assertEquals(0L, dashboard.getManagerTodoClosedCount());
    }

    @Test
    void shouldReturnAdminPageAndDashboardSummary() {
        bookingReviewMapper.insert(BookingReviewDO.builder()
                .bookingOrderId(3003L)
                .storeId(4003L)
                .technicianId(5003L)
                .memberId(62L)
                .overallScore(1)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .riskLevel(2)
                .followStatus(BookingReviewFollowStatusEnum.PENDING.getStatus())
                .replyStatus(Boolean.FALSE)
                .submitTime(LocalDateTime.now().minusHours(2).withNano(0))
                .build());
        bookingReviewMapper.insert(BookingReviewDO.builder()
                .bookingOrderId(3004L)
                .storeId(4004L)
                .technicianId(5004L)
                .memberId(63L)
                .overallScore(4)
                .reviewLevel(BookingReviewLevelEnum.POSITIVE.getLevel())
                .riskLevel(0)
                .followStatus(BookingReviewFollowStatusEnum.NONE.getStatus())
                .replyStatus(Boolean.TRUE)
                .submitTime(LocalDateTime.now().minusHours(1).withNano(0))
                .replyTime(LocalDateTime.now().minusMinutes(20).withNano(0))
                .build());

        BookingReviewPageReqVO pageReqVO = new BookingReviewPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setRiskLevel(2);
        PageResult<BookingReviewDO> pageResult = bookingReviewService.getAdminReviewPage(pageReqVO);
        BookingReviewDashboardRespVO summary = bookingReviewService.getDashboardSummary();

        assertEquals(1L, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertEquals(2L, summary.getTotalCount());
        assertEquals(1L, summary.getNegativeCount());
        assertEquals(1L, summary.getPendingFollowCount());
        assertEquals(1L, summary.getUrgentCount());
        assertEquals(1L, summary.getRepliedCount());
    }

    @Test
    void shouldScanHistoricalNegativeCandidatesWithoutBackfillingTodoFields() {
        BookingOrderDO manualReadyOrder = buildOrder(4010L, 8010L, BookingOrderStatusEnum.COMPLETED.getStatus());
        manualReadyOrder.setStoreId(9010L);
        bookingOrderMapper.insert(manualReadyOrder);
        BookingReviewDO manualReadyReview = BookingReviewDO.builder()
                .bookingOrderId(manualReadyOrder.getId())
                .storeId(999910L)
                .technicianId(5010L)
                .memberId(6010L)
                .overallScore(1)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .submitTime(LocalDateTime.now().minusDays(2).withNano(0))
                .build();
        bookingReviewMapper.insert(manualReadyReview);

        BookingOrderDO highRiskOrder = buildOrder(4011L, 8011L, BookingOrderStatusEnum.COMPLETED.getStatus());
        highRiskOrder.setStoreId(9011L);
        bookingOrderMapper.insert(highRiskOrder);
        BookingReviewDO highRiskReview = BookingReviewDO.builder()
                .bookingOrderId(highRiskOrder.getId())
                .storeId(9011L)
                .technicianId(5011L)
                .memberId(6011L)
                .overallScore(1)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .submitTime(LocalDateTime.now().minusDays(1).withNano(0))
                .build();
        bookingReviewMapper.insert(highRiskReview);

        BookingReviewDO outOfScopeReview = BookingReviewDO.builder()
                .bookingOrderId(4012L)
                .storeId(9012L)
                .technicianId(5012L)
                .memberId(6012L)
                .overallScore(1)
                .reviewLevel(BookingReviewLevelEnum.NEGATIVE.getLevel())
                .managerTodoStatus(BookingReviewManagerTodoStatusEnum.PENDING_CLAIM.getStatus())
                .submitTime(LocalDateTime.now().minusHours(12).withNano(0))
                .build();
        bookingReviewMapper.insert(outOfScopeReview);

        when(productStoreService.getStore(eq(9010L))).thenReturn(ProductStoreDO.builder()
                .id(9010L)
                .contactName("李店长")
                .contactMobile("13800000010")
                .build());
        when(productStoreService.getStore(eq(9011L))).thenReturn(ProductStoreDO.builder()
                .id(9011L)
                .contactName(null)
                .contactMobile("13800000011")
                .build());

        BookingReviewHistoryScanReqVO reqVO = new BookingReviewHistoryScanReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        BookingReviewHistoryScanRespVO result = bookingReviewService.scanAdminHistoryCandidates(reqVO);

        assertNotNull(result);
        assertNotNull(result.getSummary());
        assertEquals(3L, result.getSummary().getScannedCount());
        assertEquals(1L, result.getSummary().getManualReadyCount());
        assertEquals(1L, result.getSummary().getHighRiskCount());
        assertEquals(1L, result.getSummary().getOutOfScopeCount());
        assertEquals(3L, result.getTotal());

        Map<Long, BookingReviewHistoryScanItemRespVO> itemMap = result.getList().stream()
                .collect(java.util.stream.Collectors.toMap(BookingReviewHistoryScanItemRespVO::getReviewId, item -> item));
        assertEquals("MANUAL_READY", itemMap.get(manualReadyReview.getId()).getRiskCategory());
        assertEquals("HIGH_RISK", itemMap.get(highRiskReview.getId()).getRiskCategory());
        assertEquals("OUT_OF_SCOPE", itemMap.get(outOfScopeReview.getId()).getRiskCategory());
        assertTrue(itemMap.get(highRiskReview.getId()).getRiskReasons().contains("门店联系人缺失"));

        BookingReviewDO manualReadyAfterScan = bookingReviewMapper.selectById(manualReadyReview.getId());
        BookingReviewDO highRiskAfterScan = bookingReviewMapper.selectById(highRiskReview.getId());
        assertNull(manualReadyAfterScan.getManagerTodoStatus());
        assertNull(highRiskAfterScan.getManagerTodoStatus());
        assertNull(manualReadyAfterScan.getNegativeTriggerType());
        assertNull(highRiskAfterScan.getNegativeTriggerType());
    }

    private BookingOrderDO buildOrder(Long id, Long userId, Integer status) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        return BookingOrderDO.builder()
                .id(id)
                .orderNo("BK-REVIEW-" + id)
                .userId(userId)
                .storeId(3001L)
                .technicianId(4001L)
                .timeSlotId(5001L)
                .spuId(6001L)
                .skuId(7001L)
                .serviceName("肩颈调理")
                .servicePic("https://example.com/service-cover.png")
                .bookingDate(LocalDate.now().minusDays(1))
                .bookingStartTime(LocalTime.of(10, 0))
                .bookingEndTime(LocalTime.of(11, 0))
                .duration(60)
                .originalPrice(19900)
                .discountPrice(0)
                .payPrice(19900)
                .isOffpeak(Boolean.FALSE)
                .status(status)
                .payTime(now.minusHours(2))
                .serviceStartTime(now.minusHours(1))
                .serviceEndTime(now.minusMinutes(10))
                .userRemark("请关注颈部酸痛")
                .dispatchMode(1)
                .isAddon(0)
                .build();
    }
}
