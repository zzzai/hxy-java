# Booking Review 真实绑定来源闭环设计

## 1. 目标
- 在现有 `manager routing` 只读治理工作台上，补齐“绑定来源真值”视图。
- 让运营在 `BLOCKED_NO_OWNER` 或双通道缺失场景里，能明确区分：
  - 当前路由来源已确认
  - 当前只是联系人已核出、还没转成店长绑定
  - 当前缺来源登记
  - 当前连联系人主数据都缺
  - 当前来源登记存在，但核验已过期
- 保持 `admin-only / read-only / Can Develop / Cannot Release / No-Go`，不在本批引入在线改绑、自动修复或新的写链路。

## 2. 当前真值边界
- 当前真实可用数据只有两层：
  - `booking_review_manager_account_routing`
    - `storeId`
    - `managerAdminUserId`
    - `managerWecomUserId`
    - `bindingStatus`
    - `source`
    - `lastVerifiedTime`
  - 门店主数据
    - `ProductStoreDO.contactName`
    - `ProductStoreDO.contactMobile`
- 当前并不存在可直接接入的“第三套真实绑定来源系统”。
- 因此本批不能假装打通新系统，也不能伪造“已从企微组织架构自动同步”的闭环口径。

## 3. 方案对比

### 方案 A：继续沿用 `sourceClosureStatus` 二元状态
- 优点：改动最小。
- 缺点：只能表达“来源已登记/待登记”，无法回答“为什么待登记、下一步找谁、是否还有联系人可追”。

### 方案 B：在现有查询服务中派生来源真值分层
- 优点：
  - 不新增表，不引入写链；
  - 后端统一输出“来源结论 / 来源说明 / 下一步动作”；
  - 前端可做概览、筛选、列表和详情复用；
  - 与当前 1000 门店、1 门店 1 店长、App/企微双通道派发治理场景完全一致。
- 缺点：需要同步改 VO、查询服务、前端类型、页面和测试。
- 结论：采用。

### 方案 C：直接做门店店长绑定修改工具
- 优点：看起来一步到位。
- 缺点：
  - 越过只读治理边界；
  - 需要额外校验权限、来源、冲突、审计；
  - 当前没有真实来源系统支撑，容易写成假闭环。
- 结论：本批禁止。

## 4. 设计结论

### 4.1 新增来源真值派生字段
- 在 `BookingReviewManagerAccountRoutingRespVO` 中新增：
  - `sourceTruthStage`
  - `sourceTruthLabel`
  - `sourceTruthDetail`
  - `sourceTruthActionHint`
- 在 `BookingReviewManagerAccountRoutingSummaryRespVO` 中新增：
  - `routeConfirmedCount`
  - `sourceMissingCount`
  - `contactOnlyPendingBindCount`
  - `contactMissingCount`
  - `verifyStaleCount`
- 在 `BookingReviewManagerAccountRoutingPageReqVO` 中新增：
  - `sourceTruthStage`

### 4.2 来源真值阶段定义
- `ROUTE_CONFIRMED`
  - 路由记录存在，`source` 已登记，且当前能给出明确来源口径。
- `SOURCE_MISSING`
  - 路由记录存在，但 `source` 为空或 `UNKNOWN`。
- `CONTACT_ONLY_PENDING_BIND`
  - 当前没有稳定双通道路由，但门店联系人已核出，可作为补绑定入口。
- `CONTACT_MISSING`
  - 路由缺失，且门店联系人也未核出，属于门店主数据缺口。
- `VERIFY_STALE`
  - 路由和来源已登记，但 `lastVerifiedTime` 过旧，需要复核。

### 4.3 派生优先级
- `VERIFY_STALE` 优先于 `ROUTE_CONFIRMED`
  - 因为它意味着“来源曾登记，但真值已过期”。
- `SOURCE_MISSING` 优先于 `ROUTE_CONFIRMED`
  - 因为来源未登记时不能算闭环。
- `CONTACT_ONLY_PENDING_BIND` / `CONTACT_MISSING`
  - 只在没有稳定路由时进入。

### 4.4 前端只读增强
- 在现有“覆盖率概览”“治理工作台概览”之外，新增“来源闭环概览”：
  - 来源已确认
  - 来源缺失
  - 联系人待转绑定
  - 联系人缺失
  - 来源待复核
- 新增快捷筛选：
  - 只看来源已确认
  - 只看来源缺失
  - 只看联系人待转绑定
  - 只看联系人缺失
  - 只看来源待复核
- 详情和列表新增：
  - 来源结论
  - 来源说明
  - 下一步动作

## 5. 明确不做
- 不做在线绑定 / 解绑
- 不做自动推断店长账号
- 不改通知发送逻辑、重试逻辑和真实派发逻辑
- 不把“联系人已核出”写成“店长绑定已闭环”
- 不把“来源已确认”写成“Release Ready”

## 6. 验证策略
- Node：
  - API 类型新增 `sourceTruth*` 与来源概览字段
  - 页面新增来源概览、筛选、列表/详情字段断言
- Java：
  - 查询服务新增来源真值阶段、来源概览统计和筛选断言
  - Controller 保持接口层透传验证
- 文档：
  - 设计与实施计划落盘
  - backlog / evidence / truth 文档同步只读治理结论
