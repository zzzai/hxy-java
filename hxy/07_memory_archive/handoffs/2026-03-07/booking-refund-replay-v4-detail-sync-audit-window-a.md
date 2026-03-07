# booking refund replay v4 detail + ticket sync audit (window A)

- Date: 2026-03-07
- Branch: `feat/ui-four-account-reconcile-ops`
- Scope: booking/trade backend + SQL + governance docs

## 1. Backend capabilities delivered

1. Added replay run detail query APIs:
   - `GET /booking/refund-notify-log/replay-run-log/detail/page`
   - `GET /booking/refund-notify-log/replay-run-log/detail/get?id=...`
2. Added ticket sync audit ledger table:
   - `hxy_booking_refund_replay_ticket_sync_log`
   - SQL: `2026-03-07-hxy-booking-refund-replay-ticket-sync-log.sql`
3. Enhanced sync API (backward compatible):
   - `POST /booking/refund-notify-log/replay-run-log/sync-tickets`
   - new optional params: `dryRun` (default `false`), `forceResync` (default `false`)
   - new response fields: `skipCount`, `details[{notifyLogId,ticketId,resultCode,resultMsg}]`
4. Enhanced summary API (backward compatible):
   - `GET /booking/refund-notify-log/replay-run-log/summary`
   - added: `ticketSyncSuccessCount/ticketSyncSkipCount/ticketSyncFailCount`

## 2. Idempotency and degrade semantics

1. Idempotency rule:
   - same `runId + notifyLogId`, if latest ticket sync is `SUCCESS` and `forceResync=false` => return `SKIP` and append sync audit log.
2. Dry-run rule:
   - `dryRun=true` does not call trade ticket API, records `SKIP/DRY_RUN` audit.
3. Fail-open rule:
   - trade API failure does not break batch; response includes `failedIds` and detail error info.

## 3. Data model notes

1. `detail/page|get` returns latest ticket sync snapshot by joining run detail with latest sync log per `runId + notifyLogId`.
2. `sync-tickets` always appends audit rows for `SUCCESS/SKIP/FAIL`, preserving replay traceability.

## 4. Test coverage in this batch

1. `BookingRefundNotifyLogControllerTest`
   - added detail page/get cases
   - updated sync-tickets new params cases
   - validated summary new fields mapping
2. `BookingRefundNotifyLogServiceTest`
   - added sync idempotent skip / dry-run / force-resync / fail-open cases
   - added detail page/get and summary sync counters cases

## 5. Integration notes for B/C

1. B (overlay)
   - sync response now includes `skipCount` and `details`.
   - detail APIs available for run-level drill-down.
2. C (gate)
   - stable anchors:
     - `/replay-run-log/detail/page`
     - `/replay-run-log/detail/get`
     - `/replay-run-log/sync-tickets` params `dryRun/forceResync`
     - summary fields `ticketSyncSuccessCount/ticketSyncSkipCount/ticketSyncFailCount`
   - stable error code: `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)`
