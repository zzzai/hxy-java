# MiniApp Member Missing-Page Closure Review v1（2026-03-24）

## 1. 文档目标
- 固定 Member 域 `level / assets / tag` 三项历史缺页能力在 2026-03-24 的最终工程状态。
- 明确哪些已经完成开发与基础回归，哪些仍只能 `Can Develop / Cannot Release`。
- 作为后续项目总账、release gate、客服口径和 A 窗口复核的单一输入。

## 2. 本轮最终结论
- 03-11 以来的 Member 缺页 blocker 已完成工程闭环：
  - `/pages/user/level`
  - `/pages/profile/assets`
  - `/pages/user/tag`
- 本轮已完成：真实页面、`pages.json`、个人中心入口、前端 API、后端 app/controller 或 server 集成层、最小回归测试。
- 本轮未完成：release 样本包、灰度 / 回滚 / 门禁材料、客服与运营放量演练。
- 因此 Member 域当前正确结论是：
  - `level / assets / tag = Doc Closed / Can Develop / Cannot Release`
  - 不是 `缺页能力`
  - 也不是 `release-ready`

## 3. 能力闭环清单

| 能力 | 页面 / 入口 | 后端真值 | 当前结论 | 说明 |
|---|---|---|---|---|
| 会员等级 / 成长值 | `/pages/user/level`；`/pages/user/info` 入口 | `GET /member/level/list`、`GET /member/experience-record/page` | `Can Develop / Cannot Release` | 页面、路由、入口、API 已形成真实 runtime |
| 资产总览 / 统一资产台账 | `/pages/profile/assets`；`/pages/user/info` 入口 | `GET /member/asset-ledger/page`；`AppMemberAssetLedgerController`；`AppMemberAssetLedgerService` | `Can Develop / Cannot Release` | controller 已落在 `yudao-server` 集成层，避免 `member -> promotion` 循环依赖 |
| 用户标签中心 | `/pages/user/tag`；`/pages/user/info` 入口 | `GET /member/tag/my`；`AppMemberTagController` | `Can Develop / Cannot Release` | app 读取链路已闭环，空标签与正常标签均有真实返回 |

## 4. 本轮验证证据

### 4.1 前端
- `node --test yudao-mall-uniapp/tests/member-missing-pages-smoke.test.mjs yudao-mall-uniapp/tests/member-api-alignment.test.mjs`
- 结果：6 passed, 0 failed

### 4.2 后端
- `mvn -pl yudao-server -am -Dtest=AppMemberTagControllerTest,AppMemberAssetLedgerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：BUILD SUCCESS
- 说明：`AppMemberTagControllerTest` 与 `AppMemberAssetLedgerServiceTest` 均已通过。

## 5. 当前剩余 blocker
1. `level`：缺发布级样本与 A 窗口签发，仍不能进入可放量能力分母。
2. `assets`：缺真实灰度 / 回滚 / 门禁 / 降级样本；`degraded=false` 只是默认字段，不是门禁证据。
3. `tag`：缺 release 样本与客服 / 运营演练记录，仍不能对外改口径。
4. 项目总账、capability ledger、release decision 若不回填，会继续把本轮成果误记为“缺页能力”或误记为“已可放量”。

## 6. 明确禁止的误写
- 不得继续写：`/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 是缺页能力。
- 不得改写成：Member 缺页 blocker 已彻底转为 `ACTIVE / release-ready`。
- 不得把 `/member/asset-ledger/page` 的 `degraded/degradeReason` 当前默认输出写成“真实降级链路已验证”。
- 不得把 `controller 已存在` 写成“灰度 / 门禁 / 发布证据已闭环”。

## 7. 对项目级判断的影响
1. Member blocker 从“缺页能力”升级为“真实页面已闭环但发布 blocker 未解除”。
2. 项目主阻断列表应从：
   - `Member 缺页能力补齐`
   更新为：
   - `Member release evidence / gate closure`
3. 这会直接影响后续项目计划排序：
   - Member 本轮之后不再属于“缺页补实现”
   - 转为“release material / gate evidence / overall ledger sync”

## 8. 下一步
1. 回填 capability ledger、miniapp business ledger、全项目 business ledger。
2. 补 Member release 样本、灰度 / 回滚 / 门禁材料。
3. 完成 A 窗口 release decision 复核后，再决定是否解除 `Cannot Release`。
