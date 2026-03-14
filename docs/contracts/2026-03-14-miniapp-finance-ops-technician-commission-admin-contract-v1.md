# MiniApp Finance Ops Technician Commission Admin Contract v1 (2026-03-14)

## 1. 目标与真值来源
- 目标：为 `BO-004 技师提成明细 / 计提管理` 建立独立 admin contract，只覆盖 `TechnicianCommissionController` 这组真实 `/booking/commission/*` 接口，并与 `commission-settlement` 契约彻底拆开。
- 强约束：
  - 仅基于当前真实 controller、当前限定审查范围内的后台 API 文件、当前限定审查范围内的后台页面文件、已落盘文档判定。
  - 禁止把 `commission-settlement/index.vue`、`commission-settlement/outbox/index.vue`、`commissionSettlement.ts` 的绑定关系反推到 `/booking/commission/*`。
  - 如未核到稳定 admin 专属错误码，不得杜撰、影射或提前登记错误码。
- 真值输入：
  - 后端 controller：
    - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/TechnicianCommissionController.java`
  - 后端 request/response / service / mapper：
    - `TechnicianCommissionRespVO`
    - `TechnicianCommissionConfigSaveReqVO`
    - `TechnicianCommissionDO`
    - `TechnicianCommissionConfigDO`
    - `TechnicianCommissionService`
    - `TechnicianCommissionServiceImpl`
    - `TechnicianCommissionMapper`
    - `TechnicianCommissionConfigMapper`
  - 审查范围内后台页面 / API 文件证据：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/*`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/*`
  - 文档基线：
    - `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
    - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
    - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`

## 2. 当前绑定结论
- 在本次限定审查范围内：
  - 未核到独立前端 API 文件绑定 `/booking/commission/*`
  - 未核到独立后台页面文件绑定 `/booking/commission/*`
- 已核到的后台 API / 页面文件只有：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commissionSettlement.ts`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue`
- 这些文件只绑定 `/booking/commission-settlement/*`，不绑定 `/booking/commission/*`。
- 因此本文件的 contract 真值只能写成：
  - `TechnicianCommissionController` admin controller 已真实存在
  - 不代表后台前端绑定已存在
  - 不代表 BO-004 已形成独立页面闭环

## 3. Controller-Only Contract Matrix

| 场景 | method + path | permissionKey | request/query/body 真值 | response 字段真值 | 合法空态/成功态 | fail-close 判定 | 错误码证据 | 说明 |
|---|---|---|---|---|---|---|---|---|
| 获取技师佣金列表 | `GET /booking/commission/list-by-technician` | `booking:commission:query` | query:`technicianId(Long, required)` | `list[]:{id,technicianId,orderId,orderItemId,serviceOrderId,userId,storeId,commissionType,baseAmount,commissionRate,commissionAmount,status,sourceBizNo,settlementId,settlementTime,createTime}` | 空列表 `[]` 合法；无分页；按 `createTime DESC` 返回 | 否。查询空列表不阻断 | 当前未核到稳定 admin 专属错误码 | 真实返回类型是 `List<TechnicianCommissionRespVO>` |
| 获取订单佣金记录 | `GET /booking/commission/list-by-order` | `booking:commission:query` | query:`orderId(Long, required)` | `list[]:{id,technicianId,orderId,orderItemId,serviceOrderId,userId,storeId,commissionType,baseAmount,commissionRate,commissionAmount,status,sourceBizNo,settlementId,settlementTime,createTime}` | 空列表 `[]` 合法；无分页；当前 mapper 未显式排序 | 否。查询空列表不阻断 | 当前未核到稳定 admin 专属错误码 | 真实返回类型是 `List<TechnicianCommissionRespVO>` |
| 获取技师待结算佣金总额 | `GET /booking/commission/pending-amount` | `booking:commission:query` | query:`technicianId(Long, required)` | `Integer pendingAmount` | `0` 为合法返回；表示当前没有待结算金额 | 否。`0` 不应被解释为异常 | 当前未核到稳定 admin 专属错误码 | service 以待结算列表求和，`commissionAmount=null` 按 `0` 累加 |
| 结算单条佣金 | `POST /booking/commission/settle` | `booking:commission:settle` | query:`commissionId(Long, required)` | `true` | `true` 是唯一成功体 | 否。当前 controller/service 对“不存在记录”或“非待结算状态”不会报业务错，而是静默返回 `true` | 当前未核到稳定 admin 专属错误码 | 不能把该接口写成稳定 fail-close；底层运行时异常除外 |
| 按技师批量结算 | `POST /booking/commission/batch-settle` | `booking:commission:settle` | query:`technicianId(Long, required)` | `true` | `true` 是唯一成功体；无待结算记录时仍返回 `true` | 否。当前无待结算列表时静默成功 | 当前未核到稳定 admin 专属错误码 | 不能把“批量结算 0 条”写成错误 |
| 获取门店佣金配置列表 | `GET /booking/commission/config/list` | `booking:commission:query` | query:`storeId(Long, required)` | `list[]:{id,storeId,commissionType,rate,fixedAmount,createTime,updateTime,creator,updater,deleted}` | 空列表 `[]` 合法 | 否。查询空列表不阻断 | 当前未核到稳定 admin 专属错误码 | controller 直接返回 `List<TechnicianCommissionConfigDO>` |
| 保存门店佣金配置 | `POST /booking/commission/config/save` | `booking:commission:config` | body(JSON):`id?`,`storeId(Long, required)`,`commissionType(Integer, required)`,`rate(BigDecimal, required)`,`fixedAmount(Integer, optional)` | `true` | `true` 是唯一成功体 | 否。缺少必填字段会被 `@Valid` 阻断，但更新不存在 `id` 当前不会显式报业务错 | 当前未核到稳定 admin 专属错误码 | `id=null` 走 insert；`id!=null` 走 updateById |
| 删除门店佣金配置 | `DELETE /booking/commission/config/delete` | `booking:commission:config` | query:`id(Long, required)` | `true` | `true` 是唯一成功体 | 否。当前 `deleteById` 未校验删除行数 | 当前未核到稳定 admin 专属错误码 | 删除不存在 `id` 也不形成稳定业务错误锚点 |

## 4. 空态、金额与写操作语义
- 合法空态：
  - `GET /booking/commission/list-by-technician` -> `[]`
  - `GET /booking/commission/list-by-order` -> `[]`
  - `GET /booking/commission/config/list` -> `[]`
- 合法 0 金额：
  - `GET /booking/commission/pending-amount` -> `0`
  - 含义是“当前没有待结算佣金”，不是异常态
- 写操作语义：
  - `POST /booking/commission/settle`
    - 当前不是稳定 fail-close
    - `commissionId` 不存在或记录状态不是 `PENDING` 时，service 静默返回
  - `POST /booking/commission/batch-settle`
    - 当前不是稳定 fail-close
    - 待结算列表为空时静默返回 `true`
  - `POST /booking/commission/config/save`
    - 请求体缺少 `storeId` / `commissionType` / `rate` 会被框架校验阻断
    - 但更新不存在 `id` 当前不会显式抛业务错，因此不能写成稳定 fail-close
  - `DELETE /booking/commission/config/delete`
    - 当前不是稳定 fail-close
    - `id` 不存在时未见 controller/service 显式报错

## 5. 错误码证据结论
- 当前未核到稳定 admin 专属错误码。
- 虽然 `com.hxy.module.booking.enums.ErrorCodeConstants` 定义了：
  - `COMMISSION_NOT_EXISTS(1030007000)`
  - `COMMISSION_ALREADY_SETTLED(1030007001)`
- 但在当前 `TechnicianCommissionController` 对应的实际 controller/service 路径里，未核到它们被稳定抛出：
  - `settleCommission` 对不存在记录直接 `return`
  - `settleCommission` 对非 `PENDING` 状态直接 `return`
  - `batchSettleByTechnician` 对空列表直接完成
- 因此本 contract 不把这些错误码登记为 BO-004 的稳定 contract 锚点，也不更新 canonical error register。

## 6. 与 `commission-settlement` 契约的拆分边界
- 本文只覆盖 `TechnicianCommissionController`：
  - `/booking/commission/*`
- 本文不覆盖 `TechnicianCommissionSettlementController`：
  - `/booking/commission-settlement/*`
- 明确禁止的反推：
  - 不能因为 `commissionSettlement.ts` 已绑定 `/booking/commission-settlement/*`，就写成 `/booking/commission/*` 也存在独立 API 文件
  - 不能因为 `commission-settlement/index.vue` 已闭环，就写成 BO-004 页面也已闭环
  - 不能把 BO-003 的审批、打款、通知出站字段映射复用为 BO-004 contract 真值

## 7. 对主台账 / 联调的固定口径
- BO-004 当前只能写成：
  - `TechnicianCommissionController` 接口真实存在
  - admin page/API binding 未核出
  - `controller-only` contract 真值已固定
- 在后续独立页面文件、独立前端 API 文件、页面到 `/booking/commission/*` 的绑定证据全部核出前：
  - 不得把 BO-004 改写成“后台页面闭环完成”
  - 不得把 `commission-settlement` 的绑定证据借给 BO-004
