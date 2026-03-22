package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import com.hxy.module.booking.dal.mysql.BookingReviewManagerAccountRoutingMapper;
import com.hxy.module.booking.dal.mysql.BookingReviewMapper;
import com.hxy.module.booking.dal.mysql.BookingReviewNotifyOutboxMapper;
import com.hxy.module.booking.enums.BookingReviewLevelEnum;
import com.hxy.module.booking.enums.BookingReviewManagerTodoStatusEnum;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import com.hxy.module.booking.service.BookingReviewWecomRobotSender;
import com.hxy.module.booking.service.support.BookingReviewAdminPrioritySupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOTIFY_OUTBOX_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOTIFY_OUTBOX_STATUS_INVALID;

@Service
@Validated
@Slf4j
public class BookingReviewNotifyOutboxServiceImpl implements BookingReviewNotifyOutboxService {

    private static final String ROUTING_STATUS_ACTIVE = "ACTIVE";
    private static final String OUTBOX_BIZ_TYPE = "BOOKING_REVIEW_NEGATIVE";
    private static final String RECEIVER_ROLE_STORE_MANAGER = "STORE_MANAGER";
    private static final String NOTIFY_TYPE_NEGATIVE_CREATED = "NEGATIVE_REVIEW_CREATED";
    private static final String NOTIFY_TYPE_MANAGER_CLAIM_TIMEOUT = "MANAGER_CLAIM_TIMEOUT";
    private static final String NOTIFY_TYPE_MANAGER_FIRST_ACTION_TIMEOUT = "MANAGER_FIRST_ACTION_TIMEOUT";
    private static final String NOTIFY_TYPE_MANAGER_CLOSE_TIMEOUT = "MANAGER_CLOSE_TIMEOUT";
    private static final String CHANNEL_IN_APP = "IN_APP";
    private static final String CHANNEL_WECOM = "WECOM";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_BLOCKED_NO_OWNER = "BLOCKED_NO_OWNER";
    private static final String ACTION_CREATE = "CREATE_OUTBOX";
    private static final String ACTION_BLOCKED_NO_OWNER = "BLOCKED_NO_OWNER";
    private static final String ACTION_DISPATCH_SUCCESS = "DISPATCH_SUCCESS";
    private static final String ACTION_DISPATCH_FAILED = "DISPATCH_FAILED";
    private static final String ACTION_DISPATCH_BLOCKED = "DISPATCH_BLOCKED";
    private static final String ACTION_MANUAL_RETRY = "MANUAL_RETRY";
    private static final String TEMPLATE_NEGATIVE_REVIEW_CREATED = "hxy_booking_review_negative_created";
    private static final String DEFAULT_RECEIVER_KEY = "NO_OWNER";
    private static final int DEFAULT_DISPATCH_LIMIT = 200;
    private static final int MAX_DISPATCH_LIMIT = 1000;
    private static final int DEFAULT_REMINDER_LIMIT = 200;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_BACKOFF_MINUTES = 5;

    private final BookingReviewNotifyOutboxMapper bookingReviewNotifyOutboxMapper;
    private final BookingReviewManagerAccountRoutingMapper bookingReviewManagerAccountRoutingMapper;
    private final BookingReviewMapper bookingReviewMapper;

    @Resource
    private NotifySendService notifySendService;
    @Resource
    private BookingReviewWecomRobotSender bookingReviewWecomRobotSender;

