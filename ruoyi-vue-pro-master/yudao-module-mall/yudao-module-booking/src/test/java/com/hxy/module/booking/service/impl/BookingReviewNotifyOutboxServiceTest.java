package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import com.hxy.module.booking.dal.mysql.BookingReviewManagerAccountRoutingMapper;
import com.hxy.module.booking.dal.mysql.BookingReviewNotifyOutboxMapper;
import com.hxy.module.booking.enums.BookingReviewLevelEnum;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(BookingReviewNotifyOutboxServiceImpl.class)
class BookingReviewNotifyOutboxServiceTest extends BaseDbUnitTest {

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
