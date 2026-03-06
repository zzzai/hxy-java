# Ops StageB/P1 Required Checks Rollout

## 1. 前置条件

1. 仓库与分支
   - 目标仓库可见且可访问（如 `zzzai/hxy-java`）。
   - 目标分支存在（默认 `main`）。
2. GitHub CLI 与认证
   - 本机可执行 `gh`。
   - `gh auth status` 可见有效账号。
   - Token 至少具备 `repo`，组织仓库建议补 `read:org`。
3. 脚本与 workflow 已在仓库
   - `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`
   - `ruoyi-vue-pro-master/script/dev/apply_ops_stageb_required_checks.sh`
   - `ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh`
   - `.github/workflows/ops-stageb-p1-guard.yml`

## 2. 启用（Dry Run + Apply）

### 2.1 Dry Run（只打印，不写入）

```bash
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh \
  --dry-run \
  --include-stagea-checks 1 \
  --enable-ops-stageb-p1
```

说明：
- 未传 `--repo-owner/--repo-name` 时自动从 `git remote origin` 解析。
- 会输出可审计信息：`gh_dry_run_cmd`、`gh_apply_cmd`、payload 文件路径。
- 检查集会显示 profile：
  - `base-only`
  - `stagea-only`
  - `stagea+stageb`

### 2.2 Apply（正式写入）

```bash
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh \
  --apply \
  --include-stagea-checks 1 \
  --enable-ops-stageb-p1
```

或使用一键脚本（先 dry-run 再 apply，再打印分支保护摘要）：

```bash
bash ruoyi-vue-pro-master/script/dev/apply_ops_stageb_required_checks.sh
```

显式仓库：

```bash
bash ruoyi-vue-pro-master/script/dev/apply_ops_stageb_required_checks.sh \
  --repo-owner <owner> \
  --repo-name <repo> \
  --branch main
```

## 3. 回滚（仅移除 StageB，保留 StageA）

```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```

显式仓库：

```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh \
  --repo-owner <owner> \
  --repo-name <repo> \
  --branch main
```

说明：
- 回滚脚本会保留基础 checks + stageA checks，仅移除 `hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`。
- 执行后会打印当前分支保护 required contexts 摘要。

## 4. 常见错误与处理

### 4.1 `gh auth status` 提示缺少 `read:org`

原因：
- 组织仓库或 SSO 场景下，token 未授权组织读取。

处理：
1. 执行 `gh auth refresh -h github.com -s read:org`。
2. 若启用 SSO，到组织授权页面完成授权。
3. 复核：

```bash
gh auth status
gh api /user/orgs
```

### 4.2 `Resource not accessible by integration` / 403

原因：
- 当前账号或 token 没有分支保护写权限。

处理：
1. 确认操作者对目标仓库有 admin 级权限。
2. 重新认证并重跑 dry-run 后再 apply。

### 4.3 `404 Not Found`（分支保护 API）

原因：
- owner/repo/branch 填写错误，或仓库可见性权限不足。

处理：

```bash
gh repo view <owner>/<repo> --json nameWithOwner,visibility,defaultBranchRef
gh api /repos/<owner>/<repo>/branches/main
```

### 4.4 运行环境缺少 `gh`

处理：
1. 安装 GitHub CLI 后重试；或
2. 在 `setup_github_required_checks.sh --apply` 场景使用 `GITHUB_TOKEN` 走 `curl` 回退路径。

## 5. 快速检查命令

```bash
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh --help
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh --dry-run --enable-ops-stageb-p1
```

## 6. 巡检接口回归测试（退款-提成联调）

### 6.1 本地执行

`run_ops_stageb_p1_local_ci.sh` 默认已纳入巡检接口关键回归用例：
- `FourAccountReconcileServiceImplTest`
- `FourAccountReconcileControllerTest`

执行命令：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

如需临时覆盖回归测试集合，可通过环境变量：

```bash
REGRESSION_TEST_CLASSES=ProductStoreSkuControllerTest,ProductStoreServiceImplTest,AfterSaleReviewTicketServiceImplTest,FourAccountReconcileServiceImplTest,FourAccountReconcileControllerTest \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

### 6.2 CI 执行

`hxy-ops-stageb-p1-guard` workflow 在 `regression-tests` 步骤调用同一脚本与默认测试集合，确保本地与 CI 口径一致。
