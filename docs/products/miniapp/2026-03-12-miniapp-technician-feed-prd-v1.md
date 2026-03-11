# MiniApp 技师动态广场 PRD v1（2026-03-12）

## 0. 文档定位
- 目标：把当前仅有的 `technician-feed product policy` 升级为完整 PRD，明确技师动态广场的目标、页面边界、规划接口、审核与降级规则、开关与激活前置条件。
- 当前状态：`PLANNED_RESERVED / NO_GO`
- 真实文档基线：
  - `docs/products/miniapp/2026-03-09-miniapp-technician-feed-product-policy-v1.md`
  - `docs/contracts/2026-03-09-miniapp-technician-feed-contract-v1.md`
  - `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md`
  - `docs/plans/2026-03-11-miniapp-reserved-runtime-readiness-register-v1.md`
- 约束：
  - 当前前端无真实页面、后端无真实 app controller 闭环。
  - 本 PRD 定义的是“产品真值与激活前置条件”，不是当前已上线能力说明。

## 1. 产品目标
1. 让用户可以按门店浏览技师动态内容，增强技师信任与预约转化。
2. 支持点赞、评论等轻互动，但所有互动必须可审计、可幂等、可回退。
3. 支持内容审核与工单协同，保证内容治理闭环。
4. 在计数聚合、审核回调、工单同步异常时保持主链路可解释，而不是伪成功。

## 2. 当前阶段结论
- capability：`technician-feed`
- switch：`miniapp.technician-feed.audit`
- 当前默认值：`off`
- 当前结论：`NO_GO`

原因：
1. 无真实页面：`/pages/technician/feed` 未实现。
2. 无真实 app controller：`/booking/technician/feed/*` 未实现。
3. 无真实运行样本包。
4. 灰度、误发布、客服 SOP 虽已有治理文档，但不能替代 runtime 实现。

## 3. 产品范围

### 3.1 规划中的用户功能
1. 动态列表浏览
2. 动态点赞 / 取消点赞
3. 动态评论创建
4. 评论审核结果回显
5. 从动态跳转技师详情与预约链路

### 3.2 当前不纳入范围
1. 技师自主发帖端
2. 图文编辑器后台
3. 动态举报独立页面
4. 技师动态商业化广告位
5. 直播、短视频、打赏等重交互能力

## 4. 页面真值与规划边界

### 4.1 当前真实页面状态
- 当前无真实用户页面。
- `/pages/technician/feed` 仅为规划路径，不得写成现网已存在页面。

### 4.2 规划页面结构
- 规划列表页：`/pages/technician/feed`
- 规划详情能力：当前不单独定义独立详情页，先以列表卡片 + 技师详情跳转承接。
- 跳转去向：
  - `GET /booking/technician/get` 对应技师详情页
  - 后续可衔接预约链路，但不得越过 booking 真值边界

## 5. 接口真值与规划接口

### 5.1 已存在可复用接口
| 接口 | Method | 状态 | 用途 |
|---|---|---|---|
| `/booking/technician/list` | `GET` | 已有 | 技师列表真值 |
| `/booking/technician/get` | `GET` | 已有 | 技师详情真值 |

### 5.2 规划中的 feed 接口
| 接口 | Method | 当前状态 | 请求关键字段 | 响应关键字段 |
|---|---|---|---|---|
| `/booking/technician/feed/page` | `GET` | 规划 | `storeId`, `pageNo`, `pageSize`, `lastId?` | `list[]:{postId,technicianId,content,media[],likeCount,commentCount,publishTime,degraded}`, `hasMore` |
| `/booking/technician/feed/like` | `POST` | 规划 | `postId`, `action(1=like,0=cancel)`, `clientToken` | `postId`, `liked`, `likeCount`, `idempotentHit` |
| `/booking/technician/feed/comment/create` | `POST` | 规划 | `postId`, `content`, `clientToken` | `commentId`, `postId`, `status`, `degraded` |

规则：
- 上表是当前规划真值，但在真实 controller 落地前，仍只能视为产品规划，不得写成已上线接口。
- 既有 `/booking/technician/list`、`/booking/technician/get` 不得因 feed 规划而被改写。

## 6. 用户流程

### 6.1 目标主流程
1. 用户进入技师动态列表页。
2. 按门店浏览技师动态卡片。
3. 用户点赞或发表评论。
4. 评论进入审核流，审核通过后公开展示。
5. 用户可跳转到技师详情，再进入预约链路。

### 6.2 当前阶段异常流程
1. 当前页面不存在：只能保持 `NO_GO`，不得通过深链、隐藏入口、临时开关偷偷上线。
2. 点赞 / 评论同键异参冲突：按冲突错误码阻断。
3. 计数聚合超时：允许 `degraded=true`，计数回落为 `0`。
4. 审核回调 / 工单同步失败：打 `TICKET_SYNC_DEGRADED`，但主记录不直接回滚删除。

