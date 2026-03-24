# HXY 全项目功能文档完整度可发布清单 v1（2026-03-16）

## 1. 目的
- 本文用于对外统一回答三件事：
  - 全项目有哪些前后台功能
  - 每个功能对应的 `PRD / contract / runbook` 完整度如何
  - 哪些功能虽然文档完整，但工程上仍不能直接放量
- 本文是发布/汇报版清单；详细页面、API、controller 真值继续以根台账为准：
  - `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
  - `docs/products/2026-03-16-hxy-full-project-function-prd-completion-review-v1.md`

## 2. 总体结论

| 统计项 | 结论 |
|---|---|
| 全项目功能总数 | `51` |
| 小程序前台 | `31` |
| 管理后台 | `20` |
| PRD 完整 | `51 / 51` |
| PRD 缺口 | `0` |
| 当前主要剩余问题 | 工程真值 blocker 未解除 + release evidence 未补齐 |

补充：
- `管理后台` 已纳入正式总表，不存在“后台没有列出”的问题。
- `SPU / SKU` 已拆为独立后台能力，不再允许用模糊的“商品后台”替代。
- `BO-004` 当前只能写成 `admin-only 页面/API 真值已闭环 / Can Develop / Cannot Release`。
- `Booking` 写链、`Member` 发布证据、`Reserved` 发布证据未补齐，仍然只可开发/治理，不可直接放量。
- 当前项目级统一裁决看：`docs/products/miniapp/2026-03-24-miniapp-project-release-go-no-go-package-v1.md`。

## 3. 小程序前台功能清单

| ID | 业务域 | 功能 | 主 PRD | PRD | Contract | Runbook | 当前工程结论 | 放量结论 |
|---|---|---|---|---|---|---|---|---|
| `BF-001` | Trade | 购物车 / 结算 / 下单创建 | `docs/products/miniapp/2026-03-12-miniapp-trade-checkout-order-prd-v1.md` | 完整 | 域级已核实 | 未核出独立 | ACTIVE | 可维护 |
| `BF-002` | Pay | 支付提交 / 支付结果 / 轮询 | `docs/products/miniapp/2026-03-12-miniapp-pay-submit-result-prd-v1.md` | 完整 | 域级已核实 | 未核出独立 | ACTIVE | 可维护 |
| `BF-003` | Trade | 订单列表 / 详情 / 收货 / 取消 / 删除 | `docs/products/miniapp/2026-03-12-miniapp-trade-checkout-order-prd-v1.md` | 完整 | 域级已核实 | 未核出独立 | ACTIVE | 可维护 |
| `BF-004` | Trade | 售后申请 / 退款进度 / 回寄 / 日志 | `docs/products/miniapp/2026-03-12-miniapp-after-sale-refund-prd-v1.md` | 完整 | 域级已核实 | 未核出独立 | ACTIVE | 可维护 |
| `BF-005` | Fulfillment | 物流轨迹 / 履约口径 | `docs/products/miniapp/2026-03-09-miniapp-fulfillment-logistics-prd-v1.md` | 完整 | 域级已核实 | 未核出独立 | ACTIVE | 可维护 |
| `BF-006` | Promotion | 首页增长 / 首页装修 / 券入口 / 积分商城入口 | `docs/products/miniapp/2026-03-09-miniapp-home-growth-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md` | 完整 | 部分完整 | 未核出独立 | ACTIVE | 已上线主链 |
| `BF-007` | Member | 登录 / 注册 / 微信手机号登录 / 社交绑定 | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 已核实 | 已核实 | ACTIVE | 可维护 |
| `BF-008` | Member | 个人资料 / 安全设置 / 退出登录 | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 已核实 | 已核实 | ACTIVE | 可维护 |
| `BF-009` | Member | 地址管理 | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 已核实 | 已核实 | ACTIVE | 可维护 |
| `BF-010` | Member | 钱包 / 充值 / 钱包流水 / 积分流水 / 优惠券 / 积分商城 | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md` | 完整 | 已核实 | 已核实 | ACTIVE | 可维护 |
| `BF-011` | Member | 签到 | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 已核实 | 已核实 | ACTIVE | 可维护 |
| `BF-012` | Member | 会员等级 / 成长值 | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 已核实 | 已核实 | `Can Develop / Cannot Release` | 不可放量 |
| `BF-013` | Member | 资产总览 / 统一资产台账 | `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 已核实 | 已核实 | `Can Develop / Cannot Release` | 不可放量 |
| `BF-014` | Member | 用户标签中心 | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 已核实 | 已核实 | `Can Develop / Cannot Release` | 不可放量 |
| `BF-015` | Product | 商品分类 / 搜索 lite / 商品详情 | `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md` | 完整 | 已核实 | 已核实 | ACTIVE | 可维护 |
| `BF-016` | Product | 评论 / 收藏 / 浏览历史 | `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md` | 完整 | 已核实 | 已核实 | PLANNED_RESERVED | 不可放量 |
| `BF-017` | Product | 搜索 canonical | `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md` | 完整 | 已核实 | 已核实 | PLANNED_RESERVED | 不可放量 |
| `BF-018` | Promotion | 秒杀 / 拼团 / 满减送 / 活动聚合 | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md` | 完整 | 已核实 | 未核出独立 | PLANNED_RESERVED | 不可放量 |
| `BF-019` | Promotion | 砍价 | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md` | 完整 | 已核实 | 未核出独立 | PLANNED_RESERVED | 不可放量 |
| `BF-020` | Booking | 预约列表 / 预约详情 | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md` | 完整 | 已核实 | 已核实 | query-only ACTIVE | 不可放量 |
| `BF-021` | Booking | 技师列表 / 技师详情 / 时段查询 | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md` | 完整 | 已核实 | 已核实 | query-only ACTIVE | 不可放量 |
| `BF-022` | Booking | 预约创建 | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md` | 完整 | 已核实 | 已核实 | Doc Closed / Can Develop / Cannot Release | 不可放量 |
| `BF-023` | Booking | 预约取消 | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md` | 完整 | 已核实 | 已核实 | Doc Closed / Can Develop / Cannot Release | 不可放量 |
| `BF-024` | Booking | 加钟 / 升级 | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md` | 完整 | 已核实 | 已核实 | Doc Closed / Can Develop / Cannot Release | 不可放量 |
| `BF-025` | Content | DIY 模板 / 自定义页 | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | 完整 | 已核实 | 未核出独立 | ACTIVE | 可维护 |
| `BF-026` | Content | 聊天 / 文章详情 / FAQ 壳页 / WebView | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | 完整 | 已核实 | 未核出独立 | PLANNED_RESERVED | 不可放量 |
| `BF-027` | Content | 文章列表 / 分类 / 浏览回写 / 已读回写 | `docs/products/miniapp/2026-03-12-miniapp-content-article-list-category-writeback-prd-v1.md` | 完整 | 已核实 | 已核实 | PLANNED_RESERVED | 不可放量 |
| `BF-028` | Brokerage | 分销中心 / 佣金 / 提现 / 团队 / 排行 / 推广订单 / 推广商品 | `docs/products/miniapp/2026-03-10-miniapp-brokerage-distribution-prd-v1.md` | 完整 | 已核实 | 已核实 | PLANNED_RESERVED | 不可放量 |
| `BF-029` | Reserved | 礼品卡 | `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md` | 完整 | 已核实 | 已核实 | `Can Develop / Cannot Release` | 不可放量 |
| `BF-030` | Reserved | 邀请有礼 | `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md` | 完整 | 已核实 | 已核实 | `Can Develop / Cannot Release` | 不可放量 |
| `BF-031` | Reserved | 技师动态 | `docs/products/miniapp/2026-03-12-miniapp-technician-feed-prd-v1.md` | 完整 | 已核实 | 已核实 | `Can Develop / Cannot Release` | 不可放量 |

