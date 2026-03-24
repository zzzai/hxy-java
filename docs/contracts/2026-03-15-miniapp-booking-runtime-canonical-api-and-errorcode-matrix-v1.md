# MiniApp Booking Runtime Canonical API and ErrorCode Matrix v1 (2026-03-15)

## 1. 目标与真值来源
- 目标：把 booking runtime 当前真实页面、真实 frontend API、真实 helper、真实 app controller/service 的 canonical method/path、stable errorCode、failureMode、retryClass 固定成单一矩阵。
- 只认以下真实输入：
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - `ruoyi-vue-pro-master/**/controller/app/AppTechnicianController.java`
  - `ruoyi-vue-pro-master/**/controller/app/AppTimeSlotController.java`
  - `ruoyi-vue-pro-master/**/controller/app/AppBookingOrderController.java`
  - `ruoyi-vue-pro-master/**/controller/app/AppBookingAddonController.java`
  - `ruoyi-vue-pro-master/**/service/impl/TechnicianServiceImpl.java`
  - `ruoyi-vue-pro-master/**/service/impl/TimeSlotServiceImpl.java`
  - `ruoyi-vue-pro-master/**/service/impl/BookingOrderServiceImpl.java`
  - `ruoyi-vue-pro-master/**/service/impl/BookingAddonServiceImpl.java`
- 本文约束：
  - 不按 message 分支。
  - 不因常量存在，把错误码直接登记成当前 runtime stable code。
  - 不因 controller 存在，把能力直接写成页面闭环。
  - 当前 booking runtime 只承认 `query-only active` 与 `write-chain no-go` 的并存边界。

## 2. 真实页面绑定矩阵

| 真实页面 / 动作 | frontend 调用点 | helper / 页面调用 | backend controller | canonical method/path | 当前 stable errorCode | failureMode | retryClass | Can Develop | Can Release | 当前说明 |
|---|---|---|---|---|---|---|---|---|---|---|
| `/pages/booking/technician-list` 技师列表查询 | `BookingApi.getTechnicianList(storeId)` | `loadTechnicianList` | `AppTechnicianController#getTechnicianList` | `GET /booking/technician/list` | `-` | `FAIL_OPEN` | `NO_AUTO_RETRY` | Yes | Yes | 空列表 `[]` 合法；当前只按 query-only `ACTIVE` 管理 |
| `/pages/booking/technician-detail` 技师详情查询 | `BookingApi.getTechnician(id)` | `loadTechnicianDetail` | `AppTechnicianController#getTechnician` | `GET /booking/technician/get` | `-` | `FAIL_OPEN` | `NO_AUTO_RETRY` | Yes | Yes | 当前 miss 返回 `success(null)`；不得按 `1030001000/1030001001` 写当前 runtime 分支 |
| `/pages/booking/technician-detail` 技师时段查询 | `BookingApi.getTimeSlots(technicianId, date)` | `loadTimeSlots` | `AppTimeSlotController#getTimeSlotsByTechnician` | `GET /booking/slot/list-by-technician` | `-` | `FAIL_OPEN` | `NO_AUTO_RETRY` | Yes | Yes | 空列表 `[]` 合法；当前 query path 无稳定 `1030003001/1030003002` 抛出证据 |
| `/pages/booking/order-confirm` 已选时段详情查询 | `BookingApi.getTimeSlot(id)` | `loadTimeSlotDetail` | `AppTimeSlotController#getTimeSlot` | `GET /booking/slot/get` | `-` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | Yes | No | 当前页面用单点时段详情取代 `loadTimeSlots(technicianId, null)` 回捞，`duration` 已形成真实读取闭环 |
| `/pages/booking/order-list` 预约列表查询 | `BookingApi.getOrderList(params)` | 页面内直接调用 | `AppBookingOrderController#getOrderList` | `GET /booking/order/list` | `-` | `FAIL_OPEN` | `REFRESH_ONCE` | Yes | Yes | FE 透传 `pageNo/pageSize/status`，controller 当前已真实消费并返回 `PageResult<{list,total}>` |
| `/pages/booking/order-detail` 预约详情查询 | `BookingApi.getOrderDetail(id)` | 页面内直接调用 | `AppBookingOrderController#getOrder` | `GET /booking/order/get` | `BOOKING_ORDER_NOT_OWNER(1030004006)` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | Yes | Yes | 越权稳定抛 `1030004006`；订单不存在当前返回 `success(null)`，不是 `1030004000` |
| `/pages/booking/order-confirm` 创建预约 | `BookingApi.createOrder(data)` | `submitBookingOrder` / `submitBookingOrderAndGo` | `AppBookingOrderController#createOrder` | `POST /booking/order/create` | `TIME_SLOT_NOT_AVAILABLE(1030003001)` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | Yes | No | helper 只在 `code === 0` 时跳详情；当前不得按 `1030002001/1030003002` 写稳定 runtime 分支 |
| `/pages/booking/order-list`、`/pages/booking/order-detail` 取消预约 | `BookingApi.cancelOrder(id, cancelReason)` | `cancelBookingOrder` / `cancelBookingOrderAndRefresh` | `AppBookingOrderController#cancelOrder` | `POST /booking/order/cancel` | `BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_CANNOT_CANCEL(1030004005)`、`BOOKING_ORDER_NOT_OWNER(1030004006)` | `FAIL_CLOSE` | `REFRESH_ONCE` | Yes | No | 失败不刷新；成功后只执行一次 `onSuccess()` |
| `/pages/booking/addon` 创建加钟 / 升级 / 加项目订单 | `BookingApi.createAddonOrder(data)` | `submitAddonOrder` / `submitAddonOrderAndGo` | `AppBookingAddonController#createAddonOrder` | `POST /app-api/booking/addon/create` | `TIME_SLOT_NOT_AVAILABLE(1030003001)`、`BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_STATUS_ERROR(1030004001)`、`BOOKING_ORDER_NOT_OWNER(1030004006)` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | Yes | No | 失败不跳详情；`remark` 当前不是请求字段 |

