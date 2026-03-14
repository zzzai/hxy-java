# MiniApp 预约排期 PRD v1（2026-03-09）

## 0. 文档定位
- 目标：固定 booking runtime 的最终产品真值，只承认当前真实可查询能力，并把 `create / cancel / addon` 的阻断边界写死。
- 分支：`feat/ui-four-account-reconcile-ops`
- 对齐基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
  - `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`
  - `docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md`
  - `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-product-closure-v1.md`
- 本文约束：
  - 不再把旧 path、旧 method、抽象 alias page 当成产品真值。
  - 不再把 route 存在误写成“create / cancel / addon 已闭环”。
  - 不使用“已可放心放量”之类模糊表述，只使用 `Can Develop` 或 `Cannot Release`。

## 1. 当前真实可承认能力

### 1.1 查询侧 `ACTIVE`

| 页面 / 能力 | 真实 route | 真实 method + path | 当前产品结论 |
|---|---|---|---|
| 技师详情查询 | `/pages/booking/technician-detail` | `GET /booking/technician/get` | 可承认为 booking 查询侧真实能力 |
| 我的预约列表查询 | `/pages/booking/order-list` | `GET /booking/order/list` | 可承认为 booking 查询侧真实能力 |
| 预约详情查询 | `/pages/booking/order-detail` | `GET /booking/order/get` | 可承认为 booking 查询侧真实能力 |

### 1.2 仅承认 route 存在，不承认链路闭环
- `/pages/booking/technician-list`
- `/pages/booking/order-confirm`
- `/pages/booking/addon`

上述页面当前只能证明 route 与页面文件存在，不能外推为 `create / cancel / addon` 已闭环，更不能据此写成已上线能力。

## 2. Booking 最终产品阻断边界

### 2.1 为什么 `create / cancel / addon` 仍不能写成已闭环

| 能力 | 当前前端调用 | 当前后端真值 | 为什么仍不能写成已闭环 |
|---|---|---|---|
| 创建预约链路 | `GET /booking/technician/list-by-store`；`GET /booking/time-slot/list`；`POST /booking/order/create` | `GET /booking/technician/list`；`GET /booking/slot/list-by-technician`；`POST /booking/order/create` | 虽然 `POST /booking/order/create` 已对齐，但上游技师列表与技师时段仍是旧 path，整条 create 链路不能写成已闭环 |
| 取消预约 | `PUT /booking/order/cancel` | `POST /booking/order/cancel` | FE/BE method 不一致；`PUT` 不能继续当产品真值 |
| 加钟 / 升级 / 加项目 | `POST /booking/addon/create` | `POST /app-api/booking/addon/create` | path 前缀不一致；页面存在不等于 runtime 已可用 |

### 2.2 当前产品侧最终标签

| 能力 | Doc Closed | Engineering Blocked | Can Develop | Cannot Release |
|---|---|---|---|---|
| booking.create-chain | Yes | No | Yes | Yes |
| booking.cancel | Yes | No | Yes | Yes |
| booking.addon-upgrade | Yes | No | Yes | Yes |

说明：
- `Doc Closed`：本文已把最终产品真值固定，不再允许旧 path 回流到 PRD。
- `Can Develop`：产品口径已足够支撑工程改造。
- `Cannot Release`：在 runtime 样本与旧 path 清零前，不得进入灰度或放量。

## 3. 开发进入条件
以下条件同时满足后，booking 写链路才允许进入工程开发，不再被产品口径阻断：

1. 技师列表只认 `GET /booking/technician/list`。
2. 技师时段只认 `GET /booking/slot/list-by-technician`。
3. 取消预约只认 `POST /booking/order/cancel`，并按 `id + reason` 对齐。
4. 加钟 / 升级只认 `POST /app-api/booking/addon/create`。
5. `yudao-mall-uniapp/sheep/api/trade/booking.js` 与 `pages/booking/*` 不再引用：
   - `GET /booking/technician/list-by-store`
   - `GET /booking/time-slot/list`
   - `PUT /booking/order/cancel`
   - `POST /booking/addon/create`
6. capability ledger、contract、验收口径同步回填为 canonical 真值。

## 4. 放量进入条件
以下条件任一缺失，booking 写链路都只能保持 `Cannot Release`：

1. 真实联调样本可回放：
   - 创建成功
   - 时段冲突
   - 取消成功
   - 加钟成功
   - add-on 冲突失败
2. 联调抓包与代码扫描中不再出现旧 path / 旧 method。
3. 错误分支只按真实 `errorCode` 判断，不按 message 判断。
4. 查询侧与写链路的产品文档、contract、发布台账保持单一真值。
5. A 窗口重新签发 allowlist 前，不得把 booking 整域写成可放量。

## 5. 状态机映射
统一引用：`docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`

| 预约展示态 | 统一状态机映射 | 当前允许对外承认的前端动作 |
|---|---|---|
| 待支付（status=0） | `CREATED` | 查询、展示、去支付 |
| 已支付（status=1） | `PAID` | 查询、展示、查看详情 |
| 服务中（status=3） | `SERVING` | 查询、展示、联系客服 |
| 已完成（status=4） | `FINISHED` | 查询、展示、进入售后链路 |
| 已退款（status=5） | `REFUNDED` | 查询、展示退款结果 |
| 已取消（status=2） | 终止展示态 | 查询、只读展示 |

说明：
- 状态机可用于查询侧展示，不代表取消链路已对外放开。
- “看得到取消态”不等于“取消写链路已闭环”。

## 6. 错误码与恢复动作边界

| 场景 | 当前可承认错误码 / 语义 | 当前产品要求 |
|---|---|---|
| 预约详情 / 列表目标不存在 | `BOOKING_ORDER_NOT_EXISTS(1030004000)` | 展示空态或返回列表，不白屏 |
| 创建预约冲突 | `SCHEDULE_CONFLICT(1030002001)`、`TIME_SLOT_NOT_AVAILABLE(1030003001)`、`TIME_SLOT_ALREADY_BOOKED(1030003002)` | 仅作为 create 链路的工程实现依据；在 runtime 对齐前不得写成已放量稳定分支 |
| 支付结果聚合缺失 | `PAY_ORDER_NOT_FOUND` | 展示待确认并支持刷新，不误写为支付成功 |
| 工单同步异常 | 若运行侧真实暴露 warning，仅允许 warning | 当前不在本 PRD 中承诺稳定 booking 专属错误码分支，不允许伪成功 |

## 7. 产品验收口径
- [ ] 查询侧只认 `/pages/booking/technician-detail`、`/pages/booking/order-list`、`/pages/booking/order-detail` 三条真实能力。
- [ ] `create / cancel / addon` 不再沿用旧 path / method 作为产品真值。
- [ ] 没有 runtime 样本前，不得把 booking 写成“已闭环”“已可放心放量”。
- [ ] 后续若进入工程实现，只允许按 `Can Develop` 推进，不允许直接改写为 `Cannot Release=No`。
