# Window B Handoff - 售后退款子项台账优先联调收口（overlay）

## 日期
- 2026-03-06

## 分支
- `feat/ui-four-account-reconcile-ops`

## 变更范围
- 仅改 `overlay-vue3` + 本 handoff。
- 未改 Java 后端，未改非 overlay 文件，未新增菜单 SQL。

## 本批收口
1. 售后单列表/详情新增退款上限字段展示
- 列表新增展示：
  - `refundLimitSource`
  - `refundLimitSourceLabel`
  - `refundLimitRuleHint`
  - `refundLimitDetailJson`（可解析时结构化预览，解析失败保留原文）
- 详情新增展示：
  - `refundLimitSource`
  - `refundLimitSourceLabel`
  - `refundLimitRuleHint`
  - `refundLimitDetailJson`（可解析时结构化键值展示；解析失败显示告警并保留原文）

2. 退款失败错误码联调
- 在“确认退款”操作中，命中 `1011000125`（`AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED`）时：
  - 显示明确提示：命中子项台账优先校验，存在已履约子项且超出可退上限；
  - 同时拼接并保留后端原始错误信息。

3. 筛选增强
- 列表新增 `refundLimitSource` 筛选，支持：
  - `CHILD_LEDGER`
  - `FALLBACK_SNAPSHOT`

## 手工验收清单
1. 入口
- 步骤：进入售后退款列表，再进入任意售后详情页。
- 预期：页面正常渲染，无报错。

2. 列表字段展示
- 步骤：检查列表“退款上限来源/规则提示/上限审计明细”列。
- 预期：
  - `refundLimitSource/refundLimitSourceLabel/refundLimitRuleHint`可见；
  - `refundLimitDetailJson` 可解析时显示结构化预览；不可解析时保留原文。

3. 详情字段展示与降级
- 步骤：打开详情，分别验证合法和非法 `refundLimitDetailJson`。
- 预期：
  - 合法 JSON：按键值结构化展示；
  - 非法 JSON：出现解析失败告警，并显示原文 textarea；页面不崩溃。

4. 退款错误码提示
- 步骤：在详情点击“确认退款”，构造后端返回 `code=1011000125`。
- 预期：
  - 前端提示包含“子项台账优先校验/已履约子项超上限”明确文案；
  - 同时保留并展示后端原始错误信息。

5. 筛选联调
- 步骤：在列表筛选中选择 `CHILD_LEDGER`、`FALLBACK_SNAPSHOT` 并搜索。
- 预期：请求参数携带 `refundLimitSource`，列表按条件过滤。

## 联调字段清单（窗口A）
1. 列表 `/trade/after-sale/page`
- 请求新增：`refundLimitSource`
- 返回字段：`refundLimitSource/refundLimitSourceLabel/refundLimitRuleHint/refundLimitDetailJson`

2. 详情 `/trade/after-sale/get-detail`
- 返回字段：`refundLimitSource/refundLimitSourceLabel/refundLimitRuleHint/refundLimitDetailJson`

3. 退款 `/trade/after-sale/refund`
- 错误码联调：`1011000125`
