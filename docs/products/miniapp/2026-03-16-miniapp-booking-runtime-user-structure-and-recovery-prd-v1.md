# MiniApp Booking Runtime 用户结构态与恢复动作 PRD v1（2026-03-16）

## 0. 文档定位
- 目标：把 booking runtime 当前真实页面上的用户可见结构态、写链失败后的恢复动作、当前可引用的错误码边界固定成一份产品恢复 PRD。
- 当前分支：`window-b-booking-runtime-prd-20260315`
- 只使用以下真实输入：
  - `yudao-mall-uniapp/pages/booking/*.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
- 强约束：
  - 只按当前真实页面结构态、真实 helper 行为、当前已提交 contract 文档里的 code 名称写恢复动作。
  - 不按 message 分支。
  - 若页面字段、结构态或错误边界与 canonical contract 仍有漂移，必须直接写成“工程未闭环”，不得回退成旧稳定口径。
  - 空态 `[] / null / 0` 不是成功样本，只能写成空态或空结果。

## 1. 当前总判断
- 文档状态：`Ready`
- 查询页结构态：`Ready`
- 写链恢复动作：`Ready`
- 写链发布状态：`Still Blocked`
- 当前结论只能写成：
  - `文档已闭环`
  - `工程未闭环`
  - `可开发但不可放量`

## 2. 页面结构态矩阵

| 页面 | 当前成功态 | 当前空态 | 当前失败态 | 用户恢复动作 | 当前状态 |
|---|---|---|---|---|---|
| `/pages/booking/technician-list` | 展示技师卡片，点击进入详情 | `暂无可用技师` | 当前无独立错误 banner；失败时仍停留当前页空结构 | 重新进入当前页 | `Ready` |
| `/pages/booking/technician-detail` | 展示技师信息、日期条、时段格；选中时底部显示 `已选：...` | `该日期暂无可用时段` | 当前无独立失败文案；未选时 `立即预约` 不可点 | 切换日期重新拉取；或返回上一页重选技师 | `Ready` |
| `/pages/booking/order-confirm` | 成功提交后跳详情页 | `未核出（当前页无独立空态组件，依赖 route + 匹配到的 slot）` | 提交失败后停留确认页，loading 结束，不跳详情 | 返回技师详情页改选时段后重提 | `Still Blocked` |
| `/pages/booking/order-list` | 展示预约卡片、状态、金额；可去支付、取消 | `暂无预约` | 查询失败时停留当前页；取消失败不刷新 | 下拉刷新、切 tab、或重新进入当前页 | `Ready`（查询） / `Still Blocked`（取消） |
| `/pages/booking/order-detail` | 展示状态栏、预约信息、备注；可去支付、取消、加钟 | `订单不存在` | 查询失败时当前页只剩空态；取消失败不刷新详情 | 返回预约列表重新进入；或稍后重试 | `Ready`（查询） / `Still Blocked`（取消/加钟入口） |
| `/pages/booking/addon` | 成功提交后跳详情页 | `订单不存在` | 提交失败后停留 add-on 页，不跳详情 | 重新选择 `addonType` 再提交；或返回预约详情页 | `Still Blocked` |

## 3. 写链恢复动作矩阵

| 动作 | helper / 页面依据 | 当前用户可见结果 | 恢复动作 | 当前错误码依据 | 状态 |
|---|---|---|---|---|---|
| create | `submitBookingOrderAndGo` / `/pages/booking/order-confirm` | 失败时停留确认页，不跳详情 | 返回技师详情页改选时段后重提 | `TIME_SLOT_NOT_AVAILABLE(1030003001)` | `Still Blocked` |
| cancel | `cancelBookingOrderAndRefresh` / `/pages/booking/order-list`、`/pages/booking/order-detail` | 失败时停留当前列表或详情，不自动刷新 | 手动刷新或稍后重试 | `BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_CANNOT_CANCEL(1030004005)`、`BOOKING_ORDER_NOT_OWNER(1030004006)` | `Still Blocked` |
| addon | `submitAddonOrderAndGo` / `/pages/booking/addon` | 失败时停留 add-on 页，不跳详情 | 重新选择 `addonType`；或返回详情页 | `TIME_SLOT_NOT_AVAILABLE(1030003001)`、`BOOKING_ORDER_NOT_EXISTS(1030004000)`、`BOOKING_ORDER_STATUS_ERROR(1030004001)`、`BOOKING_ORDER_NOT_OWNER(1030004006)` | `Still Blocked` |

补充：
- `GET /booking/technician/get` 与 `GET /booking/order/get` 当前查无记录都是真实 `success(null)`，不是稳定错误码分支。
- `TECHNICIAN_NOT_EXISTS(1030001000)`、`TECHNICIAN_DISABLED(1030001001)`、`SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)` 当前都不得写成 booking runtime page 稳定分支。
- `addon` 页当前只提交 `parentOrderId`,`addonType`；`spuId/skuId` 未由页面提交，因此 `code=0` 但读后未变仍必须按 pseudo success / no-op risk 管理。

## 4. 当前真实用户文案 / 控件清单

| 页面 | 当前真实文案 / 控件 | 用途 | 备注 |
|---|---|---|---|
| `/pages/booking/technician-list` | `暂无可用技师` | 空态 | 不是成功样本 |
| `/pages/booking/technician-detail` | `选择日期`、`选择时段`、`该日期暂无可用时段`、`请选择时段`、`立即预约` | 结构态 + CTA | 未选时段时按钮不可点 |
| `/pages/booking/order-confirm` | `提交预约`、`请输入备注信息（选填）`、`闲时价` | 提交页 CTA 与展示 | 没有独立失败 banner |
| `/pages/booking/order-list` | `暂无预约`、`去支付`、`取消` | 列表空态与 CTA | `取消` 先弹确认框 |
| `/pages/booking/order-detail` | `订单不存在`、`去支付`、`取消预约`、`加钟` | 详情空态与 CTA | 状态说明随 status 切换 |
| `/pages/booking/addon` | `订单不存在`、`选择类型`、`确认加钟`、`备注（选填）` | add-on 空态与 CTA | `remark` 只在页面本地 |
| 取消确认框 | `确定要取消预约吗？` | 二次确认 | 当前 list/detail 共用同一文案 |

## 5. 当前禁止误写成成功 / ACTIVE / Release Ready 的项
1. `暂无可用技师`
   - 只能算空态，不能算查询成功样本。
2. `该日期暂无可用时段`
   - 只能算空态，不能算时段可约成功样本。
3. `暂无预约`
   - 只能算空态，不能算订单链路成功样本。
4. `订单不存在`
   - 只能算空结果或失败结构态，不能算“详情已成功加载”。
5. `提交预约 / 取消 / 确认加钟` 按钮存在
   - 只能算页面结构存在，不能算写链可放量。
6. 当前任何 `[] / null / 0`
   - 只记空态或空结果，不记成功。
7. 当前 booking 六页没有 `degraded=true / degradeReason`
   - 不得补写成用户已看到的降级提示。

## 6. 与 formal contract 的边界
1. 本文可直接冻结的内容：
   - 当前真实页面结构态
   - 当前 helper 防伪成功行为
   - 当前页面上的恢复动作
2. 本文必须继续明确“工程未闭环”的内容：
   - `technician-list` 页模板使用的 `title`,`specialties`,`status` 当前没有 backend 字段绑定
   - `order-confirm` 当前通过 `loadTimeSlots(technicianId, null)` 回捞时段，且 `duration`,`spuId`,`skuId` 没有 slot VO 闭环
   - `order-list` 当前按 `data.list/data.total` 渲染，但 backend 返回 `data[]`；`payOrderId` 也无响应绑定
   - `order-detail` 当前 `payOrderId` 无响应绑定
   - `addon` 页当前只提交 `parentOrderId`,`addonType`，没有提交 `spuId`,`skuId`
3. C 窗口吸收时：
   - 只按 code 收口
   - 不得把本文中的用户恢复动作改写成 message 分支
   - 不得把本文已明确排除的旧 code 再写回稳定 runtime page 分支

## 7. 本批验收清单
- [ ] 六个真实页面的成功态 / 空态 / 失败态 / 恢复动作均已冻结。
- [ ] `create / cancel / addon` 都明确保持 `Still Blocked`。
- [ ] 空态 `[] / null / 0` 没有被写成成功样本。
- [ ] 当前没有把 `degraded` 假想字段写进 booking 页面口径。
- [ ] 所有错误口径都能被 C 窗口按 `code` 吸收，无法稳定引用的部分都已明确排除出 runtime page 稳定分支。
