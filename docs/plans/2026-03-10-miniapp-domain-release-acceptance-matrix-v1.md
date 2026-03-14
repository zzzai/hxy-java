# MiniApp Domain Release Acceptance Matrix v1 (2026-03-10)

## 1. 目标
- 把剩余 runtime blocker 的验收、门禁、回滚收口到一张可执行矩阵，避免把 `Doc Closed` 误写成“可放量”。
- 本文当前重点只补四类剩余阻断项：
  - `Booking` FE/BE `method + path` 漂移
  - `Member` 缺页能力与 `ACTIVE` 分母污染
  - `Reserved runtime` 未实现
  - `BO-004` controller-only + pseudo success 风险

## 2. 统一门禁口径

### 2.1 状态拆分

| 标签 | 含义 |
|---|---|
| `Doc Closed` | 文档已收口，可作为单一真值输入 |
| `Can Develop` | 允许继续开发、联调、补样本、补审计键 |
| `Cannot Release` | runtime 未闭环，禁止放量、禁止冲主发布结论 |

### 2.2 统一规则
- `warning`、`degraded=true`、显式 `FAIL_OPEN`、合法空态 `[] / 0 / null` 统一进入 side pool，不得计入主成功率、主转化率、主 ROI、主放量判断。
- 任一 `RESERVED_DISABLED` 错误码在开关关闭态或未命中灰度范围返回，统一按“误发布”处理，直接 `No-Go`。
- 任何 `[] / 0` 只能证明“结构合法或当前无数据”，不能单独证明“页面成功”“写操作成功”或“能力已可放量”。
- 告警、验收、回滚记录统一五键：`runId/orderId/payRefundId/sourceBizNo/errorCode`；字段不适用时固定填字符串 `"0"`。

## 3. 当前 blocker 最终状态

| 对象 | 当前真值 | `Doc Closed` | `Can Develop` | `Cannot Release` | 当前禁止点 |
|---|---|---|---|---|---|
| `Booking create / cancel / addon` | FE 仍发旧 path；BE canonical 已固定 | 是 | 是 | 是 | 旧 path 仍在 FE/样本/发布口径中时禁止放量 |
| `Member missing-page` | 缺页能力仍无真实入口；`asset-ledger` 仍受 gate 保护 | 是 | 是 | 是 | 不得计入 `ACTIVE` 验收分母 |
| `Reserved runtime` | gift/referral/feed 仍无真实页面、controller、样本 | 是 | 是 | 是 | 治理闭环不能代替 runtime 验收 |
| `BO-004` | 真实 controller 已存在，但 `code=0` 仍可能 no-op；无独立页面/API 绑定 | 是 | 是 | 是 | 不能只验 `true`，必须写后回读 |

## 4. 逐项验收矩阵

| 域 | 用例ID | 验收对象 | 主证据 | 通过条件 | 判失败条件 | 回滚动作 | 负责角色 |
|---|---|---|---|---|---|---|---|
| `booking` | `BK-01` | `create-chain` | canonical 请求日志 + 样本回放 | `technician/list`、`slot/list-by-technician`、`order/create` 全部只命中 canonical path | 任一旧 path `list-by-store`、`time-slot/list` 命中；只验 `create` 单点成功 | 冻结 Booking 放量，维持 query-only 范围 | Booking Domain Owner + 发布负责人 |
| `booking` | `BK-02` | `cancel / addon` | controller 命中日志 + 样本回放 | `POST /booking/order/cancel`、`POST /app-api/booking/addon/create` 样本齐全 | 仍使用 `PUT /booking/order/cancel` 或 `/booking/addon/create`；只按 `message` 判定 | 从 allowlist 移除旧 path，锁定发布 | Booking Domain Owner + C 窗口 |
| `member` | `MB-01` | `ACTIVE` 分母 | 真实页面与入口清单 | 只统计真实入口：登录、个人中心、签到、地址、钱包、积分、券 | `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 被算进主分母 | 回退报表口径，重新计算主成功率 | Member Domain Owner + 数据治理 |
| `member` | `MB-02` | `asset-ledger` | gate 快照 + 页面真值 + 样本包 | 页面、controller、gate、样本同时存在后才可进入下一轮评审 | 仅因文档完备就把 `/member/asset-ledger/page` 记入 `ACTIVE` | 关闭 `miniapp.asset.ledger`，移出发布范围 | Member Domain Owner + 发布负责人 |
| `reserved-expansion` | `RV-01` | gift / referral / technician-feed runtime | 页面、controller、样本、switch 四件套 | 四件套同时存在，且关闭态误返回计数为 `0` | 只有 checklist / runbook / alert 路由，没有 runtime | 维持 `off`，禁止灰度推进 | 对应域 Owner + SRE |
| `reserved-expansion` | `RV-02` | mis-release 守卫 | `RESERVED_DISABLED` 计数 + switch 快照 | 关闭态误返回计数 `=0` | 任一误返回被当作 warning，而不是 mis-release | 立即关闭开关、清空灰度名单 | SRE + 发布负责人 |
| `finance-ops-admin` | `FO-01` | `BO-004` 查询类 | controller 返回 + 查询回读 | `list-by-technician/list-by-order/config-list => []`、`pending-amount => 0` 只记合法空态 | 把 `[] / 0` 记成页面成功或放量成功 | 保持 query-only，不冲主成功率 | 财务负责人 + Booking Admin on-call |
| `finance-ops-admin` | `FO-02` | `BO-004` 写类 | `controller 返回 + 写后回读 + 审计键` | `settle/batch-settle/config save/delete` 全部读后真实变化 | 只验 `true`；读后未变；缺审计键 | 冻结写操作，切 `query-only` 或 `single-review-only` | 财务负责人 + 发布负责人 |
| `finance-ops-admin` | `FO-03` | `BO-004` 降级方式 | runbook 运维动作 | 只使用 `query-only / single-review-only / default-rate-only` | 自造服务端 `degraded=true / degradeReason` 证据 | 撤销伪降级口径，按运维动作回滚 | 财务负责人 + C/D 窗口 |

## 5. 最终 Go / No-Go 规则
1. `Booking` 只要旧 `method + path` 仍存在，就继续 `No-Go`。
2. `Member` 缺页能力不得计入 `ACTIVE` 通过样本；一旦污染主分母，直接 `No-Go`。
3. `Reserved runtime` 未实现不得因为 alert / runbook / checklist 完整而放量。
4. `BO-004` 主证据只认 `controller 返回 + 写后回读 + 审计键`；任何 `code=0` 但 no-op 都直接按失败处理。
5. 任意 `warning / degraded / FAIL_OPEN / legal-empty` 样本污染主成功率、主转化率、主放量判断，直接 `No-Go`。

## 6. 验收输出要求
1. 每个 blocker 都必须同时写明：`Doc Closed`、`Can Develop`、`Cannot Release`。
2. 所有 `No-Go` 结论必须显式写明触发项、回滚动作、回滚完成时间和负责角色。
3. A 窗口只能依据本文和 capability ledger 更新域状态；不得凭“已有 runbook”直接改成可放量。
