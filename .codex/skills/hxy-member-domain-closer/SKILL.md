---
name: hxy-member-domain-closer
description: Use when reconciling HXY member-domain docs with real routes and APIs, or deciding which member capabilities are truly active versus planned.
---

# HXY Member Domain Closer

## Overview
Close member-domain doc gaps without promoting route aliases or missing pages into active scope.

## Required Input
- member PRD, contract, field dictionary, errorcopy, KPI/runbook docs
- `pages.json` and actual member-related page files
- member app API and controllers

## Workflow
1. Check whether each member route exists as a real uniapp page, component entry, or only a document alias.
2. Compare member API docs against frontend API files and app controllers.
3. Split member capabilities into `ACTIVE`, `PLANNED_RESERVED`, and missing-page items.
4. Preserve active runtime paths such as login components, profile, sign-in, address, wallet, and point ledger when evidence exists.
5. Keep level page, asset overview, and asset-ledger API out of active scope until route/controller truth exists.
6. Feed the result into capability ledger, coverage matrix, and freeze notes.

## Quick Run
```bash
bash .codex/skills/hxy-member-domain-closer/scripts/check_member_closure.sh .
```

## Deliverables
- Member route-truth table
- Active vs planned member capability split
- Freeze blockers for member docs

## Quality Gates
- Missing pages are called missing, not future-active.
- Component-based login entry is documented as component truth, not forced into fake route truth.
- `/member/asset-ledger/page` stays reserved until controller and gate conditions exist.

## References
- `references/member-route-truth.md`
- `references/member-closure-checklist.md`
