# MiniApp Booking Runtime Release Runbook v1 (2026-03-15)

## 1. 目标与当前状态
- 目标：把 booking 域当前“代码边界已守住，但发布证据未闭环”的状态收口成可执行 runbook，明确 gate threshold、灰度前置条件、rollback trigger、分母分池与样本判定。
- 当前固定状态：
  - `Doc Closed`：是。`03-10/03-11/03-14/03-15` 文档包已能表达 booking 当前真值。
  - `Can Develop`：是。允许继续做 query-only 开发验证、补样本、补 allowlist / 日志证据、补发布签发材料。
  - `Cannot Release`：是。当前仍缺真实 release evidence，`create / cancel / addon` 不得记为 release-ready。

## 2. 当前仓内真值
- `docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md` 继续保留“必补证据清单”价值，但其中早期 blocker 快照不再代表当前仓内实现状态；当前实现真值以本 runbook、runtime gate 与真实代码文件为准。
- `yudao-mall-uniapp/sheep/api/trade/booking.js` 已固定 canonical path / method：
  - `GET /booking/technician/list`
  - `GET /booking/slot/list-by-technician`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
- `yudao-mall-uniapp/pages/booking/logic.js` 已冻结失败分支：
  - `create` 失败不跳详情
  - `cancel` 失败不刷新
  - `addon` 失败不跳详情
- `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs` 与 `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs` 已能证明当前仓内 API / helper 边界没有回退。
- `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh` 与 `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh` 已接入共享守边界链路。

## 3. 为什么 booking 仍是 `Doc Closed + Can Develop + Cannot Release`
1. 当前仓内 gate 只证明“代码边界被守住”，不证明“真实 release evidence 已齐”。
2. `check_booking_miniapp_runtime_gate.sh` 与共享 local CI 只能静态证明：
   - 仓内 FE API 没回退到旧 path / method
   - 页面 helper 没绕过 canonical helper
   - 失败分支 smoke 仍在
3. 这些结果不能直接替代以下准发布证据：
   - 网关 allowlist 中旧 path 计数为 `0`
   - 巡检 / 回放日志中旧 path 命中为 `0`
   - `technician list -> slot list -> create / cancel / addon` 的真实样本包齐全
   - 负向样本只按 `errorCode` 判定，不按 `message` 分支
4. `query-only active` 只说明查询链路可以继续开发验证，不说明 booking 整域可放量。
5. `create / cancel / addon` 虽然仓内 canonical wrapper 已对齐，但在真实 release evidence 补齐前，仍按 `write-chain blocked` 管理。

## 4. 发布前必须补齐的证据清单

| 证据项 | 最低要求 | 当前作用 | 缺失时结论 |
|---|---|---|---|
| 仓内静态 gate | `check_booking_miniapp_runtime_gate.sh` 最新执行 `rc=0` | 证明 repo boundary 没回退 | 进入 `blocker_pool`，直接 `No-Go` |
| 共享 local CI | `run_ops_stageb_p1_local_ci.sh` 中 booking runtime gate 最新执行 `rc=0` | 证明共享 CI 边界没回退 | 进入 `blocker_pool`，直接 `No-Go` |
| FE 旧 path 清零证据 | FE 代码、allowlist、巡检日志、回放日志中旧 path / 旧 method 命中数全部 `=0` | 证明 canonical 已进入真实发布链 | 任一非 0 直接 `No-Go` |
| 样本包 | 至少具备 `technician list success`、`slot list success`、`create success`、`create conflict`、`cancel success`、`addon success`、`addon conflict / blocked` | 证明 create / cancel / addon 已有全链样本 | 样本缺任一项，继续 `Cannot Release` |
| 失败分支证据 | `create / cancel / addon` 失败时不跳成功页、不刷新成功态 | 证明 fail-close 没破坏 | 缺失即 `blocker_pool` |
| errorCode 判定 | 所有负向样本都以 `errorCode` 判定；`message` 仅作补充展示 | 防止文案漂移误判 | 一旦按 `message` 分支，直接 `No-Go` |
| 发布签发包 | A 窗口发布门禁、B 窗口口径、C 窗口 contract、D 窗口 runbook 同步更新到同一批次 | 防止窗口间误写 `release-ready` | 缺任一项，继续 `Cannot Release` |

