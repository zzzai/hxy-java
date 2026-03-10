---
name: hxy-ui-review-to-prd
description: Use when turning HXY UI reviews, prototype pages, Figma notes, or stitched mockups into product PRDs, field dictionaries, route/API mappings, and release acceptance criteria.
---

# HXY UI Review To PRD

## Overview
Convert reviewed UI into product-executable docs instead of leaving it as visual intent.

## Required Input
- reviewed UI artifact, route scope, or page set
- related uniapp pages and frontend API files
- existing product, contract, and acceptance docs for the target flow

## Workflow
1. Capture page truth from the reviewed UI and real route files.
2. Map each UI state to route, API, status, empty/error/degraded state, and owner.
3. Produce PRD-ready output, field dictionary, and acceptance criteria together.
4. Mark visual-only ideas that lack route/API truth as planned, not active.
5. Route to capability audit or freeze closure if the UI review changes release scope.

## Quick Run
```bash
bash .codex/skills/hxy-ui-review-to-prd/scripts/check_ui_review_inputs.sh . booking
```

## Deliverables
- PRD-ready page flow
- page/API field dictionary
- acceptance checklist with success, empty, error, and degraded states

## Quality Gates
- UI review never bypasses route/API truth.
- Empty, error, and degraded states are mandatory, not optional polish.
- New CTA or state changes that affect release scope must feed the capability ledger and release gate.

## References
- `references/ui-review-checklist.md`
- `references/field-dictionary-template.md`
- `references/acceptance-template.md`
- `references/skill-composition.md`
