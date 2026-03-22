# MiniApp Booking Review P2 Final Integration v1（2026-03-22）

## 1. 目标与吸收边界
- 目标：以 A 窗口身份完成 booking review P2「运营效率 + 告警联动」最终集成，把 B/C/D 已正式提交的结果收口为单一真值，并给出阶段性 Go/No-Go 判断。
- 本文只吸收已核实存在的正式提交：

| 窗口 | commit hash | message | 当前吸收范围 |
|---|---|---|---|
| B | `acd28f8dbf9d3b41211a779dd2f9c07d39b94d45` | `feat(booking-review): improve ops efficiency drilldown` | 看板/台账 drill-down、治理入口与 query helper 真值 |
| C | `ccfdcddd8f982a1ed3c3620ac479736eafeedbb1` | `feat(booking-review): close ops alert contract truth` | admin query / detail / dashboard-summary contract 真值 |
| D | `5060844bacec84f25b99a8af6ea2c5d040a4d4e4` | `docs(booking-review): close ops alert runbook gate` | runbook、acceptance SOP、threshold、blocker/degraded 口径 |

- 已核实以上 3 个 commit 真实存在；当前 `Pending formal window output = 0`。
- 本文不吸收：
  - 未归档的真实 App / 企微送达样本
  - 未归档的 release batch / rollout / rollback 证据
  - 未核出的稳定 admin 专属错误码
  - 任何把 dashboard/list/alert 文案增强外推成 `release-ready` 的说法

## 2. 阶段性判断

| 判断项 | 当前结论 | 说明 |
|---|---|---|
| 文档状态 | `Doc Closed` | P2 的产品、contract、runbook、A 窗口 final integration 已收口 |
| 已完成开发 | `Yes` | B/C/D 对应的页面治理入口、query contract、runbook/gate 口径均已正式提交 |
| 已完成联调 | `Yes` | B/C/D 的 query 字段、status/code、runbook 边界已对齐，并各自给出验证结果 |
| admin-only 已可用 | `Yes` | 当前仅限后台值班/治理/观测场景可用 |
| 当前是否可开发 | `Yes` | 可以继续沿 admin-only 方向补证据、补样本、补门禁 |
| 当前是否可放量 | `No` | 仍缺 release 级样本、外部触达证据与发布门禁材料 |
| Release Decision | `No-Go` | 当前只允许作为治理与工程闭环输入，不得写成放量结论 |

## 3. P2 解决了哪些运营效率问题

| 问题 | 本轮解决结果 | 证据来源 | 当前价值 |
|---|---|---|---|
| 看板与台账入口混用，值班路径不清 | 固定 `/mall/booking/review/dashboard` 为观察入口 + 治理跳转入口，`/mall/booking/review` 为唯一治理台账 | B `acd28f8dbf` | 运营知道“先看哪里、处理去哪里” |
| 卡片跳转口径容易漂移 | drill-down 与快捷筛选统一收口到 `queryHelpers.mjs`，避免看板/台账各写一套 query | B `acd28f8dbf` | 降低联调和运营培训成本 |
| 优先级、通知风险、SLA 字段容易被误当成 query 或 release capability | 固定 `priorityLevel / priorityReason / notifyRiskSummary` 为返回/展示字段；固定 `managerSlaStage / managerTimeoutCategory / priorityReasonCode / notifyAuditStage` 为稳定 status/code | B `acd28f8dbf` + C `ccfdcddd8f982a1ed3c3620ac479736eafeedbb1` | 减少口径误用，保证治理视图只按真实 contract 运行 |
| 看板只显示基础计数，无法快速聚焦高风险事项 | `dashboard-summary` 新增优先级、超时、notify audit 聚合计数 | C `ccfdcddd8f982a1ed3c3620ac479736eafeedbb1` | 值班更快识别高风险差评堆积 |
| 即将超时、已超时、阻断、发送失败语义混淆 | 固定 due soon 是观察态，timeout / ANY_BLOCKED / BLOCKED_NO_OWNER / FAILED 才是 blocker 语义 | C `ccfdcddd8f982a1ed3c3620ac479736eafeedbb1` + D `5060844bacec84f25b99a8af6ea2c5d040a4d4e4` | 运营不再把观察态误当事故，也不把 blocker 当 warning |
| 告警联动容易被写成“已上线自动升级” | runbook/SOP 固定 `Can Develop=Yes / Can Release=No / No-Go`，并写清 fallback/rollback | D `5060844bacec84f25b99a8af6ea2c5d040a4d4e4` | 防止口径误升、降低误发布风险 |

## 4. 已完成开发 / 已完成联调 / admin-only 已可用 / 仍不可放量

