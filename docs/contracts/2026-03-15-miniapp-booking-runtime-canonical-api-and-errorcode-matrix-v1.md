# MiniApp Booking Runtime Canonical API and ErrorCode Matrix v1 (2026-03-15)

## 1. 目标与取证范围
- 目标：把 booking runtime 当前真实 page/helper/API/controller/service 的 canonical method/path、stable errorCode、failureMode、retryClass、`Can Develop / Can Release` 收口成单一矩阵。
- 只认以下真实取证文件：
  - `yudao-mall-uniapp/pages/booking/technician-list.vue`
  - `yudao-mall-uniapp/pages/booking/technician-detail.vue`
  - `yudao-mall-uniapp/pages/booking/order-confirm.vue`
  - `yudao-mall-uniapp/pages/booking/order-list.vue`
  - `yudao-mall-uniapp/pages/booking/order-detail.vue`
  - `yudao-mall-uniapp/pages/booking/addon.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/sheep/request/index.js`
  - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - `AppTechnicianController`
  - `AppTimeSlotController`
  - `AppBookingOrderController`
  - `AppBookingAddonController`
  - `TechnicianServiceImpl`
  - `TimeSlotServiceImpl`
  - `BookingOrderServiceImpl`
  - `BookingAddonServiceImpl`
- 本文强约束：
  - 不因为 controller 或常量存在，就把能力写成页面闭环或稳定 runtime code。
  - 不按 `message` 分支，只按 `code` 分支。
  - `[] / null / 0` 只能写成结构化空态或空结果，不能写成成功样本。
  - `code=0` 但页面写后未读到预期变化，必须明确标成 `pseudo success / no-op risk`。
  - 当前没有任何已提交证据证明 booking runtime 存在服务端 `degraded=true / degradeReason`。

## 2. Canonical Matrix

| scope | 页面 / helper 调用点 | frontend canonical method/path | backend controller method/path | backend service / response 真值 | stable errorCode | failureMode | retryClass | Can Develop | Can Release | 当前结论 |
|---|---|---|---|---|---|---|---|---|---|---|
| technician-list query | `/pages/booking/technician-list` -> `loadTechnicianList` | `GET /booking/technician/list` | `AppTechnicianController#getTechnicianList` -> `GET /booking/technician/list` | `TechnicianServiceImpl#getEnabledTechnicianListByStoreId`；响应只稳定提供 `id,name,avatar,introduction,tags,rating,serviceCount` | `-` | `FAIL_OPEN` | `NO_AUTO_RETRY` | Yes | No | route 对齐；但页面在用 `title/specialties/status` 无 backend 绑定，只能落本地 fallback |
| technician-detail query | `/pages/booking/technician-detail` -> `loadTechnicianDetail` | `GET /booking/technician/get` | `AppTechnicianController#getTechnician` -> `GET /booking/technician/get` | `TechnicianServiceImpl#getTechnician`；查无记录返回 `success(null)` | `-` | `FAIL_OPEN` | `NO_AUTO_RETRY` | Yes | No | 当前不得把 `1030001000/1030001001` 写成该页稳定分支；页面 `title` 仍是本地 fallback |
| technician-detail slot query | `/pages/booking/technician-detail` -> `loadTimeSlots` | `GET /booking/slot/list-by-technician` | `AppTimeSlotController#getTimeSlotsByTechnician` -> `GET /booking/slot/list-by-technician` | `TimeSlotServiceImpl#getTimeSlotsByTechnicianAndDate`；响应稳定字段为 `id,technicianId,technicianName,technicianAvatar,slotDate,startTime,endTime,isOffpeak,offpeakPrice,status` | `-` | `FAIL_OPEN` | `NO_AUTO_RETRY` | Yes | No | technician-detail 页本身只使用字段子集；`order-confirm` 依赖的 `duration/spuId/skuId` 不在响应 VO 中 |
| order-list query | `/pages/booking/order-list` -> `BookingApi.getOrderList` | `GET /booking/order/list` | `AppBookingOrderController#getOrderList` -> `GET /booking/order/list` | `BookingOrderServiceImpl#getOrderListByUserId`；controller 返回 `data[]`，不是分页对象；`AppBookingOrderRespVO` 也不含 `payOrderId` | `-` | `FAIL_OPEN` | `REFRESH_ONCE` | Yes | No | 页面按 `data.list/data.total` 读取；即使 `code=0` 也可能只得到空渲染或丢失支付 CTA，不能当成功样本 |
| order-detail query | `/pages/booking/order-detail` -> `BookingApi.getOrderDetail` | `GET /booking/order/get` | `AppBookingOrderController#getOrder` -> `GET /booking/order/get` | `BookingOrderServiceImpl#getOrderByUser`；越权抛错，查无记录返回 `success(null)`；`AppBookingOrderRespVO` 不含 `payOrderId` | `BOOKING_ORDER_NOT_OWNER(1030004006)` | `FAIL_CLOSE` for owner check; miss=`success(null)` | `NO_AUTO_RETRY` | Yes | No | 页面能渲染 not-found 态，但当前 pay CTA 没有已提交字段绑定证据 |
| create | `/pages/booking/order-confirm` -> `submitBookingOrderAndGo` | `POST /booking/order/create` | `AppBookingOrderController#createOrder` -> `POST /booking/order/create` | `BookingOrderServiceImpl#createOrder`；稳定抛错只核到 `1030003001` | `TIME_SLOT_NOT_AVAILABLE(1030003001)` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | Yes | No | 页面预读调用 `loadTimeSlots(..., null)`，controller 却要求 `date`；提交体依赖 `slot.spuId/skuId`，但 `AppTimeSlotRespVO` 不提供这两个字段，`AppBookingOrderCreateReqVO` 又要求 `spuId` |
| cancel | `/pages/booking/order-list` / `/pages/booking/order-detail` -> `cancelBookingOrderAndRefresh` | `POST /booking/order/cancel` | `AppBookingOrderController#cancelOrder` -> `POST /booking/order/cancel` | `BookingOrderServiceImpl#cancelOrder`；成功只返回 `true` | `BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_CANNOT_CANCEL(1030004005)`、`BOOKING_ORDER_NOT_OWNER(1030004006)` | `FAIL_CLOSE` | `REFRESH_ONCE` | Yes | No | helper 只在 `code===0` 时刷新一次；当前没有写后回读样本，不能把 `code=0` 但状态未变写成成功 |
| addon | `/pages/booking/addon` -> `submitAddonOrderAndGo` | `POST /app-api/booking/addon/create` | `AppBookingAddonController#createAddonOrder` -> `POST /app-api/booking/addon/create` | `BookingAddonServiceImpl#createExtendOrder/createUpgradeOrder/createAddItemOrder` | `TIME_SLOT_NOT_AVAILABLE(1030003001)`、`BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_STATUS_ERROR(1030004001)`、`BOOKING_ORDER_NOT_OWNER(1030004006)` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | Yes | No | 页面只提交 `parentOrderId,addonType`；`upgrade` 路径缺 `skuId` 会落 `1030004000`，`add-item` 路径缺 `spuId/skuId` 时可能 `code=0` 插入零价格空商品订单，属于 `pseudo success / no-op risk` |

