package com.hxy.module.booking.service.support;

import cn.hutool.core.util.StrUtil;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import com.hxy.module.booking.enums.BookingReviewLevelEnum;
import com.hxy.module.booking.enums.BookingReviewManagerTodoStatusEnum;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Booking review 后台值班只读派生真值。
 */
public final class BookingReviewAdminPrioritySupport {

    public static final String MANAGER_SLA_STAGE_PENDING_INIT = "PENDING_INIT";
    public static final String MANAGER_SLA_STAGE_NORMAL = "NORMAL";
    public static final String MANAGER_SLA_STAGE_CLAIM_DUE_SOON = "CLAIM_DUE_SOON";
    public static final String MANAGER_SLA_STAGE_CLAIM_TIMEOUT = "CLAIM_TIMEOUT";
    public static final String MANAGER_SLA_STAGE_FIRST_ACTION_DUE_SOON = "FIRST_ACTION_DUE_SOON";
    public static final String MANAGER_SLA_STAGE_FIRST_ACTION_TIMEOUT = "FIRST_ACTION_TIMEOUT";
    public static final String MANAGER_SLA_STAGE_CLOSE_DUE_SOON = "CLOSE_DUE_SOON";
    public static final String MANAGER_SLA_STAGE_CLOSE_TIMEOUT = "CLOSE_TIMEOUT";
    public static final String MANAGER_SLA_STAGE_CLOSED = "CLOSED";

    public static final String PRIORITY_P0 = "P0";
    public static final String PRIORITY_P1 = "P1";
    public static final String PRIORITY_P2 = "P2";
    public static final String PRIORITY_P3 = "P3";

    private static final String CHANNEL_IN_APP = "IN_APP";
    private static final String CHANNEL_WECOM = "WECOM";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_BLOCKED_NO_OWNER = "BLOCKED_NO_OWNER";
    private static final String ACTION_MANUAL_RETRY = "MANUAL_RETRY";

    private static final long CLAIM_DUE_SOON_MINUTES = 5L;
    private static final long FIRST_ACTION_DUE_SOON_MINUTES = 10L;
    private static final long CLOSE_DUE_SOON_MINUTES = 120L;

    private BookingReviewAdminPrioritySupport() {
    }

    public static String resolveManagerSlaStage(BookingReviewDO review, LocalDateTime now) {
        if (review == null) {
            return null;
        }
        if (review.getManagerTodoStatus() == null) {
            return BookingReviewLevelEnum.NEGATIVE.getLevel().equals(review.getReviewLevel())
                    ? MANAGER_SLA_STAGE_PENDING_INIT : null;
        }
        if (BookingReviewManagerTodoStatusEnum.CLOSED.getStatus().equals(review.getManagerTodoStatus())) {
            return MANAGER_SLA_STAGE_CLOSED;
        }
        if (isAfter(now, review.getManagerCloseDeadlineAt())) {
            return MANAGER_SLA_STAGE_CLOSE_TIMEOUT;
        }
        if (review.getManagerFirstActionAt() == null && isAfter(now, review.getManagerFirstActionDeadlineAt())) {
            return MANAGER_SLA_STAGE_FIRST_ACTION_TIMEOUT;
        }
        if (review.getManagerClaimedAt() == null && isAfter(now, review.getManagerClaimDeadlineAt())) {
            return MANAGER_SLA_STAGE_CLAIM_TIMEOUT;
        }
        if (isDueSoon(now, review.getManagerCloseDeadlineAt(), CLOSE_DUE_SOON_MINUTES)) {
            return MANAGER_SLA_STAGE_CLOSE_DUE_SOON;
        }
        if (review.getManagerFirstActionAt() == null
                && isDueSoon(now, review.getManagerFirstActionDeadlineAt(), FIRST_ACTION_DUE_SOON_MINUTES)) {
            return MANAGER_SLA_STAGE_FIRST_ACTION_DUE_SOON;
        }
        if (review.getManagerClaimedAt() == null
                && isDueSoon(now, review.getManagerClaimDeadlineAt(), CLAIM_DUE_SOON_MINUTES)) {
            return MANAGER_SLA_STAGE_CLAIM_DUE_SOON;
        }
        return MANAGER_SLA_STAGE_NORMAL;
    }

