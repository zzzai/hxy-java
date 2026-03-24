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
- 已恢复为完整 git 仓库。
- 当前已确认存在：
  - `.git`
  - `docs/`
  - `hxy/`
  - `yudao-mall-uniapp`
  - `ruoyi-vue-pro-master`
- 当前分支：
  - `recovery/workspace-loss-20260324`
- 结论：
  - `/root/crmeb-java` 已恢复为当前正式主开发目录。
  - 后续所有窗口应基于该目录复制独立副本，不再基于事故期间的残缺目录或 `/tmp` 临时副本直接开发。

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
- `/root/crmeb-java-recovered` 仍可保留为恢复过程的辅助副本，但不再作为主开发目录。
- 主开发与后续多窗口协作一律以 `/root/crmeb-java` 为基线仓。
- 事故现场目录保留为：
  - `/root/crmeb-java-loss-snapshot-20260324`
  - 仅用于复盘与取证，不再继续开发。

## 4. 文档真值

### 4.1 已确认存在的关键文档
- `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
- `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
- `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- `docs/products/miniapp/2026-03-22-miniapp-booking-review-p2-final-integration-v1.md`
- `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`
- `docs/plans/2026-03-24-workspace-loss-incident-retrospective-v1.md`

### 4.2 当前未检索到的文档
- `docs/products/miniapp/2026-03-23-miniapp-booking-review-p3-go-no-go-package-v1.md`

### 4.3 未检索到文档的处理原则
- 当前不能把“会话记忆里提到过”当成“文件仍存在”。
- 以上未检索到文档一律按“待重建”处理。

### 4.4 已于 2026-03-25 重建的文档
- `community-store-private-domain` 9 份文档已完成重建：
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-overview-prd-v1.md`
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-public-to-private-acquisition-prd-v1.md`
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-membership-consent-and-lead-capture-prd-v1.md`
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-in-store-conversion-touchpoint-prd-v1.md`
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-wecom-community-retention-prd-v1.md`
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-member-segmentation-task-prd-v1.md`
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-social-fission-growth-prd-v1.md`
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-store-ops-sop-prd-v1.md`
  - `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-metrics-attribution-delivery-plan-v1.md`
- 当前仍未检索到并待重建的只剩：
  - `docs/products/miniapp/2026-03-23-miniapp-booking-review-p3-go-no-go-package-v1.md`

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

## 6.4 A 窗口默认决策规则
- A 窗口默认以“项目需求优先、直接推进”为原则，不再对常规推进动作反复征求确认。
- 只要能够基于当前单一真值、既有目标、风险边界做出合理判断，就直接执行最佳实践方案。
- 以下情况才允许停下确认：
  - 不可逆删除或覆盖
  - 可能影响正式发布、对外发送、生产数据
  - 需要在两个以上高成本方案间做业务取舍，且当前仓库真值无法裁决
- 以下情况禁止再次用“如果你要”“要不要继续”“是否需要我再做”之类话术打断推进：
  - 常规下一步规划
  - 文档补齐
  - 台账更新
  - blocker 盘点
  - 集成验证
  - 多窗口协作准备
- 若后续再次违反本规则，应优先修正执行方式，并把修正结果继续固化到本地记忆文档。

## 7. 本次事故真值

### 7.1 事故结论
- 主目录曾丧失正式仓资格，这不是普通单文件误删，而是目录级仓结构丢失事故。
- 原完整仓结构在 `/tmp` 的多个独立副本中仍然可见。
- 已成功恢复正式工作仓，并已重新落回 `/root/crmeb-java`。

### 7.2 事故复盘文档
- `docs/plans/2026-03-24-workspace-loss-incident-retrospective-v1.md`

### 7.3 必须吸取的结论
1. 临时副本不能长期充当主仓。
2. `/tmp` 副本完成后必须及时合回正式仓。
3. 主开发目录每日必须做完整性检查。
4. `origin` 指向本地临时目录属于高风险状态，必须改回正式 GitHub remote。
5. 任何窗口开工前必须先确认 `git rev-parse --show-toplevel`、`git remote -v`、主目录完整性，再执行开发。

## 8. Git / 远端真值

### 8.1 当前 `/root/crmeb-java` remote 状态
- `origin`
  - `git@github.com:zzzai/hxy-java.git`
- `upstream`
  - `https://github.com/YunaiV/ruoyi-vue-pro.git`
- `recovery-tmp`
  - `/tmp/window-b-booking-review-hs-frontend-truth-20260323`

### 8.2 remote 角色说明
- `origin`：当前正式 GitHub 远端，后续推送与分支协作都基于该仓。
- `upstream`：上游开源基线仓，用于对照主线能力与同步参考。
- `recovery-tmp`：恢复期保留的临时取证链路，仅用于追溯，不作为正式开发远端。

### 8.3 当前 GitHub 连通性
- 当前机器可以访问 GitHub。
- 已验证：
  - `git ls-remote git@github.com:zzzai/hxy-java.git HEAD` 可返回结果
  - `git ls-remote https://github.com/YunaiV/ruoyi-vue-pro.git HEAD` 可返回结果

### 8.4 当前结论
- “GitHub 能不能连”：能连。
- “当前项目正式 remote 是否已绑回”：已绑回。
- 结论：
  - `/root/crmeb-java` 当前已经完成正式 GitHub remote 重绑。
  - 后续不得再把 `origin` 指向本地目录或临时副本。

## 9. 当前主开发目录策略
- 用户要求后续主开发目录仍以 `/root/crmeb-java` 为主。
- 该策略现已成立，因为：
  - `/root/crmeb-java` 已恢复成完整正式仓
  - 正式 GitHub remote 已完成重绑
  - 后续只需让所有窗口基于这个目录复制独立副本

## 10. 当前立即待办
1. 基于已恢复的 `/root/crmeb-java` 重建 B/C/D/E 独立开发副本
2. 重建缺失文档：
   - `booking-review-p3-go-no-go-package`
3. 继续推进业务功能开发与 blocker 清理

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
- 截止本次更新，`/root/crmeb-java` 已重新成为可用的正式主开发仓，且 GitHub remote 已恢复正常。
