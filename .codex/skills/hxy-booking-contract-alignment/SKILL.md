---
name: hxy-booking-contract-alignment
description: Use when booking routes or APIs drift between frontend, backend, and docs, especially for technician, slot, cancel, addon, and refund replay flows.
---

# HXY Booking Contract Alignment

## Overview
Resolve booking truth drift before anyone marks booking features active or frozen.

## Required Input
- `yudao-mall-uniapp/sheep/api/trade/booking.js`
- app booking controllers
- booking PRD, canonical API docs, and capability ledger

## Workflow
1. Compare frontend booking methods and paths against backend app controllers.
2. Check route truth in `pages/booking/*` and related release docs.
3. Separate query-only active paths from create/cancel/addon paths still drifting.
4. Emit exact mismatches with file and line references.
5. Propose doc-side corrections or code-side blockers without hand-waving.
6. Feed findings into capability ledger, API matrix, and release gates.

## Quick Run
```bash
bash .codex/skills/hxy-booking-contract-alignment/scripts/check_booking_alignment.sh .
```

## Deliverables
- Booking mismatch table
- Active vs blocked booking scope
- A/B/C/D integration notes for booking domain

## Quality Gates
- No booking path is marked active when method/path still drifts.
- `addon` remains blocked if it still depends on reserved or mismatched path semantics.
- Conflict and replay codes remain code-driven, never message-driven.

## References
- `references/booking-route-truth.md`
