# MiniApp Product Doc Consistency Audit v1 (2026-03-09)

## 1. Audit Scope
- Scope: 2026-03-08 baseline freeze pack + 2026-03-09 product/contract/plan extensions.
- Baseline anchors:
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-business-rulebook-v1.md`

## 2. Cross-Doc Consistency Matrix

| Page/Feature | Route | API | State Machine Source | Error Code | Degrade Semantics | P Level | Owner | Consistency |
|---|---|---|---|---|---|---|---|---|
| Pay Result | `/pages/pay/result` | `GET /trade/order/pay-result` | Trade/Pay aggregate | `ORDER_NOT_FOUND(1011000011)` + `PAY_ORDER_NOT_FOUND` | pay order miss => `degraded=true` | P0 | Trade | PASS |
| Order List | `/pages/order/list` | `GET /trade/order/page`, `GET /trade/order/get-count` | Trade order state | `ORDER_NOT_FOUND(1011000011)` | query fail => retry | P0 | Trade | PASS |
| Order Detail | `/pages/order/detail` | `GET /trade/order/get-detail` | Trade order/item state | `ORDER_NOT_FOUND(1011000011)` | sync fail => keep local state | P0 | Trade | PASS |
| After-sale Apply | `/pages/after-sale/create` | `POST /trade/after-sale/create` | After-sale state machine | `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)` | rule block => fail-close with code | P0 | Trade | PASS |
| Refund Progress | `/pages/refund/progress` | `GET /trade/after-sale/refund-progress` | After-sale + pay refund aggregate | `AFTER_SALE_NOT_FOUND(1011000100)` | pay miss => fallback by after-sale state | P0 | Trade | PASS |
| Booking List | `/pages/booking/list` | `GET /booking/order/list`, `GET /booking/order/list-by-status` | Booking order state | `BOOKING_ORDER_NOT_EXISTS` | empty filter => fallback all status | P0 | Booking | PASS |
| Coupon Center | `/pages/coupon/center` | `GET /promotion/coupon-template/page`, `POST /promotion/coupon/take` | Promotion coupon state | `PROMOTION_COUPON_*` | take fail => no success animation | P0 | Promotion | PASS |
| Point Mall | `/pages/point/mall` | `GET /promotion/point-activity/page`, `GET /member/point/record/page` | Promotion/member point ledger | `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | over-limit => fail-close with adjust hint | P0 | Promotion+Member | PASS |
| Home Growth | `/pages/home/index` | `/promotion/*`, `/product/*` | Activity truth binding | `TBD_HOME_ACTIVITY_ERROR_CODE` | low-end motion degrade | P1 | Product+Promotion | FAIL (API/error code unresolved) |
| Service Catalog Sync | `/pages/service/catalog` | Product catalog APIs | Catalog snapshot/version | `TBD_CATALOG_VERSION_MISMATCH` | mismatch => force refresh | P1 | Product | FAIL (error code unresolved) |
| Add/Book Conflict | `/pages/service/catalog` | Add-to-cart/booking intent APIs | Booking/Product state | `TBD_ADD_CONFLICT_CODE` | conflict => server truth override | P1 | Booking+Product | FAIL (contract unresolved) |
| Booking Schedule | `/pages/booking/schedule` | Booking schedule APIs | Booking slot state | `TBD_BOOKING_SLOT_CONFLICT` | conflict => fail-fast + slots | P1 | Booking | FAIL (error code unresolved) |
| Asset Ledger | `/pages/profile/assets` | Coupon/point/member asset APIs | Unified asset ledger | `TBD_ASSET_LEDGER_MISMATCH` | partial degrade + retriable | P1 | Member+Promotion | FAIL (error code unresolved) |
| Search Discovery | `/pages/search/index` | Search APIs | Search state | `TBD_SEARCH_QUERY_INVALID` | keep query + retriable | P1 | Product | FAIL (contract unresolved) |
| Gift Card | `/pages/gift-card/*` | Gift-card domain APIs | Gift-card lifecycle state | `TBD_GIFT_CARD_*` | no fake success before backend ack | P2 (matrix) / P0 (business PRD) | Trade+Member | FAIL (priority conflict + code unresolved) |
| Referral | `/pages/referral/*` | Referral domain APIs | Referral reward ledger | `TBD_REFERRAL_*` | delayed credit => processing state | P2 (matrix) / P0 (business PRD) | Promotion | FAIL (priority conflict + code unresolved) |
| Technician Feed | `/pages/technician/feed` | Feed/post/comment APIs | Moderation state | `TBD_FEED_AUDIT_BLOCK` | audit block => fail-close | P2 (matrix) / P1 (product policy) | Booking+Content Ops | FAIL (priority conflict + code unresolved) |

## 3. Conflict List

### C-001 TBD Error Codes Not Closed
- Location: `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
- Impact: front-end and SOP cannot bind deterministic recovery action.
- Affected: home/catalog/add-conflict/schedule/asset/search/gift-card/referral/feed.

### C-002 Priority Drift Across Docs
- Gift card:
  - Matrix: P2
  - Business PRD: P0
- Referral:
  - Matrix: P2
  - Business PRD: P0
- Technician feed:
  - Matrix: P2
  - Product policy: P1
- Impact: release batch ambiguity, resource allocation mismatch.

### C-003 API Naming Inconsistency
- `feature-inventory-and-release-matrix` still contains wildcard API (`/promotion/*`, `/product/*`, `Search APIs`).
- Impact: test case generation and mock/stub routing cannot be deterministic.

### C-004 Error Surface Inconsistency
- Some paths use code anchors, others rely on non-code degrade reason naming.
- Impact: ops dashboard and CS SOP may branch by message/degrade text instead of error code.

## 4. Remediation Plan and Ownership

| Action ID | Remediation | Owner Window | Expected Output | Due Gate |
|---|---|---|---|---|
| R-001 | Replace all `TBD_*` with canonical error codes or reserved code ranges with activation rule | C | Canonical register + matrix sync | Before RB2 freeze |
| R-002 | Align gift-card/referral/feed priority to one source of truth and sync index/matrix/PRD | A + B | Priority alignment patch | Before go-live decision |
| R-003 | Replace wildcard API names with concrete endpoint lists and request/response key fields | B + C | PRD+contract sync patch | Before UAT start |
| R-004 | Enforce code-driven degrade handling (no message-driven branching) | A + C + D | Error handling convention update + metric mapping | Before stage gate |
| R-005 | Add gate checks for doc consistency (priority, route, API, code anchor completeness) | A + D | Doc consistency gate script/spec | Before Frozen final sign-off |

## 5. Release Blockers and Go/No-Go

### 5.1 Blockers (No-Go)
1. P0/P1 paths still contain `TBD_*` without canonical code mapping.
2. Priority mismatch for the same domain across matrix/PRD/contract.
3. Route/API wildcards remain in release-bound documents.
4. Degrade semantics not mapped to explicit code + recovery action.

### 5.2 Go Conditions
1. `TBD_*` reduced to 0 for RB1/RB2 features.
2. Priority conflicts resolved and reflected consistently in index + matrix + PRD pack.
3. All release-scoped APIs are concrete endpoints with request/response key fields.
4. CS SOP and operation playbook can branch only by stable codes.
5. `degraded=true` traffic is traceable and excluded from success-rate main denominator.

## 6. Audit Conclusion
- Current decision: **Conditional Go**.
- Rationale:
  - P0 baseline paths are largely frozen and executable.
  - P1/P2 expansion still has blockers (TBD code and priority/API naming drift).
  - Must clear blockers in Section 5.1 before full release freeze sign-off.
