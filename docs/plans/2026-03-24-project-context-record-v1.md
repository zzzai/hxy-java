# Project Context Record v1

## 1. 目的
- 把当前窗口已经确认过的项目上下文、目录真值、恢复结论、协作规则、下一步计划固化到本地。
- 在“长记忆机制失效”情况下，后续任何窗口都必须先读本文件，再继续开发。

## 2. 当前项目状态

### 2.1 项目阶段
- 当前项目已完成大批文档治理与 booking review 阶段性建设。
- 当前主任务已经从“继续扩文档”切换为“补工程闭环、推进全业务功能开发”。
- 技术架构优化明确后置，不作为当前业务功能开发前置条件。

### 2.2 已知阶段判断
- 文档治理：已形成较完整的业务台账与 booking review 治理文档。
- 工程状态：仍有多条链路处于 `Doc Closed / Can Develop / Cannot Release`。
- 当前应优先完成：
  - 共享业务底座
  - Member 缺页能力
  - 内容中心与邀新归因
  - 私域主链
  - Booking 写链 blocker 清理
  - 后台治理与发布材料

## 3. 当前目录与仓库真值

### 3.1 当前 `/root/crmeb-java`
- 不是 git 仓库。
- 当前仅剩：
  - `ruoyi-vue-pro-master`
  - `.worktrees`
  - `.vscode`
- 当前缺失：
  - `.git`
  - `docs/`
  - `hxy/`
  - `yudao-mall-uniapp`
- 结论：
  - `/root/crmeb-java` 当前不是可直接信任的正式仓基线。
  - 但用户要求后续主开发目录仍以 `/root/crmeb-java` 为主，因此必须先恢复，再继续使用。

### 3.2 正式恢复仓
- 恢复路径：
  - `/root/crmeb-java-recovered`
- 恢复源：
  - `/tmp/window-b-booking-review-hs-frontend-contract-delta-sync-20260323-v2`
- 当前恢复仓状态：
  - 有 `.git`
  - 有 `docs/`
  - 有 `hxy/`
  - 有 `yudao-mall-uniapp`
  - 有 `ruoyi-vue-pro-master`
- 当前恢复分支：
  - `recovery/workspace-loss-20260324`

### 3.3 当前建议
- 在正式完成目录切换前：
  - 以 `/root/crmeb-java-recovered` 作为真值仓
- 在完成目录切换后：
  - 让 `/root/crmeb-java` 恢复成完整主仓

## 4. 文档真值

### 4.1 已确认存在的关键文档
- `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
- `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
- `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- `docs/products/miniapp/2026-03-22-miniapp-booking-review-p2-final-integration-v1.md`
- `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`
- `docs/plans/2026-03-24-workspace-loss-incident-retrospective-v1.md`

### 4.2 当前未检索到的文档
- `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-*.md`
- `docs/products/miniapp/2026-03-23-miniapp-booking-review-p3-go-no-go-package-v1.md`

### 4.3 未检索到文档的处理原则
- 当前不能把“会话记忆里提到过”当成“文件仍存在”。
- 以上未检索到文档一律按“待重建”处理。

## 5. 已确认的业务与项目判断

### 5.1 全项目主规划
后续应按以下顺序推进：
1. 共享业务底座
2. Member 缺页能力
3. 内容中心 + 邀新归因
4. 私域主链一期
5. 私域主链二期
6. Booking 写链 blocker 清理
7. 后台运营补强
8. 发布材料与 Go/No-Go
9. 技术架构优化

### 5.2 技术架构优化原则
- 当前不抢先做大规模架构改造。
- 只有在主要业务功能基本完成后，再专项做：
  - 容量治理
  - 压测
  - 缓存 / MQ / 调度 / 监控优化

## 6. 多窗口协作真值

### 6.1 A/B/C/D/E 角色
- A：集成 / CTO / PM / 真值裁决
- B：小程序前端
- C：后端 / 契约
- D：后台 / 证据 / 运维面
- E：Gate / QA / drift / 回归

### 6.2 协作原则
1. 不能所有窗口直接共用同一个工作目录。
2. 每个窗口必须基于正式主仓复制独立副本。
3. 每个窗口必须只改自己的写集合。
4. 每个窗口必须回报：
   - commit hash
   - allowlist 文件
   - 验证命令与结果
   - handoff 路径
5. A 不等所有窗口结束再集成，而是滚动吸收。

### 6.3 写集合建议
- B：`yudao-mall-uniapp/**`
- C：`ruoyi-vue-pro-master/**/src/main/java/**`、`src/test/java/**`、`docs/contracts/**`
- D：`overlay-vue3/**`、`script/dev/**`、`tests/**`、`docs/plans/**`
- E：gate/test/evidence/check 脚本
- A：`docs/products/**`、总集成、handoff、结论文档

## 7. 本次事故真值

### 7.1 事故结论
- 当前主目录丢失正式仓资格，不是普通单文件误删。
- 原完整仓结构在 `/tmp` 的多个独立副本中仍然可见。
- 已成功恢复一份正式工作仓。

### 7.2 事故复盘文档
- `docs/plans/2026-03-24-workspace-loss-incident-retrospective-v1.md`

### 7.3 必须吸取的结论
1. 临时副本不能长期充当主仓。
2. `/tmp` 副本完成后必须及时合回正式仓。
3. 主开发目录每日必须做完整性检查。
4. origin 指向本地临时目录属于高风险状态。

## 8. Git / 远端真值

### 8.1 当前恢复仓 remote 状态
- 当前 `origin` 不是 GitHub，而是本地临时副本：
  - `/tmp/window-b-booking-review-hs-frontend-truth-20260323`

### 8.2 当前 GitHub 连通性
- 当前机器可以访问 GitHub。
- 已验证：
  - `curl https://github.com` 可返回 `HTTP 200`
  - `git ls-remote https://github.com/YunaiV/ruoyi-vue-pro.git HEAD` 可返回结果

### 8.3 当前结论
- “GitHub 能不能连”：能连。
- “当前项目真实 GitHub remote 还在不在”：当前这条仓链里没有保留下来。
- 因此后续必须重新绑定正式远端。

## 9. 当前主开发目录策略
- 用户要求后续主开发目录仍以 `/root/crmeb-java` 为主。
- 这可以成立，但前提是：
  - 先把 `/root/crmeb-java` 恢复成完整正式仓
  - 再让所有窗口基于这个目录复制独立副本

## 10. 当前立即待办
1. 把 `/root/crmeb-java` 恢复为正式主开发目录
2. 重新绑定正式 GitHub remote
3. 重建缺失文档：
   - `community-store-private-domain` 9 份
   - `booking-review-p3-go-no-go-package`
4. 基于恢复后的主仓重建 B/C/D/E 独立副本

## 11. 每次开工前必查
```bash
git rev-parse --show-toplevel
test -d docs && echo docs-ok
test -d hxy && echo hxy-ok
test -d yudao-mall-uniapp && echo miniapp-ok
test -d ruoyi-vue-pro-master && echo backend-ok
git status -sb
git remote -v
```

## 12. 结论
- 本文件是当前窗口上下文的本地固化记录。
- 后续若记忆再丢，必须以本文件和事故复盘文档为起点恢复上下文。
