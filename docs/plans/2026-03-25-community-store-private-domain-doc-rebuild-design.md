# Community Store Private Domain Doc Rebuild Design

## 1. 背景
- `docs/plans/2026-03-24-workspace-loss-incident-retrospective-v1.md` 与 `docs/plans/2026-03-24-project-context-record-v1.md` 已明确：`community-store-private-domain` 9 份文档在事故后未找回，只能按“待重建”处理。
- 当前项目已经完成 `Booking / Member / Reserved / BO-004` 等多个域的工程收口，但“社区门店私域主链”文档层仍缺一个成体系的专题包，导致后续产品规划、开发排期、运营口径缺少统一真值。
- 本次重建必须坚持两个原则：
  1. 不把未开发能力写成已上线。
  2. 不把最佳实践直接当成当前项目真值，而是写成“当前状态 + 目标方案 + 开发优先级”。

## 2. 目标
- 一次性重建 `community-store-private-domain` 9 份文档。
- 采用 `1 份总纲 + 8 份专题` 结构，保证既能支持产品规划，也能支持后续研发拆解。
- 每份文档都明确：
  - 当前项目真值
  - 目标业务流程
  - 依赖能力 / 现状缺口
  - 开发与验收建议
- 同步修正事故复盘与项目上下文中“未恢复”的记录。

## 3. 方案比较

### 方案 A：1 份总纲 + 8 份专题
- 优点：最贴近当前项目推进方式，既有顶层总纲，也能直接对应后续研发专题。
- 缺点：文件数量多，但这正符合“重建 9 份文档”的目标。

### 方案 B：3 份产品 + 3 份技术 + 3 份项目规划
- 优点：分类清晰。
- 缺点：会把同一个业务链路拆散到多个维度，不利于后续按专题开发。

### 方案 C：9 份全按用户生命周期切分
- 优点：用户视角连续。
- 缺点：对研发和后台治理不够友好，容易忽略组织、SOP、指标体系。

## 4. 推荐方案
- 采用方案 A。
- 文件结构：
  1. 社区门店私域总纲
  2. 公域引流与首触点转化
  3. 入会授权与留资链路
  4. 到店服务过程中的公转私触点
  5. 离店后的企微/社群沉淀与复购
  6. 会员分层、标签、任务触达
  7. 社群活动、拼团裂变、邀请有礼
  8. 店长/店员/企微运营 SOP 与分工
  9. 指标体系、归因口径、项目推进计划

## 5. 文档边界
- 文档放置目录：`docs/products/miniapp/`
- 文件名前缀统一：`2026-03-23-miniapp-community-store-private-domain-*.md`
- 只用仓内现有事实作为“当前状态”依据：`Member` 页面、`Referral` runtime、`Booking` 链路、`Booking Review` 店长双通道治理、现有 growth / member / referral / reserved 文档。
- 最佳实践只作为“目标方案 / 推荐机制”引用，不得写成当前已落地能力。

## 6. 需要同步更新的文件
- `docs/plans/2026-03-24-workspace-loss-incident-retrospective-v1.md`
- `docs/plans/2026-03-24-project-context-record-v1.md`
- 可选：治理记忆与执行看板，补充“9 份文档已重建”的事实。

## 7. 质量要求
- 新增一个轻量 Node 回归，冻结 9 份文件名与总纲引用关系。
- 最终跑 `git diff --check`、`check_hxy_naming_guard.sh`、`check_hxy_memory_guard.sh`。
- 不改业务代码，只补文档和文档守边界测试。
