# MiniApp Booking Write-Chain Closure Review v1（2026-03-24）

## 1. 本轮目标与真值来源
- 目标：在不伪造 release-ready 结论的前提下，收口 `booking` 当前最明确的协议漂移，减少 query/query-helper 与 controller/VO 之间的假闭环。
- 只认以下真实输入：
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/pages/booking/technician-detail.vue`
  - `yudao-mall-uniapp/pages/booking/service-select.vue`
  - `yudao-mall-uniapp/pages/booking/order-confirm.vue`
  - `yudao-mall-uniapp/pages/booking/addon.vue`
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
| create 商品来源真值 | `technician-detail -> service-select -> order-confirm` 已固定；`buildBookingCreatePayload` 只接受显式 `spuId/skuId` | `order-confirm` 不再从 `slot` 猜商品来源，create 页面级写链真值已收口 |
| addon 商品来源真值 | `addonType=1` 可沿用母单商品；`addonType=2/3` 必须经 `service-select` 回传显式 `spuId/skuId` | add-on 不再只靠 `parentOrderId,addonType` 裸提交，升级 / 加项目 商品来源已收口 |

## 3. 本轮仍未解除的 blocker

| blocker | 当前状态 | 为什么仍阻断 |
|---|---|---|
| release 级样本 | 仍阻断 | 03-24 已补 simulated selftest pack + evidence gate，但仍缺 create / cancel / addon 的真实发布级 success/failure 样本、回放与巡检证据 |
| gray / rollback / sign-off | 仍阻断 | 即使页面级真值已收口，当前仍缺真实灰度、回滚、门禁签字材料，不能把 app 写链改口成可放量 |
| 技师 fallback 字段 | 仍阻断 | `title/specialties/status` 仍是页面 fallback，没有 backend 绑定 |

## 4. 当前阶段结论

| 维度 | 结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 工程状态 | `Can Develop / Cannot Release` |
| 当前是否 admin / app 可继续开发 | Yes |
| 当前是否可放量 | No |
| Release Decision | `No-Go` |

## 5. 单一真值结论
1. 本轮已把 `data.list/data.total`、`payOrderId`、`order-confirm -> slot detail`、`duration`、`create/addon 商品来源真值` 这几类明确漂移从 blocker 列表中移除。
2. 本轮没有把 `create / cancel / addon` 改写成 release-ready。
3. 03-24 新增的 simulated selftest pack 只把“发布证据结构门禁”固定下来，没有替代真实发布样本。
4. 当前 booking 真正剩余的核心问题，已经收敛到“真实发布证据 + gray / rollback / sign-off + fallback 字段”。
