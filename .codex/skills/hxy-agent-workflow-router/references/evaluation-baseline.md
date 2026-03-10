# Evaluation Baseline

## Trigger Quality
- Trigger correctness >= 90% on 20 representative prompts.
- Wrong-skill dispatch on irrelevant prompts = 0 for the core six governance tasks.

## Truth and Freeze Quality
- Frozen docs wildcard APIs = 0
- Frozen docs `TBD_*` error codes = 0
- Frozen docs prototype alias routes = 0
- `RESERVED_DISABLED` misclassified as `ACTIVE` = 0

## Domain Drift Detection
- Known booking drift samples detected = 100%
- Known member drift samples detected = 100%

## Release Gate Quality
- `degraded_pool` counted into main KPI/ROI = 0
- Reserved-disabled leakage downgraded to warning = 0

## Verification Commands
```bash
bash .codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh .
git diff --check
git diff --cached --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
```
