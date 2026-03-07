# Window C Handoff - Ops StageB Finance Partial Closure Gate

- Date: 2026-03-08
- Branch: feat/ui-four-account-reconcile-ops
- Scope: StageB gate / local-ci / workflow / required-checks / rollout docs

## Delivered

1. New gate script
- Added `ruoyi-vue-pro-master/script/dev/check_finance_partial_closure_gate.sh`.
- Gate output contract:
  - `summary.txt`
  - `result.tsv`
  - exit code `0/2/1` (`PASS|WARN` / `BLOCK` / `script error`)
- Required anchors include:
  - 退款回调幂等冲突错误码（`BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT`）
  - 提成计提/冲正幂等关键契约（`ensureReversalPayloadConsistent` + mapper lookup）
  - 退款冲正唯一约束/冲突语义（`uk_origin_commission_id` + `DuplicateKeyException` 分支）
  - 四账摘要字段与 fail-open/degrade 标记（`ticketSummaryDegraded` + degrade/fail-open anchors）

2. StageB local-ci integration
- Updated `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`.
- Gate is enabled by default and participates in blocking decision.
- Added switches:
  - `--skip-finance-partial-closure-gate`
  - `RUN_FINANCE_PARTIAL_CLOSURE_GATE=0`
  - `REQUIRE_FINANCE_PARTIAL_CLOSURE_GATE=0`
- Added summary/log fields:
  - `run_finance_partial_closure_gate`
  - `require_finance_partial_closure_gate`
  - `finance_partial_closure_gate_rc`
  - `finance_partial_closure_gate_log`
  - `finance_partial_closure_gate_summary`
  - `finance_partial_closure_gate_tsv`

3. Workflow + required checks integration
- Updated `.github/workflows/ops-stageb-p1-guard.yml`:
  - trigger paths include finance gate script
  - `workflow_dispatch` adds `require_finance_partial_closure_gate`
  - injects `REQUIRE_FINANCE_PARTIAL_CLOSURE_GATE` into local-ci script
  - step summary includes finance gate rc/run/require fields
- Updated `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`:
  - stageB scope log includes `finance-partial-closure`
  - added finance gate skip/soft-block examples
  - required check context remains unchanged

## Enable / Soft-Block / Rollback

- Enable (default in local stageB):
```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

- Soft-block (keep gate execution, downgrade BLOCK to WARN):
```bash
REQUIRE_FINANCE_PARTIAL_CLOSURE_GATE=0 \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

- Hard skip (single-run rollback style):
```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --skip-finance-partial-closure-gate
```

- Required-check rollback (remove StageB guard context from branch protection):
```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```

## Merge Notes For Window A

- Keep required check context unchanged:
  - `hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
- Keep switches unchanged:
  - `--skip-finance-partial-closure-gate`
  - `RUN_FINANCE_PARTIAL_CLOSURE_GATE=0`
  - `REQUIRE_FINANCE_PARTIAL_CLOSURE_GATE=0`
- Rollback commands to preserve in docs/ops runbook:
  - `--skip-finance-partial-closure-gate`
  - `REQUIRE_FINANCE_PARTIAL_CLOSURE_GATE=0 ...run_ops_stageb_p1_local_ci.sh`
  - `rollback_ops_stageb_required_checks.sh`
