# MiniApp 业务域文档覆盖矩阵 v1（2026-03-10）

## 1. 目标
- 基于当前真实代码与 2026-03-08/03-09/03-10 文档仓，评估每个业务域的文档覆盖完整度。
- 覆盖维度固定为：`PRD / Contract / ErrorCode / Degrade / SOP / Runbook`。
- 输出 `coverageScore(0-100)`、缺口项和 P0/P1 收口顺序，作为后续冻结评审与“先文档后开发”的执行面板。

## 2. 评分规则

| 维度 | 满分 | 判定口径 |
|---|---:|---|
| PRD | 20 | 是否有用户流程、页面边界、能力分层、验收清单 |
| Contract | 20 | 是否有页面/API/字段契约或 canonical truth |
| ErrorCode | 15 | 是否有域内错误码锚点和恢复动作 |
| Degrade | 15 | 是否有 fail-open / fail-close / `[]` / `null` / warning 语义 |
| SOP | 15 | 是否有客服、运营或人工接管口径 |
| Runbook | 15 | 是否有发布门禁、监控、回滚、告警或灰度手册 |

### 2.1 评分解释
- `90-100`：文档足以独立支撑联调、验收和冻结评审。
- `80-89`：文档闭环完整，但仍有明确 scope 边界或缺页能力需要守住。
- `60-79`：主链路可执行，但补齐项仍会阻断冻结评审。
- `<60`：文档缺失明显，当前不能作为单一真值。

## 3. 业务域覆盖矩阵

