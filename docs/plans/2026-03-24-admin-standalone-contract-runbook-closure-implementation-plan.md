# Admin Standalone Contract/Runbook Closure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为后台主能力补齐独立 contract/runbook，并回灌总台账与交接文档。

**Architecture:** 采用“按业务域成组补齐”的方式，围绕现有正式 PRD 分 7 组新增 contract/runbook；然后统一更新总台账、完成度复盘文档和 handoff。整个过程不改业务代码，只做真值文档收口。

**Tech Stack:** Markdown、rg、git diff、命名门禁、记忆门禁。

---

### Task 1: Finance Ops Core 文档补齐

**Files:**
- Create: `docs/contracts/2026-03-24-admin-finance-ops-core-contract-v1.md`
- Create: `docs/plans/2026-03-24-admin-finance-ops-core-runbook-v1.md`
- Reference: `docs/products/miniapp/2026-03-12-miniapp-finance-ops-four-account-reconcile-prd-v1.md`
- Reference: `docs/products/miniapp/2026-03-12-miniapp-finance-ops-refund-notify-replay-prd-v1.md`
- Reference: `docs/products/miniapp/2026-03-12-miniapp-finance-ops-technician-commission-settlement-prd-v1.md`

**Step 1: 盘点真实页面/API/controller**
Run: `rg -n "fourAccountReconcile|refundNotifyLog|commission-settlement|FourAccountReconcileController|BookingRefundNotifyLogController|TechnicianCommissionSettlementController" ruoyi-vue-pro-master -S`
Expected: 命中 `BO-001` ~ `BO-003` 的真实页面/API/controller。

**Step 2: 编写 Finance Ops Core contract**
- 明确 `BO-001` ~ `BO-003` 的页面入口、真实 path、关键字段、空态与禁止误写内容。

**Step 3: 编写 Finance Ops Core runbook**
- 明确对账、重放、结算审批与通知补偿的操作步骤、审计键、失败处理、升级路径。

**Step 4: 自检文案一致性**
Run: `rg -n "BO-001|BO-002|BO-003|未核出独立" docs/contracts/2026-03-24-admin-finance-ops-core-contract-v1.md docs/plans/2026-03-24-admin-finance-ops-core-runbook-v1.md -S`
Expected: 三项能力都被覆盖，且不再使用“未核出独立”描述。

### Task 2: Product / Store / Store Product 文档补齐

**Files:**
- Create: `docs/contracts/2026-03-24-admin-product-spu-template-contract-v1.md`
- Create: `docs/plans/2026-03-24-admin-product-spu-template-runbook-v1.md`
- Create: `docs/contracts/2026-03-24-admin-store-master-contract-v1.md`
- Create: `docs/plans/2026-03-24-admin-store-master-runbook-v1.md`
- Create: `docs/contracts/2026-03-24-admin-store-product-sku-contract-v1.md`
- Create: `docs/plans/2026-03-24-admin-store-product-sku-runbook-v1.md`

**Step 1: 盘点真实页面/API/controller**
Run: `rg -n "mall/product/spu|mall/product/template|mall/store/index|mall/store/category|mall/store/tag|mall/store/tag-group|mall/product/store/spu|mall/product/store/sku|ProductSpuController|ProductTemplateGenerateController|ProductStoreController|ProductStoreCategoryController|ProductStoreTagController|ProductStoreTagGroupController|ProductStoreSpuController|ProductStoreSkuController" ruoyi-vue-pro-master -S`
Expected: 命中 `ADM-001` ~ `ADM-008`。

**Step 2: 编写三组 contract**
- Product SPU/Template 对齐 `ADM-001` ~ `ADM-002`
- Store Master 对齐 `ADM-003` ~ `ADM-006`
- Store Product / SKU Ops 对齐 `ADM-007` ~ `ADM-008`

**Step 3: 编写三组 runbook**
- 每组明确页面入口、批量操作、审计键、失败判断与人工接管。

