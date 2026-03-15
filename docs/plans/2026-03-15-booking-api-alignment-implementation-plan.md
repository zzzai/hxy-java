# Booking API Alignment Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修正小程序 booking API wrapper 的 method/path/参数漂移，并用测试冻结真实请求配置。

**Architecture:** 保持页面调用签名不变，只在 `booking.js` 内统一修复请求配置。测试使用 Node 内置 `node:test` 加载并执行 `booking.js`，通过 request stub 验证每个 API 返回的请求对象。

**Tech Stack:** UniApp JavaScript, Node.js built-in test runner, shell verification scripts

---

### Task 1: Freeze Current Booking API Truth in Tests

**Files:**
- Create: `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
- Modify: `yudao-mall-uniapp/sheep/api/trade/booking.js`

**Step 1: Write the failing test**
- Add a Node test that:
  - loads `booking.js`
  - replaces `@/sheep/request` with a stub returning the request config
  - asserts:
    - technician list path is `/booking/technician/list`
    - slot list path is `/booking/slot/list-by-technician`
    - cancel uses `POST` with `params.id` and `params.reason`
    - addon uses `/app-api/booking/addon/create`

**Step 2: Run test to verify it fails**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs
```

Expected:
- FAIL on old paths / old method / old payload shape.

**Step 3: Write minimal implementation**
- Update `yudao-mall-uniapp/sheep/api/trade/booking.js` only.
- Keep exported API names and function signatures unchanged.

**Step 4: Run test to verify it passes**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs
```

Expected:
- PASS

**Step 5: Commit**

```bash
git add docs/plans/2026-03-15-booking-api-alignment-design.md \
  docs/plans/2026-03-15-booking-api-alignment-implementation-plan.md \
  yudao-mall-uniapp/tests/booking-api-alignment.test.mjs \
  yudao-mall-uniapp/sheep/api/trade/booking.js

git commit -m "fix(booking): align miniapp booking api paths"
```

### Task 2: Run Repository Verification

**Files:**
- Verify only

**Step 1: Run formatting/safety checks**

```bash
git diff --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
```

**Step 2: Confirm clean result**
- All commands must pass.
- Existing unrelated modified file `hxy/04_data/HXY-全域数据价值化与门店数据治理蓝图-v1-2026-03-08.md` remains untouched.
