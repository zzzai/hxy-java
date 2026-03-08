# MiniApp 首页增长 PRD v1（2026-03-09）

## 0. 文档定位
- 目标：面向「首页增长」补齐产品执行口径，保证 UI 评审与业务落地一致。
- 约束基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`

## 1. 场景目标与非目标
### 1.1 场景目标
- 将首页从「模板展示页」升级为「可闭环增长入口」：可达券、积分、搜索、下单链路。
- 强绑定活动真值：营销样式、倒计时、领取反馈以后端状态为准，避免假成功。
- 在下游异常时保持主链路可用：可浏览、可重试、可回到订单/客服。

### 1.2 非目标
- 不改首页装修引擎（组件 DSL、后台模板结构不在本 PRD 范围）。
- 不新增后端错误码，仅复用已冻结语义。
- 不定义推荐算法策略，只定义端侧承接与状态口径。

## 2. 页面信息架构与关键流程
### 2.1 IA（首页增长）
| 页面路由 | 角色 | 关键能力 | 依赖接口 |
|---|---|---|---|
| `/pages/home/index` | 增长入口 | 首页卡片曝光、活动 CTA 导流 | 首页装修/活动聚合接口 |
| `/pages/coupon/center` | 拉新促活 | 领券、查看已领状态 | `GET /promotion/coupon-template/page`, `POST /promotion/coupon/take` |
| `/pages/point/mall` | 留存转化 | 积分活动浏览、积分消耗入口 | `GET /promotion/point-activity/page`, `GET /member/point/record/page` |
| `/pages/index/search` | 需求承接 | 关键词直达商品搜索 | 商品检索链路 |
| `/pages/pay/result` | 转化闭环 | 支付结果确认与回流订单 | `GET /trade/order/pay-result` |
| `/pages/common/exception` | 全局兜底 | 网络异常/服务降级恢复动作 | 前端兜底页 |

### 2.2 关键流程
1. 首页曝光 -> 卡片点击 -> 进入券中心/积分商城/搜索。
2. 领券链路：点击领取 -> 后端校验成功才展示成功态 -> 回流商品列表或结算页。
3. 支付回流：支付完成回到支付结果 -> 订单页/首页二次触达推荐位。

## 3. 状态机映射（引用统一状态机）
统一引用：`docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`

| 业务对象 | 统一状态机 | 首页展示规则 | 非法/异常处理 |
|---|---|---|---|
| 券模板 | `AVAILABLE | EXPIRED | OUT_OF_STOCK` | 仅 `AVAILABLE` 展示可点击 CTA | 非可领态展示禁用文案，不触发领取请求 |
| 用户券 | `UNUSED | LOCKED | USED | EXPIRED` | 首页只展示可用数量，不展示可用则置灰入口 | 状态冲突以服务端返回为准 |
| 积分活动 | `ONLINE | OFFLINE | SOLD_OUT | EXPIRED` | 仅 `ONLINE` 可跳转可消费页面 | 其余状态可浏览不可下单 |
| 支付视图 | `WAITING | SUCCESS | REFUNDED | CLOSED` | 首页回流卡片基于支付结果态展示动作按钮 | pay 缺失时走降级提示，不返回假成功 |

## 4. 错误码与降级语义
| Code/Key | 触发场景 | 前端动作 | 降级语义 |
|---|---|---|---|
| `PROMOTION_COUPON_*` | 领券失败（已领/库存不足/活动失效） | 阻断成功动画，提示错误码与重试/返回 | 保持页面可浏览，不阻断其他入口 |
| `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 积分兑换超过规则阈值 | 提示调整数量/方案 | 不清空用户上下文，允许继续浏览 |
| `ORDER_NOT_FOUND(1011000011)` | 支付后回流订单缺失 | 显示可恢复空态，提供返回订单列表按钮 | 不白屏，不自动跳错页 |
| `PAY_ORDER_NOT_FOUND`（`degradeReason`） | pay 聚合缺失支付单 | 展示“待确认”+手动刷新 | fail-open，主链路继续 |
| `TICKET_SYNC_DEGRADED` | 下游工单/审计链路异常 | 展示非阻断 warning 标签 | 主业务成功，异步补偿 |

## 5. 埋点事件（最小集）
| 事件名 | 触发时机 | 必填属性 |
|---|---|---|
| `page_view` | 进入首页 | `route`, `scene`, `traceId?` |
| `home_growth_card_click` | 点击首页增长卡片 | `route`, `cardType`, `activityId?`, `position` |
| `coupon_take` | 提交领券请求并返回 | `templateId`, `resultCode`, `errorCode?` |
| `point_activity_view` | 积分活动区曝光 | `activityId`, `status`, `route` |
| `pay_result_view` | 支付结果页曝光（首页回流） | `orderId`, `payResultCode`, `degraded` |

## 6. 验收清单
### 6.1 Happy Path
- [ ] 首页卡片可进入券中心、积分商城、搜索，跳转参数完整。
- [ ] `POST /promotion/coupon/take` 成功后才出现“领取成功”反馈。
- [ ] 支付成功后回流首页可见正确转化引导（订单/再购入口）。

### 6.2 业务错误
- [ ] 领券失败时展示 `PROMOTION_COUPON_*` 码，不展示成功态。
- [ ] 积分规则超限时展示 `1011003004` 并保留用户选择。
- [ ] 订单缺失时展示 `1011000011` 空态与返回动作。

### 6.3 降级路径
- [ ] `PAY_ORDER_NOT_FOUND` 时显示待确认+手动刷新，不阻断页面。
- [ ] `TICKET_SYNC_DEGRADED` 时保留主链路成功，展示 warning。
- [ ] 网络失败进入异常兜底页，支持重试与返回首页。
