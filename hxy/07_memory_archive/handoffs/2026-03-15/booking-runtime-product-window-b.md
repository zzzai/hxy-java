# Window B Handoff - Booking Runtime 产品验收与用户恢复 PRD 收口（2026-03-15）

## 1. 本批交付
- 分支：`window-b-booking-runtime-prd-20260315`
- 交付类型：仅产品文档与 handoff；未改业务代码、未改 overlay 页面、未动 `.codex`、未动历史 handoff、未处理无关 modified/untracked。
- 本批真实核对文件：
  - `yudao-mall-uniapp/pages/booking/*.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- 变更文件：
  1. `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`
  2. `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
  3. `docs/products/miniapp/2026-03-09-miniapp-user-facing-errorcopy-and-recovery-v1.md`
  4. `hxy/07_memory_archive/handoffs/2026-03-15/booking-runtime-product-window-b.md`

## 2. 核心收口结论
- 本批 booking runtime 真值只认 6 个真实页面：
  - `/pages/booking/technician-list`
  - `/pages/booking/technician-detail`
  - `/pages/booking/order-confirm`
  - `/pages/booking/order-list`
  - `/pages/booking/order-detail`
  - `/pages/booking/addon`
- 产品边界已固定：
  - `query-only active`：技师列表、技师详情/时段、预约列表、预约详情
  - `write-chain blocked`：create、cancel、addon
- 写链路统一标签已固定：
  - `Can Develop = Yes`
  - `Cannot Release = Yes`
- 本批不再允许以下误写：
  - 因 `POST /booking/order/create` 存在就写 booking 已可发布
  - 因页面上有取消/加钟按钮就写取消/加钟已放量
  - 因页面存在 `remark` 输入框就把 `remark` 写成 add-on 请求字段

## 3. 页面 / 字段真值摘要
- 技师列表页只认：
  - query:`storeId`
  - item:`id`,`avatar`,`name`,`title`,`rating`,`serviceCount`,`specialties`,`status`
- 技师详情页只认：
  - route:`id`,`storeId`
  - detail:`avatar`,`name`,`title`,`rating`,`serviceCount`,`introduction`
  - slot:`id`,`startTime`,`endTime`,`isOffpeak`,`status`
- 确认页只认：
  - route:`timeSlotId`,`technicianId`,`storeId`
  - submit:`timeSlotId`,`spuId?`,`skuId?`,`userRemark`
  - helper 自动补：`dispatchMode=1`
- 预约列表页只认：
  - request:`pageNo`,`pageSize`,`status`
  - item:`id`,`orderNo`,`status`,`servicePic`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`payPrice`,`payOrderId`
- 预约详情页只认：
  - query:`id`
  - detail:`id`,`status`,`technicianAvatar`,`technicianName`,`orderNo`,`serviceName`,`bookingDate`,`bookingStartTime`,`bookingEndTime`,`duration`,`originalPrice`,`discountPrice`,`payPrice`,`userRemark`,`payOrderId`
- Add-on 页只认：
  - route:`parentOrderId`
  - submit:`parentOrderId`,`addonType`
  - `remark` 当前只在页面本地，不入请求

## 4. 用户恢复动作与伪成功约束
- create：
  - 成功才跳详情
  - 失败停留确认页
  - 用户恢复动作：返回技师详情页改选时段后重提
- cancel：
  - 成功才刷新列表/详情
  - 失败不刷新
  - 用户恢复动作：手动刷新或稍后重试
- addon：
  - 成功才跳详情
  - 失败停留 add-on 页
  - 用户恢复动作：重新选择 `addonType` 后再提，或返回详情页
- 当前 booking 六页不消费 `degraded` / `degradeReason`，不允许在联调口径里补写 booking 假降级文案。

## 5. 给窗口 A / C / D 的联调注意点

### 5.1 给窗口 A
- 不得把 booking 整域写成可发布；只能写：
  - 查询侧 `active`
  - 写链 `Can Develop / Cannot Release`
- 字段注意：
  - create 只认 `timeSlotId`,`spuId?`,`skuId?`,`userRemark`，helper 自动补 `dispatchMode=1`
  - cancel 对外只认 `id`,`reason`，不要回写 `cancelReason`
  - addon 只认 `parentOrderId`,`addonType`；不要把 `remark` 写进请求体
  - technician-list 当前页面真实字段是 `title`,`specialties`,`status`，不要擅自替换成未落地字段
- 降级行为：
  - booking 当前六页没有 `degraded` 页内分支
  - 写失败时只允许停留当前页或手动刷新，不允许伪成功跳转

### 5.2 给窗口 C
- 错误码注意：
  - create 失败只按 `SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_NOT_AVAILABLE(1030003001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)` 这类真实 code 讨论恢复动作
  - 详情 / add-on 母单缺失只认 `BOOKING_ORDER_NOT_EXISTS(1030004000)`
  - cancel / addon 状态不允许只写 message，必须保留 `BOOKING_ORDER_STATUS_ERROR(1030004001)` 这类 code 驱动口径
- 字段注意：
  - `reason` 是 cancel 当前对外字段
  - add-on 当前对外字段没有 `remark`
  - 当前 booking 六页没有 `degraded` outward 字段消费
- 降级行为：
  - 若后续要新增 booking `degraded` 语义，必须先落页面实现，再改产品文档

### 5.3 给窗口 D
- 验收重点不是“写链能不能点”，而是“失败后是否仍严格阻断伪成功”。
- 必验点：
  - create 失败不跳详情
  - cancel 失败不刷新
  - addon 失败不跳详情
  - `暂无可用技师`、`该日期暂无可用时段`、`暂无预约`、`订单不存在` 按真实结构态验收
- 降级行为：
  - booking 当前页无 `degraded` 验收项
  - 若出现“失败但跳详情 / 刷新成功态”的样本，应直接判为假成功风险
