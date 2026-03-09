---
name: hxy-miniapp-doc-freeze-closer
description: Use when closing HXY miniapp doc batches, updating Ready and Frozen indexes, or preparing freeze reviews, source-of-truth updates, and handoff packages.
---

# HXY Miniapp Doc Freeze Closer

## Overview
Close documentation batches without corrupting frozen baselines or skipping release governance steps.

## Required Input
- Target batch date and docs list
- Current doc-gap index and ready-to-frozen review
- Capability ledger and consistency audit findings

## Workflow
1. Verify whether the requested docs are Draft, Ready, or already Frozen.
2. Check whether the batch changes semantics or only adds evidence.
3. Update index mappings without rolling back unrelated Frozen entries.
4. If semantics changed, keep the batch in `Ready` until consistency blockers close.
5. Update freeze-review or release-decision docs only when the gate conditions are met.
6. Emit a handoff that names blockers, not just completed files.

## Quick Run
```bash
bash .codex/skills/hxy-miniapp-doc-freeze-closer/scripts/check_freeze_inputs.sh . 2026-03-10
```

## Deliverables
- Updated doc-gap index rows
- Ready/Frozen judgment notes
- Freeze handoff summary

## Quality Gates
- Existing Frozen records never regress silently.
- No new doc is marked Frozen when route/API truth still drifts.
- Handoff names blockers, rollback conditions, and impacted owners.

## References
- `references/freeze-closure-checklist.md`
- `references/index-update-template.md`
