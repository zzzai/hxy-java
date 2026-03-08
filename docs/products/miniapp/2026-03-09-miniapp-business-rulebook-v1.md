# MiniApp Business Rulebook v1 (2026-03-09)

## 1. 目的与适用范围
- 目的：冻结小程序发布前核心业务规则，消除页面间口径漂移。
- 适用范围：`feat/ui-four-account-reconcile-ops` 对应的 miniapp 交易、预约、售后、营销与资产场景。
- 对齐基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`

## 2. 统一规则约束
- 状态以服务端状态机和聚合接口为准，端侧不推断业务状态。
- 错误处理按错误码执行，不按错误文案执行。
- 降级场景遵循 fail-open（合同允许）或 fail-close（业务冲突）明确边界。
- 所有关键链路日志必须可检索：`runId/orderId/payRefundId/sourceBizNo/errorCode`。

## 3. 业务规则字典（发布冻结）

| 规则ID | 业务能力 | 前置条件 | 错误码 | 降级语义 | 责任Owner | 验收口径 |
|---|---|---|---|---|---|---|
| BR-001 | 可下单 | 商品可售、库存充足、订单参数完整 | `ORDER_NOT_FOUND(1011000011)`（回流校验） | 订单回流缺失不白屏，保留重试与返回列表动作 | Trade Domain Owner | 1 happy + 1 not-found + 1 degraded |
| BR-002 | 可预约 | 技师可服务、时段可用、父订单状态允许 | `BOOKING_ORDER_NOT_EXISTS`, `BOOKING_ORDER_STATUS_ERROR`, `TIME_SLOT_NOT_AVAILABLE` | 时段冲突 fail-close；支付结果查询缺失 fail-open，展示待确认 | Booking Domain Owner | 预约创建/冲突拦截/支付降级三路径通过 |
| BR-003 | 可退款 | 售后单存在、退款口径合法、未触发幂等冲突 | `AFTER_SALE_NOT_FOUND(1011000100)`, `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)`, `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` | 工单同步异常 fail-open（`TICKET_SYNC_DEGRADED`），不回滚主链路 | Trade + Booking Domain Owner | 退款成功/业务阻断/降级告警可追溯 |
| BR-004 | 可领券 | 券模板状态 `AVAILABLE`、用户资格通过 | `PROMOTION_COUPON_*` | 领券失败不播放成功态，页面可继续浏览与重试 | Promotion Domain Owner | 领取成功/重复领取/活动失效三路径通过 |
| BR-005 | 可兑换 | 积分余额与规则阈值满足 | `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 规则超限提示调整，不清空上下文，不阻断浏览 | Promotion + Member Domain Owner | 兑换成功/超限提示/返回操作可用 |
| BR-006 | 可核销 | 礼品卡/券状态有效、核销参数完整、未重复核销 | `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)`（同键异参冲突语义复用） | 核销通知失败可降级记录并异步补偿，不伪成功 | Trade + Promotion Domain Owner | 核销成功/重复核销冲突/降级补偿可查 |
| BR-007 | 可改约 | 原预约状态允许、目标时段可用、规则窗口内 | `SCHEDULE_CONFLICT`, `BOOKING_ORDER_STATUS_ERROR` | 业务冲突 fail-close；依赖查询超时可重试，保留用户已选参数 | Booking Domain Owner | 改约成功/冲突拦截/重试恢复三路径通过 |

## 4. 规则优先级与冲突处理
- 优先级：业务冲突规则 > 降级展示规则 > 文案规则。
- 冲突处理：
  - 同键同参：幂等命中，返回首次结果。
  - 同键异参：返回冲突错误码，禁止静默成功。
- 对于 `TICKET_SYNC_DEGRADED`：仅标记 warning，不可替代主业务结果码。

## 5. 发布前检查清单
1. 每条规则对应的页面、API、状态机、错误码已在矩阵文档可追踪。
2. 每条规则至少具备 1 条 happy path + 1 条错误码 path + 1 条降级 path。
3. 客服SOP与运营Playbook可引用同一错误码语义，不出现文案口径冲突。
4. 关键链路日志抽样检索通过率 >= 99%。

## 6. 变更流程
- 规则变更必须提交 PR，并同步更新：
  - `miniapp-feature-inventory-and-release-matrix`
  - `miniapp-errorcode-recovery-matrix`（若涉及错误码语义）
  - 对应 PRD/contract 文档
- 变更审批：Product Owner + Data Owner + 对应 Domain Owner。
