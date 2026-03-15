# Booking Runtime Gate Shared Chain Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把 booking runtime gate 接进共享本地验收链与对应 GitHub workflow，并用静态回归测试冻结这条接入关系。

**Architecture:** 先增加一个读取脚本/YAML 的最小静态测试，证明共享链当前未包含 booking runtime gate；再在 `run_ops_stageb_p1_local_ci.sh` 中按既有 gate 模式接入 booking runtime gate，并同步更新 workflow 路径触发和 step summary；最后跑 focused verification 与一轮共享脚本轻量验证。

**Tech Stack:** Bash, GitHub Actions YAML, Node.js built-in test runner, ripgrep

---

### Task 1: 写一个会先失败的共享链接入测试

**Files:**
- Create: `tests/ops-stageb-booking-runtime-gate.test.mjs`
- Test: `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`
- Test: `.github/workflows/ops-stageb-p1-guard.yml`

**Step 1: Write the failing test**

新增 Node 内置测试，至少断言：

- local CI 脚本包含 `RUN_BOOKING_MINIAPP_RUNTIME_GATE`
- local CI 脚本包含 `REQUIRE_BOOKING_MINIAPP_RUNTIME_GATE`
- local CI 脚本会调用 `check_booking_miniapp_runtime_gate.sh`
- workflow paths 包含 booking runtime gate 文件与 booking 页面/API/test 路径
- workflow step summary 包含 `booking_miniapp_runtime_gate_rc`

**Step 2: Run test to verify it fails**

Run:
```bash
node --test tests/ops-stageb-booking-runtime-gate.test.mjs
```

Expected:
- FAIL，因为当前共享链和 workflow 还没有这些接入点。

**Step 3: Commit test scaffold if useful after green**

本任务先不单独提交，和实现一起提交即可。

### Task 2: 接入 shared local CI script

**Files:**
- Modify: `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`

**Step 1: Add run/require flags and CLI options**

按现有 gate 模式补：

- env defaults
- `usage()` 文案
- `--skip-booking-miniapp-runtime-gate`
- `--require-booking-miniapp-runtime-gate <0|1>`
- 0|1 flag validation list

**Step 2: Add artifact/log/summary/result wiring**

补：

- log path
- gate dir / summary / tsv path
- `mkdir -p`
- rc 变量初始化
- finalize summary entries

**Step 3: Execute gate in the pipeline**

按 miniapp/booking 现有 gate 模式新增 step：

- 调 `bash script/dev/check_booking_miniapp_runtime_gate.sh`
- append gate TSV
- 按 `REQUIRE_BOOKING_MINIAPP_RUNTIME_GATE` 决定 BLOCK/WARN

### Task 3: 更新 GitHub workflow 触发与摘要

**Files:**
- Modify: `.github/workflows/ops-stageb-p1-guard.yml`

**Step 1: Expand path triggers**

新增 booking runtime gate、booking pages、booking api、booking tests 的路径。

**Step 2: Expand step summary**

新增一行：

- `booking_miniapp_runtime_gate_rc`

### Task 4: 跑验证并确认转绿

**Files:**
- Verify only

**Step 1: Run red/green test**

Run:
```bash
node --test tests/ops-stageb-booking-runtime-gate.test.mjs
```

Expected:
- PASS after implementation

**Step 2: Run focused booking verification**

Run:
```bash
node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs
node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
bash ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh
```

Expected:
- PASS

**Step 3: Run shared-chain lightweight verification**

Run:
```bash
RUN_STORE_SKU_STOCK_GATE=0 \
RUN_STORE_LIFECYCLE_GATE=0 \
RUN_TESTS=0 \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

Expected:
- PASS 或至少 booking runtime gate 被执行并进入 summary；若其它已有 required gate 阻断，则需要读取 summary/log，确认 booking runtime gate 已真实接入。

**Step 4: Run repo-required verification**

Run:
```bash
git diff --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
```

Expected:
- PASS

### Task 5: Commit

**Files:**
- Add: `docs/plans/2026-03-15-booking-runtime-gate-integration-design.md`
- Add: `docs/plans/2026-03-15-booking-runtime-gate-integration-implementation-plan.md`
- Add: `tests/ops-stageb-booking-runtime-gate.test.mjs`
- Modify: `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`
- Modify: `.github/workflows/ops-stageb-p1-guard.yml`

**Step 1: Commit**

```bash
git add docs/plans/2026-03-15-booking-runtime-gate-integration-design.md \
  docs/plans/2026-03-15-booking-runtime-gate-integration-implementation-plan.md \
  tests/ops-stageb-booking-runtime-gate.test.mjs \
  ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  .github/workflows/ops-stageb-p1-guard.yml

git commit -m "ci(booking): integrate runtime gate into shared chain"
```
