package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayRefundNotifyReqDTO;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundRepairCandidateDO;
import com.hxy.module.booking.dal.mysql.BookingRefundNotifyLogMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReconcileQueryMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.service.BookingOrderService;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOT_FOUND;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_LOG_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_STATUS_INVALID;

@Service
@Validated
@Slf4j
public class BookingRefundNotifyLogServiceImpl implements BookingRefundNotifyLogService {

    private static final Pattern REFUND_BIZ_NO_PATTERN = Pattern.compile("^(\\d+)(?:-refund)?$");

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAIL = "fail";

    private static final int DEFAULT_RECONCILE_LIMIT = 200;
    private static final int MAX_RECONCILE_LIMIT = 1000;
    private static final int RETRY_BACKOFF_BASE_MINUTES = 5;
    private static final int RETRY_BACKOFF_MAX_MINUTES = 60;

    @Resource
    private BookingRefundNotifyLogMapper refundNotifyLogMapper;
    @Resource
    private BookingRefundReconcileQueryMapper refundReconcileQueryMapper;
    @Resource
    private BookingOrderService bookingOrderService;

    @Override
    public void recordNotifySuccess(Long orderId, PayRefundNotifyReqDTO notifyReqDTO) {
        persistLogSafely(buildNotifyLog(orderId, notifyReqDTO, STATUS_SUCCESS,
                "", "", 0, null));
    }

    @Override
    public void recordNotifyFailure(Long orderId, PayRefundNotifyReqDTO notifyReqDTO, Throwable throwable) {
        persistLogSafely(buildNotifyLog(orderId, notifyReqDTO, STATUS_FAIL,
                resolveErrorCode(throwable), resolveErrorMsg(throwable), 0,
                LocalDateTime.now().plusMinutes(RETRY_BACKOFF_BASE_MINUTES)));
    }

    @Override
    public PageResult<BookingRefundNotifyLogDO> getNotifyLogPage(BookingRefundNotifyLogPageReqVO reqVO) {
        if (reqVO != null && StrUtil.isNotBlank(reqVO.getStatus())) {
            reqVO.setStatus(reqVO.getStatus().trim().toLowerCase(Locale.ROOT));
        }
        return refundNotifyLogMapper.selectPage(reqVO);
    }

    @Override
    public void replayFailedLog(Long id, Long operatorId) {
        BookingRefundNotifyLogDO logDO = getRequiredLog(id);
        if (!StrUtil.equalsIgnoreCase(STATUS_FAIL, logDO.getStatus())) {
            throw exception(BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID);
        }
        Long orderId = resolveOrderId(logDO);
        Long payRefundId = resolvePayRefundId(logDO);
        int nextRetryCount = ObjectUtil.defaultIfNull(logDO.getRetryCount(), 0) + 1;
        try {
            bookingOrderService.updateOrderRefunded(orderId, payRefundId);
            assertOrderRefundSynced(orderId, payRefundId);
            refundNotifyLogMapper.updateReplaySuccess(logDO.getId(), STATUS_SUCCESS, nextRetryCount);
        } catch (Exception ex) {
            refundNotifyLogMapper.updateReplayFailure(logDO.getId(), STATUS_FAIL, nextRetryCount,
                    LocalDateTime.now().plusMinutes(resolveRetryBackoffMinutes(nextRetryCount)),
                    resolveErrorCode(ex), resolveErrorMsg(ex));
            throw ex;
        }
    }

