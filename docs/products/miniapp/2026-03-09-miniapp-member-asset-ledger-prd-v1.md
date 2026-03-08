# MiniApp 会员资产台账 PRD v1（2026-03-09）

## 0. 文档定位
- 目标：补齐「钱包+积分+优惠券+退款回补」统一资产视图口径，支撑业务落地与客服追溯。
- 约束基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`

## 1. 场景目标与非目标
### 1.1 场景目标
- 统一展示会员资产：钱包余额、钱包流水、积分余额/流水、优惠券状态。
- 打通退款回补可解释链路：用户可从资产页追溯到订单/售后进度。
- 降低客服人工成本：错误码、降级信息、关键检索字段可直接定位。

### 1.2 非目标
- 不改资金结算逻辑，不新增记账规则。
- 不新增台账后端表结构或新错误码。
- 不覆盖 B 端财务报表场景，仅定义 C 端会员资产口径。

## 2. 页面信息架构与关键流程
### 2.1 IA（会员资产）
| 页面路由 | 角色 | 关键能力 | 依赖接口 |
|---|---|---|---|
| `/pages/user/wallet/money` | 钱包总览 | 余额、收支统计、流水明细 | `/pay/wallet/get`, `/pay/wallet-transaction/page`, `/pay/wallet-transaction/get-summary` |
| `/pages/user/wallet/score` | 积分台账 | 积分余额、收入/支出记录 | `GET /member/point/record/page` |
| `/pages/coupon/list` | 券资产 | 领券中心 + 我的优惠券 | `GET /promotion/coupon-template/page`, `POST /promotion/coupon/take`, `GET /promotion/coupon/page` |
| `/pages/point/mall` | 积分资产消费 | 积分活动与兑换入口 | `GET /promotion/point-activity/page` |
| `/pages/after-sale/detail` | 退款追溯 | 退款金额、退款进度、回补结果 | `GET /trade/after-sale/get`, `GET /trade/after-sale/refund-progress` |

### 2.2 关键流程
1. 用户进入资产总览，按日期和类型查看钱包/积分变动。
2. 用户在券中心领券并在资产页看到状态变化。
3. 退款完成后，资产页可追溯退款单与售后状态（而不是仅展示结果文案）。

## 3. 状态机映射（引用统一状态机）
统一引用：`docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`

| 资产对象 | 统一状态机 | 页面展示规则 |
|---|---|---|
| 积分账户 | `ACCRUAL | CONSUME | REFUND_BACK | ADJUST` | 流水需区分正负向，时间序展示 |
| 券模板/用户券 | `AVAILABLE/EXPIRED/OUT_OF_STOCK` + `UNUSED/LOCKED/USED/EXPIRED` | 按状态分区展示，禁用态不可触发使用 |
| 售后退款 | `APPLY -> REVIEWING -> APPROVED -> REFUNDING -> REFUNDED/REJECTED` | 资产回补依赖退款终态，进度页可追溯 |
| 支付退款进度 | `REFUND_PENDING | REFUND_PROCESSING | REFUND_SUCCESS | REFUND_FAILED` | 进度态优先展示，不以文案猜测成功 |

## 4. 错误码与降级语义
| Code/Key | 触发场景 | 前端动作 | 降级语义 |
|---|---|---|---|
| `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 积分兑换超限 | 提示规则限制并保留当前选择 | 用户可调整后继续 |
| `AFTER_SALE_NOT_FOUND(1011000100)` | 退款追溯对象缺失 | 展示空态并返回售后列表 | 不阻断资产页其他模块 |
| `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)` | 套餐子项已履约，退款受阻 | 阻断退款动作并提示人工处理 | 不做静默成功 |
| `PROMOTION_COUPON_*` | 领券失败/重复领取 | 阻断成功提示，展示错误码 | 保持券列表可浏览 |
| `TICKET_SYNC_DEGRADED` | 审计/工单同步异常 | 展示 warning，不回滚主业务 | fail-open + 可追溯 |

## 5. 埋点事件（最小集）
| 事件名 | 触发时机 | 必填属性 |
|---|---|---|
| `page_view` | 进入资产页模块 | `route`, `userId`, `tab` |
| `wallet_transaction_view` | 钱包流水加载 | `route`, `dateRange`, `total` |
| `point_record_view` | 积分流水加载 | `route`, `addStatus`, `total` |
| `coupon_take` | 领券请求返回 | `templateId`, `resultCode`, `errorCode?` |
| `refund_progress_view` | 查看退款进度 | `afterSaleId`, `payRefundId?`, `progressCode` |

## 6. 验收清单
### 6.1 Happy Path
- [ ] 钱包余额、钱包流水、积分流水都可按日期过滤并分页加载。
- [ ] 领券成功后券状态更新到“已领取/可使用”。
- [ ] 退款完成后可从资产侧追溯到售后详情与进度页。

### 6.2 业务错误
- [ ] 积分超限返回 `1011003004` 且提示可操作文案。
- [ ] 售后不存在返回 `1011000100`，只影响当前追溯模块。
- [ ] 套餐履约冲突返回 `1011000125`，退款按钮阻断。

### 6.3 降级路径
- [ ] 工单同步异常按 `TICKET_SYNC_DEGRADED` 展示 warning，不回滚主流程。
- [ ] 领券失败走 `PROMOTION_COUPON_*`，页面继续可用。
- [ ] 资产页任一接口超时进入可重试空态，不出现白屏。
