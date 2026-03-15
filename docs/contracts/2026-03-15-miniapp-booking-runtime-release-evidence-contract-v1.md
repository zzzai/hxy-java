# MiniApp Booking Runtime Release Evidence Contract v1 (2026-03-15)

## 1. 目标与证据范围
- 目标：完成 booking runtime canonical contract 与 blocker evidence 的单一真值收口。
- 本批只认以下 canonical truth：
  - `GET /booking/technician/list`
  - `GET /booking/slot/list-by-technician`
  - `POST /booking/order/create`
  - `GET /booking/order/get`
  - `GET /booking/order/list`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
- 相邻但不纳入本批 release gate 的 runtime helper：
  - `GET /booking/technician/get`
- 取证文件：
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppTechnicianController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppTimeSlotController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingOrderController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingAddonController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingOrderServiceImpl.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingAddonServiceImpl.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/TechnicianServiceImpl.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/TimeSlotServiceImpl.java`
  - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`

## 2. 当前前端 `booking.js` 的真实 method/path

| API wrapper | 当前真实 method/path | 关键参数 | 说明 |
|---|---|---|---|
| `BookingApi.getTechnicianList` | `GET /booking/technician/list` | query:`storeId` | 属于本批 canonical truth |
| `BookingApi.getTechnician` | `GET /booking/technician/get` | query:`id` | 当前 helper 真实使用，但不属于本批 release gate canonical truth |
| `BookingApi.getTimeSlots` | `GET /booking/slot/list-by-technician` | query:`technicianId`,`date` | 属于本批 canonical truth |
| `BookingApi.createOrder` | `POST /booking/order/create` | body | 属于本批 canonical truth |
| `BookingApi.getOrderDetail` | `GET /booking/order/get` | query:`id` | 属于本批 canonical truth |
| `BookingApi.getOrderList` | `GET /booking/order/list` | query:`params` 原样透传 | 属于本批 canonical truth；controller 当前不消费分页/筛选参数 |
| `BookingApi.cancelOrder` | `POST /booking/order/cancel` | query:`id`,`reason` | 属于本批 canonical truth；取消原参名在 wrapper 层已对齐为 `reason` |
| `BookingApi.createAddonOrder` | `POST /app-api/booking/addon/create` | body | 属于本批 canonical truth |

证据：
- `booking-api-alignment.test.mjs` 已直接断言：
  - 技师列表与时段列表 path/method 为 canonical truth
  - 取消预约为 `POST + query`
  - add-on 为 `/app-api/booking/addon/create`

## 3. 当前页面 helper 对 API wrapper 的真实引用关系

| 页面 helper | 真实 API wrapper 引用 | helper 层附加行为 |
|---|---|---|
| `loadTechnicianList` | `api.getTechnicianList(storeId)` | 无附加逻辑 |
| `loadTechnicianDetail` | `api.getTechnician(technicianId)` | 无附加逻辑 |
| `loadTimeSlots` | `api.getTimeSlots(technicianId, date)` | 无附加逻辑 |
| `submitBookingOrder` | `api.createOrder(payload)` | 固定补 `dispatchMode: 1` |
| `submitBookingOrderAndGo` | `submitBookingOrder` | `code === 0` 时跳转 `/pages/booking/order-detail?id={orderId}` |
| `cancelBookingOrder` | `api.cancelOrder(id, reason)` | 默认 reason=`用户主动取消` |
| `cancelBookingOrderAndRefresh` | `cancelBookingOrder` | `code === 0` 时执行一次 `onSuccess()`，失败不刷新 |
| `submitAddonOrder` | `api.createAddonOrder(payload)` | 无附加逻辑 |
| `submitAddonOrderAndGo` | `submitAddonOrder` | `code === 0` 时跳转 `/pages/booking/order-detail?id={orderId}` |

证据：
- `booking-page-smoke.test.mjs` 已断言 helper surface、helper -> API 调用关系、成功跳转、失败不跳转、取消成功只刷新一次。

## 4. 当前 app controller 的真实 method/path

