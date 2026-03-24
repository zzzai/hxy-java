# MiniApp 技师提成后台页面真值审查 v1（2026-03-12）

## 1. 目标与结论
- 目标：基于当前真实后台页面文件、后台 API 文件、admin controller，审查 `BO-004 技师提成明细 / 计提管理` 是否已形成“后台页面闭环”。
- 审查范围仅限：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/*`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/*`
  - `ruoyi-vue-pro-master/**/TechnicianCommissionController.java`
  - `ruoyi-vue-pro-master/**/TechnicianCommissionSettlementController.java`
- 最终结论：
  1. `BO-003 技师提成结算 / 审核 / 驳回 / 打款 / 通知补偿` 已核到真实后台页面文件和真实 API 文件，属于页面闭环已完成。
  2. `BO-004 技师提成明细 / 计提管理` 已在当前审查范围内核到独立后台页面文件 `commission/index.vue`、独立前端 API 文件 `commission.ts`、独立菜单 SQL 和专项真值测试。
  3. 因此 `BO-004` 当前结论升级为：`admin-only 页面/API 真值已闭环 / Can Develop / Cannot Release`；不得写成 `release-ready`、不得写成“已可直接放量”。

## 2. 页面文件真值

| 业务对象 | 页面文件真值 | 页面结论 | 说明 |
|---|---|---|---|
| BO-003 结算审批主页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue` | 已核出 | 页面内已绑定结算分页、提交审核、审核通过、审核驳回、打款、日志查看、通知出站跳转 |
| BO-003 通知出站页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue` | 已核出 | 页面内已绑定通知出站分页、单条/批量重试 |
| BO-004 技师提成明细 / 计提管理页面 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission/index.vue` | 已核出 | 页面真实承接 `TechnicianCommissionController` 的 `/booking/commission/*` 接口；当前代码真值已闭环，但菜单执行样本与发布截图仍未核出 |

## 3. 前端 API 文件真值

| 业务对象 | API 文件真值 | API 结论 | 说明 |
|---|---|---|---|
| BO-003 结算审批 / 通知出站 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commissionSettlement.ts` | 已核出 | 文件内真实绑定 `/booking/commission-settlement/page`、`/get`、`/create`、`/submit`、`/approve`、`/reject`、`/pay`、`/log-list`、`/notify-outbox-page`、`/notify-outbox-retry`、`/notify-outbox-batch-retry` |
| BO-004 技师提成明细 / 计提管理 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commission.ts` | 已核出 | 文件内真实绑定 8 条 `/booking/commission/*` 路径，且未补造 `degraded` / `degradeReason` 等不存在字段 |

## 4. Controller 真值

### 4.1 `TechnicianCommissionController`
- 类路径：`/booking/commission`
- 当前真实接口：
  - `GET /booking/commission/list-by-technician`
  - `GET /booking/commission/list-by-order`
  - `GET /booking/commission/pending-amount`
  - `POST /booking/commission/settle`
  - `POST /booking/commission/batch-settle`
  - `GET /booking/commission/config/list`
  - `POST /booking/commission/config/save`
  - `DELETE /booking/commission/config/delete`
- 权限键真值：
  - 查询类：`booking:commission:query`
  - 结算类：`booking:commission:settle`
  - 配置类：`booking:commission:config`
- 接口字段真值补充：
  - `GET /booking/commission/list-by-technician`
    - query：`technicianId`
    - response：`list[]:{id,technicianId,orderId,orderItemId,serviceOrderId,userId,storeId,commissionType,baseAmount,commissionRate,commissionAmount,status,sourceBizNo,settlementId,settlementTime,createTime}`
    - 空列表 `[]` 合法
  - `GET /booking/commission/list-by-order`
    - query：`orderId`
    - response：同 `TechnicianCommissionRespVO`
    - 空列表 `[]` 合法
  - `GET /booking/commission/pending-amount`
    - query：`technicianId`
    - response：`Integer`
    - `0` 金额合法
  - `POST /booking/commission/settle`
    - query：`commissionId`
    - response：`true`
    - 当前 service 对不存在或非待结算记录静默返回，不形成稳定 admin 专属错误码
  - `POST /booking/commission/batch-settle`
    - query：`technicianId`
    - response：`true`
    - 待结算列表为空时当前仍返回 `true`
  - `GET /booking/commission/config/list`
    - query：`storeId`
    - response：`list[]:{id,storeId,commissionType,rate,fixedAmount,createTime,updateTime,creator,updater,deleted}`
    - 空列表 `[]` 合法
  - `POST /booking/commission/config/save`
    - body：`{id?,storeId,commissionType,rate,fixedAmount?}`
    - response：`true`
  - `DELETE /booking/commission/config/delete`
    - query：`id`
    - response：`true`
- 当前审查结论：接口真实存在，且在限定审查范围内已核到与之直接绑定的后台页面文件、前端 API 文件、菜单 SQL 与专项真值测试；但当前仍没有稳定 admin 专属错误码外显，也没有发布级样本。

