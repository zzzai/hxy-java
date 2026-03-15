# Window C Handoff - Booking Runtime Contract（2026-03-15）

## 1. 本批范围
- 分支：`window-c-booking-runtime-contract-20260315`
- 仅更新 booking contract / errorCode / API matrix 文档与当前 handoff。
- 未改业务代码、未改 overlay 页面、未动 `.codex`、未改历史 handoff、未处理无关 modified/untracked。
- 新增：
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
- 更新：
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-release-evidence-contract-v1.md`
  - `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
  - `hxy/07_memory_archive/handoffs/2026-03-15/booking-runtime-contract-window-c.md`

## 2. 当前固定结论

### 2.1 Canonical method/path
- 当前 `booking.js` 与 app controller 的 canonical method/path 对齐项只认：
  - `GET /booking/technician/list`
  - `GET /booking/technician/get`
  - `GET /booking/slot/list-by-technician`
  - `GET /booking/order/list`
  - `GET /booking/order/get`
  - `POST /booking/order/create`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
- 但“route 对齐”不等于“页面闭环”：
  - technician 页仍有字段 drift
  - order-list 仍有响应 shape drift
  - order-confirm / addon 仍有请求体字段 drift

### 2.2 Stable errorCode
- 当前 booking runtime page contract 只允许稳定引用：
  - `TIME_SLOT_NOT_AVAILABLE(1030003001)`
  - `BOOKING_ORDER_NOT_EXISTS(1030004000)`
  - `BOOKING_ORDER_STATUS_ERROR(1030004001)` 仅 add-on path
  - `BOOKING_ORDER_CANNOT_CANCEL(1030004005)`
  - `BOOKING_ORDER_NOT_OWNER(1030004006)`
- 当前明确不能写成稳定 runtime code：
  - `TECHNICIAN_NOT_EXISTS(1030001000)`
  - `TECHNICIAN_DISABLED(1030001001)`
  - `SCHEDULE_CONFLICT(1030002001)`
  - `TIME_SLOT_ALREADY_BOOKED(1030003002)`
- 特别说明：
  - `GET /booking/order/get` miss 是 `success(null)`，不是 `1030004000`
  - add-on 页当前缺 `skuId` 绑定时，`upgrade` 路径也可能落 `1030004000`

### 2.3 Fail-open / fail-close / retry
- `FAIL_OPEN`
  - `GET /booking/technician/list`
  - `GET /booking/technician/get`
  - `GET /booking/slot/list-by-technician`
  - `GET /booking/order/list`
- `FAIL_CLOSE`
  - `GET /booking/order/get` 的越权分支
  - `POST /booking/order/create`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
- `NO_AUTO_RETRY`
  - technician-list / technician-detail query
  - technician slot query
  - create
  - order-detail query
  - addon
- `REFRESH_ONCE`
  - order-list query 的人工刷新
  - cancel 成功后的单次刷新
- 当前 booking runtime page contract 没有 `MANUAL_RETRY_3` 的已提交证据。
- 当前没有服务端 `degraded=true / degradeReason` 证据。

### 2.4 当前 No-Go 条件
1. 把 controller-only path 写成页面闭环。
2. 把 `[] / null / 0` 写成成功样本。
3. 把 `code=0` 但写后未读到预期变化写成成功。
4. 把 add-on `code=0` 的零价格空商品写入写成真实成功；它属于 `pseudo success / no-op risk`。
5. 把 `1030001000/1030001001/1030002001/1030003002` 写成当前 booking runtime page 稳定 code。
6. 把 helper smoke、runtime gate `PASS` 或 shared chain `rc=0` 外推成 release-ready。

## 3. 给窗口 A / B / D / E 的联调注意点

### 3.1 给窗口 A
- A 侧不得继续把 booking 写成 query-only 已闭环而省略字段 drift。
- 至少要补出以下 contract 结论：
  - technician 页字段仍 drift
  - order-list 响应 shape 仍 drift
  - order-confirm / addon 请求体仍 drift
- `GET /booking/order/get` miss 只能写 `success(null)`。
- `GET /booking/order/list` 当前 controller 不消费 `pageNo/pageSize/status`，不要写成按状态分页已闭环。

### 3.2 给窗口 B
- B 侧用户恢复动作只能按 code 写，不能按错误文案写。
- create 只能稳定写：
  - `1030003001`
- cancel 只能稳定写：
  - `1030004000`
  - `1030004005`
  - `1030004006`
- addon 只能稳定写：
  - `1030003001`
  - `1030004000`
  - `1030004001`
  - `1030004006`
- B 侧不得把 `title/specialties/status/payOrderId/spuId/skuId` 写成当前已经完成真实绑定。

### 3.3 给窗口 D
- D 侧 runbook / alert / gate 需要明确：
  - `[] / null` 是结构态，不是成功样本，也不是 `degraded`
  - cancel 成功后的 `refresh-once` 是前端 helper 行为，不是服务端自动重试
  - add-on 当前存在 `pseudo success / no-op risk`
  - 当前没有服务端 `degraded=true / degradeReason`
- D 侧不得把 shared chain `booking_miniapp_runtime_gate_rc=0` 写成放量依据。

### 3.4 给窗口 E
- E 侧若做样本归档或脚本核验，必须额外检查：
  - order-list 是否真的拿到 `data[]` 还是页面误按 `data.list/data.total` 读
  - order-detail / order-list 是否读到 `payOrderId`
  - order-confirm 是否真的带上合法 `spuId`
  - addon 是否带上 `skuId/spuId`
- E 侧若命中 add-on `code=0` 但订单金额为 `0` 或商品字段为空，必须标成 `pseudo success / no-op risk`，不得归档成成功样本。
