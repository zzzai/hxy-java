# MiniApp Data Ops Governance - Window D Handoff (2026-03-09)

## 1. 变更摘要
- 新增门店经营看板规范：
  - `docs/plans/2026-03-09-miniapp-store-operations-dashboard-spec-v1.md`
  - 明确五层指标（增长/交易/履约/售后/门店经营），逐项定义公式、口径、数据源、刷新频率、阈值、告警级别、Owner。
  - 明确 `degraded_pool` 与 `main_pool` 分池规则，降级流量不计入主成功率/ROI。
- 新增实验登记与治理规范：
  - `docs/plans/2026-03-09-miniapp-experiment-registry-and-governance-v1.md`
  - 包含登记模板、准入门槛、样本量、停机线、回滚条件、复盘模板。
  - 强制实验可关联 `orderId/afterSaleId/payRefundId/storeId`。
- 新增数据质量SLO与告警规范：
  - `docs/plans/2026-03-09-miniapp-data-quality-slo-and-alerting-v1.md`
  - 覆盖唯一性、关联完整性、状态可达性、金额平衡、时序合法、幂等一致。
  - 明确 BLOCK/WARN 分级、处置时限与工单SLA。

## 2. 关键口径收口
- 统一检索键：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
- 成功指标仅统计后端确认成功。
- 降级流量单独统计并做损耗监控，不参与主口径KPI/ROI。

## 3. 验证命令与结果
1. `git diff --check`：PASS
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`：PASS
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`：PASS

## 4. 对A/B/C联调提醒
- A（集成）：看板聚合层必须按 `main_pool/degraded_pool` 双池计算，禁止混算。
- B（UI/交互）：前端成功态展示要与后端 `resultCode` 对齐，降级只显示“处理中/重试”。
- C（契约）：实验与看板接口必须透传 `experimentId/storeId/orderId/afterSaleId/payRefundId`，错误码保持字符串化。
