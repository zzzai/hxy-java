package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxPageReqVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import com.hxy.module.booking.dal.mysql.BookingReviewManagerAccountRoutingMapper;
import com.hxy.module.booking.dal.mysql.BookingReviewNotifyOutboxMapper;
import com.hxy.module.booking.enums.BookingReviewLevelEnum;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Validated
@Slf4j
public class BookingReviewNotifyOutboxServiceImpl implements BookingReviewNotifyOutboxService {

    private static final String ROUTING_STATUS_ACTIVE = "ACTIVE";
    private static final String OUTBOX_BIZ_TYPE = "BOOKING_REVIEW_NEGATIVE";
    private static final String RECEIVER_ROLE_STORE_MANAGER = "STORE_MANAGER";
    private static final String NOTIFY_TYPE_NEGATIVE_CREATED = "NEGATIVE_REVIEW_CREATED";
    private static final String CHANNEL_IN_APP = "IN_APP";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_BLOCKED_NO_OWNER = "BLOCKED_NO_OWNER";
    private static final String ACTION_CREATE = "CREATE_OUTBOX";
    private static final String ACTION_BLOCKED_NO_OWNER = "BLOCKED_NO_OWNER";
    private static final String ACTION_DISPATCH_SUCCESS = "DISPATCH_SUCCESS";
    private static final String ACTION_DISPATCH_FAILED = "DISPATCH_FAILED";
    private static final String ID_NO_OWNER = "NO_OWNER";
    private static final String TEMPLATE_NEGATIVE_REVIEW_CREATED = "hxy_booking_review_negative_created";
    private static final int DEFAULT_DISPATCH_LIMIT = 200;
    private static final int MAX_DISPATCH_LIMIT = 1000;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_BACKOFF_MINUTES = 5;

    private final BookingReviewNotifyOutboxMapper bookingReviewNotifyOutboxMapper;
    private final BookingReviewManagerAccountRoutingMapper bookingReviewManagerAccountRoutingMapper;
    @Resource
    private NotifySendService notifySendService;

