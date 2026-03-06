# Window C Handoff - StageB Refund Audit Gate V2

- Date: 2026-03-06
- Branch: current working branch (no branch switch)
- Scope: StageB/P1 automation gate for refund callback compensation + refund audit summary (scripts/workflow/docs only)

## Changes

1. New gate script
- Added `ruoyi-vue-pro-master/script/dev/check_booking_refund_audit_gate.sh`.
- Gate checks:
  - `BookingRefundNotifyLogController` exposes `/booking/refund-notify-log/page` and `/booking/refund-notify-log/replay`.
  - `FourAccountReconcileController` exposes `/booking/four-account-reconcile/refund-audit-summary`.
  - `ErrorCodeConstants` contains `1030004011`, `1030004013`, `1030004014` (refund notify audit related constants).
- Output protocol: `summary + tsv`; exit code `0/2/1` for `PASS/BLOCK/ERROR`.

2. Local CI orchestrator integration
- Updated `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`.
- Default include: refund audit gate runs by default.
- Added downgrade switches:
  - CLI: `--skip-booking-refund-audit-gate`
  - ENV: `RUN_BOOKING_REFUND_AUDIT_GATE=0`
  - Soft-fail switch: `REQUIRE_BOOKING_REFUND_AUDIT_GATE=0`
- Added audit artifacts in pipeline summary/logs:
  - `booking_refund_audit_gate_rc`
  - `booking_refund_audit_gate_log`
  - `booking_refund_audit_gate_summary`
  - `booking_refund_audit_gate_tsv`

3. Workflow and required-checks observability
- Updated `.github/workflows/ops-stageb-p1-guard.yml`:
  - PR path trigger includes `check_booking_refund_audit_gate.sh`.
  - `workflow_dispatch` adds `require_booking_refund_audit_gate` input.
  - Step summary adds `booking_refund_audit_gate_rc`.
- Updated `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`:
  - Log only: stageB scope includes `booking-refund-audit`.
  - Added explicit log that stageB required context name is unchanged.

4. Rollout doc
- Updated `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md` with:
  - New gate script in prerequisites.
  - New troubleshooting section for refund audit gate.
  - New quick command and local downgrade command examples.

## Context And Rollback Notes

- Required check context name remains unchanged:
  - `hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
- StageA/StageB existing contexts are not renamed.
- Rollback command (remove stageB required check, keep stageA contexts):

```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```

## Verification Commands

- `git diff --check` => PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh` => PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh` => FAIL (workspace has pre-existing untracked SQL domain changes; guard asks to update 3 governance docs)
- `bash ruoyi-vue-pro-master/script/dev/check_booking_refund_audit_gate.sh` => PASS
- `bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init` => FAIL (`tests_rc=1`, existing product test compilation issue: `PreAuthorize cannot be resolved to a type` in `ProductStoreSkuControllerTest`)
- Additional switch verification: `bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init --skip-tests --skip-booking-refund-audit-gate` => PASS
