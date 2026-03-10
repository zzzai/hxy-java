---
name: hxy-product-doc-gap-planner
description: Use when planning HXY miniapp product-document closure by business domain, inventorying coverage gaps, prioritizing P0/P1/P2 work, or converting scattered doc findings into an execution backlog.
---

# HXY Product Doc Gap Planner

## Overview
Plan product-document closure from domain truth instead of ad hoc doc requests.

## Required Input
- capability status ledger and domain doc coverage matrix
- doc-gap closure index and release decision pack
- current business-domain scope or release batch

## Workflow
1. Read the deliverable catalog and domain priority matrix.
2. Start from business-domain truth, not from whatever doc happens to exist.
3. Score missing PRD, contract, errorcode, degrade, SOP, and runbook coverage.
4. Split missing work into `P0`, `P1`, and `P2` by release impact.
5. Output exact doc filenames, owners, and sequencing.

## Quick Run
```bash
bash .codex/skills/hxy-product-doc-gap-planner/scripts/scan_product_doc_gaps.sh . member
```

## Deliverables
- domain-level gap table
- prioritized doc backlog
- recommended window split for parallel execution

## Quality Gates
- Product gap planning follows business domains, not random filenames.
- `ACTIVE` domains cannot hide missing PRD or recovery docs.
- Planned-only domains are not escalated above active-release blockers without cause.

## References
- `references/deliverable-catalog.md`
- `references/domain-priority-matrix.md`
- `references/planning-template.md`
