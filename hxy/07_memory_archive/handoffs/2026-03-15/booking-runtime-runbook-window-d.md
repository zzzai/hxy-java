# Booking Runtime Runbook Window D Handoff (2026-03-15)

## 1. 变更摘要
- 新增 booking runtime release runbook：
  - `docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`
  - 固定 booking 当前为何仍是 `Doc Closed + Can Develop + Cannot Release`，以及 gate threshold、灰度前置条件、rollback trigger、`degraded_pool / blocker_pool` 与分母分池规则。
- 更新 runtime blocker 总门禁：
  - `docs/plans/2026-03-14-miniapp-runtime-blocker-release-gate-v1.md`
  - 把 booking 口径从“仓内仍有旧 path”更新为“仓内边界已守住，但真实 release evidence 未闭环”。
- 更新域级发布验收矩阵：
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - 固定 booking 的 query-only / write-chain 分池、gate / local CI 的边界证据定位，以及 blocker 不得降级成 warning。

## 2. 当前真值结论
- booking 当前仍是 `Doc Closed + Can Develop + Cannot Release`。
- 当前仓内已验证到：
  - `booking.js` 使用 canonical path / method
  - `logic.js` 失败分支 fail-close
  - `booking-api-alignment.test.mjs`、`booking-page-smoke.test.mjs` 存在
  - `check_booking_miniapp_runtime_gate.sh`、`run_ops_stageb_p1_local_ci.sh` 已纳入 shared guard
- 但这些只说明“边界被守住”，不能直接说明：
  - 已经准发布
  - 已经 release-ready
  - create / cancel / addon 已可放量

## 3. 固定门禁
- `query-only active`
  - 只允许 `GET /booking/order/list`、`GET /booking/order/get`、`GET /booking/technician/get`
  - 只进 `degraded_pool`
  - 只用于开发验证，不进 release-ready 分母
- `write-chain blocked`
  - `technician list / slot list / create / cancel / addon` 全部继续留在 `blocker_pool`
  - 单点成功样本不能外推成整链 ready
- `runtime gate / local CI`
  - 只证明“边界被守住”
  - 不等于“准发布”
  - 即使被配置成 WARN，也不得改变 booking 的 `Cannot Release`
- 当前无服务端 `degraded=true / degradeReason` 证据：
  - 不得写进 runbook
  - 不得写进联调口径
  - 不得作为降级样本来源

## 4. 对窗口 A / B / C 的联调提醒
- A（集成 / 发布）
  - 只能把 booking 写成 `Doc Closed + Can Develop + Cannot Release`。
  - 不得把 runtime gate / local CI PASS 写成“准发布”。
  - 不得把 booking 整域从 `blocker_pool` 移出。
- B（产品 / 口径）
  - `query-only active` 只能解释为“可继续开发验证”，不能解释为“已可灰度/放量”。
  - `GET /booking/order/list => []` 只能算空态样本，不能算页面成功或 release-ready 成功。
  - `create conflict / addon conflict` 只算失败分支校验样本，不能写成成功样本。
- C（契约 / API / 错误码）
  - booking 负向样本必须只按 `errorCode` 判定，不能按 `message` 分支。
  - canonical 仍固定为：
    - `GET /booking/technician/list`
    - `GET /booking/slot/list-by-technician`
    - `POST /booking/order/cancel`
    - `POST /app-api/booking/addon/create`
  - 不得补写不存在的服务端 `degraded=true / degradeReason` 字段。

## 5. 固定验证命令
1. `bash ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`
2. `node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
3. `git diff --check`
