# Window E Handoff - Booking Admin Truth（2026-03-15）

## 1. 窗口范围
- 窗口：`E`
- 专项范围：`BO-004 技师提成明细 / 计提管理` 独立后台页面 / API binding 真值盘点与证据收口
- worktree：`/root/crmeb-java/.worktrees/window-e-booking-admin-truth-20260315`
- branch：`window-e-booking-admin-truth-20260315`

## 2. 本次新增 / 更新文件
- 新增：
  - `docs/products/miniapp/2026-03-15-miniapp-finance-ops-technician-commission-admin-page-api-binding-truth-review-v1.md`
  - `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`
  - `hxy/07_memory_archive/handoffs/2026-03-15/booking-admin-truth-window-e.md`
- 更新：
  - `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`

## 3. 已核实
- 后台 booking overlay 页面真实文件只有：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue`
- 后台 booking overlay API 真实文件只有：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commissionSettlement.ts`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/fourAccountReconcile.ts`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/refundNotifyLog.ts`
- `TechnicianCommissionController` 真实存在，且 `/booking/commission/*` 8 条接口真实存在。
- `TechnicianCommissionServiceImplTest`、`TechnicianCommissionServiceImplCancelCommissionTest` 真实存在。
- `check_finance_partial_closure_gate.sh` 真实存在，且只检查 service/test 锚点。
- `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md` 已回填“前台/后台功能 -> PRD -> contract/runbook”映射；其中 `BO-004` 只认 03-14 contract + 03-14 runbook + 03-15 evidence ledger。

## 4. 未核出
- `BO-004` 独立后台页面文件
- `BO-004` 独立后台 API 文件
- `BO-004` 页面到 `/booking/commission/*` 的真实 binding 样本
- `BO-004` 独立 admin page/API 自动化测试
- `BO-004` 独立发布证据、灰度记录、回滚样本
- 除 `BO-004` 外，其它 03-15 admin PRD 可直接复用的独立 contract/runbook

## 5. 只能写成
- `仅接口闭环 + 页面真值待核`
- `ACTIVE_ADMIN（controller-only）`
- `BO-004` 配套文档只认 03-14 contract + 03-14 runbook + 03-15 evidence ledger`

## 6. 不得写成
- “`BO-004` 后台页面闭环已完成”
- “已有独立后台 API 文件”
- “service 测试通过 = admin 页面已接通”
- “gate 脚本通过 = release-ready”
- “`commission-settlement` 页面 / API 可以直接作为 `BO-004` 绑定证据”

## 7. 对 A/B/C/D 的联调注意点

### A 窗口
- 主台账、coverage matrix、closure review 一律保持 `BO-004 = 仅接口闭环 + 页面真值待核`。
- 若保留状态标签，必须写成 `ACTIVE_ADMIN（controller-only）` 或等价说明，不能省略 `controller-only`。
- 全项目主台账里若某个 03-15 admin PRD 未核到独立 contract/runbook，必须继续写 `未核出独立 contract/runbook`，不要借 `BO-004` 专项文档冲抵。

### B 窗口
- 页面字段、菜单 path、路由 path 一律不能猜写；未核到就写 `未核出`。
- 关键检索键 / 主业务键只认：
  - query：`technicianId`、`orderId`、`commissionId`、`storeId`、`id`
  - body：`id?`、`storeId`、`commissionType`、`rate`、`fixedAmount?`
- 合法空态：
  - `list-by-technician` / `list-by-order` / `config/list` 返回 `[]`
  - `pending-amount` 返回 `0`

### C 窗口
- method + path 只认：
  - `GET /booking/commission/list-by-technician`
  - `GET /booking/commission/list-by-order`
  - `GET /booking/commission/pending-amount`
  - `POST /booking/commission/settle`
  - `POST /booking/commission/batch-settle`
  - `GET /booking/commission/config/list`
  - `POST /booking/commission/config/save`
  - `DELETE /booking/commission/config/delete`
- 当前未核到稳定 admin 专属错误码对外暴露证据，`1030007000/1030007001` 不能写进稳定 canonical register。
- `true` 成功体不能直接写成稳定 fail-close 成功证明。

### D 窗口
- 降级行为只能按运维动作写，不能杜撰服务端 `degraded` 字段。
- 当前可固定的行为边界：
  - 查询空态是成功态，不是降级态
  - `pending-amount=0` 是成功态，不是异常态
  - 写接口存在 `true` 但 no-op / 伪成功风险，必须继续坚持 `写后回读 + 审计键`
- 当前未核到独立发布证据，不能把本专项结论写成 `release-ready`。
