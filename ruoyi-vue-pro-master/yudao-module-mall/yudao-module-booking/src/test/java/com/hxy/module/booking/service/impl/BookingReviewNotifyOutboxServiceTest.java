package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import com.hxy.module.booking.dal.mysql.BookingReviewManagerAccountRoutingMapper;
import com.hxy.module.booking.dal.mysql.BookingReviewNotifyOutboxMapper;
import com.hxy.module.booking.enums.BookingReviewLevelEnum;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import com.hxy.module.booking.service.BookingReviewWecomRobotSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOTIFY_OUTBOX_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOTIFY_OUTBOX_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @MockBean
    private BookingReviewWecomRobotSender bookingReviewWecomRobotSender;

    @Resource
    private BookingReviewNotifyOutboxService bookingReviewNotifyOutboxService;

    @Resource
    private BookingReviewNotifyOutboxMapper bookingReviewNotifyOutboxMapper;

    @Resource
    private BookingReviewManagerAccountRoutingMapper bookingReviewManagerAccountRoutingMapper;

    @Test
    void shouldCreateTwoPendingOutboxWhenAppAndWecomBound() {
        insertRouting(3001L, 9001L, "wecom-manager-9001", "ACTIVE");
        BookingReviewDO review = buildReview(2001L, 3001L, BookingReviewLevelEnum.NEGATIVE.getLevel());

        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);

        List<BookingReviewNotifyOutboxDO> list = sortByChannel(bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId()));
        assertEquals(2, list.size());

        BookingReviewNotifyOutboxDO inApp = list.get(0);
        assertEquals("IN_APP", inApp.getChannel());
        assertEquals("PENDING", inApp.getStatus());
        assertEquals(9001L, inApp.getReceiverUserId());
        assertEquals("ADMIN#9001", inApp.getReceiverAccount());

        BookingReviewNotifyOutboxDO wecom = list.get(1);
        assertEquals("WECOM", wecom.getChannel());
        assertEquals("PENDING", wecom.getStatus());
        assertNull(wecom.getReceiverUserId());
        assertEquals("wecom-manager-9001", wecom.getReceiverAccount());
    }

    @Test
    void shouldCreateAppPendingAndWecomBlockedWhenWecomMissing() {
        insertRouting(3002L, 9002L, null, "ACTIVE");
        BookingReviewDO review = buildReview(2002L, 3002L, BookingReviewLevelEnum.NEGATIVE.getLevel());

        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);

        List<BookingReviewNotifyOutboxDO> list = sortByChannel(bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId()));
        assertEquals(2, list.size());
        assertEquals("PENDING", list.get(0).getStatus());
        assertEquals("IN_APP", list.get(0).getChannel());
        assertEquals("BLOCKED_NO_OWNER", list.get(1).getStatus());
        assertEquals("WECOM", list.get(1).getChannel());
        assertTrue(list.get(1).getLastErrorMsg().contains("NO_WECOM_ACCOUNT"));
    }

    @Test
    void shouldCreateTwoBlockedOutboxWhenRouteMissing() {
        BookingReviewDO review = buildReview(2003L, 3003L, BookingReviewLevelEnum.NEGATIVE.getLevel());

        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);

        List<BookingReviewNotifyOutboxDO> list = sortByChannel(bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId()));
        assertEquals(2, list.size());
        assertEquals("BLOCKED_NO_OWNER", list.get(0).getStatus());
        assertEquals("BLOCKED_NO_OWNER", list.get(1).getStatus());
        assertTrue(list.get(0).getLastErrorMsg().contains("NO_OWNER"));
        assertTrue(list.get(1).getLastErrorMsg().contains("NO_OWNER"));
    }

    @Test
    void shouldSkipPositiveAndNeutralReview() {
        BookingReviewDO positive = buildReview(2004L, 3004L, BookingReviewLevelEnum.POSITIVE.getLevel());
        BookingReviewDO neutral = buildReview(2005L, 3004L, BookingReviewLevelEnum.NEUTRAL.getLevel());

        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(positive);
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(neutral);

        assertTrue(bookingReviewNotifyOutboxMapper.selectListByBizId(positive.getId()).isEmpty());
        assertTrue(bookingReviewNotifyOutboxMapper.selectListByBizId(neutral.getId()).isEmpty());
    }

    @Test
    void shouldKeepIdempotentWhenCreateSameNegativeReviewTwice() {
        insertRouting(3005L, 9005L, "wecom-manager-9005", "ACTIVE");
        BookingReviewDO review = buildReview(2006L, 3005L, BookingReviewLevelEnum.NEGATIVE.getLevel());

        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);

        List<BookingReviewNotifyOutboxDO> list = sortByChannel(bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId()));
        assertEquals(2, list.size());
        assertEquals("booking_review:negative_created:2006:IN_APP:9005", list.get(0).getIdempotencyKey());
        assertEquals("booking_review:negative_created:2006:WECOM:wecom-manager-9005", list.get(1).getIdempotencyKey());
    }

    @Test
    void shouldDispatchPendingOutboxToSentForBothChannels() {
        insertRouting(3006L, 9006L, "wecom-manager-9006", "ACTIVE");
        BookingReviewDO review = buildReview(2007L, 3006L, BookingReviewLevelEnum.NEGATIVE.getLevel());
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        when(notifySendService.sendSingleNotifyToAdmin(eq(9006L), eq("hxy_booking_review_negative_created"), anyMap()))
                .thenReturn(91001L);
        when(bookingReviewWecomRobotSender.send(eq("wecom-manager-9006"), eq("NEGATIVE_REVIEW_CREATED"), anyMap()))
                .thenReturn("WECOM#91002");

        bookingReviewNotifyOutboxService.dispatchPendingNotifyOutbox(20);

        List<BookingReviewNotifyOutboxDO> list = sortByChannel(bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId()));
        assertEquals("SENT", list.get(0).getStatus());
        assertEquals("SENT", list.get(1).getStatus());
        assertEquals("DISPATCH_SUCCESS", list.get(0).getLastActionCode());
        assertEquals("DISPATCH_SUCCESS", list.get(1).getLastActionCode());
        verify(notifySendService).sendSingleNotifyToAdmin(eq(9006L), eq("hxy_booking_review_negative_created"), anyMap());
        verify(bookingReviewWecomRobotSender).send(eq("wecom-manager-9006"), eq("NEGATIVE_REVIEW_CREATED"), anyMap());
    }

    @Test
    void shouldMarkWecomOutboxBlockedWhenChannelDisabled() {
        insertRouting(3007L, 9007L, "wecom-manager-9007", "ACTIVE");
        BookingReviewDO review = buildReview(2008L, 3007L, BookingReviewLevelEnum.NEGATIVE.getLevel());
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        when(notifySendService.sendSingleNotifyToAdmin(eq(9007L), eq("hxy_booking_review_negative_created"), anyMap()))
                .thenReturn(91007L);
        when(bookingReviewWecomRobotSender.send(eq("wecom-manager-9007"), eq("NEGATIVE_REVIEW_CREATED"), anyMap()))
                .thenThrow(new BookingReviewNotifyChannelBlockedException("CHANNEL_DISABLED"));

        bookingReviewNotifyOutboxService.dispatchPendingNotifyOutbox(20);

        List<BookingReviewNotifyOutboxDO> list = sortByChannel(bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId()));
        assertEquals("SENT", list.get(0).getStatus());
        assertEquals("BLOCKED_NO_OWNER", list.get(1).getStatus());
        assertTrue(list.get(1).getLastErrorMsg().contains("CHANNEL_DISABLED"));
    }

    @Test
    void shouldMarkWecomOutboxFailedWhenWecomDispatchThrows() {
        insertRouting(3008L, 9008L, "wecom-manager-9008", "ACTIVE");
        BookingReviewDO review = buildReview(2009L, 3008L, BookingReviewLevelEnum.NEGATIVE.getLevel());
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        when(notifySendService.sendSingleNotifyToAdmin(eq(9008L), eq("hxy_booking_review_negative_created"), anyMap()))
                .thenReturn(91008L);
        when(bookingReviewWecomRobotSender.send(eq("wecom-manager-9008"), eq("NEGATIVE_REVIEW_CREATED"), anyMap()))
                .thenThrow(new RuntimeException("wecom-send-failed"));

        bookingReviewNotifyOutboxService.dispatchPendingNotifyOutbox(20);

        List<BookingReviewNotifyOutboxDO> list = sortByChannel(bookingReviewNotifyOutboxMapper.selectListByBizId(review.getId()));
        assertEquals("SENT", list.get(0).getStatus());
        assertEquals("FAILED", list.get(1).getStatus());
        assertEquals(1, list.get(1).getRetryCount());
        assertTrue(list.get(1).getLastErrorMsg().contains("wecom-send-failed"));
    }

    @Test
    void shouldNotRedispatchAlreadySentOutbox() {
        insertRouting(3009L, 9009L, "wecom-manager-9009", "ACTIVE");
        BookingReviewDO review = buildReview(2010L, 3009L, BookingReviewLevelEnum.NEGATIVE.getLevel());
        bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        when(notifySendService.sendSingleNotifyToAdmin(eq(9009L), eq("hxy_booking_review_negative_created"), anyMap()))
                .thenReturn(91009L);
        when(bookingReviewWecomRobotSender.send(eq("wecom-manager-9009"), eq("NEGATIVE_REVIEW_CREATED"), anyMap()))
                .thenReturn("WECOM#91009");

        bookingReviewNotifyOutboxService.dispatchPendingNotifyOutbox(20);
        bookingReviewNotifyOutboxService.dispatchPendingNotifyOutbox(20);

        verify(notifySendService, times(1))
                .sendSingleNotifyToAdmin(eq(9009L), eq("hxy_booking_review_negative_created"), anyMap());
        verify(bookingReviewWecomRobotSender, times(1))
                .send(eq("wecom-manager-9009"), eq("NEGATIVE_REVIEW_CREATED"), anyMap());
    }

    @Test
    void shouldRetryFailedOutboxToPending() {
        Long outboxId = insertOutbox("FAILED", "WECOM", null, "wecom-manager-9010", 2, "dispatch-failed");

        int count = bookingReviewNotifyOutboxService.retryNotifyOutbox(List.of(outboxId), 9901L, "ops-retry");

        assertEquals(1, count);
        BookingReviewNotifyOutboxDO actual = bookingReviewNotifyOutboxMapper.selectById(outboxId);
        assertEquals("PENDING", actual.getStatus());
        assertEquals(2, actual.getRetryCount());
        assertNotNull(actual.getNextRetryTime());
        assertNull(actual.getSentTime());
        assertEquals("manual-retry:ops-retry", actual.getLastErrorMsg());
        assertEquals("MANUAL_RETRY", actual.getLastActionCode());
        assertNotNull(actual.getLastActionBizNo());
        assertNotNull(actual.getLastActionTime());
    }

    @Test
    void shouldThrowWhenRetryOutboxNotExists() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> bookingReviewNotifyOutboxService.retryNotifyOutbox(List.of(999999L), "manual-retry"));
        assertEquals(BOOKING_REVIEW_NOTIFY_OUTBOX_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void shouldThrowWhenRetrySentOutbox() {
        Long outboxId = insertOutbox("SENT", "IN_APP", 9011L, "ADMIN#9011", 1, "");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> bookingReviewNotifyOutboxService.retryNotifyOutbox(List.of(outboxId), "manual-retry"));
        assertEquals(BOOKING_REVIEW_NOTIFY_OUTBOX_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void shouldThrowWhenRetryBlockedOutbox() {
        Long outboxId = insertOutbox("BLOCKED_NO_OWNER", "WECOM", null, "wecom-manager-9012", 0,
                "BLOCKED_NO_OWNER:NO_WECOM_ACCOUNT");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> bookingReviewNotifyOutboxService.retryNotifyOutbox(List.of(outboxId), "manual-retry"));
        assertEquals(BOOKING_REVIEW_NOTIFY_OUTBOX_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void shouldBuildNotifyOutboxSummaryByReview() {
        Long scopedStoreId = 3601L;
        BookingReviewNotifyOutboxDO sentInApp = bookingReviewNotifyOutboxMapper.selectById(
                insertOutbox("SENT", "IN_APP", 9101L, "ADMIN#9101", 0, ""));
        sentInApp.setBizId(6001L);
        sentInApp.setStoreId(scopedStoreId);
        bookingReviewNotifyOutboxMapper.updateById(sentInApp);
        BookingReviewNotifyOutboxDO sentWecom = bookingReviewNotifyOutboxMapper.selectById(
                insertOutbox("SENT", "WECOM", null, "wecom-manager-9101", 0, ""));
        sentWecom.setBizId(6001L);
        sentWecom.setStoreId(scopedStoreId);
        bookingReviewNotifyOutboxMapper.updateById(sentWecom);

        BookingReviewNotifyOutboxDO blockedInApp = bookingReviewNotifyOutboxMapper.selectById(
                insertOutbox("BLOCKED_NO_OWNER", "IN_APP", null, null, 0, "BLOCKED_NO_OWNER:NO_APP_ACCOUNT"));
        blockedInApp.setBizId(6002L);
        blockedInApp.setStoreId(scopedStoreId);
        bookingReviewNotifyOutboxMapper.updateById(blockedInApp);
        BookingReviewNotifyOutboxDO blockedWecom = bookingReviewNotifyOutboxMapper.selectById(
                insertOutbox("FAILED", "WECOM", null, "wecom-manager-9102", 1, "dispatch-failed"));
        blockedWecom.setBizId(6002L);
        blockedWecom.setStoreId(scopedStoreId);
        blockedWecom.setLastActionCode("MANUAL_RETRY");
        blockedWecom.setStatus("PENDING");
        bookingReviewNotifyOutboxMapper.updateById(blockedWecom);

        BookingReviewNotifyOutboxPageReqVO reqVO = new BookingReviewNotifyOutboxPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setStoreId(scopedStoreId);

        BookingReviewNotifyOutboxSummaryRespVO summary = bookingReviewNotifyOutboxService.getNotifyOutboxSummary(reqVO);

        assertEquals(2L, summary.getTotalReviewCount());
        assertEquals(1L, summary.getDualSentReviewCount());
        assertEquals(1L, summary.getBlockedReviewCount());
        assertEquals(0L, summary.getFailedReviewCount());
        assertEquals(1L, summary.getManualRetryPendingReviewCount());
        assertEquals(1L, summary.getDivergedReviewCount());
    }

    private void insertRouting(Long storeId, Long managerAdminUserId, String managerWecomUserId, String bindingStatus) {
        bookingReviewManagerAccountRoutingMapper.insert(new BookingReviewManagerAccountRoutingDO()
                .setStoreId(storeId)
                .setManagerAdminUserId(managerAdminUserId)
                .setManagerWecomUserId(managerWecomUserId)
                .setBindingStatus(bindingStatus)
                .setEffectiveTime(LocalDateTime.now().minusDays(1).withNano(0))
                .setExpireTime(LocalDateTime.now().plusDays(1).withNano(0))
                .setSource("MANUAL_BIND")
                .setLastVerifiedTime(LocalDateTime.now().withNano(0)));
    }

    private Long insertOutbox(String status, String channel, Long receiverUserId, String receiverAccount,
                              Integer retryCount, String lastErrorMsg) {
        BookingReviewNotifyOutboxDO outbox = new BookingReviewNotifyOutboxDO()
                .setBizType("BOOKING_REVIEW_NEGATIVE")
                .setBizId(5000L + System.nanoTime() % 1000)
                .setStoreId(3100L)
                .setReceiverRole("STORE_MANAGER")
                .setReceiverUserId(receiverUserId)
                .setReceiverAccount(receiverAccount)
                .setNotifyType("NEGATIVE_REVIEW_CREATED")
                .setChannel(channel)
                .setStatus(status)
                .setRetryCount(retryCount)
                .setNextRetryTime(LocalDateTime.now().minusMinutes(10).withNano(0))
                .setSentTime("SENT".equals(status) ? LocalDateTime.now().minusMinutes(1).withNano(0) : null)
                .setLastErrorMsg(lastErrorMsg)
                .setIdempotencyKey("retry-test:" + status + ":" + channel + ":" + System.nanoTime())
                .setPayloadSnapshot("{\"reviewId\":5001}")
                .setLastActionCode("FAILED".equals(status) ? "DISPATCH_FAILED" : "CREATE_OUTBOX")
                .setLastActionBizNo("OUTBOX-RETRY-TEST")
                .setLastActionTime(LocalDateTime.now().minusMinutes(5).withNano(0));
        bookingReviewNotifyOutboxMapper.insert(outbox);
        return outbox.getId();
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

    private List<BookingReviewNotifyOutboxDO> sortByChannel(List<BookingReviewNotifyOutboxDO> list) {
        list.sort(Comparator.comparing(BookingReviewNotifyOutboxDO::getChannel));
        return list;
    }
}
