# MiniApp Unified State Machine v1 (2026-03-08)

## 1. Goal
Unify state transitions across booking, trade, pay, refund, coupon, points, member to avoid contradictory UI states.

## 2. Canonical Entity States

### 2.1 Booking Order
- `CREATED -> PAID -> SERVING -> FINISHED`
- Refund side path: `PAID|SERVING|FINISHED -> REFUND_PENDING -> REFUNDED`
- Invalid transition returns business error, no silent success.

### 2.2 Trade Order / Pay Order
- Trade payment view: `WAITING | SUCCESS | REFUNDED | CLOSED`
- Pay order status drives trade aggregate when available.
- If pay order is missing, return degraded result, not 500.

### 2.3 After-sale / Refund
- After-sale: `APPLY -> REVIEWING -> APPROVED -> REFUNDING -> REFUNDED` or `REJECTED`
- Refund progress code: `REFUND_PENDING | REFUND_PROCESSING | REFUND_SUCCESS | REFUND_FAILED`
- Child ledger fulfilled item must block refund with stable code.

### 2.4 Coupon
- Template: `AVAILABLE | EXPIRED | OUT_OF_STOCK`
- User coupon: `UNUSED | LOCKED | USED | EXPIRED`
- Coupon animation/success UI can only fire after server `take` success.

### 2.5 Points
- Account operation: `ACCRUAL | CONSUME | REFUND_BACK | ADJUST`
- Points mall activity: `ONLINE | OFFLINE | SOLD_OUT | EXPIRED`

## 3. Idempotency Rules
- Refund notify: same order + same `payRefundId` => idempotent success.
- Refund notify: same order + different `payRefundId` => conflict error.
- Replay run detail: unique key by `runId + notifyLogId`.
- Ticket sync: success history + no forceResync => `SKIP`.

## 4. Truth Source Priority
1. Backend persisted state
2. Aggregated API computed state
3. Client cache (display only, never commit)

## 5. Observability Keys
All critical transitions must be searchable by:
- `runId`
- `orderId`
- `payRefundId`
- `sourceBizNo`
- `errorCode`

## 6. Compatibility
- Additive field only.
- Existing status enum names and semantics are frozen by contract docs.