### 4.1 03-24 新增仓内 selftest pack 的固定读法
- `tests/fixtures/booking-write-chain-release-evidence-simulated/` 与 `check_booking_write_chain_release_evidence_gate.sh` 已新增。
- 它们只能证明“样本结构、errorCode-only 判定、gray / rollback / sign-off 字段口径”在仓内已可校验。
- 它们不能替代真实 request / response / readback 样本，也不能替代 allowlist / 巡检日志 / 回放 / 真实签发证据。
- 因此新增 selftest pack 后，booking 仍然是 `Doc Closed / Can Develop / Cannot Release / No-Go`。

### 4.2 对 gate / local CI 的固定口径
- `runtime gate PASS` 只代表“边界被守住”，不是“已经准发布”。
- `local CI PASS` 只代表“共享链没有回退”，不是“allowlist / 日志 / 样本已闭环”。
- 即使 `REQUIRE_BOOKING_MINIAPP_RUNTIME_GATE=0` 让 CI 结果变成 WARN，也不得把 booking release blocker 降级成 warning。

### 4.3 03-24 release package 单一真值引用
- 03-24 新增 evidence ledger：`docs/plans/2026-03-24-miniapp-booking-write-chain-release-evidence-ledger-v1.md`
- 03-24 新增 release package review：`docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-release-package-review-v1.md`
- 这两份文档只负责把“还缺什么真实证据、为什么仍 `No-Go`、selftest pack 不能替代什么”固定下来。
- 它们不会把 booking 从 `Cannot Release / No-Go` 升级。

## 5. Gate Threshold

| 门禁项 | threshold | 分池 | 说明 |
|---|---|---|---|
| `booking runtime gate rc` | 必须 `=0` | `blocker_pool` | `rc=2` 或执行失败都不是 warning，而是 blocker |
| `shared local ci booking gate rc` | 必须 `=0` | `blocker_pool` | `REQUIRE=0` 只能改变 CI 呈现，不改变 release 结论 |
| 旧 path / 旧 method 命中数 | 必须 `=0` | `blocker_pool` | 包含 FE 文件、allowlist、巡检日志、回放日志 |
| query-only active 样本 | 允许继续收集，但只进 `degraded_pool` | `degraded_pool` | 可用于开发验证，不计入 release-ready 分母 |
| write-chain 样本 | 在证据补齐前全部留在 `blocker_pool` | `blocker_pool` | 单点成功也不能外推为 release-ready |
| 样本包覆盖率 | 必须 `7/7` | `blocker_pool` | 缺任一项都不能推进灰度 |
| errorCode 判定覆盖率 | 必须 `100%` | `blocker_pool` | 任一按 `message` 分支即失败 |

## 6. `degraded_pool / blocker_pool` 处理口径

### 6.1 `degraded_pool`
- 只接收 booking 当前允许继续开发验证、但不能记为 release-ready 的 query-only 样本：
  - `GET /booking/order/list`
  - `GET /booking/order/get`
  - `GET /booking/technician/get`
- `degraded_pool` 在 booking 语境里不是服务端 `degraded=true` 字段。
- 当前没有服务端 `degraded=true / degradeReason` 证据，不得在 runbook 里虚构后端 degraded 返回。
- 一旦从这些页面继续触发：
  - `GET /booking/slot/list-by-technician`
  - `POST /booking/order/create`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
  该样本就不再属于 query-only，而是回到 `blocker_pool`。
- `degraded_pool` 只用于观察 query-only 能力是否稳定，不得计入：
  - 主发布成功率
  - 主灰度通过率
  - release-ready 分母

### 6.2 `blocker_pool`
- 统一接收以下对象：
  - `technician list`
  - `slot list-by-technician`
  - `order-confirm / create`
  - `cancel`
  - `addon`
  - 任一旧 path / 旧 method 命中
  - 任一 gate / local CI 非 `rc=0`
  - 任一按 `message` 分支的负向样本
- `blocker_pool` 里的命中不能降级成 warning。
- 只要 `blocker_pool` 非空，booking 域整体继续维持 `Cannot Release`。

## 7. `query-only active` 与 `write-chain blocked` 的验收分母分池规则

