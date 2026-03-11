# MiniApp Booking Runtime Closure Checklist v1 (2026-03-11)

## 1. 目标与适用范围
- 目标：把 booking 域从“文档已齐但 runtime truth 漂移”收口成可执行的前置清单，明确哪些路径可继续保留为查询侧 `ACTIVE`，哪些路径在 FE/BE 真值完全收口前必须持续 `BLOCKED / PLANNED_RESERVED`。
- 适用范围：
  - 前端：`yudao-mall-uniapp/pages/booking/*`、`yudao-mall-uniapp/sheep/api/trade/booking.js`
  - 后端：`AppBookingOrderController`、`AppTechnicianController`、`AppTimeSlotController`、`AppBookingAddonController`
  - 文档：booking PRD、booking route truth review、booking user API alignment、capability ledger、freeze review、release decision pack
- 对齐基线：
  - `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`
  - `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md`

## 2. 当前 blocker 快照

| 链路 | 前端当前调用 | 后端真实 controller | 当前状态 | 结论 |
|---|---|---|---|---|
| 技师列表 | `GET /booking/technician/list-by-store` | `GET /booking/technician/list` | Drift | 不得升 `ACTIVE` |
| 技师详情 | `GET /booking/technician/get` | `GET /booking/technician/get` | Aligned | 可继续留在查询侧 `ACTIVE` |
| 门店时段列表 | FE 无真实门店级调用 | `GET /booking/slot/list` | BE only | 当前仅 `ACTIVE_BE_ONLY` |
| 技师时段列表 | `GET /booking/time-slot/list` | `GET /booking/slot/list-by-technician` | Drift | 不得升 `ACTIVE` |
| 创建预约单 | `POST /booking/order/create` | `POST /booking/order/create` | Partial | 上游链路未闭环前不得单独升 `ACTIVE` |
| 预约详情 | `GET /booking/order/get` | `GET /booking/order/get` | Aligned | 可继续留在查询侧 `ACTIVE` |
| 预约列表 | `GET /booking/order/list` | `GET /booking/order/list` | Aligned | 可继续留在查询侧 `ACTIVE` |
| 取消预约 | `PUT /booking/order/cancel` | `POST /booking/order/cancel` | Drift | 持续阻断 |
| 加钟 / 升级 | `POST /booking/addon/create` | `POST /app-api/booking/addon/create` | Drift | 持续阻断 |

## 3. 允许保留的当前 `ACTIVE` 范围
- `/pages/booking/order-list` -> `GET /booking/order/list`
- `/pages/booking/order-detail` -> `GET /booking/order/get`
- `/pages/booking/technician-detail` -> `GET /booking/technician/get`

这些链路允许继续按查询侧 `ACTIVE` 管理，但不得借此把 booking 整域误写成完全可发布。

## 4. 必须继续阻断的链路

| 阻断链路 | 原因 | 解除前禁止动作 |
|---|---|---|
| 技师选择链路 | 技师列表 path 漂移；时段列表 path 漂移 | 禁止升 `booking.create-chain = ACTIVE` |
| 取消预约链路 | FE `PUT` vs BE `POST` | 禁止并入 allowlist、禁止进入 Frozen Candidate |
| 加钟 / 升级链路 | FE `/booking/addon/create` vs BE `/app-api/booking/addon/create` | 禁止作为已上线能力对外表述 |
| 创建预约链路整体 | 上游技师/时段链路仍漂移 | 禁止因为 `/booking/order/create` path 对齐就单独放行 |

## 5. 文档与联调必须同步改正的项

| 项目 | 当前真值 | 目标真值 | 责任方 |
|---|---|---|---|
| 技师列表 path | `/booking/technician/list-by-store` | `/booking/technician/list` | FE + C |
| 技师时段 path | `/booking/time-slot/list` | `/booking/slot/list-by-technician` | FE + C |
| 取消方法 | `PUT /booking/order/cancel` | `POST /booking/order/cancel` | FE + C |
| 取消参数名 | `cancelReason` | `reason` | FE + C |
| add-on path | `/booking/addon/create` | `/app-api/booking/addon/create` | FE + C |
| add-on 口径 | “已可对外发布” | “仍阻断，直到 runtime truth 收口” | A + B + C |

## 6. 冻结候选前置证据
在 booking 域从 `Still Blocked` 进入 `Frozen Candidate` 前，以下证据必须齐全：

1. FE API 文件已改为 canonical `method + path`。
2. `pages/booking/*` 真实页面入口未再引用旧路径。
3. 联调包中不再出现：
   - `GET /booking/technician/list-by-store`
   - `GET /booking/time-slot/list`
   - `PUT /booking/order/cancel`
   - `POST /booking/addon/create`
4. capability ledger、freeze review、release decision、booking contract 同步回填。
5. 至少存在 1 轮可回放样本：
   - 创建成功
   - 时段冲突
   - 取消成功
   - 加钟成功
   - add-on 冲突失败
6. 错误码继续只按 code 分支，不按 message 分支。

## 7. 必测样本

| 样本 | 结果要求 |
|---|---|
| 技师列表 | 只命中 `GET /booking/technician/list` |
| 技师时段 | 只命中 `GET /booking/slot/list-by-technician` |
| 创建预约成功 | 上游路径全部 canonical，无旧路径残留 |
| 创建预约冲突 | 返回显式冲突码，不伪成功 |
| 取消预约成功 | 只命中 `POST /booking/order/cancel`，参数 `id + reason` |
| 加钟 / 升级成功 | 只命中 `POST /app-api/booking/addon/create` |
| add-on 失败 | 显式错误码阻断，不继续展示成功态 |

## 8. 明确禁止的表述
- 不得把 booking 全域写成“已对齐”。
- 不得把 `/booking/order/create` 单点对齐误写成 booking 已闭环。
- 不得把 add-on 页存在误写成 add-on 已发布。
- 不得在 FE/BE 旧路径未移除前，把 booking 改成 `Frozen Candidate`、`Go` 或准发布范围。

## 9. 退出条件
只有同时满足以下条件，booking 才能离开当前 blocker 状态：
1. FE/BE `method + path` 全量对齐。
2. FE 页面不再发旧路径。
3. 文档与发布口径同步更新。
4. 样本包和错误码证据完整。
5. A 窗口重新评审后，才允许从 `Still Blocked -> Ready/Frozen Candidate`。
