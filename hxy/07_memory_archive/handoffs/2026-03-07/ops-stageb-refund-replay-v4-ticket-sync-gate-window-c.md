# Window C Handoff - StageB Refund Replay V4 Ticket Sync Gate

- Date: 2026-03-07
- Branch: feat/ui-four-account-reconcile-ops
- Scope: DevOps gate / CI orchestration / workflow / docs only

## Delivered

1. New gate script
- Added `ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_ticket_sync_gate.sh`.
- Gate checks (BRT4xx) include:
  - `/booking/refund-notify-log/replay-run-log/detail/page`
  - `/booking/refund-notify-log/replay-run-log/sync-tickets`
  - sync-tickets request anchors: `dryRun` / `forceResync`
  - summary ticket-sync fields:
    - `ticketSyncSuccessCount`
    - `ticketSyncSkipCount`
    - `ticketSyncFailCount`
  - runId-not-exists error code anchor:
    - `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` (`1030004016`)
- Output contract: `summary.txt + result.tsv`; exit code `0/2/1`.

2. Local CI integration
- Updated `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`.
- New gate is enabled by default and participates in blocking decision.
- Added switch flags and env:
  - `--skip-booking-refund-replay-ticket-sync-gate`
  - `RUN_BOOKING_REFUND_REPLAY_TICKET_SYNC_GATE=0`
  - `REQUIRE_BOOKING_REFUND_REPLAY_TICKET_SYNC_GATE=0`
- Added summary/log fields:
  - `run_booking_refund_replay_ticket_sync_gate`
  - `require_booking_refund_replay_ticket_sync_gate`
  - `booking_refund_replay_ticket_sync_gate_rc`
  - `booking_refund_replay_ticket_sync_gate_summary`
  - `booking_refund_replay_ticket_sync_gate_tsv`

3. Workflow + required-check observability
- Updated `.github/workflows/ops-stageb-p1-guard.yml`:
  - trigger paths include new gate script
  - `workflow_dispatch` new input: `require_booking_refund_replay_ticket_sync_gate`
  - pass `REQUIRE_BOOKING_REFUND_REPLAY_TICKET_SYNC_GATE` into local-ci script
  - step summary emits ticket-sync gate run/require/rc fields
- Updated `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`:
  - keep required context unchanged: `hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
  - add ticket-sync gate observability and skip/soft-block examples

4. Rollout docs updated
- Updated `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md`:
  - new gate troubleshooting section
  - new gate quick-check commands
  - local-ci skip/soft-block combinations for V4 ticket-sync gate

## Switches

- Skip gate (CLI)
```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --skip-booking-refund-replay-ticket-sync-gate
```

- Skip gate (ENV)
```bash
RUN_BOOKING_REFUND_REPLAY_TICKET_SYNC_GATE=0 \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

- Soft block (ENV)
```bash
REQUIRE_BOOKING_REFUND_REPLAY_TICKET_SYNC_GATE=0 \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

## Soft-Block Policy

- Default policy: hard-block (`REQUIRE_BOOKING_REFUND_REPLAY_TICKET_SYNC_GATE=1`).
- Degrade policy: set `REQUIRE_BOOKING_REFUND_REPLAY_TICKET_SYNC_GATE=0`, gate `BLOCK` converts to pipeline `WARN` and remains traceable in `result.tsv`.
- Recommendation: keep soft-block temporary and restore hard-block after implementation is complete.

## Rollback Command

- Required checks rollback (context unchanged):
```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```

## Known Failure Examples

1. Missing replay-run detail page endpoint anchor
- Symptom: `BRT401_REPLAY_RUN_DETAIL_PAGE_ENDPOINT_MISSING`
- Typical cause: `/replay-run-log/detail/page` route/contract not present.

2. Missing sync-tickets request contract fields
- Symptom: `BRT403_SYNC_TICKETS_DRY_RUN_ANCHOR_MISSING` or `BRT404_SYNC_TICKETS_FORCE_RESYNC_ANCHOR_MISSING`
- Typical cause: `dryRun` / `forceResync` not exposed in request contract.

3. Missing summary ticket-sync counters
- Symptom: `BRT405/BRT406/BRT407`
- Typical cause: summary contract lacks `ticketSyncSuccessCount/ticketSyncSkipCount/ticketSyncFailCount` anchors.

4. Missing runId-not-exists error code
- Symptom: `BRT408_ERROR_CODE_RUN_ID_NOT_EXISTS_MISSING`
- Typical cause: `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)` missing or renamed.
