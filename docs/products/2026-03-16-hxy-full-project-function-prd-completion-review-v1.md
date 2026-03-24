# HXY 全项目功能清单与 PRD 完整度终审 v1（2026-03-16）

## 1. 目标
- 目标：给出一份可以直接回答“全项目前后端有哪些功能、对应 PRD 完整度如何、后台和 SPU/SKU 为什么不能漏掉”的终审综述。
- 本文不是替代逐功能总账，而是把全项目功能清单、PRD 完整度结论、后台/SPU/SKU 专项和当前剩余 blocker 放到同一页。

## 2. 单一真值来源
- 逐功能根台账：`docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
- 小程序能力与阻断单一真值：
  - `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
  - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
  - `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-final-integration-v1.md`
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md`
- 后台商品 / 门店 / 供应链 / 售后 PRD：
  - `docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md`
  - `docs/products/2026-03-15-hxy-admin-store-master-and-governance-prd-v1.md`
  - `docs/products/2026-03-15-hxy-admin-store-product-mapping-and-sku-ops-prd-v1.md`
  - `docs/products/2026-03-15-hxy-admin-store-stock-adjust-and-transfer-prd-v1.md`
  - `docs/products/2026-03-15-hxy-admin-store-lifecycle-governance-prd-v1.md`
  - `docs/products/2026-03-15-hxy-admin-after-sale-review-ticket-prd-v1.md`

## 3. 总结论

| 统计项 | 当前结论 |
|---|---|
| 全项目业务能力总数 | `51` |
| 小程序用户侧能力 | `31` |
| 管理后台能力 | `20` |
| 后台真实页面文件 | `22` 个 `index.vue` |
| 后台真实独立 API 文件 | `18` 个 |
| PRD `完整` | `51` |
| PRD `较完整` | `0` |
| PRD `部分完整 / 缺失` | `0` |
| PRD 缺口结论 | `0` |
| 当前真正剩余的问题 | 工程真值 blocker 未解除 + release evidence 未补齐 |

补充：
- 这表示“全项目 PRD 体系已经收口”，不表示“全项目工程已经闭环”。
- 后台不是没列，而是现在已经被明确映射到 `20` 个能力条目里。
- `SPU / SKU` 不是一句“商品管理”带过，而是已经拆成 6 个后台能力 + 1 个前台商品承接能力。

## 4. 前后台功能与 PRD 完整度总表

| 功能组 | 侧别 | 能力数 | 主 PRD | PRD 完整度 | 当前工程结论 |
|---|---|---:|---|---|---|
| Trade & Pay | 小程序前台 | 5 | `docs/products/miniapp/2026-03-12-miniapp-trade-checkout-order-prd-v1.md`; `docs/products/miniapp/2026-03-12-miniapp-pay-submit-result-prd-v1.md`; `docs/products/miniapp/2026-03-12-miniapp-after-sale-refund-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-fulfillment-logistics-prd-v1.md` | 完整 | Frozen 主链，可维护 |
| Home / Coupon / Point | 小程序前台 | 1 | `docs/products/miniapp/2026-03-09-miniapp-home-growth-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md` | 完整 | 已上线主链，可维护 |
| Member | 小程序前台 | 8 | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md` | 完整 | 文档闭环；等级/资产总览/标签仍有缺页 blocker |
| Product / Search / Catalog | 小程序前台 | 3 | `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md` | 完整 | 主链可用；评论/收藏/足迹、canonical search 仍受边界约束 |
| Marketing Expansion | 小程序前台 | 2 | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md` | 完整 | 文档闭环；整域仍按 `PLANNED_RESERVED` 管理 |
| Booking | 小程序前台 | 5 | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`; `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`; `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md`; `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md` | 完整 | `Doc Closed / Can Develop / Cannot Release` |
| Content / Customer Service | 小程序前台 | 3 | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md`; `docs/products/miniapp/2026-03-12-miniapp-content-article-list-category-writeback-prd-v1.md` | 完整 | DIY 已活跃；聊天/文章仍需按边界管理 |
| Brokerage | 小程序前台 | 1 | `docs/products/miniapp/2026-03-10-miniapp-brokerage-distribution-prd-v1.md` | 完整 | 文档闭环；资金类能力仍不能误升 |
| Reserved | 小程序前台 | 3 | `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md`; `docs/products/miniapp/2026-03-12-miniapp-technician-feed-prd-v1.md` | 完整 | runtime 已闭环；当前剩余问题转为 gray / rollback / sign-off evidence |
| Finance Ops | 后台 | 4 | `docs/products/miniapp/2026-03-12-miniapp-finance-ops-four-account-reconcile-prd-v1.md`; `docs/products/miniapp/2026-03-12-miniapp-finance-ops-refund-notify-replay-prd-v1.md`; `docs/products/miniapp/2026-03-12-miniapp-finance-ops-technician-commission-settlement-prd-v1.md`; `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-detail-config-prd-v1.md` | 完整 | `BO-001` ~ `BO-003` 已补齐成组独立 contract/runbook；`BO-004` 维持 `admin-only 页面/API 真值已闭环 / Can Develop / Cannot Release` |
| Product SPU / Template | 后台 | 2 | `docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md` | 完整 | 已有真实页面、真实 controller；独立 contract/runbook 已补齐 |
| Store Master | 后台 | 4 | `docs/products/2026-03-15-hxy-admin-store-master-and-governance-prd-v1.md` | 完整 | 已有真实页面、真实 controller；独立 contract/runbook 已补齐 |
| Store Product SPU / SKU | 后台 | 2 | `docs/products/2026-03-15-hxy-admin-store-product-mapping-and-sku-ops-prd-v1.md` | 完整 | 已有真实页面、真实 API、真实 controller |
| Supply Chain Stock | 后台 | 2 | `docs/products/2026-03-15-hxy-admin-store-stock-adjust-and-transfer-prd-v1.md` | 完整 | 审批页真实存在；独立 contract/runbook 已补齐 |
| Store Lifecycle | 后台 | 3 | `docs/products/2026-03-15-hxy-admin-store-lifecycle-governance-prd-v1.md` | 完整 | 守卫与复核页真实存在；独立 contract/runbook 已补齐 |
| Trade Ops After-sale | 后台 | 3 | `docs/products/2026-03-15-hxy-admin-after-sale-review-ticket-prd-v1.md` | 完整 | 页面/API/controller 已核实；独立 contract/runbook 已补齐 |

## 5. 为什么管理后台和 SPU/SKU 不能省略

### 5.1 管理后台不是空白
- 当前真实后台 `mall` 页面文件共 `22` 个：
  - booking 财务运营 `4`
  - product `5`
  - store `9`
  - trade `4`
  - 这些页面被本文映射为 `20` 个业务能力，而不是“零散页面”
- 当前真实后台独立 API 文件共 `18` 个，已和对应页面/Controller 一一核对。

### 5.2 SPU / SKU 已明确拆项

| 能力ID | 能力 | 页面 / 接口真值 | 对应 PRD | 当前结论 |
|---|---|---|---|---|
| `ADM-001` | 总部商品 SPU 管理 / 新增编辑 | `mall/product/spu/index`; `mall/product/spu/form`; `/product/spu/*`; `/product/service-spu/*`; `/product/physical-spu/*` | `docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md` | 完整 |
| `ADM-002` | 商品模板校验 / SKU 自动生成 | `mall/product/template/index`; `/product/template/*` | `docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md` | 完整 |
| `ADM-007` | 门店商品 SPU 映射 / 上下架 | `mall/product/store/spu/index`; `/product/store-spu/*` | `docs/products/2026-03-15-hxy-admin-store-product-mapping-and-sku-ops-prd-v1.md` | 完整 |
| `ADM-008` | 门店 SKU 价库存管理 / 批量调整 / 库存流水重试 | `mall/product/store/sku/index`; `/product/store-sku/*` | `docs/products/2026-03-15-hxy-admin-store-product-mapping-and-sku-ops-prd-v1.md` | 完整 |
| `ADM-009` | 门店库存调整单审批 | `mall/store/stockAdjustOrder/index`; `/product/store-sku/stock-adjust-order/*` | `docs/products/2026-03-15-hxy-admin-store-stock-adjust-and-transfer-prd-v1.md` | 完整 |
| `ADM-010` | 跨店调拨单审批 | `mall/store/transferOrder/index`; `/product/store-sku/transfer-order/*` | `docs/products/2026-03-15-hxy-admin-store-stock-adjust-and-transfer-prd-v1.md` | 完整 |
| `BF-015` | 前台商品分类 / 搜索 lite / 商品详情承接 | `/pages/index/category`; `/pages/index/search`; `/pages/goods/list`; `/pages/goods/index`; `GET /product/spu/page`; `GET /product/spu/get-detail` | `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md` | 完整 |

结论：
- 不能再说“管理后台没有列出”。
- 也不能再把 SPU/SKU 简化成一个模糊的“商品后台”条目。
- 当前项目的商品主链已经明确分成“总部 SPU / 模板联调 / 门店 SPU / 门店 SKU / 库存调整 / 跨店调拨 / 前台商品承接”七个层次。

## 6. 剩余不是 PRD 问题，而是工程真值问题

| blocker | 当前状态 | 对开发的影响 | 对放量的影响 |
|---|---|---|---|
| Booking | 文档完整，工程未闭环 | 可继续开发真值修复 | 不可放量 |
| `BO-004` | 文档完整，admin-only 页面/API 真值已闭环 | 可继续做 release evidence / 菜单执行 / 写后回读样本核查 | 不可放量 |
| Member 缺页能力 | 文档完整，缺真实页面 | 可继续补实现 | 不可放量 |
| Reserved release evidence | 文档完整，runtime 已闭环但发布证据未补齐 | 可继续受控开发 | 不可放量 |
| 后台独立 contract/runbook 已成体系 | PRD 已完整 | 不阻断存量开发 | 不再是文档 blocker；后续重点转向 release evidence / 工程真值 |

## 7. 最终使用方式
1. 要看“逐功能全量清单”，直接看：
   - `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
2. 要看“全项目 PRD 完整度有没有收口、后台/SPU/SKU 有没有被纳入”，直接看本文。
3. 要看“适合直接汇报 / 外发的前后台功能 + PRD/contract/runbook 完整度清单”，直接看：
   - `docs/products/2026-03-16-hxy-full-project-function-doc-completion-publishable-list-v1.md`
4. 要看“小程序工程 blocker 与 release 结论”，继续以 miniapp 三份单一真值为准：
   - `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-final-integration-v1.md`
   - `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
   - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md`
