# MiniApp Ticket & Audit Linkage Spec v1 (2026-03-08)

## 1. Scope
Covers replay-run ticket sync, four-account warning linkage, and refund-commission anomalies.

## 2. SourceBizNo Rules
- Replay detail ticket: `REFUND_REPLAY_RUN:<runId>:<notifyLogId>`
- Four-account reconcile: `FOUR_ACCOUNT_RECONCILE:<bizDate>`
- Refund commission audit: `REFUND_COMMISSION_AUDIT:<orderId>`

## 3. Required Audit Fields
- `runId`
- `orderId`
- `payRefundId`
- `sourceBizNo`
- `errorCode`

## 4. Fail-Open Contract
- Trade/ticket downstream errors must not rollback replay main result.
- Response must include structured `attempted/success/fail/failedIds`.
- Warning tag `TICKET_SYNC_DEGRADED` must remain searchable.

## 5. Idempotency
- same run + notifyLog already success and no forceResync => SKIP.
- same sourceBizNo upsert should not create duplicated active ticket.
