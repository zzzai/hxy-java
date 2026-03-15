# MiniApp Booking User API Alignment v1 (2026-03-10)

## 1. 目标与真值来源
- 目标：把 booking 用户侧当前真实 runtime 契约收口为单一真值，并把 legacy blocker 与 release gate 继续固定在文档层。
- 本批只认以下 canonical truth：
  - `GET /booking/technician/list`
  - `GET /booking/slot/list-by-technician`
  - `POST /booking/order/create`
  - `GET /booking/order/get`
  - `GET /booking/order/list`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
- 真值输入：
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `AppTechnicianController`
  - `AppTimeSlotController`
  - `AppBookingOrderController`
  - `AppBookingAddonController`
  - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-release-evidence-contract-v1.md`

## 2. 当前 runtime 对齐结论

| canonical truth | 当前 FE wrapper / helper | 当前 controller | runtime binding | release gate |
|---|---|---|---|---|
| `GET /booking/technician/list` | `BookingApi.getTechnicianList` / `loadTechnicianList` | `AppTechnicianController#getTechnicianList` | `Aligned` | `Cannot Release` |
| `GET /booking/slot/list-by-technician` | `BookingApi.getTimeSlots` / `loadTimeSlots` | `AppTimeSlotController#getTimeSlotsByTechnician` | `Aligned` | `Cannot Release` |
| `POST /booking/order/create` | `BookingApi.createOrder` / `submitBookingOrder*` | `AppBookingOrderController#createOrder` | `Aligned` | `Cannot Release` |
| `GET /booking/order/get` | `BookingApi.getOrderDetail` / `goToOrderDetail` | `AppBookingOrderController#getOrder` | `Aligned` | `Query Runtime OK` |
| `GET /booking/order/list` | `BookingApi.getOrderList` | `AppBookingOrderController#getOrderList` | `Aligned` | `Query Runtime OK` |
| `POST /booking/order/cancel` | `BookingApi.cancelOrder` / `cancelBookingOrder*` | `AppBookingOrderController#cancelOrder` | `Aligned` | `Cannot Release` |
| `POST /app-api/booking/addon/create` | `BookingApi.createAddonOrder` / `submitAddonOrder*` | `AppBookingAddonController#createAddonOrder` | `Aligned` | `Cannot Release` |

说明：
- `loadTechnicianDetail -> BookingApi.getTechnician -> GET /booking/technician/get` 仍是当前 helper 的真实引用关系，但它不是本批 release gate 的 canonical truth 条目。
- 当前 legacy path / old method 已不再由 `booking.js` 发出；它们继续只作为 blocker 保留项存在于产品、freeze、release 文档。

## 3. 页面 helper 与 API wrapper 真值

| 页面 helper | API wrapper | 当前真实行为 |
|---|---|---|
| `loadTechnicianList` | `BookingApi.getTechnicianList` | 直接读取技师列表 |
| `loadTechnicianDetail` | `BookingApi.getTechnician` | 直接读取技师详情；仅作为相邻 helper 真值 |
| `loadTimeSlots` | `BookingApi.getTimeSlots` | 直接读取技师时段列表 |
| `submitBookingOrder` | `BookingApi.createOrder` | 固定补 `dispatchMode: 1` |
| `submitBookingOrderAndGo` | `submitBookingOrder` | `code === 0` 时跳转 `/pages/booking/order-detail` |
| `cancelBookingOrder` | `BookingApi.cancelOrder` | 固定默认取消原因为 `用户主动取消` |
| `cancelBookingOrderAndRefresh` | `cancelBookingOrder` | `code === 0` 时仅执行一次 `onSuccess()` |
| `submitAddonOrder` | `BookingApi.createAddonOrder` | 直接提交 add-on payload |
| `submitAddonOrderAndGo` | `submitAddonOrder` | `code === 0` 时跳转 `/pages/booking/order-detail` |

## 4. 当前稳定 errorCode 与恢复口径

| canonical truth | 当前稳定 errorCode 证据 | 当前真实恢复口径 |
|---|---|---|
| `GET /booking/technician/list` | `-`；controller/service 不做技师存在性校验，空列表 `[]` 合法 | `FAIL_OPEN` 空态；`NO_AUTO_RETRY`；无服务端 `degraded` |
| `GET /booking/slot/list-by-technician` | `-`；当前 path 无 `technicianId` 有效性错误码守卫，空列表 `[]` 合法 | `FAIL_OPEN` 空态；`NO_AUTO_RETRY`；无服务端 `degraded` |
| `POST /booking/order/create` | `TIME_SLOT_NOT_AVAILABLE(1030003001)`；当前 controller/service 链路无稳定 `SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)` 抛出证据 | `FAIL_CLOSE`；`NO_AUTO_RETRY`；失败后不跳转 |
| `GET /booking/order/get` | `BOOKING_ORDER_NOT_OWNER(1030004006)`；订单不存在当前返回 `success(null)`，不是 `BOOKING_ORDER_NOT_EXISTS(1030004000)` | `FAIL_CLOSE` 仅限越权；不存在按 `null` 处理；无服务端 `degraded` |
| `GET /booking/order/list` | `-`；空列表 `[]` 合法 | `FAIL_OPEN` 空态；`NO_AUTO_RETRY`；无服务端 `degraded` |
| `POST /booking/order/cancel` | `BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_NOT_OWNER(1030004006)`、`BOOKING_ORDER_CANNOT_CANCEL(1030004005)` | `FAIL_CLOSE`；失败不刷新；成功后 `REFRESH_ONCE` |
| `POST /app-api/booking/addon/create` | `BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_NOT_OWNER(1030004006)`、`BOOKING_ORDER_STATUS_ERROR(1030004001)`、`TIME_SLOT_NOT_AVAILABLE(1030003001)`（加钟分支） | `FAIL_CLOSE`；`NO_AUTO_RETRY`；失败后不跳转 |

## 5. 必须继续保留的 legacy blocker

| legacy blocker | 当前代码状态 | blocker 仍保留的原因 |
|---|---|---|
| `GET /booking/technician/list-by-store` | 当前 `booking.js` 不再发出 | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`、`docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`、`docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md` 仍把它记为 release blocker 输入 |
| `GET /booking/time-slot/list` | 当前 `booking.js` 不再发出 | 仍是 BF-022 / freeze review 的 blocker 锚点，不能静默删除 |
| `PUT /booking/order/cancel` | 当前 `booking.js` 不再发出 | 仍是 BF-023 / freeze review 的 blocker 锚点，不能静默删除 |
| `POST /booking/addon/create` | 当前 `booking.js` 不再发出 | 仍是 BF-024 / freeze review 的 blocker 锚点，不能静默删除 |

## 6. 当前为什么仍是 `Cannot Release`
1. `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md` 仍把 `booking.create-chain`、`booking.cancel`、`booking.addon-upgrade` 固定为 `Cannot Release=Yes`，并要求 legacy path/method 清零后才能放量。
2. `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md` 仍将 BF-022/BF-023/BF-024 标为 `PLANNED_RESERVED`，且阻断理由仍引用旧 path / old method。
3. `docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md` 仍把 booking 域记为 `Still Blocked`，并要求旧 path/old method 从联调与 release allowlist 中清除后再发起下一轮 freeze。
4. 当前自动化证据仅覆盖 wrapper/helper 对齐与成功/失败分支 smoke，不等于真实 release sample 已归档。
5. 因此本文件当前只固定 contract truth，不改写窗口 A/B/D 的 release gate 结论。
