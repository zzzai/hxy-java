# Window B Handoff - MiniApp Finance Ops Commission Detail Pack（2026-03-14）

## 1. 本批交付
- 分支：`feat/ui-four-account-reconcile-ops`
- 交付类型：仅产品文档与 handoff；未改业务代码、未改 overlay 页面、未触碰 `.codex`、未动历史 handoff、未处理无关 untracked。
- 变更文件：
  1. `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-detail-config-prd-v1.md`
  2. `docs/products/miniapp/2026-03-12-miniapp-finance-ops-technician-commission-settlement-prd-v1.md`
  3. `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
  4. `hxy/07_memory_archive/handoffs/2026-03-14/miniapp-finance-ops-commission-detail-pack-window-b.md`

## 2. 核心收口结论
- BO-003 / BO-004 已拆清：
  - BO-003 继续只管结算单创建、审核、驳回、打款、通知出站。
  - BO-004 独立只管佣金记录查询、待结算金额、单条/批量直结、门店佣金配置。
- 页面真值结论保持不变：
  - `commission-settlement/index.vue`、`commission-settlement/outbox/index.vue` 只属于 BO-003。
  - 当前审查范围内没有 BO-004 独立后台页面文件，也没有独立前端 API 文件。
  - 因此 BO-004 仍只能记为“后台运营能力真实存在 + 页面真值待核”，不能写成页面已闭环上线。
- BO-004 8 条真实接口已固定：
  - `GET /booking/commission/list-by-technician`
  - `GET /booking/commission/list-by-order`
  - `GET /booking/commission/pending-amount`
  - `POST /booking/commission/settle`
  - `POST /booking/commission/batch-settle`
  - `GET /booking/commission/config/list`
  - `POST /booking/commission/config/save`
  - `DELETE /booking/commission/config/delete`

## 3. 给窗口 A / C / D 的联调注意点

### 3.1 给窗口 A（前端 / 集成）
- 不要把 BO-003 页面反推成 BO-004 页面：
  - `commission-settlement/index.vue` 不是佣金明细页
  - `commission-settlement/outbox/index.vue` 不是佣金配置页
- 当前 8 条 BO-004 接口在审查范围内没有独立前端 API 文件与页面文件，A 不能补写猜测性页面 path 或菜单 path。
- 关键字段只认真实 controller / VO：
  - 佣金记录：`id/technicianId/orderId/orderItemId/serviceOrderId/userId/storeId/commissionType/baseAmount/commissionRate/commissionAmount/status/sourceBizNo/settlementId/settlementTime/createTime`
  - 配置：`id/storeId/commissionType/rate/fixedAmount`

### 3.2 给窗口 C（契约 / 后端）
- 当前 BO-004 8 条接口没有 `degraded` 字段，也没有稳定的批量结果明细返回。
- `POST /booking/commission/settle` 与 `POST /booking/commission/batch-settle` 当前是真实直结语义：
  - 只返回 `Boolean`
  - 不创建 BO-003 结算单
  - 不返回处理条数
- 当前代码语义要继续保持写实：
  - `settle` 对不存在或非待结算记录是 no-op，controller 仍返回 `true`
  - `batch-settle` 也是 `true` 型返回，需靠重新查询确认状态
- 若后续补 contract，不要把 `COMMISSION_NOT_EXISTS(1030007000)`、`COMMISSION_ALREADY_SETTLED(1030007001)` 自动写成这 8 条接口的现网稳定返回，除非 controller/service 真正抛出并对外暴露。

### 3.3 给窗口 D（数据 / 验收）
- 验收重点是“写实边界”，不是“页面闭环”：
  - BO-004 当前无独立后台页面文件
  - BO-004 当前无独立前端 API 文件
- 验收重点字段：
  - 查询键：`technicianId`、`orderId`、`storeId`
  - 审计键：`id`、`sourceBizNo`、`settlementId`
  - 状态金额：`commissionType`、`commissionAmount`、`status`、`settlementTime`
- 降级 / 失败语义：
  - 当前 8 条接口无 `degraded`
  - 直结类接口不能只看 `true`，必须二次查询确认
  - 配置 save/delete 也必须重新拉列表确认最终状态

## 4. 风险与建议
- 风险 1：现有结算审批 PRD 之前混带 BO-004，若继续引用旧口径，会把结算审批页误写成佣金明细页。
- 风险 2：BO-004 当前最容易被误写成“后台页面已上线”；本次文档已固定不能这样写。
- 风险 3：`settle` / `batch-settle` 是 `true` 型返回且可能 no-op，若后续前端直接渲染“结算成功”，会制造假成功。
- 建议：
  1. A 若后续要补 BO-004 页面，先补真实页面文件与独立 API 文件，再做 UI。
  2. C 若后续要补 BO-004 contract，先把 no-op / 无明细 / 无 degraded 语义写实。
  3. D 把“直结返回 true 但状态未变”的校验列为 BO-004 专项验收项。
