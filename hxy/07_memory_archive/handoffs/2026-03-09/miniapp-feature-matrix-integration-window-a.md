# Window A Handoff - MiniApp Feature Matrix Integration (2026-03-09)

## 1. Scope
- 分支：`feat/ui-four-account-reconcile-ops`
- 目标：补齐小程序“功能全景与发布矩阵”主文档，并更新跨窗口文档索引状态。
- 约束执行：未改 overlay 页面、未改业务代码。

## 2. Delivered Files
1. `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
2. `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`

## 3. Integration Decisions
1. 统一对齐四份基线文档：
   - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
   - `docs/products/miniapp/2026-03-08-miniapp-ia-routing-map-v1.md`
   - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
   - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
2. 发布批次采用 `RB1-P0/RB2-P1/RB3-P2` 三段式，便于窗口并行推进与冻结。
3. 2026-03-09 批次 12 份文档已建立统一索引和状态管理（Draft/Ready/Frozen）。

## 4. Current Status Snapshot
- 2026-03-09 批次：
  - Ready: 1（主矩阵）
  - Draft: 11（待 B/C/D 分窗口补齐）
  - Frozen: 0（本批新增尚未冻结）

## 5. B/C/D 联调关注点
1. Window B（PRD/UI语义）
   - 所有交互动作必须绑定真实后端成功事件，禁止前端假成功动效。
   - 页面文案和术语必须与错误码恢复矩阵一致，禁止“文案驱动状态”。
2. Window C（契约/错误码）
   - `TBD_*` 错误码在进入对应发布批次前必须冻结并沉淀到契约文档。
   - 冲突处理场景必须给出 fail-open 与可恢复动作，保持接口向后兼容。
3. Window D（数据/合规）
   - 埋点v2需覆盖矩阵中的 P0/P1 核心动作，并携带检索键：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
   - 合规文档需明确“营销文案不得暗示成功先于后端确认”的审计规则。

## 6. Next Merge Rule for A
- A 窗口合并 B/C/D 文档时，按顺序执行：C（契约）-> B（PRD）-> D（数据合规）-> A（二次冻结矩阵状态）。
- 二次冻结触发条件：12 份文档均为 Ready 且关键契约文档通过门禁锚点检查。
