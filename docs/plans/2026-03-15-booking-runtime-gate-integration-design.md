# Booking Runtime Gate Shared Chain Integration Design

## Goal

把 `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh` 正式接入现有共享验收链 `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`，并让 `.github/workflows/ops-stageb-p1-guard.yml` 在相关变更时自动触发这条链路，确保 booking runtime truth 不再只靠人工单独执行。

## Current Context

当前仓库已经具备 booking runtime gate 本体：

- `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`
- `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
- `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`

但共享链路里还没有接入它：

- `run_ops_stageb_p1_local_ci.sh` 没有 run/require 开关、artifact、summary、执行步骤
- `ops-stageb-p1-guard.yml` 的 `paths` 也没有覆盖 booking runtime gate 文件

这意味着 booking runtime truth 目前仍依赖手工额外跑脚本，不能跟已有 refund/miniapp/store gates 一样进入统一验收证据。

## Approaches Considered

### Option A. 只在 workflow 里单独追加一步执行 booking gate

优点：改动最小。

缺点：
- 本地 `run_ops_stageb_p1_local_ci.sh` 和 CI 不一致
- artifact/summary/result.tsv 不在统一出口里
- 后续维护会出现两套真值链

### Option B. 先接入共享本地链，再由 workflow 继续只调用共享链

优点：
- 本地与 CI 共享同一条入口
- 复用现有 run/require、summary、artifact 模式
- booking gate 的结果会自然进入总 `summary.txt` 和 `result.tsv`
- 最贴合当前项目治理方式

缺点：
- 需要同步修改 shell 脚本与 workflow 路径触发

### Option C. 把 booking gate 合并进 miniapp P0 contract gate

优点：
- 表面上门禁数量更少

缺点：
- 语义混淆，booking runtime truth 和 miniapp P0 doc freeze 是不同层级
- 未来单独调试 booking gate 更困难
- 不符合当前“一个 blocker 一条独立门禁”的结构

## Recommended Design

采用 Option B。

### 1. Shared Local CI Integration

在 `run_ops_stageb_p1_local_ci.sh` 中新增 booking runtime gate 的完整接入面：

- `RUN_BOOKING_MINIAPP_RUNTIME_GATE`
- `REQUIRE_BOOKING_MINIAPP_RUNTIME_GATE`
- CLI 参数
  - `--skip-booking-miniapp-runtime-gate`
  - `--require-booking-miniapp-runtime-gate <0|1>`
- artifact/log/summary/result.tsv wiring
- summary.txt 中的 rc 与 run/require 记录
- gate step 执行与 block/warn 语义

默认策略：
- `RUN_BOOKING_MINIAPP_RUNTIME_GATE=1`
- `REQUIRE_BOOKING_MINIAPP_RUNTIME_GATE=1`

原因：这条 gate 的定位本来就是“当前 booking 文档已闭环、可开发、不可放量”的静态边界证明。它应该像其他 booking/miniapp gates 一样，默认属于共享链的阻断项。

### 2. Workflow Trigger Alignment

在 `.github/workflows/ops-stageb-p1-guard.yml` 中补 booking runtime gate 相关路径：

- `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`
- `yudao-mall-uniapp/sheep/api/trade/booking.js`
- `yudao-mall-uniapp/pages/booking/**`
- `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
- `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`

这样 booking FE/API/runtime truth 变更会自动触发共享 guard，而不是只在后端目录变化时才触发。

### 3. Summary Contract

共享 summary 中新增这些键：

- `run_booking_miniapp_runtime_gate`
- `require_booking_miniapp_runtime_gate`
- `booking_miniapp_runtime_gate_rc`
- `booking_miniapp_runtime_gate_log`
- `booking_miniapp_runtime_gate_summary`
- `booking_miniapp_runtime_gate_tsv`

workflow step summary 也增加一行：

- `booking_miniapp_runtime_gate_rc`

## Non-goals

本次不做：

- 修改 booking 页面或业务代码
- 改写 `check_booking_miniapp_runtime_gate.sh` 的 gate 规则
- 把 booking 域升级为 `can_release=YES`
- 把 booking gate 混入其它现有 gate 的语义里

## Validation Strategy

先用一个最小静态测试冻结“共享链必须包含 booking runtime gate”这一行为，再补脚本和 workflow：

1. 新增测试，先证明当前脚本/工作流尚未包含 booking runtime gate
2. 修改共享链与 workflow
3. 运行测试，确认转绿
4. 再跑 booking 现有 test + runtime gate + HXY guards + `git diff --check`
5. 视情况运行一次 `run_ops_stageb_p1_local_ci.sh` 的轻量组合，证明共享入口可执行

## Success Criteria

1. `run_ops_stageb_p1_local_ci.sh` 默认执行 booking runtime gate
2. booking runtime gate 可按 `REQUIRE_*` 语义决定 BLOCK/WARN
3. 总 `summary.txt` 与 `result.tsv` 纳入 booking runtime gate 结果
4. `ops-stageb-p1-guard.yml` 会因 booking gate/booking page/api/test 改动触发
5. 全部验证通过，且不触碰无关既有修改
