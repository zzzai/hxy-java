# MiniApp Runtime Blocker Release Gate v1 (2026-03-14)

## 1. 目标与证据边界
- 目标：把当前仍会阻断开发信心、灰度放量和最终发布的 runtime blocker 一次性收口成单一门禁文档。
- 本文只基于当前分支真实证据：
  - 前端：`yudao-mall-uniapp/pages/**`、`yudao-mall-uniapp/sheep/api/**`
  - 后端：`AppBookingOrderController`、`AppTechnicianController`、`AppTimeSlotController`、`AppBookingAddonController`、`TechnicianCommissionController`、`TechnicianCommissionServiceImpl`
  - 已正式提交文档：03-10/03-11/03-12/03-14 当前仓内文档包
- 本文不改 overlay 页面、不改业务代码；只固定最终发布门禁、验收口径和回滚动作。

## 2. 最终状态拆分

| 标签 | 定义 | 当前含义 |
|---|---|---|
| `Doc Closed` | 文档包已闭环，可作为单一真值输入 | 不代表 runtime 已可放量 |
| `Can Develop` | 可以继续做页面、接口、联调、样本、runbook、告警和审计补齐 | 不代表可以计入 `ACTIVE` 验收分母 |
| `Cannot Release` | 当前 runtime 真值仍未闭环，禁止把能力记成可放量、可签发、可冲主成功率 | 任一命中即 `No-Go` |
| `Cannot Mis-Release` | 绝不允许把 `Doc Closed`、合法空态、warning、治理闭环、controller-only、旧 path 样本伪装成放量证据 | 任一命中即按误发布处理 |

## 3. 当前最终 blocker 总览