    public static NotifyRiskSnapshot resolveNotifyRisk(List<BookingReviewNotifyOutboxDO> outboxes) {
        if (outboxes == null || outboxes.isEmpty()) {
            return new NotifyRiskSnapshot("未核出通知记录", false, false);
        }
        Map<String, BookingReviewNotifyOutboxDO> latestByChannel = buildLatestByChannel(outboxes);
        BookingReviewNotifyOutboxDO inApp = latestByChannel.get(CHANNEL_IN_APP);
        BookingReviewNotifyOutboxDO wecom = latestByChannel.get(CHANNEL_WECOM);
        String summary = resolveNotifyRiskSummary(inApp, wecom, latestByChannel);
        return new NotifyRiskSnapshot(summary,
                isStatus(inApp, STATUS_BLOCKED_NO_OWNER) || isStatus(wecom, STATUS_BLOCKED_NO_OWNER),
                isStatus(inApp, STATUS_FAILED) || isStatus(wecom, STATUS_FAILED));
    }

    public static ReviewNotifyAuditSnapshot resolveReviewNotifyAudit(List<BookingReviewNotifyOutboxDO> outboxes) {
        if (outboxes == null || outboxes.isEmpty()) {
            return new ReviewNotifyAuditSnapshot("PENDING_DISPATCH", "待继续派发",
                    "当前筛选范围内未核出完整的双通道通知记录，需要继续观察后续派发。", false, false, false, false, false);
        }
        Map<String, BookingReviewNotifyOutboxDO> latestByChannel = buildLatestByChannel(outboxes);
        BookingReviewNotifyOutboxDO inApp = latestByChannel.get(CHANNEL_IN_APP);
        BookingReviewNotifyOutboxDO wecom = latestByChannel.get(CHANNEL_WECOM);
        String summary = resolveNotifyRiskSummary(inApp, wecom, latestByChannel);
        boolean dualSent = isStatus(inApp, STATUS_SENT) && isStatus(wecom, STATUS_SENT);
        boolean anyBlocked = isStatus(inApp, STATUS_BLOCKED_NO_OWNER) || isStatus(wecom, STATUS_BLOCKED_NO_OWNER);
        boolean anyFailed = isStatus(inApp, STATUS_FAILED) || isStatus(wecom, STATUS_FAILED);
        boolean manualRetryPending = isManualRetryPending(inApp) || isManualRetryPending(wecom);
        boolean diverged = isDiverged(inApp, wecom);
        if (anyBlocked) {
            return new ReviewNotifyAuditSnapshot("ANY_BLOCKED", "存在阻断",
                    "至少一个通道仍被路由或账号问题阻断；当前整体状态：" + summary, dualSent, anyBlocked, anyFailed,
                    manualRetryPending, diverged);
        }
        if (manualRetryPending) {
            return new ReviewNotifyAuditSnapshot("MANUAL_RETRY_PENDING", "人工重试后待复核",
                    "存在人工重试后待复核记录；当前整体状态：" + summary, dualSent, anyBlocked, anyFailed,
                    true, diverged);
        }
        if (anyFailed) {
            return new ReviewNotifyAuditSnapshot("ANY_FAILED", "存在失败",
                    "至少一个通道发送失败；当前整体状态：" + summary, dualSent, anyBlocked, true,
                    manualRetryPending, diverged);
        }
        if (dualSent) {
            return new ReviewNotifyAuditSnapshot("DUAL_SENT", "双通道已发送",
                    "App / 企微两条通道的最新记录都已发送。", true, anyBlocked, anyFailed,
                    manualRetryPending, diverged);
        }
        if (diverged) {
            return new ReviewNotifyAuditSnapshot("DIVERGED", "跨通道分裂",
                    "App / 企微两条通道的最新状态不一致；当前整体状态：" + summary, dualSent, anyBlocked, anyFailed,
                    manualRetryPending, true);
        }
        return new ReviewNotifyAuditSnapshot("PENDING_DISPATCH", "待继续派发",
                "至少一个通道仍待派发；当前整体状态：" + summary, dualSent, anyBlocked, anyFailed,
                manualRetryPending, diverged);
    }

    public static PrioritySnapshot resolvePriority(BookingReviewDO review, LocalDateTime now,
                                                   NotifyRiskSnapshot notifyRiskSnapshot) {
        String managerSlaStage = resolveManagerSlaStage(review, now);
        return resolvePriority(managerSlaStage, notifyRiskSnapshot);
    }

