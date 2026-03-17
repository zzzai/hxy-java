package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.admin.vo.BookingReviewDashboardRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewFollowUpdateReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewPageReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewCreateReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewEligibilityRespVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewPageReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.mysql.BookingOrderMapper;
import com.hxy.module.booking.dal.mysql.BookingReviewMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.enums.BookingReviewDisplayStatusEnum;
import com.hxy.module.booking.enums.BookingReviewFollowStatusEnum;
import com.hxy.module.booking.enums.BookingReviewLevelEnum;
import com.hxy.module.booking.service.BookingReviewService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_NOT_OWNER;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_ALREADY_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOT_ELIGIBLE;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOT_EXISTS;

@Service
@Validated
public class BookingReviewServiceImpl implements BookingReviewService {

    private static final int RISK_LEVEL_NORMAL = 0;
    private static final int RISK_LEVEL_ATTENTION = 1;
    private static final int RISK_LEVEL_URGENT = 2;
    private static final int AUDIT_STATUS_PASS = 0;
    private static final int AUDIT_STATUS_MANUAL_REVIEW = 2;
    private static final String DEFAULT_SOURCE = "order_detail";

    private final BookingReviewMapper bookingReviewMapper;
    private final BookingOrderMapper bookingOrderMapper;

    public BookingReviewServiceImpl(BookingReviewMapper bookingReviewMapper,
                                    BookingOrderMapper bookingOrderMapper) {
        this.bookingReviewMapper = bookingReviewMapper;
        this.bookingOrderMapper = bookingOrderMapper;
    }

