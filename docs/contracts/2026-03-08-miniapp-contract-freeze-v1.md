# MiniApp P0 Contract Freeze v1 (2026-03-08)

## 1. Scope
- Branch: `feat/ui-four-account-reconcile-ops`
- Goal: freeze miniapp P0 page anchors and backend error/degrade semantics for StageB gate.

## 2. P0 页面清单
- 支付结果
- 售后申请
- 售后列表
- 售后详情
- 退款进度
- 异常兜底

## 3. 关键错误码锚点
1. `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT` (`1030004012`)
- 语义：退款回调幂等冲突。

2. `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` (`1030004016`)
- 语义：runId不存在。

3. `TICKET_SYNC_DEGRADED`
- 语义：降级语义（fail-open/degrade），保留告警并继续主流程。

## 4. Freeze Rule
- 以上页面与错误码锚点作为 StageB 发布门禁的最小冻结集；新增字段仅允许向后兼容扩展。
