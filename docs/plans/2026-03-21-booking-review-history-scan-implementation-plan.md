# Booking Review History Scan Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review 后台提供一个 admin-only 的历史差评治理扫描页，让运营人工触发扫描、查看候选清单和风险分层，但不执行任何修复写入。

**Architecture:** 后端新增一个只读 scan endpoint，基于现有 booking review、booking order 和 store 主数据做风险分类；前端新增一个独立扫描页，由台账入口跳转，用户点击“开始扫描”后才请求数据。整个功能保持 scan-on-read，不改写数据库，不伪造“已治理完成”。

**Tech Stack:** Java/Spring Boot/JUnit、Vue 3 overlay、Node built-in test、Markdown docs。

---

### Task 1: 冻结设计与计划文档

**Files:**
- Create: `docs/plans/2026-03-21-booking-review-history-scan-design.md`
- Create: `docs/plans/2026-03-21-booking-review-history-scan-implementation-plan.md`

**Step 1:** 写设计文档，固定页面结构、分类规则、后端 endpoint、No-Go。

**Step 2:** 写实施计划，明确前后端文件清单、测试和守卫命令。

**Step 3:** 提交 docs。

### Task 2: 先写后端分类红灯测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`

**Step 1: Write the failing test**

新增测试覆盖：
- negative + `managerTodoStatus=null` + valid submit/store/contact -> `MANUAL_READY`
- negative + `managerTodoStatus=null` + missing submit/store/contact -> `HIGH_RISK`
- non-negative 或已有 todo -> `OUT_OF_SCOPE`
- scan 过程不触发 `updateById`

**Step 2: Run test to verify it fails**

Run:
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest test`

Expected: FAIL，因为 scan 分类能力还不存在。

### Task 3: 实现后端 scan service

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewHistoryScanReqVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewHistoryScanRespVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewHistoryScanSummaryRespVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewHistoryScanItemRespVO.java`

**Step 1:** 为 scan 定义请求与响应 VO。

**Step 2:** 在 service 中新增只读 scan 方法：
- 拉取符合 filter 的 review 列表
- 解析 `resolvedStoreId`
- 拉取 store contact 真值
- 分类成 `MANUAL_READY / HIGH_RISK / OUT_OF_SCOPE`
- 生成 `riskReasons` 与 `riskSummary`
- 做内存分页返回

**Step 3:** 严格保证 scan 不调用任何写库路径。

**Step 4: Run test to verify it passes**

Run:
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest test`

Expected: PASS.

### Task 4: 为 controller 新增 history-scan 红灯测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java`

**Step 1: Write the failing test**

新增测试验证：
- `GET /booking/review/history-scan`
- 返回 `summary + list + total`
- 响应项带 `storeName / technicianName / memberNickname`
- 使用既有 `booking:review:query` 权限语义

**Step 2: Run test to verify it fails**

Run:
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest test`

Expected: FAIL，因为 endpoint 尚不存在。

### Task 5: 实现 history-scan endpoint

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java`

**Step 1:** 新增 `GET /booking/review/history-scan`。

**Step 2:** 复用当前 controller 中的 readable enrich 逻辑，为 scan item 增补：
- `storeName`
- `technicianName`
- `memberNickname`

**Step 3: Run test to verify it passes**

Run:
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest test`

Expected: PASS.

### Task 6: 先写前端扫描页红灯测试

**Files:**
- Create: `tests/booking-review-admin-history-scan.test.mjs`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/historyScan/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`

**Step 1: Write the failing test**

新增 node 测试验证：
- 扫描页存在 `开始扫描`
- 页面消费 `getReviewHistoryScan`
- 页面存在风险提示文案
- 页面包含 summary 卡片和结果表格
- 台账页有入口跳转到 `/mall/booking/review/history-scan`

**Step 2: Run test to verify it fails**

Run:
- `node --test tests/booking-review-admin-history-scan.test.mjs`

Expected: FAIL，因为页面与 API 还不存在。

### Task 7: 实现前端扫描页

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/historyScan/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`

**Step 1:** 扩展 API 类型与 `getReviewHistoryScan()`。

**Step 2:** 实现独立扫描页：
- 条件区
- `开始扫描` 按钮
- 说明卡
- summary 卡片
- 结果表格
- `查看详情`

**Step 3:** 台账页增加次级入口按钮，跳转到扫描页。

**Step 4:** 页面默认不自动请求，只有点击按钮才触发 scan。

**Step 5: Run test to verify it passes**

Run:
- `node --test tests/booking-review-admin-history-scan.test.mjs`

Expected: PASS.

### Task 8: 运行前后端回归

**Files:**
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`
- Test: `tests/booking-review-admin-history-scan.test.mjs`
- Test: `tests/booking-review-admin-query-helpers.test.mjs`
- Test: `tests/booking-review-admin-detail-timeline.test.mjs`

**Step 1:** 运行回归：
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewServiceImplTest test`
- `node --test tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-detail-timeline.test.mjs`

**Expected:** PASS。

### Task 9: 文档与 backlog 收口

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`
- Modify: `docs/products/miniapp/2026-03-19-miniapp-booking-review-history-and-boundary-audit-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`

**Step 1:** backlog 把 `A5` 从“仅可做方案”更新为：
- 若只完成 design/plan：`已完成方案设计`
- 若代码落地后：`已落地（admin-only, scan-only）`

**Step 2:** 在历史边界文档中补充：
- 新增 scan-only 工具，不等于修复工具
- 不写成“历史数据已治理完成”

### Task 10: 最终验证与提交

**Files:**
- All A5-related files above

**Step 1:** 运行最终守卫：
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

**Step 2:** 提交实现：

```bash
git add <A5 files>
git commit -m "feat(booking-review): add history scan page"
```
