# MiniApp Finance Ops Technician Commission Admin Page/API Binding Truth Review v1（2026-03-15）

## 1. 目标与结论
- 目标：对 `BO-004 技师提成明细 / 计提管理` 做一份独立的后台页面/API binding 真值审查，只基于当前真实页面文件、真实 API 文件、真实 controller、真实测试、真实脚本、真实已提交文档判断是否闭环。
- 本文只使用以下真实证据：
  - 后台页面目录：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking`
  - 后台 API 目录：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking`
  - 菜单 SQL：`ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-24-hxy-booking-commission-admin-menu.sql`
  - 后端 controller：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/TechnicianCommissionController.java`
  - admin truth 测试：`tests/technician-commission-admin-truth.test.mjs`
  - controller 回归测试：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/TechnicianCommissionControllerTest.java`
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
  1. `BO-004` 独立后台页面文件：`已核出`
  2. `BO-004` 独立后台 API 文件：`已核出`
  3. `BO-004` 页面到 `/booking/commission/*` 的 binding：`静态闭环已核出`
  4. 菜单 SQL、truth test、controller 回归测试只能证明代码库内的 admin-only 真值已闭环，不能等同于菜单已执行、页面已放量或 release-ready
  5. 因此当前唯一允许写法更新为：`admin-only 页面/API 真值已闭环 / Can Develop / Cannot Release`

## 2. 页面文件真值

| 审查对象 | 页面文件真值 | 是否核出 | 说明 |
|---|---|---|---|
| `BO-003` 结算审批主页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue` | 已核出 | 真实存在，但只承接 `/booking/commission-settlement/*` |
| `BO-003` 通知出站页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue` | 已核出 | 真实存在，但只承接 `/booking/commission-settlement/*` |
| 四账对账页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue` | 已核出 | 与 `BO-004` 无直接页面绑定关系 |
| 退款回调重放页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue` | 已核出 | 与 `BO-004` 无直接页面绑定关系 |
| `BO-004` 技师提成明细 / 计提管理页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission/index.vue` | 已核出 | 独立页面真实承接 `/booking/commission/*`，页面内已显式要求“写后回读”与 no-op 风险提示 |

## 3. API 文件真值

| 审查对象 | API 文件真值 | 是否核出 | 说明 |
|---|---|---|---|
| `BO-003` 结算审批 / 通知出站 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commissionSettlement.ts` | 已核出 | 真实绑定 `/booking/commission-settlement/*` |
| 四账对账 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/fourAccountReconcile.ts` | 已核出 | 与 `BO-004` 无直接 API 绑定关系 |
| 退款回调重放 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/refundNotifyLog.ts` | 已核出 | 与 `BO-004` 无直接 API 绑定关系 |
| `BO-004` 技师提成明细 / 计提管理 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commission.ts` | 已核出 | 独立 API 文件真实绑定 8 条 `/booking/commission/*` 路径，且未补造不存在字段 |

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
- 已核到 admin truth 测试：
  - `technician-commission-admin-truth.test.mjs`
    - 覆盖独立 API 文件、独立页面文件、菜单 SQL、写后回读文案与 no-op 风险提示
- 已核到 controller 回归测试：
  - `TechnicianCommissionControllerTest`
    - 覆盖列表查询、待结算金额、`CommonResult<Boolean>` 写接口包装行为
- 已核到 service 测试：
  - `TechnicianCommissionServiceImplTest`
    - 覆盖佣金计提、门店配置覆盖、重复计提防重、取消后冲销等 service 语义
  - `TechnicianCommissionServiceImplCancelCommissionTest`
    - 覆盖冲正幂等、重复 key、反向佣金生成等 service 语义
- 未核到：
  - 由后台页面实际触发 `/booking/commission/*` 的 smoke / e2e / request sample
  - 菜单 SQL 已执行后的真实导航截图 / 录屏 / 发布样本

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
| 技师佣金明细查询 | `commission/index.vue` | `commission.ts` | `GET /booking/commission/list-by-technician`; `GET /booking/commission/list-by-order` | 已核到页面/API/controller 静态 binding 与 truth test；未核到真实请求样本 | 已闭环（admin-only） | 已形成后台代码真值，但仍缺发布样本 |
| 待结算金额查询 | `commission/index.vue` | `commission.ts` | `GET /booking/commission/pending-amount` | 已核到静态 binding；未核到真实请求样本 | 已闭环（admin-only） | `0` 是合法空态；空态合法不等于已放量 |
| 单条 / 批量结算 | `commission/index.vue` | `commission.ts` | `POST /booking/commission/settle`; `POST /booking/commission/batch-settle` | 已核到页面写后回读提示、truth test 与 controller 回归；未核到真实写请求样本 | 已闭环（admin-only） | `true` 仍不能直接等于“已验收通过” |
| 门店佣金配置管理 | `commission/index.vue` | `commission.ts` | `GET /booking/commission/config/list`; `POST /booking/commission/config/save`; `DELETE /booking/commission/config/delete` | 已核到页面/API/controller 静态 binding；未核到真实发布样本 | 已闭环（admin-only） | `config/save` / `config/delete` 仍必须坚持写后回读 |

## 6. 固定口径

### 6.1 已核实
- `TechnicianCommissionController` 真实存在，且 `/booking/commission/*` 8 条接口真实可见。
- `commission/index.vue`、`commission.ts`、`2026-03-24-hxy-booking-commission-admin-menu.sql` 真实存在。
- `tests/technician-commission-admin-truth.test.mjs` 与 `TechnicianCommissionControllerTest` 真实存在。
- `commission-settlement/index.vue`、`commission-settlement/outbox/index.vue`、`commissionSettlement.ts` 真实存在，但它们只属于 `BO-003`。
- `TechnicianCommissionServiceImplTest`、`TechnicianCommissionServiceImplCancelCommissionTest` 真实存在。
- `check_finance_partial_closure_gate.sh` 真实存在，但它只检查 service/test 锚点。

### 6.2 未核出
- `BO-004` 页面到 `/booking/commission/*` 的真实 binding 样本
- `BO-004` 独立 admin request sample / smoke / e2e 测试
- `BO-004` 独立发布证据
- 菜单 SQL 已执行后的真实导航样本

### 6.3 只能写成
- `admin-only 页面/API 真值已闭环 / Can Develop / Cannot Release`
- 回填全项目主台账时，`BO-004` 的配套文档只认：
  - `docs/contracts/2026-03-14-miniapp-finance-ops-technician-commission-admin-contract-v1.md`
  - `docs/plans/2026-03-14-miniapp-finance-ops-technician-commission-admin-runbook-v1.md`
  - `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`

### 6.4 禁止写成
- “`BO-004` 后台页面闭环已完成”
- “已有独立后台 API 文件”
- “service 测试通过 = admin 页面已接通”
- “gate 脚本通过 = release-ready”
- “菜单 SQL 已落地 = 菜单已执行上线”
- “`commission-settlement` 页面 / API 可以复用成 `BO-004` binding 证据”
- “其他 03-15 admin PRD 的缺失 contract/runbook 可以借 `BO-004` 专项文档冲抵”