| 主题 | 已完成开发 | 已完成联调 | admin-only 已可用 | 仍不可放量原因 |
|---|---|---|---|---|
| 看板/台账运营效率增强 | 是 | 是 | 是 | 只是后台治理提效，不是用户侧能力放量 |
| admin query / detail / dashboard-summary contract 收口 | 是 | 是 | 是 | 仍缺 release 级 runtime 样本与外部触达证据 |
| SLA 阈值、runbook、acceptance SOP 收口 | 是 | 是 | 是 | 只证明值班边界守住，不证明告警联动已发布 |
| App / 企微通知真实送达闭环 | 否 | 否 | 仅观测与治理辅助 | 未归档真实送达回执、失败样本、灰度范围 |
| 发布级门禁材料 | 否 | 否 | 否 | 未形成 runtime gate、rollout/rollback、release evidence pack |

## 5. 哪些告警联动已经进入真实工程闭环
1. 后台观察入口与治理入口已经区分清楚：看板负责观察和一跳进入台账，台账负责筛选与治理动作。
2. P2 真实 query 字段已固定，只认：
   - `id`
   - `bookingOrderId`
   - `storeId`
   - `technicianId`
   - `memberId`
   - `reviewLevel`
   - `riskLevel`
   - `followStatus`
   - `onlyManagerTodo`
   - `onlyPendingInit`
   - `managerTodoStatus`
   - `managerSlaStatus`
   - `replyStatus`
   - `submitTime[]`
3. `GET /booking/review/page`、`GET /booking/review/get`、`GET /booking/review/dashboard-summary` 的 P2 派生字段与聚合计数已按 contract 收口。
4. 阈值与阶段语义已固定：
   - `CLAIM_DUE_SOON / FIRST_ACTION_DUE_SOON / CLOSE_DUE_SOON` 只是观察态
   - `CLAIM_TIMEOUT / FIRST_ACTION_TIMEOUT / CLOSE_TIMEOUT / ANY_BLOCKED` 才是 blocker 语义
5. runbook 已固定 reminder job、fallback、rollback 口径：PASS 只表示边界守住，不表示 `release-ready`。

## 6. 哪些仍只是治理/观察，不是发布能力
1. `priorityLevel / priorityReason / notifyRiskSummary` 当前只是返回字段/展示文案，不是 query 字段，不是独立功能开关。
2. `priorityP0Count~P3Count`、`managerTimeoutDueSoonCount`、`notifyAudit*Count` 只是后台聚合计数，不是外部送达率、升级成功率或已处置率。
3. `notifyAuditStage=PENDING_DISPATCH`、缺 notify 记录、全 0 dashboard、`ANY_FAILED`、`MANUAL_RETRY_PENDING`、`DIVERGED` 当前都还是观察态，不等于接口错误，也不等于自动联动闭环。
4. `SENT`、`DUAL_SENT` 只能解释为“系统已派发记录存在”，不能解释为“店长已读”“门店已处理”“问题已闭环”。
5. job / outbox / routing 的存在只能证明治理链路和观测面存在，不能外推成 1000 家门店真实通知闭环已上线。
6. 当前没有服务端 `degraded=true / degradeReason` 真实证据，不得补写假降级口径。

## 7. blocker、责任窗口、解除条件

| blocker | 当前状态 | 责任窗口 | 解除条件 |
|---|---|---|---|
| 发布级 App / 企微送达样本与回执未归档 | 未解除 | A + D | 归档真实发送成功/失败样本、送达回执、人工重试样本，并经 A 终审通过 |
| P2 仍停留在 admin-only，不是 release capability | 未解除 | A | 补齐 release batch、rollout/rollback、runtime gate 后，再次做 Go/No-Go 评审 |
| `priorityLevel` 等字段未形成独立 query 能力 | 未解除 | B + C | 后端补真实 query 能力、前端接入、contract 与测试同步更新，再由 A 重审 |
| 稳定 admin 专属错误码未核出 | 未解除 | C | 核出对外稳定 admin 错误码并完成 contract 固化；若没有，就继续明确“未核出”而非假写稳定码 |
| 观察态 / blocker 态 / release-ready 口径误升风险 | 持续看护 | A + D | runbook、SOP、final integration 保持 `Can Release=No / No-Go`，并用样本重新验证 |

## 8. 当前单一真值与 No-Go 判断
- 当前 booking review P2 的单一真值只认：
  - 本文 `docs/products/miniapp/2026-03-22-miniapp-booking-review-p2-final-integration-v1.md`
  - `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
  - `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`
- 当前最终结论固定为：
  - `已完成开发`
  - `已完成联调`
  - `admin-only 已可用`
  - `仍不可放量`
  - `Release Decision=No-Go`
- 本轮不得吸收以下错误结论：
  1. dashboard/list/alert 文案增强 = `release-ready`
  2. job / outbox / routing 存在 = 线上通知闭环
  3. `SENT` / `DUAL_SENT` = 店长已处理完成
  4. `priorityLevel` = 已支持独立筛选
  5. PASS = 可以放量
