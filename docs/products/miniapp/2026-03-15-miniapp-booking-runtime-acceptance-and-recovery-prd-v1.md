# MiniApp Booking Runtime 产品验收与用户恢复 PRD v1（2026-03-15）

## 0. 文档定位
- 目标：把 booking runtime 当前真实页面、真实字段、真实 helper 行为和真实 smoke test 固定成产品验收与用户恢复单一真值。
- 当前分支：`window-b-booking-runtime-prd-20260315`
- 只使用以下真实输入：
  - `yudao-mall-uniapp/pages/booking/technician-list.vue`
  - `yudao-mall-uniapp/pages/booking/technician-detail.vue`
  - `yudao-mall-uniapp/pages/booking/service-select.vue`
  - `yudao-mall-uniapp/pages/booking/order-confirm.vue`
  - `yudao-mall-uniapp/pages/booking/order-list.vue`
  - `yudao-mall-uniapp/pages/booking/order-detail.vue`
  - `yudao-mall-uniapp/pages/booking/addon.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
- 本文约束：
  - 只写真实页面，不写 `/pages/booking/schedule`、假入口、原型页。
  - 只写当前页面真实读写字段，不补 contract 侧尚未落到页面的字段。
  - 不把 `POST /booking/order/create` 单点存在误写成 booking 全域已可发布。
  - 当前 booking 的产品边界固定为：`query-only active`，`write-chain blocked`。
  - 当前 booking 写链已完成商品来源真值收口，但真实发布样本与 gray / rollback / sign-off 仍未闭环，不得把旧 code/字段继续写成稳定分支。

## 1. Booking 真实页面清单与当前状态

| 页面 | 当前真实能力 | 当前真实字段 | 当前状态 |
|---|---|---|---|
| `/pages/booking/technician-list` | `loadTechnicianList -> BookingApi.getTechnicianList(storeId)`；点击卡片进入技师详情页 | query:`storeId`；backend 稳定字段:`id`,`avatar`,`name`,`introduction`,`tags`,`rating`,`serviceCount`；页面 fallback:`title`,`specialties`,`status` | `Query-Only Active` |
| `/pages/booking/technician-detail` | `loadTechnicianDetail -> BookingApi.getTechnician(id)`；`loadTimeSlots -> BookingApi.getTimeSlots(technicianId,date)`；选择时段后进入服务选择页 | route:`id`,`storeId`；detail backend 稳定字段:`avatar`,`name`,`introduction`,`tags`,`rating`,`serviceCount`；页面 fallback:`title`；slot backend 稳定字段:`id`,`technicianId`,`technicianName`,`technicianAvatar`,`slotDate`,`startTime`,`endTime`,`isOffpeak`,`offpeakPrice`,`status`；本地日期:`date`,`label`,`weekDay` | `Query-Only Active`；下游提交入口 `Write-Chain Blocked` |
| `/pages/booking/service-select` | `SpuApi.getSpuPage` 拉服务列表；`SpuApi.getSpuDetail` + SKU 选择后把显式 `spuId/skuId` 回传给下游写链页面 | route:`flow`,`timeSlotId?`,`technicianId?`,`storeId?`,`parentOrderId?`,`addonType?`；列表字段:`id`,`name`,`introduction`,`picUrl`,`price`；详情字段:`id`,`name`,`skus[]` | `Can Develop / Cannot Release` |
| `/pages/booking/order-confirm` | 读取技师详情、读取时段详情、读取服务商品后；`submitBookingOrderAndGo` 提交预约 | route:`timeSlotId`,`technicianId`,`storeId`,`spuId`,`skuId`；页面稳定可见:`slotDate`,`startTime`,`endTime`,`duration`,`isOffpeak`,`offpeakPrice`,`avatar`,`name`,`serviceName`；submit:`timeSlotId`,`spuId`,`skuId`,`userRemark`,`dispatchMode=1` | `Can Develop / Cannot Release` |
| `/pages/booking/order-list` | `BookingApi.getOrderList` 查询预约；可进入详情；状态为 `0/1` 时可取消；状态为 `0` 且页面读到 `payOrderId` 时可去支付 | request:`pageNo`,`pageSize`,`status`；backend 稳定字段:`id`,`orderNo`,`status`,`servicePic`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`payPrice`；页面当前还读取 `data.list`,`data.total`,`payOrderId` | 查询 `Query-Only Active`；取消 `Write-Chain Blocked` |
| `/pages/booking/order-detail` | `BookingApi.getOrderDetail(id)` 查询详情；状态为 `0` 时可去支付；状态为 `0/1` 时可取消；状态为 `3` 时可进加钟页 | query:`id`；detail backend 稳定字段:`id`,`status`,`technicianAvatar`,`technicianName`,`orderNo`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`originalPrice`,`discountPrice`,`payPrice`,`userRemark`；页面当前还读取 `payOrderId` | 查询 `Query-Only Active`；取消/加钟入口 `Write-Chain Blocked` |
| `/pages/booking/addon` | `BookingApi.getOrderDetail(parentOrderId)` 读取母单；`addonType=1` 可沿用母单商品；`addonType=2/3` 先去服务选择页，之后 `submitAddonOrderAndGo` 提交 add-on 订单 | route:`parentOrderId`,`addonType?`,`spuId?`,`skuId?`；display:`serviceName`,`bookingStartTime`,`bookingEndTime`,`technicianName`,`selectedServiceName`；submit:`parentOrderId`,`addonType`,`spuId`,`skuId`；本地字段:`remark` 仅用于输入框，不入请求 | `Can Develop / Cannot Release` |

## 2. 产品边界

### 2.1 `Query-Only Active`
- 当前仅把“查询得到结构化页面内容”视为 active。
- `Query-Only Active` 页面边界固定为：
  - `/pages/booking/technician-list`
  - `/pages/booking/technician-detail`
  - `/pages/booking/order-list`
  - `/pages/booking/order-detail`
- 在上述页面内，只以下能力属于 active：
  - 技师列表查询
  - 技师详情查询
  - 技师时段查询
  - 我的预约列表查询
  - 预约详情查询
- 以下 route 不在 `Query-Only Active` 页面清单中：
  - `/pages/booking/service-select`
  - `/pages/booking/order-confirm`
  - `/pages/booking/addon`
- 以下 CTA 在页面上真实存在，但不因存在就进入 active：
  - 提交预约
  - 取消预约
  - 加钟 / 升级 / 加项目

### 2.2 `Write-Chain Blocked`

| 动作 | 真实页面 | 当前真实依据 | 当前可开发 | 当前不可放量 | 原因 |
|---|---|---|---|---|---|
| create | `/pages/booking/service-select`、`/pages/booking/order-confirm` | `service-select -> order-confirm` + `buildBookingCreatePayload` + smoke test | 是 | 是 | 当前只证明显式商品来源闭环、helper 成功才跳详情；仍不足以把 booking 写链整体写成可放量 |
| cancel | `/pages/booking/order-list`、`/pages/booking/order-detail` | `cancelBookingOrderAndRefresh` + smoke test | 是 | 是 | 当前只证明 helper 成功才刷新、失败不刷新；不能据此承诺 runtime 放量 |
| addon | `/pages/booking/service-select`、`/pages/booking/addon` | `buildBookingAddonPayload` + `submitAddonOrderAndGo` + smoke test | 是 | 是 | 当前只证明 `addonType=1/2/3` 商品来源规则和 helper 跳转边界；不能据此承诺 runtime 放量 |

## 3. 用户可见成功态 / 空态 / 失败态与恢复动作

| 页面 | 成功态 | 空态 | 失败态 | 用户恢复动作 |
|---|---|---|---|---|
| `/pages/booking/technician-list` | 展示技师卡片；可见 `name/title/rating/serviceCount/specialties` | `暂无可用技师` | 当前无独立失败文案；请求失败后停留当前页，列表保持空结构 | 重新进入当前页 |
| `/pages/booking/technician-detail` | 展示技师信息、7 天日期条、可选时段；选中后底部显示 `已选：...` | `该日期暂无可用时段` | 当前无独立失败文案；加载失败时停留当前页，未选中时“立即预约”保持不可点 | 切换日期重新拉取时段；或返回上一页重选技师 |
| `/pages/booking/service-select` | 展示服务列表，选中 SKU 后可确认回跳下游页面 | `暂无可选服务项目` | 服务详情或 SKU 选择失败时停留当前页，不进入下游写链页 | 重新选择服务项目 / 规格；或返回上一页 |
| `/pages/booking/order-confirm` | 提交成功后直接跳 `/pages/booking/order-detail?id={orderId}` | 当前无独立空态组件；依赖 route 中显式 `spuId/skuId` 与 `timeSlotId` | 提交失败后停留确认页，停止 loading，不跳详情 | 返回服务选择页重选项目/规格后重新提交 |
| `/pages/booking/order-list` | 展示预约卡片、状态文案、价格；可进入详情；状态满足条件时显示“去支付”“取消” | `暂无预约` | 当前无独立失败文案；查询失败时停留当前页，保留既有列表或空结构；取消失败不刷新 | 下拉刷新、切换 tab、或重新进入当前页 |
| `/pages/booking/order-detail` | 展示状态栏、技师信息、预约信息、备注；状态满足条件时显示“去支付”“取消预约”“加钟” | `订单不存在` | 查询失败时当前可见结果仍为 `订单不存在`；取消失败不刷新详情 | 返回预约列表后重新进入；或稍后重试当前动作 |
| `/pages/booking/addon` | 提交成功后直接跳 `/pages/booking/order-detail?id={orderId}` | `订单不存在` | 提交失败后停留加钟页，不跳详情 | `addonType=2/3` 返回服务选择页补选项目；或返回预约详情页 |

补充：
- 当前七页都没有 `degraded` / `degradeReason` 页面分支。
- 当前七页都没有“写成功但读后未确认”的伪成功兜底文案；用户恢复动作只按当前 helper 行为定义。

## 4. 严禁伪成功的行为约束
1. `create` 失败不跳详情。
   - `submitBookingOrderAndGo` 仅在 `result.code === 0` 时调用 `goToOrderDetail`。
2. `cancel` 失败不刷新。
   - `cancelBookingOrderAndRefresh` 仅在 `result.code === 0` 时调用 `onSuccess`。
3. `addon` 失败不跳详情。
   - `submitAddonOrderAndGo` 仅在 `result.code === 0` 时调用 `goToOrderDetail`。
4. `booking-page-smoke.test.mjs` 已覆盖：
   - create 成功跳详情 / 失败不跳详情
   - cancel 成功刷新 / 失败不刷新
   - addon 成功跳详情 / 失败不跳详情
5. 产品文档不得再出现以下伪成功口径：
   - “create 已上线”
   - “cancel 已放量”
   - “addon 已稳定可用”
   - “booking 已整体可发布”

## 5. 当前真实字段与恢复动作的特别说明
1. `order-confirm`
   - 真实提交字段只有 `timeSlotId`,`spuId`,`skuId`,`userRemark`，并由 helper 自动补 `dispatchMode=1`。
   - 当前 `storeId` 只作为 route 入参存在，不是提交字段。
   - 当前页面真实执行的是 `GET /booking/slot/get?id=` 读取时段详情，并通过 `GET /product/spu/get-detail?id=` 补齐服务商品名称。
   - 商品来源当前只认服务选择页回传的显式 `spuId`,`skuId`，不再允许从 slot 猜测。
2. `order-list`
   - 页面当前透传 `pageNo`,`pageSize`,`status`，并按 `data.list`、`data.total` 渲染。
   - controller 当前真实返回 `PageResult<{list,total}>`，前端分页渲染链已闭环。
   - `payOrderId` 当前已有真实响应绑定，“去支付”不再是页面假设字段。
   - 取消确认弹窗真实文案固定为：`确定要取消预约吗？`
3. `order-detail`
   - 状态文案真实值固定为：`待支付 / 已支付 / 已取消 / 服务中 / 已完成 / 已退款`
   - 状态说明真实值固定为：`请在15分钟内完成支付 / 等待技师开始服务 / 订单已取消 / 技师正在为您服务 / 服务已完成，感谢您的信任 / 退款已处理`
   - `GET /booking/order/get` 当前 miss 是 `success(null)`；`payOrderId` 已有真实响应绑定。
4. `addon`
   - 页面虽然有 `remark` 输入框，但当前 helper 与 wrapper 仍不提交 `remark`。
   - 产品文档不得把 `remark` 写成当前 add-on 请求字段。
   - 当前页面真实提交 `parentOrderId`,`addonType`,`spuId`,`skuId`；其中 `addonType=1` 可沿用母单商品，`addonType=2/3` 必须先经服务选择页回传显式商品来源。
5. 当前可稳定引用的 runtime page errorCode 只认：
   - create：`TIME_SLOT_NOT_AVAILABLE(1030003001)`
   - cancel：`BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_CANNOT_CANCEL(1030004005)`、`BOOKING_ORDER_NOT_OWNER(1030004006)`
   - addon：`TIME_SLOT_NOT_AVAILABLE(1030003001)`、`BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_STATUS_ERROR(1030004001)`、`BOOKING_ORDER_NOT_OWNER(1030004006)`
   - `TECHNICIAN_NOT_EXISTS(1030001000)`、`TECHNICIAN_DISABLED(1030001001)`、`SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)` 当前都不得写成 booking runtime page 稳定分支。

## 6. 同步补充文档
- 页面字段真值，见：
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`
- 用户结构态与恢复动作，见：
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md`

## 7. 本批产品验收清单
- [ ] 只写七个真实 booking 页面，不写 alias route。
- [ ] 每页状态、字段、用户恢复动作都来自当前页面与 helper。
- [ ] `query-only active` 与 `write-chain blocked` 边界已明确分开。
- [ ] `create / cancel / addon` 已明确标记为 `Can Develop / Cannot Release`。
- [ ] `create` 失败不跳详情、`cancel` 失败不刷新、`addon` 失败不跳详情 已写入正式约束。
- [ ] 未补原型字段、未补假入口、未把 booking 整域误写成可发布。
