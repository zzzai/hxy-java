# BO-004 Admin Truth Closure Design

## 目标
- 把 `BO-004 技师提成明细 / 计提管理` 从 `controller-only truth` 升级为真实的后台页面/API 真值闭环。
- 补齐独立页面文件、独立前端 API 文件、菜单 SQL、写后回读前端证据与专项测试。
- 固定阶段结论为 `admin-only 可用 / Can Develop / Cannot Release`，不把本轮工程闭环误写成 release-ready。

## 当前真值
- 后端已有真实 controller：`TechnicianCommissionController`
- 已存在 8 条真实 `/booking/commission/*` 接口。
- 历史 blocker 不是“没有能力定义”，而是：
  - 没有独立后台页面文件
  - 没有独立前端 API 文件
  - 没有页面到 `/booking/commission/*` 的真实 binding 证据
  - 写接口仍有 `true` 但 no-op 风险
  - 没有 release 级样本与灰度/回滚证据

## 方案比较

### 方案 A：继续复用 `commission-settlement` 页面
- 优点：改动最少。
- 缺点：会继续混淆 `BO-003` 与 `BO-004`，破坏既有文档边界。
- 结论：不采用。

### 方案 B：新增独立 booking 页面与独立 API 文件
- 形态：
  - 页面：`src/views/mall/booking/commission/index.vue`
  - API：`src/api/mall/booking/commission.ts`
  - 菜单：新增独立 SQL
- 优点：
  - 与后端 `/booking/commission/*` 路径同源
  - 最容易形成独立 page/API binding 真值
  - 不污染 `BO-003`
- 缺点：
  - 需要补菜单与前端测试
- 结论：采用。

### 方案 C：把页面挂到 `finance` 目录
- 优点：概念上更贴近财务运营。
- 缺点：与后端 booking 路径、现有 overlay 目录和既有文档审查范围不一致，会增加额外迁移成本。
- 结论：当前不采用。

## 采用方案
- 采用方案 B。
- 独立页面固定落在 booking 目录，路径与能力边界如下：
  - 页面：`mall/booking/commission/index`
  - API：`/booking/commission/*`
  - 菜单 path：`booking-commission`

## 页面设计

### 1. 页面结构
- 顶部说明区：
  - 明确 `BO-004` 当前是 admin-only 运营页
  - 明确 `Boolean true != 真实成功`
  - 明确所有写操作必须写后回读
- 查询区：
  - `technicianId`
  - `orderId`
  - `storeId`
  - 查询 / 重置
- 汇总区：
  - 当前佣金记录数
  - 当前待结算金额
  - 当前门店配置数
- 主体区：
  - Tab1：佣金记录
  - Tab2：门店佣金配置
- 结果说明区：
  - 最近一次写后回读结果
  - 若接口成功但读后未变，显式提示 `no-op 风险`

### 2. 佣金记录 Tab
- 查询策略：
  - `orderId` 有值时优先按订单查询
  - 否则按技师查询
  - 无 `technicianId/orderId` 时不自动发记录查询
- 表格字段：
  - `id`
  - `technicianId`
  - `orderId`
  - `storeId`
  - `commissionType`
  - `baseAmount`
  - `commissionRate`
  - `commissionAmount`
  - `status`
  - `sourceBizNo`
  - `settlementId`
  - `settlementTime`
  - `createTime`
- 动作：
  - 单条直结
  - 按技师批量直结
- 写后回读：
  - 单条直结后重新拉列表与待结算金额
  - 批量直结后重新拉列表与待结算金额
  - 若目标记录未从 `PENDING` 变为 `SETTLED`，或待结算金额未按预期变化，则提示“接口返回成功，但读后未观察到变化”

### 3. 配置 Tab
- 查询：按 `storeId`
- 表格字段：
  - `id`
  - `storeId`
  - `commissionType`
  - `rate`
  - `fixedAmount`
  - `createTime`
  - `updateTime`
- 动作：
  - 新增配置
  - 编辑配置
  - 删除配置
- 写后回读：
  - 保存后重新查询配置列表，确认新增/更新结果
  - 删除后重新查询配置列表，确认目标配置消失
  - 若接口成功但列表未变化，提示 `no-op 风险`

## API 设计
- 新增独立文件：`src/api/mall/booking/commission.ts`
- 只承接真实 8 条接口：
  - `getCommissionListByTechnician`
  - `getCommissionListByOrder`
  - `getPendingCommissionAmount`
  - `settleCommission`
  - `batchSettleCommission`
  - `getCommissionConfigList`
  - `saveCommissionConfig`
  - `deleteCommissionConfig`
- TypeScript 接口需完整承接真实字段，不补造：
  - `degraded`
  - `degradeReason`
  - admin 专属错误码常量

## 测试设计
- 前端 Node 测试先行：
  - API 文件存在且绑定 8 条真实路径
  - 页面存在并引用独立 API 文件
  - 页面含写后回读提示与 no-op 风险提示
  - 菜单 SQL 存在且绑定独立 route/component/permission
- 后端补 controller 测试：
  - 至少固定查询接口响应字段和写接口 `true` 包装行为
  - 明确当前 controller 仍没有稳定 fail-close 业务错误外显

## 文档回填
- 必须更新：
  - `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
  - `docs/products/miniapp/2026-03-15-miniapp-finance-ops-technician-commission-admin-page-api-binding-truth-review-v1.md`
  - `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`
  - 项目总账相关文档中的 `BO-004` 状态
- 固定结论：
  - 已完成开发：是
  - admin-only 已可用：是
  - 已完成 release 证据：否
  - 当前可放量：否

## 非目标
- 不修改 `TechnicianCommissionController` 现有 `Boolean true` 返回语义。
- 不把 `BO-004` 提升为 release-ready。
- 不借用 `commission-settlement` 页面/API 作为 `BO-004` 证据。
- 不补造不存在的稳定 admin 错误码、`degraded` 字段或发布样本。
