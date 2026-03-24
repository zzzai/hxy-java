# Admin Finance Ops Core Contract v1 (2026-03-24)

## 1. 目标与真值来源
- 目标：为 `BO-001 四账对账`、`BO-002 退款回调日志 / 重放 / 重放运行日志`、`BO-003 技师提成结算 / 审核 / 驳回 / 打款 / 通知补偿` 建立独立后台 contract。
- 本文只认真实后台页面、真实前端 API 文件、真实 controller 注解路径，以及已落盘正式 PRD。
- 本文不覆盖 `BO-004 技师提成明细 / 计提管理`；`BO-004` 继续只认它自己的独立 contract / runbook。
- 真值输入：
  - PRD：
    - `docs/products/miniapp/2026-03-12-miniapp-finance-ops-four-account-reconcile-prd-v1.md`
    - `docs/products/miniapp/2026-03-12-miniapp-finance-ops-refund-notify-replay-prd-v1.md`
    - `docs/products/miniapp/2026-03-12-miniapp-finance-ops-technician-commission-settlement-prd-v1.md`
  - 页面：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue`
  - API：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/fourAccountReconcile.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/refundNotifyLog.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commissionSettlement.ts`
  - Controller：
    - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/FourAccountReconcileController.java`
    - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogController.java`
    - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/TechnicianCommissionSettlementController.java`

## 2. 能力绑定矩阵

| 能力 | 后台页面入口 | 前端 API | 后端 controller | 当前真值 |
|---|---|---|---|---|
| `BO-001` 四账对账 | `mall/booking/fourAccountReconcile/index` | `fourAccountReconcile.ts` | `FourAccountReconcileController` | 页面 / API / controller 已闭环，属于 `ACTIVE_ADMIN` |
| `BO-002` 退款回调日志 / 重放 / 运行日志 | `mall/booking/refundNotifyLog/index` | `refundNotifyLog.ts` | `BookingRefundNotifyLogController` | 页面 / API / controller 已闭环，属于 `ACTIVE_ADMIN` |
| `BO-003` 技师提成结算 / 通知补偿 | `mall/booking/commission-settlement/index`; `mall/booking/commission-settlement/outbox/index` | `commissionSettlement.ts` | `TechnicianCommissionSettlementController` | 页面 / API / controller 已闭环，属于 `ACTIVE_ADMIN` |

## 3. Canonical Interface Matrix

| 能力 | method + path | 关键 request/query/body 真值 | 关键 response 真值 | 合法空态 / 观察态 | 禁止误写 |
|---|---|---|---|---|---|
| `BO-001` 对账台账 | `GET /booking/four-account-reconcile/page` | `reconcileNo`,`bizDate[]`,`status`,`source`,`issueCode` | `PageResult<FourAccountReconcileRespVO>` | 空分页合法 | 不能把空分页写成对账失败 |
| `BO-001` 对账详情 | `GET /booking/four-account-reconcile/get` | `id` | 单条 `FourAccountReconcileRespVO` | 无问题单也合法 | 不能把 `issueCount=0` 写成降级 |
| `BO-001` 对账运行 | `POST /booking/four-account-reconcile/run` | `bizDate`,`source` | `Long runId` | 返回 runId 只表示任务受理 | 不能把受理写成全链路已修复 |
| `BO-001` 汇总 / 退款审计汇总 | `GET /booking/four-account-reconcile/summary`; `GET /booking/four-account-reconcile/refund-audit-summary` | `bizDate[]`,`status`,`relatedTicketLinked` 等 | 汇总计数、金额聚合、`ticketSummaryDegraded` | 全 0 汇总合法 | 不能把汇总 0 写成系统异常 |
| `BO-001` 退款佣金审计 / 同步工单 | `GET /booking/four-account-reconcile/refund-commission-audit-page`; `POST /booking/four-account-reconcile/refund-commission-audit/sync-tickets` | 审计过滤项、`limit` | 分页结果；同步返回 `attemptedCount/successCount/failedCount` | 没有 mismatch 合法 | 不能把工单同步成功写成退款已闭环 |
| `BO-002` 回调日志列表 | `GET /booking/refund-notify-log/page` | `orderId`,`merchantRefundId`,`payRefundId`,`status`,`errorCode`,`createTime[]` | `PageResult<BookingRefundNotifyLogRespVO>` | 空分页合法 | 不能把无日志写成成功送达 |
| `BO-002` 单条重放 / 到期重放 | `POST /booking/refund-notify-log/replay`; `POST /booking/refund-notify-log/replay-due` | `ids[]/id`,`dryRun`,`limit` | `BookingRefundNotifyLogReplayRespVO` | `dryRun=true` 只代表演练 | 不能把 `dryRun` 样本写成真实修复 |
| `BO-002` 运行日志 / 明细 / 汇总 | `GET /booking/refund-notify-log/replay-run-log/page`; `.../detail/page`; `.../detail/get`; `.../get`; `.../summary` | `runId`,`status`,`operator`,`hasWarning`,`timeRange[]` | 运行日志、明细、top fail/warning 聚合 | 明细空集合法 | 不能把 run log 存在写成线上完全闭环 |
| `BO-002` 运行日志工单同步 | `POST /booking/refund-notify-log/replay-run-log/sync-tickets` | `runId`,`dryRun`,`forceResync`,`onlyFail` | `attempted/success/skip/fail` | `skip` 合法 | 不能把同步工单等价成退款回调已成功回补 |
| `BO-003` 结算单主链 | `POST /booking/commission-settlement/create|submit|approve|reject|pay`; `GET /booking/commission-settlement/get|page|log-list` | 创建 `commissionIds[]`; 提交 `slaMinutes`; 驳回 `rejectReason`; 打款 `payVoucherNo/payRemark` | `id`、`Boolean`、列表 / 分页 / 日志 | 空日志合法；`Boolean=true` 只是接口成功 | 不能把 `true` 直接写成打款已到账 |
| `BO-003` 通知补偿 | `GET /booking/commission-settlement/notify-outbox-page`; `POST /booking/commission-settlement/notify-outbox-retry`; `POST /booking/commission-settlement/notify-outbox-batch-retry` | `settlementId`,`status`,`notifyType`,`channel`,`lastActionCode`；重试 `ids[]` | 出站分页；批量重试结果 `retried/skipped` | 没有出站记录合法 | 不能把 `SENT` 或 `retried` 写成接收方已处理 |

## 4. 空态、观察态与 fail-close 边界
- `BO-001`
  - 对账分页空集、汇总计数全 0、退款审计分页空集都属于合法观察态。
  - `ticketSummaryDegraded=true` 只表示关联工单摘要退化，不能替代页面失败或 release blocker。
- `BO-002`
  - `dryRun=true` 的 replay / sync-tickets 一律只算演练样本。
  - `skipCount > 0`、`warningTag`、`ticketSyncStatus` 都是观察信息，不等于真正修复完成。
- `BO-003`
  - `notify-outbox-page` 为空表示当前没有待观察出站记录，属于合法空态。
  - `Boolean=true` 仍需结合页面回读和日志回读，不能只凭接口包体认定审批、驳回、打款、通知补偿已闭环。

## 5. 独立边界与禁止误写
- 不得把 `BO-004` 的独立 contract / runbook 借给 `BO-001` ~ `BO-003`。
- 不得把 job、脚本、runId、工单同步存在写成“可直接发布”或“已完成线上闭环”。
- 不得把 `notify-outbox` 有记录写成“店长已收到”或“收款方已处理完成”。
- 不得把 `run`、`replay`、`batch retry` 的受理结果写成真实业务成功率。

## 6. 当前结论
- `BO-001` ~ `BO-003` 已具备独立后台 contract，后台页面 / API / controller 真值边界已成体系。
- 本文解决的是独立 contract 缺失，不自动改变任何能力的发布结论。
- 当前固定口径：`ACTIVE_ADMIN / 可继续维护与联调 / 不因此自动升为 release-ready`。
