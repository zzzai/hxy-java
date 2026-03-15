# Booking Page Smoke Design

## Goal

为 `yudao-mall-uniapp` 的 booking 页面补一层轻量级页面逻辑 smoke，确保页面主链路在不引入重型 uniapp 渲染测试的前提下，仍能稳定命中真实 booking API，并保持正确跳转与刷新行为。

## Scope

本次只覆盖 booking 页面逻辑，不改 UI 结构，不改后端接口，不回写产品文档。

覆盖页面：

- `yudao-mall-uniapp/pages/booking/technician-list.vue`
- `yudao-mall-uniapp/pages/booking/technician-detail.vue`
- `yudao-mall-uniapp/pages/booking/order-confirm.vue`
- `yudao-mall-uniapp/pages/booking/order-list.vue`
- `yudao-mall-uniapp/pages/booking/order-detail.vue`
- `yudao-mall-uniapp/pages/booking/addon.vue`

覆盖主链：

1. 技师列表拉取并跳转详情
2. 技师详情拉取详情与时段，并跳转确认页
3. 确认页提交预约成功后跳转订单详情
4. 列表页/详情页取消预约成功后刷新
5. 加钟页提交成功后跳转订单详情

## Truth Source

页面逻辑真值：

- `yudao-mall-uniapp/pages/booking/*.vue`

API 真值：

- `yudao-mall-uniapp/sheep/api/trade/booking.js`

当前 canonical 映射：

- `getTechnicianList(storeId)` -> `GET /booking/technician/list`
- `getTechnician(technicianId)` -> `GET /booking/technician/get`
- `getTimeSlots(technicianId, date)` -> `GET /booking/slot/list-by-technician`
- `createOrder(payload)` -> `POST /booking/order/create`
- `cancelOrder(id, reason)` -> `POST /booking/order/cancel`
- `createAddonOrder(payload)` -> `POST /app-api/booking/addon/create`

## Recommended Approach

采用“页面逻辑抽离 + Node smoke 测试”方案。

核心做法：

1. 把页面里与 API 调用、跳转、刷新、成功分支有关的逻辑抽成轻量 helper。
2. 页面本身继续保留现有模板和状态组织，不引入新的测试框架。
3. 测试使用 Node 内置 `node:test`，通过 stub `BookingApi`、`sheep.$router.go`、`uni.showModal` 来冻结行为真值。

推荐原因：

- 当前仓库没有成熟的 uniapp 页面渲染测试基建，直接做组件集成测试成本高、稳定性差。
- 这批真正需要守住的是“页面是否还命中真实 API / 成功后是否走对跳转和刷新”，不是视觉渲染。
- 抽离的 helper 还能为后续 booking 页面继续开发提供稳定复用点，避免页面脚本越来越厚。

## Frozen Behaviors

需要通过 smoke 冻结的行为：

1. `technician-list` 必须调用 `getTechnicianList(storeId)`，并跳 `/pages/booking/technician-detail`
2. `technician-detail` 必须调用 `getTechnician(technicianId)` 与 `getTimeSlots(technicianId, date)`，并跳 `/pages/booking/order-confirm`
3. `order-confirm` 必须调用 `createOrder({ timeSlotId, spuId?, skuId?, userRemark, dispatchMode: 1 })`
4. `order-list` 与 `order-detail` 取消成功后，仍通过 `cancelOrder(id, '用户主动取消')` 收口
5. `addon` 必须调用 `createAddonOrder({ parentOrderId, addonType })`
6. 成功跳转只认当前真实 booking 路由，不允许引入 alias route

## Non-goals

本次不覆盖：

- 组件渲染细节
- CSS 样式与布局
- 真机事件兼容
- request 底层实现
- 后端 controller / service 改造
- booking 文档回写

## Risks

1. 如果 helper 边界切得太细，会让页面脚本拆散得不自然。
2. 如果 helper 仍然耦合 `reactive` 状态或 uni 生命周期，测试价值会下降。
3. `order-confirm` 当前通过全量时段列表反查 `timeSlotId`，这是页面级 smoke 最需要重点冻结的薄弱点。

## Success Criteria

1. Node smoke 覆盖 booking 页面 5 条主链。
2. 页面主链全部只命中 canonical booking API。
3. 成功分支的跳转与刷新行为可被测试稳定验证。
4. 不引入额外测试框架，不改变页面 UI。
