# Booking Page Smoke Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking 页面补轻量页面逻辑 smoke，冻结页面到真实 booking API 的调用、跳转和刷新行为。

**Architecture:** 采用“页面逻辑抽离 + Node 内置测试”方案。把页面里与 API 调用和成功分支有关的逻辑提取到可直接测试的 helper 中，页面本身继续保留现有模板与生命周期 wiring，不引入重型 uniapp 渲染测试基建。

**Tech Stack:** UniApp JavaScript, Node.js built-in test runner, existing booking API wrapper

---

### Task 1: 抽离 booking 页面逻辑 helper

**Files:**
- Create: `yudao-mall-uniapp/pages/booking/logic.js`
- Modify: `yudao-mall-uniapp/pages/booking/technician-list.vue`
- Modify: `yudao-mall-uniapp/pages/booking/technician-detail.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-confirm.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-list.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-detail.vue`
- Modify: `yudao-mall-uniapp/pages/booking/addon.vue`

**Step 1: Write the failing test**

先不要改页面，先在后续测试文件里引用一个尚不存在的 `logic.js` 导出函数，明确 helper 接口需要覆盖：

- `loadTechnicianList`
- `loadTechnicianDetail`
- `loadTimeSlots`
- `submitBookingOrder`
- `cancelBookingOrder`
- `submitAddonOrder`
- `goToTechnicianDetail`
- `goToOrderConfirm`
- `goToOrderDetail`

**Step 2: Run test to verify it fails**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
```

Expected:
- FAIL，报 `logic.js` 或导出函数不存在。

**Step 3: Write minimal implementation**

在 `yudao-mall-uniapp/pages/booking/logic.js` 中新增纯函数：

- 只接收依赖和参数，不直接访问组件实例
- API 调用函数返回原始结果或最小结构化结果
- 跳转 helper 统一只通过传入的 `router.go(path, query)` 执行

**Step 4: Wire pages to helper**

在 6 个 booking 页面中把主链逻辑改为调用 helper，但不要改变模板和页面对外行为。

**Step 5: Run test to verify it passes**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
```

Expected:
- 进入下一批失败点，至少不再报缺少 helper。

**Step 6: Commit**

```bash
git add yudao-mall-uniapp/pages/booking/logic.js \
  yudao-mall-uniapp/pages/booking/technician-list.vue \
  yudao-mall-uniapp/pages/booking/technician-detail.vue \
  yudao-mall-uniapp/pages/booking/order-confirm.vue \
  yudao-mall-uniapp/pages/booking/order-list.vue \
  yudao-mall-uniapp/pages/booking/order-detail.vue \
  yudao-mall-uniapp/pages/booking/addon.vue

git commit -m "refactor(booking): extract page smoke helpers"
```

### Task 2: 为技师列表与详情链路补 smoke

**Files:**
- Create: `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- Test: `yudao-mall-uniapp/pages/booking/logic.js`

**Step 1: Write the failing test**

新增测试用例覆盖：

- `loadTechnicianList(api, storeId)` 调用 `api.getTechnicianList(storeId)`
- `goToTechnicianDetail(router, id, storeId)` 跳 `/pages/booking/technician-detail`
- `loadTechnicianDetail(api, technicianId)` 调用 `api.getTechnician(technicianId)`
- `loadTimeSlots(api, technicianId, date)` 调用 `api.getTimeSlots(technicianId, date)`
- `goToOrderConfirm(router, payload)` 跳 `/pages/booking/order-confirm`

**Step 2: Run test to verify it fails**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
```

Expected:
- FAIL，缺少预期调用、路径或参数。

**Step 3: Write minimal implementation**

补齐 `logic.js` 与页面 wiring，确保：

- technician list/detail 主链全部命中 canonical API
- 跳转 query 保持 `id/storeId/timeSlotId/technicianId`

**Step 4: Run test to verify it passes**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
```

Expected:
- 技师列表与详情相关用例 PASS。

**Step 5: Commit**

```bash
git add yudao-mall-uniapp/tests/booking-page-smoke.test.mjs \
  yudao-mall-uniapp/pages/booking/logic.js \
  yudao-mall-uniapp/pages/booking/technician-list.vue \
  yudao-mall-uniapp/pages/booking/technician-detail.vue

git commit -m "test(booking): add technician page smoke coverage"
```

### Task 3: 为确认页、取消链路、加钟链路补 smoke

**Files:**
- Modify: `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- Modify: `yudao-mall-uniapp/pages/booking/logic.js`
- Modify: `yudao-mall-uniapp/pages/booking/order-confirm.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-list.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-detail.vue`
- Modify: `yudao-mall-uniapp/pages/booking/addon.vue`

**Step 1: Write the failing test**

继续追加用例覆盖：

- `submitBookingOrder(api, payload)` 调用 `createOrder`，且 payload 含 `dispatchMode: 1`
- `cancelBookingOrder(api, id)` 调用 `cancelOrder(id, '用户主动取消')`
- 列表页/详情页取消成功后会调用刷新函数
- `submitAddonOrder(api, payload)` 调用 `createAddonOrder({ parentOrderId, addonType })`
- `goToOrderDetail(router, id)` 统一跳 `/pages/booking/order-detail`

**Step 2: Run test to verify it fails**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
```

Expected:
- FAIL，暴露取消、提交或跳转路径不一致。

**Step 3: Write minimal implementation**

补齐 helper 与页面接线，确保：

- `order-confirm` 成功后跳订单详情
- `order-list` / `order-detail` 取消成功后刷新
- `addon` 成功后跳订单详情

**Step 4: Run test to verify it passes**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
```

Expected:
- 全部 booking page smoke 用例 PASS。

**Step 5: Commit**

```bash
git add yudao-mall-uniapp/tests/booking-page-smoke.test.mjs \
  yudao-mall-uniapp/pages/booking/logic.js \
  yudao-mall-uniapp/pages/booking/order-confirm.vue \
  yudao-mall-uniapp/pages/booking/order-list.vue \
  yudao-mall-uniapp/pages/booking/order-detail.vue \
  yudao-mall-uniapp/pages/booking/addon.vue

git commit -m "test(booking): cover confirm cancel and addon flows"
```

### Task 4: 跑仓库验证并提交计划文档

**Files:**
- Create: `docs/plans/2026-03-15-booking-page-smoke-design.md`
- Create: `docs/plans/2026-03-15-booking-page-smoke-implementation-plan.md`

**Step 1: Run verification**

Run:
```bash
git diff --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
```

Expected:
- 新增 booking smoke 用例通过
- 命名守卫通过
- 记忆守卫通过
- 不触碰既有无关修改 `hxy/04_data/HXY-全域数据价值化与门店数据治理蓝图-v1-2026-03-08.md`

**Step 2: Commit**

```bash
git add docs/plans/2026-03-15-booking-page-smoke-design.md \
  docs/plans/2026-03-15-booking-page-smoke-implementation-plan.md \
  yudao-mall-uniapp/pages/booking/logic.js \
  yudao-mall-uniapp/pages/booking/*.vue \
  yudao-mall-uniapp/tests/booking-page-smoke.test.mjs

git commit -m "test(booking): add page smoke coverage"
```
