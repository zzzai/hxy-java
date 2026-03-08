# MiniApp Technician Feed Contract v1 (2026-03-09)

## 1. 目标与范围
- 目标：定义“技师动态广场”后端契约，覆盖动态列表、互动、降级与审计。
- 范围：与现有技师查询接口协同，不改动当前业务代码。
- 原则：向后兼容、幂等可重试、异常可检索。

## 2. API 列表（路由、方法、请求、响应）

| 路由 | 方法 | 状态 | 请求（关键字段） | 响应（关键字段） |
|---|---|---|---|---|
| `/booking/technician/list` | `GET` | 已有 | `storeId` | `list[]:{id,name,avatar,level,storeId}` |
| `/booking/technician/get` | `GET` | 已有 | `id` | `{id,name,avatar,intro,skillTags,storeId}` |
| `/booking/technician/feed/page` | `GET` | 规划 | `storeId`, `pageNo`, `pageSize`, `lastId?` | `list[]:{postId,technicianId,content,media[],likeCount,commentCount,publishTime,degraded}`, `hasMore` |
| `/booking/technician/feed/like` | `POST` | 规划 | `postId`, `action(1=like,0=cancel)`, `clientToken` | `postId`, `liked`, `likeCount`, `idempotentHit` |
| `/booking/technician/feed/comment/create` | `POST` | 规划 | `postId`, `content`, `clientToken` | `commentId`, `postId`, `status`, `degraded` |

## 3. 幂等键与冲突策略
- 点赞幂等键：`TECH_FEED_LIKE:<memberId>:<postId>:<action>`
- 评论幂等键：`TECH_FEED_COMMENT:<memberId>:<postId>:<clientToken>`
- 动态发布/同步幂等键：`TECH_FEED_PUBLISH:<technicianId>:<sourceBizNo>`

冲突策略：
- 同键同参：幂等成功，返回首次结果。
- 同键异参：返回冲突错误（复用 `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` 语义）。
- 技师不存在/禁用：直接业务失败，不做降级成功。

## 4. 错误码清单（稳定、可检索）

| 锚点 | 编码 | 用途 | 兼容策略 |
|---|---:|---|---|
| `TECHNICIAN_NOT_EXISTS` | `1030001000` | 技师不存在 | 保持不变 |
| `TECHNICIAN_DISABLED` | `1030001001` | 技师已禁用 | 保持不变 |
| `USER_NOT_EXISTS` | `1004001000` | 互动用户不存在 | 保持不变 |
| `BOOKING_ORDER_NOT_EXISTS` | `1030004000` | 动态关联订单不存在（如需关联） | 保持不变 |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT` | `1030004012` | 互动同键异参冲突 | 未来可迁移到 feed 专属冲突码并保留映射 |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` | `1030004016` | runId 查询不存在 | 保持不变 |
| `TICKET_SYNC_DEGRADED` | warning tag | 内容审核/工单同步降级 | 仅 warning，不阻断主链路 |

## 5. fail-open / degrade 语义
- 计数聚合（点赞/评论计数）超时：降级返回 `likeCount/commentCount=0` + `degraded=true`。
- 内容审核回调失败：动态先标记 `REVIEWING`，主发布链路不回滚，异步重试。
- 工单系统不可用：返回成功但打 `TICKET_SYNC_DEGRADED`。

## 6. 审计字段要求
- 所有关键日志强制包含：`runId`, `orderId`, `payRefundId`, `sourceBizNo`, `errorCode`。
- 推荐 `sourceBizNo`：
  - 点赞：`TECH_FEED_LIKE:<postId>:<memberId>`
  - 评论：`TECH_FEED_COMMENT:<commentId|PENDING>`
  - 审核：`TECH_FEED_REVIEW:<postId>`
- 非退款场景 `payRefundId` 填 `0`，不可缺失。

## 7. 与现有契约兼容说明（向后兼容）
- 既有 `/booking/technician/list`、`/booking/technician/get` 契约完全不变。
- feed 新接口均为增量新增；旧端不调用时不受影响。
- 响应新增字段（如 `degraded`）均为可选字段，旧端可直接忽略。
