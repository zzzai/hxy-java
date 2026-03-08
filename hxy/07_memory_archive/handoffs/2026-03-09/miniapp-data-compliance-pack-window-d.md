# MiniApp Data & Compliance Pack - Window D Handoff (2026-03-09)

## 1. 变更摘要
- 新增埋点治理文档：`docs/plans/2026-03-09-miniapp-event-taxonomy-v2.md`
  - 提供统一事件命名、事件字典、字段字典、归因规则、风险分级与拦截规则、验收与监控指标。
- 新增履约物流 PRD：`docs/products/miniapp/2026-03-09-miniapp-fulfillment-logistics-prd-v1.md`
  - 明确履约/物流状态口径、SLA 归因、异常与降级可审计规则、看板指标。
- 新增内容合规风格指南：`docs/products/miniapp/2026-03-09-miniapp-content-compliance-styleguide-v1.md`
  - 明确合规/误导营销/假成功动效的分级与拦截动作，形成上线前审计闭环。

## 2. 与既有文档映射结论
- analytics-funnel：从“漏斗概览”扩展到“可执行事件与字段字典”。
- copy-terminology：从“术语规范”扩展到“可拦截的合规规则”。
- motion-accessibility：把“动效可用性”收口为“不得早于后端确认展示成功态”。

## 3. 验证命令与结果
1. `git diff --check`
- 结果：PASS

2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- 结果：PASS

3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- 结果：PASS

## 4. 联调注意点（A/B/C）
- 字段：统一使用 `resultCode/errorCode/degraded/degradeReason`，降级事件必须携带业务主键（`orderId/afterSaleId/payRefundId/fulfillmentId` 至少一个）。
- 错误码：前端展示与运营看板使用字符串化错误码，避免数值/字符串类型漂移。
- 降级行为：禁止“前端先成功后回滚”；若后端未确认成功，只允许展示处理中/降级提示并记录事件。