## 4. 管理后台功能清单

| ID | 业务域 | 功能 | 主 PRD | PRD | Contract | Runbook | 当前工程结论 | 放量结论 |
|---|---|---|---|---|---|---|---|---|
| `BO-001` | Finance Ops | 四账对账 | `docs/products/miniapp/2026-03-12-miniapp-finance-ops-four-account-reconcile-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-finance-ops-core-contract-v1.md` | `docs/plans/2026-03-24-admin-finance-ops-core-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `BO-002` | Finance Ops | 退款回调日志 / 重放 / 重放运行日志 | `docs/products/miniapp/2026-03-12-miniapp-finance-ops-refund-notify-replay-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-finance-ops-core-contract-v1.md` | `docs/plans/2026-03-24-admin-finance-ops-core-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `BO-003` | Finance Ops | 技师提成结算 / 审核 / 驳回 / 打款 / 通知补偿 | `docs/products/miniapp/2026-03-12-miniapp-finance-ops-technician-commission-settlement-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-finance-ops-core-contract-v1.md` | `docs/plans/2026-03-24-admin-finance-ops-core-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `BO-004` | Finance Ops | 技师提成明细 / 计提管理 | `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-detail-config-prd-v1.md` | 完整 | 已核实 | 已核实 | admin-only 页面/API 真值已闭环 / Can Develop / Cannot Release | 不可放量 |
| `ADM-001` | Product | 总部商品 SPU 管理 / 新增编辑 | `docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-product-spu-template-contract-v1.md` | `docs/plans/2026-03-24-admin-product-spu-template-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-002` | Product | 商品模板校验 / SKU 自动生成 | `docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-product-spu-template-contract-v1.md` | `docs/plans/2026-03-24-admin-product-spu-template-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-003` | Store | 门店主数据管理 | `docs/products/2026-03-15-hxy-admin-store-master-and-governance-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-store-master-contract-v1.md` | `docs/plans/2026-03-24-admin-store-master-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-004` | Store | 门店分类管理 | `docs/products/2026-03-15-hxy-admin-store-master-and-governance-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-store-master-contract-v1.md` | `docs/plans/2026-03-24-admin-store-master-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-005` | Store | 门店标签管理 | `docs/products/2026-03-15-hxy-admin-store-master-and-governance-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-store-master-contract-v1.md` | `docs/plans/2026-03-24-admin-store-master-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-006` | Store | 门店标签组管理 | `docs/products/2026-03-15-hxy-admin-store-master-and-governance-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-store-master-contract-v1.md` | `docs/plans/2026-03-24-admin-store-master-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-007` | Store Product | 门店商品 SPU 映射 / 上下架 | `docs/products/2026-03-15-hxy-admin-store-product-mapping-and-sku-ops-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-store-product-sku-contract-v1.md` | `docs/plans/2026-03-24-admin-store-product-sku-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-008` | Store Product | 门店 SKU 价库存管理 / 批量调整 / 库存流水重试 | `docs/products/2026-03-15-hxy-admin-store-product-mapping-and-sku-ops-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-store-product-sku-contract-v1.md` | `docs/plans/2026-03-24-admin-store-product-sku-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-009` | Supply Chain | 门店库存调整单审批 | `docs/products/2026-03-15-hxy-admin-store-stock-adjust-and-transfer-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-supply-chain-stock-approval-contract-v1.md` | `docs/plans/2026-03-24-admin-supply-chain-stock-approval-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-010` | Supply Chain | 跨店调拨单审批 | `docs/products/2026-03-15-hxy-admin-store-stock-adjust-and-transfer-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-supply-chain-stock-approval-contract-v1.md` | `docs/plans/2026-03-24-admin-supply-chain-stock-approval-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-011` | Store Governance | 门店生命周期批次执行日志 / 批次复核执行 | `docs/products/2026-03-15-hxy-admin-store-lifecycle-governance-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-store-lifecycle-governance-contract-v1.md` | `docs/plans/2026-03-24-admin-store-lifecycle-governance-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-012` | Store Governance | 门店生命周期变更单 | `docs/products/2026-03-15-hxy-admin-store-lifecycle-governance-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-store-lifecycle-governance-contract-v1.md` | `docs/plans/2026-03-24-admin-store-lifecycle-governance-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-013` | Store Governance | 门店生命周期复核日志 | `docs/products/2026-03-15-hxy-admin-store-lifecycle-governance-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-store-lifecycle-governance-contract-v1.md` | `docs/plans/2026-03-24-admin-store-lifecycle-governance-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-014` | Trade Ops | 售后单管理 / 详情 | `docs/products/2026-03-15-hxy-admin-after-sale-review-ticket-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-trade-ops-after-sale-review-ticket-contract-v1.md` | `docs/plans/2026-03-24-admin-trade-ops-after-sale-review-ticket-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-015` | Trade Ops | 售后人工复核工单 | `docs/products/2026-03-15-hxy-admin-after-sale-review-ticket-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-trade-ops-after-sale-review-ticket-contract-v1.md` | `docs/plans/2026-03-24-admin-trade-ops-after-sale-review-ticket-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |
| `ADM-016` | Trade Ops | 售后工单 SLA 路由规则 | `docs/products/2026-03-15-hxy-admin-after-sale-review-ticket-prd-v1.md` | 完整 | `docs/contracts/2026-03-24-admin-trade-ops-after-sale-review-ticket-contract-v1.md` | `docs/plans/2026-03-24-admin-trade-ops-after-sale-review-ticket-runbook-v1.md` | ACTIVE_ADMIN | 可开发；独立配套已补齐 |

