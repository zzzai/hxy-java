# MiniApp Booking Route / API Truth Review v1（2026-03-10）

## 1. 目标
- 目标：基于当前分支真实前端 `booking.js`、真实后端 app controller 和既有文档口径，输出 booking 域 route/API 真值审查，识别不可冻结的 mismatch。
- 边界：仅覆盖 booking 用户侧主链路：技师列表、技师详情/时段、创建预约、取消预约、加钟/升级。

## 2. 真实代码来源
1. 前端 API：`yudao-mall-uniapp/sheep/api/trade/booking.js`
2. 后端 controller：
   - `AppTechnicianController`
   - `AppTimeSlotController`
   - `AppBookingOrderController`
   - `AppBookingAddonController`
3. 当前文档真值：
   - `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
   - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
   - `docs/contracts/2026-03-09-miniapp-addbook-conflict-spec-v1.md`

## 3. 真实 route 与页面承接

| 页面 | 真实 route | 作用 | 当前状态 |
|---|---|---|---|
| 技师列表 | `/pages/booking/technician-list` | 选择门店技师 | 页面存在 |
| 技师详情 | `/pages/booking/technician-detail` | 查看技师详情与排期 | 页面存在 |
| 下单确认 | `/pages/booking/order-confirm` | 确认时间、商品、费用 | 页面存在 |
| 预约列表 | `/pages/booking/order-list` | 查询、筛选、取消、去支付 | 页面存在 |
| 预约详情 | `/pages/booking/order-detail` | 查看预约明细 | 页面存在 |
| 加钟/升级 | `/pages/booking/addon` | 发起加钟/升级/加项目 | 页面存在 |

## 4. 前端 / 后端 / 文档真值对照

| 场景 | 前端当前 method/path | 后端当前 method/path | 当前文档口径 | 对齐结果 | 风险 |
|---|---|---|---|---|---|
| 技师列表 | `GET /booking/technician/list-by-store` | `GET /booking/technician/list` | booking PRD 仍写 `list-by-store`；release canonical 已写 `list` | 不一致 | 前端调用与 canonical contract 不一致，无法冻结为 `ACTIVE` |
| 技师详情 | `GET /booking/technician/get` | `GET /booking/technician/get` | booking PRD 与 canonical 均一致 | 一致 | 可继续作为 booking 查询链路的一部分 |
| 时段列表 | `GET /booking/time-slot/list` | `GET /booking/slot/list`; `GET /booking/slot/list-by-technician` | booking PRD 仍写 `time-slot/list`；release canonical 已写 `slot/list` / `slot/list-by-technician` | 不一致 | 前端 path 与后端/canonical 均不一致，且“门店时段”与“技师时段”语义未分清 |
| 创建预约 | `POST /booking/order/create` | `POST /booking/order/create` | booking PRD 与 canonical 一致 | 一致 | 可作为 booking 创建链路唯一已对齐写路径 |
| 查询预约详情 | `GET /booking/order/get` | `GET /booking/order/get` | booking PRD 一致 | 一致 | 查询链路可用 |
| 查询预约列表 | `GET /booking/order/list` | `GET /booking/order/list` | booking PRD 一致 | 一致 | 查询链路可用 |
| 取消预约 | `PUT /booking/order/cancel` | `POST /booking/order/cancel` | booking PRD 仍写 `PUT`；release canonical 未冻结取消接口真值 | 不一致 | method 不一致，联调脚本与前端实现存在直接冲突 |
| 加钟/升级/加项目 | `POST /booking/addon/create` | `POST /app-api/booking/addon/create` | addbook conflict spec 已写 `/app-api/booking/addon/create` | 不一致 | path 前缀不一致，且 add-on 幂等/降级规则无法正确绑定 |

## 5. 当前文档真值冲突

### 5.1 booking PRD 与代码冲突
1. `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
   - 技师列表仍写 `GET /booking/technician/list-by-store`
   - 时段列表仍写 `GET /booking/time-slot/list`
   - 取消仍写 `PUT /booking/order/cancel`
2. 上述三项均不能直接作为冻结真值继续沿用。

### 5.2 canonical release API 与真实页面命名仍有抽象层差异
1. `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md` 用 `/pages/booking/schedule` 表示“排期页”。
2. 当前真实页面承接拆成：
   - `/pages/booking/technician-detail`
   - `/pages/booking/order-confirm`
3. 这不一定是错误，但意味着后续 contract 必须把“页面意图命名”和“真实 uniapp route”分开写清楚，不能混用。

## 6. 当前风险判断
1. `booking.query` 可继续视作 `ACTIVE`
   - `GET /booking/order/list`
   - `GET /booking/order/get`
2. `booking.create` 不能冻结为 `ACTIVE`
   - 技师列表与时段列表 path 仍不一致
3. `booking.cancel` 不能冻结为 `ACTIVE`
   - 前端 `PUT`，后端 `POST`
4. `booking.addon-upgrade` 不能冻结为 `ACTIVE`
   - 前端 `/booking/addon/create`
   - 后端 `/app-api/booking/addon/create`
5. 若现在直接进入开发或联调，会把假真值继续写进 PRD、验收和脚本。

## 7. 后续由 C 窗口补契约的阻断项
1. 新增 `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`。
2. 文档必须明确 booking 用户侧 canonical `method + path`：
   - 技师列表
   - 时段列表（门店/技师两类）
   - 取消预约
   - 加钟/升级/加项目
3. contract 必须说明：
   - 当前前端实现
   - 当前后端实现
   - canonical 决策值
   - 迁移策略
   - 错误码与降级语义绑定
4. 在 C 窗口 contract 输出前，A 侧不将 `booking.create / booking.cancel / booking.addon-upgrade` 升为 `ACTIVE`。

## 8. 当前结论
1. booking 域的主问题不是“有没有文档”，而是“文档、前端、后端三方真值不一致”。
2. 查询链路可以继续保留 `ACTIVE`，但创建/取消/addon 目前都只能维持 `PLANNED_RESERVED`。
3. 本文件是 A 侧后续更新 capability ledger、coverage matrix 和 freeze review 的 booking 单一真值输入。
