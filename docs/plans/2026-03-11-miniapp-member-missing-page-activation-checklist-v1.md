# MiniApp Member Missing-Page Activation Checklist v1 (2026-03-11)

## 1. 目标与适用范围
- 目标：保留 03-11 checklist 文件作为 member 缺页能力闭环记录，并在 2026-03-24 将其升级为“工程闭环已完成、发布 blocker 仍需解除”的执行清单。
- 适用范围：
  - 已闭环页面：`/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag`
  - 已闭环 API：`/member/level/list`、`/member/experience-record/page`、`/member/tag/my`、`/member/asset-ledger/page`
  - 已闭环入口：`pages/user/info.vue` 中的“会员等级 / 资产总览 / 我的标签”
- 对齐基线：
  - `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`
  - `docs/contracts/2026-03-10-miniapp-member-domain-contract-v1.md`
  - `docs/contracts/2026-03-10-miniapp-active-vs-planned-api-matrix-v1.md`
  - `docs/products/miniapp/2026-03-24-miniapp-member-missing-page-closure-review-v1.md`

## 2. 本轮闭环结果

| 能力 | route | 当前工程状态 | 已闭环证据 | 仍未解除的 blocker |
|---|---|---|---|---|
| 等级页 | `/pages/user/level` | 已完成开发 | 页面文件、`pages.json`、个人中心入口、`/member/level/list`、`/member/experience-record/page`、Node 测试 | 发布样本、field dictionary、release sign-off 未闭环 |
| 资产总览页 | `/pages/profile/assets` | 已完成开发 | 页面文件、`pages.json`、个人中心入口、`AppMemberAssetLedgerController`、聚合 service、Node/Maven 测试 | 灰度/回滚材料、真实样本、降级证据未闭环；当前 `degraded=false` 为默认输出 |
| 标签页 | `/pages/user/tag` | 已完成开发 | 页面文件、`pages.json`、个人中心入口、`AppMemberTagController`、Node/Maven 测试 | release 样本、客服口径、最终 allowlist 未闭环 |

## 3. 已完成清单

### 3.1 等级页 `/pages/user/level`
- [x] 新增真实页面文件并进入 `pages.json`
- [x] 页面真实入口被个人中心消费
- [x] 页面只读取真实接口：`/member/level/list`、`/member/experience-record/page`
- [x] PRD、route truth、contract truth 已同步回填
- [x] 基础工程回归通过：正常等级、无等级、经验空页、接口契约

### 3.2 资产总览页 `/pages/profile/assets`
- [x] 新增真实页面文件并进入 `pages.json`
- [x] 真实后端 controller 与前端读取链路同时存在
- [x] 钱包 / 积分 / 优惠券摘要与统一资产台账同页承接
- [x] 聚合 service 已落到 `yudao-server`，避免 `member -> promotion` 循环依赖
- [x] 基础工程回归通过：聚合列表、空列表、按 `assetType` 过滤、契约对齐

### 3.3 标签页 `/pages/user/tag`
- [x] 新增真实页面文件并进入 `pages.json`
- [x] app 端存在正式读取接口 `GET /member/tag/my`
- [x] 标签列表、空态、刷新路径已具备真实 runtime
- [x] 禁止补假数据的前提已解除，页面只消费真实接口

## 4. 剩余发布 blocker
1. `level / assets / tag` 当前只能写成 `Can Develop / Cannot Release`，不得写成 `ACTIVE 可放量`。
2. `/member/asset-ledger/page` 虽已存在真实 controller，但当前没有真实灰度、回滚、门禁样本与降级证据；`degraded/degradeReason` 不能被误写成“已完成降级链路验证”。
3. 三个页面都还缺发布级样本包、客服/运营演练记录与 A 窗口最终 allowlist。
4. Member 域相关总账、capability ledger、release decision 如不回填，会造成“工程已闭环但总账仍写成缺页”或“误升成可放量”的双向失真。

## 5. 明确禁止的表述
- 不得再把这三项写成“缺页能力”。
- 不得把这三项写成“已上线可放量页面”。
- 不得把 `/member/asset-ledger/page` 仅因 controller 已落地就写成“灰度 / 门禁 / 降级都已工程闭环”。
- 不得把 `degraded=false` 默认输出解释成“真实门禁未命中样本”。

## 6. 文档同步要求
本轮工程闭环后，以下文档必须同步改写：
1. member PRD
2. route truth
3. member contract
4. active-vs-planned API matrix
5. capability / business ledger
6. closure review

## 7. 发布前必测样本

| 能力 | 发布前必测样本 |
|---|---|
| 等级页 | 正常等级、无等级记录、经验空页、接口失败、入口跳转 |
| 资产总览页 | 聚合成功、单资产为空、总账为空、读接口失败、真实降级样本、灰度/回滚演练 |
| 标签页 | 正常读取、无标签、读取失败、入口可达、客服解释口径 |

## 8. 退出条件
只有同时满足以下条件，member 本轮闭环能力才允许从 `Can Develop / Cannot Release` 进入下一轮 release 评审：
1. 发布级样本包齐全并经 A 窗口验收。
2. `level / assets / tag` 已同步进入 capability ledger 与项目总账。
3. 资产总账的灰度、回滚、降级语义有真实证据，不再停留在文档口径。
4. 客服 / 运营 / 发布负责人完成统一口径演练。
5. A 窗口重新评审后，才允许从“工程闭环”进入 `Ready for Release Review`。