## 3. Backend-Only Truth，不能写成页面闭环

| backend-only method/path | 当前 controller | 为什么不能写成页面闭环 |
|---|---|---|
| `GET /booking/slot/list` | `AppTimeSlotController#getAvailableTimeSlots` | 当前六个真实 booking 页面没有 helper 或 route 直接绑定它 |
| `GET /booking/slot/get` | `AppTimeSlotController#getTimeSlot` | 只核到 controller；当前页面没有直接查询这个接口 |
| `GET /booking/order/get-by-order-no` | `AppBookingOrderController#getOrderByOrderNo` | 当前页面只按 `id` 查详情，没有真实 route 绑定 `orderNo` |
| `GET /booking/order/list-by-status` | `AppBookingOrderController#getOrderListByStatus` | 当前 order-list 页仍调用 `/booking/order/list`，只是把 `status` 塞进 query，不是这个 controller path |

## 4. 当前 mismatch

| mismatch | frontend 证据 | backend 证据 | 当前影响 |
|---|---|---|---|
| technician list/detail 字段漂移 | `technician-list.vue` / `technician-detail.vue` 使用 `title/specialties/status` | `AppTechnicianRespVO` 只提供 `id,name,avatar,introduction,tags,rating,serviceCount` | 页面只能靠默认值或空值，不等于字段闭环 |
| order-list 返回结构漂移 | `order-list.vue` 读取 `data.list/data.total` | `AppBookingOrderController#getOrderList` 返回 `CommonResult<List<AppBookingOrderRespVO>>` | `code=0` 也可能只得到空态，不得写成查询成功样本 |
| order list/detail 支付字段漂移 | `order-list.vue` / `order-detail.vue` 使用 `payOrderId` | `AppBookingOrderRespVO` 不含 `payOrderId` | 去支付 CTA 当前没有稳定字段绑定证据 |
| order-confirm 预读与提交字段漂移 | `order-confirm.vue` 用 `loadTimeSlots(..., null)`，并从 `slot.spuId/skuId/duration` 取值 | `AppTimeSlotController#getTimeSlotsByTechnician` 要求 `date`；`AppTimeSlotRespVO` 不含 `duration/spuId/skuId`；`AppBookingOrderCreateReqVO` 要求 `spuId` | create 链只有 helper smoke，没有页面到请求体的真实运行闭环 |
| addon 请求体漂移 | `addon.vue` 只提交 `parentOrderId,addonType`；`remark` 仅本地字段 | `AppBookingAddonCreateReqVO` 支持 `spuId/skuId`；`BookingAddonServiceImpl` 的 `upgrade`/`add-item` 路径会消费这些字段 | `upgrade` 可能被误打成 `1030004000`；`add-item` 可能 `code=0` 但写入零价格空商品订单，属于 `pseudo success / no-op risk` |

## 5. Legacy blocker 继续保留
- 以下旧 path / old method 当前已不再由 `booking.js` 发出，但仍必须作为 release blocker 保留项：
  - `GET /booking/technician/list-by-store`
  - `GET /booking/time-slot/list`
  - `PUT /booking/order/cancel`
  - `POST /booking/addon/create`
