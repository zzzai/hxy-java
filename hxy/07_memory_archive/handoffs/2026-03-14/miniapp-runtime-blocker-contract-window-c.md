# Window C Handoff - MiniApp Runtime Blocker Contract Closure（2026-03-14）

## 1. 本批范围
- 分支：`feat/ui-four-account-reconcile-ops`
- 输出类型：仅更新 contract 文档与 handoff；未改业务代码、未改 overlay 页面、未动 `.codex`、未改历史 handoff、未处理无关 untracked。
- 新增：
  - `docs/contracts/2026-03-14-miniapp-runtime-blocker-contract-closure-v1.md`
  - `hxy/07_memory_archive/handoffs/2026-03-14/miniapp-runtime-blocker-contract-window-c.md`
- 更新：
  - `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
  - `docs/contracts/2026-03-10-miniapp-member-domain-contract-v1.md`
  - `docs/contracts/2026-03-10-miniapp-active-vs-planned-api-matrix-v1.md`
  - `docs/contracts/2026-03-14-miniapp-finance-ops-technician-commission-admin-contract-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`

## 2. 当前固定结论

### 2.1 Booking
- legacy path / method 阻断继续保留，不能因为 contract 已写清就删除：
  - `GET /booking/technician/list-by-store`
  - `GET /booking/time-slot/list`
  - `PUT /booking/order/cancel`
  - `POST /booking/addon/create`
- 仍 blocked 的能力：
  - `booking.technician-select`
  - `booking.slot-select`
  - `booking.create-chain`
  - `booking.cancel`
  - `booking.addon`
- 固定口径：
  - `Doc Closed = Yes`
  - `Contract Closed = Yes`
  - `Runtime Not Proven = Yes`
  - `Release Blocked = Yes`

### 2.2 Member
- 缺页能力继续固定为 blocked：
  - `/pages/user/level`
  - `/pages/profile/assets`
  - `/pages/user/tag`
- 不能被误写为 active page capability 的接口 / 字段：
  - `/member/level/list`
  - `/member/experience-record/page`
  - `/member/asset-ledger/page`
  - `ledgerId/assetType/bizType/amount/balanceAfter/sourceBizNo/runId/total/degraded/degradeReason`
- 固定口径：
  - 页面未实现不等于 capability 已闭环。
  - `ACTIVE` API truth 不等于 `ACTIVE page capability`。

### 2.3 Reserved runtime gate
- gift-card / referral / technician-feed 的 PRD、contract、gate spec、switchKey、errorcode 都已落盘，但这只算治理层证据。
- 当前仍缺真实页面、真实 app controller、真实前端绑定或运行样本。
- 固定口径：
  - 不能把治理开关、灰度规则、`RESERVED_DISABLED` 注册表写成 runtime API 已上线证据。
  - 继续 `Runtime Not Proven / Release Blocked`。

### 2.4 BO-004
- `/booking/commission/*` 只认 `TechnicianCommissionController` controller-only truth。
- `commissionSettlement.ts`、`commission-settlement/index.vue`、`commission-settlement/outbox/index.vue` 只能证明 BO-003，不得借给 BO-004。
- 查询空态：
  - `list-by-technician` / `list-by-order` / `config/list` 的 `[]` 合法
  - `pending-amount=0` 合法
- 写接口：
  - `settle / batch-settle / config/save / config/delete` 的 `true` 不等于真实生效
  - 当前都不能写成稳定 fail-close
- 错误码 / 降级：
  - `COMMISSION_NOT_EXISTS(1030007000)`、`COMMISSION_ALREADY_SETTLED(1030007001)` 当前无真实对外暴露证据
  - 当前无服务端 `degraded=true / degradeReason` 证据

## 3. 给窗口 A / B / D 的联调注意点

### 3.1 给窗口 A
- 继续把 Booking 旧 path / old method 作为 release blocker 挂在 capability / freeze / allowlist，不得因为 C 侧文档补全而移除。
- Member 侧不能把 `/member/level/list`、`/member/experience-record/page`、`/member/asset-ledger/page` 反推为 active page capability。
- Reserved 侧不能把 switchKey、灰度规则、errorcode register 写成 runtime 已上线证据。
- BO-004 仍只能写成“controller-only contract 已固定，页面/API binding 未核出”。

### 3.2 给窗口 B
- Booking 的 create/cancel/addon 当前都不能升 `ACTIVE`。
- Member 的等级页、资产总账页、标签页继续是缺页能力。
- BO-004 不得借 `commissionSettlement.ts` 或 `commission-settlement/*.vue`，也不得把写接口写成稳定 fail-close。

### 3.3 给窗口 D
- Booking 联调继续按旧 path blocker 处理；不要把文档完整视作 runtime 已证明。
- Member 验收区分：
  - API truth 已有
  - page capability 仍 blocked
- BO-004 验收继续按：
  - 查询 `[] / 0` 合法
  - 写接口 `true` 不等于真实生效
  - 无稳定 admin 专属错误码锚点
  - 无服务端 `degraded=true / degradeReason`