| controller | 当前真实 method/path | controller 方法 |
|---|---|---|
| `AppTechnicianController` | `GET /booking/technician/list` | `getTechnicianList` |
| `AppTechnicianController` | `GET /booking/technician/get` | `getTechnician` |
| `AppTimeSlotController` | `GET /booking/slot/list` | `getAvailableTimeSlots` |
| `AppTimeSlotController` | `GET /booking/slot/list-by-technician` | `getTimeSlotsByTechnician` |
| `AppBookingOrderController` | `POST /booking/order/create` | `createOrder` |
| `AppBookingOrderController` | `GET /booking/order/get` | `getOrder` |
| `AppBookingOrderController` | `GET /booking/order/list` | `getOrderList` |
| `AppBookingOrderController` | `POST /booking/order/cancel` | `cancelOrder` |
| `AppBookingAddonController` | `POST /app-api/booking/addon/create` | `createAddonOrder` |

说明：
- 当前 app controller 与 `booking.js` 已经在 canonical truth 上对齐。
- `GET /booking/slot/list` 当前后端存在，但前端 helper 未绑定，不属于本批 release gate canonical truth。

## 5. Canonical truth 对照结论

| canonical truth | 当前 FE wrapper / helper | 当前 controller | runtime binding | 当前 release 结论 |
|---|---|---|---|---|
| `GET /booking/technician/list` | `BookingApi.getTechnicianList` / `loadTechnicianList` | `AppTechnicianController#getTechnicianList` | `Aligned` | `Cannot Release` |
| `GET /booking/slot/list-by-technician` | `BookingApi.getTimeSlots` / `loadTimeSlots` | `AppTimeSlotController#getTimeSlotsByTechnician` | `Aligned` | `Cannot Release` |
| `POST /booking/order/create` | `BookingApi.createOrder` / `submitBookingOrder*` | `AppBookingOrderController#createOrder` | `Aligned` | `Cannot Release` |
| `GET /booking/order/get` | `BookingApi.getOrderDetail` / `goToOrderDetail` | `AppBookingOrderController#getOrder` | `Aligned` | 查询侧 runtime 可用 |
| `GET /booking/order/list` | `BookingApi.getOrderList` | `AppBookingOrderController#getOrderList` | `Aligned` | 查询侧 runtime 可用 |
| `POST /booking/order/cancel` | `BookingApi.cancelOrder` / `cancelBookingOrder*` | `AppBookingOrderController#cancelOrder` | `Aligned` | `Cannot Release` |
| `POST /app-api/booking/addon/create` | `BookingApi.createAddonOrder` / `submitAddonOrder*` | `AppBookingAddonController#createAddonOrder` | `Aligned` | `Cannot Release` |

## 6. 旧 path / 旧 method 作为 blocker 的保留项

| blocker 项 | 当前代码状态 | blocker 证据仍在的真实文件 | 保留原因 |
|---|---|---|---|
| `GET /booking/technician/list-by-store` | 当前 `booking.js` 不再发出 | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`、`docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`、`docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md`、`docs/contracts/2026-03-14-miniapp-runtime-blocker-contract-closure-v1.md` | 当前 release/freeze 口径仍用它做 blocker，不能静默撤销 |
| `GET /booking/time-slot/list` | 当前 `booking.js` 不再发出 | 同上 | 当前 BF-022 / freeze review 仍引用它 |
| `PUT /booking/order/cancel` | 当前 `booking.js` 不再发出 | 同上 | 当前 BF-023 / freeze review 仍引用它 |
| `POST /booking/addon/create` | 当前 `booking.js` 不再发出 | 同上 | 当前 BF-024 / freeze review 仍引用它 |

固定口径：
- 这些 legacy 项当前不再是 runtime wrapper 真值。
- 这些 legacy 项当前仍是 release blocker 保留项。
- A/B/D 未同步 gate 前，窗口 C 不能擅自把它们从 blocker 列表中删掉。

## 7. Stable errorCode 证据

### 7.1 当前可写入 canonical 的稳定 code

| code | name | 当前对外路径 | 真实抛出证据 |
|---:|---|---|---|
| `1030003001` | `TIME_SLOT_NOT_AVAILABLE` | `POST /booking/order/create`、`POST /app-api/booking/addon/create` | `BookingOrderServiceImpl#createOrder` 在自动派单未找到可用时段、锁时段失败时抛出；`BookingAddonServiceImpl#createExtendOrder` 在无下一时段/锁失败时抛出 |
| `1030004000` | `BOOKING_ORDER_NOT_EXISTS` | `POST /booking/order/cancel`、`POST /app-api/booking/addon/create` | `BookingOrderServiceImpl#validateOrderExists`、`BookingAddonServiceImpl#validateParentOrder` 直接抛出 |
| `1030004001` | `BOOKING_ORDER_STATUS_ERROR` | `POST /app-api/booking/addon/create` | `BookingAddonServiceImpl#validateParentOrder` 在原订单不处于 `IN_SERVICE` 时抛出 |
| `1030004005` | `BOOKING_ORDER_CANNOT_CANCEL` | `POST /booking/order/cancel` | `BookingOrderServiceImpl#cancelOrder` 在订单状态非 `PENDING_PAYMENT/PAID` 时抛出 |
| `1030004006` | `BOOKING_ORDER_NOT_OWNER` | `GET /booking/order/get`、`POST /booking/order/cancel`、`POST /app-api/booking/addon/create` | `BookingOrderServiceImpl#getOrderByUser`、`cancelOrder`、`BookingAddonServiceImpl#validateParentOrder` 均直接抛出 |

