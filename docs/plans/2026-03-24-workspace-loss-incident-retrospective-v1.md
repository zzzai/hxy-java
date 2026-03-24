# Workspace Loss Incident Retrospective v1

## 1. 事故摘要
- 事故名称：`/root/crmeb-java` 主工作目录完整性丢失事故
- 发现时间：2026-03-24
- 事故等级：`SEV-1`
- 当前结论：
  - 当前 `/root/crmeb-java` 已不是原始完整工作仓。
  - 原始完整仓的多个近似副本仍残留在 `/tmp` 目录。
  - 部分关键文档可从 `/tmp` 恢复。
  - 另有两批新文档当前未在本机可见路径中找到，需视为未恢复项。

## 2. 影响范围
- 直接影响：
  - 主工作目录缺失 `.git`、`docs/`、`hxy/` 等核心结构。
  - 无法在 `/root/crmeb-java` 继续按正常 git 仓库模式开发、提交、集成。
  - 多窗口协作的默认基线失效。
- 间接影响：
  - A 窗口无法基于主目录做滚动集成。
  - B/C/D/E 若继续基于错误目录工作，会进一步放大状态漂移。
  - 文档真值、handoff、go/no-go、历史记忆可能出现断链。

## 3. 已确认事实

### 3.1 当前 `/root/crmeb-java` 的状态
- `2026-03-24 03:13` 创建。
- 不是 git 仓库。
- 顶层仅剩：
  - `ruoyi-vue-pro-master`
  - `.worktrees`
  - `.vscode`
- 当前缺失：
  - `.git`
  - `docs/`
  - `hxy/`
  - `handoff/`
  - `yudao-mall-uniapp`

### 3.2 可恢复副本仍存在
- 当前已确认的完整副本包括：
  - `/tmp/window-b-booking-review-hs-frontend-contract-delta-sync-20260323-v2`
  - `/tmp/window-b-booking-review-hs-frontend-truth-20260323`
  - `/tmp/crmeb-java-window-d-repo-20260323`
  - `/tmp/crmeb-java-window-d-worktree-20260323`
- 这些副本包含：
  - `.git`
  - `docs/`
  - `hxy/`
  - `yudao-mall-uniapp`
  - `ruoyi-vue-pro-master`

### 3.3 已恢复的正式工作仓
- 已从以下目录恢复：
  - `/tmp/window-b-booking-review-hs-frontend-contract-delta-sync-20260323-v2`
- 恢复路径：
  - `/root/crmeb-java-recovered`
- 当前验证结果：
  - `git status -sb` 正常
  - `git log` 正常
  - `git fsck --full` 可执行

## 4. 已恢复与未恢复项

### 4.1 已恢复项
- 以下关键文档已在恢复仓确认存在：
  - `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
  - `docs/products/miniapp/2026-03-22-miniapp-booking-review-p2-final-integration-v1.md`
  - `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`
  - `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`

### 4.2 当前未恢复项
- 当前在 `/root` 与 `/tmp` 范围内未检索到：
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-*.md`
  - `docs/products/miniapp/2026-03-23-miniapp-booking-review-p3-go-no-go-package-v1.md`
- 结论：
  - 这两批文件当前不能视为“已恢复”。
  - 必须重新补写并纳入正式版本管理。

## 5. 根因分析

### 5.1 已证实根因
1. 主工作路径被切换或替换为一份精简副本，而不是完整仓库。
2. 原主路径不再承载 git 元数据，导致一切基于 git 的状态判断失真。
3. 多窗口协作过程中存在以 `/tmp` 独立副本为实际工作仓、而主目录未同步收口的问题。

### 5.2 高概率诱因
1. 工作目录来源不统一：
   - 一部分工作发生在 `/tmp/...`
   - 一部分工作发生在 `/root/crmeb-java`
   - 最终没有单一主仓完成归并
2. 主工作目录缺少“完整性健康检查”：
   - 没有在每日/每次开工前检查 `.git`、`docs/`、`hxy/` 是否存在
