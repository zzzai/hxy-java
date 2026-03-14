# HXY Admin Product / Store / Trade PRD Gap Closure Plan v1（2026-03-15）

## 1. 目标与范围
- 目标：把当前后台 `product / store / trade` 域“有真实页面 / API / controller，但没有进入正式 PRD 体系”的能力，收口成一份可直接派工的文档补齐计划。
- 范围：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/**`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/**`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/**`
- 不重复覆盖：
  - 已完成正式 PRD 归档的 `Finance Ops Admin` 四项能力（`BO-001` ~ `BO-004`）
  - 已完成 formal doc pack 的 miniapp 用户侧主链

## 2. 当前审查结论
1. 当前后台 `product / store / trade` 域最核心的问题不是“代码缺失”，而是“产品单一真值没有归到正式 PRD 体系”。
2. 当前页面与 controller 已真实存在的能力共 `16` 项：
   - `Product`：2 项
   - `Store`：11 项
   - `Trade Ops`：3 项
3. 当前文档状态：
   - `较完整`：门店主数据 / 分类 / 标签 / 标签组 / 门店商品 SPU / 门店 SKU
   - `部分完整`：SPU 总部商品、模板生成、库存调整单、跨店调拨、生命周期治理、售后运营、复核工单、SLA 路由
4. 本计划的目标不是继续堆计划文档，而是产出“能作为产品输入直接使用的正式 PRD”。

## 3. 缺口分级规则
- `P0`：当前有真实页面、真实 API、真实 controller，且缺正式 PRD 已开始影响跨窗口协作、范围判断或后续开发冻结。
- `P1`：已有较完整产品设计，但没有按正式 PRD 模版统一归档；短期不阻断维护，但阻断全项目统一真值。
- `P2`：属于补充型文档，不是本轮必须先补。

## 4. P0 PRD Backlog（必须优先补齐）

| 优先级 | 目标文档 | 覆盖能力 | Owner 窗口 | 主要代码真值 | 主要上游文档 | 前置依赖 | 当前是否阻断开发 | 说明 |
|---|---|---|---|---|---|---|---|---|
| P0 | `docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md` | `ADM-001` `ADM-002` | B | `mall/product/spu/index.vue`; `mall/product/spu/form/index.vue`; `mall/product/template/index.vue`; `ProductSpuController`; `ProductServiceSpuController`; `ProductPhysicalSpuController`; `ProductTemplateGenerateController` | `hxy/01_product/HXY-瑞幸双商品体系对标-RuoYi落地方案-v1-2026-02-22.md`; `hxy/linshi/荷小悦O2O多门店（服务+实物）SPU_SKU多门店完整结构说明（最新完整版）.md`; `docs/plans/2026-03-01-hxy-sku-template-api-draft.md`; `docs/plans/2026-03-02-p0-template-booking-ci-design.md` | A 先补页面/API/Controller 真值摘要 | 部分 | 这是后台商品主数据的顶层 PRD，必须把“SPU 管理”和“模板/SKU 自动生成”一并标准化 |
| P0 | `docs/products/2026-03-15-hxy-admin-store-product-mapping-and-sku-ops-prd-v1.md` | `ADM-007` `ADM-008` | B | `mall/product/store/spu/index.vue`; `mall/product/store/sku/index.vue`; `storeSpu.ts`; `storeSku.ts`; `ProductStoreSpuController`; `ProductStoreSkuController` | `hxy/linshi/荷小悦O2O多门店（服务+实物）SPU_SKU多门店完整结构说明（最新完整版）.md`; `docs/plans/2026-03-01-store-sku-chain-priority-plan.md`; `docs/plans/2026-03-01-hxy-next-delivery-plan.md` | 依赖总部商品 / 模板 PRD 的术语和边界 | 部分 | 这是 `SPU/SKU` 后台运营最核心的门店侧能力 |
| P0 | `docs/products/2026-03-15-hxy-admin-store-stock-adjust-and-transfer-prd-v1.md` | `ADM-009` `ADM-010` | B | `mall/store/stockAdjustOrder/index.vue`; `mall/store/transferOrder/index.vue`; `storeSkuStockAdjustOrder.ts`; `storeSkuTransferOrder.ts`; `ProductStoreSkuController` | `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`; `hxy/07_memory_archive/handoffs/2026-03-06/store-sku-stock-adjust-order-window-a.md`; `hxy/07_memory_archive/handoffs/2026-03-06/window-a-p1-stock-transfer-four-account-summary.md` | 依赖门店 SKU 运营 PRD | 部分 | 库存调整/调拨已是独立审批页面，不能长期只靠 ADR 和 handoff |
| P0 | `docs/products/2026-03-15-hxy-admin-store-lifecycle-governance-prd-v1.md` | `ADM-011` `ADM-012` `ADM-013` | B | `mall/store/lifecycleBatchLog/index.vue`; `mall/store/lifecycleChangeOrder/index.vue`; `mall/store/lifecycleRecheckLog/index.vue`; `lifecycleBatchLog.ts`; `lifecycleChangeOrder.ts`; `lifecycleRecheckLog.ts`; `ProductStoreLifecycleBatchLogController`; `ProductStoreLifecycleRecheckLogController`; `ProductStoreController` | `docs/plans/2026-03-03-ticket-sla-store-lifecycle-guard-design.md`; `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`; `hxy/01_product/HXY-门店管理产品设计-万店版-v1-2026-02-28.md` | 依赖门店主数据 PRD统一术语 | 部分 | 生命周期治理已经有页面与规则，但还缺统一产品口径 |
| P0 | `docs/products/2026-03-15-hxy-admin-after-sale-review-ticket-prd-v1.md` | `ADM-014` `ADM-015` `ADM-016` | B | `mall/trade/afterSale/index.vue`; `mall/trade/afterSale/detail/index.vue`; `mall/trade/reviewTicket/index.vue`; `mall/trade/reviewTicketRoute/index.vue`; `AfterSaleController`; `AfterSaleReviewTicketController`; `AfterSaleReviewTicketRouteController` | `hxy/03_payment/HXY-退款分层策略与工单升级规则-v1-2026-02-22.md`; `docs/plans/2026-03-03-ticket-sla-store-lifecycle-guard-design.md` | 依赖退款规则与工单 SLA 设计现状 | 部分 | 售后后台管理已实装，但当前只有规则文档和设计稿，缺正式产品 PRD |

## 5. P1 PRD / 产品归档 Backlog（建议本轮一并补）

| 优先级 | 目标文档 | 覆盖能力 | Owner 窗口 | 主要代码真值 | 主要上游文档 | 当前是否阻断开发 | 说明 |
|---|---|---|---|---|---|---|---|
| P1 | `docs/products/2026-03-15-hxy-admin-store-master-and-governance-prd-v1.md` | `ADM-003` `ADM-004` `ADM-005` `ADM-006` | B | `mall/store/index.vue`; `mall/store/category/index.vue`; `mall/store/tag/index.vue`; `mall/store/tag-group/index.vue`; `ProductStoreController`; `ProductStoreCategoryController`; `ProductStoreTagController`; `ProductStoreTagGroupController` | `hxy/01_product/HXY-门店管理产品设计-万店版-v1-2026-02-28.md`; `hxy/01_product/HXY-总部门店权责利模型-v1-2026-02-22.md` | 否 | 这些能力已有较完整产品设计，但尚未转成当前正式 PRD 体例 |
| P1 | `docs/products/2026-03-15-hxy-admin-product-store-trade-page-api-field-dictionary-v1.md` | P0/P1 全覆盖 | A/B | 以上后台页面与 API 文件全集 | 以上所有正式 PRD | 部分 | 作为后台统一字段字典，支撑后续 contract / QA / runbook 口径 |

## 6. 非 PRD 跟进件（PRD 落盘后再补）

| 优先级 | 目标文档 | 类型 | Owner 窗口 | 依赖 PRD | 说明 |
|---|---|---|---|---|---|
| P1 | `docs/contracts/2026-03-15-hxy-admin-product-store-api-canonical-v1.md` | Contract | C | `hxy-admin-product-spu-and-template-prd`; `hxy-admin-store-product-mapping-and-sku-ops-prd`; `hxy-admin-store-stock-adjust-and-transfer-prd`; `hxy-admin-store-lifecycle-governance-prd` | 后台 product/store 域统一 canonical API 清单 |
| P1 | `docs/contracts/2026-03-15-hxy-admin-after-sale-ticket-contract-v1.md` | Contract | C | `hxy-admin-after-sale-review-ticket-prd` | 售后单 / 复核工单 / 路由规则后台契约统一 |
| P1 | `docs/plans/2026-03-15-hxy-admin-store-supplychain-runbook-v1.md` | Runbook | D | `hxy-admin-store-stock-adjust-and-transfer-prd`; `hxy-admin-store-lifecycle-governance-prd` | 补库存调整、调拨、生命周期的验收和应急操作 |
| P1 | `docs/plans/2026-03-15-hxy-admin-after-sale-ticket-runbook-v1.md` | Runbook | D | `hxy-admin-after-sale-review-ticket-prd` | 补售后后台运营、SLA 路由、人工兜底口径 |

## 7. 执行顺序
1. 先出 `hxy-admin-product-spu-and-template-prd`。
   - 原因：它定义总部商品主数据和模板边界，是后面 `store-spu / store-sku` 文档的上游词典。
2. 再出 `hxy-admin-store-product-mapping-and-sku-ops-prd`。
   - 原因：它决定门店商品映射、价库存、库存流水、批量调整的核心业务边界。
3. 然后并行补：
   - `hxy-admin-store-stock-adjust-and-transfer-prd`
   - `hxy-admin-store-lifecycle-governance-prd`
   - `hxy-admin-after-sale-review-ticket-prd`
4. 最后再由 A/C/D 吸收进字段字典、contract、runbook。

## 8. 固定原则
1. 不允许再把历史 `hxy/linshi`、ADR、handoff 直接当成正式 PRD 的替代品。
2. 正式 PRD 必须写清：页面边界、角色边界、关键状态机、字段最小集、失败与降级口径、禁止性边界。
3. `Product / Store / Trade Ops` 的后台文档补齐顺序，必须按“先产品主边界、再 contract、再 runbook”执行。
4. `BO-004` 已有独立 PRD，本计划不重复覆盖，但后续后台字段字典和 contract 可以继续吸收其真值结论。

## 9. 最终结论
1. 当前项目已经具备“全项目功能台账”与“后台 PRD 缺口清单”两层单一真值。
2. 后台真正缺的不是工程功能，而是正式 PRD 归档。
3. 如果要把项目推进到“全项目文档先行、再开发”的一致状态，下一步必须按本计划优先补齐 `product / store / trade` 后台 PRD。
