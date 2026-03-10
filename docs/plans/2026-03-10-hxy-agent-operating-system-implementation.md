# HXY Agent Operating System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Integrate the HXY skill methodology into the current project so recurring miniapp governance work runs through reusable skills, stable references, and fixed validation.

**Architecture:** Keep the existing six domain/governance skills as the execution core, then add a small routing layer, a handoff normalization layer, and a durable engineering-memory document. Avoid a giant all-in-one skill; prefer progressive disclosure plus script-backed evidence.

**Tech Stack:** Markdown, YAML, Bash, ripgrep, git, existing HXY docs and skill folders.

---

### Task 1: Write durable project-memory docs

**Files:**
- Create: `docs/plans/2026-03-10-hxy-agent-operating-system-design.md`
- Create: `docs/plans/2026-03-10-hxy-agent-operating-system-implementation.md`
- Create: `hxy/05_engineering/HXY-AI代理操作系统与项目技能编排-v1-2026-03-10.md`
- Modify: `hxy/README.md`

**Step 1: Capture the operating model**
Write the method adaptation, canonical inputs, success metrics, and workflow composition map.

**Step 2: Add project-memory entry points**
Expose the new operating-system doc in `hxy/README.md` so future contributors can find it.

**Step 3: Verify docs exist**
Run: `ls docs/plans/2026-03-10-hxy-agent-operating-system-*.md hxy/05_engineering/HXY-AI代理操作系统与项目技能编排-v1-2026-03-10.md`
Expected: all files exist.

### Task 2: Add a router skill for composition and discovery

**Files:**
- Create: `.codex/skills/hxy-agent-workflow-router/SKILL.md`
- Create: `.codex/skills/hxy-agent-workflow-router/agents/openai.yaml`
- Create: `.codex/skills/hxy-agent-workflow-router/references/skill-trigger-matrix.md`
- Create: `.codex/skills/hxy-agent-workflow-router/references/workflow-composition-map.md`
- Create: `.codex/skills/hxy-agent-workflow-router/references/evaluation-baseline.md`
- Create: `.codex/skills/hxy-agent-workflow-router/references/window-brief-template.md`
- Create: `.codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh`

**Step 1: Encode trigger routing**
Map common HXY requests to the minimum correct skill set.

**Step 2: Encode composite workflows**
Document skill chains for document closure, release gating, booking alignment, member closure, and health-data reviews.

**Step 3: Add a smoke check**
Create a shell script that verifies the required skill suite and operating docs exist.

**Step 4: Verify router structure**
Run: `find .codex/skills/hxy-agent-workflow-router -maxdepth 3 -type f | sort`
Expected: skill, yaml, references, and script all exist.

### Task 3: Add a handoff normalization skill for A/B/C/D windows

**Files:**
- Create: `.codex/skills/hxy-window-handoff-normalizer/SKILL.md`
- Create: `.codex/skills/hxy-window-handoff-normalizer/agents/openai.yaml`
- Create: `.codex/skills/hxy-window-handoff-normalizer/references/response-format.md`
- Create: `.codex/skills/hxy-window-handoff-normalizer/references/window-role-matrix.md`
- Create: `.codex/skills/hxy-window-handoff-normalizer/references/handoff-checklist.md`

**Step 1: Freeze the response shape**
Document the fixed report format used by windows A/B/C/D.

**Step 2: Encode role differences**
Explain what each window must contribute and what A-window integration requires.

**Step 3: Verify handoff skill structure**
Run: `find .codex/skills/hxy-window-handoff-normalizer -maxdepth 3 -type f | sort`
Expected: skill, yaml, and references exist.

### Task 4: Verify the integrated suite

**Files:**
- Modify: all new files above as needed

**Step 1: Run skill-suite smoke checks**
Run:
```bash
bash .codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh .
```
Expected: required skills and operating docs are reported as present.

**Step 2: Run repo checks**
Run:
```bash
git diff --check
git diff --cached --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
```
Expected: all commands pass.

**Step 3: Commit only intended files**
Do not stage unrelated tracked modifications.

**Step 4: Commit**
```bash
git add docs/plans/2026-03-10-hxy-agent-operating-system-design.md \
  docs/plans/2026-03-10-hxy-agent-operating-system-implementation.md \
  hxy/05_engineering/HXY-AI代理操作系统与项目技能编排-v1-2026-03-10.md \
  hxy/README.md

git add -f .codex/skills/hxy-agent-workflow-router .codex/skills/hxy-window-handoff-normalizer

git commit -m "feat(skills): integrate hxy agent operating system"
```
