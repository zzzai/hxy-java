# Window C Handoff - StageB Refund Replay Run Summary Gate

- Date: 2026-03-07
- Branch: feat/ui-four-account-reconcile-ops
- Scope: DevOps gate/workflow/docs integration only

## Delivered

1. New gate script
- Added `ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_run_summary_gate.sh`.
- Gate checks (BRS5xx):
  - controller endpoint anchors:
    - `/booking/refund-notify-log/replay-run-log/summary`
    - `/booking/refund-notify-log/replay-run-log/sync-tickets`
  - service + impl anchors: replay-run summary / ticket-sync methods.
  - fail-open/degrade semantics anchors in service impl.
  - controller/service test anchors for summary + sync-tickets.
  - error code / constant anchors:
    - runId not exists (`BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS`, 1030004016)
    - ticket-sync degraded constant/keyword.
- Output contract: `summary.txt + result.tsv`; exit code `0/2/1`.

2. Local CI integration
- Updated `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`.
- New gate is enabled by default and participates in blocking decision.
- Added summary/log fields:
  - `booking_refund_replay_run_summary_gate_rc`
  - `booking_refund_replay_run_summary_gate_summary`
  - `booking_refund_replay_run_summary_gate_tsv`

3. Workflow + rollout observability
- Updated `.github/workflows/ops-stageb-p1-guard.yml`:
  - path trigger includes new gate script.
  - workflow input adds `require_booking_refund_replay_run_summary_gate`.
  - step summary outputs `booking_refund_replay_run_summary_gate_rc`.
- Updated `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md` with new gate troubleshooting and downgrade commands.
- Updated `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh` observability logs:
  - stageB scope now includes `booking-refund-replay-run-summary`.
  - required check context name remains unchanged.

## Switches

- Skip gate (CLI):
```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --skip-booking-refund-replay-run-summary-gate
```

- Skip gate (ENV):
```bash
RUN_BOOKING_REFUND_REPLAY_RUN_SUMMARY_GATE=0 \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

- Soft block (ENV):
```bash
REQUIRE_BOOKING_REFUND_REPLAY_RUN_SUMMARY_GATE=0 \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

- Soft block (CLI equivalent):
```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --require-booking-refund-replay-run-summary-gate 0
```

## Soft-Block Policy

- Default policy: hard-block (`REQUIRE_BOOKING_REFUND_REPLAY_RUN_SUMMARY_GATE=1`).
- Degrade policy: set `REQUIRE_BOOKING_REFUND_REPLAY_RUN_SUMMARY_GATE=0`, gate `BLOCK` becomes pipeline `WARN` and is retained in `result.tsv`.
- Recommendation: only use soft-block for short-lived unblock windows, then revert to hard-block.

## Rollback Command

- Required checks rollback (context name unchanged):
```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```

- Local quick rollback (single run): use `--skip-booking-refund-replay-run-summary-gate`.

## Known Failure Examples

1. Missing controller endpoints
- Symptoms: `BRS501_REPLAY_RUN_SUMMARY_ENDPOINT_MISSING`, `BRS502_REPLAY_RUN_SYNC_TICKETS_ENDPOINT_MISSING`
- Typical cause: controller route not added or path mismatch.

2. Missing service/service-impl anchors
- Symptoms: `BRS503~BRS506`
- Typical cause: interface and implementation method names not aligned for summary/ticket-sync.

3. Missing fail-open/degrade semantics
- Symptoms: `BRS507`, `BRS512`, `BRS514`
- Typical cause: sync ticket exceptions are not downgraded or no degrade/warn anchor retained.

4. Missing runId-not-exists error code
- Symptoms: `BRS513_ERROR_CODE_RUN_ID_NOT_EXISTS_MISSING`
- Typical cause: `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS`（1030004016） removed/renamed without anchor compatibility.