    @Override
    public AppBookingReviewEligibilityRespVO getEligibility(Long memberId, Long bookingOrderId) {
        BookingOrderDO order = bookingOrderMapper.selectById(bookingOrderId);
        AppBookingReviewEligibilityRespVO respVO = new AppBookingReviewEligibilityRespVO();
        respVO.setBookingOrderId(bookingOrderId);
        if (order == null) {
            respVO.setEligible(Boolean.FALSE);
            respVO.setReason("ORDER_NOT_EXISTS");
            return respVO;
        }
        if (!memberId.equals(order.getUserId())) {
            respVO.setEligible(Boolean.FALSE);
            respVO.setReason("NOT_OWNER");
            return respVO;
        }

        BookingReviewDO existed = bookingReviewMapper.selectByBookingOrderId(order.getId());
        if (existed != null) {
            respVO.setEligible(Boolean.FALSE);
            respVO.setAlreadyReviewed(Boolean.TRUE);
            respVO.setReviewId(existed.getId());
            respVO.setReason("ALREADY_REVIEWED");
            return respVO;
        }
        if (!BookingOrderStatusEnum.COMPLETED.getStatus().equals(order.getStatus())) {
            respVO.setEligible(Boolean.FALSE);
            respVO.setReason("ORDER_NOT_COMPLETED");
            return respVO;
        }

        respVO.setEligible(Boolean.TRUE);
        respVO.setAlreadyReviewed(Boolean.FALSE);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReview(Long memberId, AppBookingReviewCreateReqVO reqVO) {
        BookingOrderDO order = validateOrderForReview(memberId, reqVO.getBookingOrderId());
        if (bookingReviewMapper.selectByBookingOrderId(order.getId()) != null) {
            throw exception(BOOKING_REVIEW_ALREADY_EXISTS);
        }

        Integer reviewLevel = resolveReviewLevel(reqVO.getOverallScore());
        BookingReviewDO review = BookingReviewDO.builder()
                .bookingOrderId(order.getId())
                .serviceOrderId(null)
                .storeId(order.getStoreId())
                .technicianId(order.getTechnicianId())
                .memberId(memberId)
                .serviceSpuId(order.getSpuId())
                .serviceSkuId(order.getSkuId())
                .overallScore(reqVO.getOverallScore())
                .serviceScore(reqVO.getServiceScore())
                .technicianScore(reqVO.getTechnicianScore())
                .environmentScore(reqVO.getEnvironmentScore())
                .tags(reqVO.getTags())
                .content(reqVO.getContent())
                .picUrls(reqVO.getPicUrls())
                .anonymous(Boolean.TRUE.equals(reqVO.getAnonymous()))
                .reviewLevel(reviewLevel)
                .riskLevel(resolveRiskLevel(reviewLevel))
                .displayStatus(resolveDisplayStatus(reviewLevel))
                .followStatus(resolveFollowStatus(reviewLevel))
                .replyStatus(Boolean.FALSE)
                .auditStatus(resolveAuditStatus(reviewLevel))
                .source(reqVO.getSource() == null || reqVO.getSource().trim().isEmpty() ? DEFAULT_SOURCE : reqVO.getSource().trim())
                .completedTime(order.getServiceEndTime())
                .submitTime(LocalDateTime.now().withNano(0))
                .build();
        try {
            bookingReviewMapper.insert(review);
        } catch (DuplicateKeyException ex) {
            throw exception(BOOKING_REVIEW_ALREADY_EXISTS);
        }
        return review.getId();
    }

    @Override
    public PageResult<BookingReviewDO> getReviewPage(Long memberId, AppBookingReviewPageReqVO reqVO) {
        return bookingReviewMapper.selectPageByMemberId(memberId, reqVO);
    }

    @Override
    public BookingReviewDO getReview(Long memberId, Long reviewId) {
        BookingReviewDO review = bookingReviewMapper.selectByIdAndMemberId(reviewId, memberId);
        if (review == null) {
            throw exception(BOOKING_REVIEW_NOT_EXISTS);
        }
        return review;
    }

    @Override
    public AppBookingReviewSummaryRespVO getSummary(Long memberId) {
        List<BookingReviewDO> list = bookingReviewMapper.selectListByMemberId(memberId);
        if (list == null) {
            list = Collections.emptyList();
        }
        int totalScore = 0;
        BookingReviewCounter counter = new BookingReviewCounter();
        for (BookingReviewDO review : list) {
            counter.accept(review);
            totalScore += review.getOverallScore() == null ? 0 : review.getOverallScore();
        }
        AppBookingReviewSummaryRespVO respVO = new AppBookingReviewSummaryRespVO();
        respVO.setTotalCount((long) list.size());
        respVO.setPositiveCount(counter.positiveCount);
        respVO.setNeutralCount(counter.neutralCount);
        respVO.setNegativeCount(counter.negativeCount);
        respVO.setAverageScore(list.isEmpty() ? 0 : Math.round((float) totalScore / list.size()));
        return respVO;
    }

    @Override
    public PageResult<BookingReviewDO> getAdminReviewPage(BookingReviewPageReqVO reqVO) {
        return bookingReviewMapper.selectAdminPage(reqVO);
    }

    @Override
    public BookingReviewDO getAdminReview(Long reviewId) {
        BookingReviewDO review = bookingReviewMapper.selectById(reviewId);
        if (review == null) {
            throw exception(BOOKING_REVIEW_NOT_EXISTS);
        }
        return review;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyReview(Long reviewId, Long operatorId, String replyContent) {
        BookingReviewDO review = getAdminReview(reviewId);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        review.setReplyStatus(Boolean.TRUE);
        review.setReplyUserId(operatorId);
        review.setReplyContent(replyContent == null ? null : replyContent.trim());
        review.setReplyTime(now);
        if (review.getFirstResponseAt() == null) {
            review.setFirstResponseAt(now);
        }
        bookingReviewMapper.updateById(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFollowStatus(Long reviewId, Long operatorId, BookingReviewFollowUpdateReqVO reqVO) {
        BookingReviewDO review = getAdminReview(reviewId);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        review.setFollowStatus(reqVO.getFollowStatus());
        review.setFollowOwnerId(operatorId);
        review.setFollowResult(reqVO.getFollowResult() == null ? null : reqVO.getFollowResult().trim());
        if (review.getFirstResponseAt() == null) {
            review.setFirstResponseAt(now);
        }
        bookingReviewMapper.updateById(review);
    }

    @Override
    public BookingReviewDashboardRespVO getDashboardSummary() {
        List<BookingReviewDO> list = bookingReviewMapper.selectList();
        if (list == null) {
            list = Collections.emptyList();
        }
        BookingReviewCounter counter = new BookingReviewCounter();
        for (BookingReviewDO review : list) {
            counter.accept(review);
        }
        BookingReviewDashboardRespVO respVO = new BookingReviewDashboardRespVO();
        respVO.setTotalCount((long) list.size());
        respVO.setPositiveCount(counter.positiveCount);
        respVO.setNeutralCount(counter.neutralCount);
        respVO.setNegativeCount(counter.negativeCount);
        respVO.setPendingFollowCount(counter.pendingFollowCount);
        respVO.setUrgentCount(counter.urgentCount);
        respVO.setRepliedCount(counter.repliedCount);
        return respVO;
    }

    private BookingOrderDO validateOrderForReview(Long memberId, Long bookingOrderId) {
        BookingOrderDO order = bookingOrderMapper.selectById(bookingOrderId);
        if (order == null) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        if (!memberId.equals(order.getUserId())) {
            throw exception(BOOKING_ORDER_NOT_OWNER);
        }
        if (!BookingOrderStatusEnum.COMPLETED.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_REVIEW_NOT_ELIGIBLE);
        }
        return order;
    }

    private Integer resolveReviewLevel(Integer overallScore) {
        if (overallScore == null || overallScore <= 2) {
            return BookingReviewLevelEnum.NEGATIVE.getLevel();
        }
        if (overallScore == 3) {
            return BookingReviewLevelEnum.NEUTRAL.getLevel();
        }
        return BookingReviewLevelEnum.POSITIVE.getLevel();
    }

    private Integer resolveRiskLevel(Integer reviewLevel) {
        if (BookingReviewLevelEnum.NEGATIVE.getLevel().equals(reviewLevel)) {
            return RISK_LEVEL_URGENT;
        }
        if (BookingReviewLevelEnum.NEUTRAL.getLevel().equals(reviewLevel)) {
            return RISK_LEVEL_ATTENTION;
        }
        return RISK_LEVEL_NORMAL;
    }

    private Integer resolveDisplayStatus(Integer reviewLevel) {
        if (BookingReviewLevelEnum.NEGATIVE.getLevel().equals(reviewLevel)) {
            return BookingReviewDisplayStatusEnum.REVIEW_PENDING.getStatus();
        }
        return BookingReviewDisplayStatusEnum.VISIBLE.getStatus();
    }

    private Integer resolveFollowStatus(Integer reviewLevel) {
        if (BookingReviewLevelEnum.NEGATIVE.getLevel().equals(reviewLevel)) {
            return BookingReviewFollowStatusEnum.PENDING.getStatus();
        }
        return BookingReviewFollowStatusEnum.NONE.getStatus();
    }

    private Integer resolveAuditStatus(Integer reviewLevel) {
        if (BookingReviewLevelEnum.NEGATIVE.getLevel().equals(reviewLevel)) {
            return AUDIT_STATUS_MANUAL_REVIEW;
        }
        return AUDIT_STATUS_PASS;
    }

    private static class BookingReviewCounter {

        private long positiveCount;
        private long neutralCount;
        private long negativeCount;
        private long pendingFollowCount;
        private long urgentCount;
        private long repliedCount;

        private void accept(BookingReviewDO review) {
            if (review == null) {
                return;
            }
            if (BookingReviewLevelEnum.POSITIVE.getLevel().equals(review.getReviewLevel())) {
                positiveCount++;
            } else if (BookingReviewLevelEnum.NEUTRAL.getLevel().equals(review.getReviewLevel())) {
                neutralCount++;
            } else if (BookingReviewLevelEnum.NEGATIVE.getLevel().equals(review.getReviewLevel())) {
                negativeCount++;
            }
            if (BookingReviewFollowStatusEnum.PENDING.getStatus().equals(review.getFollowStatus())
                    || BookingReviewFollowStatusEnum.PROCESSING.getStatus().equals(review.getFollowStatus())) {
                pendingFollowCount++;
            }
            if (RISK_LEVEL_URGENT == (review.getRiskLevel() == null ? RISK_LEVEL_NORMAL : review.getRiskLevel())) {
                urgentCount++;
            }
            if (Boolean.TRUE.equals(review.getReplyStatus())) {
                repliedCount++;
            }
        }
    }
}
