# booking-refund-replay-v2-window-a

## 1. 目标
- P1 退款回调补偿运营化 V2（后端收口）：批量重放支持 dry-run、重放结果可追溯、自动补偿任务可审计。

## 2. 关键改动
- `POST /booking/refund-notify-log/replay`：支持 `id + ids` 批量、`dryRun` 预演（默认 false），兼容旧单条 `id`。
- replay 返回统一结构：`successCount/skipCount/failCount + details[id,resultStatus,resultCode,resultMsg,payRefundId,orderId]`。
- 台账审计增强：`hxy_booking_refund_notify_log` 新增 `last_replay_operator/last_replay_time/last_replay_result/last_replay_remark`。
- 幂等策略：
  - 已成功记录重放 -> `SKIP`（不更新 booking 订单状态）；
  - 失败记录重放成功 -> 状态改 `success`，`retryCount` 递增，`nextRetryTime` 清空。
- 自动补偿任务：新增 `BookingRefundNotifyReplayJob`，扫描 `status=fail && nextRetryTime<=now` 批量重放，输出 `runId + 扫描/成功/跳过/失败` 摘要。
- 四账衔接：重放成功后触发 `runReconcile` 刷新当日四账；下游异常 fail-open，仅记 warning 与 replay 审计备注。

## 3. SQL
- `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-06-hxy-booking-refund-notify-log-replay-v2.sql`

## 4. 回归验证
- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-booking -am -Dtest=BookingRefundNotifyLogControllerTest,BookingRefundNotifyLogServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-booking,yudao-module-mall/yudao-module-trade -am -Dtest=BookingOrderServiceImplTest,BookingRefundNotifyLogControllerTest,BookingRefundNotifyLogServiceTest,FourAccountReconcileServiceImplTest,FourAccountReconcileControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 5. 联调注意点（给 B/C）
- UI 侧 replay 请求建议统一走 `ids`，保留 `id` 仅做兼容。
- `dryRun=true` 仅更新 replay 审计字段，不改 `booking_order`、不改台账业务状态。
- 结果码建议按 `resultStatus` 解读：`SUCCESS/SKIP/FAIL`；`resultCode/resultMsg` 作为展示补充。
- 自动任务与手工 replay 都会写 `lastReplay*`，可作为运营追溯主口径。
