# MiniApp 业务域文档覆盖矩阵 v1（2026-03-10）

## 1. 目标
- 基于当前真实代码与 2026-03-08/03-09 文档仓，评估每个业务域的文档覆盖完整度。
- 覆盖维度固定为：`PRD / Contract / ErrorCode / Degrade / SOP / Runbook`。
- 输出 `coverageScore(0-100)`、缺口项和 P0/P1 补齐顺序，作为后续产品文档治理的执行面板。

## 2. 评分规则

| 维度 | 满分 | 判定口径 |
|---|---|---|
| PRD | 20 | 是否有用户流程、状态、验收清单 |
| Contract | 20 | 是否有页面/API/字段契约或 canonical list |
| ErrorCode | 15 | 是否有域内错误码锚点和恢复动作 |
| Degrade | 15 | 是否有 fail-open / fail-close / degraded 语义 |
| SOP | 15 | 是否有客服/运营/人工接管口径 |
| Runbook | 15 | 是否有发布门禁、监控、回滚或告警手册 |

### 2.1 评分解释
- `90-100`：文档可以直接支撑联调、验收和发布。
- `70-89`：主链路可执行，但仍有明显补齐项。
- `40-69`：有业务文档基础，但无法独立支撑端到端交付。
- `<40`：文档严重缺失，当前只能依赖代码和口头知识。

## 3. 业务域覆盖矩阵

| 业务域 | Runtime 能力范围 | PRD | Contract | ErrorCode | Degrade | SOP | Runbook | coverageScore | 主要缺口 | 补齐优先级 |
|---|---|---|---|---|---|---|---|---:|---|---|
| Trade & Pay | 购物车、结算、支付提交/结果、订单列表/详情 | Full | Full | Full | Full | Full | Full | 95 | `wallet transfer / recharge` 仍缺独立验收条目；历史原型路由未清理 | P1 |
| After-sale & Refund | 售后申请、售后列表/详情、退款进度、回寄、日志 | Full | Full | Full | Full | Full | Full | 96 | 发布矩阵仍沿用部分原型别名路由，需与真实 uniapp 路由统一 | P1 |
| Booking & Technician Service | 预约列表/详情、技师、时段、创建、取消、加钟 | Full | Partial | Full | Full | Full | Partial | 76 | A 侧已补 `booking-route-api-truth-review`，但 booking 前后端方法/路径仍不一致；缺 C 窗口 canonical contract；addon 缺专属验收 | P0 |
| Member Account & Assets | 登录/注册、个人资料、地址、钱包、积分、签到、等级、资产总览规划 | Full | Full | Full | Full | Full | Full | 84 | A 侧已补 member route truth 收口文档，但仍有缺页能力；`/pages/user/level`、`/pages/profile/assets` 当前缺真实页面；`/member/asset-ledger/page` 仍是 `PLANNED_RESERVED` | P0 |
| Product / Search / Catalog | 首页 DIY、分类、商品列表、商品详情、搜索、收藏、浏览历史、评论 | Partial | Partial | Partial | Partial | None | None | 58 | `search-lite` 与 canonical search 未拆清；D 已补 KPI/alerting，但商品详情/收藏/浏览历史/评论仍缺 B/C 正式 PRD 与 contract | P1 |
| Promotion / Growth | 首页增长、优惠券、积分商城、活动列表、通知触点 | Full | Full | Full | Full | Full | Full | 89 | 拼团/秒杀/砍价/满减送等扩展活动仍缺 B/C 独立业务文档；D 侧 ops playbook 已提交，但真实路由与原型 alias 仍需统一 | P1 |
| Content / DIY / Customer Service | 文章、富文本、FAQ、自定义页、客服聊天 | Partial | None | None | None | Partial | None | 36 | D 已补 SOP；C contract 文件已落盘到工作树但未正式提交；当前仍缺 B PRD、正式 contract、错误码/降级/监控闭环 | P0 |
| Brokerage / Distribution | 分销中心、佣金、团队、提现、排行 | None | None | None | None | None | None | 18 | D 已补 runbook，但整域仍缺 B PRD、C contract、错误码矩阵与客服口径 | P0 |
| Reserved Expansion（Gift / Referral / Feed） | 礼品卡、邀请有礼、技师动态 | Full | Full | Full | Full | Full | Full | 88 | D 已补 activation checklist/runbook，但实现仍为空；治理文档 Ready 不等于 runtime Active | P1 |

