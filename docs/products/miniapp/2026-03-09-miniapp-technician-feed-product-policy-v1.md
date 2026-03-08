# MiniApp 技师动态治理产品策略 v1（2026-03-09）

## 0. 文档定位与契约对齐
- 文档目标：定义技师动态广场的产品治理规则，覆盖内容发布、互动、审核、降级与人工兜底。
- 对齐契约：
  - `docs/contracts/2026-03-09-miniapp-technician-feed-contract-v1.md`
  - `docs/contracts/2026-03-09-miniapp-addbook-conflict-spec-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
- 约束：不新增冲突错误码；互动冲突与 runId 语义沿用既有锚点。

## 1. 用户流程（主流程 + 异常流程）
### 1.1 主流程
1. 用户进入技师动态列表，按门店浏览内容。
2. 用户对动态点赞或发表评论。
3. 评论内容进入审核，审核通过后公开展示。
4. 用户可从动态跳转技师详情与预约链路。

### 1.2 异常流程
1. 技师不存在/禁用时，动态详情与互动入口直接阻断。
2. 点赞/评论同键异参冲突时，阻断并回显冲突语义。
3. 计数聚合服务超时时，列表可降级返回（计数置零）。
4. 审核回调或工单同步失败时，动态主记录保留并打降级标记。

## 2. 业务规则与状态流转（引用统一状态机）
统一状态机引用：`docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`

### 2.1 业务规则
- 点赞幂等键：`TECH_FEED_LIKE:<memberId>:<postId>:<action>`。
- 评论幂等键：`TECH_FEED_COMMENT:<memberId>:<postId>:<clientToken>`。
- 发布同步幂等键：`TECH_FEED_PUBLISH:<technicianId>:<sourceBizNo>`。
- 同键同参幂等成功；同键异参返回冲突错误，不做静默覆盖。

### 2.2 状态流转
| 对象 | 状态流转 | 说明 |
|---|---|---|
| 动态内容 | `PUBLISHED -> REVIEWING -> APPROVED / REJECTED` | 审核失败不删除主记录，保留申诉入口 |
| 互动行为 | `INIT -> EFFECTIVE`（点赞/评论生效） | 幂等命中不重复计数 |
| 审计联动 | `NORMAL -> DEGRADED`（`TICKET_SYNC_DEGRADED`） | 协同失败不回滚内容主状态 |

## 3. 错误码与降级语义（与现有契约一致）
| Code/Key | 触发场景 | 前端动作 | 降级语义 |
|---|---|---|---|
| `TECHNICIAN_NOT_EXISTS(1030001000)` | 技师不存在 | 阻断跳转和互动入口 | 无降级 |
| `TECHNICIAN_DISABLED(1030001001)` | 技师禁用 | 展示不可互动状态 | 无降级 |
| `USER_NOT_EXISTS(1004001000)` | 互动用户不存在 | 阻断操作并提示登录/状态异常 | 无降级 |
| `BOOKING_ORDER_NOT_EXISTS(1030004000)` | 关联订单不存在（需要关联时） | 阻断关联动作 | 无降级 |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` | 点赞/评论同键异参冲突 | 阻断并提示重试或联系客服 | 无降级 |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)` | runId 审计查询不存在 | 展示可刷新态 | 无降级 |
| `TICKET_SYNC_DEGRADED` | 审核/工单链路降级 | warning 提示，主流程继续 | fail-open |
| `degraded=true` | 计数聚合超时或审核协同失败 | 列表可展示，计数降级为默认值 | 主链路可用优先 |

## 4. 客服/申诉/人工兜底规则
- 客服入口：评论被拒、互动异常、计数异常均提供“申诉/联系客服”。
- 申诉最小字段：`postId`, `commentId`, `technicianId`, `runId`, `sourceBizNo`, `errorCode`, `payRefundId`。
- 人工兜底规则：
  1. 审核回调失败：人工复核后更新审核状态，禁止直接删除审计轨迹。
  2. 计数异常：人工对账后触发重算任务，不修改用户互动事实记录。
  3. 工单降级：先保障内容可见，再补录工单与通知。

## 5. 验收清单
### 5.1 Happy Path
- [ ] 动态列表可分页查看并跳转技师详情。
- [ ] 点赞/评论成功后状态与计数正确更新。
- [ ] 审核通过内容可稳定展示。

### 5.2 业务错误
- [ ] `1030001000/1030001001` 场景下互动入口被阻断。
- [ ] `1030004012` 冲突场景不重复入库。
- [ ] `1004001000` 用户异常时可恢复到登录/重试入口。

### 5.3 Degraded Path
- [ ] 聚合超时时返回 `degraded=true` 且页面可继续浏览。
- [ ] `TICKET_SYNC_DEGRADED` 时内容主链路不回滚。
- [ ] runId 缺失时可刷新重试，不出现白屏。
