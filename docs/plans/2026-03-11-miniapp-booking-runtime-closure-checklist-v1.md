# MiniApp Booking Runtime Closure Checklist v1 (2026-03-11)

## 1. 目标与固定结论
- 目标：把 booking 域当前分支的 runtime truth 固定成一份可执行 checklist，确保 query-only 范围与 write-chain blocker 不再混写。
- 当前固定结论：
  - `Doc Closed`
  - `Can Develop`
  - `Cannot Release`
- 本 checklist 已按 2026-03-15 最终集成评审更新；booking release 状态单一真值只认：
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-release-evidence-review-v1.md`

## 2. 当前证据快照

| 证据项 | 当前真值 | 结论 |
|---|---|---|
| API wrapper | `yudao-mall-uniapp/sheep/api/trade/booking.js` 已固定 `GET /booking/technician/list`、`GET /booking/slot/list-by-technician`、`POST /booking/order/cancel`、`POST /app-api/booking/addon/create` | canonical code 已收口 |
| 页面 helper | `yudao-mall-uniapp/pages/booking/logic.js` 统一冻结 create/cancel/addon 成功/失败分支 | 不再允许失败时伪成功 |
| booking API smoke | `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs` | API 对齐证据已存在 |
| booking page smoke | `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs` | 页面逻辑与失败分支证据已存在 |
| runtime gate | `check_booking_miniapp_runtime_gate.sh` 成功输出 `doc_closed=YES can_develop=YES can_release=NO result=PASS` | gate 明确仍不可放量 |
| shared local CI | `run_ops_stageb_p1_local_ci.sh` 已接入 booking runtime gate，轻量执行时 `booking_miniapp_runtime_gate_rc=0` | shared chain 已接入，但 booking 语义仍是 `can_release=NO` |

## 3. Query-only `ACTIVE` 范围

| 页面 / 范围 | 真实接口 | 当前判断 |
|---|---|---|
| `/pages/booking/technician-list` | `GET /booking/technician/list` | Query-only `ACTIVE` |
| `/pages/booking/technician-detail` | `GET /booking/technician/get`; `GET /booking/slot/list-by-technician` | Query-only `ACTIVE` |
| `/pages/booking/order-list` | `GET /booking/order/list` | Query-only `ACTIVE` |
| `/pages/booking/order-detail` | `GET /booking/order/get` | Query-only `ACTIVE` |

补充规则：
1. `technician-list / technician-detail` 的 query-only `ACTIVE` 只代表上游查询输入已对齐，不代表 create 已可放量。
2. `order-list / order-detail` 的 query-only `ACTIVE` 只代表查询面可维护，不代表同页 cancel / addon 已 release-ready。

## 4. Write-chain blocker 边界

| 写链路 | 当前 canonical 真值 | 当前是否可开发 | 当前是否可放量 | 当前 blocker |
|---|---|---|---|---|
| create | `POST /booking/order/create` | Yes | No | 只有静态 smoke/runtime gate，没有发布级 success/failure 样本、allowlist、巡检、回放证据 |
| cancel | `POST /booking/order/cancel` | Yes | No | 只有前端行为冻结，没有真实取消后状态变更与发布级回放证据 |
| addon | `POST /app-api/booking/addon/create` | Yes | No | 只有静态路径/失败分支证据，没有真实 success/failure 样本与订单关联核验证据 |

## 5. Shared runtime gate / shared local CI 退出前必须守住的事实
1. shared chain 已接入 booking runtime gate，但 booking gate 成功时仍然只允许输出 `can_release=NO`。
2. `booking_miniapp_runtime_gate_rc=0` 只说明当前仓库仍满足“文档已闭环、可开发、不可放量”的静态边界。
3. 不得因为 shared chain 已经接入 booking runtime gate，就把 create / cancel / addon 改写成 release-ready。

## 6. No-Go 条件
1. 把 smoke test 或 runtime gate `PASS` 解释成 booking 写链路已可放量。
2. 把 query-only `ACTIVE` 范围外推成 booking 整域 `Ready`、`Frozen Candidate`、准发布范围或可放心放量。
3. 回退或重新引入以下旧真值：
   - `GET /booking/technician/list-by-store`
   - `GET /booking/time-slot/list`
   - `PUT /booking/order/cancel`
   - `POST /booking/addon/create`
4. shared chain 不再执行 booking runtime gate，或 artifact/summary 中缺失 booking gate rc。
5. 吸收未正式提交的窗口产出作为 release 依据，而不是登记为 `Pending formal window output`。

## 7. Release 退出条件
1. create / cancel / addon 各自补齐可回放的 success + failure 发布级样本。
2. shared chain / 巡检 / allowlist / 运行回放中有真实写链路证据，而不只是静态脚本结果。
3. 写链路继续只按 `errorCode` 分支，不按 `message` 分支。
4. capability ledger、business function truth ledger、release decision pack、本文与 03-15 release review 同步回填。
5. A 窗口重新评审后，才允许讨论是否从 `Cannot Release` 退出。

## 8. 单一真值引用
- booking 当前 release 结论统一只认：
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-release-evidence-review-v1.md`
- booking canonical method/path 与 errorCode 继续下钻参考：
  - `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`
  - `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
