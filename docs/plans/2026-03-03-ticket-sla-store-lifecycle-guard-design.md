# 工单 SLA 配置中心化与门店生命周期守卫增强设计

- 日期：2026-03-03
- 负责人：Codex + HXY
- 状态：已评审通过（待实现）

## 1. 背景与目标

当前售后复核工单 SLA 路由已经具备数据库规则能力（`trade_after_sale_review_ticket_route`），但管理端页面与发布口径尚未完成收口。门店生命周期守卫已覆盖部分阻塞项（商品映射、库存、库存流水），尚未覆盖交易域未结订单与在途售后工单，且缺少“告警到阻塞”的平滑切换能力。

本批 P1 的目标：

1. 工单 SLA 规则从硬编码口径彻底收口到表配置，并由管理端维护。
2. 门店生命周期守卫升级为全边界（4 类能力，含已有项共 5 项），按守卫项独立开关从 `WARN` 渐进到 `BLOCK`。
3. 保障模块边界清晰，避免 `product` 直接依赖 `trade` 内部实现。

## 2. 明确范围

### 2.1 In Scope

1. SLA 路由配置中心：以 `trade_after_sale_review_ticket_route` 为唯一权威。
2. SLA 路由管理端页面（overlay-vue3）接入与权限/菜单收口。
3. 门店生命周期守卫新增交易域统计能力：
   - 未结订单
   - 在途售后工单
4. 生命周期守卫按项模式开关（`WARN`/`BLOCK`）接入 `infra_config`。
5. 错误码与审计口径补齐。

### 2.2 Out of Scope

1. 租户级/门店级 SLA 路由差异化配置（本批仅全局）。
2. 生命周期守卫异步投影表与复杂缓存体系。
3. 运营告警中心、通知编排等二期治理能力。

## 3. 关键决策

1. **SLA 路由权威源**：继续沿用 `trade_after_sale_review_ticket_route`，不改为 `infra_config` JSON。
2. **路由优先级**：`ruleCode > ticketType+severity > ticketType default > global default`。
3. **生命周期守卫架构**：`product` 通过 `trade-api` 调用守卫统计聚合接口，不直接查 `trade_*` 表。
4. **分阶段语义**：`WARN` 允许流转仅记审计；`BLOCK` 直接抛错阻断。
5. **开关粒度**：按守卫项独立 key（不使用全局单开关）。

## 4. 架构设计

### 4.1 SLA 路由配置中心

- Domain：`trade`
- 数据源：`trade_after_sale_review_ticket_route`
- 后端能力：
  - `AfterSaleReviewTicketRouteController`
  - `AfterSaleReviewTicketRouteServiceImpl`
  - `DefaultAfterSaleReviewTicketRouteProvider`
- 生效机制：规则变更时主动 `invalidateCache`，运行时 30s TTL 再兜底刷新。
- 管理端：新增 `review-ticket-route` 维护页面，覆盖增删改查、启停与排序。

### 4.2 生命周期守卫全边界

- Domain：`product` 主控生命周期变更。
- 新增依赖：`product -> trade-api`（仅接口）+ `product -> infra`（配置读取 API）。
- 统计来源：`trade-api` 新增聚合查询，`trade` 内部实现统计 SQL。
- 判定链：
  1. gather guard metrics
  2. read per-guard mode
  3. BLOCK -> throw error
  4. WARN -> append audit reason and continue

## 5. 数据口径

### 5.1 守卫项定义

1. `mapping`：门店 SPU/SKU 映射数量 > 0（已存在）
2. `stock`：门店正库存数量 > 0（已存在）
3. `stockFlow`：库存流水 `PENDING/PROCESSING` 数量 > 0（已存在）
4. `pendingOrder`：`pick_up_store_id = storeId` 且订单状态 in `UNPAID/UNDELIVERED/DELIVERED`（新增）
5. `inflightTicket`：关联该门店订单的售后单状态 in `APPLYING_STATUSES`（新增）

### 5.2 开关配置（infra_config）

