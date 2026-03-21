# Booking Review Admin Detail Timeline Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review 后台详情页增加“最近动作时间线”只读块，让运营更快完成服务复盘和当前状态理解。

**Architecture:** 保持纯前端实现。把 `BookingReview` 现有字段映射为两个 UI 模型：`summaryItems` 和 `timelineItems`。详情页只消费 helper 结果渲染，不改后端接口、不改状态机、不伪造历史动作日志。

**Tech Stack:** Vue 3 overlay、Node built-in test、Markdown docs。

---

### Task 1: 落设计文档与实施计划

**Files:**
- Create: `docs/plans/2026-03-21-booking-review-admin-detail-timeline-design.md`
- Create: `docs/plans/2026-03-21-booking-review-admin-detail-timeline-implementation-plan.md`

**Step 1:** 写设计文档，固定 A3 范围、节点定义、摘要边界、No-Go。

**Step 2:** 写实施计划，明确 helper、详情页、测试与验证命令。

**Step 3:** 单独提交 docs。

### Task 2: 先写 helper 红灯测试

**Files:**
- Create: `tests/booking-review-admin-detail-timeline.test.mjs`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/timelineHelpers.mjs`

**Step 1: Write the failing test**

新增测试覆盖：
- 只有 `submitTime` 时只生成一个节点
- `replyTime / managerClaimedAt / managerClosedAt` 会按时间正序输出
- `managerLatestActionRemark` 只进入摘要，不进入时间线
- `followStatus` 只进入摘要，不生成伪节点

**Step 2: Run test to verify it fails**

Run: `node --test tests/booking-review-admin-detail-timeline.test.mjs`
Expected: FAIL because helper file does not exist or output shape is missing.

**Step 3:** 不要先改页面，先拿到真实红灯。

### Task 3: 实现 timeline helper

**Files:**
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/timelineHelpers.mjs`
- Test: `tests/booking-review-admin-detail-timeline.test.mjs`

**Step 1:** 实现最小纯函数：
- `buildReviewDetailTimeline(review)`
- 返回：
  - `summaryItems`
  - `timelineItems`

**Step 2:** 时间线节点只认这些字段：
- `submitTime`
- `firstResponseAt`
- `replyTime`
- `managerClaimedAt`
- `managerFirstActionAt`
- `managerClosedAt`

**Step 3:** 摘要区只认这些字段：
- `followStatus`
- `replyStatus`
- `managerTodoStatus`
- `managerLatestActionRemark`
- `followResult`
- `replyContent`

**Step 4: Run test to verify it passes**

Run: `node --test tests/booking-review-admin-detail-timeline.test.mjs`
Expected: PASS.

### Task 4: 为详情页增加时间线静态红灯测试

**Files:**
- Modify: `tests/booking-review-admin-detail-timeline.test.mjs`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`

**Step 1: Write the failing test**

新增静态断言，验证详情页源码：
- 引用了 `buildReviewDetailTimeline`
- 出现 `最近动作时间线`
- 出现 `当前状态摘要` 或等价只读块标题

**Step 2: Run test to verify it fails**

Run: `node --test tests/booking-review-admin-detail-timeline.test.mjs`
Expected: FAIL because详情页还没接 helper。

### Task 5: 接入详情页只读块

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
- Test: `tests/booking-review-admin-detail-timeline.test.mjs`

**Step 1:** 在详情页引入 helper。

**Step 2:** 新增两个 computed：
- `detailSummaryItems`
- `detailTimelineItems`

**Step 3:** 在“评价基础信息”和操作区之间插入新的只读卡片：
- 标题：`最近动作时间线`
- 上部：当前状态摘要
- 下部：时间线节点

**Step 4:** 保持现有交互区不变：
- 回复评价
- 跟进状态
- 店长待办

**Step 5: Run test to verify it passes**

Run: `node --test tests/booking-review-admin-detail-timeline.test.mjs`
Expected: PASS.

### Task 6: 手动回归 booking review 轻量测试集

**Files:**
- Test: `tests/booking-review-admin-query-helpers.test.mjs`
- Test: `tests/booking-review-admin-detail-timeline.test.mjs`

**Step 1:** 运行前端轻量回归：
- `node --test tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-detail-timeline.test.mjs`

**Expected:** PASS.

**Step 2:** 如失败，只修最小真值映射或模板消费，不引入新功能。

### Task 7: 更新 backlog 并做最终验证

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`

**Step 1:** 把 `A3` 从“可进入设计”更新为“已落地（admin-only）”。

**Step 2:** 运行最终验证：
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- `node --test tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-detail-timeline.test.mjs`

**Expected:** PASS.

### Task 8: 提交实现

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/timelineHelpers.mjs`
- Create: `tests/booking-review-admin-detail-timeline.test.mjs`
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`

**Step 1:** `git add` 本批文件。

**Step 2:** 提交信息建议：

```bash
git commit -m "feat(booking-review): add admin detail timeline"
```
