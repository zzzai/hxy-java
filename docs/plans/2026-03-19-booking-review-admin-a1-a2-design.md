# Booking Review Admin A1 A2 Design（2026-03-19）

## 1. 背景
- 当前 booking review 后台台账、详情、看板已经可用，但运营阅读成本仍高：
  - 台账和详情主要展示 `storeId / technicianId / memberId`
  - 看板只能“返回台账”，不能带条件钻取
- 03-19 backlog 已把这两项列为优先级最高的 admin-only 增强：
  - `A1` 可读名称展示
  - `A2` 看板带条件跳回台账

## 2. 目标
- 在不引入新业务链路的前提下，补齐后台可读性和 drill-down 效率。
- 继续保持 booking review 的保守真值：
  - 只做 admin-only 治理增强
  - 不补造自动通知
  - 不补造账号级店长归属
  - 不改变 `Doc Closed / Can Develop / Cannot Release`

## 3. 备选方案

### 方案 A：后端 enrich + 前端直接消费
- 做法：
  - admin review 返回 VO 新增 `storeName / technicianName / memberNickname`
  - controller 在 page/get 响应时 best-effort enrich
  - 前端页面直接展示新增字段
  - 看板通过 query 参数跳回台账
- 优点：
  - 单一真值集中在后端返回
  - 前端改动最小
  - 不需要新增前端请求风暴
- 缺点：
  - controller 要引入 3 个依赖做响应增强

### 方案 B：前端自行二次请求名称
- 做法：
  - 台账/详情拿到 ID 后，前端再补调门店、技师、会员接口
- 优点：
  - 后端 booking review 接口不变
- 缺点：
  - 需要更多 API 绑定
  - 页面加载复杂度和失败面更高
  - 不符合当前“最小 admin-only 增强”原则

### 方案 C：只做前端显示格式优化，不补名称
- 做法：
  - 继续只显示 ID，加一些文案和跳转
- 优点：
  - 最省改动
- 缺点：
  - 几乎不能解决值班阅读成本问题

## 4. 选型
- 采用方案 A。
- 理由：
  - 最小改动即可真正提升运营效率
  - 能保持真值边界稳定
  - 后端已有 `ProductStoreService`、`TechnicianService`、`MemberUserApi` 可做 best-effort enrich

## 5. 设计细节

### 5.1 A1 可读名称展示
- 后端：
  - `BookingReviewRespVO` 新增：
    - `storeName`
    - `technicianName`
    - `memberNickname`
  - `BookingReviewController` 在 `page/get` 返回前 best-effort enrich：
    - 门店名：`ProductStoreService.getStoreMap(...)`
    - 会员昵称：`MemberUserApi.getUserMap(...)`
    - 技师名：逐个 `TechnicianService.getTechnician(...)`
- 前端：
  - 台账表格把 `门店ID / 技师ID / 会员ID` 升级成“名称 + ID”
  - 详情页基础信息把三项同样升级为“名称 + ID”
- 边界：
  - 未命中时回退到 `- / ID`
  - 不新增 `managerUserId`
  - 不把门店联系人快照写成后台账号名称

### 5.2 A2 看板带条件跳回台账
- 看板每张卡片增加“查看台账”动作
- 通过 query 参数跳到 `/mall/booking/review`
- 预置映射：
  - `negative -> reviewLevel=3`
  - `positive -> reviewLevel=1`
  - `neutral -> reviewLevel=2`
  - `pendingFollow -> followStatus=1`
  - `urgent -> riskLevel=2`
  - `replied -> replyStatus=true`
  - `managerTodoPending -> onlyManagerTodo=true & managerTodoStatus=1`
  - `managerTodoClaimTimeout -> onlyManagerTodo=true & managerSlaStatus=CLAIM_TIMEOUT`
  - `managerTodoFirstActionTimeout -> onlyManagerTodo=true & managerSlaStatus=FIRST_ACTION_TIMEOUT`
  - `managerTodoCloseTimeout -> onlyManagerTodo=true & managerSlaStatus=CLOSE_TIMEOUT`
  - `managerTodoClosed -> onlyManagerTodo=true & managerTodoStatus=4`
- 台账页在进入时解析 route query，并填充到现有筛选表单

## 6. 测试策略
- 后端：
  - controller 测试先写红灯，验证 page/get 会返回新增名称字段
- 前端：
  - 把看板到台账的 query 映射与台账解析逻辑下沉到纯函数模块
  - 用 `node --test` 验证 query 构造和解析行为

## 7. No-Go
1. 不引入自动通知、自动补偿、自动奖励。
2. 不补造账号级店长归属。
3. 不把本批 admin-only 增强写成 release-ready 证据。
4. 不修改 overlay 以外的无关页面。
