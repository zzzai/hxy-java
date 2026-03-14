# MiniApp Runtime Blocker Contract Closure v1 (2026-03-14)

## 1. 目标与真值来源
- 目标：把当前仍阻断 miniapp 开发/放量的 contract 风险一次性固定为单一真值，避免“文档已齐”被误写成“runtime 已证明”。
- 本文只认当前分支的真实证据：
  - 前端 API：`yudao-mall-uniapp/sheep/api/trade/booking.js`
  - uniapp 页面与 route：`yudao-mall-uniapp/pages.json`、`yudao-mall-uniapp/pages/**`
  - booking app controller：`AppTechnicianController`、`AppTimeSlotController`、`AppBookingOrderController`、`AppBookingAddonController`
  - member app controller：`AppAuthController`、`AppMemberLevelController`、`AppMemberExperienceRecordController`、`AppMemberSignIn*Controller`、`AppAddressController`、`AppMemberUserController`
  - admin controller：`TechnicianCommissionController`
  - 已正式提交文档：
    - `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
    - `docs/contracts/2026-03-10-miniapp-member-domain-contract-v1.md`
    - `docs/contracts/2026-03-10-miniapp-active-vs-planned-api-matrix-v1.md`
    - `docs/contracts/2026-03-14-miniapp-finance-ops-technician-commission-admin-contract-v1.md`
    - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md`
    - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`

## 2. 固定判定词
- `Doc Closed`
  - 相关 PRD / review / ledger / contract 文档已把阻断边界写清，不再允许模糊描述。
- `Contract Closed`
  - canonical `method + path + request/response` 或明确的 `N/A` 边界已冻结。
- `Runtime Not Proven`
  - 仍缺真实页面、真实前端绑定、真实 controller、真实对外行为样本中的至少一项。
- `Release Blocked`
  - 当前不得升 `ACTIVE`，不得进入 allowlist、页面验收完成口径或放量口径。

## 3. Booking canonical method/path 阻断

### 3.1 FE / BE 真实不一致项

| 阻断项 | FE 真值 | BE 真值 | 禁止再使用的旧 path | 仍 blocked 的能力 | Doc Closed | Contract Closed | Runtime Not Proven | Release Blocked |
|---|---|---|---|---|---|---|---|---|
| 技师列表 | `GET /booking/technician/list-by-store` | `GET /booking/technician/list` | `GET /booking/technician/list-by-store` | `/pages/booking/technician-list`、`booking.create` 上游技师选择 | `Yes` | `Yes` | `Yes` | `Yes` |
| 技师时段列表 | `GET /booking/time-slot/list` | `GET /booking/slot/list-by-technician` | `GET /booking/time-slot/list` | `/pages/booking/technician-detail` 的时段选择、`booking.create` 上游时段选择 | `Yes` | `Yes` | `Yes` | `Yes` |
| 取消预约 | `PUT /booking/order/cancel` + body:`id`,`cancelReason` | `POST /booking/order/cancel` + query:`id`,`reason?` | `PUT /booking/order/cancel` | `/pages/booking/order-list`、`/pages/booking/order-detail` 的取消能力 | `Yes` | `Yes` | `Yes` | `Yes` |
| 加钟 / 升级 / 加项目 | `POST /booking/addon/create` | `POST /app-api/booking/addon/create` | `POST /booking/addon/create` | `/pages/booking/addon` | `Yes` | `Yes` | `Yes` | `Yes` |

### 3.2 仍不得升 `ACTIVE` 的 Booking 能力

| 能力 | canonical contract | 当前固定结论 | Doc Closed | Contract Closed | Runtime Not Proven | Release Blocked |
|---|---|---|---|---|---|---|
| `booking.technician-select` | `GET /booking/technician/list` | 旧 path 仍在 FE 文件中，技师选择链路不得升 `ACTIVE` | `Yes` | `Yes` | `Yes` | `Yes` |
| `booking.slot-select` | `GET /booking/slot/list-by-technician` | 旧 path 仍在 FE 文件中，时段链路不得升 `ACTIVE` | `Yes` | `Yes` | `Yes` | `Yes` |
| `booking.create-chain` | `GET /booking/technician/list` + `GET /booking/slot/list-by-technician` + `POST /booking/order/create` | `POST /booking/order/create` 单点对齐，不等于整条 create 链路已证明 | `Yes` | `Yes` | `Yes` | `Yes` |
| `booking.cancel` | `POST /booking/order/cancel` | FE method/body 仍漂移，取消能力不得升 `ACTIVE` | `Yes` | `Yes` | `Yes` | `Yes` |
| `booking.addon` | `POST /app-api/booking/addon/create` | FE 缺 `/app-api` 前缀，add-on 能力不得升 `ACTIVE` | `Yes` | `Yes` | `Yes` | `Yes` |

说明：
- `GET /booking/order/get`、`GET /booking/order/list` 当前继续属于查询侧 `ACTIVE`，不在本批 blocker 之内。
- `Booking` 旧 path 必须继续作为显式阻断项保留；文档完整不构成移除 blocker 的理由。

## 4. Member 缺页能力契约边界

| 缺页能力 | 真实页面 / route 真值 | 仍可保留的接口 / 字段 | 明确禁止误写为 `ACTIVE page capability` 的内容 | Doc Closed | Contract Closed | Runtime Not Proven | Release Blocked |
|---|---|---|---|---|---|---|---|
| 等级页 `/pages/user/level` | 当前无 `pages/user/level.vue`，`pages.json` 也无入口 | `GET /member/level/list`；`GET /member/experience-record/page`；字段 `list[]:{name,level,experience,discountPercent,icon,backgroundUrl}`、`list[]:{title,experience,description,createTime}`、`total` | “等级页已上线”“等级页 capability 已闭环” | `Yes` | `Yes` | `Yes` | `Yes` |
| 统一资产总账 `/pages/profile/assets` | 当前无 `pages/profile/assets.vue`，`pages.json` 也无入口 | `GET /member/asset-ledger/page` 预留契约；字段 `list[]:{ledgerId,assetType,bizType,amount,balanceAfter,sourceBizNo,runId}`、`total`、`degraded`、`degradeReason?` | “资产总账页面已上线”“这些字段已是稳定 runtime page 字段” | `Yes` | `Yes` | `Yes` | `Yes` |
| 标签页 `/pages/user/tag` | 当前无 `pages/user/tag.vue`，也无 app 端读取 controller | `N/A`；当前没有稳定 app 标签读取字段真值 | “标签页已上线”“标签 app API 已可发布” | `Yes` | `Yes` | `Yes` | `Yes` |

固定结论：
- 页面未实现，不等于 capability 已闭环。
- `/member/level/list`、`/member/experience-record/page` 只能写成 `API/controller truth`，不能反推为 active page capability。
- `/member/asset-ledger/page` 及其 `degraded / degradeReason` 字段只算预留契约，不算当前 runtime page 证据。

## 5. Reserved runtime gate 边界

固定分层：
- 规划 / 治理层：
  - PRD、domain contract、`RESERVED_DISABLED` gate spec、switchKey、灰度 runbook、errorcode register。
- runtime capability 层：
  - 真实页面、真实 pageRoute、真实前端绑定、真实 app controller、真实对外行为样本。
- 结论：
  - 规划 / 治理层就算全部补齐，也只能说明 `Doc Closed / Contract Closed`。
  - 在 runtime capability 层未闭环前，不能写成“已上线”“可灰度”“已验证通过”。

| Reserved 能力 | 已有规划 / 治理证据 | 缺失的 runtime 证据 | 明确禁止误写为 | Doc Closed | Contract Closed | Runtime Not Proven | Release Blocked |
|---|---|---|---|---|---|---|---|
| gift-card | PRD、domain contract、gate spec、`miniapp.gift-card`、`1011009901/1011009902` 已落盘 | 当前无真实 `/pages/gift-card/*` 页面、无真实 app controller、无前端绑定 | “礼品卡 runtime capability 已上线 / 可灰度” | `Yes` | `Yes` | `Yes` | `Yes` |
| referral | PRD、domain contract、gate spec、`miniapp.referral`、`1013009901/1013009902` 已落盘 | 当前无真实 `/pages/referral/*` 页面、无真实 app controller、无前端绑定 | “邀请有礼 runtime capability 已上线 / 可灰度” | `Yes` | `Yes` | `Yes` | `Yes` |
| technician-feed | policy、PRD、domain contract、gate spec、`miniapp.technician-feed.audit`、`1030009901` 已落盘 | 当前无真实 `/pages/technician/feed` 页面、无真实 app controller、无前端绑定、无运行样本 | “技师动态 runtime capability 已上线 / 可灰度” | `Yes` | `Yes` | `Yes` | `Yes` |

## 6. BO-004 最终 contract 口径

| 阻断项 | 最终口径 | Doc Closed | Contract Closed | Runtime Not Proven | Release Blocked |
|---|---|---|---|---|---|
| `/booking/commission/*` 只认 controller-only truth | 只认 `TechnicianCommissionController`；不得借 `commissionSettlement.ts` 或 `commission-settlement/*.vue` 给 BO-004 | `Yes` | `Yes` | `Yes` | `Yes` |
| 查询空态 | `list-by-technician` / `list-by-order` / `config/list` 的 `[]` 合法；`pending-amount=0` 合法 | `Yes` | `Yes` | `Yes` | `Yes` |
| 写接口 `true` 语义 | `settle / batch-settle / config/save / config/delete` 返回 `true`，不等于真实生效；当前不能写成稳定 fail-close | `Yes` | `Yes` | `Yes` | `Yes` |
| 错误码锚点 | `COMMISSION_NOT_EXISTS(1030007000)`、`COMMISSION_ALREADY_SETTLED(1030007001)` 当前无真实对外暴露证据，不得写入稳定 contract 锚点 | `Yes` | `Yes` | `Yes` | `Yes` |
| 降级字段锚点 | 当前无服务端 `degraded=true / degradeReason` 证据 | `Yes` | `Yes` | `Yes` | `Yes` |

固定结论：
- BO-004 现在只能写成 `controller-only contract truth`。
- 查询接口允许 `[] / 0` 合法空态；写接口 `true` 只说明当前请求路径返回成功包，不说明实际业务效果已经被稳定证明。
- 当前无稳定 admin 专属错误码锚点，也无服务端降级字段证据。

## 7. 本批统一发布结论
- `Booking`：旧 path / old method 继续阻断开发与放量，不得升 `ACTIVE`。
- `Member`：缺页能力继续阻断 `ACTIVE page capability`，接口字段不能反推页面已闭环。
- `Reserved`：治理层已齐，不等于 runtime 已上线；继续 `Release Blocked`。
- `BO-004`：只到 controller-only contract，继续 `Release Blocked`。
