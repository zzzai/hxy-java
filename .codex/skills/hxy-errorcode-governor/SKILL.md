---
name: hxy-errorcode-governor
description: Use when reconciling HXY miniapp error codes across product docs, contracts, runbooks, UI recovery rules, or when clearing TBD markers and stabilizing canonical errorCode truth.
---

# HXY Errorcode Governor

## Overview
Govern error codes as a single project truth so product, contract, UI, and runbook layers stop drifting independently.

## Required Input
- canonical errorcode register and recovery matrix
- affected product, contract, SOP, and runbook docs
- target domain or release batch

## Workflow
1. Read the canonical source map before editing any downstream doc.
2. Scan for `TBD_*`, unstable aliases, or message-driven handling.
3. Compare product and runbook mentions against the canonical register.
4. Keep failure handling code-driven: `errorCode`, `resultCode`, `failureMode`, `retryClass`.
5. Emit exact doc updates, missing codes, and blockers.

## Quick Run
```bash
bash .codex/skills/hxy-errorcode-governor/scripts/check_errorcode_governance.sh .
```

## Deliverables
- canonical errorcode drift list
- blocked `TBD_*` / alias list
- downstream update checklist for product, contract, and runbook docs

## Quality Gates
- No frozen doc keeps `TBD_*` error codes.
- Message text never becomes the branching source of truth.
- `RESERVED_DISABLED`, `TICKET_SYNC_DEGRADED`, and key numeric codes stay stable across layers.

## References
- `references/canonical-source-map.md`
- `references/errorcode-governance-checklist.md`
- `references/output-template.md`
