# HXY Skill Suite Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build six HXY project-specific skills that operationalize miniapp capability auditing, doc freeze closure, release gating, domain alignment, and health-data compliance review.

**Architecture:** Each skill is a small folder under `.codex/skills/` with concise trigger metadata, reusable project references, and optional deterministic shell scripts. The first three skills govern project-wide truth and release decisions; the remaining three specialize domain alignment and compliance review.

**Tech Stack:** Markdown, YAML, Bash, ripgrep, existing HXY docs, local git workspace.

---

### Task 1: Create plan-stage documentation

**Files:**
- Create: `docs/plans/2026-03-10-hxy-skill-suite-design.md`
- Create: `docs/plans/2026-03-10-hxy-skill-suite-implementation.md`

**Step 1: Capture design constraints**
Write the stable problem statement, canonical sources, target skills, and verification expectations.

**Step 2: Save implementation plan**
Document exact skill paths and supporting file categories before creating any skills.

**Step 3: Verify docs exist**
Run: `ls docs/plans/2026-03-10-hxy-skill-suite-*.md`
Expected: both plan files exist.

### Task 2: Create project-wide governance skills

**Files:**
- Create: `.codex/skills/hxy-miniapp-capability-auditor/SKILL.md`
- Create: `.codex/skills/hxy-miniapp-capability-auditor/agents/openai.yaml`
- Create: `.codex/skills/hxy-miniapp-capability-auditor/references/canonical-sources.md`
- Create: `.codex/skills/hxy-miniapp-capability-auditor/references/output-template.md`
- Create: `.codex/skills/hxy-miniapp-capability-auditor/scripts/scan_capability_truth.sh`
- Create: `.codex/skills/hxy-miniapp-doc-freeze-closer/...`
- Create: `.codex/skills/hxy-release-gate-decider/...`

**Step 1: Write concise SKILL.md files**
Keep descriptions trigger-only and align body structure to existing repo skills.

**Step 2: Add deterministic references/scripts**
Provide checklists and shell scanners for route/API truth and freeze inputs.

**Step 3: Verify structure**
Run: `find .codex/skills/hxy-miniapp-capability-auditor .codex/skills/hxy-miniapp-doc-freeze-closer .codex/skills/hxy-release-gate-decider -maxdepth 2 -type f | sort`
Expected: each skill has `SKILL.md` and supporting files.

### Task 3: Create booking and member closure skills

**Files:**
- Create: `.codex/skills/hxy-booking-contract-alignment/...`
- Create: `.codex/skills/hxy-member-domain-closer/...`

**Step 1: Encode real project mismatch patterns**
Bake in current booking method/path drift and member route-truth drift as first-class checks.

**Step 2: Add comparison scripts**
Create lightweight shell scripts that surface booking and member closure gaps using `rg`.

**Step 3: Verify structure**
Run: `find .codex/skills/hxy-booking-contract-alignment .codex/skills/hxy-member-domain-closer -maxdepth 2 -type f | sort`
Expected: both skills exist with references and scripts.

### Task 4: Create health-data compliance guard skill

**Files:**
- Create: `.codex/skills/hxy-health-data-compliance-guard/...`

**Step 1: Encode privacy red lines**
Capture G0/G1/G2 rules, abstract-tag-only policy, and audit-field requirements.

**Step 2: Add compliance scan helper**
Create a shell script that searches for likely health-data red-flag patterns in docs and code.

**Step 3: Verify structure**
Run: `find .codex/skills/hxy-health-data-compliance-guard -maxdepth 2 -type f | sort`
Expected: skill and references/scripts exist.

### Task 5: Verify and commit

**Files:**
- Modify: any newly created skill paths
- Modify: `docs/plans/2026-03-10-hxy-skill-suite-design.md`
- Modify: `docs/plans/2026-03-10-hxy-skill-suite-implementation.md`

**Step 1: Run repository validation**
Run:
```bash
git diff --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
```
Expected: all commands pass.

**Step 2: Stage only intended files**
Do not include unrelated tracked changes.

**Step 3: Commit**
```bash
git add docs/plans/2026-03-10-hxy-skill-suite-design.md \
  docs/plans/2026-03-10-hxy-skill-suite-implementation.md \
  .codex/skills/hxy-miniapp-capability-auditor \
  .codex/skills/hxy-miniapp-doc-freeze-closer \
  .codex/skills/hxy-release-gate-decider \
  .codex/skills/hxy-booking-contract-alignment \
  .codex/skills/hxy-member-domain-closer \
  .codex/skills/hxy-health-data-compliance-guard

git commit -m "feat(skills): add hxy miniapp governance skill suite"
```
