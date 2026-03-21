# Booking Review Pending Init Filter Design（2026-03-21）

## 1. 背景
- 03-19 上线后的 booking review 后台已经能在台账行级把 `reviewLevel=差评 && managerTodoStatus=null` 显示为“待初始化”。
- 但当前后台筛选区没有独立入口，运营只能靠人工翻页识别，容易把“尚未进入店长待办池”误读成“没有风险”或“已进入 SLA 统计”。
- 历史真值已经固定：read-path 不自动补齐 `managerTodo*` 字段，只有 `claim / first-action / close` 等写动作才会触发 lazy-init。

## 2. 目标
- 在 booking review 后台台账增加一个独立筛选视角，帮助运营快速识别“历史未初始化差评”。
- 保持当前系统边界不变：只增强查询识别，不做自动修复，不改 dashboard 统计，不改变写路径 lazy-init 语义。
- 继续保持 admin-only 增强口径，不把本批工作写成 release-ready 或自动治理能力。

## 3. 备选方案

### 方案 A：新增真实筛选字段 `onlyPendingInit`
- 做法：
  - 后端分页查询新增布尔字段 `onlyPendingInit`
  - 查询语义固定为 `reviewLevel = 3 && managerTodoStatus IS NULL`
  - 前端台账新增“只看历史未初始化差评”筛选和只读提示文案
- 优点：
  - 真值集中在后端查询层
  - 运营可以稳定筛出目标数据
  - 与当前“待初始化”标签语义一致
- 缺点：
  - 需要同时改前后端和测试

### 方案 B：只补前端提示，不增加真实筛选
- 做法：
  - 页面顶部提示 read-path 不会自动补齐
  - 维持当前靠行级标签人工识别
- 优点：
  - 改动最小
- 缺点：
  - 不能真正提高排查效率
  - 无法快速定位历史未初始化差评

### 方案 C：在 read-path 自动补齐历史差评待办字段
- 做法：
  - 列表、详情或看板读取时自动初始化 `managerTodo*`
- 优点：
  - 从表面上减少“待初始化”记录
- 缺点：
  - 直接改变现有边界真值
  - 会把“识别工具”误做成“数据修复”
  - 当前明确不允许

## 4. 选型
- 采用方案 A。
- 理由：
  - 它是最小且完整的运营增强，能真正解决“找不出来”的问题。
  - 不碰 write-path，不碰 dashboard，不改变历史 lazy-init 真值。
  - 查询逻辑沉到后端 mapper，语义更稳定，也便于单元测试和后续审计。

## 5. 设计细节

### 5.1 真值定义
- “历史未初始化差评”固定定义为：
  - `reviewLevel = 3`
  - `managerTodoStatus = null`
- 这是一个筛选视角，不是新的状态枚举。
- 不新增数据库字段，不改已有 `managerTodoStatus` 状态机。

### 5.2 后端查询设计
- `BookingReviewPageReqVO` 新增：
  - `onlyPendingInit: Boolean`
- `BookingReviewMapper.buildAdminQuery(...)` 增加：
  - 当 `onlyPendingInit = true` 时，追加 `reviewLevel = 3` 与 `managerTodoStatus IS NULL`
- 边界：
  - 不自动改写 `onlyManagerTodo`
  - 不自动改写 `managerTodoStatus`
  - 该条件必须同时作用于 `selectAdminPage` 和 `selectAdminList`

### 5.3 前端台账设计
- 台账筛选区新增：
  - 标签：`历史未初始化差评`
  - 选项：`是`
- 顶部提示文案新增一段只读说明：
  - 这类记录尚未进入店长待办池
  - 系统不会在查询时自动补齐
  - 只有首次认领、首次处理、闭环等写动作才会触发初始化
- 保持现有表格行级展示不变：
  - `managerTodoStatus=null && reviewLevel=3` 继续显示 `待初始化`

### 5.4 query helper 设计
- `createDefaultLedgerQuery()` 增加 `onlyPendingInit`
- `parseLedgerQuery()` 支持把 route query 中的 `onlyPendingInit=true/false` 解析为布尔值
- 不新增 dashboard 卡片映射；本批只做台账识别，不改变看板语义

## 6. 测试策略
- 后端：
  - 在 `BookingReviewMapperTest` 先写红灯测试，验证 `onlyPendingInit=true` 时只返回 `差评 + managerTodoStatus=null` 记录
  - 再验证 `onlyPendingInit` 不会把已有正常店长待办记录混进结果
- 前端：
  - 在 `tests/booking-review-admin-query-helpers.test.mjs` 先写红灯测试，验证 `parseLedgerQuery()` 会正确解析 `onlyPendingInit`
- 回归：
  - 普通台账查询不受影响
  - `onlyManagerTodo` 语义不受影响
  - dashboard 和 detail 语义不受影响

## 7. No-Go
1. 不自动修复历史数据。
2. 不改 dashboard 统计口径。
3. 不把“历史未初始化差评”并入“店长待办池”。
4. 不新增自动通知、自动补偿、自动奖励、自动派单。
5. 不把本批 admin-only 增强外推成 release-ready 能力。
