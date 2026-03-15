# MiniApp Booking Runtime Release Gate Audit v1 (2026-03-16)

## 1. 目标与证据边界
- 目标：把 booking runtime 当前批次的 runbook、验收、灰度、告警、回滚门禁重新收口成一份审计文档，只承认当前分支真实已提交的 PRD、contract、代码、脚本、测试与门禁文档。
- 当前只认以下证据：
  - 产品文档：
    - `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`
    - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-release-sop-v1.md`
  - 契约文档：
    - `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
  - 代码与测试：
    - `yudao-mall-uniapp/sheep/api/trade/booking.js`
    - `yudao-mall-uniapp/pages/booking/logic.js`
    - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
    - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - 运行门禁：
    - `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`
    - `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`
    - `.github/workflows/ops-stageb-p1-guard.yml`
  - D 侧门禁文档：
    - `docs/plans/2026-03-14-miniapp-runtime-blocker-release-gate-v1.md`
    - `docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`
    - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
    - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
- 本分支未核出：
  - `2026-03-15` booking release-evidence contract
  - `2026-03-15` booking canonical api / errorCode matrix
- 因此本审计文档不会引用上述未核出文件，也不会猜测其 path、字段或错误码绑定。

## 2. 当前状态拆分

| 维度 | 当前结论 | 说明 |
|---|---|---|
| D 侧门禁文档 | `Closed` | D 侧 runbook / SOP / acceptance / alert routing 已能收口当前批次门禁口径 |
| booking 工程闭环 | `Not Closed` | allowlist、巡检日志、回放日志、发布级样本包仍未在本分支核出 |
| `Can Develop` | `Yes` | 允许继续做 query-only 验证、补样本、补日志、补签发材料 |
| `Can Release` | `No` | 当前没有 release-ready 证据 |
| `Release Ready` | `No` | `PASS` 只表示边界被守住 |
| `Release Decision` | `No-Go` | blocker 未清空前不得灰度、不得放量 |

### 2.1 本批必须明确区分的四件事
1. D 侧文档收口，不等于 booking 工程闭环。
2. 工程可以继续开发，不等于当前可以放量。
3. gate PASS，不等于 release-ready。
4. query-only active，不等于 write-chain ready。

## 3. 当前真实代码与门禁能证明什么

| 证据 | 当前能证明 | 当前不能证明 |
|---|---|---|
| `booking.js` | FE wrapper 当前使用 canonical path / method：`GET /booking/technician/list`、`GET /booking/slot/list-by-technician`、`POST /booking/order/cancel`、`POST /app-api/booking/addon/create` | 不能证明 allowlist、网关、巡检日志和回放日志已清零 |
| `logic.js` | `create / cancel / addon` 在 `code !== 0` 时不跳成功页、不刷新成功态，仍是 fail-close | 不能证明发布级样本包完整 |
| `booking-api-alignment.test.mjs` | wrapper 没回退到旧 path / method | 不能证明线上调用链已闭环 |
| `booking-page-smoke.test.mjs` | helper 与失败分支 smoke 还在，`code != 0` 不会伪成功 | 不能证明负向样本已在发布级环境按 `errorCode` 全量回放 |
| `check_booking_miniapp_runtime_gate.sh` | repo boundary 没回退；脚本输出固定 `doc_closed=YES can_develop=YES can_release=NO` | 不能证明 booking 已 release-ready |
| `run_ops_stageb_p1_local_ci.sh` + workflow | shared chain 已接入 booking runtime gate，且 `REQUIRE_*` 只控制 CI 呈现 | 不能把 `WARN`/`PASS` 外推成可放量 |

### 3.1 对 03-10 booking contract 的固定读法
- `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md` 在当前分支里仍保留早期 blocker 快照价值：
  - 它能证明 booking 曾经存在 legacy path / method 漂移；
  - 它不能覆盖当前真实代码里的 canonical path / method 真值；
  - 它更不能把 booking 从 `No-Go` 改写成 `Release Ready`。
- 当前 method/path 真值只认代码、测试和 runtime gate，不认历史 blocker 表里的旧 path。

## 4. Gate Threshold 与当前结论

| 门禁项 | threshold | 当前状态 | 触发池 | 口径 |
|---|---|---|---|---|
| `booking runtime gate rc` | 必须 `=0` | 需以当批脚本执行结果核对 | `blocker_pool` | `PASS` 只表示 repo boundary 被守住 |
| `shared local ci booking gate rc` | 必须 `=0` | 需以当批共享 CI 结果核对 | `blocker_pool` | `PASS` 只表示 shared chain 未回退 |
| focused booking tests | `booking-api-alignment` + `booking-page-smoke` 必须全部通过 | 需以当批执行结果核对 | `blocker_pool` | 只证明 wrapper/helper 边界稳定 |
| FE 旧 path / 旧 method 命中数 | 必须 `=0` | 当前代码可静态核对；allowlist / 日志未核出 | `blocker_pool` | 代码清零不等于发布清零 |
| allowlist / 巡检日志 / 回放日志旧 path 命中数 | 必须 `=0` | `未核出` | `blocker_pool` | 未核出不能当作通过 |
| write-chain 样本包覆盖率 | 必须 `7/7` | `未核出` | `blocker_pool` | 缺任一项继续 `No-Go` |
| 负向样本 `errorCode` 判定覆盖率 | 必须 `100%` | 发布级样本 `未核出` | `blocker_pool` | 按 `message` 分支即失败 |
| `RESERVED_DISABLED` 关闭态误命中 | 必须 `=0` | booking 专题不依赖此值放行；一旦命中即 mis-release | `blocker_pool` | 不是 warning，而是 `No-Go` |

