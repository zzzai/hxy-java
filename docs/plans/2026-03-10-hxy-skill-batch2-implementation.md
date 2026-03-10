# HXY Skill Batch 2 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add the second batch of HXY project skills and connect the HXY agent-suite smoke check into CI-required governance.

**Architecture:** Reuse the existing skill-suite pattern: concise `SKILL.md`, project-specific references, and small deterministic scripts. Update the router and engineering-memory docs so the suite remains discoverable and versioned.

**Tech Stack:** Markdown, YAML, Bash, ripgrep, git, existing HXY docs and workflow scripts.

---

### Task 1: Add second-batch skills

**Files:**
- Create: `.codex/skills/hxy-errorcode-governor/...`
- Create: `.codex/skills/hxy-product-doc-gap-planner/...`
- Create: `.codex/skills/hxy-ui-review-to-prd/...`

**Step 1: Write concise skill bodies**
Keep descriptions trigger-only and align content with current project workflows.

**Step 2: Add project references and small scan scripts**
Each skill gets references and one lightweight helper for deterministic evidence gathering.

**Step 3: Verify structure**
Run: `find .codex/skills/hxy-errorcode-governor .codex/skills/hxy-product-doc-gap-planner .codex/skills/hxy-ui-review-to-prd -maxdepth 3 -type f | sort`
Expected: each skill has `SKILL.md`, `agents/openai.yaml`, references, and a helper script.

### Task 2: Integrate the new skills into the agent operating system

**Files:**
- Modify: `.codex/skills/hxy-agent-workflow-router/references/skill-trigger-matrix.md`
- Modify: `.codex/skills/hxy-agent-workflow-router/references/workflow-composition-map.md`
- Modify: `.codex/skills/hxy-agent-workflow-router/references/evaluation-baseline.md`
- Modify: `.codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh`
- Modify: `hxy/05_engineering/HXY-AI代理操作系统与项目技能编排-v1-2026-03-10.md`

**Step 1: Update routing and composition**
Expose the new skills in trigger and composition docs.

**Step 2: Update smoke-check scope**
The suite guard must now require the second-batch skills as part of project truth.

**Step 3: Update engineering memory**
Record the expanded skill map and evaluation scope in HXY engineering memory.

### Task 3: Connect agent-suite guard into CI governance

**Files:**
- Create: `ruoyi-vue-pro-master/script/dev/check_hxy_agent_suite_guard.sh`
- Create: `ruoyi-vue-pro-master/.github/workflows/hxy-agent-suite-guard.yml`
- Modify: `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`

**Step 1: Add a wrapper script**
Bridge existing workflow/script conventions to the repo-root skill suite.

**Step 2: Add a required-check workflow**
Mirror naming/memory guard style and run the wrapper script.

**Step 3: Register the required context**
Add `hxy-agent-suite-guard / agent-suite-guard` to the default required checks set.

### Task 4: Verify and commit

**Files:**
- Modify: all new files above as needed

**Step 1: Run smoke and repo checks**
Run:
```bash
bash .codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh .
bash ruoyi-vue-pro-master/script/dev/check_hxy_agent_suite_guard.sh
git diff --check
git diff --cached --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
```
Expected: all commands pass.

**Step 2: Stage only intended files**
Do not include unrelated tracked modifications.

**Step 3: Commit**
```bash
git add docs/plans/2026-03-10-hxy-skill-batch2-design.md \
  docs/plans/2026-03-10-hxy-skill-batch2-implementation.md \
  hxy/05_engineering/HXY-AI代理操作系统与项目技能编排-v1-2026-03-10.md \
  ruoyi-vue-pro-master/script/dev/check_hxy_agent_suite_guard.sh \
  ruoyi-vue-pro-master/.github/workflows/hxy-agent-suite-guard.yml \
  ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh

git add -f .codex/skills/hxy-errorcode-governor \
  .codex/skills/hxy-product-doc-gap-planner \
  .codex/skills/hxy-ui-review-to-prd \
  .codex/skills/hxy-agent-workflow-router

git commit -m "feat(skills): add hxy batch2 skills and agent suite guard"
```