| 业务域 | 当前文档状态 | Runtime 能力范围 | PRD | Contract | ErrorCode | Degrade | SOP | Runbook | coverageScore | 主要缺口 | 补齐优先级 |
|---|---|---|---|---|---|---|---|---|---:|---|---|
| Trade & Pay | Frozen | 购物车、结算、支付提交/结果、订单列表/详情 | Full | Full | Full | Full | Full | Full | 95 | 钱包转账/充值验收条目仍可继续细化；历史 alias route 需持续清理 | P1 |
| After-sale & Refund | Frozen | 售后申请、售后列表/详情、退款进度、回寄、日志 | Full | Full | Full | Full | Full | Full | 96 | 历史原型别名仍需持续从周边文档清理 | P1 |
| Coupon / Point / Home-Growth Core | Frozen | 领券、券列表/详情、积分商城、首页增长基线 | Full | Full | Full | Full | Full | Full | 93 | `miniapp.home.context-check` 仍是门禁能力，不能与已上线入口混写 | P1 |
| Member Account & Assets | Ready | 登录/注册、个人资料、地址、钱包、积分、签到；等级/资产总览/标签为保留边界 | Full | Full | Full | Full | Full | Full | 94 | `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 缺页；`/member/asset-ledger/page` 仍是 `PLANNED_RESERVED` | P0 |
| Booking & Technician Service | Still Blocked | 预约列表/详情、技师详情查询为当前真值；创建/取消/加钟仍阻断；03-16 已补字段字典、恢复动作、canonical matrix、gate audit 与 A 集成 review | Full | Full | Full | Full | Full | Full | 95 | query-only `ACTIVE` 与 write-chain blocker 必须分池；`title/specialties/status`、`data.list/data.total`、`payOrderId`、`duration/spuId/skuId` 仍未闭环；发布级样本与巡检证据仍缺失 | P0 |
| Booking Review & Service Recovery | Ready | review-list 历史 / 汇总为 query-side `ACTIVE`；review-add 提交、结果页与后台恢复台账 / 详情 / 看板已实现，但 release proof 仍未闭环 | Full | Full | Full | Full | Full | Full | 93 | 不得复用商品评论真值；缺 feature flag / rollout / runtime sample pack；`picUrls` 前端未实现；`serviceOrderId` 仍为 `null`；自动奖励 / 补偿 / 店长通知未落地 | P0 |
| Content / DIY / Customer Service | Ready | DIY 模板/自定义页已可执行；聊天、文章正文、FAQ 壳页、WebView 已有正式文档边界 | Full | Full | Full | Full | Full | Full | 94 | FAQ 只是壳页；聊天发送失败必须 fail-close；BF-027 独立文档已落盘，但文章列表/分类/已读回写仍是 `ACTIVE_BE_ONLY` 或 `PLANNED_RESERVED` | P1 |
| Brokerage / Distribution | Ready | 分销中心、钱包、提现、团队、排行、推广订单、推广商品均已形成正式文档边界 | Full | Full | Full | Full | Full | Full | 95 | 申诉/撤回/取消提现仍缺页；`brokerageOrderCount` 与前端 `item.orderCount` 存在字段对齐风险 | P1 |
| Product / Search / Catalog | Ready | 分类、search-lite、商品详情为当前真值；评论/收藏/足迹、canonical search 仍有边界 | Full | Full | Full | Full | Full | Full | 94 | `search-lite` 与 `search-canonical` 必须分池；收藏状态路径是 `/product/favorite/exits`；评论/收藏/足迹仍不得误升 `ACTIVE` | P1 |
| Marketing Expansion | Ready | 秒杀、拼团、满减送、商品营销聚合已形成正式文档；整域仍按 `PLANNED_RESERVED` 管理 | Full | Full | Full | Full | Full | Full | 89 | `type=2 bargain` 只能隐藏或忽略；砍价仍无 FE route/API 绑定；整域不能因页面可访问就记为 `ACTIVE` | P1 |
| Reserved Expansion（Gift / Referral / Feed） | Ready | 激活 checklist、灰度验收、误发布处置与告警路由均已齐备 | Full | Full | Full | Full | Full | Full | 92 | 仍无真实页面、controller、运行样本；治理文档不能替代 runtime 闭环 | P0 |
| Finance Ops Admin | Ready | 四账对账、退款回调重放、结算审批页面已核出；03-15 已补 `BO-004` 独立 page/API binding truth review 与 evidence ledger，但结论仍是 controller-only truth | Full | Full | Partial | Partial | Full | Full | 86 | `BO-004` 独立后台页面文件和独立 API 文件仍 `未核出`；运行样本只到 service/test；发布证据 `未核出`；写接口仍存在 `true` 但 no-op 风险，必须坚持写后回读 | P0 |

## 4. 域级判断与说明

### 4.1 已冻结基线域
1. `Trade & Pay`
2. `After-sale & Refund`
3. `Coupon / Point / Home-Growth Core`

### 4.2 Ready 但未进入 Frozen Candidate 的域
1. `Member Account & Assets`
2. `Content / DIY / Customer Service`
3. `Brokerage / Distribution`
4. `Product / Search / Catalog`
5. `Marketing Expansion`
6. `Reserved Expansion`
7. `Finance Ops Admin`
8. `Booking Review & Service Recovery`

### 4.3 当前唯一 Still Blocked 域
1. `Booking & Technician Service`
   - 原因不是缺文档，而是 query-only `ACTIVE` 与 write-chain blocker 仍必须分池，且页面字段/绑定/发布证据没有闭环。
   - 只要把 gate `PASS`、空态、未绑定字段或 pseudo success 外推成可放量，booking 就不能进入冻结候选。

### 4.4 当前最危险的四类缺口
1. `member` 缺页能力被误写成已上线页面。
2. `booking` 的 query-only `ACTIVE`、gate `PASS`、空态样本或未绑定字段被误写成 write-chain 已 release-ready。
3. `catalog` 把 `search-lite` 与 `search-canonical` 混算，或把 `/product/favorite/exits` 私自更正。
4. `reserved-expansion` 在关闭态误命中 `RESERVED_DISABLED`，被当成 warning 而不是 mis-release。

### 4.5 03-10 B/C/D 已正式落盘的增量价值
1. B 侧已经补齐：content、brokerage、product-catalog、marketing-expansion PRD。
2. C 侧已经补齐：booking 用户 API alignment、content、brokerage、product-catalog、marketing-expansion contract，并扩展了 canonical errorCode register。
3. D 侧已经补齐：domain release acceptance matrix、domain alert owner routing、content SOP、brokerage runbook、product KPI/alerting、marketing ops playbook、reserved activation checklist 和灰度 runbook。
4. A 侧已在 03-11 继续补齐：
   - `docs/products/miniapp/2026-03-11-miniapp-brokerage-customer-service-sop-v1.md`
   - `docs/products/miniapp/2026-03-11-miniapp-product-catalog-customer-recovery-sop-v1.md`
5. A 侧已在 03-11 再补三份 blocker 文档：
   - `docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md`
   - `docs/plans/2026-03-11-miniapp-member-missing-page-activation-checklist-v1.md`
   - `docs/plans/2026-03-11-miniapp-reserved-runtime-readiness-register-v1.md`
6. 因此当前问题已从“缺文档”转为“capability scope 与 runtime truth 继续收口”。

### 4.6 03-12 当前分支新增情况
1. A 侧已正式补齐：
   - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
   - `docs/products/miniapp/2026-03-12-miniapp-finance-ops-four-account-reconcile-prd-v1.md`
   - `docs/products/miniapp/2026-03-12-miniapp-finance-ops-refund-notify-replay-prd-v1.md`
   - `docs/products/miniapp/2026-03-12-miniapp-finance-ops-technician-commission-settlement-prd-v1.md`
   - `docs/products/miniapp/2026-03-12-miniapp-trade-checkout-order-prd-v1.md`
   - `docs/products/miniapp/2026-03-12-miniapp-pay-submit-result-prd-v1.md`
   - `docs/products/miniapp/2026-03-12-miniapp-after-sale-refund-prd-v1.md`
   - `docs/products/miniapp/2026-03-12-miniapp-technician-feed-prd-v1.md`
   - `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
