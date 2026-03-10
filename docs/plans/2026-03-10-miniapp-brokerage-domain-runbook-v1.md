# MiniApp Brokerage Domain Runbook v1 (2026-03-10)

## 1. 目标
- 为分销/佣金域建立运营、财务、客服、风控共同可执行的运行手册，覆盖佣金结算、提现审核、提现失败、冲正、冻结、解冻、申诉。
- 对齐真实 app 接口：
  - `/trade/brokerage-user/*`
  - `/trade/brokerage-record/*`
  - `/trade/brokerage-withdraw/*`
- 对齐后台能力：
  - `booking/commission-settlement/*`
  - `booking/commission-settlement/notify-outbox-*`
- 对齐门禁与值班：
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
- 当前 capability 真值：
  - `CAP-BROKERAGE-001 brokerage.center = PLANNED_RESERVED / BACKLOG-DOC-GAP`
  - 本文补齐运行与回滚口径，不自动等价为 `ACTIVE`

## 1.1 资金治理总则
- 资金相关异常必须闭合四段：`审核 -> 冲正 -> 申诉 -> 追溯`；缺任一环都不能结案。
- 告警、申诉、人工补录统一携带五键：`runId/orderId/payRefundId/sourceBizNo/errorCode`；字段不适用时填 `"0"`。
- 任何带 `degraded=true`、warning 或 outbox 保活语义的样本，只进入 `degraded_pool`，不得计入主结算成功率、主提现成功率、主 ROI。
- 客服、财务、风控都不得在财务系统未确认前对用户承诺“已到账”“已结算成功”。

## 2. 关键对象与状态

| 对象 | 核心状态 | 说明 |
|---|---|---|
| 佣金记录 | `PENDING -> SETTLEMENT -> SETTLED / REVERSED / FROZEN` | 订单驱动生成，退款或风控会触发冲正/冻结 |
| 提现申请 | `APPLY -> AUDITING -> AUDIT_SUCCESS -> WITHDRAW_SUCCESS / WITHDRAW_FAIL / REJECT` | 财务审核后进入打款或失败 |
| 结算单 | `DRAFT -> SUBMITTED -> APPROVED / REJECTED -> PAID / CANCELED` | 后台审批与通知出站闭环 |
| 申诉单 | `OPEN -> INVESTIGATING -> RESOLVED / REJECTED` | 用户或客服发起 |

## 3. 责任边界

| 角色 | 负责事项 | 不负责事项 |
|---|---|---|
| 运营 | 分销开关、活动说明、推广员绑定投诉的业务解释 | 财务打款与风控判定 |
| 财务 | 提现审核、打款、失败复核、结算单支付确认 | 内容话术与分销招募运营 |
| 客服 | 用户咨询、申诉受理、回访、工单闭环 | 直接改账、直接解冻 |
| 风控 | 冻结/解冻、异常交易识别、黑名单处置 | 财务打款执行 |
| 技术 on-call | 佣金流水一致性、冲正幂等、打款回调异常 | 业务口径解释 |

## 4. 标准流程

### 4.1 佣金结算
1. 订单履约完成后生成佣金记录，进入 `PENDING`。
2. 结算任务汇总佣金记录，生成结算单。
3. 运营/财务按结算单审批流提交、审核、支付。
4. 支付后佣金记录进入 `SETTLED`。

### 4.2 提现审核
1. 用户通过 `/trade/brokerage-withdraw/create` 发起提现。
2. 财务审核通过后，进入转账或待确认收款。
3. 微信转账场景需校验 `transferChannelPackageInfo` 与 `transferChannelMchId`。
4. 成功后提现单进入 `WITHDRAW_SUCCESS`。

### 4.3 提现失败
1. 打款接口失败或超时，提现单进入 `WITHDRAW_FAIL` 或保留 `AUDIT_SUCCESS` 待复核。
2. 财务在 15 分钟内判断重试还是驳回。
3. 客服统一告知“提现处理中/失败原因”，不得承诺已到账。

### 4.4 冲正
1. 退款或审计发现异常时，触发佣金冲正。
2. 冲正必须使用既有唯一约束和 `origin_commission_id`，避免重复冲正。
3. 发生 `COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT(1030007011)` 时直接停重试并升级。

### 4.5 冻结 / 解冻
1. 风控命中异常订单、异常推广关系或异常提现时，冻结佣金或提现资格。
2. 解冻只能由风控或财务复核后执行。
3. 冻结/解冻都必须记录 `sourceBizNo`、执行人、理由。

### 4.6 申诉
1. 客服收集用户证据并建申诉工单。
2. 风控/财务联合复核账务与订单。
3. 结论回写申诉单并由客服回访。

### 4.7 资金异常闭环

| 阶段 | 最小动作 | 必填证据 |
|---|---|---|
| 审核 | 记录审核人、审核结论、批次/渠道信息 | `auditId/runId/sourceBizNo/errorCode` |
| 冲正 | 校验 `origin_commission_id`、执行冲正或冻结相关批次 | `reversalId/orderId/payRefundId/sourceBizNo/errorCode` |
| 申诉 | 建立用户申诉单、附证据、说明处理时限 | `appealId/orderId/payRefundId/sourceBizNo/errorCode` |
| 追溯 | 把审核、冲正、申诉、通知 outbox 串到同一追溯单 | `traceTicketId/runId/orderId/payRefundId/sourceBizNo/errorCode` |

> 任一资金异常若只能定位到“失败”而无法串到上述四段，统一视为 `No-Go`。

## 5. 执行表

