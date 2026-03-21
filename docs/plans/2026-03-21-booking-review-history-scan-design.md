# Booking Review History Scan Design（2026-03-21）

## 1. 背景
- booking review 当前后台已经具备三类 admin-only 治理能力：
  - 台账筛选与排查
  - 详情页回复 / 跟进 / 店长待办处理
  - 看板汇总与 drill-down
- 03-21 前，`A1 ~ A4` 已完成，剩余 backlog 中最接近数据治理但仍需严格保守推进的项是：
  - `A5 后台历史修复工具评估`
- 当前单一真值已经固定：
  - 历史差评若 `managerTodoStatus = null`，只会在 `claim / first-action / close` 写动作里触发 lazy-init
  - admin list / detail / dashboard 不会在 read-path 自动补齐 `managerTodo*`
- 因此 A5 第一版必须是“识别工具”，不是“修复工具”。

## 2. 目标
- 提供一个 admin-only 的“历史差评治理扫描页”，帮助运营人工识别：
  - 哪些历史差评可人工进入详情页推进
  - 哪些记录存在高风险，需要先核实主数据
  - 哪些记录不属于本轮历史治理对象
- 第一版只做：
  - 手动触发扫描
  - 候选清单
  - 风险分类
  - 风险说明
  - 跳详情排查
- 第一版明确不做：
  - 自动修复
  - 批量初始化
  - 批量认领 / 闭环
  - 自动通知
  - 导出正式修复批次

## 3. 适用范围与单一真值

### 3.1 范围
- 只针对 `booking review` 后台治理
- 只针对历史评价数据的“待办未初始化风险”识别
- 只收口 admin-only 场景，不改 miniapp，不改 release 结论

### 3.2 真值前提
- `ensureManagerTodoReady(...)` 当前只在写路径里触发
- 写路径真实补齐逻辑依赖：
  - `reviewLevel = NEGATIVE`
  - `submitTime`
  - `resolvedStoreId = order.storeId or review.storeId`
  - `productStoreService.getStore(resolvedStoreId)` 提供 `contactName/contactMobile`
- 因此扫描工具只允许据此做风险判断，不能外推成“系统已自动修复历史数据”。

## 4. 备选方案

### 方案 A：后台只读扫描页
- 做法：
  - 在 booking review 后台增加一个独立只读扫描页
  - 由人工点击“开始扫描”后返回 summary + 候选清单
  - 每条候选只提供“查看详情”动作
- 优点：
  - 对运营最友好
  - 真值最容易守住
  - 能与现有台账 / 详情形成闭环
- 缺点：
  - 需要前后端各补一个 scan-only 面
- 结论：采用

### 方案 B：后端 dry-run 接口 + 可选执行修复
- 做法：
  - 同时设计 dry-run 和执行接口
- 缺点：
  - 容易被误读成“修复能力已上线”
  - 超出当前评估阶段边界
- 结论：拒绝

### 方案 C：离线脚本 / SQL 报告
- 做法：
  - 只提供脚本，不进入后台页面
- 缺点：
  - 运营不可直接使用
  - 不符合当前 admin-only 增强路线
- 结论：拒绝

## 5. 页面与交互设计

### 5.1 入口
- 从现有 booking review 台账页增加一个次级入口按钮：
  - `历史治理扫描`
- 打开独立页面：
  - `/mall/booking/review/history-scan`
- 不新增顶级菜单 SQL；采用与 `detail / dashboard` 相同的次级页面模式。

### 5.2 页面结构
1. 顶部说明卡
   - 明确写：
     - 本页只识别治理候选
     - 不会自动修复历史数据
     - 不代表已进入店长待办池
     - 不代表已纳入 SLA 统计
2. 条件区
   - 门店 ID
   - 预约订单 ID
   - 提交时间范围
   - 风险分类
   - 按钮：`开始扫描`
3. 汇总卡片
   - 扫描总量
   - `可人工推进`
   - `高风险待核实`
   - `不在本轮范围`
4. 结果表格
   - 评价 ID
   - 预约订单 ID
   - 门店 / ID
   - 技师 / ID
   - 会员 / ID
   - 提交时间
   - 当前店长待办状态
   - 分类
   - 风险说明
   - 操作：`查看详情`

### 5.3 触发方式
- 页面加载后默认不自动扫描。
- 只有人工点击 `开始扫描` 才发起请求。
- 这样能保持“人工触发”的产品语义，也避免页面一打开就误解为系统在自动治理历史数据。

## 6. 分类规则

### 6.1 A 类：可人工推进（MANUAL_READY）
满足：
- `reviewLevel = 3`
- `managerTodoStatus = null`
- `submitTime != null`
- `resolvedStoreId` 可求得（优先 `bookingOrder.storeId`，否则 `review.storeId`）
- 对应门店主数据存在，且 `contactName/contactMobile` 可取

说明：
- 这类记录适合人工进入详情页执行认领，让写路径触发 lazy-init。
- 页面文案必须写：`可人工推进，不代表系统已修复。`

