# Booking Review Detail Design

**Date:** 2026-03-18
**Status:** Approved design, not implemented
**Scope:** HXY miniapp booking review detail page for member-side history readback

---

## 1. Background And Current Truth

Current repository truth already includes booking review submission, result, list, backend recovery detail, and related app/admin APIs.

What exists today:
- Miniapp routes:
  - `yudao-mall-uniapp/pages/booking/review-add.vue`
  - `yudao-mall-uniapp/pages/booking/review-result.vue`
  - `yudao-mall-uniapp/pages/booking/review-list.vue`
- Frontend review APIs:
  - `GET /booking/review/page`
  - `GET /booking/review/get`
  - `GET /booking/review/summary`
- Backend app controller already exposes detail readback:
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingReviewController.java`
- Admin overlay detail page already reads and displays review images and internal trace fields:
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`

The current member-side gap is narrower than the original booking review domain gap:
- miniapp list page can show history, but cannot open an independent detail page
- miniapp submit page now sends `picUrls`, but member-side history has no dedicated detail surface to read images back
- backend exposes `GET /booking/review/get`, but miniapp pages have not consumed it

Therefore this design is not a new domain. It is a member-side readback completion for the existing booking review domain. It must not be written as a release-ready milestone by itself.

---

## 2. Product Goal

Build a focused booking review detail page so members can revisit the full content of a previously submitted service review in a clean, trustworthy, service-oriented layout.

The product goal is not to add more interaction. The first-order goals are:
- make review history readable and traceable for members
- let members see pictures, tags, and official reply in one place
- close the gap between review submission and review history readback
- keep user-side information clean while leaving internal recovery fields in admin-only views

---

## 3. Non-Goals

This first version does not include:
- review editing or append review
- re-submit review
- contact customer service from the review detail page
- compensation claim entry
- store manager notification entry
- exposing `serviceOrderId`, `riskLevel`, `auditStatus`, or `followStatus` to members
- changing release truth from `Can Develop / Cannot Release`

---

## 4. Options Considered

### Option A: Add Independent Review Detail Page

Pros:
- best information hierarchy
- matches mature service-brand patterns: list -> detail
- lets us reuse existing `GET /booking/review/get`
- easy to extend later with service actions without polluting the list page

Cons:
- requires new route, new page, and new tests

Decision: approved.

### Option B: Expand Review Cards Inline In `review-list`

Pros:
- lower implementation cost
- no new route needed

Cons:
- long comments, pictures, and reply blocks would make the list page heavy
- weak detail reading experience
- mixes browsing and deep reading in one surface

Decision: reject.

### Option C: Reuse `review-result` As History Detail

Pros:
- smallest page count

Cons:
- result page and history detail have different user intent
- success/failure semantics would become mixed
- harder to maintain later

Decision: reject.

---

## 5. Product Principles

The approved design follows these principles:
- service-first tone: the page should read like a service record, not a social content feed
- clear hierarchy: rating and summary first, details second, reply last
- truthful scope: only show fields already exposed by the real app API
- internal/public separation: internal recovery fields stay in admin detail, not in member detail
- no release inflation: this page can improve completeness without being used as proof of release readiness

---

## 6. User Flow

### 6.1 Entry
- Only one entry in v1:
  - tap a review card from `/pages/booking/review-list`

### 6.2 Navigation
- `/pages/booking/review-list` -> `/pages/booking/review-detail?id=<reviewId>`
- review detail page supports:
  - return to my review list
  - view order detail via `bookingOrderId`

### 6.3 Failure Cases
- missing or invalid `id` -> show explicit empty/error state
- `GET /booking/review/get` non-zero -> show failure state, not blank page
- image array empty -> legal empty state, hide image grid
- reply empty -> legal empty state, do not fake a reply

---

## 7. Information Architecture

The review detail page should be structured as four blocks:
- Header block: review level, submit time, booking order id
- Score block: overall score plus service / technician / environment scores
- Content block: tags, text content, pictures
- Reply block: official reply content

This sequence matches how members naturally revisit a service review:
- What was this review?
- How did I score it?
- What exactly did I say?
- Has the merchant replied?

---

## 8. Member-Side Data Scope

### 8.1 Fields To Show
- `reviewLevel`
- `submitTime`
- `bookingOrderId`
- `overallScore`
- `serviceScore`
- `technicianScore`
- `environmentScore`
- `tags[]`
- `content`
- `picUrls[]`
- `replyContent`

### 8.2 Fields To Keep Hidden From Members
- `serviceOrderId`
- `storeId`
- `technicianId`
- `memberId`
- `riskLevel`
- `displayStatus`
- `followStatus`
- `auditStatus`
- `replyUserId`
- `followOwnerId`

### 8.3 Why Hide Them
These fields are useful for operations, escalation, and audit, but they do not improve member understanding. Exposing them would increase noise and weaken the service-oriented product language.

---

## 9. UI Strategy

### 9.1 Visual Direction
The page should feel like a premium service receipt rather than a marketing feed:
- calm visual hierarchy
- generous white space
- warm but restrained accent color
- pictures displayed in a clean grid
- reply card visually separated from original review content

### 9.2 Block Order
1. review level + submit time
2. order id and overall score
3. dimension scores
4. tags
5. review text
6. pictures
7. official reply
8. bottom actions

### 9.3 Empty State Language
Suggested member-facing language:
- invalid id / missing record: `评价不存在或参数异常`
- no reply: `商家暂未回复`
- empty text: `用户未填写文字评价`

---

## 10. API And Runtime Truth

### 10.1 Reused API
- `GET /booking/review/get`

### 10.2 Current Runtime Assumptions
- request param stays `id`, not `reviewId`
- `picUrls=[]` is legal empty state
- `replyContent` may be empty
- current miniapp does not consume any service-side `degraded=true / degradeReason`

### 10.3 Truth Constraints
- do not invent new app endpoints
- do not change current booking review error-code truth
- do not expose admin-only trace fields to members

---

## 11. Testing Strategy

### 11.1 Required Smoke Coverage
- `pages.json` registers `review-detail`
- `review-list.vue` navigates to `review-detail`
- `review-detail.vue` uses `BookingReviewApi.getReview`
- `review-detail.vue` renders image / reply / score blocks

### 11.2 Required Failure Coverage
- invalid `id` leads to explicit failure/empty state
- empty images do not crash the page
- empty reply does not crash the page

### 11.3 Out Of Scope For This Feature
- backend contract change tests
- runtime release evidence tests
- notification / compensation workflow tests

---

## 12. Docs And Truth Sync

Once implemented, the following truth docs must be updated conservatively:
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`

Required wording discipline:
- may write that member-side review detail route exists
- may write that `GET /booking/review/get` is now consumed by miniapp
- must not write `Can Release = Yes`
- must not infer release readiness from this detail page alone

---

## 13. Final Decision

Approved direction:
- add an independent miniapp review detail page
- limit entry to review history list in v1
- show member-relevant content only
- keep service recovery internals in admin detail
- maintain current global truth: `Doc Closed / Can Develop / Cannot Release`
