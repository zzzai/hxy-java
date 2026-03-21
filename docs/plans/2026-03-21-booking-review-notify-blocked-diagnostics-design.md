# Booking Review Notify Blocked Diagnostics Design（2026-03-21）

## 1. 背景
- 当前 booking review notify outbox 已具备：
  - 差评提交后写通知意图
  - admin 台账查询
  - 失败记录人工重试
  - `BLOCKED_NO_OWNER` 阻断态
- 当前主要问题不是“有没有状态”，而是运营看不出：
  - 为什么这条不能重试
  - 缺的是哪类主数据
  - 下一步该找谁修
- 当前业务场景已冻结：
  - 1000 家门店
  - 默认一店一店长
  - 双通道：店长端 App + 店长企微
  - 企微发送端全集团共用

## 2. 本轮目标
- 把通知阻断态从“只看见一个状态码”升级为“运营可读的诊断结论”。
- 让运营能快速区分：
  - 发送失败，可人工重试
  - 路由缺失，不能重试
  - App 账号缺失
  - 企微账号缺失
  - 通道关闭
- 保持本轮不改数据库结构，不提前实现 P1-2 的路由真值闭环。

## 3. 能力边界

### 3.1 本轮要做
1. 后端对 notify outbox response 增加诊断字段
2. 前端台账页展示“诊断结论 / 修复建议”
3. 前端详情页展示标准化阻断说明
4. 增加快捷筛选入口：只看阻断 / 只看失败 / 只看待派发
5. 按诊断结果控制是否可手工重试

### 3.2 本轮不做
1. 不改 `booking_review_notify_outbox` 表结构
2. 不改 routing 表结构
3. 不新增 `managerWecomUserId`
4. 不真正接企微发送器
5. 不改 release 判断

## 4. 方案比较

### 方案 A：只改前端文案
- 做法：继续只用 `status + lastErrorMsg`，前端硬编码更多提示。
- 优点：改动最小。
- 缺点：
  - 诊断逻辑散落在页面
  - API 不可复用
  - 详情页和台账页容易漂移
- 结论：不采用。

### 方案 B：后端标准化诊断字段，前端消费
- 做法：
  - controller 在 response 层统一派生诊断字段
  - 页面只展示标准化字段
- 优点：
  - 不改表结构
  - 诊断逻辑集中
  - 为 P1-2 / P1-3 继续扩展保留接口
- 缺点：
  - 需要扩展 response VO 和前端类型
- 结论：采用。

### 方案 C：直接新增数据库字段存 diagnosis
- 做法：在 outbox 表中新增诊断字段并持久化。
- 优点：查询最直接。
- 缺点：
  - 当前时机过重
  - 诊断规则还在演进期
- 结论：暂不采用。

## 5. 核心设计

### 5.1 标准化诊断字段
`BookingReviewNotifyOutboxRespVO` 新增：
- `diagnosticCode`
- `diagnosticLabel`
- `diagnosticDetail`
- `repairHint`
- `manualRetryAllowed`

### 5.2 第一版诊断码
- `READY_TO_DISPATCH`
- `SEND_SUCCESS`
- `ACTIONABLE_FAILED`
- `BLOCKED_NO_MANAGER_ROUTE`
- `BLOCKED_NO_APP_ACCOUNT`
- `BLOCKED_NO_WECOM_ACCOUNT`
- `BLOCKED_CHANNEL_DISABLED`
- `BLOCKED_UNKNOWN`

### 5.3 当前映射规则
- `status = PENDING` -> `READY_TO_DISPATCH`
- `status = SENT` -> `SEND_SUCCESS`
- `status = FAILED` -> `ACTIONABLE_FAILED`
- `status = BLOCKED_NO_OWNER` 且 `lastErrorMsg` 包含：
  - `NO_OWNER` -> `BLOCKED_NO_MANAGER_ROUTE`
  - `NO_APP_ACCOUNT` -> `BLOCKED_NO_APP_ACCOUNT`
  - `NO_WECOM_ACCOUNT` -> `BLOCKED_NO_WECOM_ACCOUNT`
  - `CHANNEL_DISABLED` -> `BLOCKED_CHANNEL_DISABLED`
  - 其他 -> `BLOCKED_UNKNOWN`

说明：
- 虽然当前主链路主要还是 `NO_OWNER`，但接口先对未来 App / 企微阻断做兼容。

### 5.4 手工重试规则
- 只有 `ACTIONABLE_FAILED` 返回 `manualRetryAllowed = true`
- 所有 `BLOCKED_*` 返回 `false`

## 6. 前端设计

### 6.1 台账页
- 顶部新增快捷按钮：
  - 只看阻断
  - 只看失败
  - 只看待派发
- 表格新增：
  - `诊断结论`
  - `修复建议`
- `重试` 按钮不再直接按 `status === FAILED` 判断，改为按 `manualRetryAllowed`

### 6.2 详情页
- “最新通知状态”中新增：
  - 诊断结论
  - 修复建议
- 子表中错误原因改成优先展示标准化诊断说明

## 7. 测试策略
1. controller test
   - `BLOCKED_NO_OWNER:NO_OWNER` 能映射成 `BLOCKED_NO_MANAGER_ROUTE`
   - `FAILED` 能映射成 `ACTIONABLE_FAILED`
2. node test
   - 台账页有“只看阻断 / 只看失败 / 只看待派发”
   - 页面展示 `diagnosticLabel / repairHint`
   - 重试按钮依赖 `manualRetryAllowed`

## 8. No-Go
1. 不得把诊断增强写成路由真值已闭环
2. 不得把 `BLOCKED_*` 写成发送失败
3. 不得把 `repairHint` 写成已自动修复
4. 不得在本轮提前落地企微发送

## 9. 与后续阶段关系
- P1-1：先解决“看得明白”
- P1-2：再解决“路由配得出来”
- P1-3：最后解决“链路审得清楚”

## 10. 单一真值引用
- `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- `docs/plans/2026-03-21-booking-review-manager-notify-routing-design.md`
- `docs/plans/2026-03-21-booking-review-notify-manual-retry-design.md`
