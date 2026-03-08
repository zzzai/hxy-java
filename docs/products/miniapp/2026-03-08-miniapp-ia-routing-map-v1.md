# MiniApp IA & Routing Map v1 (2026-03-08)

## 1. Scope
- Branch: `feat/ui-four-account-reconcile-ops`
- Runtime baseline: `ui/stitch_/closure_p0`
- Contract baseline: `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`

## 2. Route Map

| Route | Page Type | Required API | Status | Notes |
|---|---|---|---|---|
| `/pages/home/index` | Home | `/promotion/*`, `/product/*` | Planned | From shadcn_4 review, CTA and animation need degrade mode |
| `/pages/pay/result` | Transaction | `GET /trade/order/pay-result` | Available | Degrade field required |
| `/pages/order/list` | Transaction | `GET /trade/order/page`, `GET /trade/order/get-count` | Available | Query/filter stable |
| `/pages/order/detail` | Transaction | `GET /trade/order/get-detail` | Available | Currently merged in prototype list/detail view |
| `/pages/after-sale/create` | Service | `POST /trade/after-sale/create` | Available | Must show business errors by code |
| `/pages/after-sale/list` | Service | `GET /trade/after-sale/page` | Available | Empty list is valid result |
| `/pages/after-sale/detail` | Service | `GET /trade/after-sale/get` | Available | Shows payRefundId/refundTime |
| `/pages/refund/progress` | Service | `GET /trade/after-sale/refund-progress` | Available | Fallback by after-sale status |
| `/pages/booking/list` | Booking | `GET /booking/order/list`, `GET /booking/order/list-by-status` | Missing UI | Backend ready, UI page still missing |
| `/pages/address/list` | Member | `/member/address/*` | Missing UI | CRUD page missing |
| `/pages/coupon/center` | Promotion | `GET /promotion/coupon-template/page`, `POST /promotion/coupon/take` | Missing UI | Action must bind activity state |
| `/pages/point/mall` | Promotion+Member | `GET /promotion/point-activity/page`, `GET /member/point/record/page` | Missing UI | Need points and record joint view |
| `/pages/error/index` | Fallback | N/A | Prototype only | Keep as global degraded entry |

## 3. Navigation Rules
- Bottom tabs: Home, Orders, Booking, Profile.
- Service shortcuts: After-sale, Refund Progress, Address.
- Promotion shortcuts: Coupon Center, Points Mall.
- Any route with `orderId` or `afterSaleId` must keep query params in navigation history.

## 4. Route Guards
- Auth required routes: all except error page.
- Param guard:
  - order pages require `orderId`.
  - refund progress requires `afterSaleId` or `orderId`.
- Guard failure action:
  - show error code if available;
  - fallback to `/pages/error/index` with retry action.

## 5. Release Acceptance
- P0 release requires all rows in Route Map marked Available or explicit Missing UI waiver approved.
- Missing UI waiver is only allowed for non-blocking routes; current blocking routes: booking/address/coupon/point.