## 5. SPU / SKU 专项口径
- 不允许再把 `SPU / SKU` 混成泛化的“商品后台”。
- 当前必须按下列 6 个后台能力 + 1 个前台承接能力理解：
  - `ADM-001` 总部商品 SPU 管理
  - `ADM-002` 商品模板校验 / SKU 自动生成
  - `ADM-007` 门店商品 SPU 映射 / 上下架
  - `ADM-008` 门店 SKU 价库存管理 / 批量调整 / 库存流水重试
  - `ADM-009` 门店库存调整单审批
  - `ADM-010` 跨店调拨单审批
  - `BF-015` 前台商品分类 / 搜索 lite / 商品详情承接

## 6. 当前仍需强调的 No-Go
- `Booking` 写链：文档完整，但当前只允许写成 `Can Develop / Cannot Release`。
- `BO-004`：只允许写成 `admin-only 页面/API 真值已闭环 / Can Develop / Cannot Release`，不得误写成 release-ready。
- `Member`：`/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 已落地，但当前仍只允许写成 `Can Develop / Cannot Release`，不能再误写为缺页 blocker。
- `Reserved`：gift-card / referral / technician-feed runtime 已闭环，但 gray / rollback / sign-off 证据仍未补齐，不能误写成已可放量。
- 后台主能力：PRD、独立 contract、独立 runbook 已成体系；当前仍不能把 `ACTIVE_ADMIN` 误写成 release-ready。