    public static PrioritySnapshot resolvePriority(String managerSlaStage, NotifyRiskSnapshot notifyRiskSnapshot) {
        if (notifyRiskSnapshot != null && notifyRiskSnapshot.hasBlockedNoOwner()) {
            return new PrioritySnapshot(PRIORITY_P0, "存在店长路由阻断");
        }
        if (Objects.equals(managerSlaStage, MANAGER_SLA_STAGE_CLOSE_TIMEOUT)) {
            return new PrioritySnapshot(PRIORITY_P0, "闭环已超时");
        }
        if (Objects.equals(managerSlaStage, MANAGER_SLA_STAGE_FIRST_ACTION_TIMEOUT)) {
            return new PrioritySnapshot(PRIORITY_P1, "首次处理已超时");
        }
        if (Objects.equals(managerSlaStage, MANAGER_SLA_STAGE_CLAIM_TIMEOUT)) {
            return new PrioritySnapshot(PRIORITY_P1, "认领已超时");
        }
        if (Objects.equals(managerSlaStage, MANAGER_SLA_STAGE_CLOSE_DUE_SOON)) {
            return new PrioritySnapshot(PRIORITY_P1, "闭环即将超时");
        }
        if (notifyRiskSnapshot != null && notifyRiskSnapshot.hasFailedDispatch()) {
            return new PrioritySnapshot(PRIORITY_P1, "存在通知发送失败");
        }
        if (Objects.equals(managerSlaStage, MANAGER_SLA_STAGE_FIRST_ACTION_DUE_SOON)) {
            return new PrioritySnapshot(PRIORITY_P2, "首次处理即将超时");
        }
        if (Objects.equals(managerSlaStage, MANAGER_SLA_STAGE_CLAIM_DUE_SOON)) {
            return new PrioritySnapshot(PRIORITY_P2, "认领即将超时");
        }
        if (Objects.equals(managerSlaStage, MANAGER_SLA_STAGE_PENDING_INIT)) {
            return new PrioritySnapshot(PRIORITY_P2, "历史差评待初始化");
        }
        if (Objects.equals(managerSlaStage, MANAGER_SLA_STAGE_CLOSED)) {
            return new PrioritySnapshot(PRIORITY_P3, "店长待办已闭环");
        }
        return new PrioritySnapshot(PRIORITY_P3, "正常待办观察");
    }

    private static boolean isAfter(LocalDateTime now, LocalDateTime deadline) {
        return now != null && deadline != null && now.isAfter(deadline);
    }

    private static boolean isDueSoon(LocalDateTime now, LocalDateTime deadline, long thresholdMinutes) {
        if (now == null || deadline == null || now.isAfter(deadline)) {
            return false;
        }
        long minutes = Duration.between(now, deadline).toMinutes();
        return minutes >= 0 && minutes <= thresholdMinutes;
    }

    private static String resolveNotifyRiskSummary(BookingReviewNotifyOutboxDO inApp,
                                                   BookingReviewNotifyOutboxDO wecom,
                                                   Map<String, BookingReviewNotifyOutboxDO> latestByChannel) {
        if (latestByChannel.isEmpty()) {
            return "未核出通知记录";
        }
        if (isStatus(inApp, STATUS_BLOCKED_NO_OWNER) && isStatus(wecom, STATUS_BLOCKED_NO_OWNER)) {
            return "双通道阻断";
        }
        if (isStatus(inApp, STATUS_FAILED) && isStatus(wecom, STATUS_FAILED)) {
            return "双通道发送失败";
        }
        if (isStatus(inApp, STATUS_PENDING) && isStatus(wecom, STATUS_PENDING)) {
            return "双通道待派发";
        }
        if (isStatus(inApp, STATUS_SENT) && isStatus(wecom, STATUS_SENT)) {
            return "双通道已派发";
        }
        if (isStatus(inApp, STATUS_BLOCKED_NO_OWNER) && isStatus(wecom, STATUS_PENDING)) {
            return "App 阻断，企微待派发";
        }
        if (isStatus(wecom, STATUS_BLOCKED_NO_OWNER) && isStatus(inApp, STATUS_PENDING)) {
            return "企微阻断，App 待派发";
        }
        if (isStatus(inApp, STATUS_BLOCKED_NO_OWNER) && isStatus(wecom, STATUS_SENT)) {
            return "App 阻断，企微已派发";
        }
        if (isStatus(wecom, STATUS_BLOCKED_NO_OWNER) && isStatus(inApp, STATUS_SENT)) {
            return "企微阻断，App 已派发";
        }
        if (isStatus(inApp, STATUS_FAILED)) {
            return "App 发送失败";
        }
        if (isStatus(wecom, STATUS_FAILED)) {
            return "企微发送失败";
        }
        if (isStatus(inApp, STATUS_BLOCKED_NO_OWNER)) {
            return "App 阻断";
        }
        if (isStatus(wecom, STATUS_BLOCKED_NO_OWNER)) {
            return "企微阻断";
        }
        if (isStatus(inApp, STATUS_PENDING) && isStatus(wecom, STATUS_SENT)) {
            return "App 待派发，企微已派发";
        }
        if (isStatus(wecom, STATUS_PENDING) && isStatus(inApp, STATUS_SENT)) {
            return "企微待派发，App 已派发";
        }
        if (isStatus(inApp, STATUS_SENT)) {
            return "App 已派发";
        }
        if (isStatus(wecom, STATUS_SENT)) {
            return "企微已派发";
        }
        if (isStatus(inApp, STATUS_PENDING)) {
            return "App 待派发";
        }
        if (isStatus(wecom, STATUS_PENDING)) {
            return "企微待派发";
        }
        return "未核出通知记录";
    }

