# HXY Skill Batch 2 Design

**Date:** 2026-03-10
**Branch:** `feat/ui-four-account-reconcile-ops`
**Scope:** Expand the HXY project skill system with second-batch skills and connect the agent-suite smoke check into CI-required governance.

## 1. Problem
The first skill batch solved capability truth, freeze closure, release gates, booking/member closure, and health-data compliance. The next recurring gaps are now clear:
- error code governance still requires repeated manual reconciliation across product, contract, and runbook docs
- product document gap planning still needs repeated domain-by-domain inventory and prioritization
- UI review outputs still need to be translated manually into PRD, field dictionary, and acceptance docs
- the current agent suite has a smoke-check script, but it is not yet enforced by CI-required checks

## 2. Decision
Add three focused skills and one CI guard path:
1. `hxy-errorcode-governor`
2. `hxy-product-doc-gap-planner`
3. `hxy-ui-review-to-prd`
4. `hxy-agent-suite-guard` workflow + wrapper script + required-check registration

## 3. Design Principles
1. Keep the new skills workflow-scoped, not org-scoped.
2. Keep frontmatter trigger-only and concise.
3. Put projectized rules into references, not long skill bodies.
4. Use deterministic scan scripts where drift can be measured.
5. Update the router and operating-system docs so the new skills are discoverable immediately.

## 4. Intended Outcomes
- errorcode drift can be governed from a canonical entry point
- product document planning can be repeated without rebuilding the inventory prompt
- UI review work can flow into PRD and acceptance assets with consistent structure
- CI can fail fast if the HXY agent operating system is missing core pieces