| 池 | 允许纳入的样本 | 不允许纳入的样本 | 当前结论 |
|---|---|---|---|
| `degraded_pool` | `GET /booking/order/list`、`GET /booking/order/get`、`GET /booking/technician/get` 的查询样本与空态样本 | `slot list / create / cancel / addon` 任意样本 | 只代表 query-only 仍可开发验证 |
| `blocker_pool` | `technician list`、`slot list`、`create / cancel / addon` 正负样本、旧 path 命中、gate 失败 | 无 | 任一命中继续 `Cannot Release` |
| `release-ready` 主分母 | 当前固定为 `0` | booking 全域所有样本 | 在真实 release evidence 补齐前，不存在 booking release-ready 分母 |

### 7.1 固定规则
- `query-only active` 可以继续开发验证，但不得计为 release-ready。
- `create / cancel / addon` 在真实 release evidence 补齐前，必须继续按 `Cannot Release` 管理。
- 不得把 booking 域整体从 `blocker_pool` 移出。

## 8. 灰度前置条件
1. 最新一轮 `check_booking_miniapp_runtime_gate.sh` 为 `rc=0`。
2. 最新一轮共享 local CI 中 booking runtime gate 为 `rc=0`。
3. 旧 path / 旧 method 在 FE 文件、allowlist、巡检日志、回放日志中计数全部 `=0`。
4. `technician list -> slot list -> create / cancel / addon` 样本包完整，且正负样本都只按 `errorCode` 判定。
5. A/B/C/D 四窗口材料已同步写明：
   - query-only active 仍不等于 release-ready
   - create / cancel / addon 仍由 `blocker_pool` 管理
6. rollback owner、runId、样本批次、回放入口、人工接管人已在发布签发单中固化。

## 9. Rollback Trigger

| 触发条件 | 动作 |
|---|---|
| 任一旧 path / 旧 method 在 FE、allowlist、巡检日志、回放日志中重新出现 | 立即停止灰度，回退到 query-only 验证态，重新清零证据 |
| runtime gate / shared local CI booking gate 非 `rc=0` | 立即停止当前批次，不得继续签发 |
| 任一负向样本按 `message` 而不是 `errorCode` 判定 | 立即回退本批验收结论，重做样本 |
| 任一人把 query-only active 写成 release-ready，或把 blocker 降级成 warning | 立即回退发布口径，标记本批 `No-Go` |
| 任一文档或实现补写不存在的服务端 `degraded=true / degradeReason` 字段 | 立即回退该口径，按误发布治理处理 |
| create / cancel / addon 缺样本、少链路、单点成功外推整链成功 | 立即停留在 `Cannot Release`，不得继续灰度 |

## 10. 样本判定规则

| 样本 | 当前只能算什么 | 不得外推成什么 |
|---|---|---|
| `GET /booking/order/list => code=0 && data=[]` | 空态样本 | 页面成功、release-ready 成功 |
| `GET /booking/order/list => code=0 && data!=[]` | query-only 样本 | booking 整域可放量 |
| `GET /booking/order/get => code=0` | query-only 样本 | write-chain 已闭环 |
| `GET /booking/technician/get => code=0` | query-only 样本 | `create` 已可放量 |
| `create conflict` / `addon conflict` 返回显式 `errorCode` 且未进入成功态 | 失败分支校验样本 | 正向成功样本 |
| `cancel` 失败且未刷新、`create` 失败且未跳详情、`addon` 失败且未跳详情 | 失败分支校验样本 | query-only 成功、release-ready 成功 |

### 10.1 直接算失败的情况
- `code != 0` 但页面仍进入成功态。
- 负向样本没有稳定 `errorCode`，只能靠 `message` 判断。
- `technician list / slot list / create / cancel / addon` 任一链路缺样本。

### 10.2 命中即直接 `No-Go` 的情况
- 任一旧 path / 旧 method 命中。
- runtime gate / shared local CI 结果被当成“已经准发布”。
- 任何人把 `query-only active` 计入 booking release-ready 分母。
- 任何人把 `blocker_pool` 降级成 warning。
- 任何文档或结论虚构服务端 `degraded=true / degradeReason`。

## 11. 验收清单
- [ ] 文档明确 booking 当前为何仍是 `Doc Closed + Can Develop + Cannot Release`
- [ ] 文档明确 gate / local CI 只证明“边界被守住”，不等于“准发布”
- [ ] 文档明确 `degraded_pool / blocker_pool` 分池口径
- [ ] 文档明确 `query-only active` 与 `write-chain blocked` 的分母规则
- [ ] 文档明确哪些样本只算空态、哪些只算失败分支校验、哪些命中直接 `No-Go`
- [ ] 文档明确当前无服务端 `degraded=true / degradeReason` 证据