### 7.2 当前不能写成稳定 code 的项
- `TECHNICIAN_NOT_EXISTS(1030001000)`
  - 当前 `AppTechnicianController#getTechnician -> TechnicianServiceImpl#getTechnician` 只是 `selectById`，找不到时返回 `null`，没有稳定抛出。
- `SCHEDULE_CONFLICT(1030002001)`
  - 当前 booking app user 路径只在常量定义中存在；未在 `AppBookingOrderController -> BookingOrderServiceImpl` 当前链路核到稳定抛出。
- `TIME_SLOT_ALREADY_BOOKED(1030003002)`
  - 当前 booking app user 路径只在常量定义中存在；未在 create/add-on 当前链路核到稳定抛出。
- `BOOKING_ORDER_NOT_EXISTS(1030004000)` 对 `GET /booking/order/get`
  - 当前 `BookingOrderServiceImpl#getOrderByUser` 查无订单返回 `null`，不是稳定报错。
- `BOOKING_ORDER_STATUS_ERROR(1030004001)` 对 `POST /booking/order/cancel`
  - 当前取消路径实际抛出的是 `BOOKING_ORDER_CANNOT_CANCEL(1030004005)`，不是 `BOOKING_ORDER_STATUS_ERROR`。

## 8. Fail-open / fail-close / no-auto-retry / refresh-once 的真实口径
- `FAIL_OPEN`
  - `GET /booking/technician/list`、`GET /booking/slot/list-by-technician`、`GET /booking/order/list` 当前都允许合法空态；空列表 `[]` 不是错误码。
  - `GET /booking/order/get` 与 `GET /booking/technician/get` 当前查无记录时都可能返回 `success(null)`；这不是 `degraded`，也不是稳定 errorCode。
- `FAIL_CLOSE`
  - `POST /booking/order/create`、`POST /booking/order/cancel`、`POST /app-api/booking/addon/create` 当前都依赖 service exception 直接阻断，helper 侧只有 `code === 0` 才会继续跳转或刷新。
- `NO_AUTO_RETRY`
  - `submitBookingOrderAndGo` 失败时不跳转、无重试循环。
  - `submitAddonOrderAndGo` 失败时不跳转、无重试循环。
  - `cancelBookingOrderAndRefresh` 失败时不刷新、无重试循环。
- `REFRESH_ONCE`
  - 当前只有 `cancelBookingOrderAndRefresh` 暴露出明确的“一次刷新”语义：`code === 0` 时执行一次 `onSuccess()`。
- `degraded=true / degradeReason`
  - 当前 booking runtime controller、service、helper、tests 都没有服务端 `degraded=true / degradeReason` 证据，本批不得写入任何相关服务端口径。

## 9. 当前为什么仍是 `Cannot Release`
1. `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md` 仍把 `booking.create-chain`、`booking.cancel`、`booking.addon-upgrade` 标为 `Cannot Release=Yes`，并把 legacy path/method 作为 release 前置退出条件。
2. `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md` 仍将 BF-022/BF-023/BF-024 维持在 `PLANNED_RESERVED`，阻断理由仍是旧 path / old method。
3. `docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md` 仍把 booking 域判定为 `Still Blocked`，并要求旧 path / old method 从联调和 release allowlist 中清除后再触发下一轮 freeze。
4. 当前自动化证据只覆盖 wrapper/helper 对齐与 smoke 行为，不是 create/cancel/add-on 的真实 release sample 包。
5. 因此本批结论只能是：
  - runtime canonical contract 已对齐；
  - legacy blocker 继续保留；
  - 当前 booking 写链路仍为 `Cannot Release`。
