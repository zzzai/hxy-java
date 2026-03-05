# Window A Handoff - Store Lifecycle Change Order (2026-03-05)

## Scope
- 分支：`feat/ui-four-account-reconcile-ops`
- 目标：P1 门店生命周期流程引擎最小闭环（开店/停业/闭店审批化）
- 约束：仅改 `product` 后端 + SQL + 治理文档，不改 overlay 页面

## Delivered
1. 新增生命周期变更单主表：`hxy_store_lifecycle_change_order`
2. 新增管理端接口：
   - `POST /product/store/lifecycle-change-order/create`
   - `POST /product/store/lifecycle-change-order/submit`
   - `POST /product/store/lifecycle-change-order/approve`
   - `POST /product/store/lifecycle-change-order/reject`
   - `POST /product/store/lifecycle-change-order/cancel`
   - `GET /product/store/lifecycle-change-order/get`
   - `GET /product/store/lifecycle-change-order/page`
3. 状态机与守卫：
   - `submit`：守卫预检并固化快照，不迁移门店状态
   - `approve`：二次复核守卫 + fromStatus 漂移校验，复核通过后才调用 `updateStoreLifecycle`
   - `reject/cancel`：仅变更单状态流转，不迁移门店状态
4. 审计字段：
   - `guard_snapshot_json/guard_blocked/guard_warnings`
   - `approve_operator/approve_remark/approve_time`

## Risk & Rollback
- 风险：审批时点若门店状态已变化会触发 `STORE_LIFECYCLE_CHANGE_ORDER_FROM_STATUS_CHANGED`，需前端引导重建变更单。
- 回滚：可禁用变更单入口，暂时回退到既有 `/product/store/update-lifecycle` 直改链路（接口未破坏）。

## Verification
- 待窗口 A 执行统一门禁与测试命令后补充最终结果。