    public BookingReviewNotifyOutboxServiceImpl(BookingReviewNotifyOutboxMapper bookingReviewNotifyOutboxMapper,
                                                BookingReviewManagerAccountRoutingMapper bookingReviewManagerAccountRoutingMapper) {
        this.bookingReviewNotifyOutboxMapper = bookingReviewNotifyOutboxMapper;
        this.bookingReviewManagerAccountRoutingMapper = bookingReviewManagerAccountRoutingMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void createNegativeReviewCreatedOutbox(BookingReviewDO review) {
        if (review == null || review.getId() == null || review.getStoreId() == null
                || !Objects.equals(review.getReviewLevel(), BookingReviewLevelEnum.NEGATIVE.getLevel())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now().withNano(0);
        BookingReviewManagerAccountRoutingDO routing = bookingReviewManagerAccountRoutingMapper
                .selectEffectiveByStoreId(review.getStoreId(), ROUTING_STATUS_ACTIVE, now);
        Long receiverUserId = routing == null ? null : routing.getManagerAdminUserId();
        String idempotencyKey = buildIdempotencyKey(review.getId(), receiverUserId);
        if (bookingReviewNotifyOutboxMapper.selectByIdempotencyKey(idempotencyKey) != null) {
            return;
        }

        BookingReviewNotifyOutboxDO outbox = new BookingReviewNotifyOutboxDO()
                .setBizType(OUTBOX_BIZ_TYPE)
                .setBizId(review.getId())
                .setStoreId(review.getStoreId())
                .setReceiverRole(RECEIVER_ROLE_STORE_MANAGER)
                .setReceiverUserId(receiverUserId)
                .setNotifyType(NOTIFY_TYPE_NEGATIVE_CREATED)
                .setChannel(CHANNEL_IN_APP)
                .setStatus(receiverUserId == null ? STATUS_BLOCKED_NO_OWNER : STATUS_PENDING)
                .setRetryCount(0)
                .setNextRetryTime(receiverUserId == null ? null : now)
                .setSentTime(null)
                .setLastErrorMsg(receiverUserId == null ? "BLOCKED_NO_OWNER:NO_OWNER" : "")
                .setIdempotencyKey(idempotencyKey)
                .setPayloadSnapshot(buildPayloadSnapshot(review))
                .setLastActionCode(receiverUserId == null ? ACTION_BLOCKED_NO_OWNER : ACTION_CREATE)
                .setLastActionBizNo(StrUtil.maxLength("REVIEW#" + review.getId(), 64))
                .setLastActionTime(now);
        try {
            bookingReviewNotifyOutboxMapper.insert(outbox);
        } catch (DuplicateKeyException ex) {
            log.info("booking review notify outbox idempotent hit, reviewId={}, idempotencyKey={}",
                    review.getId(), idempotencyKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int dispatchPendingNotifyOutbox(Integer limit) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        List<BookingReviewNotifyOutboxDO> dispatchableList = bookingReviewNotifyOutboxMapper
                .selectDispatchableList(now, resolveDispatchLimit(limit), MAX_RETRY_COUNT);
        if (dispatchableList.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        for (BookingReviewNotifyOutboxDO outbox : dispatchableList) {
            String currentStatus = StrUtil.blankToDefault(outbox.getStatus(), STATUS_PENDING);
            try {
                Long messageId = dispatchInAppNotify(outbox);
                int updated = bookingReviewNotifyOutboxMapper.updateByIdAndStatus(outbox.getId(), currentStatus,
                        new BookingReviewNotifyOutboxDO()
                                .setStatus(STATUS_SENT)
                                .setRetryCount(ObjUtil.defaultIfNull(outbox.getRetryCount(), 0))
                                .setNextRetryTime(null)
                                .setSentTime(now)
                                .setLastErrorMsg("")
                                .setLastActionCode(ACTION_DISPATCH_SUCCESS)
                                .setLastActionBizNo(buildDispatchBizNo(messageId, outbox.getId()))
                                .setLastActionTime(now));
                if (updated > 0) {
                    sentCount++;
                }
            } catch (Exception ex) {
                int nextRetryCount = ObjUtil.defaultIfNull(outbox.getRetryCount(), 0) + 1;
                bookingReviewNotifyOutboxMapper.updateByIdAndStatus(outbox.getId(), currentStatus,
                        new BookingReviewNotifyOutboxDO()
                                .setStatus(STATUS_FAILED)
                                .setRetryCount(nextRetryCount)
                                .setNextRetryTime(now.plusMinutes((long) RETRY_BACKOFF_MINUTES * nextRetryCount))
                                .setLastErrorMsg(StrUtil.maxLength(StrUtil.blankToDefault(ex.getMessage(),
                                        ex.getClass().getSimpleName()), 255))
                                .setLastActionCode(ACTION_DISPATCH_FAILED)
                                .setLastActionBizNo(buildDispatchBizNo(null, outbox.getId()))
                                .setLastActionTime(now));
            }
        }
        return sentCount;
    }

    @Override
    public List<BookingReviewNotifyOutboxDO> getNotifyOutboxList(Long reviewId, String status, Integer limit) {
        return bookingReviewNotifyOutboxMapper.selectListByReviewAndStatus(reviewId, status, limit);
    }

    @Override
    public PageResult<BookingReviewNotifyOutboxDO> getNotifyOutboxPage(BookingReviewNotifyOutboxPageReqVO reqVO) {
        return bookingReviewNotifyOutboxMapper.selectPage(reqVO);
    }

    private String buildIdempotencyKey(Long reviewId, Long receiverUserId) {
        return "booking_review:negative_created:" + reviewId + ":"
                + (receiverUserId == null ? ID_NO_OWNER : receiverUserId);
    }

    private String buildPayloadSnapshot(BookingReviewDO review) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", review.getId());
        payload.put("storeId", review.getStoreId());
        payload.put("bookingOrderId", review.getBookingOrderId());
        payload.put("reviewLevel", review.getReviewLevel());
        payload.put("memberId", review.getMemberId());
        payload.put("submitTime", review.getSubmitTime());
        return JsonUtils.toJsonString(payload);
    }

    private int resolveDispatchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_DISPATCH_LIMIT;
        }
        return Math.min(limit, MAX_DISPATCH_LIMIT);
    }

    private Long dispatchInAppNotify(BookingReviewNotifyOutboxDO outbox) {
        if (!Objects.equals(outbox.getChannel(), CHANNEL_IN_APP)) {
            throw new IllegalStateException("UNSUPPORTED_CHANNEL:" + outbox.getChannel());
        }
        if (outbox.getReceiverUserId() == null) {
            throw new IllegalStateException("MISSING_RECEIVER");
        }
        return notifySendService.sendSingleNotifyToAdmin(outbox.getReceiverUserId(),
                TEMPLATE_NEGATIVE_REVIEW_CREATED, buildNotifyParams(outbox));
    }

    private Map<String, Object> buildNotifyParams(BookingReviewNotifyOutboxDO outbox) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("reviewId", outbox.getBizId());
        params.put("storeId", outbox.getStoreId());
        params.put("notifyType", outbox.getNotifyType());
        params.put("receiverRole", outbox.getReceiverRole());
        return params;
    }

    private String buildDispatchBizNo(Long messageId, Long outboxId) {
        return messageId == null ? "OUTBOX#" + outboxId : "MSG#" + messageId;
    }
}
