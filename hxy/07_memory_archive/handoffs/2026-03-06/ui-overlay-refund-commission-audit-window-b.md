# Window B Handoff - 四账退款-提成联调巡检页（overlay）

## 日期
- 2026-03-06

## 分支
- `feat/ui-four-account-reconcile-ops`

## 变更范围
- 仅改 `overlay-vue3` + 本 handoff。
- 未改 Java 后端，未改菜单 SQL。

## 本批交付
1. API 扩展（四账 API 内）
- 文件：`src/api/mall/booking/fourAccountReconcile.ts`
- 新增接口：
  - `GET /booking/four-account-reconcile/refund-commission-audit-page`
- 新增请求类型：`FourAccountRefundCommissionAuditPageReq`
  - `beginBizDate/endBizDate/mismatchType/keyword/orderId/pageNo/pageSize`
- 新增响应类型：`FourAccountRefundCommissionAuditVO`
  - `orderId/tradeOrderNo/userId/payTime/refundPrice/settledCommissionAmount/reversalCommissionAmountAbs/activeCommissionAmount/expectedReversalAmount/mismatchType/mismatchReason`

2. 页面接入（四账运营页）
- 文件：`src/views/mall/booking/fourAccountReconcile/index.vue`
- 新增“退款-提成巡检”区块：
  - 独立筛选 + 独立列表 + 独立分页
  - 不影响原四账 summary 与原列表功能
- 筛选项：
  - 日期区间（映射 `beginBizDate/endBizDate`）
  - 异常类型（固定枚举）
  - 订单号关键词（`keyword`）
  - 订单ID（`orderId`）
- 列字段：
  - 订单号、退款金额、已结算提成、冲正金额、期望冲正、异常类型、异常原因、支付时间
  - 额外保留 `orderId` 列便于排查
- 展示规范：
  - 空值统一 `--`
  - 金额统一分转元展示
  - 金额列增加 tooltip 显示原始分值（可选增强已实现）

3. 交互约束
- 异常类型固定选项：
  - `REFUND_WITHOUT_REVERSAL`
  - `REVERSAL_WITHOUT_REFUND`
  - `REVERSAL_AMOUNT_MISMATCH`
- 巡检查询失败时：
  - 仅当前巡检区块提示可读错误
  - 不阻断四账页面其它功能

4. 同步工单收口（本批追加）
- 新增接口：
  - `POST /booking/four-account-reconcile/refund-commission-audit/sync-tickets`
- 同步入参与当前筛选一致：
  - `beginBizDate/endBizDate/mismatchType/keyword/orderId/limit`
- `limit` 默认 `200`，可配置区间 `1~1000`。
- 仅用户二次确认后触发同步，避免误操作。
- 同步结果弹窗展示：
  - `totalMismatchCount/attemptedCount/successCount/failedCount`
  - `failedOrderIds` 可展开查看并支持复制。
- 同步失败时不清空筛选条件，便于二次重试。
- 空数据/接口异常/字段缺失统一兜底，不阻断其它区块。

## 手工验收清单
1. 筛选
- 步骤：在“退款-提成巡检”区块设置日期区间、异常类型、关键词、订单ID后查询。
- 预期：请求参数正确映射到 `beginBizDate/endBizDate/mismatchType/keyword/orderId`。

2. 分页
- 步骤：切换页码、每页条数。
- 预期：请求携带 `pageNo/pageSize`，列表与总数联动更新。

3. 异常类型显示
- 步骤：查看三种异常类型数据。
- 预期：页面文案分别显示“退款未冲正/冲正未退款/冲正金额不一致”。

4. 空值降级
- 步骤：构造部分字段为空的数据（金额、原因、支付时间等）。
- 预期：统一展示 `--`，页面无 `null/undefined` 泄漏。

5. 查询失败容错
- 步骤：模拟巡检接口异常（5xx/网络错误）。
- 预期：仅巡检区块报错提示“退款-提成巡检查询失败...”，四账 summary 与原四账列表可继续使用。

6. 同步确认
- 步骤：点击“同步工单”。
- 预期：先弹确认框，确认后才调用同步接口。

7. 同步结果展示
- 步骤：执行一次同步并查看结果弹窗。
- 预期：展示 `totalMismatchCount/attemptedCount/successCount/failedCount`，空字段可兜底显示。

8. 失败订单ID展开与复制
- 步骤：当 `failedOrderIds` 非空时展开折叠面板并点击复制。
- 预期：可见完整失败订单ID列表，复制成功提示可见。

9. 同步失败重试
- 步骤：模拟同步接口失败后再次点击同步。
- 预期：筛选条件保持不变，可直接二次重试。