    public BookingReviewNotifyOutboxServiceImpl(BookingReviewNotifyOutboxMapper bookingReviewNotifyOutboxMapper,
                                                BookingReviewManagerAccountRoutingMapper bookingReviewManagerAccountRoutingMapper,
                                                BookingReviewMapper bookingReviewMapper) {
        this.bookingReviewNotifyOutboxMapper = bookingReviewNotifyOutboxMapper;
        this.bookingReviewManagerAccountRoutingMapper = bookingReviewManagerAccountRoutingMapper;
        this.bookingReviewMapper = bookingReviewMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void createNegativeReviewCreatedOutbox(BookingReviewDO review) {
        if (review == null || review.getId() == null || review.getStoreId() == null
                || !Objects.equals(review.getReviewLevel(), BookingReviewLevelEnum.NEGATIVE.getLevel())) {
            return;
        }
        createOutboxForNotifyType(review, NOTIFY_TYPE_NEGATIVE_CREATED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createManagerTodoSlaReminderOutbox(Integer limit) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        List<BookingReviewDO> candidates = bookingReviewMapper.selectManagerTodoSlaReminderCandidates(now,
                resolveReminderLimit(limit));
        if (candidates.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (BookingReviewDO review : candidates) {
            String notifyType = resolveSlaNotifyType(review, now);
            if (notifyType == null) {
                continue;
            }
            count += createOutboxForNotifyType(review, notifyType);
        }
        return count;
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
                String bizNo = dispatchNotify(outbox);
                int updated = bookingReviewNotifyOutboxMapper.updateByIdAndStatus(outbox.getId(), currentStatus,
                        new BookingReviewNotifyOutboxDO()
                                .setStatus(STATUS_SENT)
                                .setRetryCount(ObjUtil.defaultIfNull(outbox.getRetryCount(), 0))
                                .setNextRetryTime(null)
                                .setSentTime(now)
                                .setLastErrorMsg("")
                                .setLastActionCode(ACTION_DISPATCH_SUCCESS)
                                .setLastActionBizNo(buildDispatchBizNo(bizNo, outbox.getId()))
                                .setLastActionTime(now));
                if (updated > 0) {
                    sentCount++;
                }
            } catch (BookingReviewNotifyChannelBlockedException ex) {
                bookingReviewNotifyOutboxMapper.updateByIdAndStatus(outbox.getId(), currentStatus,
                        new BookingReviewNotifyOutboxDO()
                                .setStatus(STATUS_BLOCKED_NO_OWNER)
                                .setNextRetryTime(null)
                                .setLastErrorMsg("BLOCKED_NO_OWNER:" + ex.getReasonCode())
                                .setLastActionCode(ACTION_DISPATCH_BLOCKED)
                                .setLastActionBizNo("OUTBOX#" + outbox.getId())
                                .setLastActionTime(now));
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
                                .setLastActionBizNo("OUTBOX#" + outbox.getId())
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
    public List<BookingReviewNotifyOutboxDO> getNotifyOutboxListByReviewIds(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return Collections.emptyList();
        }
        return bookingReviewNotifyOutboxMapper.selectListByBizIds(reviewIds);
    }

    @Override
    public PageResult<BookingReviewNotifyOutboxDO> getNotifyOutboxPage(BookingReviewNotifyOutboxPageReqVO reqVO) {
        return bookingReviewNotifyOutboxMapper.selectPage(reqVO);
    }

    @Override
    public BookingReviewNotifyOutboxSummaryRespVO getNotifyOutboxSummary(BookingReviewNotifyOutboxPageReqVO reqVO) {
        List<BookingReviewNotifyOutboxDO> filteredList = bookingReviewNotifyOutboxMapper.selectList(reqVO);
        BookingReviewNotifyOutboxSummaryRespVO summary = new BookingReviewNotifyOutboxSummaryRespVO();
        if (filteredList.isEmpty()) {
            summary.setTotalReviewCount(0L);
            summary.setDualSentReviewCount(0L);
            summary.setBlockedReviewCount(0L);
            summary.setFailedReviewCount(0L);
            summary.setManualRetryPendingReviewCount(0L);
            summary.setDivergedReviewCount(0L);
            return summary;
        }
        Map<Long, List<BookingReviewNotifyOutboxDO>> reviewOutboxMap = filteredList.stream()
                .filter(outbox -> outbox != null && outbox.getBizId() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        BookingReviewNotifyOutboxDO::getBizId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
        List<BookingReviewAdminPrioritySupport.ReviewNotifyAuditSnapshot> snapshots = reviewOutboxMap.values().stream()
                .map(BookingReviewAdminPrioritySupport::resolveReviewNotifyAudit)
                .collect(java.util.stream.Collectors.toList());
        summary.setTotalReviewCount((long) reviewOutboxMap.size());
        summary.setDualSentReviewCount(snapshots.stream().filter(BookingReviewAdminPrioritySupport.ReviewNotifyAuditSnapshot::isDualSent).count());
        summary.setBlockedReviewCount(snapshots.stream().filter(BookingReviewAdminPrioritySupport.ReviewNotifyAuditSnapshot::isBlocked).count());
        summary.setFailedReviewCount(snapshots.stream().filter(BookingReviewAdminPrioritySupport.ReviewNotifyAuditSnapshot::isFailed).count());
        summary.setManualRetryPendingReviewCount(snapshots.stream().filter(BookingReviewAdminPrioritySupport.ReviewNotifyAuditSnapshot::isManualRetryPending).count());
        summary.setDivergedReviewCount(snapshots.stream().filter(BookingReviewAdminPrioritySupport.ReviewNotifyAuditSnapshot::isDiverged).count());
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int retryNotifyOutbox(List<Long> ids, Long operatorId, String reason) {
        List<Long> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return 0;
        }
        int affected = 0;
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String retryReason = StrUtil.maxLength(StrUtil.blankToDefault(reason, "manual-retry"), 120);
        for (Long id : normalizedIds) {
            BookingReviewNotifyOutboxDO outbox = bookingReviewNotifyOutboxMapper.selectById(id);
            if (outbox == null) {
                throw exception(BOOKING_REVIEW_NOTIFY_OUTBOX_NOT_EXISTS);
            }
            String currentStatus = StrUtil.blankToDefault(outbox.getStatus(), STATUS_PENDING);
            if (!Objects.equals(currentStatus, STATUS_FAILED)) {
                throw exception(BOOKING_REVIEW_NOTIFY_OUTBOX_STATUS_INVALID, currentStatus);
            }
            int updated = bookingReviewNotifyOutboxMapper.updateByIdAndStatus(id, currentStatus,
                    new BookingReviewNotifyOutboxDO()
                            .setStatus(STATUS_PENDING)
                            .setNextRetryTime(now)
                            .setSentTime(null)
                            .setLastErrorMsg("manual-retry:" + retryReason)
                            .setLastActionCode(ACTION_MANUAL_RETRY)
                            .setLastActionBizNo(buildManualRetryAuditBizNo(operatorId, id))
                            .setLastActionTime(now));
            if (updated == 0) {
                throw exception(BOOKING_REVIEW_NOTIFY_OUTBOX_STATUS_INVALID, currentStatus);
            }
            affected += updated;
        }
        return affected;
    }

    private int createOutboxForNotifyType(BookingReviewDO review, String notifyType) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BookingReviewManagerAccountRoutingDO routing = bookingReviewManagerAccountRoutingMapper
                .selectEffectiveByStoreId(review.getStoreId(), ROUTING_STATUS_ACTIVE, now);
        int count = 0;
        count += createChannelOutbox(review, routing, notifyType, CHANNEL_IN_APP, now);
        count += createChannelOutbox(review, routing, notifyType, CHANNEL_WECOM, now);
        return count;
    }

    private int createChannelOutbox(BookingReviewDO review, BookingReviewManagerAccountRoutingDO routing,
                                    String notifyType, String channel, LocalDateTime now) {
        ChannelTarget target = resolveTarget(routing, channel);
        String idempotencyKey = buildIdempotencyKey(notifyType, review.getId(), channel, target.idempotencyReceiver);
        if (bookingReviewNotifyOutboxMapper.selectByIdempotencyKey(idempotencyKey) != null) {
            return 0;
        }

        BookingReviewNotifyOutboxDO outbox = new BookingReviewNotifyOutboxDO()
                .setBizType(OUTBOX_BIZ_TYPE)
                .setBizId(review.getId())
                .setStoreId(review.getStoreId())
                .setReceiverRole(RECEIVER_ROLE_STORE_MANAGER)
                .setReceiverUserId(target.receiverUserId)
                .setReceiverAccount(target.receiverAccount)
                .setNotifyType(notifyType)
                .setChannel(channel)
                .setStatus(target.blockedReason == null ? STATUS_PENDING : STATUS_BLOCKED_NO_OWNER)
                .setRetryCount(0)
                .setNextRetryTime(target.blockedReason == null ? now : null)
                .setSentTime(null)
                .setLastErrorMsg(target.blockedReason == null ? "" : "BLOCKED_NO_OWNER:" + target.blockedReason)
                .setIdempotencyKey(idempotencyKey)
                .setPayloadSnapshot(buildPayloadSnapshot(review, notifyType, channel, target.receiverAccount))
                .setLastActionCode(target.blockedReason == null ? ACTION_CREATE : ACTION_BLOCKED_NO_OWNER)
                .setLastActionBizNo(buildCreateBizNo(review.getId(), notifyType, channel))
                .setLastActionTime(now);
        try {
            bookingReviewNotifyOutboxMapper.insert(outbox);
            return 1;
        } catch (DuplicateKeyException ex) {
            log.info("booking review notify outbox idempotent hit, reviewId={}, idempotencyKey={}",
                    review.getId(), idempotencyKey);
            return 0;
        }
    }

    private ChannelTarget resolveTarget(BookingReviewManagerAccountRoutingDO routing, String channel) {
        if (routing == null) {
            return new ChannelTarget(null, null, DEFAULT_RECEIVER_KEY, DEFAULT_RECEIVER_KEY);
        }
        if (Objects.equals(channel, CHANNEL_IN_APP)) {
            if (routing.getManagerAdminUserId() == null || routing.getManagerAdminUserId() <= 0) {
                return new ChannelTarget(null, null, "NO_APP_ACCOUNT", "NO_APP_ACCOUNT");
            }
            return new ChannelTarget(routing.getManagerAdminUserId(), "ADMIN#" + routing.getManagerAdminUserId(),
                    String.valueOf(routing.getManagerAdminUserId()), null);
        }
        if (Objects.equals(channel, CHANNEL_WECOM)) {
            if (StrUtil.isBlank(routing.getManagerWecomUserId())) {
                return new ChannelTarget(null, null, "NO_WECOM_ACCOUNT", "NO_WECOM_ACCOUNT");
            }
            return new ChannelTarget(null, routing.getManagerWecomUserId(), routing.getManagerWecomUserId(), null);
        }
        throw new IllegalArgumentException("unsupported notify channel: " + channel);
    }

    private String resolveSlaNotifyType(BookingReviewDO review, LocalDateTime now) {
        if (review == null || review.getManagerTodoStatus() == null
                || Objects.equals(review.getManagerTodoStatus(), BookingReviewManagerTodoStatusEnum.CLOSED.getStatus())) {
            return null;
        }
        if (review.getManagerCloseDeadlineAt() != null && now.isAfter(review.getManagerCloseDeadlineAt())) {
            return NOTIFY_TYPE_MANAGER_CLOSE_TIMEOUT;
        }
        if (review.getManagerFirstActionAt() == null
                && review.getManagerFirstActionDeadlineAt() != null
                && now.isAfter(review.getManagerFirstActionDeadlineAt())) {
            return NOTIFY_TYPE_MANAGER_FIRST_ACTION_TIMEOUT;
        }
        if (review.getManagerClaimedAt() == null
                && review.getManagerClaimDeadlineAt() != null
                && now.isAfter(review.getManagerClaimDeadlineAt())) {
            return NOTIFY_TYPE_MANAGER_CLAIM_TIMEOUT;
        }
        return null;
    }

    private String buildIdempotencyKey(String notifyType, Long reviewId, String channel, String receiverKey) {
        return StrUtil.format("booking_review:{}:{}:{}:{}",
                normalizeNotifyType(notifyType),
                reviewId,
                channel,
                StrUtil.blankToDefault(receiverKey, DEFAULT_RECEIVER_KEY));
    }

    private String normalizeNotifyType(String notifyType) {
        if (Objects.equals(notifyType, NOTIFY_TYPE_NEGATIVE_CREATED)) {
            return "negative_created";
        }
        if (Objects.equals(notifyType, NOTIFY_TYPE_MANAGER_CLAIM_TIMEOUT)) {
            return "manager_claim_timeout";
        }
        if (Objects.equals(notifyType, NOTIFY_TYPE_MANAGER_FIRST_ACTION_TIMEOUT)) {
            return "manager_first_action_timeout";
        }
        if (Objects.equals(notifyType, NOTIFY_TYPE_MANAGER_CLOSE_TIMEOUT)) {
            return "manager_close_timeout";
        }
        return StrUtil.blankToDefault(notifyType, "negative_created").toLowerCase();
    }

    private String buildPayloadSnapshot(BookingReviewDO review, String notifyType, String channel, String receiverAccount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", review.getId());
        payload.put("storeId", review.getStoreId());
        payload.put("bookingOrderId", review.getBookingOrderId());
        payload.put("reviewLevel", review.getReviewLevel());
        payload.put("memberId", review.getMemberId());
        payload.put("submitTime", review.getSubmitTime());
        payload.put("notifyType", notifyType);
        payload.put("channel", channel);
        payload.put("receiverAccount", receiverAccount);
        return JsonUtils.toJsonString(payload);
    }

    private int resolveDispatchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_DISPATCH_LIMIT;
        }
        return Math.min(limit, MAX_DISPATCH_LIMIT);
    }

    private int resolveReminderLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_REMINDER_LIMIT;
        }
        return Math.min(limit, MAX_DISPATCH_LIMIT);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> normalized = new ArrayList<>();
        for (Long id : ids) {
            if (id != null && !normalized.contains(id)) {
                normalized.add(id);
            }
        }
        return normalized;
    }

    private String dispatchNotify(BookingReviewNotifyOutboxDO outbox) {
        if (Objects.equals(outbox.getChannel(), CHANNEL_IN_APP)) {
            return dispatchInAppNotify(outbox);
        }
        if (Objects.equals(outbox.getChannel(), CHANNEL_WECOM)) {
            return dispatchWecomNotify(outbox);
        }
        throw new IllegalArgumentException("UNSUPPORTED_CHANNEL:" + outbox.getChannel());
    }

    private String dispatchInAppNotify(BookingReviewNotifyOutboxDO outbox) {
        if (outbox.getReceiverUserId() == null) {
            throw new BookingReviewNotifyChannelBlockedException("NO_APP_ACCOUNT");
        }
        Long messageId = notifySendService.sendSingleNotifyToAdmin(outbox.getReceiverUserId(),
                TEMPLATE_NEGATIVE_REVIEW_CREATED, buildNotifyParams(outbox));
        if (messageId == null) {
            throw new IllegalStateException("notify message skipped due template status: " + TEMPLATE_NEGATIVE_REVIEW_CREATED);
        }
        return "MSG#" + messageId;
    }

    private String dispatchWecomNotify(BookingReviewNotifyOutboxDO outbox) {
        if (StrUtil.isBlank(outbox.getReceiverAccount())) {
            throw new BookingReviewNotifyChannelBlockedException("NO_WECOM_ACCOUNT");
        }
        return bookingReviewWecomRobotSender.send(outbox.getReceiverAccount(), outbox.getNotifyType(), buildNotifyParams(outbox));
    }

    private Map<String, Object> buildNotifyParams(BookingReviewNotifyOutboxDO outbox) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("reviewId", outbox.getBizId());
        params.put("storeId", outbox.getStoreId());
        params.put("notifyType", outbox.getNotifyType());
        params.put("receiverRole", outbox.getReceiverRole());
        params.put("channel", outbox.getChannel());
        params.put("receiverAccount", outbox.getReceiverAccount());
        return params;
    }

    private String buildCreateBizNo(Long reviewId, String notifyType, String channel) {
        return StrUtil.maxLength(StrUtil.format("REVIEW#{}/{}#{}", reviewId, notifyType, channel), 64);
    }

    private String buildDispatchBizNo(String bizNo, Long outboxId) {
        return StrUtil.blankToDefault(StrUtil.maxLength(bizNo, 64), "OUTBOX#" + outboxId);
    }

    private String buildManualRetryAuditBizNo(Long operatorId, Long outboxId) {
        String operator = operatorId == null ? "SYSTEM" : String.valueOf(operatorId);
        return StrUtil.maxLength(StrUtil.format("ADMIN#{}/OUTBOX#{}", operator, outboxId), 64);
    }

    private static final class ChannelTarget {
        private final Long receiverUserId;
        private final String receiverAccount;
        private final String idempotencyReceiver;
        private final String blockedReason;

        private ChannelTarget(Long receiverUserId, String receiverAccount, String idempotencyReceiver,
                              String blockedReason) {
            this.receiverUserId = receiverUserId;
            this.receiverAccount = receiverAccount;
            this.idempotencyReceiver = idempotencyReceiver;
            this.blockedReason = blockedReason;
        }
    }
}