### 4.2 `TechnicianCommissionSettlementController`
- 类路径：`/booking/commission-settlement`
- 当前真实接口：
  - `POST /booking/commission-settlement/create`
  - `POST /booking/commission-settlement/submit`
  - `POST /booking/commission-settlement/approve`
  - `POST /booking/commission-settlement/reject`
  - `POST /booking/commission-settlement/pay`
  - `GET /booking/commission-settlement/get`
  - `GET /booking/commission-settlement/page`
  - `GET /booking/commission-settlement/list`
  - `GET /booking/commission-settlement/sla-overdue-list`
  - `GET /booking/commission-settlement/log-list`
  - `GET /booking/commission-settlement/notify-outbox-list`
  - `GET /booking/commission-settlement/notify-outbox-page`
  - `POST /booking/commission-settlement/notify-outbox-retry`
  - `POST /booking/commission-settlement/notify-outbox-batch-retry`
- 当前审查结论：接口真实存在，且在限定审查范围内可被 `commission-settlement/index.vue` 与 `commission-settlement/outbox/index.vue` 配套页面/API 文件承接。

## 5. 页面到接口映射

| 业务功能 | 页面文件 | API 文件 | Controller | Method + Path | 页面闭环判定 |
|---|---|---|---|---|---|
| 结算单分页查询 | `commission-settlement/index.vue` | `commissionSettlement.ts` | `TechnicianCommissionSettlementController` | `GET /booking/commission-settlement/page` | 已闭环 |
| 提交审核 | `commission-settlement/index.vue` | `commissionSettlement.ts` | `TechnicianCommissionSettlementController` | `POST /booking/commission-settlement/submit` | 已闭环 |
| 审核通过 / 驳回 | `commission-settlement/index.vue` | `commissionSettlement.ts` | `TechnicianCommissionSettlementController` | `POST /booking/commission-settlement/approve`; `POST /booking/commission-settlement/reject` | 已闭环 |
| 打款 | `commission-settlement/index.vue` | `commissionSettlement.ts` | `TechnicianCommissionSettlementController` | `POST /booking/commission-settlement/pay` | 已闭环 |
| 操作日志查看 | `commission-settlement/index.vue` | `commissionSettlement.ts` | `TechnicianCommissionSettlementController` | `GET /booking/commission-settlement/log-list` | 已闭环 |
| 通知出站分页 / 重试 | `commission-settlement/outbox/index.vue` | `commissionSettlement.ts` | `TechnicianCommissionSettlementController` | `GET /booking/commission-settlement/notify-outbox-page`; `POST /booking/commission-settlement/notify-outbox-batch-retry` | 已闭环 |
| 技师佣金明细 | `commission/index.vue` | `commission.ts` | `TechnicianCommissionController` | `GET /booking/commission/list-by-technician?technicianId`; `GET /booking/commission/list-by-order?orderId`；response 为 `TechnicianCommissionRespVO[]` | 已闭环（admin-only） |
| 待结算金额 | `commission/index.vue` | `commission.ts` | `TechnicianCommissionController` | `GET /booking/commission/pending-amount?technicianId`；response=`Integer`，`0` 合法 | 已闭环（admin-only） |
| 单条 / 批量结算 | `commission/index.vue` | `commission.ts` | `TechnicianCommissionController` | `POST /booking/commission/settle?commissionId`; `POST /booking/commission/batch-settle?technicianId`；response=`true` | 已闭环（admin-only） |
| 门店佣金配置 | `commission/index.vue` | `commission.ts` | `TechnicianCommissionController` | `GET /booking/commission/config/list?storeId`; `POST /booking/commission/config/save(body:{id?,storeId,commissionType,rate,fixedAmount?})`; `DELETE /booking/commission/config/delete?id` | 已闭环（admin-only） |

## 6. 对 BO-003 / BO-004 的单一真值判断

### 6.1 BO-003
- 当前结论：`页面闭环已完成`
- 依据：
  - 已核到真实页面文件 `commission-settlement/index.vue` 与 `commission-settlement/outbox/index.vue`
  - 已核到真实 API 文件 `commissionSettlement.ts`
  - 已核到真实 controller `TechnicianCommissionSettlementController`
  - 页面、API、controller 三者映射闭合

### 6.2 BO-004
- 当前结论：`admin-only 页面/API 真值已闭环 / Can Develop / Cannot Release`
- 依据：
  - 已核到真实 controller `TechnicianCommissionController`
  - 已核到独立后台页面文件 `commission/index.vue`
  - 已核到独立前端 API 文件 `commission.ts`
  - 已核到菜单 SQL `2026-03-24-hxy-booking-commission-admin-menu.sql`
  - 已核到专项真值测试 `tests/technician-commission-admin-truth.test.mjs`
  - 写接口仍只有 `true` 语义，必须坚持“写后回读”，且发布级样本仍未核出

## 7. 对主台账的回填口径
1. `BO-004` 可回填为 `Can Develop / Cannot Release` 的后台能力条目，说明必须写成“独立页面/API 已落地，但当前只到 admin-only 真值闭环”。
2. `BO-004` 的页面字段与 API 字段应回填为真实路径，不能继续写 `未核出`。
3. 后续若要把 `BO-004` 升级为“可放量 / release-ready”，至少还要补齐：
   - 页面真实可达样本 / 菜单执行样本
   - 页面触发 `/booking/commission/*` 的真实 request/response 样本
   - 灰度、回滚、发布回执与 sign-off 证据

## 8. 非目标
- 不在本次文档中猜测后台菜单、路由注册、权限菜单树。
- 不以 `TechnicianCommissionSettlementController` 的页面闭环反推 `TechnicianCommissionController` 也已闭环。
- 不把“结算审批页面存在”误写成“技师提成明细 / 计提管理页面存在”。
- 不把“代码库中已落地独立页面/API”误写成“已完成发布证据闭环”。
