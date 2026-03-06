# Four Account Refund Audit Backend - Window A Handoff

## 1. 变更摘要

1. 四账台账接口增强：
   - `GET /booking/four-account-reconcile/page` 新增筛选：
     - `refundAuditStatus`
     - `refundExceptionType`
     - `refundLimitSource`
     - `payRefundId`
     - `refundTimeRange`
   - `GET /booking/four-account-reconcile/page|get` 响应新增：
     - `payRefundId/refundTime/refundLimitSource/refundExceptionType`
     - `refundAuditStatus/refundAuditRemark/refundEvidenceJson`
     - `refundEvidence/refundEvidenceJsonParseError`
2. JSON 降级解析：
   - `refundEvidenceJson` 非法时不抛异常，返回原文并标记 `refundEvidenceJsonParseError=true`。
3. 新增退款审计汇总接口：
   - `GET /booking/four-account-reconcile/refund-audit-summary`
   - 返回总数、差异金额聚合、状态聚合、异常类型聚合、未收口工单数。
   - trade 工单摘要异常时 fail-open 降级（`ticketSummaryDegraded=true`）。
4. `runReconcile` 固化退款审计快照：
   - 新增快照字段写入 `hxy_four_account_reconcile`。
   - 无异常写 `PASS`，有异常写 `WARN` 并写证据 JSON。

## 2. 数据与契约

1. 新增 SQL：
   - `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-06-hxy-four-account-refund-audit-columns.sql`
2. `hxy_four_account_reconcile` 新增列：
   - `pay_refund_id`
   - `refund_time`
   - `refund_limit_source`
   - `refund_exception_type`
   - `refund_audit_status`
   - `refund_audit_remark`
   - `refund_evidence_json`

## 3. 测试覆盖

1. `FourAccountReconcileControllerTest`
   - `/get` 非法 `refundEvidenceJson` 降级测试
   - `/refund-audit-summary` 委托测试
2. `FourAccountReconcileServiceImplTest`
   - `runReconcile` 退款审计快照写入测试
   - `/page` 新增筛选参数规范化测试
   - `refund-audit-summary` 聚合与降级测试
3. 回归：
   - `BookingOrderServiceImplTest`
   - `AfterSaleReviewTicketServiceImplTest`

## 4. 联调注意（窗口 B/C）

1. 前端可直接使用新增筛选字段与响应字段，不需要额外拼装。
2. `refundEvidenceJson` 不保证永远是合法 JSON，需优先读取 `refundEvidenceJsonParseError`。
3. 汇总接口差异金额口径为 `abs(tradeMinusCommissionSplit)` 聚合。
4. 当 `ticketSummaryDegraded=true` 时，`unresolvedTicketCount` 可能偏保守（降级为 0 或部分可得值）。
