# MiniApp Member Domain KPI and Alerts v1 (2026-03-10)

## 1. 目标
- 为会员域建立可执行的数据门禁与发布 KPI 联动规则，保证登录、签到、积分、地址、资产账本上线后可观测、可追责。
- 对齐既有基线：
  - `docs/plans/2026-03-09-miniapp-data-quality-slo-and-alerting-v1.md`
  - `docs/plans/2026-03-09-miniapp-release-gate-kpi-runbook-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`

## 2. 口径总则
- 成功类指标只认后端确认成功，不以前端展示成功替代。
- `degraded=true` 的流量单独进入 `degraded_pool`，不得计入主成功率、主转化率、一致率。
- 关键检索键统一：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
- 会员域 ACTIVE 指标默认参与发布门禁；`PLANNED_RESERVED` 指标仅在对应开关开启且命中灰度范围后参与 Go/No-Go。

## 3. 会员域核心指标执行表

| 指标编码 | 指标名称 | 公式 | 数据源 | 刷新频率 | 阈值 | 告警等级 | Owner | 发布门禁联动 |
|---|---|---|---|---|---|---|---|---|
| `MEMBER_LOGIN_SUCCESS_RATE` | 登录成功率 | `login_success_uv / login_attempt_uv` | `member_auth_attempt`、`member_auth_success`、账号会话台账 | 5 分钟 | 最近 15 分钟 `>=97%` | P0 | 账号体系值班 | 低于阈值连续 2 个窗口直接 `No-Go`；涉及会员登录/鉴权发布批次必须回滚或停止扩容 |
| `MEMBER_SIGNIN_CONVERSION_RATE` | 签到转化率 | `signin_success_uv / signin_exposure_uv` | `marketing_points_activity_view(activityType=signin)`、`member_signin_success`、`member_signin_record` | 15 分钟 | 最近 2 小时 `>= max(18%, 近 7 日同小时基线 * 80%)` | P2 | 会员增长运营 | 单独跌破只触发 `Go with Watch`；连续 3 个窗口跌破且伴随积分异常时升级为 `No-Go` |
| `MEMBER_POINT_LEDGER_CONSISTENCY_RATE` | 积分流水一致率 | `matched_point_ledger_cnt / total_point_ledger_cnt` | `member_point_record`、积分业务台账、`point_reconcile_diff` | 15 分钟 | 最近 30 分钟 `>=99.90%` | P1 | 会员资产负责人 | 低于阈值连续 2 个窗口 `No-Go`；若错误码 `1004008000` Top1 占比 > 40% 直接冻结签到/积分投放 |
| `MEMBER_ADDRESS_AVAILABILITY_RATE` | 地址可用率 | `valid_address_response_cnt / total_address_request_cnt` | `/member/address/*` 网关日志、`member_address`、地址写操作审计日志 | 5 分钟 | 最近 15 分钟 `>=99.50%` | P1 | 会员基础资料负责人 | 低于阈值连续 2 个窗口 `No-Go`；若写接口异常集中，先降级只读再回滚发布 |
| `MEMBER_ASSET_LEDGER_CONSISTENCY_RATE` | 资产账本一致率 | `matched_asset_entry_cnt / total_asset_change_cnt` | `/member/asset-ledger/page` 结果、`member_asset_ledger`、`member_point_record`、券资产台账、`asset_reconcile_diff` | 15 分钟 | 开关开启时最近 30 分钟 `>=99.95%`；开关关闭时 `1004009901` 误返回计数 `=0` | P1 | 会员资产负责人 + 促销账本负责人 | `miniapp.asset.ledger=on` 时低于阈值连续 2 个窗口 `No-Go`；开关关闭却返回 `1004009901` 视为误发布，按 P1 配置事故立即回滚 |

## 4. 指标补充说明

### 4.1 登录成功率
- 统计对象：登录页、静默登录、token 刷新三类鉴权动作。
- 剔除范围：重复重试、测试账号、已判定机器人流量。
- 重点错误码：`1004001000 USER_NOT_EXISTS`。

