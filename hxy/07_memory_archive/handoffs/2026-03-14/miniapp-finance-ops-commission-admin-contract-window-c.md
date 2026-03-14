# Window C Handoff - MiniApp Finance Ops Commission Admin Contract（2026-03-14）

## 1. 本批交付
- 分支：`feat/ui-four-account-reconcile-ops`
- 交付类型：仅补 BO-004 admin contract、更新 truth review、补 handoff；未改业务代码、未改 overlay 页面、未动 `.codex`、未改历史 handoff、未处理无关 untracked。
- 新增文件：
  1. `docs/contracts/2026-03-14-miniapp-finance-ops-technician-commission-admin-contract-v1.md`
  2. `hxy/07_memory_archive/handoffs/2026-03-14/miniapp-finance-ops-commission-admin-contract-window-c.md`
- 更新文件：
  1. `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`

## 2. 核心收口结论

### 2.1 BO-004 现在有独立 admin contract
- 新 contract 只覆盖 `TechnicianCommissionController` 这一组真实接口：
  - `GET /booking/commission/list-by-technician`
  - `GET /booking/commission/list-by-order`
  - `GET /booking/commission/pending-amount`
  - `POST /booking/commission/settle`
  - `POST /booking/commission/batch-settle`
  - `GET /booking/commission/config/list`
  - `POST /booking/commission/config/save`
  - `DELETE /booking/commission/config/delete`
- 已与 `commission-settlement` 完全拆开：
  - `/booking/commission/*` 归 BO-004
  - `/booking/commission-settlement/*` 继续归 BO-003

### 2.2 当前绑定结论固定
- 在限定审查范围内：
  - 未核到独立前端 API 文件绑定 `/booking/commission/*`
  - 未核到独立后台页面文件绑定 `/booking/commission/*`
- 已核到的 `commissionSettlement.ts`、`commission-settlement/index.vue`、`commission-settlement/outbox/index.vue` 只绑定 `/booking/commission-settlement/*`。
- 因此 BO-004 当前只能记为：
  - admin controller 已存在
  - 不是前端绑定已存在
  - 不是后台页面闭环已完成

### 2.3 字段 / 权限 / 空态语义固定
- 权限键：
  - `booking:commission:query`
  - `booking:commission:settle`
  - `booking:commission:config`
- 查询空态：
  - `list-by-technician` / `list-by-order` / `config/list` 的 `[]` 合法
  - `pending-amount` 的 `0` 合法
- 写操作：
  - `settle` / `batch-settle` 当前不是稳定 fail-close
  - `config/save` / `config/delete` 当前也不是稳定 fail-close
  - 原因是 controller/service 没有把“记录不存在 / 状态不符 / 删除 0 行 / 更新 0 行”固定为业务错误码返回

### 2.4 错误码结论固定
- 当前未核到稳定 admin 专属错误码。
- 虽然 booking 模块定义了：
  - `COMMISSION_NOT_EXISTS(1030007000)`
  - `COMMISSION_ALREADY_SETTLED(1030007001)`
- 但当前 `TechnicianCommissionController -> TechnicianCommissionServiceImpl` 路径没有稳定抛出它们：
  - `settleCommission` 对不存在记录直接返回
  - `settleCommission` 对非待结算状态直接返回
  - `batchSettleByTechnician` 对空列表直接完成
- 因此本批未更新 canonical error register。

## 3. 给窗口 A / B / D 的联调注意点

### 3.1 给窗口 A
- 不要把 `commissionSettlement.ts` 或 `commission-settlement/*.vue` 的绑定关系借给 BO-004。
- 若后续要做 BO-004 admin 页面，接口字段只能按当前 contract 接：
  - `list-by-technician`：`technicianId`
  - `list-by-order`：`orderId`
  - `pending-amount`：`technicianId`
  - `settle`：`commissionId`
  - `batch-settle`：`technicianId`
  - `config/list`：`storeId`
  - `config/save`：`{id?,storeId,commissionType,rate,fixedAmount?}`
  - `config/delete`：`id`
- `pending-amount=0` 和查询返回 `[]` 都必须按合法空态处理。

### 3.2 给窗口 B
- BO-004 现阶段只能写成“controller-only contract 真值已固定”，不能改写成“后台页面闭环完成”。
- `settle` / `batch-settle` / `config/save` / `config/delete` 当前都不应被产品文档描述成稳定 fail-close 写链路。
- `COMMISSION_NOT_EXISTS` / `COMMISSION_ALREADY_SETTLED` 不能直接写进 BO-004 contract/errorCode 文档，因为当前 controller 路径没有稳定抛出证据。

### 3.3 给窗口 D
- 验收与 runbook 不能假设 BO-004 已有独立后台页。
- 降级/阻断要按当前真实语义区分：
  - 查询接口：`[]` / `0` 为合法空态
  - 写接口：当前存在静默成功/no-op 语义，不能按稳定 fail-close 设计监控分支
- 当前没有稳定 admin 专属错误码锚点可验，不能按不存在的 code 做自动化分支。

## 4. 风险与建议
- 风险 1：若后续继续沿用 `commission-settlement` 页面证据，BO-003/BO-004 边界会再次混淆。
- 风险 2：`settle`/`batch-settle` 当前静默成功语义较弱，若前端或验收按“失败必报错”理解，会与真实 controller 行为冲突。
- 风险 3：`config/save` 更新不存在 `id`、`config/delete` 删除不存在 `id` 当前都没有稳定业务错误锚点，不能被文档写成强校验写链路。
- 建议：
  1. A 侧若要补 BO-004 页面，先独立建 API 文件与页面文件，再回填 truth review。
  2. B 侧若要补 BO-004 产品口径，必须显式写“当前只有 controller contract，没有 admin 页面闭环证据”。
  3. D 侧将“空列表/0 合法、写接口非稳定 fail-close、无稳定 admin 专属错误码”列为本轮固定验收前提。
