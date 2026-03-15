# Booking Runtime Release Gate Window D Handoff (2026-03-16)

## 1. 变更摘要
- 新增 booking release gate 审计文档：
  - `docs/plans/2026-03-16-miniapp-booking-runtime-release-gate-audit-v1.md`
- 新增 booking gate acceptance SOP：
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-gate-acceptance-sop-v1.md`
- 更新 D 侧总门禁：
  - `docs/plans/2026-03-14-miniapp-runtime-blocker-release-gate-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`

## 2. 当前 release 结论
- 当前只能写成：
  - D 侧门禁文档已闭环
  - booking 工程未闭环
  - `Can Develop=Yes`
  - `Can Release=No`
  - `Release Ready=No`
  - `Release Decision=No-Go`
- 当前不得写成：
  - `Release Ready`
  - `可灰度`
  - `可放量`

## 3. Gate Threshold
- `booking runtime gate rc = 0`
- `shared local ci booking gate rc = 0`
- focused booking tests 全部通过
- FE 旧 path / 旧 method 命中数 `= 0`
- allowlist / 巡检日志 / 回放日志旧 path 命中数 `= 0`
- write-chain 样本包覆盖率 `7/7`
- 负向样本 `errorCode` 判定覆盖率 `100%`
- 任一 `RESERVED_DISABLED` 关闭态命中都按 mis-release / `No-Go`

## 4. 回滚条件
- 任一 gate / shared local CI 非 `rc=0`
- 任一文档把 PASS 写成 release-ready
- 任一文档把 `[] / null / 0` 写成成功样本
- 任一负向样本按 `message` 分支
- 任一文档虚构服务端 `degraded=true / degradeReason`
- 任一 `RESERVED_DISABLED` 关闭态命中被当成 warning

## 5. 降级行为
- 当前 booking 不存在服务端 `degraded=true / degradeReason` 真实证据。
- 当前 booking 的 `degraded_pool` 只表示 query-only 观察池，不是服务端降级返回池。
- `GET /booking/order/list`、`GET /booking/order/get`、`GET /booking/technician/get` 的 query-only 样本可以留在 `degraded_pool`。
- `technician list / slot list / create / cancel / addon` 以及任一 gate 异常、旧 path 命中都必须进入 `blocker_pool`。

## 6. 对窗口 A / B / C / E 的联调注意点
- A：
  - 只能集成为 `Can Develop=Yes / Can Release=No / No-Go`。
  - 不得把 gate PASS、CI PASS 或 query-only active 写成 release-ready。
- B：
  - `[] / null / 0` 只能是空态，不是成功样本。
  - query-only active 只能表达“可继续开发验证”，不能表达“可放量”。
- C：
  - 当前分支只核到 `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`。
  - 03-10 contract 只保留早期 blocker 快照价值，不得拿旧 path 行覆盖当前代码 canonical 真值。
  - 失败分支必须按 `errorCode`，不能按 `message`。
- E：
  - 归档样本时必须分清 `degraded_pool` 与 `blocker_pool`。
  - gate PASS / local CI PASS 只能归档为“边界守住”，不能归档为“Release Ready”。
