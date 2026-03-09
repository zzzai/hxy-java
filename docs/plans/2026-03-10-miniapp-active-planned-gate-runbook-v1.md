# MiniApp ACTIVE and PLANNED_RESERVED Gate Runbook v1 (2026-03-10)

## 1. 目标
- 将会员域发布门禁拆成 `ACTIVE` 与 `PLANNED_RESERVED` 两套执行流程，避免“已上线能力”和“预留能力”混跑。
- 对齐 `RESERVED_DISABLED` 规范，保证误发布可发现、可降级、可回滚、可复盘。

## 2. 状态定义

| 状态 | 定义 | 当前会员域覆盖 |
|---|---|---|
| `ACTIVE` | 已正式对外、生效即参与主发布门禁 | 登录、签到、积分流水、地址管理 |
| `PLANNED_RESERVED` | 契约已冻结但默认关闭，只能在开关开启且命中灰度范围时返回 | 会员资产总账 `/member/asset-ledger/page` |

## 3. 运行门禁流程

### 3.1 T-30 分钟：冻结输入
1. 冻结本次 `releaseId`、发布批次、灰度名单和开关快照。
2. 拉取会员域 5 个 KPI 最近一个观察窗口数据。
3. 核对结构化检索键是否完整：`runId/orderId/payRefundId/sourceBizNo/errorCode`。

### 3.2 ACTIVE 门禁

| 检查项 | 通过条件 | 失败动作 |
|---|---|---|
| 登录成功率 | `MEMBER_LOGIN_SUCCESS_RATE >= 97%` | 直接 `No-Go`，停止会员相关灰度扩容 |
| 地址可用率 | `MEMBER_ADDRESS_AVAILABILITY_RATE >= 99.50%` | 先降级地址写操作，再判定回滚 |
| 积分流水一致率 | `MEMBER_POINT_LEDGER_CONSISTENCY_RATE >= 99.90%` | 冻结签到/积分投放，`No-Go` |
| 签到转化率 | `MEMBER_SIGNIN_CONVERSION_RATE` 达标 | 未达标先 `Watch`，连续 3 窗口异常升级 |
| 错误码 TopN | `1004001000/1004004000/1004008000` 无异常爆发 | 若 Top1 占比 > 40%，停止放量并拉起排障 |

### 3.3 PLANNED_RESERVED 门禁

| 检查项 | 通过条件 | 失败动作 |
|---|---|---|
| 开关状态 | `miniapp.asset.ledger` 与发布单一致 | 开关不一致直接 `No-Go` |
| 灰度范围 | 命中白名单用户/门店/请求头才允许返回保留能力 | 出现越权命中立即清空灰度名单 |
| 误返回检查 | 开关关闭时 `1004009901` 计数 `=0` | 任一命中即按误发布处置 |
| 账本一致率 | 开关开启时 `MEMBER_ASSET_LEDGER_CONSISTENCY_RATE >= 99.95%` | 关闭开关并回滚灰度 |
| 错误码语义 | `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901)` 仅在开关开启且命中灰度时返回 | 语义不符立即 `No-Go` |

### 3.4 Go/No-Go 规则
1. 任一 P0 指标失败，直接 `No-Go`。
2. 任一 `PLANNED_RESERVED` 误返回，直接 `No-Go`。
3. P1 指标失败达到 2 项，`No-Go`。
4. 仅 P2 指标失败时，可 `Go with Watch`，每 15 分钟复核。

## 4. 误发布场景处置

### 4.1 发现
- 发现信号：
  - 开关关闭但日志/前端返回 `1004009901 MINIAPP_ASSET_LEDGER_MISMATCH`。
  - ACTIVE 发布后登录成功率、地址可用率、积分一致率在 2 个窗口内快速跌破阈值。
  - 错误码 TopN 中 `1004001000/1004004000/1004008000/1004009901` 占比异常上升。
- 发现动作：
  1. 固定采集 `releaseId + switchSnapshot + 五键`。
  2. 在 5 分钟内确认影响范围：用户范围、门店范围、批次范围。
  3. 拉起会员域 incident 频道并指定 Incident Commander。

### 4.2 降级

| 场景 | 降级动作 | 目标 |
|---|---|---|
| 登录链路异常 | 停止灰度扩容，保留已登录会话，不继续放量登录/鉴权变更 | 先稳住主入口 |
| 地址链路异常 | 将地址能力降为只读，关闭新增/编辑/删除入口的运营配置 | 保留查询，不放大写故障 |
| 签到/积分异常 | 下线签到活动投放或奖励发放配置，仅保留查询和历史记录展示 | 阻断新增错误账 |
| 资产账本误发布 | 立即执行 `miniapp.asset.ledger=off`，关闭灰度名单 | 立刻止血保留主链路 |

### 4.3 回滚
1. ACTIVE 场景：
   - 若登录成功率或地址可用率 15 分钟内未恢复，回滚当前 `releaseId`。
   - 若积分一致率持续不达标，回滚发布并暂停签到/积分相关运营活动。
2. PLANNED_RESERVED 场景：
   - 立即关闭 `miniapp.asset.ledger`。
   - 恢复上一稳定开关快照和灰度名单。
   - 清理异常实验/白名单配置，确认 `1004009901` 计数恢复为 0。

### 4.4 复盘
1. 24 小时内完成事故复盘。
2. 复盘必须回答：
   - 误发布是开关错误、灰度错误还是发布批次错误。
   - 是否在 SLA 内完成发现、降级、回滚。
   - 五键是否完整，是否存在 `0` 占位以外的真实业务主键。
   - 是否需要新增门禁项、缩紧阈值或补开关审计。

## 5. 错误码与开关策略

| 错误码 / 信号 | 状态 | 开关策略 | 默认处置 |
|---|---|---|---|
| `1004001000 USER_NOT_EXISTS` | `ACTIVE` | 无专属功能开关；停止会员登录相关灰度批次 | 重新登录引导 + 回滚当前发布批次 |
| `1004004000 ADDRESS_NOT_EXISTS` 异常爆发 | `ACTIVE` | 无专属功能开关；必要时将地址能力降级为只读 | 保留列表/默认地址查询，暂停写操作 |
| `1004008000 POINT_RECORD_BIZ_NOT_SUPPORT` | `ACTIVE` | 关闭签到活动投放或奖励发放配置；无统一代码开关时按活动配置下线 | 冻结新增积分发放，保留查询 |
| `1004009901 MINIAPP_ASSET_LEDGER_MISMATCH` | `PLANNED_RESERVED` | `miniapp.asset.ledger=off`，清空灰度名单 | 立即止血并回滚灰度 |
| `1004009901` 在开关关闭态返回 | `PLANNED_RESERVED` 误发布 | `miniapp.asset.ledger=off` + 锁定发布 | 直接按 P1 配置事故处理 |

## 6. 审计要求
- 每次门禁决策必须落库：
  - `releaseId`
  - `gateType`（`ACTIVE`/`PLANNED_RESERVED`）
  - `result`（`GO/NO_GO/GO_WITH_WATCH`）
  - `switchSnapshot`
  - `runId/orderId/payRefundId/sourceBizNo/errorCode`
- 适用字段为空时不允许省略，按 `"0"` 字符串填充。

## 7. 验收标准
1. `ACTIVE` 与 `PLANNED_RESERVED` 的检查项、通过条件、失败动作明确。
2. 误发布处置流程覆盖发现、降级、回滚、复盘四阶段。
3. 每个关键错误码都有对应的开关策略或明确的“无专属开关”说明。
4. 任一门禁结论都能反查到开关快照与五键审计信息。