3. 多窗口工作使用了临时副本链式 clone / copy：
   - 使得 `origin` 指向本地临时目录
   - 仓库基线越来越脆弱

### 5.3 当前无法证实的部分
- 当前没有足够证据证明是具体哪一条 shell 命令删除了原目录内容。
- 当前没有足够证据证明原完整仓是否还存在于其他未扫描挂载点。
- 因此不能把结论写成“误删”，只能写成“主工作目录被替换/漂移为精简副本”。

## 6. 处置过程
1. 识别 `/root/crmeb-java` 不是 git 仓库。
2. 全盘搜索 `.git`、`docs/`、`hxy/` 与关键文档文件名。
3. 在 `/tmp` 发现多份完整独立副本。
4. 选定最完整且 git 健康的一份作为恢复源：
   - `/tmp/window-b-booking-review-hs-frontend-contract-delta-sync-20260323-v2`
5. 恢复出正式工作仓：
   - `/root/crmeb-java-recovered`
6. 核对已恢复文档与未恢复文档边界。

## 7. 事故责任判断
- 这不是单个开发窗口的编码错误，而是协作基线管理失效。
- 责任集中在流程设计，而不是某一条功能分支。
- A 窗口需要承担以下治理责任：
  - 没有提前强制“唯一主仓”规则
  - 没有把“临时副本禁止作为长期主仓”写成强约束
  - 没有建立每日仓库完整性检查

## 8. 必须落地的整改项

### 8.1 仓库基线整改
1. 后续唯一正式工作仓固定为：
   - `/root/crmeb-java-recovered`
2. 原 `/root/crmeb-java` 不再作为正式开发基线。
3. 所有新窗口只能从正式工作仓复制：
   - `/root/crmeb-java-window-b`
   - `/root/crmeb-java-window-c`
   - `/root/crmeb-java-window-d`
   - `/root/crmeb-java-window-e`

### 8.2 多窗口协作整改
1. 不允许 B/C/D/E 直接在同一目录工作。
2. 不允许把 `/tmp/...` 作为长期正式仓。
3. 所有窗口必须回报：
   - commit hash
   - allowlist 文件清单
   - 验证命令与结果
   - handoff 路径
4. A 只吸收已正式提交成果，不吸收脏工作区。

### 8.3 每日健康检查
每日开工前必须执行：
```bash
git rev-parse --show-toplevel
test -d docs && echo docs-ok
test -d hxy && echo hxy-ok
test -d yudao-mall-uniapp && echo miniapp-ok
test -d ruoyi-vue-pro-master && echo backend-ok
git status -sb
```

### 8.4 临时副本治理
1. `/tmp/...` 只允许作为隔离实验副本。
2. 临时副本完成后，必须在 24 小时内：
   - 合并回正式仓，或
   - 标记废弃，或
   - 导出 handoff 并删除
3. 禁止链式以临时副本作为新的 `origin`。

## 9. 后续动作

### 9.1 立即动作
1. 将 A 窗口工作基线切换到 `/root/crmeb-java-recovered`
2. 基于恢复仓重建 B/C/D/E 的独立副本
3. 重新补写当前未恢复的两批文档

### 9.2 本周动作
1. 补齐缺失文档：
   - `community-store-private-domain` 9 份
   - `booking-review-p3-go-no-go-package`
2. 形成新的窗口启动 SOP
3. 建立“Ready-to-Integrate 队列”

### 9.3 本月动作
1. 建立仓库完整性巡检脚本
2. 建立临时副本回收机制
3. 建立多窗口协作统一分支/副本命名规范

## 10. 最终结论
- 本次事故本质不是单文件丢失，而是主工作目录失去“正式仓库”资格。
- 当前已经找回一份可用正式恢复仓：
  - `/root/crmeb-java-recovered`
- 这份恢复仓应立即升格为后续所有开发与集成的唯一基线。
- 对于当前未找回的文档，不再继续假设其可恢复，直接进入重建流程。
