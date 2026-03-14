# MiniApp Booking User API Alignment v1 (2026-03-10)

## 1. 目标与真值来源
- 目标：把 booking 用户侧真实 `controllerPath + method + path + request/response` 固定成唯一可执行契约，并正式收口 create/cancel/addon 链路的阻断结论。
- 约束：
  - 只认当前分支真实 controller、真实前端 API 文件、已落盘产品文档。
  - 不接受 wildcard API、`TBD_*`、基于返回文案的分支判断。
  - 前端未绑定但后端已存在的接口显式记为 `ACTIVE_BE_ONLY`。
  - 旧路径一旦和真实 controller 不一致，固定记为 `BLOCKED`，不得模糊表述。
  - `Doc Closed / Contract Closed` 只代表文档与契约口径冻结；旧 path 仍是 runtime 阻断，不因文档完整而消失。
- 真值输入：
  - 前端 API：`yudao-mall-uniapp/sheep/api/trade/booking.js`
  - 后端 controller：
    - `AppTechnicianController`
    - `AppTimeSlotController`
    - `AppBookingOrderController`
    - `AppBookingAddonController`
  - 产品/真值文档：
    - `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
    - `docs/contracts/2026-03-14-miniapp-runtime-blocker-contract-closure-v1.md`

## 2. 状态定义
- `ACTIVE`：当前前端调用方与真实 controller `method + path` 一致，且允许进入当前发布口径。
- `ACTIVE_BE_ONLY`：后端接口已存在，但当前前端没有真实绑定。
- `PLANNED_RESERVED`：接口真实存在，但当前页面链路或发布门禁仍未满足，不能记为 `ACTIVE`。
- `BLOCKED`：旧路径/旧方法与真实 controller 冲突，禁止进入 `ACTIVE` 或发布 allowlist。

## 3. 页面与前端调用方

| 页面/组件 | 前端调用方 |
|---|---|
| `/pages/booking/technician-list` | `BookingApi.getTechnicianList` |
| `/pages/booking/technician-detail` | `BookingApi.getTechnician`、`BookingApi.getTimeSlots` |
| `/pages/booking/order-confirm` | `BookingApi.createOrder` |
| `/pages/booking/order-list` | `BookingApi.getOrderList`、`BookingApi.cancelOrder` |
| `/pages/booking/order-detail` | `BookingApi.getOrderDetail` |
| `/pages/booking/addon` | `BookingApi.createAddonOrder` |

## 4. Booking Canonical Contract

| 场景 | 页面/前端调用方 | controllerPath | method + path | request params/body/query | response 字段 | canonical errorCode | 状态 | failureMode | retryClass | degrade 语义 | 发布口径 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 技师列表 | `/pages/booking/technician-list` / `BookingApi.getTechnicianList` | `AppTechnicianController#getTechnicianList` | `GET /booking/technician/list` | query:`storeId(Long)` | `list[]:{id,name,avatar,introduction,tags,rating,serviceCount}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 无服务端 `degraded` 字段；无技师时返回 `[]` 为合法空态 | canonical 查询路径固定为 `GET /booking/technician/list`；由于当前 FE 仍发 `GET /booking/technician/list-by-store`，整条技师选择链路不得升 `ACTIVE` |
| 技师详情 | `/pages/booking/technician-detail` / `BookingApi.getTechnician` | `AppTechnicianController#getTechnician` | `GET /booking/technician/get` | query:`id(Long)` | `{id,name,avatar,introduction,tags,rating,serviceCount}` | `TECHNICIAN_NOT_EXISTS(1030001000)` | `ACTIVE` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；技师不存在直接报错，不回退伪对象 | 允许进入 booking 查询侧 `ACTIVE` allowlist |
| 门店时段列表 | 当前无 FE 绑定 | `AppTimeSlotController#getAvailableTimeSlots` | `GET /booking/slot/list` | query:`storeId(Long)`,`date(yyyy-MM-dd)` | `list[]:{id,technicianId,technicianName,technicianAvatar,slotDate,startTime,endTime,isOffpeak,offpeakPrice,status}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 无服务端 `degraded` 字段；无可约时段返回 `[]` | 后端真实接口已存在；后续若前端新增门店级时段页，只允许绑定此 path |
| 技师时段列表 | `/pages/booking/technician-detail` / `BookingApi.getTimeSlots` | `AppTimeSlotController#getTimeSlotsByTechnician` | `GET /booking/slot/list-by-technician` | query:`technicianId(Long)`,`date(yyyy-MM-dd)` | `list[]:{id,technicianId,technicianName,technicianAvatar,slotDate,startTime,endTime,isOffpeak,offpeakPrice,status}` | `TECHNICIAN_NOT_EXISTS(1030001000)` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 无服务端 `degraded` 字段；无可约时段返回 `[]`，技师无效仍按错误码阻断 | canonical 时段查询固定为 `GET /booking/slot/list-by-technician`；当前 FE `GET /booking/time-slot/list` 必须继续视为阻断项 |
| 创建预约订单 | `/pages/booking/order-confirm` / `BookingApi.createOrder` | `AppBookingOrderController#createOrder` | `POST /booking/order/create` | body:`timeSlotId?`,`spuId`,`skuId?`,`userRemark?`,`dispatchMode?`,`storeId?`,`bookingDate?`,`startTime?` | `orderId(Long)` | `SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_NOT_AVAILABLE(1030003001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)` | `PLANNED_RESERVED` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；冲突/库存/时段失败必须直接返回错误码 | 单接口 path 已对齐，但 booking.create 的上游 `technician list + slot list` 仍未对齐，因此 create 链路不能单独记为 `ACTIVE` |
| 预约详情 | `/pages/booking/order-detail` / `BookingApi.getOrderDetail` | `AppBookingOrderController#getOrder` | `GET /booking/order/get` | query:`id(Long)` | `{id,orderNo,storeId,storeName,technicianId,technicianName,technicianAvatar,serviceName,servicePic,bookingDate,bookingStartTime,bookingEndTime,duration,originalPrice,discountPrice,payPrice,isOffpeak,status,payTime,serviceStartTime,serviceEndTime,cancelTime,cancelReason,userRemark,createTime}` | `BOOKING_ORDER_NOT_EXISTS(1030004000)` | `ACTIVE` | `FAIL_CLOSE` | `REFRESH_ONCE` | 无 `degraded` 字段；订单不存在/无权查看直接失败 | 已纳入 booking 查询链路 `ACTIVE` 范围 |
| 我的预约列表 | `/pages/booking/order-list` / `BookingApi.getOrderList` | `AppBookingOrderController#getOrderList` | `GET /booking/order/list` | 无 query；当前 FE 透传的分页/筛选参数不被 controller 消费 | `list[]:{id,orderNo,storeId,storeName,technicianId,technicianName,technicianAvatar,serviceName,servicePic,bookingDate,bookingStartTime,bookingEndTime,duration,originalPrice,discountPrice,payPrice,isOffpeak,status,payTime,serviceStartTime,serviceEndTime,cancelTime,cancelReason,userRemark,createTime}` | `-` | `ACTIVE` | `FAIL_OPEN` | `REFRESH_ONCE` | 空列表为合法空态；不返回 `degraded` 字段 | 允许进入查询 allowlist；若需要状态筛选，不得继续假定 `/list` 支持扩参 |
| 按状态查询预约列表 | 当前无 FE 绑定 | `AppBookingOrderController#getOrderListByStatus` | `GET /booking/order/list-by-status` | query:`status(Integer)` | `list[]:{id,orderNo,storeId,storeName,technicianId,technicianName,technicianAvatar,serviceName,servicePic,bookingDate,bookingStartTime,bookingEndTime,duration,originalPrice,discountPrice,payPrice,isOffpeak,status,payTime,serviceStartTime,serviceEndTime,cancelTime,cancelReason,userRemark,createTime}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 空列表为合法空态；不回落到 `/list` 伪筛选 | 后端已存在但 FE 未绑定；后续若做状态筛选，只允许接此接口 |
| 根据订单号查询预约 | 当前无 FE 绑定 | `AppBookingOrderController#getOrderByOrderNo` | `GET /booking/order/get-by-order-no` | query:`orderNo(String)` | `{id,orderNo,storeId,storeName,technicianId,technicianName,technicianAvatar,serviceName,servicePic,bookingDate,bookingStartTime,bookingEndTime,duration,originalPrice,discountPrice,payPrice,isOffpeak,status,payTime,serviceStartTime,serviceEndTime,cancelTime,cancelReason,userRemark,createTime}` | `BOOKING_ORDER_NOT_EXISTS(1030004000)` | `ACTIVE_BE_ONLY` | `FAIL_CLOSE` | `REFRESH_ONCE` | 无 `degraded` 字段；订单号无效时直接报错 | 只作为后端可用真实查单接口；前端支付回流如需按订单号查单，只能绑定此 path |
| 取消预约 | `/pages/booking/order-list` / `BookingApi.cancelOrder` | `AppBookingOrderController#cancelOrder` | `POST /booking/order/cancel` | query:`id(Long)`,`reason(String)?` | `true` | `BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_STATUS_ERROR(1030004001)` | `PLANNED_RESERVED` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；取消失败必须按 errorCode 分支，不得看返回文案 | canonical 取消口径固定为 `POST + query(id,reason?)`；当前 FE `PUT + body{id,cancelReason}` 固定阻断，取消链路不得升 `ACTIVE` |
| 创建加钟/升级/加项目订单 | `/pages/booking/addon` / `BookingApi.createAddonOrder` | `AppBookingAddonController#createAddonOrder` | `POST /app-api/booking/addon/create` | body:`parentOrderId`,`addonType`,`spuId?`,`skuId?` | `orderId(Long)` | `BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_STATUS_ERROR(1030004001)` | `PLANNED_RESERVED` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；失败时只按 errorCode 驱动 UI 恢复 | canonical add-on path 固定带 `/app-api` 前缀；当前 FE `/booking/addon/create` 必须继续视为阻断项 |

