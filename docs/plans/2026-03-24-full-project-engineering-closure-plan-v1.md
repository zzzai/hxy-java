# Full Project Engineering Closure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 按既定顺序完成全项目剩余工程闭环，先打穿 `Booking` 写链，再依次补齐 `Member` 缺页、`BO-004` 页面/API 真值、`Reserved` runtime、后台独立 contract/runbook 体系。

**Architecture:** 以当前仓库真实代码和真实页面为唯一真值，优先修复“页面已存在但协议/字段/运行样本未闭环”的真实阻断，再进入“从 0 到 1”的 runtime 实现。每个专题都要求代码、测试、产品文档、contract、runbook、evidence 同步落地，避免再次出现“文档闭环替代工程闭环”的假完成。

**Tech Stack:** Vue/uni-app、Java Spring Boot、MapStruct、node:test、JUnit 5、Mockito、Markdown 文档体系

---

### Task 1: 固化全项目执行顺序与验收门槛

**Files:**
- Create: `docs/plans/2026-03-24-full-project-engineering-closure-plan-v1.md`
- Modify: `docs/plans/2026-03-24-project-context-record-v1.md`

**Step 1: 固化执行顺序**

- `Booking` 写链闭环
- `Member` 缺页能力补齐
- `BO-004` 页面/API 真值闭环
- `Reserved` runtime 实现
- 后台独立 contract/runbook 成体系补齐

**Step 2: 固化每个专题统一验收门槛**

- 必须有真实页面 / 真实 API / 真实 controller 三方一致
- 必须先补失败测试，再补实现
- 必须补产品文档、contract/runbook、handoff
- 必须跑专题最小回归 + naming/memory guard

**Step 3: 提交**

```bash
git add docs/plans/2026-03-24-full-project-engineering-closure-plan-v1.md docs/plans/2026-03-24-project-context-record-v1.md
git commit -m "docs(plan): add engineering closure roadmap"
```

### Task 2: Booking 写链闭环

**Files:**
- Modify: `yudao-mall-uniapp/sheep/api/trade/booking.js`
- Modify: `yudao-mall-uniapp/pages/booking/logic.js`
- Modify: `yudao-mall-uniapp/pages/booking/order-confirm.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-list.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-detail.vue`
- Modify: `yudao-mall-uniapp/pages/booking/addon.vue`
- Modify: `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
- Modify: `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingOrderPageReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingOrderController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingOrderRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppTimeSlotRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/app/AppBookingOrderControllerTest.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/app/AppTimeSlotControllerTest.java`
- Modify: `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md`
- Modify: `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`
- Create: `docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-closure-review-v1.md`

**Step 1: 写失败测试**

- 前端失败测试锁定：
  - `BookingApi.getTimeSlot(id)` 必须存在且命中 `/booking/slot/get`
  - `loadTimeSlotDetail(api, id)` 必须存在
- 后端失败测试锁定：
  - 订单列表必须返回 `PageResult`
  - 订单详情/列表必须真实暴露 `payOrderId/spuId/skuId/timeSlotId`
  - 时间槽详情必须真实暴露 `duration`

**Step 2: 跑失败测试**

```bash
node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
mvn -pl ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking -Dtest=AppBookingOrderControllerTest,AppTimeSlotControllerTest test
```

**Step 3: 写最小实现**

- 新增 `AppBookingOrderPageReqVO`
- 订单列表 controller 支持 `pageNo/pageSize/status` 并返回 `PageResult`
- 订单 VO 暴露真实支付与商品绑定字段
- 时间槽 VO 暴露 `duration`
- 前端改为读取 `getTimeSlot(id)`，不再用 `loadTimeSlots(technicianId, null)` 回捞

**Step 4: 跑通过测试**

```bash
node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
mvn -pl ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking -Dtest=AppBookingOrderControllerTest,AppTimeSlotControllerTest test
```

**Step 5: 补文档**

- 明确哪些 blocker 已解除
- 明确哪些仍因缺商品来源或发布样本而不能改写为 `Can Release`

**Step 6: 提交**

```bash
git add yudao-mall-uniapp ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking docs/products/miniapp
git commit -m "feat(booking): close write-chain contract gaps"
```

### Task 3: Member 缺页能力补齐

**Files:**
- Modify: `yudao-mall-uniapp/pages.json`
- Create: `yudao-mall-uniapp/pages/user/level.vue`
- Create: `yudao-mall-uniapp/pages/profile/assets.vue`
- Create: `yudao-mall-uniapp/pages/user/tag.vue`
- Modify: `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`
- Modify: `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md`
- Modify: `docs/plans/2026-03-11-miniapp-member-missing-page-activation-checklist-v1.md`

**Step 1: 先核后端 app controller 真值与缺口**
- `level`
- `experience-record`
- `asset-ledger`
- `tag`

**Step 2: 为每个缺页补失败测试与页面入口**
- 页面存在
- `pages.json` 可达
- 真实入口可点击

**Step 3: 对齐 contract / runbook / 样本**

**Step 4: 提交**

```bash
git add yudao-mall-uniapp docs/products/miniapp docs/plans
git commit -m "feat(member): activate missing member pages"
```

### Task 4: BO-004 页面/API 真值闭环

**Files:**
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/finance/technicianCommission.ts`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/finance/technicianCommission/index.vue`
- Modify: `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
- Modify: `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`

**Step 1: 写失败测试，锁定“不能只有 controller”**

**Step 2: 补真实后台 API + 页面 + 回读样本**

**Step 3: 更新 evidence ledger**

**Step 4: 提交**

```bash
git add ruoyi-vue-pro-master/script/docker/hxy-ui-admin docs/products/miniapp docs/plans
git commit -m "feat(finance-ops): close bo-004 admin truth"
```

### Task 5: Reserved runtime 实现

**Files:**
- Create: `yudao-mall-uniapp/pages/...`
- Create: `ruoyi-vue-pro-master/.../controller/app/...`
- Modify: `docs/plans/2026-03-11-miniapp-reserved-runtime-readiness-register-v1.md`
- Modify: `docs/contracts/2026-03-14-miniapp-runtime-blocker-contract-closure-v1.md`

**Step 1: Gift Card runtime**

**Step 2: Referral runtime**

**Step 3: Technician Feed runtime**

**Step 4: 为三域分别补 contract/runbook/evidence**

**Step 5: 提交**

```bash
git add yudao-mall-uniapp ruoyi-vue-pro-master docs/plans docs/contracts
git commit -m "feat(reserved): implement reserved runtime flows"
```

### Task 6: 后台独立 contract/runbook 体系补齐

**Files:**
- Modify: `docs/products/2026-03-16-hxy-full-project-function-doc-completion-publishable-list-v1.md`
- Create: `docs/contracts/...`
- Create: `docs/plans/...`

**Step 1: 优先补 Finance Ops 主链**

**Step 2: 补 Product/Store/Store Product/Supply Chain/Trade Ops**

**Step 3: 回灌总台账**

**Step 4: 提交**

```bash
git add docs/products docs/contracts docs/plans
git commit -m "docs(admin): fill standalone contract and runbook gaps"
```
