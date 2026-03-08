# MiniApp P0 Contract Regression - Window D Handoff (2026-03-08)

## 1. 变更摘要

- 新增 `trade app` 聚合接口契约回归测试：
  - `GET /trade/order/pay-result`：正常/查无/降级（`degraded` + `degradeReason`）
  - `GET /trade/after-sale/refund-progress`：正常/查无
- 新增错误码稳定性锚点测试：
  - `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT = 1030004012`
  - `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS = 1030004016`
  - `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED = 1011000125`
- 新增 fail-open 回归：
  - 下游 `tradeServiceOrderApi.cancelByPayOrderId` 异常时，不阻断退款主链路，且可通过 `getOrder` 检索退款结果。
- 新增发布验收清单文档：
  - `docs/plans/2026-03-08-miniapp-p0-contract-regression-checklist.md`

## 2. 测试命令与结果

1. `git diff --check`
- 结果：PASS

2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- 结果：PASS（`[naming-guard] result=PASS checked_files=126`）

3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- 结果：PASS（`[hxy-memory-guard] result=PASS checked_files=133 changed_core_domains=trade`）

4. `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-trade,yudao-module-mall/yudao-module-booking -am -Dtest=AppTradeOrderControllerTest,AppAfterSaleControllerTest,BookingOrderServiceImplTest,BookingRefundNotifyLogServiceTest,FourAccountReconcileServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：PASS（`Tests run: 65, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`）

## 3. 可 Cherry-pick 结论与潜在冲突点

- 结论：可 cherry-pick 到窗口 A。
- 建议粒度：按本次单提交直接 cherry-pick。
- 潜在冲突点：
  - `BookingOrderServiceImplTest`（若窗口 A 并行修改退款回调/fail-open 用例）
  - `BookingRefundNotifyLogServiceTest`（若窗口 A 并行修改 replay runId 校验用例）
  - 新增 `AppTradeOrderControllerTest`、`AppAfterSaleControllerTest` 通常低冲突。
