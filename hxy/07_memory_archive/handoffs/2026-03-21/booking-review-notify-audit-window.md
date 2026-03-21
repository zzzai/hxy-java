# Booking Review Notify Audit Window Handoff（2026-03-21）

## 1. 本批目标
- 完成 booking review notify outbox 的审计增强，让运营能直接看清最近动作、执行人和原因。

## 2. 本批新增真值
1. notify outbox 分页查询当前支持 `lastActionCode` 过滤，可直接只看 `MANUAL_RETRY`。
2. notify outbox 返回值当前补齐：
   - `actionLabel`
   - `actionOperatorLabel`
   - `actionReason`
3. `MANUAL_RETRY` 当前按 `lastActionBizNo=ADMIN#{id}/OUTBOX#{id}` 解析为“管理员#{id}”。
4. `manual-retry:{reason}` 当前按 `actionReason` 直接透出，避免只看到技术字段。
5. 这批增强只属于 admin-only 审计可读性，不改变派发语义，不代表消息通道已闭环。

## 3. 本批涉及文件
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxController.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxPageReqVO.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxRespVO.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewNotifyOutboxMapper.java`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue`
- `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`

## 4. 当前结论

| 维度 | 当前结论 |
|---|---|
| 当前是否可开发 | `Yes` |
| 当前是否可放量 | `No` |
| 变更语义 | `Audit Only` |
| Release Decision | `No-Go` |

## 5. 对后续开发的注意点
1. 不得把“已能看见人工重试”写成“通知链路已闭环”。
2. `BLOCKED_NO_OWNER` 仍然是主数据阻断，不允许在本页直接改接收人越权补发。
3. 后续接企微双通道时，应继续保持“每通道一条出站记录”的审计模型，不要把 App / 企微糊成一条状态。
4. 若后续扩展 `STORE_MANAGER_APP / STORE_MANAGER_WECOM`，动作审计字段仍应复用当前三元组：
   - `actionLabel`
   - `actionOperatorLabel`
   - `actionReason`
