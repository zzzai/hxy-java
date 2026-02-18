# 仓库同步规则（hxy-java）

- 生效日期：2026-02-18
- 目标仓库：`https://github.com/zzzai/hxy-java.git`
- 适用范围：本机 `root` 仓库 + `crmeb_java` 内层仓库

## 1. 双仓库事实（必须先明确）

1. 外层仓库（业务主仓）  
路径：`/root/crmeb-java`  
默认分支：`main`  
远端：`origin=https://github.com/zzzai/hxy-java.git`

2. 内层仓库（CRMEB 源码仓）  
路径：`/root/crmeb-java/crmeb_java`  
默认分支：`1.4`  
远端：`origin=https://github.com/crmeb/crmeb_java.git`

3. 风险点  
外层仓库把 `crmeb_java` 记录为 gitlink（子仓库指针）。  
如果直接推外层而内层提交并未在可访问远端存在，会导致其他环境无法正确拉取该指针。

## 2. 凭证与安全规则

1. token 只允许保存在本机环境变量或 Git 凭证缓存，不写入仓库文件。  
2. 禁止把 token 放进脚本、Markdown、`.env` 并提交。  
3. 若 token 在聊天或截图中暴露，立即到 GitHub 撤销并重建。  
4. 推荐使用临时缓存（8小时）：

```bash
export GITHUB_USER='zzzai'
export GITHUB_TOKEN='<new_token>'
git config --global credential.helper 'cache --timeout=28800'
printf "protocol=https\nhost=github.com\nusername=%s\npassword=%s\n\n" "$GITHUB_USER" "$GITHUB_TOKEN" | git credential approve
unset GITHUB_TOKEN
```

可选：使用本地模板  
`hxy/tools/scripts/.repo_sync.env.example` -> 复制为 `.repo_sync.env`（已忽略，不会入库）。

使用方式：

```bash
set -a
source /root/crmeb-java/hxy/tools/scripts/.repo_sync.env
set +a
```

## 3. 同步策略（默认）

1. 日常只推外层仓库 `main`。  
2. 内层仓库默认仅用于本地开发，不直接向官方 `crmeb` 远端推送。  
3. 若确需更新外层的 `crmeb_java` 指针，必须先确保内层 `HEAD` 已存在于可访问远端，再允许推送外层。  
4. 禁止 `git push --force` 到 `main`。  
5. 同步顺序（`all` 场景）：先内层、后外层。

## 4. 执行门槛（DoR/DoD）

1. 同步前必须通过：
- 分支正确（root=main, inner=1.4）
- 工作区干净（无未提交变更）
- `fetch --prune` 正常

2. 推送前必须额外通过：
- 外层 gitlink 变更策略校验（默认阻断）
- 必要的本地 smoke/CI 门槛已通过

## 5. 一键脚本（已落地）

脚本：`hxy/tools/scripts/repo_sync.sh`

### 5.1 仅检查（推荐每次开发前执行）

```bash
/root/crmeb-java/hxy/tools/scripts/repo_sync.sh --target all --mode check
```

若网络或鉴权不稳定，可限制单条 Git 命令超时时间：

```bash
/root/crmeb-java/hxy/tools/scripts/repo_sync.sh --target all --mode check --cmd-timeout-sec 20
```

### 5.2 同步（拉取并 rebase）

```bash
/root/crmeb-java/hxy/tools/scripts/repo_sync.sh --target root --mode sync
```

### 5.3 推送外层仓库

```bash
/root/crmeb-java/hxy/tools/scripts/repo_sync.sh --target root --mode push
```

### 5.4 确认推送 gitlink（仅特殊场景）

```bash
/root/crmeb-java/hxy/tools/scripts/repo_sync.sh \
  --target root \
  --mode push \
  --allow-submodule-pointer
```

## 6. 异常处理

1. 提示“工作区不干净”  
- 先 `git status`，提交或暂存后再同步。

2. 提示“分支不匹配”  
- 切回规则分支：root=`main`，inner=`1.4`。

3. 提示“gitlink 变更被阻断”  
- 若非必须，撤销该指针变更；若必须，先发布内层提交到可访问远端，再加 `--allow-submodule-pointer`。