- `hxy.store.lifecycle.guard.mapping.mode`
- `hxy.store.lifecycle.guard.stock.mode`
- `hxy.store.lifecycle.guard.stock-flow.mode`
- `hxy.store.lifecycle.guard.pending-order.mode`
- `hxy.store.lifecycle.guard.inflight-ticket.mode`

取值：`WARN` / `BLOCK`，默认（缺省或非法）按 `BLOCK`。

### 5.3 与现有退款上限收紧改动的一致性

当前分支已引入退款上限收紧场景的人工审计决策，规则码为 `REFUND_LIMIT_CHANGED`：

1. 当 `afterSale.refundPrice > latestDecision.upperBound` 时，写入人工决策审计并触发手工复核工单更新。
2. 该规则码在 SLA 路由中若无 `RULE` 级显式配置，将按既定优先级回退到 `ticketType+severity / ticketType default / global default`，功能可用。
3. 为便于运营识别与精细化 SLA，本批建议在路由种子中补充 `RULE=REFUND_LIMIT_CHANGED`（可选但推荐）。

## 6. 接口设计

### 6.1 Trade API（新增）

建议新增：
- `TradeStoreLifecycleGuardApi`
- `TradeStoreLifecycleGuardStatRespDTO`

接口：
- `TradeStoreLifecycleGuardStatRespDTO getStoreLifecycleGuardStat(Long storeId)`

字段：
- `pendingOrderCount`
- `inflightTicketCount`

### 6.2 Product 生命周期守卫扩展

在 `ProductStoreServiceImpl` 中抽离 lifecycle guard evaluator：

- 输入：`storeId`、目标生命周期状态、原因
- 输出：命中项集合（含 mode 与 count）
- side effect：
  - BLOCK：抛对应错误码
  - WARN：reason 追加告警明细并写审计日志

## 7. 错误码与审计

### 7.1 错误码

复用：
- `STORE_LIFECYCLE_CLOSE_BLOCKED_BY_MAPPING`
- `STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK`
- `STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW`

新增：
- `STORE_LIFECYCLE_CLOSE_BLOCKED_BY_PENDING_ORDER`
- `STORE_LIFECYCLE_CLOSE_BLOCKED_BY_INFLIGHT_TICKET`

### 7.2 审计

`WARN` 命中统一写入门店审计日志 `reason`，格式固定：
- `LIFECYCLE_GUARD_WARN:<guardKey>:count=<n>`

## 8. 管理端设计

新增页面：
- `overlay-vue3/src/views/mall/trade/review-ticket-route/index.vue`

新增 API：
- `overlay-vue3/src/api/mall/trade/reviewTicketRoute.ts`

页面能力：
1. 分页筛选：scope/ruleCode/ticketType/severity/enabled
2. 新增/编辑：按 scope 动态展示必填项
3. 启停与删除
4. 排序字段维护

## 9. 发布与回滚

### 9.1 发布顺序

1. 发布 `trade`：SLA 路由页面/API、守卫统计 API
2. 发布 `product`：生命周期守卫接入新 API 与配置开关
3. 配置灰度：5 个守卫 key 先 `WARN`，再按项切 `BLOCK`

### 9.2 回滚策略

1. 生命周期一键降级：全部 key 回设 `WARN`
2. SLA 路由兜底：DB 规则异常时 provider 回退内建默认路由
3. 代码回滚支持 `trade/product` 独立进行

## 10. 测试策略

1. `trade`：
   - 路由 provider/route service/controller 单测
   - 新增守卫统计 API/service/mapper 单测
2. `product`：
   - `ProductStoreServiceImplTest` 增加 5 守卫项 `WARN/BLOCK` 分支
   - 单条/批量生命周期变更一致性用例
3. 前端：
   - 路由页面增删改查与 scope 动态校验 smoke

## 11. 验收标准（DoD）

1. SLA 规则可在管理端维护并生效，优先级符合既定链路。
2. 生命周期守卫覆盖五项，`WARN` 与 `BLOCK` 语义准确。
3. 单条与批量生命周期变更行为一致。
4. 核心单测通过，发布支持按项渐进切换。
