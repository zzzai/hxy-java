# Booking Review Dual Channel Manager Notify Handoff（2026-03-21）

## 1. 本批目标
- 一次性收口 booking review 的店长双通道通知场景：差评创建、双通道路由、双通道出站、人工重试、SLA 提醒、后台观测、企微发送器。

## 2. 本批新增真值
1. 同一条差评当前会生成两条独立 outbox：
   - `IN_APP`
   - `WECOM`
2. 路由真值当前固定为：
   - `storeId -> managerAdminUserId`
   - `storeId -> managerWecomUserId`
3. 通道阻断统一落 `BLOCKED_NO_OWNER`，具体原因通过 `lastErrorMsg` 区分：
   - `NO_OWNER`
   - `NO_APP_ACCOUNT`
   - `NO_WECOM_ACCOUNT`
   - `CHANNEL_DISABLED`
4. 企微 sender 当前走共享机器人发送端，配置键固定为：
   - `hxy.booking.review.notify.wecom.enabled`
   - `hxy.booking.review.notify.wecom.webhook-url`
   - `hxy.booking.review.notify.wecom.app-name`
5. 店长待办 SLA 提醒当前继续走双通道 outbox，提醒类型固定为：
   - `MANAGER_CLAIM_TIMEOUT`
   - `MANAGER_FIRST_ACTION_TIMEOUT`
   - `MANAGER_CLOSE_TIMEOUT`

## 3. 本批涉及文件
- `docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-design.md`
- `docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-implementation-plan.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`
- `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- `docs/plans/2026-03-21-booking-review-manager-notify-routing-design.md`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewWecomRobotSenderImpl.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/job/BookingReviewManagerTodoSlaReminderJob.java`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/managerRouting/index.vue`

## 4. 当前结论

| 维度 | 当前结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 当前是否可开发 | `Yes` |
| 当前是否可放量 | `No` |
| 工程语义 | `Dual-channel manager notify implemented, release evidence pending` |
| Release Decision | `No-Go` |

## 5. 联调注意点
1. App / 企微必须一通道一条 outbox，不能合并成一条总状态。
2. `BLOCKED_NO_OWNER` 不等于发送失败；要结合 `lastErrorMsg` 区分缺路由、缺 App 账号、缺企微账号和通道关闭。
3. `SENT` 只表示系统已派发成功，不等于店长已读或问题已闭环。
4. 当前没有发布级企微送达样本、灰度证据和 runtime gate，不能把本批改成 `Can Release=Yes`。
5. 奖励、补偿、区域负责人升级不在本批范围内，后续若做，需要在现有 outbox 模型上新增通道或角色，不能回退当前审计模型。
