# MiniApp Content and Customer Service SOP v1 (2026-03-10)

## 1. 目标与适用范围
- 目标：建立内容域与客服域的统一接待、转人工、升级、回访闭环 SOP，覆盖文章、FAQ、客服聊天、DIY 页异常的标准处置。
- 适用页面与接口：
  - 聊天：`/pages/chat/index` -> `/promotion/kefu-message/send`、`/promotion/kefu-message/list`
  - 文章/富文本：`/pages/public/richtext`、`/pages/public/webview` -> `/promotion/article/get`
  - FAQ：`/pages/public/faq` -> FAQ 本地数据 + 文章跳转
  - DIY：首页/自定义页 -> `/promotion/diy-template/used`、`/promotion/diy-template/get`、`/promotion/diy-page/get`
- 对齐基线：
  - `docs/products/miniapp/2026-03-09-miniapp-cs-sop-and-escalation-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-user-facing-errorcopy-and-recovery-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-content-compliance-styleguide-v1.md`
  - `docs/plans/2026-03-08-miniapp-degrade-retry-playbook-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
- 当前 capability 真值：
  - `CAP-CONTENT-001 content.kefu-article-faq = PLANNED_RESERVED / BACKLOG-DOC-GAP`
  - 本文补齐的是 D 窗口客服 / 运营 / 回滚闭环，不自动等价为 A 窗口可改 `ACTIVE`

## 2. 处理原则
- 先保活页面，再判断是否转人工，不允许“白屏后再解释”。
- 按错误码和降级标记处置，不按 message 自由发挥。
- `content / FAQ / chat / DIY` 只能使用内容域帮助与咨询话术，禁止套用交易域“退款中 / 到账中 / 售后处理中 / 已完成”话术。
- 所有工单必须携带：`runId/orderId/payRefundId/sourceBizNo/errorCode`；无值时统一填 `"0"`。
- 降级不等于成功，任何 `degraded=true` 都必须同步恢复动作和回访计划。
- `degraded=true`、`TICKET_SYNC_DEGRADED` 样本统一进入 `degraded_pool`，不得计入内容主可用率，也不得回填活动主 ROI / 主转化。

## 3. 客服接待分层

| 层级 | 角色 | 职责 | 首响时限 |
|---|---|---|---|
| `L1` | 客服一线 | 接待、识别页面/错误码、执行标准话术、收集五键 | 5 分钟 |
| `L2` | 客服组长 / 转人工坐席 | 判断是否升级、补充证据、跟进恢复进度 | 15 分钟 |
| `L3` | 域 on-call（Content / Product / Promotion） | 技术/运营处置、降级/回滚、给出根因与恢复时间 | P0 5 分钟，P1 15 分钟 |

## 4. 标准流程

### 4.1 客服接待
1. 先确认场景：文章、FAQ、聊天、DIY 页。
2. 记录用户描述、页面路由、出现时间、最近操作。
3. 读取错误码或判断是否 `degraded=true`。
4. 执行对应话术与恢复动作。

### 4.2 转人工
- 满足任一条件必须转人工：
  - 用户连续 2 次按标准动作仍无法恢复。
  - 聊天发送失败、内容页返回白屏、DIY 首屏不可用。
  - 命中 P0/P1 错误码或内容合规拦截。
  - 同一问题 15 分钟内出现 3 单以上。
- 固定人工接管入口：
  - 客服一线 `L1` -> 转人工坐席 `L2`
  - 工单类型固定为 `CONTENT_CS_INCIDENT` 或 `CONTENT_DEGRADE_TASK`
  - DIY / 内容配置异常必须同步内容运营执行“下线 / 恢复上一稳定模板快照”

### 4.3 失败升级

| 级别 | 典型场景 | 升级对象 | 升级时限 |
|---|---|---|---|
| P0 | DIY 首页不可用、聊天完全不可发、内容误导/假成功拦截 | Content Ops Owner + Product on-call + SRE | 5 分钟 |
| P1 | 文章详情异常、FAQ 跳转失效、聊天历史拉取异常、DIY 局部模块失效 | Content on-call / Product on-call | 15 分钟 |
| P2 | 单篇文章异常、FAQ 单条失效、聊天延迟、个别 DIY 组件降级 | 值班客服 + 内容运营 | 30 分钟 |

### 4.4 回访闭环
1. 处理完成后 24 小时内回访用户。
2. 回访必须说明：问题原因、临时措施、恢复状态、后续入口。
3. 工单状态必须从 `Open -> Ack -> Mitigating -> Resolved -> Closed` 闭环。

## 5. 工单字段模板

| 字段 | 说明 | 规则 |
|---|---|---|
| `ticketType` | `CONTENT_CS_INCIDENT` / `CONTENT_DEGRADE_TASK` | 必填 |
| `scene` | `article/faq/chat/diy/webview` | 必填 |
| `route` | 页面路由 | 必填 |
| `runId` | 批次/巡检/回放 ID | 无值填 `"0"` |
| `orderId` | 订单主键 | 无值填 `"0"` |
| `payRefundId` | 退款单主键 | 无值填 `"0"` |
| `sourceBizNo` | 业务来源号 | 无值填 `"0"` |
| `errorCode` | 错误码或 `"0"` | 必填 |
| `degraded` | 是否降级 | 必填 |
| `contentId` | 内容主键 | 无值填 `"0"` |
| `contentVersion` | 内容版本 | 无值填 `"0"` |
| `recoveryAction` | 已执行恢复动作 | 必填 |

## 6. 场景话术与恢复动作

### 6.1 文章 / 富文本异常

| 场景 | 标准话术 | 恢复动作 | 升级条件 |
|---|---|---|---|
| 文章打不开 | `当前文章内容暂时不可用，请刷新后重试。若仍失败，我已为你转人工跟进。` | 刷新文章页；失败则回退 FAQ 或上一页 | 连续 2 次失败转 P1 |
| 文章内容错版 | `当前内容正在同步新版本，请稍后刷新查看。` | 刷新；记录 `contentId/contentVersion` | 同版本投诉 >=3 单转内容运营 |
| WebView 打不开 | `当前页面加载较慢，建议返回后重新进入；若仍失败我们会转人工处理。` | 回退上一页；必要时提供 H5 兜底链接 | 白屏或 crash 直接 P1 |

### 6.2 FAQ 异常

| 场景 | 标准话术 | 恢复动作 | 升级条件 |
|---|---|---|---|
| FAQ 列表为空 | `帮助内容正在更新中，请稍后刷新；你也可以直接转人工咨询。` | 刷新 FAQ；提供人工入口 | 连续 2 个窗口空列表转 P1 |
| FAQ 跳文章失败 | `当前帮助详情暂不可见，我先帮你转人工继续处理。` | 跳转人工；记录 FAQ 条目 ID | 同条 FAQ 3 次失败转内容运营 |

### 6.3 客服聊天异常

| 场景 | 标准话术 | 恢复动作 | 升级条件 |
|---|---|---|---|
| 发消息失败 | `当前消息发送未成功，请稍后重试；若仍失败我将直接帮你升级人工处理。` | 重新发送 1 次；失败转人工 | 发送失败率 5 分钟 > 5% 转 P0 |
| 历史消息拉取为空 | `聊天记录可能延迟同步，不影响继续咨询；我已记录并帮你跟进。` | 下拉刷新；允许继续发新消息 | 15 分钟内集中爆发转 P1 |
| 客服不在线 / 未接待 | `当前人工坐席较忙，我已为你排队并保留问题。` | 进入排队/回呼；记录时间 | 排队超 10 分钟升级客服组长 |

### 6.4 DIY 页异常

| 场景 | 标准话术 | 恢复动作 | 升级条件 |
|---|---|---|---|
| 首页 DIY 整页异常 | `当前首页内容加载中，请稍后刷新；如仍异常，我们会尽快恢复。` | 切回基础首页模板 | 首屏不可用直接 P0 |
| 单个模块异常 | `当前部分活动模块同步中，不影响你继续浏览其他内容。` | 隐藏异常模块，保留主 CTA | 同模块 15 分钟异常 >=3 次转 P1 |
| DIY 配置错版 | `页面内容正在更新，请刷新后再看最新版本。` | 恢复上一稳定模板快照 | 误发错误配置直接 P1 |

## 7. 错误码与降级场景标准处置

| 错误码 / 场景 | 适用页面 | 标准处置 | 升级 |
|---|---|---|---|
| `1004001000 USER_NOT_EXISTS` | 聊天 / 内容页需登录场景 | 引导重新登录；不允许继续业务动作 | 连续失败转 P1 |
| `1011000011 ORDER_NOT_FOUND` | 内容内跳订单/售后帮助 | 引导刷新或返回订单列表 | 3 次失败转 P1 |
| `1030004016 BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` | 内容内批次说明/运营公告 | 记录审计 warning，不阻断主阅读 | P2 |
| `TICKET_SYNC_DEGRADED` | 聊天 / 内容协同工单 | 告知“同步中”，保留页面可用 | 15 分钟未恢复转 P1 |
| `degraded=true` | 文章 / DIY / 聊天通用 | 只承诺“部分信息延迟”，不承诺已成功恢复；样本进入 `degraded_pool`，不计主可用率 / 主 ROI | 视影响面升级 |
| `content_fake_success_block` | DIY / 活动页 / 文章 CTA | 立即下线对应内容，客服统一回复“当前活动信息已更新，请以页面最新展示为准” | P0 |

## 8. 运营与技术责任边界

| 团队 | 负责内容 |
|---|---|
| 客服 | 接待、转人工、回访、工单闭环 |
| 内容运营 | 文章/FAQ/DIY 配置修正、下线、恢复公告 |
| Product / Promotion on-call | DIY/内容接口异常、错误码趋势、配置快照回滚 |
| SRE | P0 事故协调、监控路由、回滚执行 |

## 9. 验收标准
1. 四类页面都有可执行的话术、恢复动作、升级条件。
2. 转人工、失败升级、回访闭环步骤明确可审计。
3. 工单字段固定包含 `runId/orderId/payRefundId/sourceBizNo/errorCode`。
4. `degraded=true`、`TICKET_SYNC_DEGRADED`、合规拦截等场景有统一处置口径。
5. 内容域话术与交易域话术边界清晰，客服不会把内容异常误解释为退款/到账/售后成功。
