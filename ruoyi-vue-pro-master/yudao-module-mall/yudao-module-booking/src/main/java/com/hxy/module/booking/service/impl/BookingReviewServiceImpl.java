package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import cn.iocoder.yudao.module.trade.api.order.TradeServiceOrderApi;
import cn.iocoder.yudao.module.trade.api.order.dto.TradeServiceOrderTraceRespDTO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewDashboardRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewFollowUpdateReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanItemRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanSummaryRespVO;
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
import com.hxy.module.booking.enums.BookingReviewManagerTodoStatusEnum;
import com.hxy.module.booking.enums.BookingReviewNegativeTriggerTypeEnum;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import com.hxy.module.booking.service.BookingReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_NOT_OWNER;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_ALREADY_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOT_ELIGIBLE;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_NOT_EXISTS;

@Service
@Validated
@Slf4j
public class BookingReviewServiceImpl implements BookingReviewService {

    private static final int RISK_LEVEL_NORMAL = 0;
    private static final int RISK_LEVEL_ATTENTION = 1;
    private static final int RISK_LEVEL_URGENT = 2;
    private static final int AUDIT_STATUS_PASS = 0;
    private static final int AUDIT_STATUS_MANUAL_REVIEW = 2;
    private static final String DEFAULT_SOURCE = "order_detail";
    private static final String MANAGER_SLA_NORMAL = "NORMAL";
    private static final String MANAGER_SLA_CLAIM_TIMEOUT = "CLAIM_TIMEOUT";
    private static final String MANAGER_SLA_FIRST_ACTION_TIMEOUT = "FIRST_ACTION_TIMEOUT";
    private static final String MANAGER_SLA_CLOSE_TIMEOUT = "CLOSE_TIMEOUT";
    private static final String HISTORY_SCAN_MANUAL_READY = "MANUAL_READY";
    private static final String HISTORY_SCAN_HIGH_RISK = "HIGH_RISK";
    private static final String HISTORY_SCAN_OUT_OF_SCOPE = "OUT_OF_SCOPE";

    private final BookingReviewMapper bookingReviewMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final TradeServiceOrderApi tradeServiceOrderApi;
    private final ProductStoreService productStoreService;
    private final BookingReviewNotifyOutboxService bookingReviewNotifyOutboxService;

    public BookingReviewServiceImpl(BookingReviewMapper bookingReviewMapper,
                                    BookingOrderMapper bookingOrderMapper,
                                    TradeServiceOrderApi tradeServiceOrderApi,
                                    ProductStoreService productStoreService,
                                    BookingReviewNotifyOutboxService bookingReviewNotifyOutboxService) {
        this.bookingReviewMapper = bookingReviewMapper;
        this.bookingOrderMapper = bookingOrderMapper;
        this.tradeServiceOrderApi = tradeServiceOrderApi;
        this.productStoreService = productStoreService;
        this.bookingReviewNotifyOutboxService = bookingReviewNotifyOutboxService;
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
                .serviceOrderId(resolveServiceOrderId(order))
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
        populateManagerTodoFields(review, order);
        try {
            bookingReviewMapper.insert(review);
        } catch (DuplicateKeyException ex) {
            throw exception(BOOKING_REVIEW_ALREADY_EXISTS);
        }
        triggerNegativeReviewNotifyOutbox(review);
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
            counter.acceptReviewSummary(review);
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
        if (StrUtil.isBlank(reqVO.getManagerSlaStatus())) {
            return bookingReviewMapper.selectAdminPage(reqVO);
        }
        List<BookingReviewDO> filtered = bookingReviewMapper.selectAdminList(reqVO);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        filtered.removeIf(review -> !StrUtil.equals(reqVO.getManagerSlaStatus(), resolveManagerSlaStatus(review, now)));
        return buildPageResult(filtered, reqVO);
    }

