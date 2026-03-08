# Window A Handoff - MiniApp Doc Gap Closure (2026-03-08)

## 1) 变更摘要
- 在 A 窗口完成小程序缺失文档全量补齐（P0/P1/P2）。
- 新增文档覆盖：IA 路由、统一状态机、错误码恢复、降级重试、发布回滚、活动真值、同源刷新、工单审计、文案规范、埋点漏斗、推荐可解释性、动效与无障碍。
- 新增文档索引，避免后续窗口重复定义。
- 同步更新长记忆：事实基线、ADR、执行状态看板。

## 2) 新增文档
- `docs/products/miniapp/2026-03-08-miniapp-ia-routing-map-v1.md`
- `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
- `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
- `docs/plans/2026-03-08-miniapp-degrade-retry-playbook-v1.md`
- `docs/plans/2026-03-08-miniapp-go-live-checklist-and-rollback-v1.md`
- `docs/contracts/2026-03-08-miniapp-activity-truth-binding-spec-v1.md`
- `docs/contracts/2026-03-08-miniapp-catalog-sync-consistency-spec-v1.md`
- `docs/contracts/2026-03-08-miniapp-ticket-audit-linkage-spec-v1.md`
- `docs/products/miniapp/2026-03-08-miniapp-copy-terminology-spec-v1.md`
- `docs/plans/2026-03-08-miniapp-analytics-funnel-spec-v1.md`
- `docs/contracts/2026-03-08-miniapp-recommendation-explainability-spec-v1.md`
- `docs/plans/2026-03-08-miniapp-motion-accessibility-performance-spec-v1.md`
- `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`

## 3) 长记忆同步
- `hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md`
- `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`（新增 ADR-101）
- `hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md`

## 4) 联调注意点
- 端侧交互必须以后端状态机与错误码为准，不可用文案推断状态。
- 动效成功反馈只能绑定真实后端成功事件（尤其领券、加购、退款链路）。
- 降级路径必须保留可恢复动作与可检索字段（`runId/orderId/payRefundId/sourceBizNo/errorCode`）。