2. B/C/D 侧已正式补齐 BF-027 独立文档包：
   - `docs/products/miniapp/2026-03-12-miniapp-content-article-list-category-writeback-prd-v1.md`
   - `docs/contracts/2026-03-12-miniapp-content-article-list-category-writeback-contract-v1.md`
   - `docs/products/miniapp/2026-03-12-miniapp-content-article-list-category-writeback-sop-v1.md`
   - `docs/plans/2026-03-12-miniapp-content-article-list-category-writeback-runbook-v1.md`
3. 03-14 Finance Ops Admin 当前分支已正式补齐：
   - `docs/products/miniapp/2026-03-14-miniapp-finance-ops-admin-doc-closure-review-v1.md`
   - `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-detail-config-prd-v1.md`
   - `docs/contracts/2026-03-14-miniapp-finance-ops-technician-commission-admin-contract-v1.md`
   - `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-admin-sop-v1.md`
   - `docs/plans/2026-03-14-miniapp-finance-ops-technician-commission-admin-runbook-v1.md`
4. 因此 03-14 Finance Ops Admin 当前不再存在 `Pending formal window output`，问题已从“待提交”切到“页面/API 真值仍未闭环”。
5. 03-14 已新增：
   - `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-final-integration-v1.md`
   - 用于把“文档已闭环 / 工程未闭环 / 可开发但不可放量”的最终判断固化为项目级单一真值

### 4.7 03-15 Window E 专项证据新增情况
1. 已新增：
   - `docs/products/miniapp/2026-03-15-miniapp-finance-ops-technician-commission-admin-page-api-binding-truth-review-v1.md`
   - `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`
2. 两份文档只基于真实 overlay 页面、真实 overlay API、真实 admin controller、真实 service 测试、真实脚本、真实已提交文档。
3. 固定结论：
   - `BO-004` 独立后台页面文件：`未核出`
   - `BO-004` 独立后台 API 文件：`未核出`
   - 运行样本只到 service/test
   - 发布证据：`未核出`
4. 因此 03-15 新增的是“专项真值证据包”，不是“`BO-004` 页面闭环升级”。
5. `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md` 已同步回填“前台/后台功能 -> PRD -> contract/runbook”映射。
6. 其中 `BO-004` 只认：
   - `docs/contracts/2026-03-14-miniapp-finance-ops-technician-commission-admin-contract-v1.md`
   - `docs/plans/2026-03-14-miniapp-finance-ops-technician-commission-admin-runbook-v1.md`
   - `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`
7. 若 03-15 其他后台 PRD 未核到独立 contract/runbook，必须继续写 `未核出独立 contract/runbook`，不能借 `BO-004` 专项文档冲抵。

### 4.8 03-16 Booking Final Truth Batch 新增情况
1. 已新增：
   - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`
   - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md`
   - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
   - `docs/plans/2026-03-16-miniapp-booking-runtime-release-gate-audit-v1.md`
   - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-gate-acceptance-sop-v1.md`
   - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md`
2. 这一批新增的作用不是把 booking 升级为 release-ready，而是把字段漂移、稳定 errorCode、gate 语义与 A 窗口单一真值彻底锁定。
3. 当前 booking 的唯一允许结论固定为：`Doc Closed / Can Develop / Cannot Release`。

### 4.9 03-17 Booking Review Truth Batch 新增情况
1. 已新增：
   - `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
   - `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
   - `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
   - `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
   - `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
   - `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
   - `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
2. 这批新增把 booking review 固定为 booking 域独立子域，不复用商品评论真值，也不把后台恢复页面存在外推成已放量能力。
3. 当前只允许：
   - 把 review history / summary 记为 query-side `ACTIVE`
   - 把 review submit / recovery 记为 `Can Develop / Cannot Release`
4. 当前未核出的关键发布证据包括：
   - feature flag / rollout control
   - runtime sample pack
   - 自动通知店长 / 技师负责人 / 客服负责人链路
   - 自动奖励 / 自动补偿
5. 因此 booking review 当前属于文档闭环完成，但仍不能进入 `Frozen Candidate` 或 release-ready。

## 5. P0 收口顺序
1. `Booking method + path 真值收口`
   - 守住 canonical method/path，不允许旧路径回流。
   - 解除条件：旧路径完全移出 FE 联调和发布 allowlist，且不再被任何主文档引用。
2. `Booking 字段 / 绑定 / 发布证据收口`
   - 清除 `title/specialties/status`、`data.list/data.total`、`payOrderId`、`duration/spuId/skuId` 这些未闭环项被误写成成功。
   - 解除条件：页面读取/提交与 controller/VO 真值收口，且 create/cancel/addon 有发布级样本与巡检证据。
3. `Member 缺页能力边界固化`
   - `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 持续固定为缺页能力。
   - 解除条件：真实页面落地，且同步回填 PRD/contract/capability ledger。