### 6.2 B 类：高风险待核实（HIGH_RISK）
满足：
- `reviewLevel = 3`
- `managerTodoStatus = null`
- 且任一高风险条件命中：
  - `submitTime = null`
  - `resolvedStoreId = null`
  - 门店主数据未命中
  - `contactName/contactMobile` 缺失

说明：
- 这类记录仍可能通过人工进一步核实，但不能直接当作“可顺畅推进”的候选。
- 页面文案必须写：`需先核实订单、门店或联系人真值。`

### 6.3 C 类：不在本轮范围（OUT_OF_SCOPE）
满足任一：
- `reviewLevel != 3`
- `managerTodoStatus != null`

说明：
- 这类记录不属于“历史未初始化差评”治理范围。
- 可计入 summary，但默认不作为重点候选。

## 7. 后端设计

### 7.1 Endpoint
建议新增 admin-only 只读接口：
- `GET /booking/review/history-scan`

说明：
- 虽然是“人工触发”，但本质仍是一次只读扫描，不产生持久化副作用。
- 手动触发由前端按钮保证，不依赖 POST 来表达“命令”。

### 7.2 Request VO
建议新增：
- `BookingReviewHistoryScanReqVO`

字段：
- `pageNo`
- `pageSize`
- `storeId?`
- `bookingOrderId?`
- `riskCategory?`：`MANUAL_READY / HIGH_RISK / OUT_OF_SCOPE`
- `submitTime?[]`

### 7.3 Response VO
建议新增：
- `BookingReviewHistoryScanRespVO`
  - `summary`
  - `list`
  - `total`
- `BookingReviewHistoryScanSummaryRespVO`
  - `scannedCount`
  - `manualReadyCount`
  - `highRiskCount`
  - `outOfScopeCount`
- `BookingReviewHistoryScanItemRespVO`
  - `reviewId`
  - `bookingOrderId`
  - `storeId`
  - `storeName`
  - `technicianId`
  - `technicianName`
  - `memberId`
  - `memberNickname`
  - `submitTime`
  - `managerTodoStatus`
  - `riskCategory`
  - `riskReasons[]`
  - `riskSummary`

### 7.4 Service 设计
建议新增 service 方法：
- `scanAdminHistoryCandidates(reqVO)`

行为：
1. 基于 filter 拉取 review 列表
2. 为每条记录解析：
   - `resolvedStoreId`
   - store 主数据命中情况
   - contact 是否存在
3. 生成分类与风险说明
4. 计算 summary
5. 返回分页结果

### 7.5 Mapper 设计
第一版可接受：
- 基于 review 表原始筛选先取记录，再在 service 层分类
- 不要求第一版就做复杂 SQL 聚合

理由：
- 当前是评估工具，优先把规则真值做清楚
- 避免在第一版过早引入难维护的 SQL 分类逻辑

## 8. 前端设计

### 8.1 页面文件
建议新增：
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/historyScan/index.vue`

### 8.2 API 类型
扩展：
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`

新增：
- `BookingReviewHistoryScanReq`
- `BookingReviewHistoryScanSummary`
- `BookingReviewHistoryScanItem`
- `getReviewHistoryScan()`

### 8.3 页面行为
- 不自动请求
- 点击“开始扫描”后才请求
- 默认优先展示 A/B 类结果
- C 类只作为 summary 和可选筛选，不作为默认运营主视角

## 9. 测试策略

### 9.1 后端
- `BookingReviewControllerTest`
  - 新增 history-scan endpoint 测试
- `BookingReviewServiceImplTest`
  - 覆盖分类规则：
    - negative + null + 完整 store contact -> `MANUAL_READY`
    - negative + null + missing submit/store/contact -> `HIGH_RISK`
    - non-negative / existing todo -> `OUT_OF_SCOPE`
- 明确断言：
  - 不写库
  - 不调用 `updateById`

### 9.2 前端
新增 node 测试：
- `tests/booking-review-admin-history-scan.test.mjs`

覆盖：
- 页面有 `开始扫描`
- 页面有只读风险提示
- 页面消费 `getReviewHistoryScan`
- 页面有 summary + table + 查看详情

### 9.3 文档真值
- backlog 可更新为 `已完成方案设计`
- 不得更新为“已落地”或“已修复历史数据”

## 10. No-Go
1. 不新增任何写库修复动作。
2. 不新增批量初始化、批量认领、批量闭环。
3. 不把扫描结果写成修复结果。
4. 不把 C 类记录误写成候选已治理。
5. 不把 read-path 识别写成自动补齐。
6. 不新增自动通知、自动补偿、自动奖励、自动派单。
7. 不把本轮工具写成 release-ready 数据治理平台。

## 11. 后续衔接
- 只有当 A5 的扫描规则、候选口径和风险说明被验证稳定后，后续才有资格进入：
  - dry-run 报告导出
  - 人工确认执行的修复方案
  - 批次留痕与审批
- 这些都不属于当前版本。