## 7. 核心业务规则

### 7.1 幂等键
- 点赞：`TECH_FEED_LIKE:<memberId>:<postId>:<action>`
- 评论：`TECH_FEED_COMMENT:<memberId>:<postId>:<clientToken>`
- 动态发布 / 同步：`TECH_FEED_PUBLISH:<technicianId>:<sourceBizNo>`

规则：
- 同键同参：幂等成功
- 同键异参：返回冲突错误
- 不允许静默覆盖或重复记数

### 7.2 状态流转
| 对象 | 状态流转 | 规则 |
|---|---|---|
| 动态内容 | `PUBLISHED -> REVIEWING -> APPROVED / REJECTED` | 审核失败不删除主记录，保留审计轨迹 |
| 点赞 / 评论 | `INIT -> EFFECTIVE` | 幂等命中不重复记数 |
| 审核 / 工单协同 | `NORMAL -> DEGRADED` | 可 warning，不可伪成功 |

### 7.3 互动边界
- 技师不存在 / 禁用时，直接阻断互动入口。
- 用户不存在时，阻断互动并提示登录或状态异常。
- 动态与预约链路可衔接，但不得绕过 booking 当前的真实边界。

## 8. 错误码与降级语义

| Code / Key | 场景 | 端侧动作 | 备注 |
|---|---|---|---|
| `TECHNICIAN_NOT_EXISTS(1030001000)` | 技师不存在 | 阻断跳转和互动 | fail-close |
| `TECHNICIAN_DISABLED(1030001001)` | 技师禁用 | 展示不可互动态 | fail-close |
| `USER_NOT_EXISTS(1004001000)` | 互动用户不存在 | 阻断并引导登录/重试 | fail-close |
| `BOOKING_ORDER_NOT_EXISTS(1030004000)` | 关联订单不存在 | 阻断关联动作 | fail-close |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` | 点赞/评论同键异参冲突 | 阻断并提示重试或联系客服 | fail-close |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)` | runId 审计查询不存在 | 允许刷新重试 | fail-close |
| `TICKET_SYNC_DEGRADED` | 审核/工单同步降级 | warning 提示，主流程继续 | fail-open |
| `degraded=true` | 计数聚合超时等降级 | 列表可浏览，计数降为默认值 | fail-open |
| `1030009901` | reserved 关闭态误发布 | 直接 No-Go / mis-release | 仅在关闭态扫描与误发布判定中使用 |

## 9. 客服 / 审核 / 人工兜底
- 客服入口：评论被拒、互动异常、计数异常均提供申诉 / 联系客服通道。
- 审核最小追踪字段：`postId`, `commentId`, `technicianId`, `runId`, `sourceBizNo`, `errorCode`, `payRefundId`。
- 人工兜底规则：
  1. 审核回调失败：人工复核后更新审核结果，不删审计痕迹。
  2. 计数异常：人工重算，不直接修改互动事实记录。
  3. 工单降级：先保留内容主状态，再补录工单与通知。

## 10. 激活前置条件
以下条件全部满足前，不允许把该能力从 `PLANNED_RESERVED` 改成任何灰度或上线状态：
- [ ] 页面存在：`/pages/technician/feed`
- [ ] app controller 存在：`/booking/technician/feed/*`
- [ ] 列表、点赞、评论、审核回写可回归
- [ ] `miniapp.technician-feed.audit` 开关审批完成
- [ ] `1030009901` 关闭态误返回监控到位
- [ ] 客服 / 审核 / 灰度 runbook 可执行
- [ ] 五键日志齐全：`runId/orderId/payRefundId/sourceBizNo/errorCode`

## 11. 验收标准

### 11.1 规划态文档验收
1. 已明确页面、接口、错误码、审核、降级、激活前置条件。
2. 已明确当前仍为 `NO_GO / PLANNED_RESERVED`，不会被误写成已上线能力。
3. 已明确 switch、误发布、warning、degraded 的边界。

### 11.2 未来实现态验收
1. 列表接口分页稳定返回，`hasMore` 可用。
2. 点赞 / 评论幂等规则稳定生效。
3. 审核失败不删除主记录，保留审计轨迹。
4. 聚合降级时页面可浏览，且 `degraded=true` 有清晰解释。
5. 关闭态误命中保留能力，直接触发 No-Go。

## 12. 非目标
- 不把当前 policy 文档当作已上线说明。
- 不定义技师发布后台。
- 不定义直播、短视频、广告等扩展能力。
- 不把“治理文档已齐”误写成“runtime 已闭环”。
