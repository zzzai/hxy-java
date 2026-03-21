# Booking Review Manager Notify Routing Design（2026-03-21）

## 1. 背景
- booking review 当前已经具备：
  - 差评提交
  - admin-only 店长待办治理
  - 台账 / 详情 / 看板 / 历史治理扫描页
  - notify outbox、manager routing 只读核查、人工重试
- 2026-03-21 当前设计已经从“单通道 App 通知”升级为“双通道店长通知”：
  - 店长端 App
  - 店长企微
- 固定业务场景：
  - `1000` 家门店
  - 默认 `1 店 1 店长`
  - 集团共用一个企微发送端
  - 同一条差评按 `storeId` 命中店长后，再拆成两条独立出站记录

## 2. 目标
- 固定 booking review 店长通知的路由真值与分发模型。
- 保证差评提交主链路与通知链路分离。
- 让每个通道拥有独立状态、失败原因、重试和审计。
- 在发布证据未闭环前，仍保持 `Can Develop / Cannot Release`。

## 3. 核心结论
1. 当前采用的不是“单记录多子通道”方案，而是“一通道一条 outbox”方案。
2. 路由真值固定为：
   - `storeId -> managerAdminUserId`
   - `storeId -> managerWecomUserId`
3. 同一条差评会至少生成两条 outbox：
   - `IN_APP`
   - `WECOM`
4. 通道阻断统一进入 `BLOCKED_NO_OWNER`，再由 `lastErrorMsg` 区分：
   - `NO_OWNER`
   - `NO_APP_ACCOUNT`
   - `NO_WECOM_ACCOUNT`
   - `CHANNEL_DISABLED`

## 4. 路由模型
- 归属关系至少表达：
  - `storeId`
  - `managerAdminUserId`
  - `managerWecomUserId`
  - `bindingStatus`
  - `effectiveTime`
  - `expireTime`
  - `source`
  - `lastVerifiedTime`
- 在上述真值未闭环前：
  - 可以生成 outbox
  - 可以进入工程派发
  - 但不允许宣称“发布级消息闭环已经成立”

## 5. 出站模型
- 最小字段：
  - `bizType`
  - `bizId`
  - `storeId`
  - `receiverRole`
  - `receiverUserId`
  - `receiverAccount`
  - `notifyType`
  - `channel`
  - `status`
  - `retryCount`
  - `nextRetryTime`
  - `sentTime`
  - `lastErrorMsg`
  - `idempotencyKey`
  - `payloadSnapshot`
  - `lastActionCode / lastActionBizNo / lastActionTime`
- 推荐幂等键：
  - `booking_review:{notifyType}:{reviewId}:{channel}:{receiver}`

## 6. 派发策略
1. `IN_APP` 通道继续复用 `NotifySendService.sendSingleNotifyToAdmin(...)`。
2. `WECOM` 通道走共享企微机器人 sender。
3. 企微 sender 依赖：
   - `hxy.booking.review.notify.wecom.enabled`
   - `hxy.booking.review.notify.wecom.webhook-url`
   - `hxy.booking.review.notify.wecom.app-name`
4. 任一通道失败，不影响另一通道状态。
5. 通知链路失败，不影响用户评价提交成功。

## 7. SLA 提醒
1. 当前继续复用同一套 outbox / dispatch 机制，不新增旁路线。
2. 固定三类提醒：
   - `MANAGER_CLAIM_TIMEOUT`
   - `MANAGER_FIRST_ACTION_TIMEOUT`
   - `MANAGER_CLOSE_TIMEOUT`
3. 每种提醒都按“评价 + 通道 + 接收账号”幂等。

## 8. 后台观测要求
1. notify outbox 台账必须能看见：
   - 通道
   - 接收账号
   - 诊断结论
   - 修复建议
   - 最近动作
2. 详情页必须能看见：
   - 同一条评价下 App / 企微两条记录
   - 每条的状态、错误原因与修复建议
3. routing 页必须能看见：
   - App / 企微各自的绑定状态和 repairHint

## 9. No-Go
1. 不得把 `contactMobile` 当自动通知目标。
2. 不得把 `managerClaimedByUserId` 当门店店长账号。
3. 不得把 outbox 已写出写成“店长已收到通知”。
4. 不得在没有发布级样本前宣称“自动通知已上线”。
5. 不得扩展到客服负责人、区域负责人升级链路。
6. 不得把本专题写成 release-ready。

## 10. 单一真值引用
- `docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-design.md`
- `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
