# Window A Handoff - booking refund replay run-log (V3)

- Date: 2026-03-06
- Branch: feat/ui-four-account-reconcile-ops
- Scope: booking/trade backend only (no overlay changes)

## 1. Change Summary

1. Added run-log table for replay batches: `hxy_booking_refund_replay_run_log`.
2. Added manual replay-due endpoint:
   - `POST /booking/refund-notify-log/replay-due`
   - input: `limit` (optional), `dryRun` (optional, default false)
   - output: `runId + successCount/skipCount/failCount + details`
3. Added run-log query endpoints:
   - `GET /booking/refund-notify-log/replay-run-log/page`
   - `GET /booking/refund-notify-log/replay-run-log/get?id=xxx`
4. Unified job and manual replay audit path:
   - `BookingRefundNotifyReplayJob` now writes run-log via service and returns runId summary.
5. Kept fail-open guarantees:
   - single replay failure does not stop batch
   - four-account refresh failure does not rollback refund replay; warning kept in replay detail and run-log errorMsg

## 2. Key Contract Notes

1. Run status:
   - `started`, `success`, `partial_fail`, `fail`
2. Trigger source:
   - `MANUAL` for controller replay-due
   - `JOB` for scheduled replay job
3. Backward compatibility:
   - existing replay APIs and service default signature remain available
4. Searchable warning:
   - four-account degrade warning uses marker `FOUR_ACCOUNT_REFRESH_WARN`

## 3. SQL

- Added:
  - `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-06-hxy-booking-refund-replay-run-log.sql`

## 4. Tests Added/Updated

1. `BookingRefundNotifyLogControllerTest`
   - replay-due delegate
   - replay-run-log page/get delegates
   - null request fallback for replay-due
2. `BookingRefundNotifyLogServiceTest`
   - run-log persist/update for due replay
   - partial-fail and dry-run scenarios
3. `BookingRefundNotifyReplayJobTest` (new)
   - runId and counter summary
   - invalid param fallback without interrupting replay execution

## 5. Risks and Operational Notes

1. If run-log table insert/update fails, replay main flow still continues (degraded observability only).
2. Large replay batches should still use reasonable `limit` to avoid long single-run windows.
3. For incident replay, prefer first run with `dryRun=true`, then execute apply run.