| 对象 | 当前真值 | `Doc Closed` | `Can Develop` | `Cannot Release` | `Cannot Mis-Release` | 固定原因 |
|---|---|---|---|---|---|---|
| `Booking create / cancel / addon` | 仓内 FE API / helper / smoke / runtime gate 已守住 canonical 边界，但真实 release evidence 仍未闭环 | 是 | 是 | 是 | 是 | 静态 gate / local CI 只能证明“边界被守住”，不能替代 allowlist / 日志 / 样本包 / 签发证据 |
| `Member missing-page` | `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 仍缺页；`/member/asset-ledger/page` 仍受 gate 保护 | 是 | 是 | 是 | 是 | 缺页能力没有真实入口和真实用户样本，不能进入 `ACTIVE` 分母 |
| `Reserved runtime` | gift-card / referral / technician-feed 仍无真实页面、真实 app controller、真实样本，开关默认 `off` | 是 | 是 | 是 | 是 | 规划、治理、gray runbook 只是前置治理，不是 runtime 验收 |
| `BO-004` | 真实 `/booking/commission/*` controller 已存在，但仍是 controller-only + no-op 风险 + 无独立页面/API 绑定 | 是 | 是 | 是 | 是 | `code=0` 不能代表真实生效，且当前无服务端 degraded 字段证据 |

## 4. Booking 域最终 No-Go 条件

### 4.1 为什么当前仍禁止放量
1. 当前仓内 FE API 与页面 helper 已对齐到 canonical path / method，且 runtime gate / local CI 已能守住这条静态边界。
2. 但 `check_booking_miniapp_runtime_gate.sh` 与共享 local CI 只证明“代码边界未回退”，不证明：
   - 网关 allowlist 中旧 path 命中数为 `0`
   - 巡检 / 回放日志中旧 path 命中数为 `0`
   - `technician list -> slot list -> create / cancel / addon` 的真实样本包已闭环
   - 负向样本只按 `errorCode` 判定
3. 因此 Booking 当前只有读查询动作 `GET /booking/order/list`、`GET /booking/order/get`、`GET /booking/technician/get` 允许继续开发验证；一旦进入 `slot list / create / cancel / addon`，仍不得进入发布签发、Frozen Candidate 或 release-ready 分母。
4. 单点 `create / cancel / addon` wrapper 对齐不能单独放行。只要真实 release evidence 还没补齐，write-chain 仍是 `blocker_pool`。

### 4.2 样本、验收、发布如何判失败

| 阶段 | 主证据 | 判失败条件 |
|---|---|---|
| 样本 | 请求日志与样本包只命中 canonical `method + path` | 任一旧 path 被调用；缺少 `technician list / slot list / create / cancel / addon` 任一必备样本 |
| 验收 | 至少覆盖 `technician list`、`slot list-by-technician`、`create success/conflict`、`cancel success`、`addon success/fail`，且负向样本只按 `errorCode` 判定 | 样本仍混有旧 path；只验单点成功；按 `message` 而非 `errorCode` 判定 |
| 发布 | FE 代码、联调包、网关 allowlist、巡检日志、回放日志中旧 path 计数都为 `0`；A/B/C/D 材料一致写明 query-only 仍非 release-ready | 把 runtime gate / local CI PASS 写成准发布；把查询侧 `ACTIVE` 外推成 Booking 整域可放量 |

### 4.3 Booking 分母分池与 threshold
- 详细口径见 `docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`。
- Booking 当前固定分三池：
  - `degraded_pool`：只收读查询动作样本与空态样本；可继续开发验证，但不进 release-ready 分母。
  - `blocker_pool`：收 `technician list / slot list / create / cancel / addon` 正负样本，以及任一旧 path 命中、gate 非 `rc=0`、按 `message` 分支等 blocker。
  - `release-ready` 主分母：当前固定为 `0`，直到真实 release evidence 补齐。
- `runtime gate PASS` 与 `local CI PASS` 只代表“边界被守住”；即使 `REQUIRE_BOOKING_MINIAPP_RUNTIME_GATE=0` 把 CI 呈现成 WARN，也不得改变 booking 的 `Cannot Release` 结论。

### 4.4 Booking 当前最终结论
- `Doc Closed`：是。booking PRD、alignment、checklist 已闭环。
- `Can Develop`：是。可以继续改 FE API、补样本、补日志、补联调。
- `Cannot Release`：是。直到真实 release evidence 补齐前，Booking 继续保持 `No-Go`。
- `Cannot Mis-Release`：是。任何把 `query-only ACTIVE` 扩写成 Booking 全域可放量的行为，都算误发布。

## 5. Member 缺页能力最终门禁

### 5.1 为什么缺页能力不能计入 `ACTIVE` 验收分母
1. `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 当前仓内没有真实页面文件，也没有 `pages.json` 入口。
2. `/member/asset-ledger/page` 当前仍受 `miniapp.asset.ledger` gate 保护，且没有与真实页面闭环的运行样本。
3. 如果把这些能力记进 `ACTIVE` 分母，会出现三类伪结论：
   - 用不存在的页面冲高通过样本数
   - 用规划态或 gate 保护接口伪装成已上线能力
   - 把 `Doc Closed` 误写成 runtime `ACTIVE`

### 5.2 当前允许计入 `ACTIVE` 分母的 member 范围
- `component:s-auth-modal`、`/pages/index/login`
- `/pages/index/user`、`/pages/user/info`、`/pages/public/setting`
- `/pages/app/sign`
- `/pages/user/address/list`、`/pages/user/address/edit`
- `/pages/user/wallet/money`、`/pages/user/wallet/score`
- `/pages/coupon/list`、`/pages/activity/point/list`

### 5.3 Member 判失败条件
- 任一验收、看板、周报把 `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 算进 `ACTIVE` 通过样本。
- 任一口径把 `/member/asset-ledger/page` 在 gate 未闭环前算进 `ACTIVE` 主成功率或主转化率。
- 任一人把缺页能力写成“已上线页面，只待联调”。

### 5.4 Member 当前最终结论
- `Doc Closed`：是。member PRD、contract、checklist、gate runbook 已闭环。
- `Can Develop`：是。可以继续开发真实页面、真实入口和真实样本。
- `Cannot Release`：是。缺页能力在页面、入口、controller 真值闭环前不得进入 `ACTIVE` 发布分母。
- `Cannot Mis-Release`：是。把缺页能力计入 `ACTIVE` 分母，直接视为误发布。

## 6. Reserved runtime 未实现最终门禁

### 6.1 为什么规划/治理闭环不能代替 runtime 验收
1. activation checklist、gray runbook、告警路由、误发布 spec 解决的是“如何治理”，不是“能力已经实现”。
2. gift-card / referral / technician-feed 当前仍缺 runtime 三件套：
   - 真实页面
   - 真实 app controller
   - 真实可回放样本
3. 开关默认 `off` 只说明它受管控，不说明它可放量。

### 6.2 当前最终判定表

| capability | 页面 | controller | 样本 | 当前结论 |
|---|---|---|---|---|
| `gift-card` | 无真实 `/pages/gift-card/*` | 无真实 `/promotion/gift-card/*` app controller | 无 | 持续 `Cannot Release` |
| `referral` | 无真实 `/pages/referral/*` | 无真实 `/promotion/referral/*` app controller | 无 | 持续 `Cannot Release` |
| `technician-feed` | 无真实 `/pages/technician/feed` | 无真实 `/booking/technician/feed/*` app controller | 无 | 持续 `Cannot Release` |

### 6.3 Reserved 判失败条件
- 只因为 activation checklist / gray runbook / alert routing 已齐，就把 capability 写成“可进入灰度”。
- 在 `miniapp.gift-card / miniapp.referral / miniapp.technician-feed.audit = off` 时，把任何命中写成 warning 而非 mis-release。
- 把治理文档完备度当成 runtime 成功样本。

### 6.4 Reserved 当前最终结论
- `Doc Closed`：是。治理和灰度文档包已闭环。
- `Can Develop`：是。可以继续开发页面、controller、contract、switch 和样本。
- `Cannot Release`：是。规划/治理闭环不能替代 runtime 验收。
- `Cannot Mis-Release`：是。没有 runtime 真值时，绝不允许把 reserved 写进放量范围。

## 7. `BO-004` 最终运行门禁

### 7.1 主证据只认三段式
- `controller 返回`
- `写后回读`
- `审计键`

当前 `BO-004` 的主审计键固定为：
- `technicianId/storeId/orderId/commissionId/settlementId/runId/sourceBizNo/errorCode`

三段式缺任一段，都不能算主成功样本。

### 7.2 `code=0` 但 no-op / 伪成功如何判失败

| 场景 | 当前返回 | 最终判定 |
|---|---|---|
| `POST /booking/commission/settle` | `code=0 && data=true`，但 `commissionId` 读后仍未变成 `SETTLED` | `FAIL_PSEUDO_SUCCESS` |
| `POST /booking/commission/batch-settle` | `code=0 && data=true`，但 `postPendingCount/postPendingAmount` 未下降 | `FAIL_PSEUDO_SUCCESS` |
| `POST /booking/commission/config/save` | `code=0 && data=true`，但 `config/list(storeId)` 读后无目标配置、字段未变或出现重复 | `FAIL_PSEUDO_SUCCESS` |
| `DELETE /booking/commission/config/delete` | `code=0 && data=true`，但目标 `id` 读后仍存在 | `FAIL_PSEUDO_SUCCESS` |

### 7.3 合法空态与非成功态
- `GET /booking/commission/list-by-technician -> []`：合法空态
- `GET /booking/commission/list-by-order -> []`：合法空态
- `GET /booking/commission/config/list -> []`：合法空态
- `GET /booking/commission/pending-amount -> 0`：合法空态

这些结果只代表“结构合法、当前无数据”，不能代表：
- 页面成功
- 结算成功
- 配置写入成功
- `BO-004` 已可放量

### 7.4 写接口必须以“写后回读 + 审计键”判真
- `settle`：回读目标 `commissionId`，确认 `status=SETTLED` 且 `settlementTime` 有值。
- `batch-settle`：回读 `pending count / pending amount`，确认按预期下降。
- `config/save`：回读 `config/list(storeId)`，确认目标 `storeId + commissionType` 唯一且字段一致。
- `config/delete`：回读 `config/list(storeId)`，确认目标 `id` 消失。

### 7.5 当前无服务端 degraded 字段时的唯一降级方式
- 当前没有服务端 `degraded=true / degradeReason` 证据。
- 因此只允许运维动作降级：
  - `query-only`
  - `single-review-only`
  - `default-rate-only`
- 不允许自造服务端降级返回，不允许把 no-op 写成 fail-open 成功。

### 7.6 `BO-004` 当前最终结论
- `Doc Closed`：是。PRD、contract、SOP、runbook 都已闭环。
- `Can Develop`：是。可以继续开发独立页面、独立 API 绑定、写路径稳定错误码。
- `Cannot Release`：是。controller-only + pseudo success 风险仍在，不能放心放量。
- `Cannot Mis-Release`：是。`BO-003` 页面样本、合法空态、Boolean `true` 都不能伪装成 `BO-004` 发布成功。

## 8. 最终 Go / No-Go 规则

### 8.1 哪些问题属于 `Can Develop`
- 文档已收口，但 runtime 仍待补齐：member 缺页、reserved runtime、`BO-004` 独立页面/API 绑定。
- Booking 查询侧 `ACTIVE` 继续开发、继续维护，不等于 create/cancel/addon 可放量。
- 允许继续补样本、补审计键、补告警、补 gate、补 contract。

### 8.2 哪些问题属于 `Cannot Release`
- Booking 真实 release evidence 未补齐，即使仓内 gate / local CI 为 PASS。
- Member 缺页能力仍被算进 `ACTIVE` 验收分母。
- Reserved 仍无真实页面、controller、样本。
- `BO-004` 任何写接口只验 `true` 不验回读。
- 任意 `warning / degraded / fail-open / legal-empty` 样本进入主成功率、主转化率、主放量判断。

### 8.3 哪些问题属于 `Cannot Mis-Release`
- 把 `Doc Closed` 写成已可放量。
- 把 runtime gate / local CI PASS 写成“准发布”。
- 把合法空态 `[] / 0` 写成页面成功。
- 把 `BO-003` 页面样本拿来冲抵 `BO-004`。
- 把 activation checklist / gray runbook 完整写成 reserved runtime 已验收。
- 把 Booking 查询侧 `ACTIVE` 外推成 Booking 全域 `Go`。
- 把 `RESERVED_DISABLED`、旧 path 命中、伪成功、分母污染当成 warning。

## 9. 当前分支最终结论
- 当前分支这批 blocker 全部已经 `Doc Closed`。
- 当前分支这批 blocker 全部仍然 `Can Develop`。
- 当前分支这批 blocker 全部仍然 `Cannot Release`。
- 发布签发只能继续沿用既有 03-09 Frozen baseline；本批 blocker 不新增任何可放心开发后直接放量的能力。
