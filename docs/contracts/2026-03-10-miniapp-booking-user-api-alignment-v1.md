# MiniApp Booking User API Alignment v1 (2026-03-10)

## 1. 目标与真值来源
- 目标：把 booking 用户侧当前真实 route/helper/controller 对齐状态写清，同时把“route 已对齐”和“页面已闭环”明确拆开。
- 当前单一细表引用：
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-release-evidence-contract-v1.md`

## 2. 当前 route / helper / controller 对齐结论

| scope | 当前 FE wrapper / helper | 当前 controller | static route alignment | runtime field / shape alignment | Can Develop | Can Release |
|---|---|---|---|---|---|---|
| technician-list query | `BookingApi.getTechnicianList` / `loadTechnicianList` | `AppTechnicianController#getTechnicianList` | `Aligned` | `Drifted`：`title/specialties/status` 没有 backend 字段绑定 | Yes | No |
| technician-detail query | `BookingApi.getTechnician` / `loadTechnicianDetail` | `AppTechnicianController#getTechnician` | `Aligned` | `Drifted`：查无记录是 `success(null)`；不得按 `1030001000/1030001001` 分支 | Yes | No |
| technician-detail slot query | `BookingApi.getTimeSlots` / `loadTimeSlots` | `AppTimeSlotController#getTimeSlotsByTechnician` | `Aligned` | `Partially aligned`：详情页使用的字段可核出，但确认页依赖的 `duration/spuId/skuId` 没有响应绑定 | Yes | No |
| order-list query | `BookingApi.getOrderList` | `AppBookingOrderController#getOrderList` | `Aligned` | `Drifted`：页面按 `data.list/data.total` 读取，controller 实际返回 `data[]`；`payOrderId` 未绑定 | Yes | No |
| order-detail query | `BookingApi.getOrderDetail` / `goToOrderDetail` | `AppBookingOrderController#getOrder` | `Aligned` | `Drifted`：`success(null)` 是空结果；`payOrderId` 未绑定 | Yes | No |
| create | `BookingApi.createOrder` / `submitBookingOrder*` | `AppBookingOrderController#createOrder` | `Aligned` | `Drifted`：确认页预读和请求体字段链路未闭环 | Yes | No |
| cancel | `BookingApi.cancelOrder` / `cancelBookingOrder*` | `AppBookingOrderController#cancelOrder` | `Aligned` | `Partially aligned`：helper 行为冻结，但没有读后回写运行样本 | Yes | No |
| addon | `BookingApi.createAddonOrder` / `submitAddonOrder*` | `AppBookingAddonController#createAddonOrder` | `Aligned` | `Drifted`：页面不提交 `spuId/skuId`，存在 `pseudo success / no-op risk` | Yes | No |

## 3. 当前 helper 真值

| helper | 当前真实行为 | 风险边界 |
|---|---|---|
| `loadTechnicianList` | 直接调用 `GET /booking/technician/list` | 只证明 route 对齐，不证明页面字段闭环 |
| `loadTechnicianDetail` | 直接调用 `GET /booking/technician/get` | `success(null)` 只能当空结果 |
| `loadTimeSlots` | 直接调用 `GET /booking/slot/list-by-technician` | 详情页和确认页对字段需求不一致 |
| `submitBookingOrderAndGo` | 仅 `code===0` 跳详情 | 当前没有已提交证据证明页面能稳定拼出合法 `spuId` |
| `cancelBookingOrderAndRefresh` | 仅 `code===0` 刷新一次 | 当前没有读后回写样本，不能把 `code=0` 直接写成成功 |
| `submitAddonOrderAndGo` | 仅 `code===0` 跳详情 | add-on 页 payload 仍缺 `spuId/skuId` 绑定 |

## 4. 当前 stable errorCode 与 failureMode

| scope | 当前 stable errorCode | failureMode | retryClass | 当前真实口径 |
|---|---|---|---|---|
| technician-list query | `-` | `FAIL_OPEN` | `NO_AUTO_RETRY` | 合法空列表 `[]`；无 `degraded` |
| technician-detail query | `-` | `FAIL_OPEN` | `NO_AUTO_RETRY` | miss=`success(null)`；不是 `1030001000/1030001001` |
| technician-detail slot query | `-` | `FAIL_OPEN` | `NO_AUTO_RETRY` | 空列表合法；不是成功样本 |
| order-list query | `-` | `FAIL_OPEN` | `REFRESH_ONCE` | `code=0` 但页面仍可能因 shape drift 渲染空态 |
| order-detail query | `BOOKING_ORDER_NOT_OWNER(1030004006)` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | miss=`success(null)`；不是 `1030004000` |
| create | `TIME_SLOT_NOT_AVAILABLE(1030003001)` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 当前不得把 `1030002001/1030003002` 写成稳定 code |
| cancel | `1030004000/1030004005/1030004006` | `FAIL_CLOSE` | `REFRESH_ONCE` | 成功才刷新一次；失败不刷新 |
| addon | `1030003001/1030004000/1030004001/1030004006` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | upgrade 缺 `skuId` 也可能落 `1030004000`；add-item 存在 `pseudo success / no-op risk` |

## 5. 必须继续保留的 legacy blocker
- `GET /booking/technician/list-by-store`
- `GET /booking/time-slot/list`
- `PUT /booking/order/cancel`
- `POST /booking/addon/create`

这些 legacy blocker 当前只能写成：
- release blocker 保留项
- truth ledger / freeze review 锚点

不能再写成：
- 当前 frontend canonical path
- 当前页面仍在调用的 method/path

## 6. 当前为什么仍是 `Can Develop / Cannot Release`
1. 当前 booking 只证明了 wrapper/helper/controller 的 route 对齐。
2. 当前 booking 还没有证明 page -> response field、page -> request payload、写后回读 已闭环。
3. 当前没有 create / cancel / addon 的发布级 success/failure 样本包。
4. 当前没有服务端 `degraded=true / degradeReason` 证据。
5. 因此本文件只能把 booking 写成：
   - `Can Develop=Yes`
   - `Can Release=No`
