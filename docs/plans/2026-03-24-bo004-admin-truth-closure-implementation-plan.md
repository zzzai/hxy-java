# BO-004 Admin Truth Closure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 `BO-004 技师提成明细 / 计提管理` 补齐独立后台页面、独立前端 API、菜单路由、写后回读证据和文档真值。

**Architecture:** 独立页面与 API 固定落在 booking 目录，直接绑定 `TechnicianCommissionController` 的 8 条真实 `/booking/commission/*` 接口；页面所有写操作都必须执行写后回读并在 UI 中显式暴露 no-op 风险，不改写后端 `Boolean true` 语义。

**Tech Stack:** Vue3 + Element Plus + TypeScript, Node test, Spring Boot JUnit Mockito, SQL menu seed, Markdown docs

---

### Task 1: 写前端失败测试

**Files:**
- Create: `tests/technician-commission-admin-truth.test.mjs`

**Step 1: 写失败测试**
- 断言独立 API 文件存在且绑定 8 条 `/booking/commission/*` 路径
- 断言独立页面文件存在并引用独立 API 文件
- 断言页面有“写后回读”“接口返回成功但读后未变”等真值提示
- 断言菜单 SQL 存在并绑定 `mall/booking/commission/index`

**Step 2: 运行测试确认失败**
- Run: `node --test tests/technician-commission-admin-truth.test.mjs`

**Step 3: 最小实现通过**
- 新建 API / 页面 / 菜单 SQL

**Step 4: 再跑测试确认通过**
- Run 同上

### Task 2: 写后端失败测试

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/TechnicianCommissionControllerTest.java`

**Step 1: 写失败测试**
- 断言 `list-by-technician` 正确转换 `TechnicianCommissionRespVO`
- 断言 `pending-amount` 返回 `Integer`
- 断言 `settle` / `batch-settle` / `config/save` / `config/delete` 仍包装成 `CommonResult<Boolean>` 的 `true`

**Step 2: 运行测试确认失败**
- Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=TechnicianCommissionControllerTest test`

**Step 3: 最小实现通过**
- 如需补 import / convert 细节，则只做最小改动

**Step 4: 再跑测试确认通过**
- Run 同上

### Task 3: 实现独立前端 API 与页面

**Files:**
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commission.ts`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission/index.vue`
- Create: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-24-hxy-booking-commission-admin-menu.sql`

**Step 1: 补独立 API 文件**
- 定义佣金记录、配置、请求体类型
- 绑定 8 条真实 `/booking/commission/*` 路径

**Step 2: 补独立页面**
- 查询区：`technicianId/orderId/storeId`
- 汇总区：记录数 / 待结算金额 / 配置数
- 佣金记录 tab：按技师 / 按订单查询、单条直结、批量直结
- 配置 tab：列表、新增、编辑、删除
- 写后回读结果区：显式提示“成功但读后未变”的 no-op 风险

**Step 3: 补菜单 SQL**
- route path: `booking-commission`
- component: `mall/booking/commission/index`
- permissions: `booking:commission:query` / `booking:commission:settle` / `booking:commission:config`

**Step 4: 复跑前端测试**
- Run: `node --test tests/technician-commission-admin-truth.test.mjs`

### Task 4: 回填文档真值

**Files:**
- Modify: `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
- Modify: `docs/products/miniapp/2026-03-15-miniapp-finance-ops-technician-commission-admin-page-api-binding-truth-review-v1.md`
- Modify: `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`
- Modify: `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
- Modify: `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-final-integration-v1.md`

**Step 1: 更新 truth review**
- 把“未核出独立页面/API 文件”改为真实已核出

**Step 2: 更新 evidence ledger**
- 区分静态 page/API/menu/test 证据已闭合
- 继续保留 runtime / release 证据未闭合

**Step 3: 更新项目总账**
- 把 `BO-004` 从 `controller-only truth` 升级为 `admin-only 可用 / Can Develop / Cannot Release`

### Task 5: 最终验证与提交

**Files:**
- Verify only

**Step 1: 跑前端测试**
- Run: `node --test tests/technician-commission-admin-truth.test.mjs`

**Step 2: 跑后端测试**
- Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=TechnicianCommissionControllerTest test`

**Step 3: 跑仓库校验**
- Run: `git diff --check`
- Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

**Step 4: 提交**
- Run:
```bash
git add ruoyi-vue-pro-master/script/docker/hxy-ui-admin ruoyi-vue-pro-master/sql/mysql/hxy ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java docs/products docs/plans tests hxy
git commit -m "feat(finance-ops): close bo-004 admin truth"
```
