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

## Errorcode Governance Quality
- Frozen docs `TBD_*` error markers = 0
- Product or SOP docs branching by message text = 0
- Codes mentioned outside canonical register without explicit approval = 0

## Product Planning Quality
- Active domains with missing PRD/recovery planning hidden from backlog = 0
- P0/P1/P2 sequencing without capability truth evidence = 0

## UI Review Quality
- UI-to-PRD outputs missing empty/error/degraded states = 0
- UI-only ideas promoted to active release without route/API truth = 0

## Release Gate Quality
- `degraded_pool` counted into main KPI/ROI = 0
- Reserved-disabled leakage downgraded to warning = 0

## Verification Commands
```bash
bash .codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh .
bash ruoyi-vue-pro-master/script/dev/check_hxy_agent_suite_guard.sh
git diff --check
git diff --cached --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
```
