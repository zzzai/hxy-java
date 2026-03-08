package com.hxy.module.booking.service.support;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 财务链路结构化日志字段校验与兜底。
 */
public final class FinanceLogFieldValidator {

    private static final String DEFAULT_RUN_ID = "NO_RUN";
    private static final Long DEFAULT_ORDER_ID = -1L;
    private static final Long DEFAULT_PAY_REFUND_ID = -1L;
    private static final String DEFAULT_SOURCE_BIZ_NO = "NO_SOURCE_BIZ_NO";
    private static final String DEFAULT_ERROR_CODE = "UNKNOWN";

    private FinanceLogFieldValidator() {
    }

    public static FinanceLogFields validate(String runId, Long orderId, Long payRefundId,
                                            String sourceBizNo, String errorCode) {
        List<String> missingFields = new ArrayList<>();
        if (runId == null) {
            missingFields.add("runId");
        }
        if (orderId == null) {
            missingFields.add("orderId");
        }
        if (payRefundId == null) {
            missingFields.add("payRefundId");
        }
        if (StrUtil.isBlank(sourceBizNo)) {
            missingFields.add("sourceBizNo");
        }
        if (StrUtil.isBlank(errorCode)) {
            missingFields.add("errorCode");
        }
        return new FinanceLogFields(
                StrUtil.blankToDefault(StrUtil.trim(runId), DEFAULT_RUN_ID),
                orderId == null ? DEFAULT_ORDER_ID : orderId,
                payRefundId == null ? DEFAULT_PAY_REFUND_ID : payRefundId,
                StrUtil.blankToDefault(StrUtil.trim(sourceBizNo), DEFAULT_SOURCE_BIZ_NO),
                StrUtil.blankToDefault(StrUtil.trim(errorCode), DEFAULT_ERROR_CODE),
                missingFields.isEmpty(),
                missingFields
        );
    }

    public static final class FinanceLogFields {
        private final String runId;
        private final Long orderId;
        private final Long payRefundId;
        private final String sourceBizNo;
        private final String errorCode;
        private final boolean complete;
        private final List<String> missingFields;

        private FinanceLogFields(String runId, Long orderId, Long payRefundId,
                                 String sourceBizNo, String errorCode,
                                 boolean complete, List<String> missingFields) {
            this.runId = runId;
            this.orderId = orderId;
            this.payRefundId = payRefundId;
            this.sourceBizNo = sourceBizNo;
            this.errorCode = errorCode;
            this.complete = complete;
            this.missingFields = missingFields;
        }

        public String getRunId() {
            return runId;
        }

        public Long getOrderId() {
            return orderId;
        }

        public Long getPayRefundId() {
            return payRefundId;
        }

        public String getSourceBizNo() {
            return sourceBizNo;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public boolean isComplete() {
            return complete;
        }

        public List<String> getMissingFields() {
            return Collections.unmodifiableList(missingFields);
        }
    }
}
