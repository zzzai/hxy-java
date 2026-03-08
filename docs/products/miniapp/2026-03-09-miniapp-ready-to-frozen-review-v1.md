# MiniApp Ready-to-Frozen Review v1 (2026-03-09)

## 1. 评审目标
- 目标：将 2026-03-09 批次文档由 `Ready` 收口到 `Frozen`，形成发布前审计闭环。
- 适用范围：`docs/products/miniapp`、`docs/contracts`、`docs/plans` 中 2026-03-09 miniapp 相关文档。

## 2. 冻结范围

### 2.1 本次冻结文档（21份）
1. `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
2. `docs/products/miniapp/2026-03-09-miniapp-home-growth-prd-v1.md`
3. `docs/contracts/2026-03-09-miniapp-addbook-conflict-spec-v1.md`
4. `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
5. `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md`
6. `docs/contracts/2026-03-09-miniapp-gift-card-domain-contract-v1.md`
7. `docs/contracts/2026-03-09-miniapp-referral-domain-contract-v1.md`
8. `docs/contracts/2026-03-09-miniapp-technician-feed-contract-v1.md`
9. `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md`
10. `docs/plans/2026-03-09-miniapp-event-taxonomy-v2.md`
11. `docs/products/miniapp/2026-03-09-miniapp-fulfillment-logistics-prd-v1.md`
12. `docs/products/miniapp/2026-03-09-miniapp-content-compliance-styleguide-v1.md`
13. `docs/products/miniapp/2026-03-09-miniapp-business-rulebook-v1.md`
14. `docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md`
15. `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md`
16. `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md`
17. `docs/products/miniapp/2026-03-09-miniapp-technician-feed-product-policy-v1.md`
18. `docs/products/miniapp/2026-03-09-miniapp-cs-sop-and-escalation-v1.md`
19. `docs/products/miniapp/2026-03-09-miniapp-operation-config-playbook-v1.md`
20. `docs/products/miniapp/2026-03-09-miniapp-growth-kpi-and-experiment-plan-v1.md`
21. `docs/products/miniapp/2026-03-09-miniapp-commercial-model-and-unit-economics-v1.md`

### 2.2 冻结后约束
- 仅允许向后兼容补充。
- 禁止删除/重命名既有字段与错误码语义。
- 任何语义变更必须通过审批模板并附回滚策略。

## 3. 版本边界
- 基线版本：2026-03-08 Frozen 包。
- 增量版本：2026-03-09 Frozen 包（本次）。
- 客户端适配边界：
  - 03-08 与 03-09 文档并存，按“旧端可忽略新增字段”原则兼容。
  - 错误码锚点保持稳定：`1030004012`、`1030004016`、`1011000125`、`1011000011`。

## 4. 冻结评审结果

| 检查项 | 结果 | 说明 |
|---|---|---|
| 文档齐套性 | PASS | 21/21 文档存在且可追踪 |
| 契约一致性 | PASS | 路由/API/错误码语义与既有基线一致 |
| 状态机一致性 | PASS | 统一引用 03-08 状态机文档 |
| 降级语义一致性 | PASS | fail-open/fail-close 边界明确 |
| 审计可检索性 | PASS | 关键字段约束统一（runId/orderId/payRefundId/sourceBizNo/errorCode） |
| SOP/运营口径一致性 | PASS | 客服SOP、运营配置与契约错误码矩阵对齐 |

## 5. 回滚策略
- 文档回滚触发条件：
  1. 关键错误码语义冲突导致端侧误判；
  2. 状态机口径冲突导致流程不可恢复；
  3. 关键降级语义与实际系统行为不一致。
- 回滚动作：
  1. 索引状态从 `Frozen` 回退为 `Ready`；
  2. 发布“冻结撤销说明”并标记受影响文档；
  3. 以 03-08 Frozen 包作为临时执行基线；
  4. 48小时内完成修订版并重新走冻结评审。

## 6. 变更审批记录模板

| 字段 | 内容模板 |
|---|---|
| 变更单号 | CHG-YYYYMMDD-XXX |
| 变更文档 | 文档路径 + 版本 |
| 变更类型 | 字段补充 / 语义修正 / 错误码补充 / 降级策略调整 |
| 影响范围 | 页面/接口/埋点/SOP |
| 兼容性评估 | 向后兼容 / 需灰度 / 需回滚预案 |
| 审批人 | Product Owner / Data Owner / Domain Owner |
| 生效时间 | YYYY-MM-DD HH:mm |
| 回滚预案 | 回滚步骤 + 验证口径 |

## 7. 发布建议
1. 当前 03-09 文档包可进入发布前联调与灰度。
2. 先做跨文档抽样验证（规则 -> 接口 -> 错误码 -> 埋点 -> SOP）。
3. 冻结后任何变更都必须附审批记录，不接受口头变更。

## 8. 冻结后变更门禁

### 8.1 可直接补充（保持 Frozen，不回退 Ready）
1. 纯描述补充：示例、说明文字、非语义性注释，不改变字段/状态/错误码语义。
2. 向后兼容补充：新增可选字段（默认值明确，旧端可忽略）。
3. 验收证据补全：补截图位、补日志检索样例、补操作步骤说明。
4. Owner 与值班信息更新：不改变流程语义，仅组织信息调整。

### 8.2 必须回退 Ready（先回退再修订）
1. 任何既有字段的删除、重命名、必填性变化。
2. 任何错误码语义变化或同码多义调整。
3. 状态机流转变更（新增/删除状态，转移条件变化）。
4. 降级语义变化（fail-open 改 fail-close，或反向变化）。
5. 优先级与发布批次变更（P级、RB批次变化）。
6. API 名称/路由从通配改为具体接口并影响联调脚本与验收用例。

### 8.3 门禁执行流程
1. 发起变更单并标记影响文档。
2. A窗口判定是否触发“回退Ready”条件。
3. 若触发，索引状态从 `Frozen` 回退为 `Ready`，并冻结上线窗口。
4. 完成修订后重新走一致性审计 + Frozen 评审。
