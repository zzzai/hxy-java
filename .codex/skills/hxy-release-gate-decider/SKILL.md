---
name: hxy-release-gate-decider
description: Use when making HXY miniapp Go or No-Go decisions, checking release gates, or reviewing degraded pool, RESERVED_DISABLED, KPI, rollback, and release-batch blockers.
---

# HXY Release Gate Decider

## Overview
Make release decisions from project evidence instead of optimistic document completion.

## Required Input
- Release batch and target scope
- Capability ledger, doc coverage matrix, release decision pack, and runbooks
- Current blocker list, KPI status, and reserved-switch status

## Workflow
1. Confirm the active release scope from the latest capability ledger.
2. Exclude `PLANNED_RESERVED` and `DEPRECATED` items from active gates.
3. Check blockers from consistency audit, route/API drift, and freeze status.
4. Check runbook and KPI thresholds, especially degraded-pool isolation and reserved-disabled leakage.
5. Produce `Go`, `Go with Gate`, or `No-Go` with explicit triggers and rollback actions.
6. Write the decision with exact evidence, not confidence language.

## Quick Run
```bash
bash .codex/skills/hxy-release-gate-decider/scripts/check_release_gate_inputs.sh .
```

## Deliverables
- Go/No-Go decision note
- Blocker table with owner and SLA
- Rollback and re-entry conditions

## Quality Gates
- `PLANNED_RESERVED` features never enter active release denominator.
- `degraded_pool` never pollutes main KPI or ROI judgment.
- Reserved-disabled leakage is treated as a gate failure, not a warning.

## References
- `references/release-decision-checklist.md`
- `references/go-no-go-template.md`
