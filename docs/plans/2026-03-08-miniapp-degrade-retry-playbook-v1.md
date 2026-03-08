# MiniApp Degrade & Retry Playbook v1 (2026-03-08)

## 1. Principles
- Main business chain first; downstream failure fail-open where contract allows.
- No fake success animations without backend confirmation.
- Retry policy must be explicit and bounded.

## 2. Degrade Scenarios

| Scenario | Detect Signal | Degrade Behavior | Retry |
|---|---|---|---|
| Pay aggregate missing pay-order | `degraded=true` | show WAITING + warning | manual retry, max 3 |
| Refund progress missing pay refund | no pay refund row | fallback by after-sale status | retry every 5s * 3 |
| Ticket sync downstream error | warning tag `TICKET_SYNC_DEGRADED` | keep replay success, mark warning | background retry/job |
| Coupon take timeout | request timeout | do not animate success | user retry with idempotent call |
| Catalog stale after category switch | version mismatch | force refresh list and stock | immediate refresh |

## 3. Client Retry Policy
- Network timeout: exponential backoff 1s/2s/4s.
- Business conflict code: no auto retry, direct operator flow.
- Max auto retries per action: 3.

## 4. Operator SOP
1. Read error code and route context.
2. Query by `orderId/payRefundId/runId` in logs.
3. For replay chain, check run log then detail then ticket sync log.
4. If conflict codes persist, stop retry and escalate.

## 5. Exit Criteria
- No scenario returns blank screen.
- Each scenario has recoverable action.
- All degraded paths are searchable by structured keys.
