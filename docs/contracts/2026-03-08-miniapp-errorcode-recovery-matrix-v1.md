# MiniApp ErrorCode Recovery Matrix v1 (2026-03-08)

## 1. Goal
Define stable front-end and support recovery behavior by error code (or canonical key), not by message text.

## 2. Matrix

| Code/Key | Meaning | Front Action | User Action | Escalation |
|---|---|---|---|---|
| `ORDER_NOT_FOUND(1011000011)` | Trade order missing | Keep page alive with empty/error state | Retry + go order list | create ticket after 3 retries |
| `AFTER_SALE_NOT_FOUND(1011000100)` | After-sale missing | Show no-data state | Back to after-sale list | log warning |
| `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)` | Child fulfilled item blocks refund | Block submit | Contact support/manual review | open P1 ticket |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` | Refund id conflict | Block replay/notify | Trigger operator review | open P0 ticket |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)` | Run id not found | Keep run page alive | Refresh run list | log audit warning |
| `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | Points rule limit hit | Show rule hint | adjust quantity/selection | none |
| `TICKET_SYNC_DEGRADED` | Downstream ticket system degraded | Keep main chain success + warning | Retry ticket sync later | monitor warning |
| `PAY_ORDER_NOT_FOUND` (degradeReason) | pay order unavailable | Show degraded pay status (`WAITING + warning`) | Pull-to-refresh | monitor pay dependency |
| `CATEGORY_NOT_EXISTS(1008001000)` | Catalog category missing | Fallback to default category | Switch category / refresh | log warning |
| `SKU_STOCK_NOT_ENOUGH(1008006004)` | SKU stock insufficient | Block submit and show available quantity | Reduce quantity and retry | none |
| `STORE_SKU_STOCK_BIZ_KEY_CONFLICT(1008009006)` | Store stock idempotency conflict | Block action, require refresh | Re-enter after data refresh | open P1 ops ticket if persistent |
| `SCHEDULE_CONFLICT(1030002001)` | Booking schedule conflict | Fail-fast and show alternative slots | Pick another slot | none |
| `TIME_SLOT_NOT_AVAILABLE(1030003001)` | Slot unavailable | Show slot unavailable state | Re-select time slot | none |
| `TIME_SLOT_ALREADY_BOOKED(1030003002)` | Slot already booked | Refresh slot list | Choose another slot | none |
| `COUPON_NOT_EXISTS(1013005000)` | Coupon record missing | Keep asset page alive with partial data | Refresh coupon list | log warning |
| `USER_NOT_EXISTS(1004001000)` | User identity invalid/missing | Redirect to auth flow | Re-login | open auth ticket if recurring |
| `TECHNICIAN_NOT_EXISTS(1030001000)` | Technician record missing | Keep feed/list alive, remove invalid card | Pick another technician | log warning |
| `TECHNICIAN_DISABLED(1030001001)` | Technician disabled | Keep list alive, mark unavailable | Pick another technician | none |
| `MINIAPP_HOME_ACTIVITY_CONTEXT_MISMATCH(1008009901)` | Home activity context mismatch | Force refresh and degrade card rendering | Retry later | reserved-disabled; if returned, raise P1 config incident |
| `MINIAPP_CATALOG_VERSION_MISMATCH(1008009902)` | Catalog version snapshot mismatch | Force full catalog refresh | Retry after refresh | reserved-disabled; if returned, raise P1 config incident |
| `MINIAPP_ADDON_INTENT_CONFLICT(1008009903)` | Add-on/cart intent conflict | Block and echo server-side accepted quantity | Re-submit with latest quantity | reserved-disabled; if returned, operator review |
| `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901)` | Asset ledger inconsistency | Show partial asset ledger with degrade tag | Retry later | reserved-disabled; if returned, open P1 reconciliation ticket |
| `MINIAPP_SEARCH_QUERY_INVALID(1008009904)` | Search query invalid | Keep query and show correction hint | Fix query and retry | reserved-disabled; if returned, verify validation rollout |
| `GIFT_CARD_ORDER_NOT_FOUND(1011009901)` | Gift-card order not found | Keep page alive with order-not-found state | Back to gift-card order list | reserved-disabled; if returned, open P1 trade ticket |
| `GIFT_CARD_REDEEM_CONFLICT(1011009902)` | Gift-card redeem conflict | Block redeem action | Contact support for manual check | reserved-disabled; if returned, open P1 trade ticket |
| `REFERRAL_BIND_CONFLICT(1013009901)` | Referral bind conflict | Block bind and show rules | Contact support | reserved-disabled; if returned, open P1 promotion ticket |
| `REFERRAL_REWARD_LEDGER_MISMATCH(1013009902)` | Referral reward ledger mismatch | Show reward `PROCESSING` with warning | Retry later | reserved-disabled; if returned, open P1 ledger ticket |
| `TECHNICIAN_FEED_AUDIT_BLOCKED(1030009901)` | Technician feed audit blocked | Keep browse flow, hide blocked content | Retry later | reserved-disabled; if returned, open P1 content ticket |

## 3. UI Error Contract
- Every blocking error must render: `code + neutral message + retry/next action`.
- Every degraded error must render: action available + non-blocking warning (`degraded` or warning tag).
- Do not parse server message text to drive business logic.

## 4. Retry & Degrade Alignment
- Align to `miniapp-degrade-retry-playbook`:
  - Network timeout: exponential backoff `1s/2s/4s`.
  - Business conflict code: no auto retry, go operator flow.
  - Max auto retries per action: `3`.
  - Main chain first; downstream failure fail-open where contract allows.

## 5. Audit Requirement
Error reporting payload must include: `route`, `orderId`, `afterSaleId`, `runId`, `errorCode`.
