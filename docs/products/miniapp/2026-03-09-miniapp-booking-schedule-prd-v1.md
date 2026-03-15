# MiniApp 预约排期 PRD v1（2026-03-09）

## 0. 文档定位
- 目标：基于当前分支真实页面、真实 `logic.js`、真实 API wrapper 与真实 smoke test，固定 booking runtime 的产品真值。
- 当前分支：`window-b-booking-runtime-prd-20260315`
- 真值输入：
  - `yudao-mall-uniapp/pages/booking/*.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
- 本文约束：
  - 只写真实页面、真实字段、真实 helper 行为。
  - 不再使用 `/pages/booking/schedule` 之类抽象 alias page。
  - 不把 `POST /booking/order/create` 单点存在误写成 booking 已可发布。
  - 当前 booking 只承认 `query-only active`；`create / cancel / addon` 只承认 `Can Develop`，不承认 `Can Release`。
  - 当前页面/helper 与 canonical contract 仍有漂移的字段或错误边界，统一直接写成“工程未闭环”，不得回退成旧稳定口径。

## 1. 真实页面与当前状态

| 页面 | 真实 route | 当前真实能力 | 当前真实字段 | 当前状态 |
|---|---|---|---|---|
| 技师列表 | `/pages/booking/technician-list` | `BookingApi.getTechnicianList(storeId)` 拉取技师列表，点卡片进入技师详情 | query:`storeId`；backend 稳定字段:`id`,`avatar`,`name`,`introduction`,`tags`,`rating`,`serviceCount`；页面 fallback:`title`,`specialties`,`status` | `Query-Only Active` |
| 技师详情 | `/pages/booking/technician-detail` | `BookingApi.getTechnician(id)` + `BookingApi.getTimeSlots(technicianId,date)`；选择日期、选择时段、进入确认页 | route:`id`,`storeId`；detail backend 稳定字段:`avatar`,`name`,`introduction`,`tags`,`rating`,`serviceCount`；页面 fallback:`title`；slot backend 稳定字段:`id`,`technicianId`,`technicianName`,`technicianAvatar`,`slotDate`,`startTime`,`endTime`,`isOffpeak`,`offpeakPrice`,`status`；本地日期:`date`,`label`,`weekDay` | `Query-Only Active`；下游下单入口 `Can Develop / Cannot Release` |
| 确认预约 | `/pages/booking/order-confirm` | 读取技师与时段后提交 `BookingApi.createOrder` | route:`timeSlotId`,`technicianId`,`storeId`；页面稳定可见:`slotDate`,`startTime`,`endTime`,`isOffpeak`,`offpeakPrice`,`avatar`,`name`；页面尝试读取但未闭环:`duration`,`title`,`spuId`,`skuId`；提交:`timeSlotId`,`spuId?`,`skuId?`,`userRemark`,`dispatchMode=1(helper追加)` | `Can Develop / Cannot Release` |
| 我的预约 | `/pages/booking/order-list` | `BookingApi.getOrderList` 查询；点击详情；满足条件时去支付、取消 | request:`pageNo`,`pageSize`,`status`；backend 稳定字段:`id`,`orderNo`,`status`,`servicePic`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`payPrice`；页面当前还读取 `data.list`,`data.total`,`payOrderId` | 查询 `Query-Only Active`；取消 `Can Develop / Cannot Release` |
| 预约详情 | `/pages/booking/order-detail` | `BookingApi.getOrderDetail(id)` 查询；满足条件时去支付、取消、跳转加钟页 | query:`id`；detail backend 稳定字段:`id`,`status`,`technicianAvatar`,`technicianName`,`orderNo`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`originalPrice`,`discountPrice`,`payPrice`,`userRemark`；页面当前还读取 `payOrderId` | 查询 `Query-Only Active`；取消/加钟入口 `Can Develop / Cannot Release` |
| 加钟服务 | `/pages/booking/addon` | 先用 `BookingApi.getOrderDetail(parentOrderId)` 读取母单，再提交 `BookingApi.createAddonOrder` | route:`parentOrderId`；展示:`serviceName`,`bookingStartTime`,`bookingEndTime`,`technicianName`；提交:`parentOrderId`,`addonType`；页面 `remark` 仅本地保留，当前不入请求 | `Can Develop / Cannot Release` |

## 2. `Query-Only Active` 边界
- 当前可承认的活跃边界只覆盖读取链路，不覆盖任何写入成功承诺。
- 页面级 `Query-Only Active` 范围固定为：
  - `/pages/booking/technician-list` 的技师列表查询
  - `/pages/booking/technician-detail` 的技师详情与时段查询
  - `/pages/booking/order-list` 的预约列表查询
  - `/pages/booking/order-detail` 的预约详情查询
- 以下页面不进入 `Query-Only Active` 页面清单：
  - `/pages/booking/order-confirm`
  - `/pages/booking/addon`
- 以下动作即使页面上已有按钮或 helper，也仍不属于 `Query-Only Active`：
  - 创建预约
  - 取消预约
  - 加钟 / 升级 / 加项目

## 3. 写链路边界

| 写动作 | 页面 | 当前 helper / wrapper 证据 | 当前产品结论 | 当前失败后恢复动作 |
|---|---|---|---|---|
| 创建预约 | `/pages/booking/order-confirm` | `submitBookingOrderAndGo` 只在 `code === 0` 时跳 `/pages/booking/order-detail`；smoke test 已覆盖失败不跳详情 | `Can Develop`；`Cannot Release` | 停留确认页，结束 loading；用户返回技师详情页改选时段后重提 |
| 取消预约 | `/pages/booking/order-list`、`/pages/booking/order-detail` | `cancelBookingOrderAndRefresh` 只在 `code === 0` 时刷新；默认 reason=`用户主动取消`；smoke test 已覆盖失败不刷新 | `Can Develop`；`Cannot Release` | 停留当前列表或详情，不自动刷新；用户手动刷新或稍后重试 |
| 加钟 / 升级 / 加项目 | `/pages/booking/addon` | `submitAddonOrderAndGo` 只在 `code === 0` 时跳 `/pages/booking/order-detail`；smoke test 已覆盖失败不跳详情 | `Can Develop`；`Cannot Release` | 停留加钟页，不自动跳转；用户重新选择 `addonType` 或返回详情页 |

说明：
- 当前“可开发”依据是真实 route、真实 helper、真实 wrapper、真实 smoke test 都已存在。
- 当前“不可放量”依据是：写链路只具备 helper 级防伪成功约束，还没有足够的 runtime 放量证据与当前页内显式失败分支，不得被写成 booking 整域可发布。

## 4. 产品验收与用户恢复最低约束
1. 严禁伪成功：
   - create 失败不跳详情
   - cancel 失败不刷新
   - addon 失败不跳详情
2. 只承认当前六个真实页面，不补：
   - 假 route
   - 假入口
   - 原型字段
3. 只承认当前真实请求字段：
   - create：`timeSlotId`,`spuId?`,`skuId?`,`userRemark`,`dispatchMode=1`
   - cancel：`id`,`reason`
   - addon：`parentOrderId`,`addonType`
4. 当前 booking 页面不消费 `degraded` / `degradeReason` 字段；恢复动作只按当前 helper 行为与页面结构态定义。
5. `order-confirm` 当前通过 `loadTimeSlots(technicianId, null)` 回捞时段并按 `timeSlotId` 匹配 slot，`order-list` 当前透传 `pageNo/pageSize/status`；这两项只算当前页面/helper 真值，不自动等于 formal contract 已闭环。
6. 当前可稳定写入 booking runtime page 的 errorCode 只认：
   - create：`TIME_SLOT_NOT_AVAILABLE(1030003001)`
   - cancel：`BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_CANNOT_CANCEL(1030004005)`、`BOOKING_ORDER_NOT_OWNER(1030004006)`
   - addon：`TIME_SLOT_NOT_AVAILABLE(1030003001)`、`BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_STATUS_ERROR(1030004001)`、`BOOKING_ORDER_NOT_OWNER(1030004006)`
   - `TECHNICIAN_NOT_EXISTS(1030001000)`、`TECHNICIAN_DISABLED(1030001001)`、`SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)` 当前不得写成 runtime page 稳定分支。

## 5. 同步文档
- 详细的页面成功态 / 空态 / 失败态 / 恢复动作，见：
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`
- 页面字段字典，见：
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`
- 用户结构态与恢复动作补充 PRD，见：
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md`
- Booking 运行时错误文案与恢复口径，见：
  - `docs/products/miniapp/2026-03-09-miniapp-user-facing-errorcopy-and-recovery-v1.md`