- 这些 legacy blocker 当前只能写成：
  - 历史阻断项
  - freeze / release / truth ledger 的保留锚点
- 这些 legacy blocker 不能再写成：
  - 当前 frontend canonical path
  - 当前页面仍在调用的接口

## 6. Stable ErrorCode 与禁止提前登记项

### 6.1 当前可稳定写入 booking runtime page contract 的 code

| code | name | 当前稳定 page scope | backend 证据 | 备注 |
|---:|---|---|---|---|
| `1030003001` | `TIME_SLOT_NOT_AVAILABLE` | create、addon(extend) | `BookingOrderServiceImpl#createOrder`，`BookingAddonServiceImpl#createExtendOrder` | 只证明时段不可用 / 锁失败，当前不得外推成 `1030002001/1030003002` |
| `1030004000` | `BOOKING_ORDER_NOT_EXISTS` | cancel、addon | `BookingOrderServiceImpl#validateOrderExists`；`BookingAddonServiceImpl#validateParentOrder`；`createUpgradeOrder` 里 `newSkuId` 未取到也会落此码 | add-on 页当前不能把它简化成“母单不存在”单义触发 |
| `1030004001` | `BOOKING_ORDER_STATUS_ERROR` | addon only | `BookingAddonServiceImpl#validateParentOrder` | 只限母单不在 `IN_SERVICE` |
| `1030004005` | `BOOKING_ORDER_CANNOT_CANCEL` | cancel | `BookingOrderServiceImpl#cancelOrder` | 只限状态非 `PENDING_PAYMENT/PAID` |
| `1030004006` | `BOOKING_ORDER_NOT_OWNER` | order-detail、cancel、addon | `BookingOrderServiceImpl#getOrderByUser/cancelOrder`，`BookingAddonServiceImpl#validateParentOrder` | 当前 page contract 只能按 code 分支，不得按错误文案分支 |

### 6.2 当前不能登记成 booking runtime stable code 的项
- `TECHNICIAN_NOT_EXISTS(1030001000)`
  - `GET /booking/technician/get` 当前查无记录返回 `success(null)`，不是稳定报错。
- `TECHNICIAN_DISABLED(1030001001)`
  - 当前 app user 页面链路没有稳定对外抛出证据。
- `SCHEDULE_CONFLICT(1030002001)`
  - 当前 booking runtime create 链路没有已提交 page/helper/controller/service 联动证据。
- `TIME_SLOT_ALREADY_BOOKED(1030003002)`
  - 当前 booking runtime create/add-on 链路没有稳定对外抛出证据。
- `BOOKING_ORDER_NOT_EXISTS(1030004000)` 用于 `GET /booking/order/get`
  - 当前 miss 真实口径是 `success(null)`。
- `BOOKING_ORDER_STATUS_ERROR(1030004001)` 用于 `POST /booking/order/cancel`
  - 当前 cancel 稳定报错是 `1030004005`。

### 6.3 当前没有已提交证据的 retryClass
- 当前 booking runtime page contract 没有 `MANUAL_RETRY_3` 的已提交 helper / SOP / runbook 证据。
- 当前 booking runtime page contract 只核出：
  - `NO_AUTO_RETRY`
  - `REFRESH_ONCE`

## 7. 静态对齐、运行证据、发布证据
- 静态对齐证据：
  - `booking.js` 已把 canonical method/path 收口到当前 controller 映射。
  - `booking-api-alignment.test.mjs`、`booking-page-smoke.test.mjs` 只冻结 wrapper/helper 调用面与“成功才跳转/刷新”的控制流。
- 运行证据：
  - 当前没有已提交页面级 read-after-write 样本，无法证明 `order-confirm`、`order-list`、`order-detail`、`addon` 的字段绑定已闭环。
  - 当前也没有已提交运行样本证明 `code=0` 后 UI 已读到预期状态变化。
- 发布证据：
  - 当前没有 create / cancel / addon 的发布级 success/failure 样本包、allowlist、巡检日志、回放证据。
  - 因此当前只能写 `Can Develop=Yes / Can Release=No`。

## 8. 当前没有证据的 `degraded=true / degradeReason`
- `booking.js`
- `logic.js`
- 当前六个 booking 页面
- booking app controller/service
- 当前两份 node smoke test
- 当前 03-15 booking runtime 文档

以上范围都没有服务端 `degraded=true / degradeReason` 的已提交证据。

## 9. 当前 No-Go 条件
1. 把 controller-only path 写成页面闭环。
2. 把 `TECHNICIAN_NOT_EXISTS / TECHNICIAN_DISABLED / SCHEDULE_CONFLICT / TIME_SLOT_ALREADY_BOOKED` 写成当前 booking runtime page 的稳定运行分支。
3. 把 `[] / null / 0` 写成成功样本。
4. 把 `code=0` 但页面没有读回预期变化，或者 add-on 写入零价格空商品订单，写成真实成功。
5. 把静态 helper smoke、runtime gate `PASS` 或 shared chain `rc=0` 外推成 release-ready。
