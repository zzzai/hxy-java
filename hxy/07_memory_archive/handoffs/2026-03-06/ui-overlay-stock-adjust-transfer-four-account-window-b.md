# Window B Handoff - 库存调整/调拨审批 + 四账运营看板（overlay）

## 日期
- 2026-03-06

## 分支
- `feat/ui-four-account-reconcile-ops`

## 变更范围
- 仅改 overlay 前端 + 菜单 SQL + 本 handoff。
- 未改 Java 后端，未改治理文档。

## 本批交付
1. 门店 SKU 库存调整单审批页
- 新增 API：`/product/store-sku/stock-adjust-order/{page|get|submit|approve|reject|cancel}`。
- 新增页面：`mall/store/stockAdjustOrder/index`。
- 支持筛选：`orderNo/storeId/skuId/status/bizType/applyOperator/createTime`。
- 支持详情抽屉：基础字段、审批字段、审计字段（`lastActionCode/lastActionOperator/lastActionTime`）。
- 支持行内状态动作：`submit/approve/reject/cancel`（按状态显隐）。
- `detailJson` 非法时 fail-soft：提示“明细解析失败（原文保留）”，并保留原文。

2. 跨店调拨审批页（契约预留 + fail-soft）
- 新增 API：`/product/store-sku/transfer-order/{page|get|submit|approve|reject|cancel}`。
- 新增页面：`mall/store/transferOrder/index`。
- 支持列表、详情抽屉、状态动作。
- 后端接口未就绪时：
  - 页面顶部明确提示“调拨审批接口未就绪”；
  - 动作按钮统一禁用，并显示“接口未就绪”提示；
  - 详情/动作会被拦截，不再触发误操作。
- `detailJson` 非法时 fail-soft：提示并保留原文。

3. 四账运营看板增强
- 在原页面新增 summary 区域（统计卡片）：
  - 总笔数
  - PASS/WARN
  - 差异金额
  - 未收口工单数
- 日期区间筛选与列表、summary 联动。
- 保持既有能力不回退：
  - 跳转工单
  - 复制来源号
  - 详情抽屉 + issueDetailJson 降级
- 当 `/booking/four-account-reconcile/summary` 未就绪时，自动降级为“列表近似统计”并显式告警。

4. 菜单与权限 SQL（幂等）
- 新增幂等脚本：
  - 库存调整单审批菜单 + 子权限
  - 跨店调拨审批菜单 + 子权限
  - 四账入口更新为“四账运营看板”（含子权限补齐）
- 角色授权幂等：`admin(1)`、`operator(2)`。

## 手工验收清单
1. 库存调整单入口
- 步骤：从菜单进入“库存调整单审批”。
- 预期：页面可打开，列表可加载，空数据不报错。

2. 库存调整单筛选
- 步骤：分别输入 `orderNo/storeId/skuId/status/bizType/applyOperator/createTime` 后搜索。
- 预期：请求参数带齐；重置后回到默认分页。

3. 库存调整单动作
- 步骤：
  - 草稿单点击“提交”；
  - 待审批单点击“审批通过/驳回”；
  - 草稿/待审批点击“取消”。
- 预期：动作按状态显隐；确认后请求成功并刷新列表。

4. 库存调整单详情 + 降级
- 步骤：打开详情抽屉，分别验证合法与非法 `detailJson` 数据。
- 预期：
  - 合法 JSON 展示结构化明细；
  - 非法 JSON 显示“明细解析失败（原文保留）”，页面不崩溃。

5. 调拨审批入口与未就绪降级
- 步骤：进入“跨店调拨审批”。
- 预期：
  - 若接口已就绪：正常列表/详情/动作；
  - 若接口未就绪：顶部告警可见，动作按钮禁用，提示“接口未就绪”。

6. 调拨审批筛选与详情
- 步骤：使用筛选项搜索，打开详情抽屉。
- 预期：参数透传；详情可读；非法 `detailJson` 时降级展示原文。

7. 四账 summary 联动
- 步骤：切换业务日期区间并搜索。
- 预期：列表与 summary 同步刷新。

8. 四账既有能力回归
- 步骤：
  - 点击“跳转工单”；
  - 点击“复制来源号”；
  - 打开详情抽屉。
- 预期：能力保持可用，无回退。

9. 空值兜底
- 步骤：检查列表与详情中的可空字段。
- 预期：统一显示 `--`，无 `undefined/null` 泄漏。

## 与窗口A联调字段清单
1. 库存调整单（已对接）
- 列表请求：`pageNo,pageSize,orderNo,storeId,skuId,status,bizType,applyOperator,createTime[]`。
- 列表/详情返回关键字段：
  - `id,orderNo,storeId,storeName,bizType,reason,remark,status,detailJson`
  - `applyOperator,applySource,approveOperator,approveRemark,approveTime`
  - `lastActionCode,lastActionOperator,lastActionTime,createTime`
- 动作请求：`id,remark?`（submit/approve/reject/cancel）。

2. 调拨单（契约预留）
- 列表请求：`pageNo,pageSize,orderNo,outStoreId,inStoreId,skuId,status,bizType,applyOperator,createTime[]`。
- 列表/详情建议返回关键字段：
  - `id,orderNo,outStoreId,outStoreName,inStoreId,inStoreName,bizType,reason,remark,status,detailJson`
  - `applyOperator,applySource,approveOperator,approveRemark,approveTime`
  - `lastActionCode,lastActionOperator,lastActionTime,createTime`
- 动作请求：`id,remark?`（submit/approve/reject/cancel）。

3. 四账 summary（建议接口）
- `GET /booking/four-account-reconcile/summary`
- 入参：`bizDate[],status,source,issueCode`
- 返回：`totalCount,passCount,warnCount,diffAmount,openTicketCount`
