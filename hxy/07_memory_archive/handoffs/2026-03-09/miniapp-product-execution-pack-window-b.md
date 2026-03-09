# Window B Handoff - MiniApp Product Execution Pack（2026-03-09）

## 1. 本批交付
- 分支：`feat/ui-four-account-reconcile-ops`
- 新增文档：
  1. `docs/products/miniapp/2026-03-09-miniapp-feature-priority-alignment-v1.md`
  2. `docs/products/miniapp/2026-03-09-miniapp-page-api-field-dictionary-v1.md`
  3. `docs/products/miniapp/2026-03-09-miniapp-user-facing-errorcopy-and-recovery-v1.md`

## 2. 关键收口
- 完成 gift-card/referral/technician-feed 最终优先级与 RB 批次单点收口：统一 `P2 / RB3-P2`。
- 输出页面到 API 字段字典，覆盖：首页、项目页、预约、售后、资产、搜索、礼品卡、邀请、技师 feed。
- 输出用户可见错误文案标准：错误码驱动 + 恢复动作必填 + 降级不伪成功。

## 3. 对齐基线
- `feature-matrix`：发布优先级与批次。
- `contract + canonical register`：字段、错误码、生效条件与降级语义。
- 既有 PRD：承接业务流程与页面触点。

## 4. 约束执行声明
- 未修改 overlay 页面。
- 未修改业务代码。
- 未触碰 `.codex` 与历史 handoff。
- 未处理无关 untracked 文件。

## 5. 联调提示（A/C/D）
- A：前端渲染仅按 `errorCode/degraded/degradeReason` 分支；不按 message。
- C：保持 `RESERVED_DISABLED` 生效门禁，不提前放开 gift/referral/feed 错误码。
- D：验收证据需覆盖字段完整性 + 错误码动作 + 降级文案真实性（无伪成功）。
