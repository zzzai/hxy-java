# Window C Handoff - Booking Runtime Contract（2026-03-15）

## 1. 本批范围
- 分支：`window-c-booking-runtime-contract-20260315`
- 输出类型：仅更新 booking contract / errorCode 文档与新增 handoff；未改业务代码、未改 overlay 页面、未动 `.codex`、未改历史 handoff、未处理无关 modified/untracked。
- 新增：
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-release-evidence-contract-v1.md`
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
  - `hxy/07_memory_archive/handoffs/2026-03-15/booking-runtime-contract-window-c.md`
- 更新：
  - `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`

## 2. 当前固定结论

### 2.1 Runtime canonical truth
- `booking.js` 当前真实 method/path 已与 app controller 对齐：
  - `GET /booking/technician/list`
  - `GET /booking/technician/get`
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
  - 当前已并入 query-only release scope：
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
- 当前 booking runtime 可确认的稳定 code：
  - `TIME_SLOT_NOT_AVAILABLE(1030003001)`
  - `BOOKING_ORDER_NOT_EXISTS(1030004000)`
  - `BOOKING_ORDER_STATUS_ERROR(1030004001)` 仅 add-on path
  - `BOOKING_ORDER_CANNOT_CANCEL(1030004005)`
  - `BOOKING_ORDER_NOT_OWNER(1030004006)`
- 当前明确不能写成稳定 code：
  - `TECHNICIAN_NOT_EXISTS(1030001000)` 不能用于 `GET /booking/technician/get`
  - `TECHNICIAN_DISABLED(1030001001)` 不能用于 `GET /booking/technician/get`
  - `SCHEDULE_CONFLICT(1030002001)` 不能用于当前 app create path
  - `TIME_SLOT_ALREADY_BOOKED(1030003002)` 不能用于当前 app create/add-on path
  - `BOOKING_ORDER_NOT_EXISTS(1030004000)` 不能用于 `GET /booking/order/get` miss
  - `BOOKING_ORDER_STATUS_ERROR(1030004001)` 不能用于 `POST /booking/order/cancel`

### 2.4 Fail-open / fail-close / retry 口径
- `FAIL_OPEN`
  - `GET /booking/technician/list`
  - `GET /booking/technician/get` 的 miss 当前返回 `success(null)`
  - `GET /booking/slot/list-by-technician`
  - `GET /booking/order/list`
  - 空列表 `[]` 与 `success(null)` 都是结构态，不走 errorCode 分支
- `FAIL_CLOSE`
  - `GET /booking/order/get` 的越权分支
  - `POST /booking/order/create`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
  - service exception 直接阻断，helper 只在 `code === 0` 时继续
- `NO_AUTO_RETRY`
  - create 失败不跳转
  - add-on 失败不跳转
- `REFRESH_ONCE`
  - 预约列表查询支持手动刷新一次
  - 仅 `cancelBookingOrderAndRefresh` 在 `code === 0` 时执行一次 `onSuccess()`
- 当前未核到 `MANUAL_RETRY_3` 类型的 booking runtime helper/contract 证据
- 当前无服务端 `degraded=true / degradeReason` 证据

### 2.5 为什么当前仍是 `Cannot Release`
- 当前 query-only 范围已可 `Can Release=Yes`：
  - `technician-list`
  - `technician-detail`
  - `order-list`
  - `order-detail`
- 当前 write-chain 仍统一 `Can Release=No`：
  - create
  - cancel
  - addon
- 当前自动化仅为 wrapper/helper smoke，不是 release sample pack；A/B/D 未完成 release 签发前，No-Go 继续保留。

## 3. 给窗口 A / B / D / E 的联调注意点

### 3.1 给窗口 A
- A 侧可以把 query-only 四页写成当前可发布查询范围，但不能把 create / cancel / addon 一并改成 `Release OK`。
- 旧 blocker 四项必须继续留在 capability / freeze / allowlist。
- `GET /booking/order/get` 的 miss 不能再按 `BOOKING_ORDER_NOT_EXISTS(1030004000)` 分支；当前真实口径是 `success(null)`。
- `GET /booking/order/list` 当前 controller 不消费 `pageNo/pageSize/status`，A 不要把 tabs 当成服务端状态筛选闭环。

### 3.2 给窗口 B
- B 侧产品文档应继续保持：
  - query-only `ACTIVE`
  - create / cancel / addon `Can Develop / Cannot Release`
- create/cancel/add-on 的用户恢复动作只能按 code 写：
  - create：`1030003001`
  - cancel：`1030004000` / `1030004005` / `1030004006`
  - add-on：`1030003001` / `1030004000` / `1030004001` / `1030004006`
- 不得把 `SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)`、`TECHNICIAN_NOT_EXISTS(1030001000)`、`TECHNICIAN_DISABLED(1030001001)` 写成当前 runtime 稳定分支。

### 3.3 给窗口 D
- D 侧 runbook / alert / gate 需要区分：
  - list 查询空态 `[]` 是合法空态，不是 degraded
  - `success(null)` 也是结构态，不是 degraded
  - cancel 成功后的 `refresh-once` 是前端 helper 行为，不是服务端重试
  - 当前没有服务端 `degraded=true / degradeReason`
- release gate 仍应保持 `Cannot Release`，直到 D 侧 sample/gate 文档与 A/B 同步完成。

### 3.4 给窗口 E
- E 侧若负责样本归档、验收脚本或证据汇总，只能按当前 stable code 归档：
  - `1030003001`
  - `1030004000`
  - `1030004001`（仅 add-on）
  - `1030004005`
  - `1030004006`
- 不得把以下 code 归档成当前 booking runtime 页面稳定样本：
  - `1030001000`
  - `1030001001`
  - `1030002001`
  - `1030003002`
- 若 E 侧脚本发现 `degraded=true / degradeReason` 样本，应先判为口径漂移，而不是直接吸收到 booking runtime 成功/降级统计。
