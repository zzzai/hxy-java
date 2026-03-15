# Window C Handoff - Booking Runtime Contract（2026-03-15）

## 1. 本批范围
- 分支：`window-c-booking-runtime-contract-20260315`
- 输出类型：仅更新 booking contract / errorCode 文档与新增 handoff；未改业务代码、未改 overlay 页面、未动 `.codex`、未改历史 handoff、未处理无关 modified/untracked。
- 新增：
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-release-evidence-contract-v1.md`
  - `hxy/07_memory_archive/handoffs/2026-03-15/booking-runtime-contract-window-c.md`
- 更新：
  - `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`

## 2. 当前固定结论

### 2.1 Runtime canonical truth
- `booking.js` 当前真实 method/path 已与 app controller 对齐：
  - `GET /booking/technician/list`
  - `GET /booking/slot/list-by-technician`
  - `POST /booking/order/create`
  - `GET /booking/order/get`
  - `GET /booking/order/list`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
- `pages/booking/logic.js` 当前 helper 也已对齐：
  - `loadTechnicianList -> getTechnicianList`
  - `loadTimeSlots -> getTimeSlots`
  - `submitBookingOrder* -> createOrder`
  - `cancelBookingOrder* -> cancelOrder`
  - `submitAddonOrder* -> createAddonOrder`
- 相邻但不在本批 release gate 的 helper 真值：
  - `loadTechnicianDetail -> getTechnician -> GET /booking/technician/get`

### 2.2 Legacy blocker 固定保留
- 以下 legacy path / old method 当前已不再由 `booking.js` 发出，但必须继续保留为 blocker：
  - `GET /booking/technician/list-by-store`
  - `GET /booking/time-slot/list`
  - `PUT /booking/order/cancel`
  - `POST /booking/addon/create`
- 保留原因：
  - 03-09 booking PRD 仍用它们作为 `Cannot Release` 条件
  - 03-12 truth ledger 仍用它们阻断 BF-022/BF-023/BF-024
  - 03-09 ready-to-frozen review 仍要求它们从联调与 allowlist 中清零
  - 03-14 blocker closure 文档仍把它们作为 release blocker 保留项

### 2.3 Stable errorCode 结论
- 本批新增进入 canonical register：
  - `BOOKING_ORDER_CANNOT_CANCEL(1030004005)`
  - `BOOKING_ORDER_NOT_OWNER(1030004006)`
- 当前 booking runtime 可确认的稳定 code：
  - `TIME_SLOT_NOT_AVAILABLE(1030003001)`
  - `BOOKING_ORDER_NOT_EXISTS(1030004000)`
  - `BOOKING_ORDER_STATUS_ERROR(1030004001)` 仅 add-on path
  - `BOOKING_ORDER_CANNOT_CANCEL(1030004005)`
  - `BOOKING_ORDER_NOT_OWNER(1030004006)`
- 当前明确不能写成稳定 code：
  - `TECHNICIAN_NOT_EXISTS(1030001000)` 不能用于 `GET /booking/technician/get`
  - `SCHEDULE_CONFLICT(1030002001)` 不能用于当前 app create path
  - `TIME_SLOT_ALREADY_BOOKED(1030003002)` 不能用于当前 app create/add-on path
  - `BOOKING_ORDER_NOT_EXISTS(1030004000)` 不能用于 `GET /booking/order/get` miss
  - `BOOKING_ORDER_STATUS_ERROR(1030004001)` 不能用于 `POST /booking/order/cancel`

### 2.4 Fail-open / fail-close / retry 口径
- `FAIL_OPEN`
  - `GET /booking/technician/list`
  - `GET /booking/slot/list-by-technician`
  - `GET /booking/order/list`
  - 空列表 `[]` 合法，不走 errorCode 分支
- `FAIL_CLOSE`
  - `POST /booking/order/create`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
  - service exception 直接阻断，helper 只在 `code === 0` 时继续
- `NO_AUTO_RETRY`
  - create 失败不跳转
  - cancel 失败不刷新
  - add-on 失败不跳转
- `REFRESH_ONCE`
  - 仅 `cancelBookingOrderAndRefresh` 在 `code === 0` 时执行一次 `onSuccess()`
- 当前无服务端 `degraded=true / degradeReason` 证据

### 2.5 为什么当前仍是 `Cannot Release`
- 当前 runtime contract 已对齐，但 release gate 还没有被 A/B/D 侧重签。
- 当前 booking 写链路仍被以下文档口径阻断：
  - `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
  - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md`
- 当前自动化仅为 wrapper/helper smoke，不是 release sample pack。

## 3. 给窗口 A / B / D 的联调注意点

### 3.1 给窗口 A
- A 侧可以把 booking runtime contract 改写为“当前 FE wrapper 与 app controller 已对齐”，但不能直接改成 `Release OK`。
- 旧 blocker 四项必须继续留在 capability / freeze / allowlist，直到 A 完成 release gate 重签。
- `GET /booking/order/get` 的 miss 不能再按 `BOOKING_ORDER_NOT_EXISTS(1030004000)` 分支；当前真实口径是 `success(null)`。

### 3.2 给窗口 B
- B 侧需要把产品文档里的“FE 仍发旧 path / old method”改成“legacy blocker 保留，但当前 wrapper 已对齐”。
- create/cancel/add-on 的用户恢复动作只能按 code 写：
  - create：`1030003001`
  - cancel：`1030004000` / `1030004005` / `1030004006`
  - add-on：`1030003001` / `1030004000` / `1030004001` / `1030004006`
- 不得把 `SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)`、`TECHNICIAN_NOT_EXISTS(1030001000)` 写成当前 runtime 稳定分支。

### 3.3 给窗口 D
- D 侧 runbook / alert / gate 需要区分：
  - list 查询空态 `[]` 是合法空态，不是 degraded
  - cancel 成功后的 `refresh-once` 是前端 helper 行为，不是服务端重试
  - 当前没有服务端 `degraded=true / degradeReason`
- release gate 仍应保持 `Cannot Release`，直到 D 侧 sample/gate 文档与 A/B 同步完成。
