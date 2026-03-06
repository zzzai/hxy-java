# Window C Handoff - StageB Refund Replay V2 Gate

- Date: 2026-03-06
- Branch: feat/ui-four-account-reconcile-ops
- Scope: DevOps and gate automation only (scripts/workflow/docs), no business code changes

## Delivered

1. New gate script
- Added `ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_v2_gate.sh`.
- Checks include:
  - replay endpoint anchor exists.
  - replay request supports `ids + dryRun` anchors.
  - replay response supports `successCount/skipCount/failCount/details` anchors.
  - replay v2 service anchors for batch replay semantics.
  - `booking_refund_notify_log` audit field anchors exist in SQL/DO/Mapper (any source accepted).
  - invalid-param/status-conflict error code anchors exist (`1030004011` / `1030004014`).
- Output protocol: `summary + tsv`; exit codes `0/2/1`.

2. Local StageB CI integration
- Updated `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`.
- New gate wired in by default.
- New downgrade switches:
  - CLI: `--skip-booking-refund-replay-v2-gate`
  - ENV: `RUN_BOOKING_REFUND_REPLAY_V2_GATE=0`
  - Soft fail: `REQUIRE_BOOKING_REFUND_REPLAY_V2_GATE=0`
- Summary output now includes:
  - `booking_refund_replay_v2_gate_rc`
  - `clean_before_tests` (kept stable)

3. Workflow and required-checks observability
- Updated `.github/workflows/ops-stageb-p1-guard.yml`:
  - path trigger includes `check_booking_refund_replay_v2_gate.sh`
  - workflow input adds `require_booking_refund_replay_v2_gate`
  - workflow input adds `clean_before_tests`
  - step summary adds `booking_refund_replay_v2_gate_rc` and `clean_before_tests`
- Updated `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`:
  - stageb scope log now includes `booking-refund-replay-v2`
  - context name unchanged log retained

4. Rollout doc
- Updated `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md` with:
  - replay-v2 gate prerequisites and troubleshooting anchors
  - enable/disable commands for replay-v2 gate
  - clean-before-tests combination recommendations
  - rollback path reminder (context unchanged)

## Rollback Notes

- Required check context name remains unchanged:
  - `hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
- StageA/StageB context naming was not changed.
- StageB rollback command:

```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```
