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
| Home Growth | `/pages/home/index` | `/promotion/*`, `/product/*` | Activity truth binding | `1008009901 (RESERVED_DISABLED)` | low-end motion degrade | P1 | Product+Promotion | PARTIAL (API wildcard unresolved) |
| Service Catalog Sync | `/pages/service/catalog` | Product catalog APIs | Catalog snapshot/version | `1008009902 (RESERVED_DISABLED)` | mismatch => force refresh | P1 | Product | PARTIAL (API wildcard unresolved) |
| Add/Book Conflict | `/pages/service/catalog` | Add-to-cart/booking intent APIs | Booking/Product state | `1008009903 (RESERVED_DISABLED)` | conflict => server truth override | P1 | Booking+Product | PARTIAL (API naming unresolved) |
| Booking Schedule | `/pages/booking/schedule` | Booking schedule APIs | Booking slot state | `1030002001/1030003001/1030003002` | conflict => fail-fast + slots | P1 | Booking | PASS |
| Asset Ledger | `/pages/profile/assets` | Coupon/point/member asset APIs | Unified asset ledger | `1004009901 (RESERVED_DISABLED)` | partial degrade + retriable | P1 | Member+Promotion | PARTIAL (API naming unresolved) |
| Search Discovery | `/pages/search/index` | Search APIs | Search state | `1008009904 (RESERVED_DISABLED)` | keep query + retriable | P1 | Product | PARTIAL (API naming unresolved) |
| Gift Card | `/pages/gift-card/*` | Gift-card domain APIs | Gift-card lifecycle state | `1011009901/1011009902 (RESERVED_DISABLED)` | no fake success before backend ack | P2 (matrix) / P0 (business PRD) | Trade+Member | PARTIAL (priority drift) |
| Referral | `/pages/referral/*` | Referral domain APIs | Referral reward ledger | `1013009901/1013009902 (RESERVED_DISABLED)` | delayed credit => processing state | P2 (matrix) / P0 (business PRD) | Promotion | PARTIAL (priority drift) |
| Technician Feed | `/pages/technician/feed` | Feed/post/comment APIs | Moderation state | `1030009901 (RESERVED_DISABLED)` | audit block => fail-close | P2 (matrix) / P1 (product policy) | Booking+Content Ops | PARTIAL (priority drift) |

## 3. Conflict List

### C-001 Matrix Code Mapping Not Backfilled
- Location: `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
- Current status: canonical register and P1/P2 closure doc已完成，但矩阵主表仍保留占位写法。
- Impact: read path already available, but release matrix still not single-source truth.

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
| R-001 | Replace all `TBD_*` with canonical error codes or reserved code ranges with activation rule | C | Canonical register + p1/p2 closure delivered (`047dc257ca`) | Done (pending matrix backfill by A/B) |
| R-002 | Align gift-card/referral/feed priority to one source of truth and sync index/matrix/PRD | A + B | Priority alignment patch | Before go-live decision |
| R-003 | Replace wildcard API names with concrete endpoint lists and request/response key fields | B + C | PRD+contract sync patch | Before UAT start |
| R-004 | Enforce code-driven degrade handling (no message-driven branching) | A + C + D | Error handling convention update + metric mapping | Before stage gate |
| R-005 | Add gate checks for doc consistency (priority, route, API, code anchor completeness) | A + D | Doc consistency gate script/spec | Before Frozen final sign-off |

## 5. Release Blockers and Go/No-Go

### 5.1 Blockers (No-Go)
1. Release matrix 主表仍有占位错误码写法，未完全回填 canonical code。
2. Priority mismatch for the same domain across matrix/PRD/contract.
3. Route/API wildcards remain in release-bound documents.
4. RESERVED_DISABLED 码上线开关与禁用态未纳入统一门禁脚本。

### 5.2 Go Conditions
1. 矩阵文档回填 canonical code，`TBD_*` 在 release matrix 归零。
2. Priority conflicts resolved and reflected consistently in index + matrix + PRD pack.
3. All release-scoped APIs are concrete endpoints with request/response key fields.
4. CS SOP and operation playbook can branch only by stable codes.
5. `degraded=true` traffic is traceable and excluded from success-rate main denominator.

## 6. Audit Conclusion
- Current decision: **Conditional Go**.
- Rationale:
  - P0 baseline paths are largely frozen and executable.
  - P1/P2 expansion still has blockers（priority/API naming drift + reserved-disabled gate）。
  - Must clear blockers in Section 5.1 before full release freeze sign-off.
