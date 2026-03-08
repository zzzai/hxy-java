# Window B Handoff - MiniApp Business PRD Pack（2026-03-09）

## 1. 交付范围
- 分支：`feat/ui-four-account-reconcile-ops`
- 交付内容：礼品卡 / 邀请有礼 / 技师动态治理 3 份核心业务 PRD + 本 handoff。
- 约束执行：仅文档改动；未改 overlay 页面、未改业务代码、未触碰 `.codex` 与历史 handoff。

## 2. 新增文档
1. `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md`
2. `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md`
3. `docs/products/miniapp/2026-03-09-miniapp-technician-feed-product-policy-v1.md`

## 3. 对齐基线
- `docs/contracts/2026-03-09-miniapp-gift-card-domain-contract-v1.md`
- `docs/contracts/2026-03-09-miniapp-referral-domain-contract-v1.md`
- `docs/contracts/2026-03-09-miniapp-technician-feed-contract-v1.md`
- `docs/contracts/2026-03-09-miniapp-addbook-conflict-spec-v1.md`
- `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
- `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`

## 4. 关键收口
- 三份 PRD 均包含：
  - 用户流程（主流程 + 异常流程）
  - 业务规则与状态流转（统一状态机引用）
  - 错误码与降级语义（契约一致）
  - 客服/申诉/人工兜底规则
  - 验收清单（happy path + 业务错误 + degraded path）
- 错误码语义未新增冲突口径，延续锚点：
  - `1004001000`, `1011000011`, `1013004000`, `1013007008`, `1030001000`, `1030001001`, `1030004000`, `1030004012`, `1030004016`, `TICKET_SYNC_DEGRADED`。
- 降级语义统一：fail-open 仅用于协同链路，不回滚主业务成功态。

## 5. 给窗口A的联动建议
1. 先按礼品卡与邀请有礼 PRD 建立客服工单模板字段，减少灰度期人工沟通成本。
2. 技师动态优先上线“审核降级可观测”，再灰度互动权重策略。
3. 联调验收优先核对错误码和降级标记，再核对文案与 UI 反馈。
