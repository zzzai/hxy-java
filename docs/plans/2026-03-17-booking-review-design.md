# Booking Review Design

**Date:** 2026-03-17
**Status:** Approved design, not implemented
**Scope:** HXY miniapp service-quality review domain for booking orders

---

## 1. Background And Current Truth

Current repository truth does not contain an independent "store / service / technician review" runtime domain.

What exists today:
- Miniapp has product comment routes only: `yudao-mall-uniapp/pages.json`
- Frontend comment APIs only cover product comment list and trade order item comment creation:
  - `yudao-mall-uniapp/sheep/api/product/comment.js`
  - `yudao-mall-uniapp/sheep/api/trade/order.js`
- Backend comment controllers only cover product comments and trade order item comment submission:
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/app/comment/AppProductCommentController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/controller/app/order/AppTradeOrderController.java`
- Business truth currently records comment capability under product domain, not booking domain:
  - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`

Therefore this design must be treated as a new planned domain. It must not be backfilled into existing product comment truth, and must not be written as `ACTIVE` until route + frontend API + backend controller + acceptance evidence all exist.

---

## 2. Product Goal

Build a booking-linked service-quality review capability that lets members evaluate a completed service experience, while giving operations a reliable low-score recovery workflow.

The product goal is not community engagement first. The first-order goals are:
- capture real post-service feedback tied to a completed booking
- split quality signals by store, technician, and service item
- route low-score feedback into service recovery quickly
- preserve truthful quality metrics for later operations and growth

---

## 3. Non-Goals

This P0 design does not include:
- community-style likes, nested comments, or feed interaction
- public review ranking and content growth loops
- review editing or append review
- automatic positive-review reward
- automatic low-score compensation
- reusing product comment runtime truth as booking review runtime truth

These can be considered later, but not in the first implementation wave.

---

## 4. Options Considered

### Option A: Reuse Existing Product Comment Domain

Pros:
- lowest initial engineering cost
- existing UI patterns and some backend primitives already exist

Cons:
- core key is `orderItemId`, not `bookingOrderId/serviceOrderId/storeId/technicianId`
- response model is product-centric, not service-fulfillment-centric
- would pollute store/technician/service quality analytics
- would create long-term truth confusion across ledgers and release docs

Decision: reject.

### Option B: Frontend Service Review UX Backed By Product Comment Storage

Pros:
- quick perceived delivery for user experience

Cons:
- data semantics stay wrong
- admin handling, escalation, and analytics still become fragile
- future migration cost remains high

Decision: reject.

### Option C: New `booking review` Domain, Reuse UI Interaction Patterns Only

Pros:
- clean domain model tied to real booking fulfillment
- supports store / technician / service quality breakdown
- supports service-recovery routing and admin follow-up
- preserves future expansion room for rewards, compensation, public display, and dashboards

Cons:
- higher initial design and implementation cost

Decision: approved.

---

## 5. Product Principles

The approved design follows these principles:
- light user effort: users should be able to submit a real review within seconds
- strong structure: one review creates reliable signals for store, technician, and service item
- service recovery first: low-score reviews are first treated as recovery events, not content assets
- truthful quality metrics: reward and compensation mechanisms must not distort the signal too early
- no runtime inflation: docs can become ready for build before runtime is real, but cannot be written as live capability

This is aligned with mature service brands where feedback is used primarily to fix experience issues before it becomes a marketing artifact.

---

## 6. Domain Model

### 6.1 Core Record

Each completed booking can create one primary review record.

Recommended core keys:
- `reviewId`
- `bookingOrderId`
- `serviceOrderId`
- `storeId`
- `technicianId`
- `memberId`
- `serviceSpuId`
- `serviceSkuId`
- `completedTime`
- `submitTime`

### 6.2 User Input Fields

P0 input fields:
- `overallScore`
- `serviceScore`
- `technicianScore`
- `environmentScore`
- `tags[]`
- `content`
- `picUrls[]`
- `anonymous`

### 6.3 Derived Fields

System-derived fields:
- `reviewLevel`: `positive / neutral / negative`
- `riskLevel`: `normal / attention / urgent`
- `displayStatus`: `visible / hidden / review_pending`
- `serviceFollowStatus`: `none / pending / processing / resolved / closed`
- `replyStatus`: `none / replied`
- `auditStatus`: `pass / reject / manual_review`
- `source`: `order_detail / order_list / notice`

### 6.4 Why This Model

This split ensures:
- one real review submission by the member
- three useful quality dimensions for operations
- a separate service-recovery lifecycle that does not depend on public display logic

---

## 7. Information Architecture

The recommended information architecture has four objects:
- `Review Record`: the fact record bound to one completed booking
- `Store Rating`: aggregated store quality signal
- `Technician Rating`: aggregated technician quality signal
- `Service Rating`: aggregated service-item quality signal

This allows one user action to support both member history and operational analysis.

---

## 8. Frontend Scope And Page Flow

### 8.1 P0 Entry Points

P0 entry points should be limited to real booking flow only:
- order detail page: `去评价`
- order list page: `评价`
- service completion notice -> order detail -> review page

P0 should not add:
- homepage review prompts
- technician profile public review CTA without completed booking context
- store profile open review entry without fulfillment context

### 8.2 Planned Pages

Recommended pages:
- `/pages/booking/review/add`
- `/pages/booking/review/result`
- `/pages/booking/review/list`

### 8.3 Page Structure

`review/add` should follow a lightweight two-step expression flow:
1. show the service card: store, technician, service item, completed time
2. capture `overallScore` first
3. expand dimensional scores, tags, text, pictures, anonymous after score is chosen