## 5. 明确禁止进入 `ACTIVE` 的旧路径

| legacy method + path | 当前前端调用方 | canonical 替代 | 状态 | 阻断原因 | Doc Closed | Contract Closed | Runtime Not Proven | Release Blocked | 发布口径 |
|---|---|---|---|---|---|---|---|---|---|
| `GET /booking/technician/list-by-store` | `BookingApi.getTechnicianList` | `GET /booking/technician/list` | `BLOCKED` | 后端 app controller 不存在该 path | `Yes` | `Yes` | `Yes` | `Yes` | 不得进入 allowlist、网关转发白名单或 frozen API 清单 |
| `GET /booking/time-slot/list` | `BookingApi.getTimeSlots` | `GET /booking/slot/list-by-technician` | `BLOCKED` | path 与语义均漂移；真实 controller 在 `/booking/slot/*` | `Yes` | `Yes` | `Yes` | `Yes` | 不得进入 `ACTIVE`，不得在 FE/PRD 中继续作为时段真值 |
| `PUT /booking/order/cancel` | `BookingApi.cancelOrder` | `POST /booking/order/cancel` | `BLOCKED` | method、参数承载方式、字段名均不一致 | `Yes` | `Yes` | `Yes` | `Yes` | 任何 cancel 联调、网关签名、缓存策略都只能按 `POST` 版本执行 |
| `POST /booking/addon/create` | `BookingApi.createAddonOrder` | `POST /app-api/booking/addon/create` | `BLOCKED` | 缺少 `/app-api` 前缀，无法定位真实 controller | `Yes` | `Yes` | `Yes` | `Yes` | add-on 能力在前端改绑前一律不得记为已上线 |

