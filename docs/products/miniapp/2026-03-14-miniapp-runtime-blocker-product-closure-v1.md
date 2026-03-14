# MiniApp Runtime Blocker Product Closure v1（2026-03-14）

## 0. 文档定位
- 目标：把当前仍能阻断 miniapp 开发或放量的四类能力固定为最终产品真值：
  - Booking `create / cancel / addon`
  - Member 缺页能力
  - Reserved runtime 未实现能力
  - BO-004 技师提成明细 / 计提管理
- 适用范围：`feat/ui-four-account-reconcile-ops`
- 真值输入：
  - `yudao-mall-uniapp/pages.json`
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `ruoyi-vue-pro-master/**/controller/app/*`
  - `ruoyi-vue-pro-master/**/TechnicianCommissionController.java`
  - `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md`
  - `docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md`
  - `docs/plans/2026-03-11-miniapp-member-missing-page-activation-checklist-v1.md`
  - `docs/plans/2026-03-11-miniapp-reserved-runtime-readiness-register-v1.md`
  - `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
- 本文约束：
  - 不改业务代码。
  - 不把旧 path / alias path / 原型页当成产品真值。
  - 不把“文档齐了”写成“runtime 已上线”。
  - 不使用“可放心放量”这类模糊表述，只使用 `Can Develop` 或 `Cannot Release`。
  - 若没有真实对外暴露证据，不在 PRD 中承诺稳定错误码分支。

## 1. 标签定义

| 标签 | 定义 |
|---|---|
| `Doc Closed` | 产品真值已固定，不再允许旧口径回流 |
| `Engineering Blocked` | 产品真值不足，工程无法启动 |
| `Can Develop` | 产品真值已固定，工程可以开始对齐或实现 |
| `Cannot Release` | runtime 证据不完整，当前不得灰度或放量 |

说明：
- 本批收口的目标是把产品真值全部关掉歧义，因此 blocker 项统一应达到 `Doc Closed=Yes`。
- 本批收口后，以下 blocker 项默认不再是“产品侧 Engineering Blocked”，但仍全部保持 `Cannot Release=Yes`。

## 2. Booking 最终产品阻断边界

### 2.1 当前真实可承认能力

| 页面 / 能力 | 真实 route | 真实 method + path | 当前可承认结论 |
|---|---|---|---|
| 技师详情查询 | `/pages/booking/technician-detail` | `GET /booking/technician/get` | 查询侧真实能力 |
| 我的预约列表查询 | `/pages/booking/order-list` | `GET /booking/order/list` | 查询侧真实能力 |
| 预约详情查询 | `/pages/booking/order-detail` | `GET /booking/order/get` | 查询侧真实能力 |

补充：
- `/pages/booking/technician-list`
- `/pages/booking/order-confirm`
- `/pages/booking/addon`

上述 3 个 route 只说明页面文件存在，不等于 create / cancel / addon 已闭环。

### 2.2 `create / cancel / addon` 为什么仍不能写成已闭环

| 能力 | 当前前端调用 | 当前后端真值 | 不能写成已闭环的原因 |
|---|---|---|---|
| create chain | `GET /booking/technician/list-by-store`；`GET /booking/time-slot/list`；`POST /booking/order/create` | `GET /booking/technician/list`；`GET /booking/slot/list-by-technician`；`POST /booking/order/create` | create 自身 path 对齐，但上游技师列表与技师时段仍是旧 path，整条创建链路未闭环 |
| cancel | `PUT /booking/order/cancel` | `POST /booking/order/cancel` | FE/BE method 不一致；旧 `PUT` 不能再当产品真值 |
| addon | `POST /booking/addon/create` | `POST /app-api/booking/addon/create` | path 前缀不一致，route 存在不等于 runtime 可用 |

### 2.3 开发进入条件
1. 技师列表只认 `GET /booking/technician/list`。
2. 技师时段只认 `GET /booking/slot/list-by-technician`。
3. 取消预约只认 `POST /booking/order/cancel`，并按 `id + reason` 对齐。
4. 加钟 / 升级只认 `POST /app-api/booking/addon/create`。
5. 前端 API 文件与页面代码不再出现旧 path / 旧 method。
6. booking PRD、contract、capability ledger、release gate 同步回填 canonical 真值。

### 2.4 放量进入条件
1. 创建成功、时段冲突、取消成功、add-on 成功、add-on 冲突失败样本全部可回放。
2. 抓包与代码扫描中不再出现旧 path / 旧 method。
3. 错误分支只按真实 `errorCode` 判断，不按 message 判断。
4. A 窗口重新签发 allowlist 前，booking 写链路一律保持 `Cannot Release`。

### 2.5 产品侧最终标签

| 能力 | Doc Closed | Engineering Blocked | Can Develop | Cannot Release |
|---|---|---|---|---|
| booking.create-chain | Yes | No | Yes | Yes |
| booking.cancel | Yes | No | Yes | Yes |
| booking.addon-upgrade | Yes | No | Yes | Yes |

## 3. Member 缺页能力边界

### 3.1 当前缺页能力

| 能力 | 当前 route 真值 | 当前 API 真值 | 为什么当前不能写成已上线页面 |
|---|---|---|---|
| `/pages/user/level` | 无真实页面文件、无 `pages.json` 入口 | `GET /member/level/list`；`GET /member/experience-record/page` | 只有 API，没有用户页；不能把 API 存在写成页面已上线 |
| `/pages/profile/assets` | 无真实页面文件、无 `pages.json` 入口 | `GET /member/asset-ledger/page` 仅能保留为规划接口 | 当前没有统一资产总账 runtime 页面，也没有真实对外证据支撑页面口径 |
| `/pages/user/tag` | 无真实页面文件、无 `pages.json` 入口 | 当前无 app 端正式读取接口 | 缺页面、缺入口、缺读取接口，不能写成已上线标签页 |

### 3.2 为什么当前不能写成已上线页面
1. 没有真实页面文件。
2. 没有真实入口。
3. 没有可回放的用户页样本。
4. 缺页能力不能计入发布范围、埋点范围、验收范围。

### 3.3 开发进入条件
1. 新增真实页面文件并进入 `pages.json`。
2. 从真实已发布入口可以跳转到对应页面。
3. 页面只读取正式 contract 允许的真实接口。
4. PRD、contract、field dictionary、errorcopy、capability ledger 同步回填真实 route。

### 3.4 放量进入条件
1. 正常查看、空页、接口失败、入口关闭样本全部可回放。
2. 客服、运营、前端的页面态与恢复动作统一。
3. A 窗口重新签发 allowlist 前，3 个缺页能力一律保持 `Cannot Release`。

### 3.5 产品侧最终标签

| 能力 | Doc Closed | Engineering Blocked | Can Develop | Cannot Release |
|---|---|---|---|---|
| `/pages/user/level` | Yes | No | Yes | Yes |
| `/pages/profile/assets` | Yes | No | Yes | Yes |
| `/pages/user/tag` | Yes | No | Yes | Yes |

## 4. Reserved runtime 未实现边界

### 4.1 当前真值

| capability | 当前页面真值 | 当前 app controller 真值 | 为什么规划 / 治理文档齐了也不能写成 runtime 已上线 |
|---|---|---|---|
| gift-card | 无真实 `/pages/gift-card/*` | 无真实 `/promotion/gift-card/*` app controller | 文档冻结只说明规划齐备，不代表 runtime 已实现 |
| referral | 无真实 `/pages/referral/*` | 无真实 `/promotion/referral/*` app controller | 没有页面、没有 controller、没有样本，不能写成运行上线 |
| technician-feed | 无真实 `/pages/technician/feed` | 无真实 `/booking/technician/feed/*` app controller | 只有规划与治理，不存在 runtime 页面与 app controller |

### 4.2 开发进入条件
1. 真实页面文件存在并进入 `pages.json`。
2. 真实 app controller 存在，且 method + path 与 contract 对齐。
3. 开关默认关闭态、灰度态、开启态全部写入发布口径。
4. capability ledger、release decision、PRD、contract 回填真实 route / API / 样本规则。

### 4.3 放量进入条件
1. 页面、controller、样本、五键日志全部具备。
2. `RESERVED_DISABLED` 关闭态误返回计数为 `0`。
3. gray runbook 与回滚演练通过。
4. A 窗口签发前，三项全部保持 `Cannot Release`。

### 4.4 产品侧最终标签

| 能力 | Doc Closed | Engineering Blocked | Can Develop | Cannot Release |
|---|---|---|---|---|
| gift-card | Yes | No | Yes | Yes |
| referral | Yes | No | Yes | Yes |
| technician-feed | Yes | No | Yes | Yes |

## 5. BO-004 最终产品边界

### 5.1 当前只认真实 8 条 `/booking/commission/*`
- `GET /booking/commission/list-by-technician`
- `GET /booking/commission/list-by-order`
- `GET /booking/commission/pending-amount`
- `POST /booking/commission/settle`
- `POST /booking/commission/batch-settle`
- `GET /booking/commission/config/list`
- `POST /booking/commission/config/save`
- `DELETE /booking/commission/config/delete`

### 5.2 不得把 `commission-settlement/*.vue` 反推成 BO-004 页面
- 当前只核到：
  - `commission-settlement/index.vue`
  - `commission-settlement/outbox/index.vue`
- 上述 2 个页面只属于 BO-003，不属于 BO-004。
- 当前没有核出 BO-004 独立后台页面文件，也没有核出 BO-004 独立前端 API 文件。

### 5.3 合法空态
以下全部是当前必须承认的合法空态，不得误写成失败或未实现：
- `list-by-technician => []`
- `list-by-order => []`
- `pending-amount => 0`
- `config/list => []`

### 5.4 `Boolean true` 不等于真实业务完成
以下写接口都必须写明“写后回读确认”，不能只看 `true`：
- `POST /booking/commission/settle`
- `POST /booking/commission/batch-settle`
- `POST /booking/commission/config/save`
- `DELETE /booking/commission/config/delete`

原因：
1. `settle` 对不存在或非待结算记录是 no-op，也可能返回 `true`。
2. `batch-settle` 不返回命中数、失败数、跳过数。
3. `config/save` / `config/delete` 不返回最终列表快照。

### 5.5 开发进入条件
1. 核出独立后台页面文件。
2. 核出独立前端 API 文件。
3. 页面与 API 只绑定上述 8 条真实接口。
4. 所有写动作都补齐“写后回读确认”的产品交互与验收样本。

### 5.6 放量进入条件
1. 独立页面、独立 API 文件、controller 三方映射闭合。
2. `[] / 0` 空态样本与写后回读样本全部可回放。
3. 不再把 BO-003 页面或 API 借写成 BO-004 页面已闭环。
4. A 窗口签发前，BO-004 保持 `Cannot Release`。

### 5.7 产品侧最终标签

| 能力 | Doc Closed | Engineering Blocked | Can Develop | Cannot Release |
|---|---|---|---|---|
| BO-004 8 条接口边界 | Yes | No | Yes | Yes |
| BO-004 独立后台页面文件 | Yes | No | Yes | Yes |
| BO-004 独立前端 API 文件 | Yes | No | Yes | Yes |
| BO-004 写后回读确认 | Yes | No | Yes | Yes |

## 6. 统一产品结论
1. 本批 blocker 项全部进入 `Doc Closed`，不再允许口径漂移。
2. 本批 blocker 项全部可以进入 `Can Develop`，不再被产品真值阻断。
3. 本批 blocker 项全部继续保持 `Cannot Release`，直到 runtime 证据完整。
4. 后续任一 PRD 若无真实对外暴露证据，不得承诺稳定错误码分支。
5. 后续任一窗口不得再使用“已可放心放量”之类模糊表述。
