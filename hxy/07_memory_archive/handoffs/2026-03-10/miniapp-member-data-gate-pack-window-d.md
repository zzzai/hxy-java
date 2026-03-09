# MiniApp Member Data Gate Pack - Window D Handoff (2026-03-10)

## 1. 变更摘要
- 新增会员域 KPI 与告警文档：
  - `docs/plans/2026-03-10-miniapp-member-domain-kpi-and-alerts-v1.md`
  - 补齐登录成功率、签到转化率、积分流水一致率、地址可用率、资产账本一致率五项指标。
  - 每项指标均明确公式、数据源、刷新频率、阈值、告警等级、Owner，并与发布门禁联动。
- 新增 ACTIVE / PLANNED_RESERVED 运行手册：
  - `docs/plans/2026-03-10-miniapp-active-planned-gate-runbook-v1.md`
  - 明确会员域 ACTIVE 与 `miniapp.asset.ledger` 保留能力的门禁流程、误发布处置、错误码与开关策略。
- 新增会员域告警路由与 SLA 文档：
  - `docs/plans/2026-03-10-miniapp-member-domain-sla-routing-v1.md`
  - 明确 P0 / P1 / P2 响应时限、升级链路，以及五键字段与 `"0"` 占位规则。

## 2. 关键收口
- 会员域门禁不再只看通用 DQ 指标，新增会员专属五项 KPI。
- `ACTIVE` 与 `PLANNED_RESERVED` 拆开执行：
  - `ACTIVE`：登录、签到、积分、地址
  - `PLANNED_RESERVED`：资产账本 `miniapp.asset.ledger`
- `1004009901 MINIAPP_ASSET_LEDGER_MISMATCH` 在开关关闭态返回，固定按误发布/P1 配置事故处理。
- 工单与告警统一携带：`runId/orderId/payRefundId/sourceBizNo/errorCode`；无值时填 `"0"`。

## 3. 验证命令
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

## 4. 对 A / B / C 联调提醒
- A（集成 / 发布）：
  - 会员域发布门禁需新增 `RG-MEMBER-*` 判断，不可只看通用 `RG-KPI-*`。
  - `PLANNED_RESERVED` 必须校验开关快照与灰度范围，禁止默认对外返回 `1004009901`。
- B（产品 / 运营）：
  - 签到异常优先下线活动或奖励发放配置，不允许继续放量。
  - 地址链路异常时先只读降级，避免继续放大写故障。
- C（契约 / 后端）：
  - 错误码重点对齐：`1004001000`、`1004004000`、`1004008000`、`1004009901`。
  - 所有告警、工单、门禁日志继续强制透传五键；无值时按 `"0"` 规则填充，不得省略字段。
