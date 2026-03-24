# MiniApp Booking Write-Chain Closure Review v1（2026-03-24）

## 1. 本轮目标与真值来源
- 目标：在不伪造 release-ready 结论的前提下，收口 `booking` 当前最明确的协议漂移，减少 query/query-helper 与 controller/VO 之间的假闭环。
- 只认以下真实输入：
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/pages/booking/order-confirm.vue`
  - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - `ruoyi-vue-pro-master/**/AppBookingOrderController.java`
  - `ruoyi-vue-pro-master/**/AppBookingOrderRespVO.java`
  - `ruoyi-vue-pro-master/**/AppTimeSlotController.java`
  - `ruoyi-vue-pro-master/**/AppTimeSlotRespVO.java`
  - `ruoyi-vue-pro-master/**/AppBookingOrderControllerTest.java`
  - `ruoyi-vue-pro-master/**/AppTimeSlotControllerTest.java`

## 2. 本轮已真实闭环项

| 主题 | 03-24 当前真值 | 影响 |
|---|---|---|
| order-confirm 时段读取 | 页面不再使用 `loadTimeSlots(technicianId, null)` 回捞，而是改为 `BookingApi.getTimeSlot(id)` + `loadTimeSlotDetail` 命中 `GET /booking/slot/get` | 已选时段读取从“列表回捞”改为“单点详情”，`duration` 已进入真实闭环 |
| 时间槽详情字段 | `AppTimeSlotRespVO` 已真实暴露 `duration` | `order-confirm` 页面可直接读取真实 `duration`，不再依赖模糊 slot 匹配 |
| 订单列表协议 | `GET /booking/order/list` 当前真实消费 `pageNo/pageSize/status`，返回 `PageResult<{list,total}>` | 页面不再存在 `data.list/data.total` 与 controller `data[]` 的结构漂移 |
| 订单支付绑定字段 | `AppBookingOrderRespVO` 已真实暴露 `payOrderId` | `order-list` / `order-detail` 的“去支付”分支不再是无响应字段假设 |
| 订单绑定字段 | `AppBookingOrderRespVO` 已真实暴露 `timeSlotId/spuId/skuId` | 下游继续补 create/addon 真值时，已有真实订单侧绑定字段可引用 |

## 3. 本轮仍未解除的 blocker

| blocker | 当前状态 | 为什么仍阻断 |
|---|---|---|
| create 商品来源 | 仍阻断 | 当前真实 route 链仍是 `technician-list -> technician-detail -> order-confirm`，但没有真实商品选择入口给出稳定 `spuId/skuId` 来源 |
| addon 升级 / 加项目 商品来源 | 仍阻断 | `/pages/booking/addon` 仍只提交 `parentOrderId,addonType`；`UPGRADE / ADD_ITEM` 没有真实商品选择器或可审计来源 |
| release 级样本 | 仍阻断 | 仍缺 create / cancel / addon 的发布级 success/failure 样本、回放与巡检证据 |
| 技师 fallback 字段 | 仍阻断 | `title/specialties/status` 仍是页面 fallback，没有 backend 绑定 |

## 4. 当前阶段结论

| 维度 | 结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 工程状态 | `Can Develop / Still Blocked` |
| 当前是否 admin / app 可继续开发 | Yes |
| 当前是否可放量 | No |
| Release Decision | `No-Go` |

## 5. 单一真值结论
1. 本轮已把 `data.list/data.total`、`payOrderId`、`order-confirm -> slot detail`、`duration` 这四类明确漂移从 blocker 列表中移除。
2. 本轮没有把 `create / cancel / addon` 改写成 release-ready。
3. 当前 booking 真正剩余的核心问题，已经收敛到“商品来源缺失 + 发布证据缺失”，而不是继续混在 query 漂移里。
