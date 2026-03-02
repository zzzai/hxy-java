# P0 Template Binding + Booking Placeholder + Payment CI Design

## Scope

1. SPU/SKU 主数据显式绑定 `templateVersionId`。
2. 预约网关“主动占位”实接（查不到 booking 时自动创建占位并回写服务履约单）。
3. 支付 P0 #23 CI 收口（合并前自动回归阻断）。

## Goals

- 服务商品下单不再仅依赖前端透传模板版本，支持从商品主数据兜底读取。
- 支付成功后的服务履约单在 booking 侧具备可追踪占位记录，避免长期“待预约无单号”。
- 在 PR 阶段自动跑支付核心回归，形成可配置 required check。

## Design Decisions

### A. TemplateVersion 主数据绑定

- 在 `product_spu`、`product_sku` 增加 `template_version_id`。
- `ProductSpuSaveReqVO`、`ProductSkuSaveReqVO`、对应 DO/DTO/RespVO 新增字段。
- 规则：
  - 服务商品（`productType=SERVICE`）`SPU.templateVersionId` 必填。
  - `SKU.templateVersionId` 为空时继承 SPU；非空时必须与 SPU 一致（当前阶段保持单模板约束）。
  - SPU 绑定模板版本需满足：存在、已发布、类目匹配。
- 交易价格计算阶段新增兜底：
  - 请求项未传 `templateVersionId` 时，优先读取 `SKU.templateVersionId`，再回退 `SPU.templateVersionId`。
  - 仍为空则维持现有服务商品拦截。

### B. 预约占位实接

- 在 booking 服务新增“占位预约创建”接口（幂等）：
  - 以 `payOrderId` 幂等，已存在直接复用。
  - 不依赖时间槽，生成占位 booking 单号。
  - 状态使用 `WAIT_BOOKING`（新增枚举值），用于与待支付/已支付区分。
- `ServerTradeServiceBookingGateway` 逻辑改为：
  - 优先按 `payOrderId` 查 booking；
  - 查不到则主动创建占位 booking；
  - 拿到 booking 单号后调用 `tradeServiceOrderService.markBooked` 回写服务履约单。

### C. Payment CI #23 收口

- 新增 `payment-stagea-p0-23` workflow（`pull_request` + `workflow_dispatch`）：
  - 运行支付核心 fast-track（兼容回归 + 韧性回归 + notify smoke）。
  - 产出 artifact 与 summary，失败即阻断。
- 更新 `setup_github_required_checks.sh`，支持把 `p0-23` context 纳入 required checks。

## Risks and Mitigation

- 历史服务商品未绑定模板版本：SQL 回填 + 运行时兜底，避免一次性硬中断。
- booking 占位状态新增影响旧逻辑：仅用于占位场景，不进入支付超时任务。
- CI 时间增加：限定路径触发 + workflow_dispatch 可手动补跑。

## Acceptance

- 服务商品下单请求未传模板版本，且主数据已绑定时，可成功完成价格计算并落模板快照。
- 支付成功后若 booking 不存在，会自动生成占位 booking 并把服务履约单更新为已预约（有 bookingNo）。
- PR 改动命中支付高风险路径时自动触发 `stagea-p0-23`，可作为 required check。
