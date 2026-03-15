# MiniApp Booking Runtime Gate Acceptance SOP v1 (2026-03-16)

## 1. 目标与当前固定结论
- 目标：把 booking runtime 当前批次的 gate 执行、验收判定、样本归类、告警升级和回滚动作固定成一份可直接执行的 SOP。
- 当前固定结论：
  - `Can Develop=Yes`
  - `Can Release=No`
  - `Release Ready=No`
  - `Release Decision=No-Go`
- 本 SOP 只认当前分支真实已提交的：
  - booking PRD / route truth / user-api alignment
  - `booking.js`、`logic.js`
  - `booking-api-alignment.test.mjs`、`booking-page-smoke.test.mjs`
  - `check_booking_miniapp_runtime_gate.sh`
  - `run_ops_stageb_p1_local_ci.sh`
  - `.github/workflows/ops-stageb-p1-guard.yml`

## 2. 执行顺序
1. 运行 `check_booking_miniapp_runtime_gate.sh`。
2. 运行 focused booking tests：
   - `node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
   - `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
3. 运行共享 local CI 轻量链，确认 booking runtime gate 被真实执行并写入 summary。
4. 核对门禁结果时只回答四个问题：
   - repo boundary 是否守住
   - shared chain 是否守住
   - 失败分支 smoke 是否仍 fail-close
   - release-ready 证据是否已经补齐
5. 如果第 4 步的前 3 项为 `Yes`，第 4 项仍是 `No`，结论仍然只能是 `No-Go`。

## 3. 每一步的允许结论

| 步骤 | 允许写出的结论 | 禁止写出的结论 |
|---|---|---|
| booking runtime gate `rc=0` | repo boundary 被守住 | booking 已可放量 |
| focused tests 通过 | helper / wrapper / fail-close 未回退 | 发布级样本已闭环 |
| shared local CI booking gate `rc=0` | shared chain 未回退 | booking 已 `Release Ready` |
| query-only 样本 `code=0` | 查询链路仍可开发验证 | create/cancel/addon 已可放量 |
| 空态 `[] / null / 0` | 合法空态 | 成功样本 |

## 4. 样本归类 SOP

### 4.1 只能进 `degraded_pool` 的样本
- `GET /booking/order/list`
- `GET /booking/order/get`
- `GET /booking/technician/get`
- 上述接口返回 `[] / null / 0` 时，只记合法空态。

### 4.2 必须进 `blocker_pool` 的样本
- `GET /booking/technician/list`
- `GET /booking/slot/list-by-technician`
- `POST /booking/order/create`
- `POST /booking/order/cancel`
- `POST /app-api/booking/addon/create`
- 任一 gate / shared local CI 非 `rc=0`
- 任一旧 path / 旧 method 命中
- 任一把 `message` 当作主判据的失败样本
- 任一 `RESERVED_DISABLED` 关闭态误命中

### 4.3 当前不存在的降级样本
- 当前没有服务端 `degraded=true / degradeReason` 真实证据。
- 因此 booking 当前所有“降级”只允许写成：
  - query-only 观察池
  - 只读验证态
  - 暂停 write-chain 放量
- 不允许写成：
  - 服务端 degraded 成功
  - degraded 返回已上线
  - degradeReason 已稳定对外

## 5. Decision Tree
1. `booking runtime gate rc != 0`
   - 直接 `No-Go`
   - 不得降级成 warning
2. focused booking tests 任一失败
   - 直接 `No-Go`
   - 视为 helper / fail-close 漂移
3. shared local CI 未执行 booking runtime gate 或 `booking_miniapp_runtime_gate_rc != 0`
   - 直接 `No-Go`
   - 不得写成“只差 CI 展示”
4. 上述三项都通过，但 allowlist / 巡检日志 / 回放日志 / 7类样本包未核出
   - 仍然 `No-Go`
   - 只允许写“边界守住”
5. 任一 `RESERVED_DISABLED` 关闭态误命中
   - 直接按 mis-release / `No-Go` 处理
   - 不得保留在 P1/P2 warning

## 6. 对外口径

### 6.1 可以说的话
- `booking runtime gate PASS，说明当前代码边界被守住。`
- `booking shared local CI PASS，说明共享链没有回退。`
- `booking 当前仍可继续开发验证，但不能放量。`
- `当前没有服务端 degraded 字段证据。`

### 6.2 不能说的话
- `booking 已经 Release Ready`
- `booking gate 通过，所以可以灰度`
- `空态也算成功样本`
- `degraded=true / degradeReason 已经上线`
- `REQUIRE_BOOKING_MINIAPP_RUNTIME_GATE=0，所以 booking blocker 只是 warning`

## 7. 告警与回滚动作

| 触发 | 级别 | 动作 |
|---|---|---|
| gate / shared local CI 非 `rc=0` | `P0` | 立即停止当前批次，维持 `No-Go` |
| PASS 被误写成 release-ready | `P0` | 立即回退文档口径、签发口径和 handoff |
| 空态被记成成功样本 | `P1` | 立即重算样本与 acceptance 统计 |
| 负向样本按 `message` 判定 | `P1` | 立即重做样本并锁定当前批次 |
| `RESERVED_DISABLED` 关闭态误命中 | `P0` | 按 mis-release 执行回滚，不得降级 |
| 虚构 `degraded=true / degradeReason` | `P0` | 立即回退错误口径并复核所有引用文档 |

## 8. 当前 release 结论模板
- 只能输出：
  - `D 侧 runbook / acceptance / alert routing 已闭环。`
  - `booking runtime 边界已守住。`
  - `booking 工程未闭环。`
  - `booking 当前 Can Develop=Yes / Can Release=No / Release Decision=No-Go。`
- 禁止输出：
  - `booking 已完成放量前收口`
  - `booking 可放量`
  - `booking 已准发布`

## 9. 人工接管入口
- 审计主文档：`docs/plans/2026-03-16-miniapp-booking-runtime-release-gate-audit-v1.md`
- booking runbook：`docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`
- blocker 总门禁：`docs/plans/2026-03-14-miniapp-runtime-blocker-release-gate-v1.md`
- 跨域 acceptance：`docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
- 告警 owner routing：`docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
