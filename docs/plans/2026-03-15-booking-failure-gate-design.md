# Booking Failure Branch And Runtime Gate Design

## Goal

为 `yudao-mall-uniapp` 的 booking 页面补齐失败分支行为冻结，并把 `docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md` 中当前能够静态证明的项收口成可执行 gate，避免 booking 链路在失败场景下伪成功，或在发布前把 query-only 能力误写成可放量。

## Scope

本次范围只包含两类能力：

1. booking 页面逻辑的失败分支 smoke
2. booking runtime checklist 的静态 gate

覆盖页面与入口：

- `yudao-mall-uniapp/pages/booking/logic.js`
- `yudao-mall-uniapp/pages/booking/technician-list.vue`
- `yudao-mall-uniapp/pages/booking/technician-detail.vue`
- `yudao-mall-uniapp/pages/booking/order-confirm.vue`
- `yudao-mall-uniapp/pages/booking/order-list.vue`
- `yudao-mall-uniapp/pages/booking/order-detail.vue`
- `yudao-mall-uniapp/pages/booking/addon.vue`
- `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
- `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`

新增 gate 入口：

- `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`

## Non-goals

本次明确不做：

- 页面提示文案与 `uni.showToast` 收口
- 真机 UI 交互测试
- 运行日志或线上 allowlist 校验
- 后端 controller / service 改造
- 将 booking 整域从 `Cannot Release` 升级为可放量

## Problem Statement

当前 booking 已具备两层保护：

1. API wrapper 真值对齐测试
2. 页面成功链路 smoke

但仍缺两层关键约束：

1. 失败分支未冻结
   `create/cancel/addon` 在 `code !== 0` 时是否继续跳详情、刷新列表，当前没有自动化保证。

2. runtime checklist 未落成 gate
   `booking-runtime-closure-checklist` 明确要求 booking 目前只能 `Doc Closed + Can Develop + Cannot Release`，但仓库里还没有一个静态 gate 来证明页面/API/test 仍维持这条边界。

## Recommended Approach

采用“失败分支 smoke + 静态 runtime gate”方案。

### Part A. 失败分支 smoke

在 `logic.js` 中继续保持页面主链 helper 的设计，把失败分支显式化：

- `submitBookingOrderAndGo` 在 `code !== 0` 时不得跳详情
- `cancelBookingOrderAndRefresh` 在 `code !== 0` 时不得刷新
- `submitAddonOrderAndGo` 在 `code !== 0` 时不得跳详情

测试只验证“失败时不进入成功分支”，不引入页面提示文案。

### Part B. booking runtime gate

新增一个轻量 shell gate，静态检查当前仓库是否仍满足 checklist 的关键边界：

1. API wrapper 只使用 canonical path/method
2. 页面脚本通过 helper 命中 canonical helper，不直接散落旧路径
3. 两份 booking 测试文件存在并覆盖关键链路
4. gate 输出明确结论：
   - `Doc Closed`
   - `Can Develop`
   - `Cannot Release`

同时把当前不可放量的 blocker 继续显式打印：

- create chain 仍未构成 release proof
- cancel / addon 仍不能按“现网已放量”解释

## Why This Approach

相比直接做完整 release gate，这个方案更真实：

- 当前仓库没有运行日志与 release 样本输入，无法证明“旧 path 命中数为 0”
- 但我们可以稳定证明“代码当前只保留 canonical FE path/method”与“失败时不会继续走成功分支”
- 这正好对应当前阶段的项目目标：继续开发可以，但不可误放量

## Frozen Behaviors

### 页面失败分支

1. `createOrder` 返回 `code !== 0` 时：
   - 不跳 `/pages/booking/order-detail`
   - 保持调用结果可回传给页面

2. `cancelOrder` 返回 `code !== 0` 时：
   - 不触发刷新回调
   - 保持调用结果可回传给页面

3. `createAddonOrder` 返回 `code !== 0` 时：
   - 不跳 `/pages/booking/order-detail`
   - 保持调用结果可回传给页面

### runtime gate

1. 技师列表仍只认 `GET /booking/technician/list`
2. 技师时段仍只认 `GET /booking/slot/list-by-technician`
3. 取消仍只认 `POST /booking/order/cancel`
4. add-on 仍只认 `POST /app-api/booking/addon/create`
5. booking 页面 smoke 和 API alignment smoke 必须同时存在
6. gate 结论固定为：
   - `Doc Closed`
   - `Can Develop`
   - `Cannot Release`

## Output Contract For The Gate

建议 gate 输出采用单行 summary + 明细列表：

- summary:
  - `domain=booking`
  - `doc_closed=YES`
  - `can_develop=YES`
  - `can_release=NO`
  - `result=PASS|BLOCK`

- block reasons:
  - 若发现旧 path / method
  - 若缺失 smoke tests
  - 若 helper wiring 被绕过

## Success Criteria

1. booking 失败分支 smoke 全部通过
2. booking runtime gate 可本地执行
3. gate 能在旧 path/method、缺测试、绕过 helper 时明确失败
4. gate 成功时仍明确输出 `Cannot Release`
