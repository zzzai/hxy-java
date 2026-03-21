package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import com.hxy.module.booking.dal.mysql.BookingReviewManagerAccountRoutingMapper;
import com.hxy.module.booking.dal.mysql.BookingReviewNotifyOutboxMapper;
import com.hxy.module.booking.enums.BookingReviewLevelEnum;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(BookingReviewNotifyOutboxServiceImpl.class)
class BookingReviewNotifyOutboxServiceTest extends BaseDbUnitTest {

    @MockBean
    private NotifySendService notifySendService;

    @Resource
    private BookingReviewNotifyOutboxService bookingReviewNotifyOutboxService;

    @Resource
    private BookingReviewNotifyOutboxMapper bookingReviewNotifyOutboxMapper;

    @Resource
    private BookingReviewManagerAccountRoutingMapper bookingReviewManagerAccountRoutingMapper;

    @Test
    void shouldCreatePendingOutboxWhenStoreManagerBound() {
        insertRouting(3001L, 9001L, "ACTIVE");
        BookingReviewDO review = buildReview(2001L, 3001L, BookingReviewLevelEnum.NEGATIVE.getLevel());

        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);

        List<BookingReviewNotifyOutboxDO> list = bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId());
        assertEquals(1, list.size());
        BookingReviewNotifyOutboxDO actual = list.get(0);
        assertEquals("PENDING", actual.getStatus());
        assertEquals(9001L, actual.getReceiverUserId());
        assertEquals("STORE_MANAGER", actual.getReceiverRole());
        assertEquals("NEGATIVE_REVIEW_CREATED", actual.getNotifyType());
        assertEquals("IN_APP", actual.getChannel());
    }

    @Test
    void shouldCreateBlockedOutboxWhenOwnerMissing() {
        BookingReviewDO review = buildReview(2002L, 3002L, BookingReviewLevelEnum.NEGATIVE.getLevel());

        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);

        List<BookingReviewNotifyOutboxDO> list = bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId());
        assertEquals(1, list.size());
        BookingReviewNotifyOutboxDO actual = list.get(0);
        assertEquals("BLOCKED_NO_OWNER", actual.getStatus());
        assertNull(actual.getReceiverUserId());
        assertTrue(actual.getLastErrorMsg().contains("NO_OWNER"));
    }

    @Test
    void shouldSkipPositiveAndNeutralReview() {
        BookingReviewDO positive = buildReview(2003L, 3003L, BookingReviewLevelEnum.POSITIVE.getLevel());
        BookingReviewDO neutral = buildReview(2004L, 3003L, BookingReviewLevelEnum.NEUTRAL.getLevel());

        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(positive);
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(neutral);

        assertTrue(bookingReviewNotifyOutboxMapper.selectListByBizId(positive.getId()).isEmpty());
        assertTrue(bookingReviewNotifyOutboxMapper.selectListByBizId(neutral.getId()).isEmpty());
    }

    @Test
    void shouldKeepIdempotentWhenCreateSameNegativeReviewTwice() {
        insertRouting(3004L, 9004L, "ACTIVE");
        BookingReviewDO review = buildReview(2005L, 3004L, BookingReviewLevelEnum.NEGATIVE.getLevel());

        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);

        List<BookingReviewNotifyOutboxDO> list = bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId());
        assertEquals(1, list.size());
        assertEquals("booking_review:negative_created:2005:9004", list.get(0).getIdempotencyKey());
    }

    @Test
    void shouldDispatchPendingOutboxToSent() {
        insertRouting(3005L, 9005L, "ACTIVE");
        BookingReviewDO review = buildReview(2006L, 3005L, BookingReviewLevelEnum.NEGATIVE.getLevel());
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        when(notifySendService.sendSingleNotifyToAdmin(eq(9005L), eq("hxy_booking_review_negative_created"), anyMap()))
                .thenReturn(91001L);

        bookingReviewNotifyOutboxService.dispatchPendingNotifyOutbox(20);

        BookingReviewNotifyOutboxDO actual = bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId()).get(0);
        assertEquals("SENT", actual.getStatus());
        assertEquals(0, actual.getRetryCount());
        assertNotNull(actual.getSentTime());
        assertEquals("DISPATCH_SUCCESS", actual.getLastActionCode());
        verify(notifySendService).sendSingleNotifyToAdmin(eq(9005L), eq("hxy_booking_review_negative_created"), anyMap());
    }

    @Test
    void shouldMarkOutboxFailedWhenDispatchThrows() {
        insertRouting(3006L, 9006L, "ACTIVE");
        BookingReviewDO review = buildReview(2007L, 3006L, BookingReviewLevelEnum.NEGATIVE.getLevel());
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        when(notifySendService.sendSingleNotifyToAdmin(eq(9006L), eq("hxy_booking_review_negative_created"), anyMap()))
                .thenThrow(new RuntimeException("mock-send-failed"));

        bookingReviewNotifyOutboxService.dispatchPendingNotifyOutbox(20);

        BookingReviewNotifyOutboxDO actual = bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId()).get(0);
        assertEquals("FAILED", actual.getStatus());
        assertEquals(1, actual.getRetryCount());
        assertNotNull(actual.getNextRetryTime());
        assertTrue(actual.getLastErrorMsg().contains("mock-send-failed"));
        assertEquals("DISPATCH_FAILED", actual.getLastActionCode());
    }

    @Test
    void shouldNotRedispatchAlreadySentOutbox() {
        insertRouting(3007L, 9007L, "ACTIVE");
        BookingReviewDO review = buildReview(2008L, 3007L, BookingReviewLevelEnum.NEGATIVE.getLevel());
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        when(notifySendService.sendSingleNotifyToAdmin(eq(9007L), eq("hxy_booking_review_negative_created"), anyMap()))
                .thenReturn(91007L);

        bookingReviewNotifyOutboxService.dispatchPendingNotifyOutbox(20);
        bookingReviewNotifyOutboxService.dispatchPendingNotifyOutbox(20);

        verify(notifySendService, times(1))
                .sendSingleNotifyToAdmin(eq(9007L), eq("hxy_booking_review_negative_created"), anyMap());
    }

    private void insertRouting(Long storeId, Long managerAdminUserId, String bindingStatus) {
        bookingReviewManagerAccountRoutingMapper.insert(new BookingReviewManagerAccountRoutingDO()
                .setStoreId(storeId)
                .setManagerAdminUserId(managerAdminUserId)
                .setBindingStatus(bindingStatus)
                .setEffectiveTime(LocalDateTime.now().minusDays(1).withNano(0))
                .setExpireTime(LocalDateTime.now().plusDays(1).withNano(0))
                .setSource("MANUAL_BIND")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0)));
    }

    private BookingReviewDO buildReview(Long reviewId, Long storeId, Integer reviewLevel) {
        return BookingReviewDO.builder()
                .id(reviewId)
                .bookingOrderId(7000L + reviewId)
                .storeId(storeId)
                .memberId(8000L + reviewId)
                .overallScore(1)
                .reviewLevel(reviewLevel)
                .submitTime(LocalDateTime.now().withNano(0))
                .build();
    }
}
