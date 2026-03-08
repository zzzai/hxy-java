# MiniApp Fulfillment & Logistics PRD v1 (2026-03-09)

## 1. 产品目标
- 建立小程序履约与物流的统一数据口径，使“履约可见、异常可追、运营可干预”。
- 支持三类履约：快递履约、到店核销履约、服务上门履约。

## 2. 事件字典（履约与物流）

| 事件名 | 说明 | 必填 | 枚举/取值 | 示例 |
|---|---|---|---|---|
| `fulfillment_order_bind` | 订单与履约单绑定 | 是 | `fulfillmentType=express/pickup/service` | `fulfillmentType=service` |
| `fulfillment_status_change` | 履约状态变更 | 是 | `WAIT_ACCEPT/ACCEPTED/IN_TRANSIT/IN_SERVICE/DONE/FAIL` | `IN_SERVICE` |
| `fulfillment_eta_publish` | 预计到达/完成时间发布 | 否 | `etaSource=system/manual` | `etaSource=system` |
| `logistics_track_sync` | 物流轨迹同步 | 否 | `syncResult=success/fail/degraded` | `syncResult=degraded` |
| `logistics_track_view` | 用户查看物流轨迹 | 否 | `trackType=express/service_progress` | `trackType=service_progress` |
| `fulfillment_exception_raise` | 履约异常上报 | 是 | `exceptionType=delay/lost/damage/no_show` | `exceptionType=delay` |
| `fulfillment_sla_breach` | SLA 超时命中 | 否 | `slaType=accept/dispatch/complete` | `slaType=complete` |
| `fulfillment_fake_success_block` | 假成功履约动效拦截 | 否 | `scene=delivered/arrived/completed` | `scene=completed` |

## 3. 字段字典（含必填/枚举/示例）

| 字段 | 必填 | 类型 | 枚举/规则 | 示例 |
|---|---|---|---|---|
| `orderId` | 是 | string | 交易订单 ID | `202603090301` |
| `fulfillmentId` | 是 | string | 履约单 ID | `F20260309001` |
| `fulfillmentType` | 是 | string | `express/pickup/service` | `express` |
| `fulfillmentStatus` | 是 | string | 见状态机 | `IN_TRANSIT` |
| `logisticsCompany` | 否 | string | 快递公司编码 | `YTO` |
| `logisticsNo` | 否 | string | 运单号 | `YT1234567890` |
| `serviceTechnicianId` | 否 | string | 服务履约技师 ID | `T_9001` |
| `etaTime` | 否 | string | 预计完成时间 ISO8601 | `2026-03-09T21:00:00+08:00` |
| `actualCompleteTime` | 否 | string | 实际完成时间 ISO8601 | `2026-03-09T20:45:00+08:00` |
| `slaDeadlineTime` | 否 | string | SLA 截止时间 | `2026-03-09T20:30:00+08:00` |
| `slaBreach` | 否 | bool | 是否超时 | `true` |
| `resultCode` | 否 | string | 履约结果码 | `SUCCESS` |
| `errorCode` | 否 | string | 错误码 | `1011000125` |
| `degraded` | 否 | bool | 降级标记 | `true` |
| `degradeReason` | 否 | string | 降级原因 | `TRACK_PROVIDER_TIMEOUT` |
| `riskLevel` | 否 | string | `P0/P1/P2` | `P1` |

## 4. 口径定义与归因规则
### 4.1 状态口径
- `DONE` 仅在服务端确认完成后写入；前端动画或用户本地点击不改变状态。
- `IN_TRANSIT/IN_SERVICE` 为过程态，不能作为成功率分母外的成功口径。
- `FAIL` 必须带 `errorCode` 或 `exceptionType`。

### 4.2 归因规则
- 履约时效归因键：`orderId + fulfillmentId`。
- SLA 归因：按“首次进入目标状态的时间戳”对比 `slaDeadlineTime`。
- 延迟责任归因：`delayOwner=platform/merchant/logistics/user/unknown`，默认 `unknown`，待运营复核后修正。

## 5. 风险分级与拦截规则

| 风险类型 | 分级 | 命中条件 | 拦截策略 |
|---|---|---|---|
| 合规风险 | P0 | 伪造履约完成、隐瞒失败或丢件状态 | 阻断用户成功态渲染，强制展示处理中/异常页 |
| 误导性营销 | P1 | 文案承诺“立即送达/绝不延误”与实时 SLA 不符 | 阻断活动文案发布，进入合规复审 |
| 假成功动效 | P0 | 服务端未确认 `DONE` 时展示“已送达/已完成”动画 | 强拦截并记录 `fulfillment_fake_success_block` |
| 降级不可审计 | P1 | `degraded=true` 且缺 `degradeReason/orderId` | 拦截埋点入库并报警 |

## 6. 与既有文档映射
- `analytics-funnel`：履约链路承接 `Pay -> Completion` 阶段，新增履约中间层事件用于拆分损耗。
- `copy-terminology`：统一使用“履约中/配送中/服务中/已完成/异常”，禁止“已成功”替代服务端未确认状态。
- `motion-accessibility`：完成态动效受“服务端确认后触发”约束；低性能设备保留文本状态，不依赖动画传达。

## 7. 验收标准
1. 事件与字段已在埋点 SDK 注册且字典一致。
2. `DONE` 状态无前端先行写入；抽样审计一致率 100%。
3. SLA 看板能按 `fulfillmentType/storeId/delayOwner` 切片。
4. 所有异常单均可通过 `orderId/fulfillmentId/errorCode` 检索。

## 8. 运营监控指标
- `履约完成率`：`DONE / (DONE + FAIL)`。
- `按时完成率`：`actualCompleteTime <= slaDeadlineTime` 占比。
- `履约异常率`：`fulfillment_exception_raise / fulfillment_status_change`。
- `降级率`：`degraded=true` 占履约事件比。
- `假成功拦截次数`：`fulfillment_fake_success_block` 日/周趋势。
- `物流轨迹可见率`：有轨迹详情的订单占已发货订单比。
