# Booking Failure Branch And Runtime Gate Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking 页面补失败分支 smoke，并新增 booking runtime 静态 gate，确保当前阶段继续保持 `Doc Closed + Can Develop + Cannot Release`。

**Architecture:** 在现有 `yudao-mall-uniapp/pages/booking/logic.js` 的基础上补失败分支行为断言，继续用 Node 内置测试冻结页面逻辑；同时新增一个 shell gate 静态检查 API wrapper、页面 helper wiring、测试文件存在性与 checklist 结论，不依赖运行日志。

**Tech Stack:** UniApp JavaScript, Node.js built-in test runner, Bash, ripgrep

---

### Task 1: 为 booking 页面失败分支补 failing tests

**Files:**
- Modify: `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- Test: `yudao-mall-uniapp/pages/booking/logic.js`

**Step 1: Write the failing test**

在现有 smoke 测试中补 3 组失败分支断言：

- `submitBookingOrderAndGo` 当 `code !== 0` 时不跳详情
- `cancelBookingOrderAndRefresh` 当 `code !== 0` 时不执行刷新回调
- `submitAddonOrderAndGo` 当 `code !== 0` 时不跳详情

**Step 2: Run test to verify it fails**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
```

Expected:
- FAIL，当前 helper 未显式覆盖失败分支或测试暴露未冻结行为。

**Step 3: Write minimal implementation**

只修改 `yudao-mall-uniapp/pages/booking/logic.js`：

- 保持成功分支现有行为不变
- 显式确保 `code !== 0` 时不走跳转或刷新
- 返回原始 result，便于页面后续自己处理提示

**Step 4: Run test to verify it passes**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
```

Expected:
- PASS

**Step 5: Commit**

```bash
git add yudao-mall-uniapp/pages/booking/logic.js \
  yudao-mall-uniapp/tests/booking-page-smoke.test.mjs

git commit -m "test(booking): freeze failure branch behavior"
```

### Task 2: 新增 booking runtime 静态 gate

**Files:**
- Create: `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`

**Step 1: Write the failing gate**

新增 shell gate，至少检查：

1. `yudao-mall-uniapp/sheep/api/trade/booking.js` 中存在 canonical path/method：
   - `/booking/technician/list`
   - `/booking/slot/list-by-technician`
   - `POST /booking/order/cancel`
   - `/app-api/booking/addon/create`
2. 不存在旧 path/method：
   - `/booking/technician/list-by-store`
   - `/booking/time-slot/list`
   - `PUT /booking/order/cancel`
   - `/booking/addon/create`
3. `yudao-mall-uniapp/pages/booking/*.vue` 中已接 helper，不再散落旧路径字符串
4. 测试文件存在：
   - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
   - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`

**Step 2: Run gate to verify it fails at least once during development**

可以先写成最小骨架后运行，确认脚本逻辑真实执行、退出码受检查项控制。

Run:
```bash
bash ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh
```

Expected:
- 在脚本开发过程中出现 FAIL/BLOCK，然后修正为 PASS。

**Step 3: Write minimal implementation**

完善脚本：

- 使用 `rg` 搜索目标文件
- 输出 summary 行
- 输出 block reason 列表
- 成功时固定打印：
  - `doc_closed=YES`
  - `can_develop=YES`
  - `can_release=NO`
  - `result=PASS`

**Step 4: Run gate to verify it passes**

Run:
```bash
bash ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh
```

Expected:
- PASS

**Step 5: Commit**

```bash
git add ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh

git commit -m "test(booking): add miniapp runtime gate"
```

### Task 3: 把 booking gate 接入本批验证链

**Files:**
- Verify only

**Step 1: Run full verification**

Run:
```bash
git diff --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
bash ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh
```

Expected:
- 全部通过
- 既有无关修改 `hxy/04_data/HXY-全域数据价值化与门店数据治理蓝图-v1-2026-03-08.md` 仍保持未触碰

**Step 2: Commit remaining changes**

```bash
git add yudao-mall-uniapp/pages/booking/logic.js \
  yudao-mall-uniapp/tests/booking-page-smoke.test.mjs \
  ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh

git commit -m "test(booking): add failure smoke and runtime gate"
```
