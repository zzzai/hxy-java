---
name: hxy-agent-workflow-router
description: Use when deciding which HXY governance skill or skill sequence applies to capability audits, doc freeze closure, release gating, errorcode governance, product doc-gap planning, UI review to PRD, booking/member alignment, health-data compliance, or A/B/C/D window dispatch.
---

# HXY Agent Workflow Router

## Overview
Route HXY work to the minimum correct skill chain instead of rebuilding long prompts every round.

## Required Input
- task goal and target batch/date
- affected domain or business scope
- whether the task is single-window or A/B/C/D parallel work
- current canonical docs and local repo access

## Workflow
1. Read the trigger matrix to classify the request.
2. Select the smallest valid skill set and execution order.
3. Use the workflow composition map for multi-step or multi-domain work.
4. Use the window brief template when dispatching parallel windows.
5. End with the evaluation baseline and repository guards.

## Quick Run
```bash
bash .codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh .
```

## Deliverables
- chosen skill chain
- concise execution brief
- verification list and success criteria

## Quality Gates
- Do not replace specialist skills with one giant prompt.
- Do not skip code/doc truth checks before freeze or release judgment.
- All windows use the fixed handoff format.

## References
- `references/skill-trigger-matrix.md`
- `references/workflow-composition-map.md`
- `references/evaluation-baseline.md`
- `references/window-brief-template.md`
