# booking refund replay run summary + ticket sync (window A)

- Date: 2026-03-07
- Branch: `feat/ui-four-account-reconcile-ops`
- Scope: booking/trade backend + SQL + governance docs; no overlay change in this batch.

## 1. Deliverables

1. Added run-level summary API:
   - `GET /booking/refund-notify-log/replay-run-log/summary?runId=...`
   - Returns: run meta/counts + `warningCount` + `topFailCodes` + `topWarningTags`.
   - If runId missing/invalid/not-exists -> `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS`.
2. Added run-level ticket sync API:
   - `POST /booking/refund-notify-log/replay-run-log/sync-tickets`
   - Request: `runId` required, `onlyFail` optional default `true`.
   - Behavior: idempotent upsert to trade ticket center with source `REFUND_REPLAY_RUN:<runId>:<notifyLogId>`.
   - Fail-open: trade errors do not block; returns `attemptedCount/successCount/failedCount/failedIds`.
3. Enhanced page compatibility:
   - `GET /booking/refund-notify-log/replay-run-log/page` adds optional filters: `hasWarning`, `minFailCount`, `triggerSource`.
4. Added replay run detail ledger:
   - Table: `hxy_booking_refund_replay_run_detail`
   - SQL: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-07-hxy-booking-refund-replay-run-detail.sql`
   - Unique key: `(run_id, notify_log_id)` for idempotent detail persistence.

## 2. Main backend files

- Controller:
  - `BookingRefundNotifyLogController.java`
- Service:
  - `BookingRefundNotifyLogService.java`
  - `BookingRefundNotifyLogServiceImpl.java`
- VO:
  - `BookingRefundReplayRunLogSummaryRespVO.java`
  - `BookingRefundReplayRunLogSyncTicketReqVO.java`
  - `BookingRefundReplayRunLogSyncTicketRespVO.java`
  - `BookingRefundReplayRunLogPageReqVO.java`
- Mapper/DO:
  - `BookingRefundReplayRunDetailDO.java`
  - `BookingRefundReplayRunDetailMapper.java`
  - `BookingRefundReplayRunLogMapper.java`
- Error code:
  - `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS`

## 3. Tests

- `BookingRefundNotifyLogControllerTest`
  - added summary/sync/page filter coverage.
- `BookingRefundNotifyLogServiceTest`
  - added summary aggregation, sync fail-open, page normalization coverage.
- Regression command executed (booking+trade target suite) and passed.

## 4. Integration notes for window B/C

1. Window B:
   - New backend fields/endpoints available for run-level board and ticket sync.
   - `sync-tickets` default behavior is `onlyFail=true`.
2. Window C:
   - Gate scripts can anchor these APIs/VOs:
     - `/replay-run-log/summary`
     - `/replay-run-log/sync-tickets`
     - response fields `warningCount/topFailCodes/topWarningTags`.
3. Degrade semantics:
   - Trade API failure in sync path is fail-open; inspect `failedIds` for retry.
