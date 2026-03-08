# Finance Release Readiness - Window A (2026-03-08)

## 1. Change Summary
- Added finance contract freeze checklist:
  - `docs/plans/2026-03-08-finance-contract-freeze-checklist.md`
- Added rollback SQL for this batch traceability DDL:
  - `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-08-hxy-technician-commission-traceability-rollback.sql`
- Added structured log field validation utility:
  - `runId/orderId/payRefundId/sourceBizNo/errorCode`
  - file: `FinanceLogFieldValidator`
- Applied structured finance logs to critical paths:
  - booking refund callback entry (`AppBookingOrderController`)
  - booking refund callback core state transition (`BookingOrderServiceImpl`)
  - refund replay/run/ticket sync/reconcile repair (`BookingRefundNotifyLogServiceImpl`)
  - technician commission accrual/reversal (`TechnicianCommissionServiceImpl`)
- Updated governance docs (baseline + ADR + roadmap).

## 2. Contract Freeze Scope
- booking:
  - `/booking/order/update-refunded`
  - `/booking/refund-notify-log/replay`
  - `/booking/refund-notify-log/replay-due`
  - `/booking/refund-notify-log/replay-run-log/*`
  - `/booking/four-account-reconcile/page|get|summary|refund-audit-summary`
- trade-api:
  - `TradeServiceOrderApi.listTraceByPayOrderId`
- Error code and degrade semantics frozen in checklist doc.

## 3. Rollback Plan
- Execute:
  - `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-08-hxy-technician-commission-traceability-rollback.sql`
- Rollback only:
  - `technician_commission` indexes:
    - `idx_technician_commission_order_item_id`
    - `idx_technician_commission_service_order_id`
    - `idx_technician_commission_source_biz_no`
  - columns:
    - `order_item_id`
    - `service_order_id`
    - `source_biz_no`
- Script is idempotent by `information_schema` guard.

## 4. Risk and Notes
- Structured log validation is fail-open (warn + fallback value), no business flow interruption.
- Existing untracked file intentionally untouched by window-A policy.
- Backward compatibility preserved (old replay `id` input still valid).

## 5. Verification
- `git diff --check`
- `check_hxy_naming_guard.sh`
- `check_hxy_memory_guard.sh`
- `run_ops_stageb_p1_local_ci.sh --skip-mysql-init`
- target maven regression set in booking/trade/pay
