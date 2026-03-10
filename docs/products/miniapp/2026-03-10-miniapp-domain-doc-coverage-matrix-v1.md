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
| Booking & Technician Service | Still Blocked | 预约列表/详情、技师详情查询为当前真值；创建/取消/加钟仍阻断 | Full | Full | Full | Full | Full | Full | 92 | FE/BE `method + path` 漂移未清除；旧路径仍可能被误放进 allowlist | P0 |
| Content / DIY / Customer Service | Ready | DIY 模板/自定义页已可执行；聊天、文章正文、FAQ 壳页、WebView 已有正式文档边界 | Full | Full | Full | Full | Full | Full | 91 | FAQ 只是壳页；聊天发送失败必须 fail-close；文章列表/分类/已读回写仍是 `ACTIVE_BE_ONLY` 或 `PLANNED_RESERVED` | P1 |
| Brokerage / Distribution | Ready | 分销中心、钱包、提现、团队、排行、推广订单、推广商品均已形成正式文档边界 | Full | Full | Full | Full | Partial | Full | 90 | 缺少独立客服 SOP；申诉/撤回/取消提现仍缺页；`brokerageOrderCount` 与前端 `item.orderCount` 存在字段对齐风险 | P1 |
| Product / Search / Catalog | Ready | 分类、search-lite、商品详情为当前真值；评论/收藏/足迹、canonical search 仍有边界 | Full | Full | Full | Full | Partial | Full | 90 | `search-lite` 与 `search-canonical` 必须分池；收藏状态路径是 `/product/favorite/exits`；评论/收藏/足迹仍不得误升 `ACTIVE` | P1 |
| Marketing Expansion | Ready | 秒杀、拼团、满减送、商品营销聚合已形成正式文档；整域仍按 `PLANNED_RESERVED` 管理 | Full | Full | Full | Full | Full | Full | 89 | `type=2 bargain` 只能隐藏或忽略；砍价仍无 FE route/API 绑定；整域不能因页面可访问就记为 `ACTIVE` | P1 |
| Reserved Expansion（Gift / Referral / Feed） | Ready | 激活 checklist、灰度验收、误发布处置与告警路由均已齐备 | Full | Full | Full | Full | Full | Full | 92 | 仍无真实页面、controller、运行样本；治理文档不能替代 runtime 闭环 | P0 |

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

### 4.3 当前唯一 Still Blocked 域
1. `Booking & Technician Service`
   - 原因不是缺文档，而是已存在明确的 FE/BE `method + path` 漂移。
   - 只要旧路径 `list-by-store / time-slot/list / PUT cancel / /booking/addon/create` 还在联调口径中，就不能进入冻结候选。

### 4.4 当前最危险的四类缺口
1. `member` 缺页能力被误写成已上线页面。
2. `booking` 旧路径被误当成 canonical truth。
3. `catalog` 把 `search-lite` 与 `search-canonical` 混算，或把 `/product/favorite/exits` 私自更正。
4. `reserved-expansion` 在关闭态误命中 `RESERVED_DISABLED`，被当成 warning 而不是 mis-release。

### 4.5 03-10 B/C/D 已正式落盘的增量价值
1. B 侧已经补齐：content、brokerage、product-catalog、marketing-expansion PRD。
2. C 侧已经补齐：booking 用户 API alignment、content、brokerage、product-catalog、marketing-expansion contract，并扩展了 canonical errorCode register。
3. D 侧已经补齐：domain release acceptance matrix、domain alert owner routing、content SOP、brokerage runbook、product KPI/alerting、marketing ops playbook、reserved activation checklist 和灰度 runbook。
4. 因此 03-10 的问题已从“缺文档”转为“capability scope 与 runtime truth 继续收口”。

## 5. P0 收口顺序
1. `Booking method + path 真值收口`
   - 清除 FE/BE 旧路径漂移。
   - 解除条件：旧路径完全移出 FE 联调和发布 allowlist。
2. `Member 缺页能力边界固化`
   - `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 持续固定为缺页能力。
   - 解除条件：真实页面落地，且同步回填 PRD/contract/capability ledger。
3. `Reserved Expansion 激活边界固化`
   - gift/referral/feed 继续只按治理和灰度文档管理。
   - 解除条件：真实 route、controller、样本包、开关审批、误发布告警全部闭环。
4. `03-10 Ready 域 capability scope 继续收口`
   - content / brokerage / product / marketing 继续按 contract 明示的 `PLANNED_RESERVED / ACTIVE_BE_ONLY` 管理。
   - 解除条件：A 侧 capability ledger、freeze review、release decision 一致升级，而不是单点误升。

## 6. P1 收口顺序
1. `Alias route 持续清理`
   - `/pages/public/login`、`/pages/user/index`、`/pages/user/sign-in`、`/pages/search/index`、`/pages/booking/list` 不再出现在新增真值文档。
2. `Catalog 细粒度运行口径补强`
   - 单独补齐评论/收藏/浏览历史的 SOP 和更细的 acceptance evidence。
3. `Brokerage 客服 SOP 补齐`
   - 在 runbook 之外，单独固化提现失败、处理中、到账确认、人工复核的客服话术。
4. `Marketing Expansion capability freeze 预评估`
   - 在不误伤 `type=2 bargain`、不误升砍价的前提下，评估秒杀/拼团/满减送是否具备后续冻结候选条件。

## 7. 03-10 终审状态判定

| 域 | 当前状态 | 终审说明 |
|---|---|---|
| Member | Ready | 文档包完整，仍受缺页能力与 `PLANNED_RESERVED` API 约束 |
| Booking | Still Blocked | 文档包完整，但 FE/BE 真值漂移仍在 |
| Content / Customer Service | Ready | 文档包完整，但只能按 content scope 分层使用，不得整域误升 `ACTIVE` |
| Brokerage | Ready | 文档包完整，但到账/申诉/撤回等资金边界仍需守住 |
| Product / Search / Catalog | Ready | 文档包完整，但 `search-lite`、canonical search、互动链路必须分层 |
| Marketing Expansion | Ready | 文档包完整，但整域继续按 `PLANNED_RESERVED` 管理 |
| Reserved Activation | Ready | 治理闭环完整，但 runtime 仍未闭环 |

## 8. 结论
1. 03-10 业务域文档覆盖已经完成从“缺口补齐”到“正式落盘”的闭环，当前 `Ready = 31`，`Draft = 0`。
2. 文档完整不等于 capability `ACTIVE`；后续冻结评审仍必须以真实 route/API/contract/runbook 四件套同步校验。
3. 03-09 Frozen 基线不回退；03-10 当前仍没有新的 `Frozen Candidate`。
4. 接下来的治理重点不再是补文档数量，而是把 booking、member、reserved 和 mixed-scope domains 的边界继续守住。
