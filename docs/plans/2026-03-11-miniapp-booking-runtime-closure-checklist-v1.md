# MiniApp Booking Runtime Closure Checklist v1 (2026-03-11)

## 1. 目标与适用范围
- 目标：把 Booking 域从“文档已齐但 runtime truth 漂移”收口成可执行 blocker checklist，明确哪些路径可以继续查询侧开发，哪些路径在 FE/BE 真值完全收口前必须持续 `No-Go`。
- 这份 checklist 固定三层结论：
  - `Doc Closed`：booking 文档包已闭环
  - `Can Develop`：可以继续改 FE API、补样本、补审计
  - `Cannot Release`：create/cancel/addon 在真值收口前禁止放量

## 2. 当前 blocker 快照

| 链路 | 前端当前调用 | 后端真实 controller | 当前状态 | `Doc Closed` | `Can Develop` | `Cannot Release` |
|---|---|---|---|---|---|---|
| 技师列表 | `GET /booking/technician/list-by-store` | `GET /booking/technician/list` | Drift | 是 | 是 | 是 |
| 技师详情 | `GET /booking/technician/get` | `GET /booking/technician/get` | Aligned | 是 | 是 | 否 |
| 门店时段列表 | FE 无真实门店级调用 | `GET /booking/slot/list` | BE only | 是 | 是 | 是 |
| 技师时段列表 | `GET /booking/time-slot/list` | `GET /booking/slot/list-by-technician` | Drift | 是 | 是 | 是 |
| 创建预约单 | `POST /booking/order/create` | `POST /booking/order/create` | Partial | 是 | 是 | 是 |
| 预约详情 | `GET /booking/order/get` | `GET /booking/order/get` | Aligned | 是 | 是 | 否 |
| 预约列表 | `GET /booking/order/list` | `GET /booking/order/list` | Aligned | 是 | 是 | 否 |
| 取消预约 | `PUT /booking/order/cancel` | `POST /booking/order/cancel` | Drift | 是 | 是 | 是 |
| 加钟 / 升级 | `POST /booking/addon/create` | `POST /app-api/booking/addon/create` | Drift | 是 | 是 | 是 |

## 3. 允许保留的当前 `ACTIVE` 范围
- `/pages/booking/order-list` -> `GET /booking/order/list`
- `/pages/booking/order-detail` -> `GET /booking/order/get`
- `/pages/booking/technician-detail` -> `GET /booking/technician/get`

这些链路允许继续按查询侧 `ACTIVE` 管理，但不得借此把 Booking 整域误写成完全可发布。

## 4. 最终 No-Go 条件

### 4.1 为什么 FE/BE `method + path` 未收口时禁止放量
1. 旧 path 不能命中真实 controller，发布样本会直接失真。
2. 即使 `POST /booking/order/create` 已对齐，它依赖的技师列表和时段列表仍未对齐，整条 create chain 仍不可执行。
3. `cancel` 和 `addon` 仍存在方法或 path 漂移，任何放量都会把旧调用写进 allowlist、日志和验收口径。

### 4.2 样本、验收、发布如何判失败

| 阶段 | 必须满足 | 判失败条件 |
|---|---|---|
| 样本 | 只命中 canonical `method + path` | 任一旧 path 命中 |
| 验收 | create/cancel/addon 样本齐全，且只按 `errorCode` 判定 | 只验 `create` 单点成功；按 `message` 分支；时段链路仍走旧 path |
| 发布 | FE 文件、联调包、allowlist、巡检日志中旧 path 均为 `0` | 旧 path 仍在任何一层发布口径中 |

## 5. 文档与联调必须同步改正的项

| 项目 | 当前真值 | 目标真值 | 责任方 |
|---|---|---|---|
| 技师列表 path | `/booking/technician/list-by-store` | `/booking/technician/list` | FE + C |
| 技师时段 path | `/booking/time-slot/list` | `/booking/slot/list-by-technician` | FE + C |
| 取消方法 | `PUT /booking/order/cancel` | `POST /booking/order/cancel` | FE + C |
| 取消参数名 | `cancelReason` | `reason` | FE + C |
| add-on path | `/booking/addon/create` | `/app-api/booking/addon/create` | FE + C |
| 发布口径 | “Booking 可放量” | “Booking 查询侧可继续开发，但 create/cancel/addon 仍 `Cannot Release`” | A + B + C + D |

## 6. 放量前置证据
在 Booking 域从当前 blocker 状态离开前，以下证据必须齐全：

1. FE API 文件已改为 canonical `method + path`。
2. `pages/booking/*` 真实页面入口未再引用旧路径。
3. 联调包、巡检日志和 allowlist 中不再出现：
   - `GET /booking/technician/list-by-store`
   - `GET /booking/time-slot/list`
   - `PUT /booking/order/cancel`
   - `POST /booking/addon/create`
4. capability ledger、freeze review、release decision、booking contract 同步回填。
5. 至少存在 1 轮可回放样本：
   - 技师列表成功
   - 技师时段成功
   - 创建成功
   - 创建冲突失败
   - 取消成功
   - add-on 成功
   - add-on 冲突失败
6. 错误码继续只按 `code` 分支，不按 `message` 分支。

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
- 不得把 Booking 全域写成“已对齐”。
- 不得把 `/booking/order/create` 单点对齐误写成 Booking 已闭环。
- 不得把 add-on 页存在误写成 add-on 已发布。
- 不得在 FE/BE 旧路径未移除前，把 Booking 改成 `Frozen Candidate`、`Go`、准发布范围或“可放心放量”。

## 9. 退出条件
只有同时满足以下条件，Booking 才能离开当前 blocker 状态：
1. FE/BE `method + path` 全量对齐。
2. FE 页面不再发旧路径。
3. 文档与发布口径同步更新。
4. 样本包和错误码证据完整。
5. A 窗口重新评审后，才允许从 `Still Blocked -> Ready/Frozen Candidate`。
