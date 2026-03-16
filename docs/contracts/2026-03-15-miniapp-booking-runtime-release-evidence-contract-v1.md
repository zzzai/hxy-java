# MiniApp Booking Runtime Release Evidence Contract v1 (2026-03-15)

## 1. 目标与证据范围
- 目标：把 booking runtime 当前 release 证据、canonical method/path、stable errorCode、failureMode、retryClass 与 No-Go 条件收口成单一 contract。
- 取证文件：
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - `AppTechnicianController`
  - `AppTimeSlotController`
  - `AppBookingOrderController`
  - `AppBookingAddonController`
  - `BookingOrderServiceImpl`
  - `BookingAddonServiceImpl`
  - `TechnicianServiceImpl`
  - `TimeSlotServiceImpl`
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-release-evidence-review-v1.md`
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`

## 2. 当前 release scope 结论

| scope | canonical method/path | frontend 调用点 | stable errorCode | failureMode / retryClass | Can Develop | Can Release | 当前结论 |
|---|---|---|---|---|---|---|---|
| 技师列表查询 | `GET /booking/technician/list` | `BookingApi.getTechnicianList` / `loadTechnicianList` | `-` | `FAIL_OPEN / NO_AUTO_RETRY` | Yes | Yes | query-only `ACTIVE` |
| 技师详情查询 | `GET /booking/technician/get` | `BookingApi.getTechnician` / `loadTechnicianDetail` | `-` | `FAIL_OPEN / NO_AUTO_RETRY` | Yes | Yes | query-only `ACTIVE`；miss 返回 `success(null)` |
| 技师时段查询 | `GET /booking/slot/list-by-technician` | `BookingApi.getTimeSlots` / `loadTimeSlots` | `-` | `FAIL_OPEN / NO_AUTO_RETRY` | Yes | Yes | query-only `ACTIVE`；空列表合法 |
| 预约列表查询 | `GET /booking/order/list` | `BookingApi.getOrderList` | `-` | `FAIL_OPEN / REFRESH_ONCE` | Yes | Yes | query-only `ACTIVE`；页面支持手动刷新 |
| 预约详情查询 | `GET /booking/order/get` | `BookingApi.getOrderDetail` | `BOOKING_ORDER_NOT_OWNER(1030004006)` | `FAIL_CLOSE / NO_AUTO_RETRY` | Yes | Yes | 越权稳定报错；订单 miss 当前返回 `success(null)` |
| 创建预约 | `POST /booking/order/create` | `BookingApi.createOrder` / `submitBookingOrder*` | `TIME_SLOT_NOT_AVAILABLE(1030003001)` | `FAIL_CLOSE / NO_AUTO_RETRY` | Yes | No | helper 失败不跳详情，仍缺 release sample |
| 取消预约 | `POST /booking/order/cancel` | `BookingApi.cancelOrder` / `cancelBookingOrder*` | `1030004000`、`1030004005`、`1030004006` | `FAIL_CLOSE / REFRESH_ONCE` | Yes | No | helper 失败不刷新，成功只刷新一次 |
| 加钟 / 升级 / 加项目 | `POST /app-api/booking/addon/create` | `BookingApi.createAddonOrder` / `submitAddonOrder*` | `1030003001`、`1030004000`、`1030004001`、`1030004006` | `FAIL_CLOSE / NO_AUTO_RETRY` | Yes | No | helper 失败不跳详情，仍缺 release sample |

## 3. Controller-only 不等于页面闭环

| controller method/path | 当前状态 | 结论 |
|---|---|---|
| `GET /booking/slot/list` | backend only | 不得写成门店时段页已闭环 |
| `GET /booking/order/list-by-status` | backend only | 不得写成列表页已绑定服务端状态筛选 |
| `GET /booking/order/get-by-order-no` | backend only | 不得写成支付回流页已闭环 |

## 4. 当前 stable errorCode 与非 stable 边界
- 当前 booking runtime 可稳定对外分支的 code 只认：
  - `TIME_SLOT_NOT_AVAILABLE(1030003001)`
  - `BOOKING_ORDER_NOT_EXISTS(1030004000)` 仅限 cancel / add-on path
  - `BOOKING_ORDER_STATUS_ERROR(1030004001)` 仅限 add-on path
  - `BOOKING_ORDER_CANNOT_CANCEL(1030004005)`
  - `BOOKING_ORDER_NOT_OWNER(1030004006)`
- 当前不得写成 booking runtime stable code：
  - `TECHNICIAN_NOT_EXISTS(1030001000)`
  - `TECHNICIAN_DISABLED(1030001001)`
  - `SCHEDULE_CONFLICT(1030002001)`
  - `TIME_SLOT_ALREADY_BOOKED(1030003002)`
  - `BOOKING_ORDER_NOT_EXISTS(1030004000)` 用于 `GET /booking/order/get` miss
  - `BOOKING_ORDER_STATUS_ERROR(1030004001)` 用于 `POST /booking/order/cancel`

## 5. legacy blocker 继续保留，但不再是当前 wrapper 真值
- 以下旧 path / old method 当前已不再由 `booking.js` 发出：
  - `GET /booking/technician/list-by-store`
  - `GET /booking/time-slot/list`
  - `PUT /booking/order/cancel`
  - `POST /booking/addon/create`
- 但它们仍必须作为 release blocker 保留项存在于产品、freeze、release 口径中，直到窗口 A/B/D 重新签发 release gate。

## 6. 当前没有证据的 `degraded=true / degradeReason`
- 当前 booking runtime controller、service、helper、tests 都没有服务端 `degraded=true / degradeReason` 证据。
- 当前 query 页的空列表与 `success(null)` 只是结构化成功或空态，不是服务端降级。

## 7. 当前为什么仍是 `Can Develop = Yes / Can Release = No`
1. 03-15 产品 release evidence review 已把 booking 域固定为：
   - `Doc Closed`
   - `Can Develop`
   - `Cannot Release`
2. 当前 query-only 页面已有真实 wrapper/helper/controller/test 证据，因此 query scope 可继续 `ACTIVE`。
3. 当前 create / cancel / addon 只有：
   - wrapper 与 controller 对齐证据
   - helper 成功/失败行为 smoke 证据
4. 当前仍缺：
   - 发布级 success / failure sample 包
   - 运行回放证据
   - allowlist / 巡检 / No-Go 退出证据
5. 因此本批 contract 结论只能是：
   - query-only `Can Release=Yes`
   - write-chain `Can Release=No`

## 8. 当前 No-Go 条件
1. 把 create / cancel / addon 任一写链改写为 `Can Release=Yes`。
2. 把 controller-only path 误写成真实页面闭环。
3. 在 booking runtime 当前页按 message 分支。
4. 把 `1030001000/1030001001/1030002001/1030003002` 写成当前 runtime stable code。
5. 在 contract、SOP、runbook 里补造 `degraded=true / degradeReason`。
