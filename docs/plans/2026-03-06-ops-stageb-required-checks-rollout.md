# Ops StageB/P1 Required Checks Rollout

## 1. 启用前检查

1. 本地确认 workflow 已在默认分支：
```bash
git checkout main
git pull --ff-only
ls .github/workflows/ops-stageb-p1-guard.yml
```

2. 确认 required checks 脚本参数可用：
```bash
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh --help
```

3. 确认 `gh` 登录状态与仓库权限：
```bash
gh auth status
gh repo view <owner>/<repo> --json nameWithOwner,defaultBranchRef
```

4. 建议先看当前分支保护：
```bash
gh api /repos/<owner>/<repo>/branches/main/protection -H "Accept: application/vnd.github+json"
```

## 2. 启用命令

### 2.1 预演（Dry Run，不写入）

```bash
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh \
  --repo-owner <owner> \
  --repo-name <repo> \
  --branch main \
  --enable-ops-stageb-p1 \
  --include-stagea-checks 1 \
  --dry-run 1
```

说明：
- 该命令会输出：
  - `payload_cmd_file`（可复用 payload 文件）
  - `gh_dry_run_cmd`（读取当前保护配置）
  - `gh_apply_cmd`（可直接执行的写入命令）

### 2.2 正式启用（Apply）

方式 A：直接用脚本写入
```bash
GITHUB_TOKEN=<token> \
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh \
  --repo-owner <owner> \
  --repo-name <repo> \
  --branch main \
  --enable-ops-stageb-p1 \
  --include-stagea-checks 1 \
  --dry-run 0
```

方式 B：复制脚本输出的 `gh_apply_cmd` 直接执行
```bash
# 示例（以脚本实际输出为准）
gh api --method PUT "/repos/<owner>/<repo>/branches/main/protection" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  --input "/tmp/github_required_checks_<owner>_<repo>_main.json"
```

## 3. 回滚命令

目标：仅撤销 ops-stageb-p1 required check，保留现有 stageA 检查。

```bash
GITHUB_TOKEN=<token> \
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh \
  --repo-owner <owner> \
  --repo-name <repo> \
  --branch main \
  --include-stagea-checks 1 \
  --include-ops-stageb-checks 0 \
  --dry-run 0
```

如需完全回到默认 checks（不包含 stageA/ops-stageb），去掉 `--include-stagea-checks 1` 即可。

## 4. 常见失败与处理

### 4.1 `Resource not accessible by integration` / 403

原因：
- Token 无分支保护写权限，或并非仓库管理员角色。

处理：
1. 检查 Token 绑定身份是否对仓库有 admin 权限。
2. 重新生成 PAT 并授予 `repo`（私有仓库）或对应管理权限。
3. 重新执行 dry-run，再执行 apply。

### 4.2 `read:org` 相关错误（组织资源不可读）

原因：
- 使用 SSO/组织策略时，当前 token 未授权组织读取，`gh auth status` 会报组织访问不足。

处理：
1. 在 GitHub 组织 SSO 页面授权该 token。
2. 对 classic token 补齐组织读取权限（常见为 `read:org`）。
3. 重新登录并校验：
```bash
gh auth status
gh api /user/orgs
```

### 4.3 `gh: not found`

原因：
- 运行机未安装 GitHub CLI。

处理：
1. 安装 `gh` 后重试；或
2. 使用脚本内置 `curl + GITHUB_TOKEN` 回退路径执行 apply。

### 4.4 分支保护接口 404

原因：
- 仓库、分支名错误，或 token 无访问目标仓库权限。

处理：
```bash
gh repo view <owner>/<repo>
gh api /repos/<owner>/<repo>/branches/main
```

