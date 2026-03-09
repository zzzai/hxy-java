---
name: hxy-miniapp-capability-auditor
description: Use when auditing HXY miniapp capability truth, checking ACTIVE versus PLANNED_RESERVED, or reconciling routes, frontend API calls, backend controllers, and release docs.
---

# HXY Miniapp Capability Auditor

## Overview
Audit capability truth from code and frozen docs before anyone updates ledgers, release matrices, or activation status.

## Required Input
- Target domain, route set, or release batch
- Current miniapp docs and canonical contract docs
- Local repo access to `yudao-mall-uniapp` and app controllers

## Workflow
1. Read canonical sources in `references/canonical-sources.md`.
2. Run the scan script to collect route, frontend API, and controller evidence.
3. Mark each capability only after three checks pass: real page, aligned API, executable acceptance basis.
4. Classify gaps as `ACTIVE`, `PLANNED_RESERVED`, or `DEPRECATED`.
5. Emit blockers with exact file references and no message-driven inference.
6. Feed confirmed results into capability ledger, coverage matrix, and handoff.

## Quick Run
```bash
bash .codex/skills/hxy-miniapp-capability-auditor/scripts/scan_capability_truth.sh . booking
```

## Deliverables
- Capability truth table
- Route/API/controller mismatch list
- Ready/Frozen input notes for A-window integration

## Quality Gates
- No capability is marked `ACTIVE` without route, API, and acceptance evidence.
- Prototype aliases are called out explicitly instead of reused as truth.
- `RESERVED_DISABLED` features stay out of active release scope.

## References
- `references/canonical-sources.md`
- `references/output-template.md`
