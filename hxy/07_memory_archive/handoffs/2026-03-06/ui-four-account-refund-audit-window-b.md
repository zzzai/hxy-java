# Window B Handoff - 四账退款佣金审计运营化收口（overlay）

## 日期
- 2026-03-06

## 分支
- `feat/ui-four-account-reconcile-ops`

## 变更范围
- 仅改 `overlay-vue3` 与本 handoff。
- 未改 Java 后端。
- 菜单 SQL 本批未新增（现有四账入口脚本已覆盖）。

## 本批交付
1. API 对齐（增量字段）
- 文件：`src/api/mall/booking/fourAccountReconcile.ts`
  - 退款审计分页/同步请求新增：
    - `refundAuditStatus`
    - `refundExceptionType`
    - `refundLimitSource`
    - `payRefundId`
    - `refundTimeRange`
  - 退款审计响应新增：
    - `refundEvidenceJson`
    - `refundAuditRemark`
    - `refundLimitSource`
    - `payRefundId`
    - `refundTime`
    - 兼容保留 `mismatchType`
- 文件：`src/api/mall/trade/afterSale/index.ts`
  - 售后详情/分页类型新增：
    - `refundAuditStatus`
    - `refundExceptionType`
    - `refundEvidenceJson`
    - `refundAuditRemark`
    - `payRefundId`
    - `refundTimeRange`

2. 四账页面增强
- 文件：`src/views/mall/booking/fourAccountReconcile/index.vue`
- 退款佣金审计区新增筛选：
  - 业务日期（原有）
  - 退款时间范围（`refundTimeRange`）
  - 审计状态（`refundAuditStatus`）
  - 异常类型（`refundExceptionType`，并兼容映射到 `mismatchType`）
  - 上限来源（`refundLimitSource`）
  - 订单号关键词、订单ID（原有）
  - 退款单ID（`payRefundId`）
- 列表新增展示列：
  - `payRefundId`
  - `refundTime`
  - `refundAuditStatus`
  - `refundExceptionType`（兼容 `mismatchType`）
  - `refundLimitSource`
  - `refundAuditRemark`
- 新增退款审计汇总卡：
  - 总数
  - 差异金额（分转元）
  - 未收口工单数
  - 后端未返回汇总字段时自动降级为列表近似统计（标记“汇总降级”）。
- 新增“查看证据”详情抽屉：
  - `refundEvidenceJson` 可解析时结构化展示
  - 解析失败时提示“证据解析失败（原文保留）”并保留原文
  - 空值统一 `--`

3. 售后详情页增强
- 文件：`src/views/mall/trade/afterSale/detail/index.vue`
- 售后信息区新增字段可读性展示：
  - `refundAuditStatus`
  - `refundExceptionType`
  - `payRefundId`
  - `refundTime`
  - `refundAuditRemark`
  - `refundEvidenceJson`（结构化+解析失败原文保留）
- 退款失败错误文案映射增强：
  - `1011000125`：子项台账优先命中提示（保留原始信息）
  - `1030004012` 或关键字 `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT`：幂等冲突提示（保留原始信息）
  - `1030004011` 或关键字 `BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID`/`merchantRefundId invalid`：非法 merchantRefundId 提示（保留原始信息）

## 手工验收清单（步骤 + 预期）
1. 退款审计筛选联动
- 步骤：在“四账-退款佣金审计”区依次选择业务日期、退款时间、审计状态、异常类型、上限来源，输入订单号关键词/订单ID/退款单ID后查询。
- 预期：请求参数按字段透传：`beginBizDate/endBizDate/refundTimeRange/refundAuditStatus/refundExceptionType/refundLimitSource/keyword/orderId/payRefundId`，页面正常刷新。

2. 分页与空值兜底
- 步骤：切换分页与每页条数，观察含空字段行。
- 预期：分页生效；空值统一显示 `--`，无 `null/undefined` 泄漏。

3. 汇总卡展示与降级
- 步骤：执行查询，观察“总数/差异金额/未收口工单”。
- 预期：有汇总字段时展示服务端值；无汇总字段时自动降级为列表近似统计并显示“汇总降级”标记，不阻断列表。

4. 证据详情抽屉（可解析）
- 步骤：点击任一行“查看证据”，构造合法 `refundEvidenceJson`。
- 预期：抽屉内结构化键值展示、金额列分转元，原文区保留原 JSON。

5. 证据详情抽屉（解析失败）
- 步骤：构造非法 `refundEvidenceJson`（如缺失引号），点击“查看证据”。
- 预期：显示“证据解析失败（原文保留）”提示；原文文本框显示完整原串；页面不崩溃。

6. 售后详情字段展示
- 步骤：打开售后详情页，检查退款审计相关字段。
- 预期：`refundAuditStatus/refundExceptionType/payRefundId/refundTime/refundAuditRemark/refundEvidenceJson` 可读展示；空值为 `-`。

7. 退款失败文案映射
- 步骤：模拟退款接口返回错误码 `1011000125`、`1030004012`、`1030004011`（或对应关键字报文）。
- 预期：弹出明确提示文案，并附带“原始信息”内容，便于排查。

8. 同步工单不回退
- 步骤：在新筛选条件下执行“同步工单”，查看结果弹窗。
- 预期：原有确认、结果统计、失败订单ID复制能力保留；失败时筛选条件不丢失。

## 与窗口A联调字段清单
- 退款审计分页请求：`refundAuditStatus/refundExceptionType/refundLimitSource/payRefundId/refundTimeRange`。
- 退款审计分页响应：`refundEvidenceJson/refundAuditRemark/refundLimitSource/payRefundId/refundTime/refundExceptionType`（兼容 `mismatchType`）。
- 售后详情响应：`refundAuditStatus/refundExceptionType/refundEvidenceJson/refundAuditRemark/payRefundId/refundTime`。
