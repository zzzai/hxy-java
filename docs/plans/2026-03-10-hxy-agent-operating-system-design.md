# HXY Agent Operating System Design

**Date:** 2026-03-10
**Branch:** `feat/ui-four-account-reconcile-ops`
**Scope:** Integrate Anthropic skill methodology into HXY as a durable project operating system for miniapp document closure, release governance, domain alignment, and compliance review.

## 1. Problem
The repository already has strong product, contract, release, and memory artifacts, but execution still depends on long prompts and repeated human orchestration. The repeated failure modes are stable:
- A/B/C/D prompts are rewritten instead of reused.
- `ACTIVE`, `PLANNED_RESERVED`, and `DEPRECATED` drift from code truth.
- `Ready` and `Frozen` are confused with "docs exist" instead of "gates closed".
- Booking and member domains need repeated manual alignment checks.
- High-risk health-data reviews still rely on ad hoc reading.
- Window handoff format drifts, making A-window integration expensive.

## 2. Design Goal
Turn current HXY governance knowledge into a project-specific agent operating system with four properties:
- discoverable: agents can find the right workflow with a small trigger surface
- composable: skills chain together instead of becoming a giant prompt
- verifiable: critical truth checks are backed by scripts and fixed evidence
- durable: rules live in tracked project memory, not only in current-session prompts

## 3. Options Considered
### Option A: Keep long prompts and only add more reference docs
Pros:
- lowest immediate edit cost

Cons:
- repeated prompt authoring remains
- trigger quality stays human-dependent
- no stable routing layer
- no fixed evaluation baseline

### Option B: Build one "HXY super skill"
Pros:
- single entry point

Cons:
- too heavy for discovery
- poor progressive disclosure
- high risk of over-triggering
- mixes unrelated workflows into one context blob

### Option C: Modular skill suite plus router and handoff normalizer
Pros:
- matches repeated workflows instead of org chart
- keeps specialized skills small
- enables composition by task chain
- supports stable window prompts and fixed outputs
- easy to expand with future product or errorcode governance skills

Cons:
- requires one more layer of documentation and indexing

## 4. Decision
Choose Option C.

The existing six HXY skills remain the execution core. This batch adds:
- a lightweight router skill for trigger mapping and workflow composition
- a handoff normalizer skill for A/B/C/D collaboration discipline
- a durable engineering doc that explains how the project uses progressive disclosure, composability, projectized success metrics, and script-first verification
- plan docs and index updates so the skill system is part of project memory

## 5. Target Artifacts
1. `hxy/05_engineering/HXY-AI代理操作系统与项目技能编排-v1-2026-03-10.md`
2. `docs/plans/2026-03-10-hxy-agent-operating-system-design.md`
3. `docs/plans/2026-03-10-hxy-agent-operating-system-implementation.md`
4. `.codex/skills/hxy-agent-workflow-router/...`
5. `.codex/skills/hxy-window-handoff-normalizer/...`
6. `hxy/README.md` update to expose the new operating-system doc

## 6. Canonical Inputs
The operating system will explicitly route around current project truth:
- `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
- `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
- `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
- `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
- `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
- `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`
- `docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md`
- `docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md`
- `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
- `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`

## 7. Operating Principles
1. Progressive disclosure
   - frontmatter only states when a skill should be used
   - `SKILL.md` contains workflow skeleton
   - detailed rules move to `references/`
   - repeatable checks move to `scripts/`
2. Composability
   - common chains are explicit, such as capability audit -> freeze closure -> release gate
3. Projectized success metrics
   - zero wildcard APIs in frozen docs
   - zero `TBD_*` error codes in frozen docs
   - zero false `ACTIVE` for reserved features
   - 100% detection on known booking/member drift samples
4. Script-first critical validation
   - route truth, API truth, controller truth, and skill-suite health are scriptable
5. Durable memory
   - all agent-governance rules must be recoverable from tracked files

## 8. Constraints
- no overlay or business-code edits
- do not touch unrelated tracked modification under `hxy/04_data/`
- keep skill descriptions trigger-only and concise
- maintain compatibility with existing `.codex/skills` conventions

## 9. Verification Strategy
- structural: every new skill has `SKILL.md`, `agents/openai.yaml`, and references
- smoke: router script confirms required skill and doc artifacts exist
- repo: `git diff --check`, naming guard, memory guard