## 6. 03-14 Runtime Blocker Closure Ledger

| 阻断能力 | canonical contract | 当前真实阻断证据 | 不得误升的能力口径 | Doc Closed | Contract Closed | Runtime Not Proven | Release Blocked |
|---|---|---|---|---|---|---|---|
| `booking.technician-select` | `GET /booking/technician/list` | FE 仍调用 `GET /booking/technician/list-by-store`；真实 controller 仅有 `GET /booking/technician/list` | `/pages/booking/technician-list` 不得写成已闭环或 `ACTIVE` | `Yes` | `Yes` | `Yes` | `Yes` |
| `booking.slot-select` | `GET /booking/slot/list-by-technician` | FE 仍调用 `GET /booking/time-slot/list`；真实时段 controller 在 `/booking/slot/*` | `/pages/booking/technician-detail` 的时段选择不得写成已闭环或 `ACTIVE` | `Yes` | `Yes` | `Yes` | `Yes` |
| `booking.create-chain` | `GET /booking/technician/list` + `GET /booking/slot/list-by-technician` + `POST /booking/order/create` | `POST /booking/order/create` 本身已对齐，但上游技师列表/时段列表仍卡在 legacy path | `booking.create` 不得因为 create 接口单点对齐就升为 `ACTIVE` | `Yes` | `Yes` | `Yes` | `Yes` |
| `booking.cancel` | `POST /booking/order/cancel` + query:`id`,`reason?` | FE 仍走 `PUT /booking/order/cancel` + body:`id`,`cancelReason` | `/pages/booking/order-list`、`/pages/booking/order-detail` 的取消能力不得升 `ACTIVE` | `Yes` | `Yes` | `Yes` | `Yes` |
| `booking.addon` | `POST /app-api/booking/addon/create` | FE 仍走 `POST /booking/addon/create`，缺少 `/app-api` 前缀 | `/pages/booking/addon` 不得写成已上线或放量中 | `Yes` | `Yes` | `Yes` | `Yes` |