4. `Reserved Expansion 激活边界固化`
   - gift/referral/feed 继续只按治理和灰度文档管理。
   - 解除条件：真实 route、controller、样本包、开关审批、误发布告警全部闭环。
5. `03-10 Ready 域 capability scope 继续收口`
   - content / brokerage / product / marketing 继续按 contract 明示的 `PLANNED_RESERVED / ACTIVE_BE_ONLY` 管理。
   - 解除条件：A 侧 capability ledger、freeze review、release decision 一致升级，而不是单点误升。
6. `Finance Ops Admin 页面真值继续收口`
   - 当前 `BO-004` 仍只有 controller 接口真值，没有独立后台页面文件、没有独立 API 文件、没有独立发布证据。
   - 解除条件：独立后台页面文件、独立前端 API 文件、页面到 `/booking/commission/*` 的绑定证据、独立发布证据全部核出。
7. `Booking Review 发布证据继续收口`
   - 当前 review 子域代码、页面、controller、admin overlay 与文档都已存在，但 release proof 仍未闭环。
   - 解除条件：runtime sample pack、rollout/rollback 控制面、人工恢复升级链路证据与关键字段闭环结论全部核出。

## 6. P1 收口顺序
1. `Alias route 持续清理`
   - `/pages/public/login`、`/pages/user/index`、`/pages/user/sign-in`、`/pages/search/index`、`/pages/booking/list` 不再出现在新增真值文档。
2. `Catalog 细粒度 acceptance evidence 补强`
   - 当前客户恢复 SOP 已补齐，后续重点转为补齐更多可回放验收样本。
3. `Marketing Expansion capability freeze 预评估`
   - 在不误伤 `type=2 bargain`、不误升砍价的前提下，评估秒杀/拼团/满减送是否具备后续冻结候选条件。
4. `Brokerage 资金类样本库补强`
   - 当前客服 SOP 已补齐，后续重点转为补齐提现处理中、到账确认、字段错位的验收样本。

## 7. 当前终审状态判定

| 域 | 当前状态 | 终审说明 |
|---|---|---|
| Member | Ready | 文档包完整，仍受缺页能力与 `PLANNED_RESERVED` API 约束 |
| Booking | Still Blocked | 文档包完整，但 query-only / write-chain 分池、字段/绑定漂移与 release proof 仍未闭环 |
| Content / Customer Service | Ready | 文档包完整，但只能按 content scope 分层使用，不得整域误升 `ACTIVE` |
| Brokerage | Ready | 文档包完整，但到账/申诉/撤回等资金边界仍需守住 |
| Product / Search / Catalog | Ready | 文档包完整，但 `search-lite`、canonical search、互动链路必须分层 |
| Marketing Expansion | Ready | 文档包完整，但整域继续按 `PLANNED_RESERVED` 管理 |
| Reserved Activation | Ready | 治理闭环完整，但 runtime 仍未闭环 |
| Finance Ops Admin | Ready | A/B/C/D 文档包均已落盘，但 `BO-004` 仍只是“仅接口闭环 + 页面真值待核” |
| Booking Review & Service Recovery | Ready | 文档包完整，但当前最终结论仍固定为 `Doc Closed / Can Develop / Cannot Release`，不得误升为已放量评价体系 |

## 8. 结论
1. 当前业务域文档覆盖已经完成从“缺口补齐”到“正式落盘”的闭环，当前 `Ready = 70`，`Draft = 0`，`Pending formal window output = 0`。
2. 文档完整不等于 capability `ACTIVE`；后续冻结评审仍必须以真实 route/API/contract/runbook 四件套同步校验。
3. 03-09 Frozen 基线不回退；03-10 当前仍没有新的 `Frozen Candidate`。
4. 03-11 三份 blocker checklist 只是把 booking/member/reserved 的退出条件写实，不改变既有 `Ready / Still Blocked / PLANNED_RESERVED` 判定；03-12 的 `BO-004` truth review、03-14 的 controller-only contract、03-15 的 page/API binding truth review 与 evidence ledger、03-16 的 booking final truth batch，也都只固定“文档闭环不等于工程闭环，不等于 release-ready”。
5. 03-17 booking review 子域已完成文档闭环，但当前只能按“query/history 可维护、submit/recovery 不可放量”执行，不得外推成已上线评价体系。
6. 接下来的治理重点不再是补文档数量，而是把 booking、booking review、member、reserved、BF-027 content scope，以及 finance-ops admin mixed-scope 的边界继续守住。
7. 当前项目级最终判断已经补齐：`文档已闭环`，但 blocker scope 仍只允许“进入真值修复开发”，不允许直接“进入放量”。