    @Override
    public int reconcileRefundedOrders(Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, DEFAULT_RECONCILE_LIMIT), MAX_RECONCILE_LIMIT));
        List<BookingRefundRepairCandidateDO> candidates = refundReconcileQueryMapper.selectRepairCandidates(
                PayRefundStatusEnum.SUCCESS.getStatus(),
                BookingOrderStatusEnum.REFUNDED.getStatus(),
                safeLimit);
        if (candidates.isEmpty()) {
            return 0;
        }
        int successCount = 0;
        for (BookingRefundRepairCandidateDO candidate : candidates) {
            try {
                bookingOrderService.updateOrderRefunded(candidate.getOrderId(), candidate.getPayRefundId());
                assertOrderRefundSynced(candidate.getOrderId(), candidate.getPayRefundId());
                persistLogSafely(buildRepairLog(candidate, STATUS_SUCCESS, null));
                successCount++;
            } catch (Exception ex) {
                persistLogSafely(buildRepairLog(candidate, STATUS_FAIL, ex));
                log.error("[reconcileRefundedOrders][repair failed][orderId={}, payRefundId={}]",
                        candidate.getOrderId(), candidate.getPayRefundId(), ex);
            }
        }
        return successCount;
    }

    private BookingRefundNotifyLogDO buildNotifyLog(Long orderId, PayRefundNotifyReqDTO notifyReqDTO, String status,
                                                    String errorCode, String errorMsg, Integer retryCount,
                                                    LocalDateTime nextRetryTime) {
        return new BookingRefundNotifyLogDO()
                .setOrderId(orderId)
                .setMerchantRefundId(StrUtil.blankToDefault(notifyReqDTO == null ? null : notifyReqDTO.getMerchantRefundId(), ""))
                .setPayRefundId(notifyReqDTO == null ? null : notifyReqDTO.getPayRefundId())
                .setStatus(status)
                .setErrorCode(StrUtil.blankToDefault(errorCode, ""))
                .setErrorMsg(StrUtil.blankToDefault(errorMsg, ""))
                .setRawPayload(toRawPayload(notifyReqDTO))
                .setRetryCount(ObjectUtil.defaultIfNull(retryCount, 0))
                .setNextRetryTime(nextRetryTime);
    }

    private BookingRefundNotifyLogDO buildRepairLog(BookingRefundRepairCandidateDO candidate, String status, Throwable throwable) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("source", "RECONCILE_JOB");
        payload.put("orderId", candidate.getOrderId());
        payload.put("merchantRefundId", candidate.getMerchantRefundId());
        payload.put("payRefundId", candidate.getPayRefundId());
        return new BookingRefundNotifyLogDO()
                .setOrderId(candidate.getOrderId())
                .setMerchantRefundId(StrUtil.blankToDefault(candidate.getMerchantRefundId(), ""))
                .setPayRefundId(candidate.getPayRefundId())
                .setStatus(status)
                .setErrorCode(resolveErrorCode(throwable))
                .setErrorMsg(resolveErrorMsg(throwable))
                .setRawPayload(toRawPayload(payload))
                .setRetryCount(0)
                .setNextRetryTime(StrUtil.equals(status, STATUS_FAIL)
                        ? LocalDateTime.now().plusMinutes(RETRY_BACKOFF_BASE_MINUTES)
                        : null);
    }

    private void assertOrderRefundSynced(Long orderId, Long payRefundId) {
        BookingOrderDO latest = bookingOrderService.getOrder(orderId);
        if (latest == null) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        if (!Objects.equals(BookingOrderStatusEnum.REFUNDED.getStatus(), latest.getStatus())
                || !Objects.equals(payRefundId, latest.getPayRefundId())
                || latest.getRefundTime() == null) {
            throw exception(BOOKING_ORDER_REFUND_STATUS_INVALID);
        }
    }

    private BookingRefundNotifyLogDO getRequiredLog(Long id) {
        BookingRefundNotifyLogDO logDO = refundNotifyLogMapper.selectById(id);
        if (logDO == null) {
            throw exception(BOOKING_ORDER_REFUND_NOTIFY_LOG_NOT_EXISTS);
        }
        return logDO;
    }

    private Long resolveOrderId(BookingRefundNotifyLogDO logDO) {
        if (logDO.getOrderId() != null && logDO.getOrderId() > 0) {
            return logDO.getOrderId();
        }
        return parseOrderId(logDO.getMerchantRefundId());
    }

    private Long resolvePayRefundId(BookingRefundNotifyLogDO logDO) {
        if (logDO.getPayRefundId() == null || logDO.getPayRefundId() <= 0) {
            throw exception(BOOKING_ORDER_REFUND_NOT_FOUND);
        }
        return logDO.getPayRefundId();
    }

    private Long parseOrderId(String merchantRefundId) {
        Matcher matcher = REFUND_BIZ_NO_PATTERN.matcher(merchantRefundId == null ? "" : merchantRefundId.trim());
        if (!matcher.matches()) {
            throw exception(BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID);
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw exception(BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID);
        }
    }

    private int resolveRetryBackoffMinutes(int retryCount) {
        int candidate = Math.max(1, retryCount) * RETRY_BACKOFF_BASE_MINUTES;
        return Math.min(candidate, RETRY_BACKOFF_MAX_MINUTES);
    }

    private String resolveErrorCode(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        if (throwable instanceof ServiceException) {
            return String.valueOf(((ServiceException) throwable).getCode());
        }
        return throwable.getClass().getSimpleName();
    }

    private String resolveErrorMsg(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        String message = throwable.getMessage();
        if (StrUtil.isBlank(message)) {
            message = throwable.getClass().getSimpleName();
        }
        return StrUtil.maxLength(message, 512);
    }

    private String toRawPayload(Object payload) {
        if (payload == null) {
            return "";
        }
        try {
            return JsonUtils.toJsonString(payload);
        } catch (Exception ex) {
            return String.valueOf(payload);
        }
    }

    private void persistLogSafely(BookingRefundNotifyLogDO logDO) {
        try {
            refundNotifyLogMapper.insert(logDO);
        } catch (Exception ex) {
            log.error("[persistLogSafely][insert refund notify log failed][orderId={}, merchantRefundId={}, payRefundId={}]",
                    logDO.getOrderId(), logDO.getMerchantRefundId(), logDO.getPayRefundId(), ex);
        }
    }
}
