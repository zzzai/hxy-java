# Booking Review Notify Manual Retry Design（2026-03-21）

## 1. 背景
- 当前 `booking review notify outbox` 已具备：
  - 差评提交后写出站记录
  - 后台台账查询
  - 异步派发 `PENDING -> SENT / FAILED`
  - 无门店店长账号时进入 `BLOCKED_NO_OWNER`
- 当前缺口是：
  - 后台无法对 `FAILED` 记录执行人工重试
  - 运营只能看失败，不能把记录重新入队
- 本轮已确认边界：
  - 只做 `FAILED` 的后台手工重试
  - `BLOCKED_NO_OWNER` 继续只读展示，不提供重试按钮
  - 不引入 `contactMobile` 自动兜底
  - 不扩展客服/区域负责人升级链
  - 结论继续保持 `Can Develop / Cannot Release`

## 2. 目标
- 给后台通知出站台账补齐最小手工重试闭环。
- 明确“手工重试”语义是重新入队，不是通知已送达。
- 保持现有状态机和派发任务模型，不额外引入批处理流程复杂度。

## 3. 能力边界

### 3.1 本轮要做
1. 新增后台接口 `POST /booking/review/notify-outbox/retry`
2. 支持运营填写重试原因，默认 `manual-retry`
3. 仅对 `FAILED` 行展示 `重试` 按钮
4. 成功后将记录改回 `PENDING` 并立即可被派发任务消费
5. 补齐 controller / service / node 页面测试

### 3.2 本轮明确不做
1. 不支持 `BLOCKED_NO_OWNER` 强制重试
2. 不支持 `SENT / PENDING` 人工重试
3. 不支持批量 UI 重试
4. 不做接收人改派
5. 不做短信、电话、客服升级链

## 4. 方案比较

### 方案 A：直接在页面上重新派发通知
- 做法：点击按钮后前端直接调用派发逻辑。
- 优点：链路看起来最短。
- 缺点：
  - 绕过 outbox 状态机
  - 与现有 dispatch job 模型冲突
  - 不利于审计和并发保护
- 结论：否决。

### 方案 B：行级重试，把失败记录重新入队
- 做法：
  - 前端只对单条 `FAILED` 记录展示 `重试`
  - 后端接口按 `ids[]` 设计，当前只传单个 id
  - 服务端校验通过后把状态改回 `PENDING`
- 优点：
  - 复用既有 outbox + job 模型
  - 后端接口天然可扩展到以后批量重试
  - 审计字段一致，范围最小
- 缺点：
  - 本轮 UI 仍只支持单条重试
- 结论：采用。

### 方案 C：自动扫描全部失败记录并重跑
- 做法：不加按钮，改由任务自动扫描 `FAILED` 统一重试。
- 优点：页面最简单。
- 缺点：
  - 无法表达“人工确认后再重试”
  - 容易把配置错误、账号错误、逻辑错误无限循环
- 结论：否决。

## 5. 核心设计

### 5.1 后端接口
- 路径：`POST /booking/review/notify-outbox/retry`
- 权限：`booking:review:update`
- Request VO：
  - `ids: List<Long>` 必填
  - `reason?: String` 可选，最大 255

### 5.2 状态校验规则
- 只允许 `FAILED` 重试
- `BLOCKED_NO_OWNER` 返回状态非法
- `SENT / PENDING` 返回状态非法
- 记录不存在时返回不存在错误

### 5.3 回写规则
对每条通过校验的记录执行：
- `status -> PENDING`
- `nextRetryTime -> now`
- `sentTime -> null`
- `lastErrorMsg -> manual-retry:<reason>`
- `lastActionCode -> MANUAL_RETRY`
- `lastActionBizNo -> 手工重试审计号`
- `lastActionTime -> now`

说明：
- 本轮不重置 `retryCount`
- 本轮不直接发送通知，只是重新入队

### 5.4 新错误码
- `BOOKING_REVIEW_NOTIFY_OUTBOX_NOT_EXISTS`
- `BOOKING_REVIEW_NOTIFY_OUTBOX_STATUS_INVALID`

## 6. 前端交互
- 页面：`mall/booking/review/notifyOutbox/index.vue`
- 交互规则：
  - 只有 `row.status === 'FAILED'` 显示 `重试`
  - 点击后弹出原因输入框
  - 默认值固定 `manual-retry`
  - 成功提示 `已重新入队`
  - 成功后刷新列表
- 明确不展示：
  - `BLOCKED_NO_OWNER` 的重试按钮
  - `SENT / PENDING` 的重试按钮

## 7. 测试策略
1. controller test
   - retry 接口透传 `ids` 和 `reason`
2. service test
   - `FAILED -> PENDING`
   - `SENT` 拒绝
   - `BLOCKED_NO_OWNER` 拒绝
   - 不存在记录报错
3. node test
   - API 暴露 retry 方法
   - 失败态显示重试按钮
   - 非失败态不显示
   - 页面包含“已重新入队”成功提示

## 8. No-Go
1. 不得把手工重试写成“已通知成功”
2. 不得给 `BLOCKED_NO_OWNER` 提供重试入口
3. 不得引入 `contactMobile` 兜底通知
4. 不得把本专题写成 release-ready
5. 不得扩展到客服负责人或区域负责人升级链

## 9. 发布口径
- 当前能力只能写成：
  - `Doc Closed`
  - `Can Develop`
  - `Cannot Release`
- 原因：
  - 手工重试只是后台运营补救能力
  - 不等于通知链路已有稳定发布证据

## 10. 单一真值引用
- `docs/plans/2026-03-21-booking-review-manager-notify-routing-design.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
