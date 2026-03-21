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
| A6 | notify outbox 阻断诊断与修复建议 | 当前 notify outbox 已补标准化诊断码、修复建议和 `manualRetryAllowed`，详情/台账可区分“发送失败”与“缺店长路由/账号” | 降低误重试和误判 | 已落地（admin-only, diagnosis-only） |
| A7 | 门店店长账号路由只读核查页 | 当前已新增 `/booking/review/manager-routing/get|page` 与 admin 只读核查页，notify outbox/详情可一跳查看 | 让 `BLOCKED_NO_OWNER` 有明确排查入口 | 已落地（admin-only, readonly） |
| A8 | 通知链路审计增强 | 当前 notify outbox 台账已支持按 `lastActionCode` 过滤，并展示“最近动作说明 / 最近动作人 / 动作原因” | 回答“谁重试的、什么时候重试的、为什么失败” | 已落地（admin-only, audit-only） |
| A9 | 台账页 SLA 快捷筛选 | 当前台账页已补“待认领优先 / 认领超时 / 首次处理超时 / 闭环超时 / 历史待初始化”快捷入口 | 减少值班筛选点击成本 | 已落地（admin-only, ops-efficiency） |
| A10 | 台账页店长待办快捷动作 | 当前台账页已支持直接“快速认领 / 记录首次处理 / 标记闭环”，无需先进入详情页 | 缩短常见处理路径 | 已落地（admin-only, ops-efficiency） |

## 5. Not Now（当前不得排进本批开发）

| 编号 | 项目 | 原因 |
|---|---|---|
| N1 | 自动通知店长 / 客服 / 区域负责人 | 当前虽已新增账号路由模型、notify outbox、只读路由核查页与阻断跳转入口，但消息通道、数据覆盖、运行样本和发布证据仍未闭环 |
| N2 | 自动差评补偿 | 当前业务明确未落地 |
| N3 | 自动好评奖励 | 当前业务明确未落地 |
| N4 | 基于 `managerUserId` 的自动派单或审批 | 当前未核出 `managerUserId` 真值 |
| N5 | 发布级 rollout / feature flag | 当前不属于 admin-only UI 增强范围 |

## 6. 排序建议
1. `A1 ~ A8` 当前都已完成第一阶段 admin-only 落地。
2. 下一阶段应转入“多门店多店长通知分发、跨通道派发审计、发布证据包”三条线，不再重复建设只读真值面。
3. `A5` 若要进入修复工具，必须另起方案评审，不得从 scan-only 直接外推到自动修复。

## 7. No-Go
1. 不得把 admin-only 增强 backlog 写成 release plan。
2. 不得把“可做 next”写成“当前已实现”。
3. 不得把任何候选项外推成自动通知、自动补偿或账号级店长路由正式上线。
