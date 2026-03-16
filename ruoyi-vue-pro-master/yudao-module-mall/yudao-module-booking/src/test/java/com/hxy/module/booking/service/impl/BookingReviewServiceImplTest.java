package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
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
import com.hxy.module.booking.service.BookingReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_ALREADY_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOT_ELIGIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(BookingReviewServiceImpl.class)
class BookingReviewServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BookingReviewService bookingReviewService;

    @Resource
    private BookingOrderMapper bookingOrderMapper;

    @Resource
    private BookingReviewMapper bookingReviewMapper;

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
        bookingOrderMapper.insert(order);

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
        assertEquals(order.getStoreId(), actual.getStoreId());
        assertEquals(order.getTechnicianId(), actual.getTechnicianId());
        assertEquals(order.getSpuId(), actual.getServiceSpuId());
        assertEquals(order.getSkuId(), actual.getServiceSkuId());
        assertEquals(reqVO.getTags(), actual.getTags());
        assertEquals(reqVO.getPicUrls(), actual.getPicUrls());
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
