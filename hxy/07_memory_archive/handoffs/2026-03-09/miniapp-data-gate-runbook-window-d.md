# MiniApp Data Gate Runbook - Window D Handoff (2026-03-09)

## 1. 变更摘要
- 新增 `degraded_pool` 治理文档：
  - `docs/plans/2026-03-09-miniapp-degraded-pool-governance-v1.md`
  - 定义入池/出池、主池排除、降级损耗与复盘机制。
- 新增发布门禁 KPI 运行手册：
  - `docs/plans/2026-03-09-miniapp-release-gate-kpi-runbook-v1.md`
  - 明确阈值、观察窗口、Go/No-Go、TopN 错误码策略与升级路径。
- 新增告警路由与值班SLA文档：
  - `docs/plans/2026-03-09-miniapp-alert-routing-and-oncall-sla-v1.md`
  - 明确产品/运营/技术/门店告警流，P0/P1/P2响应链路。

## 2. 关键收口
- `degraded_pool` 明确不得计入主成功率、主ROI、北极星指标。
- 发布门禁引入可执行 `RG-KPI-*` 判定规则，支持 Go/No-Go。
- 工单闭环强制携带五键：`runId/orderId/payRefundId/sourceBizNo/errorCode`。

## 3. 验证命令与结果
1. `git diff --check`：PASS
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`：PASS
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`：PASS

## 4. 对A/B/C联调提醒
- A（集成）：门禁计算必须先分池再聚合，严禁 `degraded_pool` 与 `main_pool` 混算。
- B（产品/运营）：发布决策按 `RG-KPI-*` 走，不再靠单一主观判断；P0项任一命中即 No-Go。
- C（契约/后端）：所有告警与工单接口要保证五键字段透传与错误码字符串化。
