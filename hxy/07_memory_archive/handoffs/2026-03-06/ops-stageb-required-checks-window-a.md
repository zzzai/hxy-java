# Ops StageB Required Checks - Window A Handoff

## 1. 变更摘要

1. `setup_github_required_checks.sh` 已具备 StageB 可控接入所需参数与行为：
   - `--repo-owner`、`--repo-name`
   - `--enable-ops-stageb-p1`
   - `--dry-run`（只打印）
   - `--apply`（真正写入）
2. setup 脚本支持：
   - 未传 owner/repo 时自动从 `git remote origin` 推断；
   - 输出完整 `gh api` dry-run/apply 命令和 payload 路径；
   - `base-only`、`stagea-only`、`stagea+stageb` 检查集 profile；
   - context 去重，避免重复 required check。
3. 新增运维脚本：
   - `script/dev/apply_ops_stageb_required_checks.sh`：启用 stageA+stageB。
   - `script/dev/rollback_ops_stageb_required_checks.sh`：回滚 StageB（保留 StageA）。
4. 新增落地文档：
   - `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md`。
5. 治理同步：
   - 事实基线、ADR、执行看板已更新本批变更。

## 2. 启用命令

```bash
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh \
  --dry-run \
  --include-stagea-checks 1 \
  --enable-ops-stageb-p1
```

```bash
bash ruoyi-vue-pro-master/script/dev/apply_ops_stageb_required_checks.sh
```

## 3. 回滚命令

```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```

## 4. 风险与注意事项

1. `gh auth status` 若缺 `read:org`，组织仓库场景可能读不到分支保护信息；需先补授权。
2. apply 前必须先做 dry-run 审核 context 清单，确保不误删现有 stageA checks。
3. 目标仓库默认分支若非 `main`，执行时需显式 `--branch <name>`。
4. 若运行环境无 `gh`，setup 脚本可走 `GITHUB_TOKEN + curl` 回退路径，但 apply/rollback 脚本的“分支保护摘要”会受限。
