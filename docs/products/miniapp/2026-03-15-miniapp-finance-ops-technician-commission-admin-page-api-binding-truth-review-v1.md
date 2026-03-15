# MiniApp Finance Ops Technician Commission Admin Page/API Binding Truth Review v1（2026-03-15）

## 1. 目标与结论
- 目标：对 `BO-004 技师提成明细 / 计提管理` 做一份独立的后台页面/API binding 真值审查，只基于当前真实页面文件、真实 API 文件、真实 controller、真实测试、真实脚本、真实已提交文档判断是否闭环。
- 本文只使用以下真实证据：
  - 后台页面目录：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking`
  - 后台 API 目录：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking`
  - 后端 controller：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/TechnicianCommissionController.java`
  - service 测试：
    - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/TechnicianCommissionServiceImplTest.java`
    - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/TechnicianCommissionServiceImplCancelCommissionTest.java`
  - gate 脚本：`ruoyi-vue-pro-master/script/dev/check_finance_partial_closure_gate.sh`
  - 已提交文档：
    - `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
    - `docs/contracts/2026-03-14-miniapp-finance-ops-technician-commission-admin-contract-v1.md`
    - `docs/products/miniapp/2026-03-14-miniapp-finance-ops-admin-doc-closure-review-v1.md`
    - `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
- 当前结论固定为：
  1. `BO-004` 独立后台页面文件：`未核出`
  2. `BO-004` 独立后台 API 文件：`未核出`
  3. `BO-004` 页面到 `/booking/commission/*` 的 binding：`未闭环`
  4. service 测试和 finance gate 脚本只能证明 controller/service 语义存在，不能等同于 admin page/API binding 已闭环，更不能等同于 release-ready
  5. 因此当前唯一允许写法仍是：`仅接口闭环 + 页面真值待核`

## 2. 页面文件真值

| 审查对象 | 页面文件真值 | 是否核出 | 说明 |
|---|---|---|---|
| `BO-003` 结算审批主页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue` | 已核出 | 真实存在，但只承接 `/booking/commission-settlement/*` |
| `BO-003` 通知出站页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue` | 已核出 | 真实存在，但只承接 `/booking/commission-settlement/*` |
| 四账对账页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue` | 已核出 | 与 `BO-004` 无直接页面绑定关系 |
| 退款回调重放页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue` | 已核出 | 与 `BO-004` 无直接页面绑定关系 |
| `BO-004` 技师提成明细 / 计提管理页面 | `未核出` | 未核出 | 在上述真实目录内未核到独立页面文件承接 `/booking/commission/*` |

## 3. API 文件真值

| 审查对象 | API 文件真值 | 是否核出 | 说明 |
|---|---|---|---|
| `BO-003` 结算审批 / 通知出站 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commissionSettlement.ts` | 已核出 | 真实绑定 `/booking/commission-settlement/*` |
| 四账对账 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/fourAccountReconcile.ts` | 已核出 | 与 `BO-004` 无直接 API 绑定关系 |
| 退款回调重放 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/refundNotifyLog.ts` | 已核出 | 与 `BO-004` 无直接 API 绑定关系 |
| `BO-004` 技师提成明细 / 计提管理 | `未核出` | 未核出 | 在上述真实目录内未核到 `commission.ts`、`technicianCommission.ts` 或任何直接绑定 `/booking/commission/*` 的独立 API 文件 |

## 4. Controller、测试、脚本真值

### 4.1 Controller 真值
- `TechnicianCommissionController` 类路径：`/booking/commission`
- 当前真实接口：
  - `GET /booking/commission/list-by-technician`
  - `GET /booking/commission/list-by-order`
  - `GET /booking/commission/pending-amount`
  - `POST /booking/commission/settle`
  - `POST /booking/commission/batch-settle`
  - `GET /booking/commission/config/list`
  - `POST /booking/commission/config/save`
  - `DELETE /booking/commission/config/delete`
- 当前真实权限键：
  - `booking:commission:query`
  - `booking:commission:settle`
  - `booking:commission:config`

### 4.2 测试真值
- 已核到 service 测试：
  - `TechnicianCommissionServiceImplTest`
    - 覆盖佣金计提、门店配置覆盖、重复计提防重、取消后冲销等 service 语义
  - `TechnicianCommissionServiceImplCancelCommissionTest`
    - 覆盖冲正幂等、重复 key、反向佣金生成等 service 语义
- 未核到：
  - 独立 admin page 测试
  - 独立 admin API binding 测试
  - 由后台页面实际触发 `/booking/commission/*` 的 smoke / e2e / request sample

### 4.3 脚本真值
- `check_finance_partial_closure_gate.sh` 已核到 `TechnicianCommissionServiceImplTest` 的 warn 级 anchor 检查：
  - `testCalculateCommission_duplicatePrevention`
  - `BOOKING_COMMISSION_ACCRUAL`
  - `testCancelCommission_shouldBeIdempotentForSettledReversal`
- 该脚本结论边界必须固定为：
  - 只是在检查 service/test 锚点是否存在
  - 不证明独立 admin page 文件存在
  - 不证明独立 admin API 文件存在
  - 不证明页面到 `/booking/commission/*` 的 binding 已闭环
  - 不证明 release evidence 已齐

## 5. 页面到接口映射真值

| 业务能力 | 页面文件真值 | API 文件真值 | Controller 真值 | 测试 / 脚本真值 | 是否闭环 | 结论 |
|---|---|---|---|---|---|---|
| 技师佣金明细查询 | `未核出` | `未核出` | `GET /booking/commission/list-by-technician`; `GET /booking/commission/list-by-order` | 仅核到 service 层佣金计提 / 冲正测试；未核到 admin query page/API binding | 未闭环 | 只能写成 controller 接口存在，不能写成后台页面已闭环 |
| 待结算金额查询 | `未核出` | `未核出` | `GET /booking/commission/pending-amount` | 未核到后台页面请求样本；未核到 admin API binding 测试 | 未闭环 | `0` 是合法空态，但空态合法不等于页面闭环 |
| 单条 / 批量结算 | `未核出` | `未核出` | `POST /booking/commission/settle`; `POST /booking/commission/batch-settle` | service 测试只证明底层语义；未核到后台页面发起写请求的真实样本 | 未闭环 | `true` 不能直接等于“页面已接通”或“写链路已验收” |
| 门店佣金配置管理 | `未核出` | `未核出` | `GET /booking/commission/config/list`; `POST /booking/commission/config/save`; `DELETE /booking/commission/config/delete` | 未核到后台配置页、未核到 config API binding 测试 | 未闭环 | `config/save` / `config/delete` 仍不能写成页面闭环 |

## 6. 固定口径

### 6.1 已核实
- `TechnicianCommissionController` 真实存在，且 `/booking/commission/*` 8 条接口真实可见。
- `commission-settlement/index.vue`、`commission-settlement/outbox/index.vue`、`commissionSettlement.ts` 真实存在，但它们只属于 `BO-003`。
- `TechnicianCommissionServiceImplTest`、`TechnicianCommissionServiceImplCancelCommissionTest` 真实存在。
- `check_finance_partial_closure_gate.sh` 真实存在，但它只检查 service/test 锚点。

### 6.2 未核出
- `BO-004` 独立后台页面文件
- `BO-004` 独立后台 API 文件
- `BO-004` 页面到 `/booking/commission/*` 的真实 binding 样本
- `BO-004` 独立 admin page/API smoke 或 e2e 测试
- `BO-004` 独立发布证据

### 6.3 只能写成
- `仅接口闭环 + 页面真值待核`
- 回填全项目主台账时，`BO-004` 的配套文档只认：
  - `docs/contracts/2026-03-14-miniapp-finance-ops-technician-commission-admin-contract-v1.md`
  - `docs/plans/2026-03-14-miniapp-finance-ops-technician-commission-admin-runbook-v1.md`
  - `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`

### 6.4 禁止写成
- “`BO-004` 后台页面闭环已完成”
- “已有独立后台 API 文件”
- “service 测试通过 = admin 页面已接通”
- “gate 脚本通过 = release-ready”
- “`commission-settlement` 页面 / API 可以复用成 `BO-004` binding 证据”
- “其他 03-15 admin PRD 的缺失 contract/runbook 可以借 `BO-004` 专项文档冲抵”