## 4. 域级判断与说明

### 4.1 已可作为发布基线的域
1. `Trade & Pay`
2. `After-sale & Refund`
3. `Promotion / Growth`（限定在券、积分、首页增长、通知触点，不含全部营销扩展）

### 4.2 当前最危险的三类缺口
1. Booking 不是“文档少”，而是“文档已写，但代码真实路径/方法不一致”，会制造假 Active。
2. Member 域已不再是“缺文档”，而是“文档已补齐但 route truth 与 Active/Planned 边界仍需收口”；最明显的是 `/pages/public/login`、`/pages/user/index`、`/pages/user/sign-in` 与当前真实 uniapp 路由不一致。
3. Brokerage / Content 域已有真实代码和页面，但几乎没有发布级产品文档，当前高度依赖口头知识。

### 4.3 Route Alias 漂移
- 03-08 IA / 03-09 部分矩阵仍使用原型别名：`/pages/after-sale/*`、`/pages/refund/progress`、`/pages/coupon/center`、`/pages/point/mall`、`/pages/booking/list`。
- 真实 uniapp 路由已经收口到：
  - `/pages/order/aftersale/*`
  - `/pages/coupon/list`
  - `/pages/activity/point/list`
  - `/pages/booking/order-list`
- 这类漂移不改会继续污染验收、埋点和联调脚本。

### 4.4 03-10 A 侧集成增量
1. Booking 域已新增 A 侧真值审查文档 `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`。
   - 覆盖结论：查询链路可保留 `ACTIVE`；`create / cancel / addon` 仍因 method/path 漂移维持阻断。
   - 未完成项：等待 C 窗口交付 booking canonical contract 后，才可重新评估是否进入 Frozen。