**Step 4: 自检文案一致性**
Run: `rg -n "ADM-001|ADM-008|未核出独立" docs/contracts/2026-03-24-admin-product-spu-template-contract-v1.md docs/contracts/2026-03-24-admin-store-master-contract-v1.md docs/contracts/2026-03-24-admin-store-product-sku-contract-v1.md docs/plans/2026-03-24-admin-product-spu-template-runbook-v1.md docs/plans/2026-03-24-admin-store-master-runbook-v1.md docs/plans/2026-03-24-admin-store-product-sku-runbook-v1.md -S`
Expected: `ADM-001` ~ `ADM-008` 被覆盖。

### Task 3: Supply Chain / Store Lifecycle / Trade Ops 文档补齐

**Files:**
- Create: `docs/contracts/2026-03-24-admin-supply-chain-stock-approval-contract-v1.md`
- Create: `docs/plans/2026-03-24-admin-supply-chain-stock-approval-runbook-v1.md`
- Create: `docs/contracts/2026-03-24-admin-store-lifecycle-governance-contract-v1.md`
- Create: `docs/plans/2026-03-24-admin-store-lifecycle-governance-runbook-v1.md`
- Create: `docs/contracts/2026-03-24-admin-trade-ops-after-sale-review-ticket-contract-v1.md`
- Create: `docs/plans/2026-03-24-admin-trade-ops-after-sale-review-ticket-runbook-v1.md`

**Step 1: 盘点真实页面/API/controller**
Run: `rg -n "stockAdjustOrder|transferOrder|lifecycleBatchLog|lifecycleChangeOrder|lifecycleRecheckLog|afterSale/index|reviewTicket|reviewTicketRoute|ProductStoreLifecycleBatchLogController|AfterSaleController|AfterSaleReviewTicketController|AfterSaleReviewTicketRouteController|storeSkuStockAdjustOrder|storeSkuTransferOrder" ruoyi-vue-pro-master -S`
Expected: 命中 `ADM-009` ~ `ADM-016`。

**Step 2: 编写三组 contract**
- Supply Chain 对齐 `ADM-009` ~ `ADM-010`
- Store Lifecycle 对齐 `ADM-011` ~ `ADM-013`
- Trade Ops 对齐 `ADM-014` ~ `ADM-016`

**Step 3: 编写三组 runbook**
- 明确审批、复核、SLA、批量动作、审计键、失败与回滚边界。

**Step 4: 自检文案一致性**
Run: `rg -n "ADM-009|ADM-016|未核出独立" docs/contracts/2026-03-24-admin-supply-chain-stock-approval-contract-v1.md docs/contracts/2026-03-24-admin-store-lifecycle-governance-contract-v1.md docs/contracts/2026-03-24-admin-trade-ops-after-sale-review-ticket-contract-v1.md docs/plans/2026-03-24-admin-supply-chain-stock-approval-runbook-v1.md docs/plans/2026-03-24-admin-store-lifecycle-governance-runbook-v1.md docs/plans/2026-03-24-admin-trade-ops-after-sale-review-ticket-runbook-v1.md -S`
Expected: `ADM-009` ~ `ADM-016` 被覆盖。

### Task 4: 回灌总台账与完成度复盘

**Files:**
- Modify: `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
- Modify: `docs/products/2026-03-16-hxy-full-project-function-prd-completion-review-v1.md`
- Modify: `docs/products/2026-03-16-hxy-full-project-function-doc-completion-publishable-list-v1.md`
- Create: `hxy/07_memory_archive/handoffs/2026-03-24/admin-standalone-contract-runbook-closure-window-a.md`

**Step 1: 更新总台账**
- 把后台各组从“未核出独立 contract/runbook”更新为对应新文档路径。

**Step 2: 更新完成度复盘文档**
- 把“后台多数能力缺独立 contract/runbook”改成“后台独立 contract/runbook 已成体系，剩余问题转为 release evidence 或工程 blocker”。

**Step 3: 写 handoff**
- 记录本轮新增文档、单一真值结论、剩余 blocker 与后续顺序。

### Task 5: 门禁与提交

**Files:**
- Verify only

**Step 1: 运行格式与门禁**
Run: `git diff --check`
Expected: PASS

Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
Expected: PASS

Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
Expected: PASS

**Step 2: 提交**
```bash
git add docs/contracts docs/plans docs/products hxy/07_memory_archive/handoffs
git commit -m "docs(admin): fill standalone contract and runbook gaps"
```
