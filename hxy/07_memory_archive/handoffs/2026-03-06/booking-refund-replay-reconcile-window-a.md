# Window A Handoff - booking 退款回调补偿与对账自动闭环

## 1. 目标与范围

- 分支：`feat/ui-four-account-reconcile-ops`
- 范围：仅 booking 后端 + SQL + 治理文档
- 目标：退款回调失败可追溯、可重放、可自动补偿修复

## 2. 关键交付

1. 新增台账表 `hxy_booking_refund_notify_log`
   - SQL：`ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-06-hxy-booking-refund-notify-log.sql`
   - 字段：`orderId/merchantRefundId/payRefundId/status/errorCode/errorMsg/rawPayload/retryCount/nextRetryTime`
2. 回调全量落账
   - `POST /booking/order/update-refunded` 成功与失败都写台账
   - 失败场景不吞错，保留原异常语义
3. 管理端接口
   - `GET /booking/refund-notify-log/page`
   - `POST /booking/refund-notify-log/replay`（仅 `fail` 记录可重放）
4. 自动补偿任务
   - Job：`BookingRefundReconcileRepairJob`
   - 逻辑：按 `pay_refund.SUCCESS` 扫描“支付已退款但 booking 未退款”并修复
   - 单条失败 fail-open，写失败台账，不阻断批次

## 3. 回放规则

- 仅允许 `status=fail` 的日志重放
- 重放成功：
  - 调用 `BookingOrderService.updateOrderRefunded`
  - 二次核验 `booking_order` 已处于 `REFUNDED` 且 `payRefundId/refundTime` 已回写
  - 日志转 `success`，`retryCount+1`，清空 `nextRetryTime/error*`
- 重放失败：
  - 日志保持 `fail`，`retryCount+1`
  - 按退避写入 `nextRetryTime`
  - 回写 `errorCode/errorMsg`

## 4. 主要文件

- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingOrderController.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogController.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingRefundNotifyLogService.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingRefundNotifyLogServiceImpl.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/job/BookingRefundReconcileRepairJob.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingRefundNotifyLogDO.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingRefundNotifyLogMapper.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingRefundReconcileQueryMapper.java`

## 5. 测试与验证

已通过：

- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-booking -am -Dtest=AppBookingOrderControllerTest,BookingRefundNotifyLogControllerTest,BookingRefundNotifyLogServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-booking -am -Dtest=BookingOrderServiceImplTest,FourAccountReconcileServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

待窗口A收尾执行：

- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

## 6. 联调注意点（给 B/C）

- 给 B（overlay）：
  - 回调台账分页状态固定小写：`success/fail`
  - `replay` 成功返回 `true`，失败透传原业务错误码
- 给 C（门禁/CI）：
  - 若加 booking 退款回调门禁，建议检查点：台账表存在、page/replay 路由存在、回调失败可落 `fail`
