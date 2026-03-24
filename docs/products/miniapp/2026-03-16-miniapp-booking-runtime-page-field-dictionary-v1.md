# MiniApp Booking Runtime 页面字段字典 PRD v1（2026-03-16）

## 0. 文档定位
- 目标：把 booking runtime 当前六个真实页面的 route、helper、前端 API、展示字段、提交字段冻结成一份可直接被 A/C/D/E 吸收的页面字段字典。
- 当前分支：`window-b-booking-runtime-prd-20260315`
- 只使用以下真实输入：
  - `yudao-mall-uniapp/pages/booking/technician-list.vue`
  - `yudao-mall-uniapp/pages/booking/technician-detail.vue`
  - `yudao-mall-uniapp/pages/booking/order-confirm.vue`
  - `yudao-mall-uniapp/pages/booking/order-list.vue`
  - `yudao-mall-uniapp/pages/booking/order-detail.vue`
  - `yudao-mall-uniapp/pages/booking/addon.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
- 强约束：
  - 只认当前真实页面、真实 helper、真实前端 API，不用原型 alias 或历史 route。
  - 若页面读写字段与 canonical contract 仍有漂移，必须直接写明 drift，不得再把旧字段口径写成 `Pending formal contract truth`。
  - `query-only active` 不等于 `Release Ready`；写链 `create / cancel / addon` 继续固定为 `Can Develop / Cannot Release`。

## 1. 当前总判断
- 文档状态：`Ready`
- 查询页产品真值：`Ready`
- 写链产品真值：`Still Blocked`
- 当前只固定“页面字段真值已闭环”，不等于“工程放量已闭环”。

## 2. 页面到 helper / API / 字段映射

| 页面 | 当前真实 helper | 当前真实前端 API | route / query 真值 | 提交 / 请求真值 | 展示字段真值 | contract 同步状态 |
|---|---|---|---|---|---|---|
| `/pages/booking/technician-list` | `loadTechnicianList` | `BookingApi.getTechnicianList` | query:`storeId` | `GET /booking/technician/list?storeId=` | backend 稳定字段：`id`,`avatar`,`name`,`introduction`,`tags`,`rating`,`serviceCount`；页面 fallback：`title`,`specialties`,`status` 当前无 backend 绑定 | `已与 03-15 canonical matrix 收口；字段仍未闭环` |
| `/pages/booking/technician-detail` | `loadTechnicianDetail`、`loadTimeSlots` | `BookingApi.getTechnician`、`BookingApi.getTimeSlots` | route:`id`,`storeId`；本地日期:`date`,`label`,`weekDay` | `GET /booking/technician/get?id=`；`GET /booking/slot/list-by-technician?technicianId=&date=` | 技师 backend 稳定字段：`avatar`,`name`,`introduction`,`tags`,`rating`,`serviceCount`；页面 fallback：`title`；时段 backend 稳定字段：`id`,`technicianId`,`technicianName`,`technicianAvatar`,`slotDate`,`startTime`,`endTime`,`isOffpeak`,`offpeakPrice`,`status` | `已与 03-15 canonical matrix 收口；title/duration 等仍未闭环` |
| `/pages/booking/order-confirm` | `loadTechnicianDetail`、`loadTimeSlotDetail`、`submitBookingOrderAndGo` | `BookingApi.getTechnician`、`BookingApi.getTimeSlot`、`BookingApi.createOrder` | route:`timeSlotId`,`technicianId`,`storeId` | 查询：`GET /booking/slot/get?id=`；提交：`timeSlotId`,`spuId?`,`skuId?`,`userRemark`,`dispatchMode=1(helper追加)` | 页面稳定可见：`slotDate`,`startTime`,`endTime`,`duration`,`isOffpeak`,`offpeakPrice`,`avatar`,`name`；页面 fallback：`title`；页面仍缺真实商品来源：`spuId`,`skuId` | `03-24 已把 slot 回捞替换为 slot 详情读取；商品来源仍未闭环` |
| `/pages/booking/order-list` | `cancelBookingOrderAndRefresh`、`goToOrderDetail` | `BookingApi.getOrderList`、`BookingApi.cancelOrder` | query:`type?` 只驱动本地 tab；请求透传：`pageNo`,`pageSize`,`status` | 列表请求：`GET /booking/order/list`；取消请求：`POST /booking/order/cancel?id=&reason=` | backend 稳定字段：`id`,`orderNo`,`status`,`servicePic`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`payPrice`,`payOrderId`,`timeSlotId`,`spuId`,`skuId` | `03-24 已完成分页结构与支付字段闭环` |
| `/pages/booking/order-detail` | `cancelBookingOrderAndRefresh` | `BookingApi.getOrderDetail`、`BookingApi.cancelOrder` | query:`id` | 详情：`GET /booking/order/get?id=`；取消：`POST /booking/order/cancel?id=&reason=`；跳转 add-on：`parentOrderId` | backend 稳定字段：`id`,`status`,`technicianAvatar`,`technicianName`,`orderNo`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`originalPrice`,`discountPrice`,`payPrice`,`payOrderId`,`timeSlotId`,`spuId`,`skuId`,`userRemark` | `03-24 已完成 payOrderId/订单绑定字段闭环；addon 提交仍阻断` |
| `/pages/booking/addon` | `submitAddonOrderAndGo` | `BookingApi.getOrderDetail`、`BookingApi.createAddonOrder` | route:`parentOrderId` | 读取母单：`GET /booking/order/get?id=`；提交：`parentOrderId`,`addonType` | `serviceName`,`bookingStartTime`,`bookingEndTime`,`technicianName`；本地字段：`remark` | `已与 03-15 canonical matrix 收口；submit 字段仍未闭环` |

## 3. 页面级字段字典

### 3.1 技师列表页
- 页面边界：
  - 只负责“列出技师并进入技师详情”。
  - 当前没有筛选器、没有排序器、没有支付或写操作。
- 关键字段：
  - 请求键：`storeId`
  - 列表主键：`id`
  - backend 稳定字段：`avatar`,`name`,`introduction`,`tags`,`rating`,`serviceCount`
  - 页面 fallback：`title`,`specialties`,`status`
- 用户可见结构态：
  - 空态：`暂无可用技师`
- 当前状态：
  - 页面文档：`Ready`
  - 工程放量：`不可直接判为 Release Ready`

### 3.2 技师详情页
- 页面边界：
  - 只负责“展示技师详情 + 展示 7 天日期条 + 选择时段”。
  - 当前不直接提交预约，只跳确认页。
- 关键字段：
  - route：`id`,`storeId`
  - 技师详情 backend 稳定字段：`avatar`,`name`,`introduction`,`tags`,`rating`,`serviceCount`
  - 页面 fallback：`title`
  - 日期条本地字段：`date`,`label`,`weekDay`
  - 时段 backend 稳定字段：`id`,`technicianId`,`technicianName`,`technicianAvatar`,`slotDate`,`startTime`,`endTime`,`isOffpeak`,`offpeakPrice`,`status`
- 用户可见结构态：
  - 空态：`该日期暂无可用时段`
  - 未选择态：`请选择时段`
- 当前状态：
  - 页面文档：`Ready`
  - 下游预约动作：`Still Blocked`

### 3.3 确认预约页
- 页面边界：
  - 只负责“展示已选技师/时段信息 + 提交预约”。
  - 当前没有独立失败文案池。
- 关键字段：
  - route：`timeSlotId`,`technicianId`,`storeId`
  - 页面稳定可见：`slotDate`,`startTime`,`endTime`,`duration`,`isOffpeak`,`offpeakPrice`,`avatar`,`name`
  - 页面 fallback / 未闭环：`title`,`spuId`,`skuId`
  - 提交：`timeSlotId`,`spuId?`,`skuId?`,`userRemark`
  - helper 自动补：`dispatchMode=1`
- 特别说明：
  - 页面当前通过 `GET /booking/slot/get?id=` 单点读取已选时段。
  - `duration` 已由 slot detail 真实返回并进入页面闭环。
  - 当前仍没有真实商品选择入口为 create 提供稳定 `spuId/skuId` 来源。
- 当前状态：
  - 页面文档：`Ready`
  - 能力状态：`Still Blocked`

### 3.4 我的预约页
- 页面边界：
  - 只负责“查询列表、切 tab、进入详情、去支付、发起取消”。
  - tab 状态当前只作为前端本地筛选意图，不得直接写成后端已承诺支持同等筛选。
- 关键字段：
  - 本地分页：`pageNo`,`pageSize`
  - 本地 tab 映射：`status`
  - backend 稳定字段：`id`,`orderNo`,`status`,`servicePic`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`payPrice`,`payOrderId`,`timeSlotId`,`spuId`,`skuId`
  - 取消请求键：`id`,`reason`
- 特别说明：
  - 当前页面透传 `pageNo/pageSize/status`，controller 已真实消费并返回 `PageResult<{list,total}>`。
  - `payOrderId` 已有真实响应绑定，“去支付”分支不再是前端假设字段。
- 当前状态：
  - 列表查询：`Ready`
  - 取消动作：`Still Blocked`

### 3.5 预约详情页
- 页面边界：
  - 只负责“查单详情、去支付、取消、进入 add-on 页”。
  - 当前没有读后校验页内提示，失败恢复动作只按 helper 行为定义。
- 关键字段：
  - query：`id`
  - backend 稳定字段：`id`,`status`,`technicianAvatar`,`technicianName`,`orderNo`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`originalPrice`,`discountPrice`,`payPrice`,`payOrderId`,`timeSlotId`,`spuId`,`skuId`,`userRemark`
  - add-on 跳转键：`parentOrderId`
- 用户可见结构态：
  - 空态：`订单不存在`
- 当前状态：
  - 详情查询：`Ready`
  - 取消 / 加钟入口：`Still Blocked`

### 3.6 Add-on 页
- 页面边界：
  - 只负责“读取母单 + 选择 add-on 类型 + 提交 add-on”。
  - 当前 `remark` 只存在于页面本地输入框，不入请求体。
- 关键字段：
  - route：`parentOrderId`
  - 母单展示：`serviceName`,`bookingStartTime`,`bookingEndTime`,`technicianName`
  - 提交：`parentOrderId`,`addonType`
  - 本地：`remark`
- 特别说明：
  - controller 支持 `spuId`,`skuId`，但当前页面并未提交；因此 `upgrade / add-item` 路径仍存在 pseudo success / no-op risk。
- 当前状态：
  - 页面文档：`Ready`
  - 能力状态：`Still Blocked`

## 4. 当前禁止误写的口径
1. 不得把“页面存在”写成“能力已放量”。
2. 不得把“controller 在 03-10 contract 中存在”写成“产品已上线”。
3. 不得把 `query-only active` 写成 `Release Ready`。
4. 不得把 `/pages/booking/addon` 的 `remark` 写成当前真实请求字段。
5. 不得把 `[] / null / 0` 写成成功样本。
6. 不得把 `title/specialties/status`、`spuId/skuId` 商品来源缺失、或 addon pseudo success 风险写成“字段已闭环”或 “Release Ready”。

## 5. 本批验收清单
- [ ] 六个真实页面的 route、helper、前端 API、展示字段、提交字段均已冻结。
- [ ] `Query-Only Active` 与 `Still Blocked` 的页面边界已拆开。
- [ ] 所有字段漂移都已按 03-15 canonical matrix 写实，不再保留旧稳定口径。
- [ ] 没有把 `remark`、空态、按钮存在、controller 存在误写成放量证据。
