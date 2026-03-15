# Window A Handoff - Booking Runtime Release Integration (2026-03-15)

## Scope
- Window: A（CTO / 集成窗口）
- Branch: `window-a-booking-runtime-release-integration-20260315`
- Batch target:
  - booking runtime 最终集成评审
  - capability / business / release / checklist 四份主文档回写
  - A 窗口 release-evidence 单一真值与 handoff 落盘

## Delivered
- 新增 booking release 单一真值：
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-release-evidence-review-v1.md`
- 更新主文档：
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
  - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md`
  - `docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md`
- 当前 booking 最终结论固定为：
  - `Doc Closed / Can Develop / Cannot Release`
- 已明确拆分：
  - query-only `ACTIVE`：`technician-list / technician-detail / order-list / order-detail`
  - write-chain blocker：`create / cancel / addon`
- 已明确 shared chain 真值：
  - booking runtime gate 已接入 `run_ops_stageb_p1_local_ci.sh`
  - booking gate 成功输出仍是 `can_release=NO`

## Evidence
- 代码与测试真值：
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- gate / shared-chain 真值：
  - `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`
  - `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`
- 当前分支已正式提交的 booking runtime 证据：
  - `c8531df2d4 refactor(booking): extract page smoke helpers`
  - `1ec3424702 test(booking): add technician page smoke coverage`
  - `45f5f690a7 test(booking): cover confirm cancel and addon flows`
  - `fa37cbce92 test(booking): freeze failure branch behavior`
  - `70bd89c07c test(booking): add miniapp runtime gate`
  - `aa2c13e7cc ci(booking): integrate runtime gate into shared chain`
- 当前分支已正式吸收的 2026-03-15 窗口产出：
  - 窗口B：`36babd984e docs(booking): close runtime acceptance and recovery prd`
  - 窗口C：`39a5e7d4ac docs: close booking runtime contract evidence`
  - 窗口D：`460380893c docs(booking): close runtime release runbook gate`

## Verification
- `node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
  - PASS
- `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - PASS
- `node --test tests/ops-stageb-booking-runtime-gate.test.mjs`
  - PASS
- `bash ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`
  - PASS，summary=`doc_closed=YES can_develop=YES can_release=NO result=PASS`
- `RUN_ID=booking_runtime_audit_doc SKIP_MYSQL_INIT=1 bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-naming-guard --skip-memory-guard --skip-booking-refund-notify-gate --skip-booking-refund-audit-gate --skip-booking-refund-replay-v2-gate --skip-booking-refund-replay-runlog-gate --skip-booking-refund-replay-run-summary-gate --skip-booking-refund-replay-ticket-sync-gate --skip-finance-partial-closure-gate --skip-miniapp-p0-contract-gate --skip-stock-gate --skip-lifecycle-gate --skip-tests`
  - PASS，shared chain 中 `booking_miniapp_runtime_gate_rc=0`，但 booking gate 日志仍输出 `can_release=NO`
- `git diff --check -- docs/products/miniapp/2026-03-15-miniapp-booking-runtime-release-evidence-review-v1.md docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md hxy/07_memory_archive/handoffs/2026-03-15/booking-runtime-release-integration-window-a.md`
  - PASS

## Integration Notes
- 字段：
  - 技师查询侧只认 `storeId`、`technicianId`、`date`
  - create 只认 `timeSlotId`、`spuId`、`skuId?`、`userRemark?`、`dispatchMode?`、`storeId?`
  - cancel 只认 query `id`、`reason`
  - addon 只认 `parentOrderId`、`addonType`、`spuId?`、`skuId?`
- 错误码：
  - 技师查询：`TECHNICIAN_NOT_EXISTS(1030001000)`、`TECHNICIAN_DISABLED(1030001001)`
  - create：`SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_NOT_AVAILABLE(1030003001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)`
  - cancel / addon：`BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_STATUS_ERROR(1030004001)`
  - 只按 `code` 分支，不按 `message` 分支
- 降级行为：
  - query-only 接口当前无服务端 `degraded` 字段；空列表 `[]` 属合法空态
  - create / cancel / addon 均按 `FAIL_CLOSE` 管理；失败时不得伪成功跳转/刷新
  - shared booking runtime gate `PASS` 不是“降级成功”，它只证明当前边界仍是 `can_release=NO`

## Risks
- Booking 不得被整域写成 `Ready`、`Frozen Candidate`、可放量。
- `technician-list / technician-detail / order-list / order-detail` 的 query-only `ACTIVE` 不能冲抵 `create / cancel / addon` 的 release blocker。
- 当前未见独立 B/C/D 2026-03-15 正式 handoff，若后续补交，A 窗口只能在正式提交后再吸收。
- 无关修改 `hxy/04_data/HXY-全域数据价值化与门店数据治理蓝图-v1-2026-03-08.md` 未触碰。