说明：
- `GET /booking/order/get`、`GET /booking/order/list` 这类查询接口仍按本文件第 4 节维持 `ACTIVE`，不属于本批 runtime blocker。
- 旧 path 阻断项继续保留在第 5 节，后续文档不能因为“contract 已写清”而删除这些阻断条目。

## 7. Booking 链路 canonical 结论
1. `technician list`
   - canonical：`GET /booking/technician/list`
   - 结论：当前 FE 旧 path 被 `BLOCKED`，因此整条技师选择链路仍是 `PLANNED_RESERVED`
2. `slot list / list-by-technician`
   - store 级列表：`GET /booking/slot/list`，当前仅 `ACTIVE_BE_ONLY`
   - technician 级列表：`GET /booking/slot/list-by-technician`
   - 结论：当前 FE `/booking/time-slot/list` 被 `BLOCKED`，时段链路不得记 `ACTIVE`
3. `order create`
   - canonical：`POST /booking/order/create`
   - 结论：单接口对齐，但因为上游 technician/slot 链路仍阻断，create 维持 `PLANNED_RESERVED`
4. `order cancel`
   - canonical：`POST /booking/order/cancel` + query `id`,`reason?`
   - 结论：当前 FE `PUT` 版本固定 `BLOCKED`，cancel 维持 `PLANNED_RESERVED`
5. `addon create`
   - canonical：`POST /app-api/booking/addon/create`
   - 结论：前端 path 缺前缀，当前固定 `BLOCKED`，add-on 维持 `PLANNED_RESERVED`
6. `order list-by-status`
   - canonical：`GET /booking/order/list-by-status`
   - 结论：真实后端存在但当前 FE 未绑定，状态固定为 `ACTIVE_BE_ONLY`
7. `get-by-order-no`
   - canonical：`GET /booking/order/get-by-order-no`
   - 结论：真实后端存在但当前 FE 未绑定，状态固定为 `ACTIVE_BE_ONLY`
8. `03-14 blocker closure`
   - 结论：`Booking` 的 legacy path 阻断项只算 `Doc Closed / Contract Closed`，不算 runtime 已证实；开发与放量仍继续阻断。

## 8. 跨窗口联调约束
- 只按 `errorCode` 分支，不按返回文案分支。
- `cancelOrder` 字段冲突必须显式处理：
  - FE 旧字段：`cancelReason`
  - BE 真实字段：`reason`
- `getOrderList` 不消费分页和筛选参数；若继续传递，只能视作 FE 本地状态，不是服务端协议。
- `createOrder` 互斥字段约束必须由前端显式遵守：
  - 点钟：`timeSlotId` 必填
  - 排钟：`storeId + bookingDate + startTime` 必填
- `createAddonOrder` 字段组合固定：
  - `addonType=1(加钟)`：`parentOrderId + spuId + skuId`
  - `addonType=2(升级)`：`parentOrderId + skuId`
  - `addonType=3(加项目)`：`parentOrderId + spuId + skuId`
- 旧 path 阻断项必须继续出现在联调清单、发布门禁和 freeze review 中，不得因本文件已完整描述而移除。