### 4.1 当前不能当作放量证据的 PASS
- `check_booking_miniapp_runtime_gate.sh = PASS`
- `run_ops_stageb_p1_local_ci.sh` 中 `booking_miniapp_runtime_gate_rc = 0`
- `booking-api-alignment.test.mjs` 与 `booking-page-smoke.test.mjs` 通过

以上 3 类 PASS 只能写成：
- 边界被守住
- 代码与脚本没回退
- 当前仍是 `Can Develop`

以上 3 类 PASS 不得写成：
- `Can Release=Yes`
- `Release Ready`
- 已可灰度
- 已可放量

## 5. `degraded_pool / blocker_pool` 与样本归类

### 5.1 `degraded_pool`
- 只接收 query-only 样本：
  - `GET /booking/order/list`
  - `GET /booking/order/get`
  - `GET /booking/technician/get`
- 当前 booking 没有服务端 `degraded=true / degradeReason` 真实证据。
- 因此 booking 的 `degraded_pool` 只是观察池，不是服务端降级返回池。
- `[] / null / 0` 只能算合法空态，不算成功样本。

### 5.2 `blocker_pool`
- 统一接收：
  - `GET /booking/technician/list`
  - `GET /booking/slot/list-by-technician`
  - `POST /booking/order/create`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
  - 任一旧 path / 旧 method 命中
  - 任一 gate / shared CI 非 `rc=0`
  - 任一把 `message` 当成主判据的负向样本
  - 任一 `RESERVED_DISABLED` 关闭态误命中
- 只要 `blocker_pool` 非空，booking 当前批次固定 `No-Go`。

### 5.3 release-ready 主分母
- 当前固定为 `0`。
- 在 allowlist / 日志 / 回放 / 样本包未核出前，不存在 booking release-ready 主分母。

## 6. 当前哪些样本不是成功样本

| 样本 | 只能算什么 | 不得写成什么 |
|---|---|---|
| `GET /booking/order/list => code=0 && data=[]` | 合法空态 | 列表成功样本、release-ready 成功 |
| `GET /booking/order/get => code=0 && data=null` | 合法空结果 | 查询链路闭环、写链路闭环 |
| `GET /booking/technician/get => code=0 && data=null` | 合法空结果 | create 可放量 |
| `create / cancel / addon` 失败时未跳成功页、未刷新成功态 | fail-close 校验样本 | 正向成功样本 |
| gate PASS / local CI PASS | 边界守住样本 | 放量证据、发布证据 |

## 7. Rollback Trigger

| 触发条件 | 处理动作 |
|---|---|
| 任一 gate / shared local CI 非 `rc=0` | 立即停止当前批次，维持 `No-Go` |
| 任一文档把 PASS 写成 release-ready | 立即回退发布口径并重发 handoff |
| 任一文档把 `[] / null / 0` 写成成功样本 | 立即回退样本统计并重算 |
| 任一人按 `message` 而不是 `errorCode` 判定负向样本 | 立即回退验收结论并补做样本 |
| 任一文档补写不存在的服务端 `degraded=true / degradeReason` | 立即回退文档口径，按误写处理 |
| 任一 `RESERVED_DISABLED` 关闭态命中被当成 warning | 立即按 mis-release / `No-Go` 升级并回滚 |
| 任一 allowlist / 巡检日志 / 回放日志出现旧 path / 旧 method | 立即冻结 booking 放量，只保留 query-only 验证 |

## 8. 当前 release 结论
- 当前只能写成：`D 侧门禁文档已闭环，工程未闭环，Can Develop=Yes，Can Release=No，Release Decision=No-Go`。
- 当前不能写成：
  - `Booking 已可灰度`
  - `Booking 已可放量`
  - `Booking 已 Release Ready`
  - `Booking gate 已通过，因此允许升级 release decision`

## 9. 本批操作入口
- 门禁审计与阈值判定：`docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`
- 当批 step-by-step SOP：`docs/products/miniapp/2026-03-16-miniapp-booking-runtime-gate-acceptance-sop-v1.md`
- 总 blocker 门禁：`docs/plans/2026-03-14-miniapp-runtime-blocker-release-gate-v1.md`
- 跨域 acceptance：`docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
- 告警与升级链：`docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
