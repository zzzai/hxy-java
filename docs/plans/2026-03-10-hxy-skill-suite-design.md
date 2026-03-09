# HXY Skill Suite Design

**Date:** 2026-03-10
**Branch:** `feat/ui-four-account-reconcile-ops`
**Scope:** Create six project-specific skills that turn recurring HXY governance and release workflows into reusable agent workflows.

## 1. Problem
The current project already has strong governance artifacts, but execution still depends on long prompt handoffs and repeated human restatement of rules. The recurring failures are stable:
- false `ACTIVE` classification
- route alias drift between docs and real uniapp pages
- `Ready` vs `Frozen` ambiguity
- wildcard / non-canonical API references
- domain closure work repeated window by window
- health-data compliance red lines enforced manually

## 2. Design Goal
Build a compact HXY skill suite that makes the project rules discoverable and executable with low prompt overhead.

## 3. Skill Set
1. `hxy-miniapp-capability-auditor`
2. `hxy-miniapp-doc-freeze-closer`
3. `hxy-release-gate-decider`
4. `hxy-booking-contract-alignment`
5. `hxy-member-domain-closer`
6. `hxy-health-data-compliance-guard`

## 4. Architecture
Each skill follows the same structure:
- `SKILL.md`: trigger conditions, workflow, deliverables, quality gates
- `agents/openai.yaml`: UI metadata aligned to existing local skills
- `references/`: project-specific source-of-truth lists, checklists, templates
- `scripts/`: lightweight deterministic scanners for route/API/compliance checks where natural language is too fragile

## 5. Canonical Sources
The suite will standardize around existing HXY truth artifacts:
- `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
- `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
- `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
- `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
- `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
- `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`
- `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
- `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`

## 6. Intended Outcomes
- Reduce repeated prompt boilerplate for A/B/C/D style work
- Make route/API truth checks scriptable and reusable
- Keep 03-09 frozen baseline stable while enabling 03-10 closure work
- Convert health-data red lines into explicit, reusable review logic

## 7. Constraints
- No business code or overlay UI changes in this batch
- Must fit existing `.codex/skills` conventions
- Must stay concise enough for skill discovery and low context cost
- Must support local repo-first execution with shell and ripgrep

## 8. Verification Plan
- Structural validation: every skill has `SKILL.md`; names are kebab-case
- Content validation: descriptions start with `Use when...` and describe trigger conditions only
- Repo validation: `git diff --check`, naming guard, memory guard
