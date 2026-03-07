# Finance Partial Closure - Window A (2026-03-08)

## 1. Scope
- Branch: `feat/ui-four-account-reconcile-ops`
- Domain: booking/trade/pay backend + SQL + governance docs
- Excluded: overlay frontend, `.codex`, historical handoffs, unrelated untracked files

## 2. Delivered Changes

### 2.1 Refund notify consistency hardening (booking/pay)
- Unified refund callback consistency handling in booking:
  - `updateOrderRefunded` now rejects invalid booking status transitions (no silent ignore for non-PAID/REFUNDED)
  - conflict/idempotent semantics stay inspectable through stable error-code behavior
- Refund callback + replay path keeps auditability by preserving retriable/failure signals and identifiers

### 2.2 Technician commission accrual end-to-end closure (trade/booking)
- Added traceability fields for commission records:
  - `orderItemId`
  - `serviceOrderId`
  - `sourceBizNo`
- Enforced accrual idempotency key semantics:
  - `bizType=FULFILLMENT_COMPLETE` + `bizNo(sourceBizNo)` + `staffId`
- Added conflict error code for idempotent-key / amount mismatch:
  - `COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT`

### 2.3 Refund reversal commission closure (trade/booking)
- Refund-triggered commission reversal now carries full traceability fields (`orderItemId/serviceOrderId/sourceBizNo`)
- Reversal execution is fail-open for refund mainline:
  - refund status update is not blocked by downstream reversal failure
  - failure is logged for audit/reconciliation follow-up

### 2.4 Reconcile + ticket sync compatibility enhancement (booking -> trade)
- Four-account summary response now includes commission aggregates (backward compatible):
  - `commissionAmountSum`
  - `commissionDifferenceAbsSum`
- Extended trade API trace query contract for stronger booking-side evidence linkage

## 3. SQL/Schema
- Added idempotent migration script:
  - `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-08-hxy-technician-commission-traceability.sql`
- Updated booking test schema fixture to align with new fields:
  - `yudao-module-booking/src/test/resources/sql/create_tables.sql`

## 4. Risks / Notes
- Existing parallel-window dirty files remain untouched by design.
- Commission reversal is fail-open by policy; operations should watch warning/audit logs for compensation tasks.
- API compatibility preserved; additive fields only on summary/trace contracts.

## 5. Verification Commands (Executed)
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-booking,yudao-module-mall/yudao-module-trade,yudao-module-pay -am -Dtest=BookingOrderServiceImplTest,FourAccountReconcileServiceImplTest,FourAccountReconcileControllerTest,AfterSaleReviewTicketServiceImplTest,TechnicianCommissionServiceImplTest,TechnicianCommissionSettlementServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
