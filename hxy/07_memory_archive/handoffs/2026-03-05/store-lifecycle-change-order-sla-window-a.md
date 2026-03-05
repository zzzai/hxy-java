# Window A Handoff - Store Lifecycle Change Order SLA Ops (2026-03-05)

## Scope
- 分支：`feat/ui-four-account-reconcile-ops`
- 目标：生命周期变更单运营化增强（SLA 超时自动收口 + 动作审计字段）
- 约束：仅改 `product` 后端 + SQL + 治理文档

## Delivered
1. 变更单字段增强：
   - `submitTime`
   - `slaDeadlineTime`
   - `lastActionCode/lastActionOperator/lastActionTime`
2. 提交/审批/驳回/取消动作统一写 `lastAction*`。
3. 分页增强：
   - 新增 `overdue`（仅待审批且超时）
   - 新增 `lastActionCode`、`lastActionOperator` 筛选
4. 新增超时收口任务：
   - `ProductStoreLifecycleChangeOrderExpireJob`
   - 扫描 `PENDING && slaDeadlineTime < now`
   - 自动收口到 `CANCELLED`
   - 备注 `SYSTEM_SLA_EXPIRED`
   - 动作 `EXPIRE`
   - 不触发门店生命周期迁移
5. SQL 脚本：
   - `2026-03-05-hxy-store-lifecycle-change-order-sla.sql`（幂等加列与索引）

## Risk & Rollback
- 风险：SLA 时长配置过短会导致待审单过快过期；当前已提供默认与上下限钳制。
- 回滚：可临时停用过期 Job，仅保留字段与筛选能力，不影响审批主链路。

## Verification
- 由窗口A统一执行 `diff/naming/memory/mvn` 四条门禁命令并回传结果。