| 动作ID | 场景 | 执行人 | 关键动作 | 必填审计字段 |
|---|---|---|---|---|
| `BRO-01` | 佣金记录待结算 | 运营 + 财务 | 核对佣金批次、结算周期、差异单 | `runId/orderId/payRefundId/sourceBizNo/errorCode` |
| `BRO-02` | 结算单提交审批 | 财务 | 提交 `commission-settlement` 审批流 | `runId/sourceBizNo/errorCode` |
| `BRO-03` | 提现申请审核 | 财务 | 审核提现信息、支付渠道、实名信息 | `runId/sourceBizNo/errorCode` |
| `BRO-04` | 提现失败 | 财务 + 客服 | 判断重试/驳回，通知客服回访 | `runId/sourceBizNo/errorCode` |
| `BRO-05` | 退款触发冲正 | 技术 on-call + 财务 | 校验 `origin_commission_id`、执行冲正 | `runId/orderId/payRefundId/sourceBizNo/errorCode` |
| `BRO-06` | 佣金冻结 | 风控 | 冻结可提现金额/记录，写入原因 | `runId/orderId/sourceBizNo/errorCode` |
| `BRO-07` | 佣金解冻 | 风控 + 财务 | 复核后解冻并通知客服 | `runId/orderId/sourceBizNo/errorCode` |
| `BRO-08` | 用户申诉 | 客服 | 建单、附证据、跟进复核、回访 | `runId/orderId/payRefundId/sourceBizNo/errorCode` |

## 6. 监控指标与阈值

| 指标编码 | 指标 | 公式 | 频率 | 阈值 | 告警等级 | Owner |
|---|---|---|---|---|---|---|
| `BRO-KPI-01` | 佣金结算成功率 | `settled_commission_cnt / settlement_attempt_cnt` | 15 分钟 | `<99.5%` | P1 | 财务系统 Owner |
| `BRO-KPI-02` | 提现审核超时率 | `audit_timeout_cnt / withdraw_apply_cnt` | 15 分钟 | `>5%` | P1 | 财务值班 |
| `BRO-KPI-03` | 提现失败率 | `withdraw_fail_cnt / audit_success_cnt` | 15 分钟 | `>2%` | P1 | 财务值班 |
| `BRO-KPI-04` | 冲正幂等冲突率 | `conflict_cnt / reversal_attempt_cnt` | 5 分钟 | `>0` | P0 | 技术 on-call |
| `BRO-KPI-05` | 冻结余额异常率 | `freeze_mismatch_cnt / frozen_record_cnt` | 30 分钟 | `>0.5%` | P1 | 风控 Owner |
| `BRO-KPI-06` | 申诉超时未结率 | `appeal_overdue_cnt / open_appeal_cnt` | 小时 | `>10%` | P2 | 客服组长 |
| `BRO-KPI-07` | 通知 outbox 堵塞量 | `pending_outbox_cnt` | 15 分钟 | `>100` | P1 | 技术 on-call |
| `BRO-KPI-08` | 资金异常追溯完整率 | `traced_finance_incident_cnt / finance_incident_cnt` | 15 分钟 | `<100%` | P0 | 技术 on-call + 财务值班 |

## 7. 告警路由

| 场景 | 告警 | 主责 | 升级链路 |
|---|---|---|---|
| 冲正幂等冲突 | `1030007011` 命中 | 技术 on-call | 技术负责人 -> 财务负责人 |
| 提现失败集中爆发 | `BRO-KPI-03` 超阈值 | 财务值班 | 财务负责人 -> 发布负责人 |
| 结算成功率下滑 | `BRO-KPI-01` 超阈值 | 财务系统 Owner | 技术 on-call -> 财务负责人 |
| 冻结余额不一致 | `BRO-KPI-05` 超阈值 | 风控 Owner | 风控负责人 -> 技术负责人 |
| 申诉积压 | `BRO-KPI-06` 超阈值 | 客服组长 | 客服负责人 |

## 8. 回滚动作

| 触发条件 | 回滚动作 | 时限 |
|---|---|---|
| 冲正幂等冲突 `>0` | 停止自动冲正任务，冻结相关批次，转人工复核 | 5 分钟 |
| 提现失败率 `>2%` | 暂停新提现申请入口或切换人工审核模式 | 15 分钟 |
| 结算单支付异常集中 | 暂停结算支付动作，仅保留查询与审核 | 15 分钟 |
| 冻结余额异常持续 | 停止解冻与打款，锁定相关用户 | 15 分钟 |
| 资金异常缺失审核/冲正/申诉/追溯任一闭环证据 | 视为 `No-Go`，冻结相关批次并转人工总复核 | 15 分钟 |

## 9. 客服标准口径
- 佣金未到账：`当前佣金仍在结算流程中，我已记录并为你核查。`
- 提现审核中：`提现申请已提交审核，审核完成后会同步结果，请勿重复提交。`
- 提现失败：`本次提现未成功，我们已记录失败原因并安排复核，到账前请勿重复发起。`
- 冻结/风控：`当前账户存在待核验信息，已转交专员复核，结果会第一时间通知你。`
- 申诉回访：`你的申诉已进入复核流程，处理结果会通过站内消息或人工回访同步。`

## 10. 验收标准
1. 佣金结算、提现审核、提现失败、冲正、冻结、解冻、申诉都有明确动作和责任边界。
2. 每个关键场景均有指标、阈值、告警等级和回滚动作。
3. 审计字段固定包含 `runId/orderId/payRefundId/sourceBizNo/errorCode`。
4. 客服、财务、风控、运营口径一致，不出现“已到账但系统未确认”的假成功表述。
5. 资金异常都能回溯到审核、冲正、申诉、追溯四段闭环。
