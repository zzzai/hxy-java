# Booking Refund-Commission Audit Ticket Sync - Window A Handoff

## 1. 变更摘要

1. 新增接口：
   - `POST /booking/four-account-reconcile/refund-commission-audit/sync-tickets`
2. 接口能力：
   - 按筛选条件批量将巡检异常 upsert 到统一工单；
   - 返回批次回执：`totalMismatchCount/attemptedCount/successCount/failedCount/failedOrderIds`。
3. 风险映射：
   - `REFUND_WITHOUT_REVERSAL` / `REVERSAL_WITHOUT_REFUND` -> `P0`, SLA `30m`
   - `REVERSAL_AMOUNT_MISMATCH` -> `P1`, SLA `120m`
4. 幂等与来源号：
   - `ticketType=40`
   - `sourceBizNo=REFUND_COMMISSION_AUDIT:<orderId>`
5. 容错策略：
   - 单条 upsert 失败不阻断批次（fail-open），失败订单 ID 回传。

## 2. 关键文件

1. `FourAccountReconcileController`（新增 sync endpoint）
2. `FourAccountReconcileService`（新增 sync 方法）
3. `FourAccountReconcileServiceImpl`（批量同步实现、严重级别映射、fail-open 计数）
4. 新增 VO：
   - `FourAccountRefundCommissionAuditSyncReqVO`
   - `FourAccountRefundCommissionAuditSyncRespVO`
5. 单测：
   - `FourAccountReconcileControllerTest`
   - `FourAccountReconcileServiceImplTest`

## 3. 联调注意

1. 前端触发同步前建议先跑分页筛选，避免无效批次。
2. `limit` 上限 1000，建议运营默认 200 分批执行。
3. 回执中若 `failedCount > 0`，可按 `failedOrderIds` 二次重试。