This keeps submission fast while still supporting rich feedback when needed.

### 8.4 Page States

Required page states:
- eligible to review
- already reviewed
- not eligible to review
- empty list state
- submit success
- submit failure with form content retained

Empty states, null states, and submit failures must be designed explicitly and not treated as polish work.

---

## 9. Rating And Tag Strategy

### 9.1 Rating Dimensions

P0 dimensions:
- overall satisfaction
- service experience
- technician performance
- store environment

### 9.2 Tag Strategy

P0 uses structured tags plus optional free text.

Positive tag examples:
- 服务专业
- 手法舒服
- 环境整洁
- 沟通耐心
- 到店顺畅

Negative tag examples:
- 等待过久
- 服务敷衍
- 手法不适
- 环境一般
- 价格体验差
- 技师迟到
- 沟通不清楚

Guidelines:
- keep positive and negative tag counts roughly balanced
- optimize tags for problem clustering, not emotion amplification
- keep P0 tag count restrained for fast completion

---

## 10. Notification, Recovery, Reward, And Compensation Rules

### 10.1 Low-Score Notification Rule

Low-score feedback should notify quickly, but not every non-positive review should become an immediate store-manager interruption.

Recommended routing:
- `overallScore <= 2`, or high-risk complaint tags: immediate routing
- `overallScore = 3` without high-risk tags: attention pool, not necessarily immediate push
- `overallScore >= 4`: no immediate alert, feed into dashboard/reporting

Immediate-notify examples:
- 服务态度差
- 技师迟到
- 手法不适/造成不适
- 环境卫生差
- 强推销
- 价格争议
- 安全风险

### 10.2 Notification Recipients

Notification should be routed by issue type, not only by store manager identity:
- store manager: store environment / reception / fulfillment issues
- technician lead: technician-quality issues
- customer-service recovery owner: dispute, appeasement, and return contact
- regional owner: high-risk escalation or store-manager conflict cases

### 10.3 SLA Recommendation

Recommended P0 SLA:
- claim within 10 minutes for urgent low-score records
- first user contact within 30 minutes
- result within 24 hours
- overdue escalation within 48 hours

### 10.4 Reward Rule

P0 should not implement automatic positive-review reward.

Reason:
- it distorts the truthfulness of quality scoring
- it introduces early incentive bias
- it weakens later operational analytics

If incentive is added later, the safer version is `completion reward`, not `positive-review reward`.

### 10.5 Compensation Rule

P0 should not implement automatic low-score compensation.

Reason:
- it creates exploitation risk
- it encourages low-score arbitrage before the recovery process is stable
- it should remain a manual service-recovery decision until enough experience data is collected

P0 should support manual compensation decisions in operations, but not automatic compensation issuance.

---

## 11. Backend And Admin Capability Shape

### 11.1 App Capability

The app side needs:
- review eligibility check
- review creation
- review list
- review detail
- review summary query for future display aggregation

### 11.2 Admin Capability

Admin side needs:
- review ledger page
- low-score / pending recovery queue
- reply capability
- follow-up update capability
- aggregated store / technician / service dashboard view

P0 admin priority is service recovery operations, not public content management.

---

## 12. High-Level API Shape

Suggested app APIs:
- `GET /booking/review/eligibility`
- `POST /booking/review/create`
- `GET /booking/review/page`
- `GET /booking/review/get`
- `GET /booking/review/summary`

Suggested admin APIs:
- `GET /admin-api/booking/review/page`
- `GET /admin-api/booking/review/get`
- `POST /admin-api/booking/review/reply`
- `POST /admin-api/booking/review/audit`
- `POST /admin-api/booking/review/follow-up/update`
- `GET /admin-api/booking/review/dashboard/summary`

These are planning-level APIs only. They must not be written into runtime truth ledgers until real frontend bindings and backend controllers exist.

---

## 13. P0 / P1 / P2 Scope

### P0
- eligibility check for completed booking review
- review submission page
- result page
- my review list
- store / technician / service score ingestion
- low-score pending queue
- admin reply and follow-up state
- basic score aggregation

### P1
- completion reward, not positive-review reward
- configurable tag operations
- store / technician profile review summary display
- review dashboard and overdue alerting

### P2
- append review
- review appeal
- curated public review display
- compensation tooling productization
- more advanced ranking and CRM linkage

---

## 14. Engineering And Release Truth

Current truth classification for this design:
- document status: approved design
- engineering status: not implemented
- can develop: yes, as a planned new domain
- can release: no

No part of this design should currently be recorded as:
- runtime active
- page closed-loop complete
- release ready

The design exists to guide future implementation and documentation closure, not to redefine current runtime truth.

---

## 15. Recommended Documentation Split

Recommended doc set for parallel delivery:
- product PRD: `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- page field dictionary: `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- contract: `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- errorcode/failure mode: `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
- service recovery runbook: `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- release gate: `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- final integration review: `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`

Recommended window split:
- B: PRD + field dictionary
- C: contract + errorcode/failure mode
- D: runbook + release gate
- A: final integration + ledger/index updates

---

## 16. Implementation Starting Point

Implementation should begin from these real runtime paths:
- miniapp booking pages:
  - `yudao-mall-uniapp/pages/booking/order-list.vue`
  - `yudao-mall-uniapp/pages/booking/order-detail.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
- miniapp booking API:
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
- booking app controllers:
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingOrderController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppTechnicianController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingAddonController.java`
- booking admin controller base path:
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/`
- current admin overlay booking pages for naming reference only:
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/`

Implementation must keep product comment truth and booking review truth separate from day one.

