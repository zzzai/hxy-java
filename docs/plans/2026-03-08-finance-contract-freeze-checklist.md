# Finance Partial Closure Contract Freeze Checklist (2026-03-08)

## 1. Scope
- Branch: `feat/ui-four-account-reconcile-ops`
- Domain: booking/trade/pay backend contracts
- Goal: freeze interface fields, error codes, and degrade semantics before release

## 2. Interface Contract Freeze

### 2.1 Refund callback and replay (booking)

1. `POST /booking/order/update-refunded`
- Request: `PayRefundNotifyReqDTO`
- Required contract:
  - `merchantRefundId`: supports `{orderId}-refund` and `{orderId}`
  - `payRefundId`: required for refund sync
- Response: `CommonResult<Boolean>`
- Strong rules:
  - refund record must exist and be `SUCCESS`
  - refund amount must equal booking `payPrice`
  - `merchantOrderId` and `merchantRefundId` must match booking order

2. `GET /booking/refund-notify-log/page`
- Filter freeze:
  - `status/merchantRefundId/payRefundId/createTime/retryCount` (existing fields remain compatible)

3. `POST /booking/refund-notify-log/replay`
- Backward compatibility:
  - supports old `id`
  - supports new `ids` and `dryRun`
- Response freeze:
  - `successCount/skipCount/failCount/details[]`
  - `details`: `id/orderId/payRefundId/resultStatus/resultCode/resultMsg`

4. `POST /booking/refund-notify-log/replay-due`
- Request freeze: `limit/dryRun`
- Response freeze: includes `runId` + replay aggregate + `details[]`

5. `GET /booking/refund-notify-log/replay-run-log/page|get|summary`
- Summary freeze:
  - `runId/runStatus/triggerSource/operator/dryRun/startTime/endTime`
  - `scannedCount/successCount/skipCount/failCount`
  - `warningCount/topFailCodes/topWarningTags`
  - `ticketSyncSuccessCount/ticketSyncSkipCount/ticketSyncFailCount`

6. `GET /booking/refund-notify-log/replay-run-log/detail/page|get`
- Detail freeze:
  - `runId/notifyLogId/orderId/payRefundId`
  - `resultStatus/resultCode/resultMsg/warningTag`
  - `ticketSyncStatus/ticketId/ticketSyncTime/ticketSyncErrorCode/ticketSyncErrorMsg`

7. `POST /booking/refund-notify-log/replay-run-log/sync-tickets`
- Request freeze:
  - required `runId`
  - optional `onlyFail` (default `true`)
  - optional `dryRun` (default `false`)
  - optional `forceResync` (default `false`)
- Response freeze:
  - `attemptedCount/successCount/skipCount/failedCount/failedIds`
  - `details[]`: `notifyLogId/resultCode/resultMsg/ticketId`

### 2.2 Four-account (booking)

1. `GET /booking/four-account-reconcile/page|get`
- Additional response fields remain additive:
  - `payRefundId/refundTime/refundLimitSource`
  - `refundExceptionType/refundAuditStatus/refundAuditRemark/refundEvidenceJson`

2. `GET /booking/four-account-reconcile/summary`
- Aggregate response freeze:
  - `totalCount/passCount/warnCount`
  - `tradeMinusFulfillmentSum/tradeMinusCommissionSplitSum`
  - `commissionAmountSum/commissionDifferenceAbsSum`
  - `unresolvedTicketCount/ticketSummaryDegraded`

3. `GET /booking/four-account-reconcile/refund-audit-summary`
- Aggregate response freeze:
  - `totalCount/differenceAmountSum`
  - `statusAgg/exceptionTypeAgg`
  - `unresolvedTicketCount/ticketSummaryDegraded`

### 2.3 Trade API for traceability

1. `TradeServiceOrderApi.listTraceByPayOrderId(Long payOrderId)`
- Response DTO freeze: `TradeServiceOrderTraceRespDTO`
  - `serviceOrderId/orderId/orderItemId/spuId/skuId/status`

## 3. Error Code Freeze

1. `BOOKING_ORDER_STATUS_ERROR (1030004001)`
- Used for invalid booking state transition on refund callback.

2. `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT (1030004008)`
- Used when already-refunded booking receives a different `payRefundId`.

3. `BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID (1030004010)`
- Used when `merchantRefundId` cannot parse to a valid booking order id.

4. `COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT (1030007012)`
- Used when same accrual idempotent key maps to different amount payload.

5. `COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT (1030007005)`
- Used when same reversal idempotent key maps to different amount payload.

## 4. Degrade/Fallback Semantics Freeze

1. Refund callback -> commission reversal:
- fail-open, do not rollback refund status update.

2. Refund replay -> four-account refresh:
- fail-open, append warning marker in replay detail remark.

3. Replay run -> trade ticket sync:
- fail-open, return `failedIds/details`, do not fail whole batch.

4. Four-account summary -> trade ticket summary query:
- fail-open, return summary with `ticketSummaryDegraded=true`.

5. Refund evidence JSON parse:
- invalid JSON does not return 500; keep raw string and parse-error marker.

## 5. Backward Compatibility Guard

1. Keep existing API paths and old request fields working (`id` in replay).
2. New fields are additive; existing response semantics unchanged.
3. Existing state machine semantics remain stable:
- `PAID -> REFUNDED` mainline
- already-refunded same `payRefundId` idempotent success
- already-refunded different `payRefundId` conflict error