    @Override
    public BookingReviewHistoryScanRespVO scanAdminHistoryCandidates(BookingReviewHistoryScanReqVO reqVO) {
        List<BookingReviewDO> baseList = bookingReviewMapper.selectAdminList(buildHistoryScanBaseQuery(reqVO));
        if (baseList == null) {
            baseList = Collections.emptyList();
        }

        List<BookingReviewHistoryScanItemRespVO> allItems = baseList.stream()
                .map(this::buildHistoryScanItem)
                .collect(Collectors.toList());
        BookingReviewHistoryScanSummaryRespVO summary = buildHistoryScanSummary(allItems);
        List<BookingReviewHistoryScanItemRespVO> filteredItems = filterHistoryScanItems(allItems, reqVO.getRiskCategory());

        BookingReviewHistoryScanRespVO respVO = new BookingReviewHistoryScanRespVO();
        respVO.setSummary(summary);
        respVO.setTotal((long) filteredItems.size());
        respVO.setList(paginateHistoryScanItems(filteredItems, reqVO));
        return respVO;
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
    @Transactional(rollbackFor = Exception.class)
    public void claimManagerTodo(Long reviewId, Long operatorId) {
        BookingReviewDO review = ensureManagerTodoReady(getAdminReview(reviewId));
        assertManagerTodoStatus(review, BookingReviewManagerTodoStatusEnum.PENDING_CLAIM.getStatus());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        review.setManagerTodoStatus(BookingReviewManagerTodoStatusEnum.CLAIMED.getStatus());
        review.setManagerClaimedByUserId(operatorId);
        review.setManagerClaimedAt(now);
        bookingReviewMapper.updateById(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordManagerFirstAction(Long reviewId, Long operatorId, String remark) {
        BookingReviewDO review = ensureManagerTodoReady(getAdminReview(reviewId));
        assertManagerTodoStatus(review,
                BookingReviewManagerTodoStatusEnum.CLAIMED.getStatus(),
                BookingReviewManagerTodoStatusEnum.PROCESSING.getStatus());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        review.setManagerTodoStatus(BookingReviewManagerTodoStatusEnum.PROCESSING.getStatus());
        review.setManagerFirstActionAt(now);
        review.setManagerLatestActionRemark(StrUtil.trim(remark));
        review.setManagerLatestActionByUserId(operatorId);
        bookingReviewMapper.updateById(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeManagerTodo(Long reviewId, Long operatorId, String remark) {
        BookingReviewDO review = ensureManagerTodoReady(getAdminReview(reviewId));
        assertManagerTodoStatus(review,
                BookingReviewManagerTodoStatusEnum.CLAIMED.getStatus(),
                BookingReviewManagerTodoStatusEnum.PROCESSING.getStatus());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        review.setManagerTodoStatus(BookingReviewManagerTodoStatusEnum.CLOSED.getStatus());
        review.setManagerClosedAt(now);
        review.setManagerLatestActionRemark(StrUtil.trim(remark));
        review.setManagerLatestActionByUserId(operatorId);
        bookingReviewMapper.updateById(review);
    }

    @Override
    public BookingReviewDashboardRespVO getDashboardSummary() {
        List<BookingReviewDO> list = bookingReviewMapper.selectList();
        if (list == null) {
            list = Collections.emptyList();
        }
        BookingReviewCounter counter = new BookingReviewCounter();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        for (BookingReviewDO review : list) {
            counter.acceptReviewSummary(review);
            counter.acceptManagerTodoSummary(review, now);
        }
        BookingReviewDashboardRespVO respVO = new BookingReviewDashboardRespVO();
        respVO.setTotalCount((long) list.size());
        respVO.setPositiveCount(counter.positiveCount);
        respVO.setNeutralCount(counter.neutralCount);
        respVO.setNegativeCount(counter.negativeCount);
        respVO.setPendingFollowCount(counter.pendingFollowCount);
        respVO.setUrgentCount(counter.urgentCount);
        respVO.setRepliedCount(counter.repliedCount);
        respVO.setManagerTodoPendingCount(counter.managerTodoPendingCount);
        respVO.setManagerTodoClaimTimeoutCount(counter.managerTodoClaimTimeoutCount);
        respVO.setManagerTodoFirstActionTimeoutCount(counter.managerTodoFirstActionTimeoutCount);
        respVO.setManagerTodoCloseTimeoutCount(counter.managerTodoCloseTimeoutCount);
        respVO.setManagerTodoClosedCount(counter.managerTodoClosedCount);
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

    private Long resolveServiceOrderId(BookingOrderDO order) {
        if (order == null || order.getPayOrderId() == null || order.getPayOrderId() <= 0) {
            return null;
        }
        try {
            List<TradeServiceOrderTraceRespDTO> traces = tradeServiceOrderApi.listTraceByPayOrderId(order.getPayOrderId());
            if (traces == null || traces.isEmpty()) {
                return null;
            }
            for (TradeServiceOrderTraceRespDTO trace : traces) {
                if (trace == null || trace.getServiceOrderId() == null) {
                    continue;
                }
                if (Objects.equals(trace.getSpuId(), order.getSpuId())
                        && Objects.equals(trace.getSkuId(), order.getSkuId())) {
                    return trace.getServiceOrderId();
                }
            }
            return traces.stream()
                    .filter(Objects::nonNull)
                    .map(TradeServiceOrderTraceRespDTO::getServiceOrderId)
                    .filter(Objects::nonNull)
                    .min(Long::compareTo)
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void populateManagerTodoFields(BookingReviewDO review, BookingOrderDO order) {
        if (review == null
                || !BookingReviewLevelEnum.NEGATIVE.getLevel().equals(review.getReviewLevel())
                || review.getSubmitTime() == null) {
            return;
        }
        review.setNegativeTriggerType(BookingReviewNegativeTriggerTypeEnum.REVIEW_LEVEL_NEGATIVE.getType());
        ProductStoreDO store = null;
        Long storeId = order != null ? order.getStoreId() : review.getStoreId();
        if (storeId != null) {
            try {
                store = productStoreService.getStore(storeId);
            } catch (Exception ignored) {
                store = null;
            }
        }
        review.setManagerContactName(store == null ? null : store.getContactName());
        review.setManagerContactMobile(store == null ? null : store.getContactMobile());
        review.setManagerTodoStatus(BookingReviewManagerTodoStatusEnum.PENDING_CLAIM.getStatus());
        review.setManagerClaimDeadlineAt(review.getSubmitTime().plusMinutes(10));
        review.setManagerFirstActionDeadlineAt(review.getSubmitTime().plusMinutes(30));
        review.setManagerCloseDeadlineAt(review.getSubmitTime().plusHours(24));
    }

    private BookingReviewDO ensureManagerTodoReady(BookingReviewDO review) {
        if (review == null) {
            throw exception(BOOKING_REVIEW_NOT_EXISTS);
        }
        if (review.getManagerTodoStatus() != null) {
            return review;
        }
        if (!BookingReviewLevelEnum.NEGATIVE.getLevel().equals(review.getReviewLevel())) {
            throw exception(BOOKING_REVIEW_NOT_ELIGIBLE);
        }
        BookingOrderDO order = review.getBookingOrderId() == null ? null : bookingOrderMapper.selectById(review.getBookingOrderId());
        populateManagerTodoFields(review, order);
        bookingReviewMapper.updateById(review);
        return review;
    }

    private void assertManagerTodoStatus(BookingReviewDO review, Integer... allowedStatuses) {
        if (review == null || review.getManagerTodoStatus() == null) {
            throw exception(BOOKING_REVIEW_NOT_ELIGIBLE);
        }
        for (Integer allowedStatus : allowedStatuses) {
            if (Objects.equals(review.getManagerTodoStatus(), allowedStatus)) {
                return;
            }
        }
        throw exception(BOOKING_REVIEW_NOT_ELIGIBLE);
    }

    private PageResult<BookingReviewDO> buildPageResult(List<BookingReviewDO> list, BookingReviewPageReqVO reqVO) {
        int pageNo = reqVO.getPageNo() == null || reqVO.getPageNo() <= 0 ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null || reqVO.getPageSize() <= 0 ? 10 : reqVO.getPageSize();
        int fromIndex = Math.max((pageNo - 1) * pageSize, 0);
        if (fromIndex >= list.size()) {
            return new PageResult<>(Collections.emptyList(), (long) list.size());
        }
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return new PageResult<>(list.subList(fromIndex, toIndex), (long) list.size());
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

    private BookingReviewPageReqVO buildHistoryScanBaseQuery(BookingReviewHistoryScanReqVO reqVO) {
        BookingReviewPageReqVO pageReqVO = new BookingReviewPageReqVO();
        pageReqVO.setStoreId(reqVO.getStoreId());
        pageReqVO.setBookingOrderId(reqVO.getBookingOrderId());
        pageReqVO.setSubmitTime(reqVO.getSubmitTime());
        return pageReqVO;
    }

    private BookingReviewHistoryScanItemRespVO buildHistoryScanItem(BookingReviewDO review) {
        BookingReviewHistoryScanItemRespVO item = new BookingReviewHistoryScanItemRespVO();
        item.setReviewId(review.getId());
        item.setBookingOrderId(review.getBookingOrderId());
        item.setTechnicianId(review.getTechnicianId());
        item.setMemberId(review.getMemberId());
        item.setSubmitTime(review.getSubmitTime());
        item.setManagerTodoStatus(review.getManagerTodoStatus());

        if (!BookingReviewLevelEnum.NEGATIVE.getLevel().equals(review.getReviewLevel())
                || review.getManagerTodoStatus() != null) {
            item.setStoreId(resolveHistoryScanStoreId(review, null));
            item.setRiskCategory(HISTORY_SCAN_OUT_OF_SCOPE);
            item.setRiskReasons(Collections.singletonList(resolveOutOfScopeReason(review)));
            item.setRiskSummary("不属于历史未初始化差评治理范围");
            return item;
        }

        BookingOrderDO order = review.getBookingOrderId() == null ? null : bookingOrderMapper.selectById(review.getBookingOrderId());
        Long resolvedStoreId = resolveHistoryScanStoreId(review, order);
        item.setStoreId(resolvedStoreId);

        List<String> riskReasons = new ArrayList<>();
        if (review.getSubmitTime() == null) {
            riskReasons.add("提交时间缺失");
        }
        if (resolvedStoreId == null) {
            riskReasons.add("门店ID缺失");
        } else {
            ProductStoreDO store = getStoreQuietly(resolvedStoreId);
            if (store == null) {
                riskReasons.add("门店主数据不存在");
            } else if (StrUtil.isBlank(store.getContactName()) || StrUtil.isBlank(store.getContactMobile())) {
                riskReasons.add("门店联系人缺失");
            }
        }

        if (riskReasons.isEmpty()) {
            item.setRiskCategory(HISTORY_SCAN_MANUAL_READY);
            item.setRiskReasons(Collections.singletonList("满足人工推进前置条件"));
            item.setRiskSummary("可人工进入详情页推进，不代表系统已修复");
            return item;
        }

        item.setRiskCategory(HISTORY_SCAN_HIGH_RISK);
        item.setRiskReasons(riskReasons);
        item.setRiskSummary("需先核实订单、门店或联系人真值");
        return item;
    }

    private BookingReviewHistoryScanSummaryRespVO buildHistoryScanSummary(List<BookingReviewHistoryScanItemRespVO> items) {
        BookingReviewHistoryScanSummaryRespVO summary = new BookingReviewHistoryScanSummaryRespVO();
        summary.setScannedCount((long) items.size());
        summary.setManualReadyCount(items.stream()
                .filter(item -> StrUtil.equals(item.getRiskCategory(), HISTORY_SCAN_MANUAL_READY))
                .count());
        summary.setHighRiskCount(items.stream()
                .filter(item -> StrUtil.equals(item.getRiskCategory(), HISTORY_SCAN_HIGH_RISK))
                .count());
        summary.setOutOfScopeCount(items.stream()
                .filter(item -> StrUtil.equals(item.getRiskCategory(), HISTORY_SCAN_OUT_OF_SCOPE))
                .count());
        return summary;
    }

    private List<BookingReviewHistoryScanItemRespVO> filterHistoryScanItems(List<BookingReviewHistoryScanItemRespVO> items,
                                                                            String riskCategory) {
        if (StrUtil.isBlank(riskCategory)) {
            return items;
        }
        return items.stream()
                .filter(item -> StrUtil.equals(item.getRiskCategory(), riskCategory))
                .collect(Collectors.toList());
    }

    private List<BookingReviewHistoryScanItemRespVO> paginateHistoryScanItems(List<BookingReviewHistoryScanItemRespVO> items,
                                                                              BookingReviewHistoryScanReqVO reqVO) {
        int pageNo = reqVO.getPageNo() == null || reqVO.getPageNo() <= 0 ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null || reqVO.getPageSize() <= 0 ? 10 : reqVO.getPageSize();
        int fromIndex = Math.max((pageNo - 1) * pageSize, 0);
        if (fromIndex >= items.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private Long resolveHistoryScanStoreId(BookingReviewDO review, BookingOrderDO order) {
        if (order != null && order.getStoreId() != null) {
            return order.getStoreId();
        }
        return review == null ? null : review.getStoreId();
    }

    private ProductStoreDO getStoreQuietly(Long storeId) {
        if (storeId == null) {
            return null;
        }
        try {
            return productStoreService.getStore(storeId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void triggerNegativeReviewNotifyOutbox(BookingReviewDO review) {
        if (!Objects.equals(review.getReviewLevel(), BookingReviewLevelEnum.NEGATIVE.getLevel())) {
            return;
        }
        try {
            bookingReviewNotifyOutboxService.createNegativeReviewCreatedOutbox(review);
        } catch (Exception ex) {
            log.warn("failed to create booking review notify outbox, reviewId={}", review.getId(), ex);
        }
    }

    private String resolveOutOfScopeReason(BookingReviewDO review) {
        if (review == null) {
            return "记录不存在";
        }
        if (!BookingReviewLevelEnum.NEGATIVE.getLevel().equals(review.getReviewLevel())) {
            return "非差评记录";
        }
        if (review.getManagerTodoStatus() != null) {
            return "已有店长待办状态";
        }
        return "不在本轮范围";
    }

    private static String resolveManagerSlaStatus(BookingReviewDO review, LocalDateTime now) {
        if (review == null || review.getManagerTodoStatus() == null) {
            return null;
        }
        if (BookingReviewManagerTodoStatusEnum.CLOSED.getStatus().equals(review.getManagerTodoStatus())) {
            return MANAGER_SLA_NORMAL;
        }
        if (review.getManagerCloseDeadlineAt() != null && now.isAfter(review.getManagerCloseDeadlineAt())) {
            return MANAGER_SLA_CLOSE_TIMEOUT;
        }
        if (review.getManagerFirstActionAt() == null
                && review.getManagerFirstActionDeadlineAt() != null
                && now.isAfter(review.getManagerFirstActionDeadlineAt())) {
            return MANAGER_SLA_FIRST_ACTION_TIMEOUT;
        }
        if (review.getManagerClaimedAt() == null
                && review.getManagerClaimDeadlineAt() != null
                && now.isAfter(review.getManagerClaimDeadlineAt())) {
            return MANAGER_SLA_CLAIM_TIMEOUT;
        }
        return MANAGER_SLA_NORMAL;
    }

    private static class BookingReviewCounter {

        private long positiveCount;
        private long neutralCount;
        private long negativeCount;
        private long pendingFollowCount;
        private long urgentCount;
        private long repliedCount;
        private long managerTodoPendingCount;
        private long managerTodoClaimTimeoutCount;
        private long managerTodoFirstActionTimeoutCount;
        private long managerTodoCloseTimeoutCount;
        private long managerTodoClosedCount;

        private void acceptReviewSummary(BookingReviewDO review) {
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

        private void acceptManagerTodoSummary(BookingReviewDO review, LocalDateTime now) {
            if (review == null || review.getManagerTodoStatus() == null) {
                return;
            }
            if (BookingReviewManagerTodoStatusEnum.PENDING_CLAIM.getStatus().equals(review.getManagerTodoStatus())) {
                managerTodoPendingCount++;
            }
            String managerSlaStatus = resolveManagerSlaStatus(review, now);
            if (MANAGER_SLA_CLAIM_TIMEOUT.equals(managerSlaStatus)) {
                managerTodoClaimTimeoutCount++;
            } else if (MANAGER_SLA_FIRST_ACTION_TIMEOUT.equals(managerSlaStatus)) {
                managerTodoFirstActionTimeoutCount++;
            } else if (MANAGER_SLA_CLOSE_TIMEOUT.equals(managerSlaStatus)) {
                managerTodoCloseTimeoutCount++;
            }
            if (BookingReviewManagerTodoStatusEnum.CLOSED.getStatus().equals(review.getManagerTodoStatus())) {
                managerTodoClosedCount++;
            }
        }
    }
}
