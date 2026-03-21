# Booking Review Admin Ops Enhancement Backlog v1（2026-03-19）

## 1. 目标
- 基于当前真实 admin 页面，整理 booking review 后台治理的下一步增强候选。
- 只收口 admin-only 范围，不把自动通知、自动补偿、账号路由等未落地能力混进本批 backlog。

## 2. 当前页面真值
- 台账：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
- 详情：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
- 看板：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`
- API：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`

## 3. 当前已具备能力
1. 台账筛选差评、风险等级、跟进状态、店长待办状态、SLA 状态。
2. 详情页执行回复、跟进状态更新、店长待办认领、首次处理、闭环。
3. 看板查看差评量、待处理量和店长待办 SLA 统计。

## 4. Can Do Next（仍属 admin-only）

| 编号 | 候选项 | 当前依据 | 价值 | 当前状态 |
|---|---|---|---|---|
| A1 | 台账补门店名 / 技师名 / 会员昵称只读展示 | admin review response 已补 `storeName / technicianName / memberNickname`，台账/详情已消费 | 提升值班效率 | 已落地（admin-only） |
| A2 | 看板卡片支持一键带条件跳回台账 | 看板卡片已支持 query drill-down，台账已按 route query 回填筛选 | 缩短排查链路 | 已落地（admin-only） |
| A3 | 详情页增加“最近动作时间线”只读块 | 当前只有最近一次处理备注，没有时间线 | 提升复盘可读性 | 已落地（admin-only） |
| A4 | 台账增加“历史未初始化差评”筛选提示 | 当前 read-path 不自动补齐，容易误读 | 降低误判 | 已落地（admin-only） |
| A5 | 后台历史治理扫描页（scan-only） | 已新增 `GET /booking/review/history-scan` 与 admin 扫描页，只做人工触发扫描、候选清单和风险提示 | 为后续数据治理准备，先把识别边界做实 | 已落地（admin-only, scan-only） |

## 5. Not Now（当前不得排进本批开发）

| 编号 | 项目 | 原因 |
|---|---|---|
| N1 | 自动通知店长 / 客服 / 区域负责人 | 当前没有稳定账号归属与消息通道真值；03-21 已完成账号路由真值审计与 notify outbox 方案设计，仍未进入实现 |
| N2 | 自动差评补偿 | 当前业务明确未落地 |
| N3 | 自动好评奖励 | 当前业务明确未落地 |
| N4 | 基于 `managerUserId` 的自动派单或审批 | 当前未核出 `managerUserId` 真值 |
| N5 | 发布级 rollout / feature flag | 当前不属于 admin-only UI 增强范围 |

## 6. 排序建议
1. 先做 `A1 + A2`，它们不依赖新后端能力，能直接改善值班效率。
2. 再做 `A4`，明确告诉运营哪些记录尚未进入店长待办池。
3. `A3` 可在字段真值确认后进入实现。
4. `A5` 第一阶段已落地为 scan-only 工具；后续若要进入修复工具，必须另起方案评审，不得直接外推。

## 7. No-Go
1. 不得把 admin-only 增强 backlog 写成 release plan。
2. 不得把“可做 next”写成“当前已实现”。
3. 不得把任何候选项外推成自动通知、自动补偿或账号级店长路由。
