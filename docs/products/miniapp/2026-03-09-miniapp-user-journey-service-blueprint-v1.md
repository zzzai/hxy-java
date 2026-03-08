# MiniApp 用户旅程与服务蓝图 v1（2026-03-09）

## 1. 文档目标与对齐基线
- 目标：把“拉新 -> 下单 -> 预约 -> 履约 -> 售后 -> 复购”打通为可执行服务蓝图，明确前台与门店协同动作。
- 对齐基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
  - `docs/contracts/2026-03-09-miniapp-gift-card-domain-contract-v1.md`
  - `docs/contracts/2026-03-09-miniapp-referral-domain-contract-v1.md`
  - `docs/contracts/2026-03-09-miniapp-technician-feed-contract-v1.md`

## 2. 全旅程主线与异常线
### 2.1 主流程
1. 拉新触达（首页/邀请/券）引导用户浏览并完成首单。
2. 支付成功后用户进入预约链路并由门店接单排班。
3. 到店后完成核销与履约，用户进入售后/评价/复购分支。
4. 售后完成后通过资产回补与触达策略驱动复购。

### 2.2 异常流程
1. 支付结果异常：`PAY_ORDER_NOT_FOUND` 降级为待确认，不假成功。
2. 预约冲突：`TIME_SLOT_NOT_AVAILABLE(1030003001)` / `SCHEDULE_CONFLICT(1030002001)` 阻断并改约。
3. 退款受阻：`AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)` 进入人工审核。
4. 工单同步降级：`TICKET_SYNC_DEGRADED` 标记 warning，主链路不回滚。

## 3. 用户旅程服务蓝图（触点明细）

| 阶段 | 触点 | 页面路由 | 关键动作 | 真值系统 | 失败降级 | Owner | SLA |
|---|---|---|---|---|---|---|---|
| 拉新 | 首页曝光 | `/pages/home/index` | 查看活动卡、进入搜索/券中心 | Promotion + Product | 活动接口异常时展示基础首页模块，不白屏 | 产品运营（增长） | 首屏可用 <= 2s |
| 拉新 | 邀请绑定 | `/pages/referral/index`（规划） | 绑定邀请码 | Referral Domain | 绑定异常返回错误码，不做静默成功 | 用户增长负责人 | 绑定反馈 <= 3s |
| 拉新 | 领券触达 | `/pages/coupon/center` | 领取优惠券 | Promotion Coupon | `PROMOTION_COUPON_*` 阻断成功动效，保留可浏览 | 活动运营 | 领券结果 <= 2s |
| 下单 | 搜索选品 | `/pages/index/search` -> `/pages/goods/list` | 搜索、筛选、进详情 | Product Domain | 列表异常返回空态+重试 | 商品运营 | 检索响应 <= 2s |
| 下单 | 订单确认 | `/pages/order/confirm` | 提交订单 | Trade Domain | 提交失败按码提示，保留购物上下文 | 交易产品经理 | 提交反馈 <= 3s |
| 下单 | 支付结果 | `/pages/pay/result` | 查看支付状态、回流订单 | Trade 聚合 + Pay | `PAY_ORDER_NOT_FOUND` 降级 WAITING + 手动刷新 | 支付产品经理 | 轮询确认 <= 10s |
| 预约 | 预约列表 | `/pages/booking/order-list` | 查看待服务预约单 | Booking Domain | 查询失败回退空态，不影响其他入口 | 预约产品经理 | 列表响应 <= 2s |
| 预约 | 预约改约/取消 | `/pages/booking/order-detail` | 取消或改时段 | Booking Domain | 状态冲突返回业务码，不自动重试 | 预约运营 | 操作反馈 <= 3s |
| 门店侧 | 接单（强制） | `店端/门店工作台-接单池` | 门店确认接单 | Booking + Store Ops | 下游异常时记录待处理并通知值班 | 门店店长 | 新单 5 分钟内接单 |
| 门店侧 | 排班（强制） | `店端/门店排班台` | 指派技师与时间段 | Booking Schedule | `TIME_SLOT_NOT_AVAILABLE` 时自动推荐可用时段 | 门店调度 | 10 分钟内完成排班 |
| 履约 | 到店核销 | `店端/核销页` + `/pages/booking/order-detail` | 扫码/口令核销 | Booking Fulfillment | 核销失败进入人工核验，不直接拒绝服务 | 门店前台主管 | 到店后 3 分钟内核销 |
| 履约 | 服务中状态 | `/pages/booking/order-detail` | 服务进度可视 | Booking + Trade Service | 同步失败仅告警，不回滚服务状态 | 门店技师长 | 状态同步 <= 30s |
| 售后 | 发起售后 | `/pages/order/aftersale/apply` | 提交售后申请 | Trade After-sale | 旧后端不支持时降级人工通道 | 售后运营 | 申请反馈 <= 3s |
| 售后 | 退款进度 | `/pages/refund/progress` | 查看退款节点 | Trade After-sale + Pay | pay 缺失按售后状态回退 | 交易客服 | 进度刷新 <= 5s |
| 门店侧 | 客诉处置（强制） | `店端/客诉工单台` | 处理客诉与回访记录 | CS/Ticket System | `TICKET_SYNC_DEGRADED` 时本地台账先落，后补工单 | 客服主管 + 店长 | P1 30 分钟首响，P0 5 分钟首响 |
| 复购 | 资产回补 | `/pages/user/wallet/*` `/pages/coupon/list` | 查看返券/返积分/退款回补 | Member + Promotion + Pay | 资产接口失败时模块级降级，不影响主页面 | 会员运营 | 资产可见延迟 <= 10 分钟 |
| 复购 | 二次触达 | `/pages/home/index` + 通知触达 | 复购推荐、召回活动 | Growth + Notification | 触达系统失败按补发策略执行 | CRM 运营 | 触达任务执行成功率 >= 98% |

## 4. 门店侧动作标准（接单/排班/核销/客诉）
| 门店动作 | 输入 | 输出 | 风险码/信号 | 处置规则 |
|---|---|---|---|---|
| 接单 | 新预约单 | 接单确认记录 | `BOOKING_ORDER_NOT_EXISTS(1030004000)` | 校验订单有效性，异常转客服 L2 |
| 排班 | 接单成功单 + 技师资源 | 排班计划 | `TIME_SLOT_NOT_AVAILABLE(1030003001)` / `SCHEDULE_CONFLICT(1030002001)` | 自动推荐备选时段并短信通知用户改约 |
| 核销 | 到店订单 + 核销凭证 | 核销成功/失败 | `BOOKING_ORDER_STATUS_ERROR(1030004001)` | 失败时人工核验一次后再拒绝 |
| 客诉 | 用户投诉单 | 客诉结论 + 回访 | `TICKET_SYNC_DEGRADED` | 先落本地处置，再补录工单系统 |

## 5. 旅程验收检查点（产品视角）
- [ ] 每个阶段至少有 1 个可恢复动作（重试/回退/客服）。
- [ ] 关键异常（支付结果、预约冲突、退款受阻、工单降级）均有明确 Owner 与 SLA。
- [ ] 门店四个关键动作（接单、排班、核销、客诉）都有输入/输出/处置规则。
- [ ] 全链路可检索字段保持一致：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
