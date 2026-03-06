# Window B Handoff - 库存调整单/跨店调拨/四账汇总运营页联调收口（overlay）

## 日期
- 2026-03-06

## 分支
- `feat/ui-four-account-reconcile-ops`

## 变更范围
- 仅改 overlay 前端 + 菜单 SQL + 本 handoff。
- 未改 Java 后端与治理文档。

## 本批收口
1. 库存调整单页面（store-sku stock-adjust-order）
- 已对接接口：
  - `GET /product/store-sku/stock-adjust-order/page|get`
  - `POST /product/store-sku/stock-adjust-order/create|submit|approve|reject|cancel`
- 列表筛选字段已按后端对齐：
  - `orderNo/storeId/status/bizType/applyOperator/lastActionCode/lastActionOperator/createTime`
- 增加“新建调整单”弹窗（`storeId/bizType/reason/remark/applySource/items[skuId,incrCount]`）。
- 详情抽屉支持 `detailJson` 结构化渲染；非法 JSON 时降级为“明细解析失败（原文保留）”。

2. 跨店调拨页面（store-sku transfer-order）
- 已对接接口：
  - `GET /product/store-sku/transfer-order/page|get`
  - `POST /product/store-sku/transfer-order/create|submit|approve|reject|cancel`
- 列表筛选字段已按后端对齐：
  - `orderNo/fromStoreId/toStoreId/status/applyOperator/lastActionCode/lastActionOperator/createTime`
- 增加“新建调拨单”弹窗（`fromStoreId/toStoreId/reason/remark/applySource/items[skuId,quantity]`）。
- 状态按钮按状态机显隐：
  - `DRAFT(0)`：提交、取消
  - `PENDING(10)`：审批通过、驳回、取消
  - `APPROVED(20)/REJECTED(30)/CANCELLED(40)`：无状态动作
- 详情抽屉支持 `detailJson` 结构化渲染；非法 JSON 时降级为“明细解析失败（原文保留）”。

3. 四账汇总卡片（booking four-account-reconcile）
- 已接入：`GET /booking/four-account-reconcile/summary`
- 汇总条件：`bizDate/status/relatedTicketLinked`
- 展示字段：
  - `totalCount`
  - `passCount`
  - `warnCount`
  - `tradeMinusFulfillmentSum`
  - `tradeMinusCommissionSplitSum`
  - `unresolvedTicketCount`
- `ticketSummaryDegraded=true` 时显示降级标识（tag），不阻断列表。
- 若 summary 请求异常，回落为列表近似统计并告警提示，同样不阻断列表。

4. 菜单 SQL（幂等）
- 新增幂等脚本：
  - 库存调整单审批入口
  - 跨店调拨审批入口
  - 四账入口统一为“四账运营看板”
- 避免重复命名/重复授权：
  - stock/transfer 页面复用既有 `product:store-sku:query/update` 权限，不重复创建同名按钮权限菜单。
  - 角色授权使用 `NOT EXISTS` 幂等写法。

## 手工验收清单
1. 入口
- 步骤：依次进入“库存调整单审批”“跨店调拨审批”“四账运营看板”。
- 预期：页面均可打开，空数据不报错。

2. 库存调整单筛选
- 步骤：分别设置 `orderNo/storeId/status/bizType/applyOperator/lastActionCode/lastActionOperator/createTime` 后搜索。
- 预期：请求参数与后端分页 VO 一致，重置后恢复默认。

3. 库存调整单状态动作
- 步骤：
  - 草稿单执行提交、取消；
  - 待审批单执行审批通过、驳回、取消。
- 预期：动作按状态显隐，执行成功后列表刷新。

4. 库存调整单详情降级
- 步骤：打开合法/非法 `detailJson` 的详情。
- 预期：
  - 合法：结构化明细显示；
  - 非法：出现“明细解析失败（原文保留）”，原文可见，页面不崩溃。

5. 跨店调拨筛选
- 步骤：分别设置 `orderNo/fromStoreId/toStoreId/status/applyOperator/lastActionCode/lastActionOperator/createTime` 后搜索。
- 预期：请求参数与后端分页 VO 一致。

6. 跨店调拨状态动作
- 步骤：验证 DRAFT/PENDING/终态记录的按钮显隐与可执行动作。
- 预期：与状态机一致（仅 DRAFT/PENDING 可操作）。

7. 跨店调拨详情降级
- 步骤：打开合法/非法 `detailJson` 的详情。
- 预期：合法结构化显示；非法时“原文保留”降级可见。

8. 四账汇总联动
- 步骤：变更 `bizDate/status/relatedTicketLinked` 后点击搜索。
- 预期：summary 与列表同步刷新。

9. 四账降级标识
- 步骤：构造 `ticketSummaryDegraded=true` 数据或模拟工单摘要降级。
- 预期：summary 区域显示降级标识，不影响列表查询与详情。

10. 复制/跳转能力回归
- 步骤：在四账列表点击“复制来源号”“跳转工单”，并打开详情抽屉。
- 预期：复制成功；跳转携带 `ticketType=40&sourceBizNo=...`；详情能力不回退。

## 与窗口A联调字段清单
1. 库存调整单（store-sku stock-adjust-order）
- 列表请求：
  - `pageNo/pageSize/orderNo/storeId/status/bizType/applyOperator/lastActionCode/lastActionOperator/createTime[]`
- 详情返回关键字段：
  - 头信息：`id/orderNo/storeId/storeName/bizType/reason/remark/status`
  - 审计信息：`applyOperator/applySource/approveOperator/approveRemark/approveTime`
  - 最近动作：`lastActionCode/lastActionOperator/lastActionTime`
  - 时间：`createTime`
  - 明细：`detailJson`（结构化字段使用 `items[].skuId/items[].incrCount`）
- 创建请求：
  - `storeId/bizType/reason/remark?/applySource?/items[{skuId,incrCount}]`
- 状态动作请求：
  - `submit/approve/reject/cancel` 均为 `id + remark?`

2. 跨店调拨（store-sku transfer-order）
- 列表请求：
  - `pageNo/pageSize/orderNo/fromStoreId/toStoreId/status/applyOperator/lastActionCode/lastActionOperator/createTime[]`
- 详情返回关键字段：
  - 头信息：`id/orderNo/fromStoreId/fromStoreName/toStoreId/toStoreName/reason/remark/status`
  - 审计信息：`applyOperator/applySource/approveOperator/approveRemark/approveTime`
  - 最近动作：`lastActionCode/lastActionOperator/lastActionTime`
  - 时间：`createTime`
  - 明细：`detailJson`（结构化字段使用 `items[].skuId/items[].quantity`）
- 创建请求：
  - `fromStoreId/toStoreId/reason/remark?/applySource?/items[{skuId,quantity}]`
- 状态动作请求：
  - `submit/approve/reject/cancel` 均为 `id + remark?`

3. 四账汇总（booking four-account-reconcile）
- 列表请求（现有不回退）：
  - `pageNo/pageSize/bizDate[]/status/source/issueCode`
- 汇总请求：
  - `bizDate[]/status/relatedTicketLinked`
- 汇总返回字段：
  - `totalCount/passCount/warnCount/tradeMinusFulfillmentSum/tradeMinusCommissionSplitSum/unresolvedTicketCount/ticketSummaryDegraded`
- 降级约定：
  - 若 summary 异常，前端降级为列表近似统计并提示告警，不阻断列表与详情。
