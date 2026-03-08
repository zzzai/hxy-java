# MiniApp ErrorCode Recovery Matrix v1 (2026-03-08)

## 1. Goal
Define stable front-end recovery behavior by error code, not by message text.

## 2. Matrix

| Code/Key | Meaning | Front Action | User Action | Escalation |
|---|---|---|---|---|
| `ORDER_NOT_FOUND(1011000011)` | Trade order missing | Keep page alive with empty/error state | Retry + go order list | create ticket after 3 retries |
| `AFTER_SALE_NOT_FOUND(1011000100)` | After-sale missing | Show no-data state | Back to after-sale list | log warning |
| `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)` | Child fulfilled item blocks refund | Block submit | Contact support/manual review | open P1 ticket |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` | Refund id conflict | Block replay/notify | Trigger operator review | open P0 ticket |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)` | Run id not found | Keep run page alive | Refresh run list | log audit warning |
| `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | Points rule limit hit | Show rule hint | adjust quantity/selection | none |
| `TICKET_SYNC_DEGRADED` | Downstream ticket system degraded | Keep main chain success | Retry ticket sync later | monitor warning |
| `PAY_ORDER_NOT_FOUND` (degradeReason) | pay order unavailable | Show degraded pay status | Pull-to-refresh | monitor pay dependency |

## 3. UI Error Contract
- Every blocking error must render: code + neutral message + retry action.
- Every degraded error must render: action available + non-blocking warning.
- Do not parse server message to drive business logic.

## 4. Audit Requirement
Error screen/reporting payload must include: `route`, `orderId`, `afterSaleId`, `runId`, `errorCode`.