    private static Map<String, BookingReviewNotifyOutboxDO> buildLatestByChannel(List<BookingReviewNotifyOutboxDO> outboxes) {
        Map<String, BookingReviewNotifyOutboxDO> latestByChannel = new LinkedHashMap<>();
        for (BookingReviewNotifyOutboxDO outbox : outboxes) {
            if (outbox == null || StrUtil.isBlank(outbox.getChannel())) {
                continue;
            }
            latestByChannel.compute(outbox.getChannel(), (channel, current) ->
                    isNewer(current, outbox) ? outbox : current);
        }
        return latestByChannel;
    }

    private static boolean isNewer(BookingReviewNotifyOutboxDO current, BookingReviewNotifyOutboxDO candidate) {
        if (current == null) {
            return true;
        }
        Long currentId = current.getId();
        Long candidateId = candidate.getId();
        if (candidateId != null && currentId != null) {
            return candidateId > currentId;
        }
        if (candidate.getLastActionTime() != null && current.getLastActionTime() != null) {
            return candidate.getLastActionTime().isAfter(current.getLastActionTime());
        }
        return candidate.getCreateTime() != null && current.getCreateTime() != null
                && candidate.getCreateTime().isAfter(current.getCreateTime());
    }

    private static boolean isManualRetryPending(BookingReviewNotifyOutboxDO outbox) {
        return outbox != null
                && isStatus(outbox, STATUS_PENDING)
                && Objects.equals(ACTION_MANUAL_RETRY, StrUtil.blankToDefault(outbox.getLastActionCode(), ""));
    }

    private static boolean isDiverged(BookingReviewNotifyOutboxDO inApp, BookingReviewNotifyOutboxDO wecom) {
        if (inApp == null || wecom == null) {
            return false;
        }
        String inAppStatus = StrUtil.blankToDefault(inApp.getStatus(), STATUS_PENDING);
        String wecomStatus = StrUtil.blankToDefault(wecom.getStatus(), STATUS_PENDING);
        return !Objects.equals(inAppStatus, wecomStatus);
    }

    private static boolean isStatus(BookingReviewNotifyOutboxDO outbox, String status) {
        return outbox != null && Objects.equals(status, StrUtil.blankToDefault(outbox.getStatus(), STATUS_PENDING));
    }

    public static List<BookingReviewNotifyOutboxDO> emptyOutboxes() {
        return Collections.emptyList();
    }

    public static final class NotifyRiskSnapshot {
        private final String summary;
        private final boolean blockedNoOwner;
        private final boolean failedDispatch;

        private NotifyRiskSnapshot(String summary, boolean blockedNoOwner, boolean failedDispatch) {
            this.summary = summary;
            this.blockedNoOwner = blockedNoOwner;
            this.failedDispatch = failedDispatch;
        }

        public String getSummary() {
            return summary;
        }

        public boolean hasBlockedNoOwner() {
            return blockedNoOwner;
        }

        public boolean hasFailedDispatch() {
            return failedDispatch;
        }
    }

    public static final class PrioritySnapshot {
        private final String level;
        private final String reason;

        private PrioritySnapshot(String level, String reason) {
            this.level = level;
            this.reason = reason;
        }

        public String getLevel() {
            return level;
        }

        public String getReason() {
            return reason;
        }
    }

    public static final class ReviewNotifyAuditSnapshot {
        private final String stage;
        private final String label;
        private final String detail;
        private final boolean dualSent;
        private final boolean blocked;
        private final boolean failed;
        private final boolean manualRetryPending;
        private final boolean diverged;

        private ReviewNotifyAuditSnapshot(String stage, String label, String detail,
                                          boolean dualSent, boolean blocked, boolean failed,
                                          boolean manualRetryPending, boolean diverged) {
            this.stage = stage;
            this.label = label;
            this.detail = detail;
            this.dualSent = dualSent;
            this.blocked = blocked;
            this.failed = failed;
            this.manualRetryPending = manualRetryPending;
            this.diverged = diverged;
        }

        public String getStage() {
            return stage;
        }

        public String getLabel() {
            return label;
        }

        public String getDetail() {
            return detail;
        }

        public boolean isDualSent() {
            return dualSent;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public boolean isFailed() {
            return failed;
        }

        public boolean isManualRetryPending() {
            return manualRetryPending;
        }

        public boolean isDiverged() {
            return diverged;
        }
    }
}
