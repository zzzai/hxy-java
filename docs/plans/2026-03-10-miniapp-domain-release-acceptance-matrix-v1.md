# MiniApp Domain Release Acceptance Matrix v1 (2026-03-10)

## 1. 目标
- 把 `content / brokerage / catalog / marketing-expansion / reserved-expansion` 的验收、门禁、回滚收口到一张可执行矩阵，避免各域各写一套 Go/No-Go 口径。
- 本文只使用当前仓库已落盘真值：
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md`
  - `docs/plans/2026-03-09-miniapp-degraded-pool-governance-v1.md`
  - `docs/contracts/2026-03-09-miniapp-reserved-disabled-gate-spec-v1.md`
  - 各域已落盘 SOP / runbook / playbook

## 2. 统一门禁口径
- `degraded=true` 或已定义 fail-open/warning 样本统一进入 `degraded_pool`，不得计入主成功率、主转化率、主 ROI。
- 任一 `RESERVED_DISABLED` 错误码在开关关闭态或未命中灰度范围返回，统一按“误发布”处理，直接 `No-Go`。
- `search-lite`（`GET /product/spu/page`）与 `search-canonical`（`GET /product/search/page`）必须分池治理，不共享分母、TopN、放量结论。
- 告警、验收、回滚记录统一五键：`runId/orderId/payRefundId/sourceBizNo/errorCode`；字段不适用时固定填字符串 `"0"`。

## 3. 当前域状态真值

| 业务域 | capability 真值 | 当前发布口径 |
|---|---|---|
| `content` | `CAP-CONTENT-001 = PLANNED_RESERVED / BACKLOG-DOC-GAP` | 有页面与接口运行，但未形成 Frozen 级产品/契约闭环，当前只能按联调/预发布门禁执行，不能直接算入 `ACTIVE` 发布分母 |
| `brokerage` | `CAP-BROKERAGE-001 = PLANNED_RESERVED / BACKLOG-DOC-GAP` | 分销页与 controller 存在，但产品/契约未冻结；运行手册可执行，不等于能力已放行 |
| `catalog` | `CAP-PRODUCT-002/004 = ACTIVE`；`CAP-PRODUCT-003/005 = PLANNED_RESERVED` | `catalog-browse + search-lite` 参与主发布门禁；评论/收藏/历史与 canonical search 只走保留能力门禁 |
| `marketing-expansion` | `CAP-PROMO-003 = PLANNED_RESERVED / BACKLOG-DOC-GAP` | 当前只能做联调、灰度准备和回滚预案，不得并入 `promotion.coupon/point-mall` 的 `ACTIVE` 分母 |
| `reserved-expansion` | `CAP-RESERVED-001/002/003 = PLANNED_RESERVED` 且开关默认 `off` | 当前默认 `No-Go`；只有“真实页面 + app controller + checklist + gray evidence”全部成立后才允许进入灰度 |

## 4. 逐域验收矩阵

| 业务域 | 用例ID | 验收用例 | 证据字段 | Go 阈值 | No-Go / 回滚触发 | 回滚动作 | 负责角色 |
|---|---|---|---|---|---|---|---|
| `content` | `CT-01` | 文章 / FAQ / WebView 可读且可恢复 | `scene/route/contentId/contentVersion/degraded/runId/orderId/payRefundId/sourceBizNo/errorCode` | 白屏/崩溃 `=0`；同一 FAQ/文章 15 分钟内失败投诉 `<3`；成功、刷新、转人工三类证据齐全 | 任一白屏/崩溃；同条内容 15 分钟内失败投诉 `>=3` | 下线异常文章/FAQ，转人工承接，冻结当前发布批次 | 客服一线 + 内容运营 |
| `content` | `CT-02` | 客服聊天可发送、可拉取、可转人工 | `scene=chat/route/createTime/degraded/runId/orderId/payRefundId/sourceBizNo/errorCode` | 5 分钟发送失败率 `<=5%`；历史消息异常可刷新后继续咨询；人工接管入口可用 | 聊天完全不可发；5 分钟发送失败率 `>5%` | 统一切人工/排队入口，冻结发布并由内容 on-call 接管 | 客服组长 + Content on-call |
| `content` | `CT-03` | DIY 首页 / 自定义页配置正确，且无假成功表达 | `scene=diy/route/templateId/pageId/contentVersion/degraded/runId/orderId/payRefundId/sourceBizNo/errorCode` | 首页首屏可用；同模块 15 分钟异常 `<3`；`content_fake_success_block = 0` | 首屏不可用任一命中；`content_fake_success_block > 0` | 切回基础首页模板或上一稳定快照，更新客服口径，锁定放量 | Product on-call + Content Ops Owner + SRE |
| `brokerage` | `BR-01` | 佣金结算批次可闭环 | `settlementBatchId/commissionId/runId/orderId/payRefundId/sourceBizNo/errorCode` | `BRO-KPI-01 >= 99.5%`；`pending_outbox_cnt <= 100`；批次审批与支付证据齐全 | 结算成功率跌破阈值；outbox 堵塞 `>100` | 暂停结算支付，只保留查询/审核，转人工复核差异批次 | 财务系统 Owner + Brokerage Domain Owner |
| `brokerage` | `BR-02` | 提现审核 / 打款失败可控 | `withdrawId/amount/auditOperator/payChannel/runId/orderId/payRefundId/sourceBizNo/errorCode` | `BRO-KPI-02 <= 5%`；`BRO-KPI-03 <= 2%`；失败单均有审核结论 | 审核超时率或失败率超阈值 | 暂停新提现申请或切人工审核模式，客服统一改为“处理中/复核中”口径 | 财务值班 + 客服组长 |
| `brokerage` | `BR-03` | 资金异常闭环完整（审核 -> 冲正 -> 申诉 -> 追溯） | `auditId/originCommissionId/reversalId/appealId/traceTicketId/runId/orderId/payRefundId/sourceBizNo/errorCode` | `1030007011 = 0`；冻结余额异常率 `<=0.5%`；申诉超时未结率 `<=10%`；每单异常都能串起四段闭环证据 | 任一 `1030007011`；缺任一闭环证据；冻结余额异常率超阈值 | 停止自动冲正，冻结相关批次与用户，按人工审计单追溯 | 技术 on-call + 财务负责人 + 风控负责人 |
| `catalog` | `CA-01` | `catalog-browse` 主链路可用（分类/列表/详情） | `route/spuId/catalogVersion/degraded/runId/orderId/payRefundId/sourceBizNo/errorCode` | `CAT-KPI-01 <= 1%`；异常空结果率 `<=2%`；`degraded=true` 样本仅在 `degraded_pool` | 详情错误率/异常空结果率超阈值；降级样本混入主口径 | 隐藏异常营销模块，必要时回退到默认商品列表并冻结发布 | Product Domain Owner |
| `catalog` | `CA-02` | `search-lite` 单独治理，不与 canonical 混算 | `searchPool=lite/route/keyword/resultCount/degraded/runId/orderId/payRefundId/sourceBizNo/errorCode` | `CAT-KPI-07 >= 18%`；`CAT-KPI-08 <= 1.5%`；lite 分母、TopN、告警独立 | 与 canonical 共用分母/TopN；lite 错误率超阈值 | 保留 query，回退默认商品列表，暂停继续放量 | Search Owner + Product Domain Owner |
| `catalog` | `CA-03` | `search-canonical` / 目录版本守卫按保留能力门禁执行 | `switchKey/switchValue/matchedRule/searchPool=canonical/catalogVersion/keyword/runId/orderId/payRefundId/sourceBizNo/errorCode` | 开关关闭态 `1008009902/1008009904 = 0`；开关开启灰度时目录版本冲突率 `<=0.5%`、canonical 降级率 `<=3%`、恢复率 `>=85%` | 任一关闭态误返回；保留能力误算进 `ACTIVE` 分母 | 立即关闭 `miniapp.catalog.version-guard` / `miniapp.search.validation`，停止灰度并回退上一稳定口径 | Product on-call + Search Owner + SRE |
| `catalog` | `CA-04` | 评论 / 收藏 / 浏览历史只在保留池内部验收 | `route/featurePack/degraded/runId/orderId/payRefundId/sourceBizNo/errorCode` | 内部联调证据完整，且仍留在 `PLANNED_RESERVED` 分池 | 未补产品/契约仍对外宣称 `ACTIVE` | 隐藏入口或只读承接，不得并入对外发布范围 | Product Domain Owner |
| `marketing-expansion` | `MK-01` | `promotion.activity-growth` 进入灰度前的文档冻结门禁 | `capabilityId/releaseBatch/configSnapshot/runId/orderId/payRefundId/sourceBizNo/errorCode` | PRD / Contract / ErrorCode / Degrade / Ops playbook 全部落盘，并由 A 窗口更新 capability 真值 | `CAP-PROMO-003` 仍为 `BACKLOG-DOC-GAP` 或缺任一文档包 | 保持 capability 处于 `PLANNED_RESERVED`，不得并入发布分母 | Promotion Domain Owner + 发布负责人 |
| `marketing-expansion` | `MK-02` | 秒杀 / 拼团 / 砍价 / 满减送灰度与库存门禁 | `activityId/activityType/storeId/stockSnapshot/runId/orderId/payRefundId/sourceBizNo/errorCode` | 5% -> 20% -> 50% -> 100% 分阶段完成；无新增 P0；库存不低于强制降级阈值（秒杀 `>=5%`、拼团 `>=8%`、砍价 `>=10%`、满减送 `>=10%`） | 任一阶段越级放量；库存低于强制阈值；活动错误率超既定阈值 | 执行 `MKT-07`，关闭入口并恢复上一稳定配置快照 | 运营值班 + 商品/产品 on-call |
| `marketing-expansion` | `MK-03` | 配置/价格/规则冲突不污染交易口径 | `activityId/activityType/ruleSnapshot/runId/orderId/payRefundId/sourceBizNo/errorCode` | 页面价与结算价一致；满减送结算冲突率 `<=1%`；活动关闭/恢复公告同步 | 任一价格错配；结算冲突率 `>1%`；投诉 15 分钟内爆发 3 倍 | 回退活动配置、下线活动、同步客服公告 | 运营值班 + 技术 on-call |
| `marketing-expansion` | `MK-04` | 降级与 ROI 口径隔离 | `activityId/activityType/degraded/orderId/sourceBizNo/errorCode/runId/payRefundId` | `degraded=true` 样本只进 `degraded_pool`，不计入主 ROI / 主转化 / 主成功率 | 降级样本混入主 ROI 或主转化；异常阶段继续放量 | 冻结当前阶段，重算指标，必要时直接关闭活动 | 值班经理 + 运营值班 |
| `reserved-expansion` | `RV-01` | 激活前硬门禁：真实页面、app controller、开关、台账齐全 | `capabilityId/pageRoute/backendApi/switchKey/switchValue/statusReason/runId/orderId/payRefundId/sourceBizNo/errorCode` | 真实页面存在、app controller 存在、capability ledger 改为可灰度状态、激活 checklist 全量通过 | 任一页面/controller 缺失；开关仍为 `off`；A 台账未更新 | 维持 `off`，从发布范围移除，结论固定为 `No-Go` | 对应域 Owner + 发布负责人 |
| `reserved-expansion` | `RV-02` | `RESERVED_DISABLED` 误返回清零 | `switchKey/switchValue/matchedRule/errorCode/runId/orderId/payRefundId/sourceBizNo` | 最近 24 小时误返回计数 `=0` | 任一误返回 | 立即关闭对应开关、清空灰度范围、锁定发布 | SRE + 对应域 on-call |
| `reserved-expansion` | `RV-03` | 灰度阶段推进可量化 | `releaseId/capability/stage/sampleCount/passCount/failCount/switchSnapshot/runId/orderId/payRefundId/sourceBizNo/errorCode` | 样本量达标；主成功率不低于基线；错误率不高于基线 +1pp；降级率 `<=3%`；恢复率 `>=85%`；投诉量不超过基线 2 倍 | 任一指标越阈值；样本未达标仍推进；阶段未完成回滚演练 | 回退到上一阶段或直接停灰，发布公告并复盘 | 对应域 Owner + 运营值班 |
| `reserved-expansion` | `RV-04` | gift / referral / feed 各自保留能力样本包完整 | `capability/errorCode/samplePack/runId/orderId/payRefundId/sourceBizNo` | gift-card / referral / technician-feed 均完成各自最小样本与错误码 evidence；仍按保留能力管理 | 任一能力样本缺失或被提前记为 `ACTIVE` | 该能力单独回退到 `PLANNED_RESERVED` 管理，不影响其他域判断 | Trade / Promotion / Booking + Content Ops Owner |

## 5. 验收输出要求
1. 每个域至少保留一条 `Go` 证据、一条 `degraded` 证据、一条 `rollback` 证据。
2. 所有 `No-Go` 结论必须显式写明触发项、回滚动作、回滚完成时间和负责角色。
3. A 窗口只能依据本文和 capability ledger 更新域状态；不得凭“已有 runbook”直接改 `ACTIVE`。