### 4.2 签到转化率
- 分母为签到曝光 UV，不是访问会员中心 UV。
- 若签到能力被活动配置临时下线，则该窗口标记为 `not_applicable`，不纳入发布阻断。
- 与积分指标联动：签到转化率下降且积分一致率同时下滑，优先按“奖励发放异常”处置。

### 4.3 积分流水一致率
- 一致的定义：积分变更事件、业务台账、会员流水三者在 `memberId + sourceBizNo + bizType` 维度一一对应。
- 差异单进入 `point_reconcile_diff` 后必须在 24 小时内关闭。
- 重点错误码：`1004008000 POINT_RECORD_BIZ_NOT_SUPPORT`。

### 4.4 地址可用率
- `地址列表为空`、`默认地址为 null` 属于合法业务结果，计入可用。
- 仅接口超时、5xx、结构化字段缺失、地址写成功后读不到数据视为不可用。
- 重点错误码：`1004004000 ADDRESS_NOT_EXISTS`（趋势异常时需判断是否为真实空数据还是误判）。

### 4.5 资产账本一致率
- 一致的定义：券、积分、会员资产总账在 `memberId + sourceBizNo + assetType` 维度账变数量与余额结果一致。
- `degraded=true` 且带 `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901)` 的记录进入 `degraded_pool` 单独监控，不得计入主一致率。
- 若 `miniapp.asset.ledger=off` 仍出现 `1004009901`，不等待阈值判断，直接进入误发布处置流程。

## 5. 发布 KPI 门禁规则

| 规则编码 | 触发条件 | 门禁动作 | 责任人 |
|---|---|---|---|
| `RG-MEMBER-01` | `MEMBER_LOGIN_SUCCESS_RATE` 不达标 | 直接 `No-Go` | 账号体系值班 |
| `RG-MEMBER-02` | `MEMBER_POINT_LEDGER_CONSISTENCY_RATE` 不达标 | `No-Go`，冻结签到/积分相关投放 | 会员资产负责人 |
| `RG-MEMBER-03` | `MEMBER_ADDRESS_AVAILABILITY_RATE` 不达标 | `No-Go`，地址入口先读后写降级 | 会员基础资料负责人 |
| `RG-MEMBER-04` | `MEMBER_SIGNIN_CONVERSION_RATE` 单独不达标 | `Go with Watch`，15 分钟复核一次 | 会员增长运营 |
| `RG-MEMBER-05` | `MEMBER_ASSET_LEDGER_CONSISTENCY_RATE` 不达标或 `1004009901` 误返回 | 直接 `No-Go`，关闭 `miniapp.asset.ledger` | 会员资产负责人 + 发布负责人 |

## 6. 告警路由建议

| 指标 | 默认通知渠道 | 升级条件 | 升级对象 |
|---|---|---|---|
| 登录成功率 | IM + 电话 + 工单 | 连续 2 个窗口不达标 | 域负责人 + 发布负责人 |
| 签到转化率 | IM + 工单 | 连续 3 个窗口不达标 | 增长负责人 |
| 积分流水一致率 | IM + 工单 | 连续 2 个窗口不达标或 diff 单激增 | 域负责人 + 数据治理值班 |
| 地址可用率 | IM + 工单 | 连续 2 个窗口不达标 | 域负责人 + 发布负责人 |
| 资产账本一致率 | IM + 电话 + 工单 | 不达标或 `1004009901` 误返回 | 发布负责人 + 会员/促销账本负责人 |

## 7. 验收标准
1. 五个核心指标均具备公式、数据源、刷新频率、阈值、告警等级、Owner。
2. 指标异常能直接映射到发布 `Go/No-Go` 决策。
3. 所有告警都能通过 `runId/orderId/payRefundId/sourceBizNo/errorCode` 追溯。
4. `PLANNED_RESERVED` 的资产账本能力可在开关关闭状态下验证“误返回 = 0”。
