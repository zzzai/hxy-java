# Booking Review 门店绑定治理工作台设计

## 1. 目标
- 在现有 `manager routing` 只读核查页基础上，补成可运营的“门店绑定治理工作台”。
- 让运营在约 1000 门店规模下，快速区分“缺 App 账号 / 缺企微账号 / 双缺失 / 路由未启用 / 路由未生效 / 路由已过期 / 来源待闭环 / 长期未核验”。
- 保持 `admin-only / read-only / Can Develop / Cannot Release`，不在本批引入在线改绑、自动修复或发布口径升级。

## 2. 当前真值
- 当前已具备：
  - `storeId -> managerAdminUserId / managerWecomUserId` 路由模型
  - 路由覆盖率摘要
  - 缺 App / 缺企微 / 双缺失 / 双通道就绪 快捷筛选
  - `repairHint`、`routingLabel` 等只读结论
- 当前仍缺：
  - 运营优先级视角
  - 路由核验新鲜度视角
  - 来源闭环状态视角
  - 可直接转交主数据/账号治理同学的“治理交接文案”

## 3. 方案对比

### 方案 A：只在前端补说明文案
- 优点：改动最小。
- 缺点：无法在分页结果和汇总卡片中稳定复用；筛选和统计都要靠页面拼接，后续 `P1-2 / P1-3` 难接。

### 方案 B：后端统一派生治理字段，前端工作台消费
- 优点：治理优先级、核验状态、来源闭环状态和交接建议都有单一真值；后续审计二期和绑定来源闭环都可复用。
- 缺点：需要同步补 VO、查询服务、前端类型和页面。
- 结论：采用。

### 方案 C：直接做在线改绑工作台
- 优点：一步到位。
- 缺点：越过当前“只读治理”边界，直接触碰 `P1-3` 的真实绑定来源和写入权限问题，风险高。
- 结论：本批禁止。

## 4. 设计结论

### 4.1 后端派生字段
- 在 `BookingReviewManagerAccountRoutingRespVO` 中新增：
  - `governanceStage / governanceStageLabel`
  - `governancePriority / governancePriorityLabel`
  - `verificationFreshnessStatus / verificationFreshnessLabel`
  - `sourceClosureStatus / sourceClosureLabel`
  - `governanceOwnerLabel`
  - `governanceActionSummary`
- 在 `BookingReviewManagerAccountRoutingSummaryRespVO` 中新增：
  - `immediateFixCount`
  - `verifySourceCount`
  - `staleVerifyCount`
  - `sourcePendingCount`
  - `observeReadyCount`
- 统一由查询服务派生，不新增数据库字段。

### 4.2 治理规则
- `IMMEDIATE_FIX`：
  - `NO_ROUTE / PARTIAL_ROUTE / INACTIVE_ROUTE / EXPIRED_ROUTE`
  - 当前已经阻断双通道稳定派发，需要优先处理
- `WAIT_EFFECTIVE`：
  - `PENDING_EFFECTIVE`
  - 当前有路由但未到生效时间，需关注但不是来源闭环问题
- `VERIFY_SOURCE`：
  - 当前路由已可用，但 `source` 缺失或 `lastVerifiedTime` 为空/过旧
  - 为 `P1-3` 做治理准备
- `OBSERVE_READY`：
  - 当前双通道就绪，来源和核验也满足当前工作台阈值

### 4.3 核验新鲜度
- `UNVERIFIED`：`lastVerifiedTime` 为空
- `STALE_VERIFY`：`lastVerifiedTime` 距当前超过 7 天
- `RECENT_VERIFY`：7 天内已核验

### 4.4 来源闭环状态
- `SOURCE_PENDING`：`source` 为空或 `UNKNOWN`
- `SOURCE_READY`：已有稳定来源值

### 4.5 前端工作台增强
- 在现有覆盖率卡片之外新增“治理工作台概览”卡片：
  - 立即治理
  - 来源待闭环
  - 长期未核验
  - 可观察就绪
- 新增快捷筛选：
  - 只看立即治理
  - 只看来源待闭环
  - 只看长期未核验
  - 只看可观察就绪
- 表格新增：
  - 治理优先级
  - 治理分组
  - 核验状态
  - 来源闭环
  - 治理归口
  - 交接摘要

## 5. 明确不做
- 不做在线绑定/解绑
- 不做自动修复
- 不改 notify dispatch 真实发送逻辑
- 不把“治理工作台”写成“账号绑定已闭环”

## 6. 验证
- Node：路由页和 API 类型测试新增治理字段与快捷筛选断言
- Java：查询服务测试新增治理派生字段、汇总统计和筛选逻辑断言
- 回归：现有 manager routing / notify outbox 聚焦测试继续通过
