# Booking Review Admin Detail Timeline Design（2026-03-21）

## 1. 背景
- booking review 后台详情页当前已经支持：
  - 回复评价
  - 更新跟进状态
  - 店长待办认领 / 首次处理 / 闭环
- 详情页也已经展示了较多基础字段和当前状态字段，但仍然存在一个明显阅读断点：
  - 运营能看到当前状态
  - 也能看到少量时间点字段
  - 但无法快速回答“这条评价从提交到当前，经历了哪些关键动作”
- 当前 backlog 已把 `A3` 定义为 admin-only 的可读性增强项：`详情页增加“最近动作时间线”只读块`。

## 2. 目标
- 在详情页增加一个纯只读的“最近动作时间线”区块，帮助运营快速完成复盘。
- 只基于当前真实字段进行时间线重建，不新增后端动作日志、不改变状态机、不引入新写动作。
- 保持 booking review 的保守真值：这是 admin-only 的详情可读性增强，不是审计流水系统，也不是 release-ready 证据。

## 3. 当前真值边界

### 3.1 可直接用于时间线的字段
- `submitTime`：评价提交时间
- `firstResponseAt`：首次响应时间
- `replyTime`：正式回复时间
- `managerClaimedAt`：店长待办认领时间
- `managerFirstActionAt`：首次处理时间
- `managerClosedAt`：闭环时间

### 3.2 可用于摘要但不能伪装成历史日志的字段
- `followStatus`
- `replyStatus`
- `managerTodoStatus`
- `managerLatestActionRemark`
- `followResult`
- `replyContent`

### 3.3 当前不成立的真值
- 当前没有独立“动作日志表”或“动作明细列表”接口。
- `managerLatestActionRemark` 只是最近一次处理备注，不是每个动作节点的历史备注。
- `followStatus` 没有独立的历史更新时间字段，不能伪造“状态切换日志”。

## 4. 备选方案

### 方案 A：纯时间点列表
- 做法：
  - 只把真实时间字段映射成时间线节点
- 优点：
  - 最稳妥
- 缺点：
  - 复盘信息量有限，可读性一般

### 方案 B：关键节点时间线 + 当前状态摘要 + 最近备注
- 做法：
  - 先按真实时间点生成时间线
  - 再把当前状态、最近处理备注、当前跟进结论、回复摘要以“摘要区”方式展示
- 优点：
  - 可读性最好
  - 完全基于现有字段
  - 不需要新增后端能力
- 缺点：
  - 需要严格区分“摘要”与“历史节点”，避免误导
- 结论：采用

### 方案 C：伪完整操作流水
- 做法：
  - 用现有字段拼装成类似审计日志的多条流水
- 优点：
  - 展示最丰富
- 缺点：
  - 当前缺少真实动作流水支撑
  - 容易把“最近备注”误写成“历史日志”
- 结论：拒绝

## 5. 设计方案

### 5.1 展示位置
- 新增一个只读块：`最近动作时间线`
- 放置顺序：
  1. 评价基础信息
  2. 最近动作时间线
  3. 回复评价 / 跟进状态 / 店长待办操作区
- 理由：
  - 用户先理解上下文，再做处理动作
  - 不把只读信息和操作表单混杂在一起

### 5.2 时间线节点
- 节点按时间正序展示，只渲染真实存在的时间点。
- 推荐节点：
  - `评价已提交`
    - 时间：`submitTime`
    - 说明：带出总体评分、评价等级
  - `首次响应已记录`
    - 时间：`firstResponseAt`
    - 说明：这是首次进入响应口径，不等于正式回复
  - `已正式回复用户`
    - 时间：`replyTime`
    - 说明：可展示 `replyUserId`
  - `店长待办已认领`
    - 时间：`managerClaimedAt`
    - 说明：可展示 `managerClaimedByUserId`
  - `已记录首次处理`
    - 时间：`managerFirstActionAt`
    - 说明：只说明进入首次处理阶段，不把最近备注强绑到该节点
  - `店长待办已闭环`
    - 时间：`managerClosedAt`
    - 说明：展示闭环完成态
- 如果某字段缺失，则对应节点不出现。

### 5.3 当前状态摘要
- 在时间线块顶部增加“当前状态摘要”区。
- 展示项：
  - 当前跟进状态
  - 当前回复状态
  - 当前店长待办状态
- 如果存在 `managerLatestActionRemark`：
  - 展示为 `最近处理备注`
- 如果存在 `followResult`：
  - 展示为 `当前跟进结论`
- 如果存在 `replyContent` 且已回复：
  - 展示为 `最新回复摘要`
- 这些内容都属于“当前摘要”，不能写成历史时间节点。

### 5.4 前端实现建议
- 新建一个纯函数 helper，集中做字段到 UI 模型的映射：
  - 输入：`BookingReview`
  - 输出：
    - `summaryItems`
    - `timelineItems`
- 纯函数建议放在：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/timelineHelpers.mjs`
- 详情页只负责消费 helper 输出并渲染。

## 6. 测试策略
- 新增纯函数测试，验证：
  1. 只有 `submitTime` 时，只输出一个节点
  2. 有多个时间点时，节点顺序正确
  3. `managerLatestActionRemark` 只出现在摘要区，不出现在时间线节点中
  4. `followStatus` 只形成当前态摘要，不生成伪时间节点
- 可增加页面静态测试，验证详情页已消费时间线 helper，并包含时间线块文案。

## 7. No-Go
1. 不新增后端动作日志或独立审计表。
2. 不把 `managerLatestActionRemark` 拆成多条历史日志。
3. 不为 `followStatus` 伪造历史变更时间。
4. 不新增自动通知、自动补偿、自动奖励、自动派单。
5. 不修改现有回复、跟进、店长待办动作语义。
6. 不把本批 admin-only 只读增强写成“流程闭环能力升级”。

## 8. 单一真值引用
- `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`
- `docs/products/miniapp/2026-03-19-miniapp-booking-review-history-and-boundary-audit-v1.md`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
