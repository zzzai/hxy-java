# Booking Refund-Commission Audit - Window A Handoff

## 1. 变更摘要

1. 新增退款-提成联调巡检分页接口：
   - `GET /booking/four-account-reconcile/refund-commission-audit-page`
2. 新增巡检请求/响应模型：
   - `FourAccountRefundCommissionAuditPageReqVO`
   - `FourAccountRefundCommissionAuditRespVO`
3. 新增候选聚合查询模型与 mapper：
   - `FourAccountRefundCommissionAuditRow`
   - `FourAccountReconcileQueryMapper.selectRefundCommissionAuditCandidates`
4. service 层新增异常判定规则（三类）：
   - `REFUND_WITHOUT_REVERSAL`
   - `REVERSAL_WITHOUT_REFUND`
   - `REVERSAL_AMOUNT_MISMATCH`
5. 新增单测覆盖：
   - Controller 委托测试
   - Service 异常判定与筛选测试

## 2. 接口行为

1. 输入条件：`beginBizDate/endBizDate/mismatchType/keyword/orderId + pageNo/pageSize`
2. 查询口径：
   - 按 `trade_order.pay_time` 时间窗拉取支付订单候选
   - 聚合 `technician_commission` 正向已结算金额与有效冲正金额
3. 输出内容：
   - 订单维度审计字段（退款额、正向已结算提成、有效冲正额、期望冲正额、净额）
   - 异常类型与异常原因

## 3. 风险与注意

1. 当前巡检为“读取+判定”模型，不会阻断支付/退款主链路。
2. 默认时间窗为最近 7 天；高数据量场景可在管理端显式缩短窗口。
3. `REVERSAL_AMOUNT_MISMATCH` 口径当前对比“有效冲正额 vs 已结算正向提成额”，若未来引入部分退款比例冲正策略，需要同步扩展规则。
