# Window C Handoff - Ops StageB/P1 Required Checks Rollout

- Date: 2026-03-06
- Branch: `feat/ui-four-account-reconcile-ops` (continue work on current branch)
- Scope: StageB/P1 required checks 启用能力收口

## Changes

1. `setup_github_required_checks.sh`
- 新增 `--repo-owner` / `--repo-name` 显式参数（兼容旧参数 `--owner` / `--repo`）。
- 新增快捷开关 `--enable-ops-stageb-p1`（等价 `INCLUDE_OPS_STAGEB_CHECKS=1`）。
- 输出两类可直接执行的 `gh api` 命令：
  - `gh_dry_run_cmd`（GET 当前分支保护）
  - `gh_apply_cmd`（PUT 应用 payload）
- 输出 `payload_cmd_file`（持久化到 `/tmp/github_required_checks_<owner>_<repo>_<branch>.json`）。
- `--dry-run 0` 时优先走 `gh api`；未安装 gh 则回退到 `curl + GITHUB_TOKEN`。
- 增强日志输出：
  - `profile=stagea+stageb|stagea-only|base-only`
  - dry-run 下额外打印手工执行 `gh_apply_cmd`。

2. 新增运维文档
- `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md`
- 覆盖：启用前检查、启用命令、回滚命令、常见失败处理（含 token scope/read:org）。

## Verification

- `git diff --check` => PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 check_hxy_naming_guard.sh` => PASS (`[naming-guard] result=PASS checked_files=209`)
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 check_hxy_memory_guard.sh` => PASS (`[hxy-memory-guard] result=PASS checked_files=209 core_domains=0`)
- `run_ops_stageb_p1_local_ci.sh --skip-mysql-init` => PASS (`result=PASS`)

## Commits

- `9c93f1c922` feat: improve ops stageb required checks rollout tooling
- `0fea8f6b6a` chore: refine required checks rollout logging
