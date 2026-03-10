---
name: hxy-window-handoff-normalizer
description: Use when preparing A/B/C/D prompts, normalizing multi-window handoffs, or requiring fixed commit, file, verification, handoff, and integration-note output for HXY work.
---

# HXY Window Handoff Normalizer

## Overview
Standardize window prompts and handoffs so A-window integration stays deterministic and low-cost.

## Required Input
- window role and domain scope
- exact deliverable files
- required verification commands
- expected handoff consumer and integration risks

## Workflow
1. Fix the window role and one explicit scope.
2. Use the response-format reference for mandatory return sections.
3. Use the role matrix to decide what field, errorcode, and degrade notes are required.
4. Reject vague status such as "done" without commit, files, and verification.
5. Emit handoff notes that A-window can integrate directly.

## Deliverables
- normalized prompt skeleton
- fixed handoff structure
- role-specific integration note checklist

## Quality Gates
- Every window report includes commit, files, verification, handoff path, and integration notes.
- Field/errorcode/degrade notes are explicit, not hidden in prose.
- A-window never has to infer whether the result is mergeable.

## References
- `references/response-format.md`
- `references/window-role-matrix.md`
- `references/handoff-checklist.md`
