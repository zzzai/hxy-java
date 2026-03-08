# Window A Handoff - MiniApp Business Freeze Integration (2026-03-09)

## 1. 目标与范围
- 目标：完成发布前“业务规则冻结与文档封版”收口，并完成跨窗口第二波文档集成。
- 范围：03-09 miniapp 文档包状态从 `Ready` 收口为 `Frozen`，补齐业务规则、SOP、经营指标与商业模型。

## 2. 本次新增与更新
1. 新增：`docs/products/miniapp/2026-03-09-miniapp-business-rulebook-v1.md`
2. 新增：`docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md`
3. 更新：`docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`
4. 更新：`hxy/07_memory_archive/handoffs/2026-03-09/miniapp-business-freeze-integration-window-a.md`

## 3. 第二波跨窗口集成记录
- 窗口B提交：`40e60fe0dc`（gift/referral/feed 业务PRD）
- 窗口C提交：`1d7bbb1a9a`（客服SOP + 运营配置playbook）
- 窗口D提交：`b9fcc16f54`（增长KPI + 单位经济模型）
- 集成结论：全部已进入当前分支并纳入 03-09 冻结包。

## 4. 收口结果
- 03-09 批次文档由 14 份扩展为 21 份。
- 21/21 文档状态统一为 `Frozen`。
- 业务规则字典覆盖发布前必需能力：
  - 可下单
  - 可预约
  - 可退款
  - 可领券
  - 可兑换
  - 可核销
  - 可改约

## 5. 冻结边界
- 错误码锚点保持稳定：`1030004012`、`1030004016`、`1011000125`、`1011000011`。
- 降级语义保持一致：
  - 依赖链路异常：fail-open + 可检索 warning
  - 业务冲突：fail-close + 明确错误码
- 变更必须走审批模板，不允许口头变更。

## 6. 对窗口B/C/D联调注意点
1. Window B（产品/UI）
   - 严禁“文案驱动状态”，按状态机和错误码渲染。
   - 后端未确认成功前，禁止成功文案/动效。
2. Window C（契约/SOP）
   - 冲突场景必须同键同参幂等命中、同键异参冲突报错。
   - 关键审计字段不允许缺失：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
3. Window D（数据/经营）
   - 指标与看板按字符串错误码聚合，避免类型漂移。
   - `degraded=true` 流量独立分池，不计入支付成功率、毛利与 ROI 主口径。

## 7. 下一步建议
- 进入发布前联合抽样验证（规则 -> 契约 -> 页面 -> 埋点 -> SOP）。
- 若发现语义冲突，先回退状态为 `Ready` 再修订，不直接改 `Frozen` 基线。
