# MiniApp 财务运营退款回调重放 PRD v1（2026-03-12）

## 0. 文档定位
- 目标：把 booking 退款回调日志、人工重放、重放运行日志、工单补同步流程收口为后台财务运营可执行产品文档。
- 真实代码基线：
  - 页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue`
  - API：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/refundNotifyLog.ts`
  - Controller：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogController.java`
- 约束：只认当前日志页、重放能力、运行日志能力，不扩写不存在的退款工单大盘或客服工单页。

## 1. 产品目标
1. 支持按订单、退款单、错误码、状态筛选退款回调日志。
2. 支持单条或批量人工重放。
3. 支持按“到期待处理”批量重放。
4. 支持查看每次重放的运行日志、明细、汇总和票据同步结果。
5. 让值班人员能够区分：真实成功、跳过、失败、部分失败、warning 样本。

## 2. 页面真值与核心模块
- 页面文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue`
- 当前页面承接 4 类能力：
  1. 退款回调原始日志查询
  2. 人工重放与到期重放
  3. 重放运行日志查询
  4. 重放后工单同步

## 3. 接口真值

| 动作 | Method + Path | 说明 |
|---|---|---|
| 分页查回调日志 | `GET /booking/refund-notify-log/page` | 查询原始退款回调日志 |
| 人工重放 | `POST /booking/refund-notify-log/replay` | 单条或批量重放 |
| 到期重放 | `POST /booking/refund-notify-log/replay-due` | 按 limit 扫描到期日志并重放 |
| 查重放运行日志页 | `GET /booking/refund-notify-log/replay-run-log/page` | 查询重放批次 |
| 查重放明细页 | `GET /booking/refund-notify-log/replay-run-log/detail/page` | 查询批次下明细 |
| 查重放明细详情 | `GET /booking/refund-notify-log/replay-run-log/detail/get` | 查询单条明细 |
| 查重放批次详情 | `GET /booking/refund-notify-log/replay-run-log/get` | 查询批次详情 |
| 查重放批次汇总 | `GET /booking/refund-notify-log/replay-run-log/summary` | 查询批次汇总与 Top fail / warning |
| 重放批次同步工单 | `POST /booking/refund-notify-log/replay-run-log/sync-tickets` | 同步票据或工单 |

## 4. 关键字段真值

### 4.1 原始回调日志
- `id`
- `orderId`
- `merchantRefundId`
- `payRefundId`
- `runId`
- `resultCode`
- `warningTag`
- `ticketSyncStatus`
- `status`
- `errorCode`
- `errorMsg`
- `rawPayload`
- `retryCount`
- `nextRetryTime`
- `lastReplayOperator`
- `lastReplayTime`
- `lastReplayResult`
- `lastReplayRemark`

### 4.2 重放批次汇总
- `runId`
- `triggerSource`
- `operator`
- `dryRun`
- `scannedCount`
- `successCount`
- `skipCount`
- `failCount`
- `warningCount`
- `status`
- `topFailCodes`
- `topWarningTags`

### 4.3 重放明细
- `notifyLogId`
- `orderId`
- `merchantRefundId`
- `payRefundId`
- `resultStatus`
- `resultCode`
- `warningTag`
- `ticketSyncStatus`
- `ticketId`
- `ticketSyncTime`
- `errorMsg`

## 5. 状态与结果口径

### 5.1 原始日志状态
- `success`
- `fail`
- `pending`

### 5.2 重放结果状态
- 明细级：`SUCCESS` / `SKIP` / `FAIL`
- 批次级：`RUNNING` / `SUCCESS` / `PARTIAL_FAIL` / `FAIL`

规则：
- `SKIP` 不等于成功，必须能解释跳过原因。
- `PARTIAL_FAIL` 必须明确失败笔数与失败原因，不能按成功批次处理。
- `warningTag` 需要保留，不允许因重放成功而丢失 warning 样本标签。

## 6. 产品操作规则
1. 人工重放必须支持 `dryRun`。
2. 批量重放必须返回：
   - attempted / attemptedCount
   - scannedCount
   - successCount
   - skipCount
   - failCount
3. 运行日志必须可回查到具体 `runId`。
4. 批次工单同步必须能返回：
   - attempted
   - success / skip / fail
   - failedIds
5. 原始回调 `rawPayload` 允许查看，但不能作为客服外发内容。

## 7. 值班与风控边界
- 财务运营：负责重放、查看批次结果、跟踪失败样本。
- 客服主管：仅消费业务解释结果，不直接解释原始 technical payload。
- 后端：保证 `runId`、`warningTag`、`ticketSyncStatus` 一致可追。
- 前端后台：必须保留 warning / fail / partial-fail 区分，禁止“成功态吞没失败明细”。

## 8. 验收标准
1. 可以按 `orderId`、`merchantRefundId`、`payRefundId`、`errorCode` 查到日志。
2. 可以执行单条或批量重放，且返回非空运行结果对象。
3. 可以按 `runId` 查到批次页、明细页、汇总页。
4. 可以对批次执行 `sync-tickets`，并看到成功 / 跳过 / 失败结果。
5. 任一 warning / fail 样本都不能在 UI 中被伪装成“全部成功”。

## 9. 非目标
- 不定义支付网关自身的退款协议。
- 不定义外部票据系统页面。
- 不把“可重放”误写成“必定可恢复”。