## 3. 只存在 controller，不等于页面闭环

| backend controller method/path | 当前 frontend 绑定 | 当前状态 | 为什么不能写成页面闭环 |
|---|---|---|---|
| `GET /booking/slot/list` | 当前无真实 frontend 绑定 | `Can Develop=Yes / Can Release=No` | 后端存在，不等于有真实门店时段页 |
| `GET /booking/order/list-by-status` | 当前无真实 frontend 绑定；`order-list` tabs 仍走 `/booking/order/list` | `Can Develop=Yes / Can Release=No` | controller 存在，但当前没有页面真正命中这条 path |
| `GET /booking/order/get-by-order-no` | 当前无真实 frontend 绑定 | `Can Develop=Yes / Can Release=No` | controller 存在，但当前没有真实页面闭环 |

## 4. mismatch 与 legacy blocker

| 项目 | 当前真实状态 | 必须保留的口径 |
|---|---|---|
| `GET /booking/technician/list-by-store` | 当前 `booking.js` 已不再发出 | 继续作为 legacy blocker 保留项，不能静默删除 |
| `GET /booking/time-slot/list` | 当前 `booking.js` 已不再发出 | 继续作为 legacy blocker 保留项，不能静默删除 |
| `PUT /booking/order/cancel` | 当前 `booking.js` 已不再发出 | 继续作为 legacy blocker 保留项，不能静默删除 |
| `POST /booking/addon/create` | 当前 `booking.js` 已不再发出 | 继续作为 legacy blocker 保留项，不能静默删除 |
| `GET /booking/order/list` 的 query 参数 | FE 继续透传 `pageNo/pageSize/status` | 03-24 起 controller 已真实消费，可按当前页面稳定分页/筛选协议管理 |
| `GET /booking/technician/get` miss | 当前返回 `success(null)` | 不能按 `TECHNICIAN_NOT_EXISTS(1030001000)` 或 `TECHNICIAN_DISABLED(1030001001)` 写稳定 runtime 分支 |
| `GET /booking/order/get` miss | 当前返回 `success(null)` | 不能按 `BOOKING_ORDER_NOT_EXISTS(1030004000)` 写稳定 runtime 分支 |

## 5. 当前不能登记成 booking runtime stable code 的项

| code | 当前不能登记的原因 |
|---|---|
| `TECHNICIAN_NOT_EXISTS(1030001000)` | 当前 `GET /booking/technician/get` 由 `TechnicianServiceImpl#getTechnician` 直接 `selectById`，miss 返回 `null`，没有稳定抛出 |
| `TECHNICIAN_DISABLED(1030001001)` | 当前 app user query path 没有稳定启停校验抛出 |
| `SCHEDULE_CONFLICT(1030002001)` | 当前 app create / add-on runtime 链路未核到稳定抛出 |
| `TIME_SLOT_ALREADY_BOOKED(1030003002)` | 当前 app create / add-on runtime 链路未核到稳定抛出 |
| `BOOKING_ORDER_NOT_EXISTS(1030004000)` 用于 `GET /booking/order/get` miss | 当前 `getOrderByUser` miss 返回 `null`，不是稳定报错 |
| `BOOKING_ORDER_STATUS_ERROR(1030004001)` 用于 `POST /booking/order/cancel` | 当前取消路径稳定抛出的是 `BOOKING_ORDER_CANNOT_CANCEL(1030004005)` |

## 6. 当前没有证据的 `degraded=true / degradeReason`
- 当前 booking runtime controller、service、helper、tests 都没有服务端 `degraded=true / degradeReason` 证据。
- 当前 booking 查询页的空列表、`success(null)`、结构化空态都属于正常结构态，不属于 `degraded`。
- 因此当前 C 窗口 contract 不得补写：
  - `degraded=true`
  - `degradeReason`
  - 任何 booking 服务端 warning 字段

## 7. 当前 No-Go 条件
1. 把 `POST /booking/order/create`、`POST /booking/order/cancel`、`POST /app-api/booking/addon/create` 任一项改写为 `Can Release=Yes`。
2. 把 controller-only path 写成页面闭环：
   - `GET /booking/slot/list`
   - `GET /booking/order/list-by-status`
   - `GET /booking/order/get-by-order-no`
3. 在 booking runtime 当前页面分支里按 message 做 UI / SOP / runbook 决策。
4. 把 `1030001000/1030001001/1030002001/1030003002` 写成当前 booking runtime 真实页面的稳定分支。
5. 在 contract、PRD、runbook 中补造 `degraded=true / degradeReason`。