2. Member 域已新增 A 侧 route truth 收口文档 `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md`。
   - 覆盖结论：登录、资料、安全、签到、地址、钱包/积分可继续按 Active 承接；`/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 不能假设上线。
   - 未完成项：03-10 member 文档批次仍维持 `Ready`，不强行 Frozen。
3. Content / Brokerage / Catalog / Marketing Expansion 的剩余缺口已统一登记在 `docs/products/miniapp/2026-03-10-miniapp-doc-completion-master-plan-v1.md`。
   - 这些域当前已形成“D 部分提交 + B/C 仍待正式输出”的状态，不应因单侧文档到位被误判为完成。

### 4.5 03-10 已正式提交的 D 侧增量
1. `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md`
   - 作用：把 content/customer-service 域从“只有缺口登记”推进到“已有正式 SOP 输入”。
   - 限制：缺 B PRD 与 C 正式 contract，因此不能升为 `Frozen Candidate`。
2. `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md`
   - 作用：为分销域补齐运行手册与责任边界。
   - 限制：缺 B PRD 与 C 正式 contract，因此仍是 `Still Blocked`。
3. `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md`
   - 作用：为商品目录域提供 KPI/告警输入。
   - 限制：不替代商品详情/收藏/评论/search 套件的正式 PRD 与 contract。
4. `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md`
   - 作用：为营销扩展域补齐运营执行口径。
   - 限制：没有 B/C 正式 PRD/contract 时，仍不能判为闭环。
5. `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md`
   `docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md`
   - 作用：为 reserved activation 建立激活与灰度门禁。
   - 限制：治理文档 Ready，不改变 gift/referral/feed 当前 `PLANNED_RESERVED` runtime 状态。

## 5. P0 补齐顺序
1. `Booking 用户侧真实 API 对齐增补文档`
   - 当前状态：A 侧 route/API truth 审查已交付，但 canonical contract 仍缺。
   - 目标：把 `technician-list / slot / cancel / addon` 的真实方法和路径对齐成可执行单一真值。
   - 交付建议：新增 `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`。
2. `Member route truth / Active-Planned 收口文档`
   - 当前状态：A 侧 route truth 收口已交付，但 03-10 文档仍未进入 Frozen。
   - 目标：把 03-10 member PRD/contract 中的别名路由校正为真实 uniapp route，并明确哪些能力是真 Active、哪些仍是 Planned。
   - 交付建议：优先校正 `/pages/public/login`、`/pages/user/index`、`/pages/user/sign-in`、`/pages/address/list`、`/pages/point/mall`，并把 `/pages/user/level`、`/pages/profile/assets` 固定为缺页能力。
3. `Content / Customer Service 文档包`
   - 当前状态：D 侧 SOP 已正式提交，B PRD 与 C 正式 contract 待补。
   - 目标：把客服聊天、文章、FAQ、DIY 页从“代码存在”提升到“可验收、可运营、可回滚”。
   - 交付建议：补齐 PRD、contract、error/degrade、客服 SOP。
4. `Brokerage / Distribution 文档包`
   - 当前状态：D 侧 runbook 已正式提交，B PRD 与 C 正式 contract 待补。
   - 目标：补齐分销中心、佣金、提现、团队、排行的业务规则和资金口径。
   - 交付建议：至少补齐 PRD、contract、errorcode、runbook 四件套。

## 6. P1 补齐顺序
1. `Search-lite 与 canonical search 拆分文档`
   - 当前真实能力是 `keyword -> /product/spu/page`，规划能力才是 `/product/search/page`。
   - 需要单文档说明二者切换门槛、灰度条件和错误码生效边界。
2. `营销扩展活动文档包`
   - D 侧 ops playbook 已提交，但拼团、秒杀、砍价、满减送、活动聚合页仍未形成完整 PRD/contract。
3. `商品互动文档包`
   - 收藏、浏览历史、评价发布/列表需要从代码状态补到产品验收状态。
4. `Reserved -> Active 切换 checklist`
   - 03-10 D 侧已补灰度验收手册，但治理文档仍不能替代真实 runtime 落地。

## 7. 03-10 终审状态判定

| 域 | 当前状态 | 终审说明 |
|---|---|---|
| Member | Ready | 真值已补齐到可执行输入，但缺页能力与 `PLANNED_RESERVED` API 仍在 |
| Booking | Still Blocked | `create / cancel / addon` 的 method/path 漂移未关闭 |
| Content / Customer Service | Still Blocked | 已有 SOP 输入，但 PRD 与正式 contract 未闭环 |
| Brokerage | Still Blocked | 已有 runbook 输入，但 PRD 与 contract 未闭环 |
| Product / Search / Catalog | Still Blocked | 已有 KPI/告警输入，但缺正式 PRD/contract 套件 |
| Marketing Expansion | Still Blocked | 已有 ops playbook 输入，但缺正式 PRD/contract 套件 |
| Reserved Activation | Ready | 激活治理文档已 Ready，但 runtime 仍是 `PLANNED_RESERVED` |

## 8. 结论
1. 当前文档最完整的是交易、售后、退款、券积分、首页增长主链路。
2. 当前最需要补的不是再写一轮“总览”，而是针对 booking/member/content/brokerage 做域内真值收口。
3. `coverageScore` 不等于代码可用性；它衡量的是“是否已经具备发布级单一真值”。Member 域分数已上升，但还不能直接冻结。
4. D 侧 03-10 文档已经显著提升 content / brokerage / catalog / marketing-expansion / reserved-activation 的终审输入完整度，但不能替代 B/C 正式 PRD 与 contract。
5. 后续封版应先消除 booking 假 Active 与 member route truth 漂移，再决定是否将 03-10 文档推进到下一轮冻结评审。
