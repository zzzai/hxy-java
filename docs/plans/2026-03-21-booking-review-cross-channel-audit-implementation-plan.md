# Booking Review Cross-Channel Audit Implementation Plan

**Goal:** 为 booking review 详情页补齐双通道通知聚合摘要，让运营能一眼看懂同一条差评在 `IN_APP / WECOM` 两条链路上的整体派发状态。

**Architecture:** 采用前端 helper 对当前 outbox 列表做只读聚合，不新增后端接口，不改变现有 outbox 状态机。详情页消费聚合结果，继续保留原始 outbox 表作为明细真值。

**Tech Stack:** Vue 3 overlay、Node built-in test、Markdown docs。

---

### Task 1: 落设计与实施计划

**Files:**
- Create: `docs/plans/2026-03-21-booking-review-cross-channel-audit-design.md`
- Create: `docs/plans/2026-03-21-booking-review-cross-channel-audit-implementation-plan.md`

**Step 1:** 固定“前端 helper 聚合”的方案与 No-Go。

### Task 2: 先写前端红灯测试

**Files:**
- Modify: `tests/booking-review-admin-notify-outbox.test.mjs`

**Step 1:** 增加 helper 红灯：
- 双通道阻断
- 单通道失败
- 缺失通道记录

**Step 2:** 增加页面结构红灯：
- `双通道摘要`
- `App 通道`
- `企微通道`
- 页面引用新 helper

**Step 3:** 运行：
- `node --test tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-detail-timeline.test.mjs`

**Expected:** FAIL。

### Task 3: 实现前端聚合 helper 与详情页展示

**Files:**
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/notifyAuditHelpers.mjs`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`

**Step 1:** helper 输出：
- 总体摘要
- 状态计数
- App 通道卡片
- 企微通道卡片

**Step 2:** 详情页接入 helper，新增聚合摘要块。

**Step 3:** 保留现有 outbox 明细表，不替代明细真值。

### Task 4: 更新 backlog / evidence 并做最终验证

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`
- Modify: `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`

**Step 1:** backlog 记入“跨通道审计摘要”现状。

**Step 2:** evidence ledger 补“详情页双通道摘要块”与最新验证口径。

**Step 3:** 运行最终验证：
- `node --test tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-sla-reminder.test.mjs`
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

### Task 5: 提交实现

**Step 1:** 提交本次 cross-channel audit 聚合增强。
