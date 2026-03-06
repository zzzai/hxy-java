# Window C Handoff - StageB Refund Notify And Four Account Audit Gate

- Date: 2026-03-06
- Branch: `feat/ui-four-account-reconcile-ops`
- Scope: StageB/P1 gate hardening for booking refund callback and four-account audit checks (scripts/workflow/docs only)

## Changes

1. New gate script:
- Added `script/dev/check_booking_refund_notify_gate.sh`.
- Checks include:
  - migration SQL contains `booking_order.pay_refund_id` and `booking_order.refund_time`
  - booking `ErrorCodeConstants` contains refund notify invalid-id and idempotent-conflict codes
  - `AppBookingOrderController` exposes `/booking/order/update-refunded`
  - `BookingOrderServiceImpl` has refunded idempotent conflict branch.
- Unified output: `summary + tsv`; exit code `0/2/1`.

2. Local CI script:
- Updated `script/dev/run_ops_stageb_p1_local_ci.sh`.
- Added `booking-refund-notify-gate` stage and tsv aggregation.
- Added gate controls:
  - `--skip-booking-refund-notify-gate`
  - `--require-booking-refund-notify-gate <0|1>`
- Default regression test set now includes:
  - `BookingOrderServiceImplTest`
  - `AppBookingOrderControllerTest`
  - `FourAccountReconcileServiceImplTest`
  - (keeps previous tests unchanged)
- `REGRESSION_TEST_CLASSES` override remains supported.

3. Workflow and required-checks observability:
- Updated `.github/workflows/ops-stageb-p1-guard.yml`:
  - path trigger includes `check_booking_refund_notify_gate.sh`
  - workflow regression test list includes `AppBookingOrderControllerTest`
  - step summary includes `booking_refund_notify_gate_rc`.
- Updated `script/dev/setup_github_required_checks.sh` (logging only):
  - mode/profile/stageb scope display
  - helper commands for dry-run/apply/rollback.

4. Rollout doc update:
- Updated `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md`
  - added refund callback consistency test notes
  - added standalone gate command and troubleshooting
  - clarified dry-run/apply/rollback helper command usage.

## Verification

- `git diff --check` => PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh` => PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh` => PASS
- `bash ruoyi-vue-pro-master/script/dev/check_booking_refund_notify_gate.sh` => PASS
- `bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init` => PASS
