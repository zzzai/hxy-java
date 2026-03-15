# Booking API Alignment Design

## Goal
修正小程序 booking 前端与后端真实 app controller 的 method/path/参数漂移，解除 `BF-022`、`BF-023`、`BF-024` 的工程真值阻断，不改后端，不改页面交互结构。

## Scope
- 修改文件：`yudao-mall-uniapp/sheep/api/trade/booking.js`
- 验证范围：
  - 技师列表
  - 技师时间槽
  - 预约取消
  - 加钟/升级创建
- 不包含：
  - booking 页面 UI 调整
  - 后端 controller / service 改造
  - booking 文档回写

## Truth Source
- 前端真值入口：`yudao-mall-uniapp/sheep/api/trade/booking.js`
- 页面调用方：
  - `yudao-mall-uniapp/pages/booking/technician-list.vue`
  - `yudao-mall-uniapp/pages/booking/order-confirm.vue`
  - `yudao-mall-uniapp/pages/booking/technician-detail.vue`
  - `yudao-mall-uniapp/pages/booking/order-list.vue`
  - `yudao-mall-uniapp/pages/booking/order-detail.vue`
  - `yudao-mall-uniapp/pages/booking/addon.vue`
- 后端 controller 真值：
  - `AppTechnicianController`
  - `AppTimeSlotController`
  - `AppBookingOrderController`
  - `AppBookingAddonController`

## Final Mapping
- `getTechnicianList(storeId)` -> `GET /booking/technician/list?storeId=...`
- `getTimeSlots(technicianId, date)` -> `GET /booking/slot/list-by-technician?technicianId=...&date=...`
- `cancelOrder(id, reason)` -> `POST /booking/order/cancel?id=...&reason=...`
- `createAddonOrder(data)` -> `POST /app-api/booking/addon/create`

## Design Choice
采用最小修复方案：只调整 booking API wrapper 的请求配置，不改页面调用签名。

理由：
1. 页面层已经按 `storeId / technicianId / id / reason / addon data` 传入足够上下文。
2. 当前阻断来自 method/path/参数承载方式漂移，适合在 API wrapper 一层集中修复。
3. 改动面最小，最容易用行为测试冻结住，不影响既有页面状态管理。

## Risks
- 取消接口从 `PUT + body` 改为 `POST + params` 后，若页面里有隐藏依赖 `data` 结构，会暴露出来。
- 时间槽接口切到 `list-by-technician` 后，会更严格依赖 `technicianId` 和 `date` 参数。

## Verification
- 新增 booking API wrapper 行为测试，冻结 4 个请求配置。
- 运行 Node 内置测试。
- 运行 `git diff --check`、命名守卫、记忆守卫。
