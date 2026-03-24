# MiniApp 财务运营技师提成明细 / 计提管理 PRD v1（2026-03-14）

## 0. 文档定位
- 目标：为 `BO-004 技师提成明细 / 计提管理` 补齐独立产品 PRD，固定后台运营能力真值、接口边界、审计字段与页面真值现状。
- 分支：`feat/ui-four-account-reconcile-ops`
- 范围约束：
  - 本文只覆盖 `TechnicianCommissionController` 暴露的 8 条真实接口。
  - 本文不覆盖 `BO-003 技师提成结算 / 审核 / 驳回 / 打款 / 通知补偿`；该部分继续由 `docs/products/miniapp/2026-03-12-miniapp-finance-ops-technician-commission-settlement-prd-v1.md` 负责。
  - 当前在审查范围内未核到独立后台页面文件，也未核到独立前端 API 文件。
  - 因此本文定义的是“后台运营能力与页面真值边界”，不是“页面已闭环上线说明”。
  - 所有写接口必须按“写后回读确认”定义结果，`Boolean true` 不能直接当成业务完成。
- 真值输入：
  - `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
  - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/TechnicianCommissionController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/TechnicianCommissionRespVO.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/TechnicianCommissionConfigSaveReqVO.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/TechnicianCommissionDO.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/TechnicianCommissionConfigDO.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/TechnicianCommissionServiceImpl.java`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/*`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/*`

## 1. 业务目标
1. 支持按技师维度查看佣金记录，识别待结算、已结算、已取消三类状态。
2. 支持按订单维度查看佣金记录，服务财务、运营、审计对单条订单的提成追溯。
3. 支持查询技师待结算佣金总额，作为是否进入 BO-003 结算审批的判断输入。
4. 支持后台直接对单条佣金或单个技师的全部待结算佣金执行“直结”操作。
5. 支持按门店维护佣金配置，定义门店级佣金类型覆盖规则。
6. 固定 BO-004 的页面真值边界，避免把 BO-003 结算审批页面反推成 BO-004 页面已闭环。

## 2. 使用角色

| 角色 | 真实能力边界 | 对应权限口径 |
|---|---|---|
| 财务运营 / 结算专员 | 查询佣金记录、查看待结算金额、执行单条结算、执行批量结算 | `booking:commission:query`; `booking:commission:settle` |
| 审计 / 对账复核 | 查看技师维度、订单维度佣金记录与待结算金额 | `booking:commission:query` |
| 佣金规则管理员 | 查看、保存、删除门店佣金配置 | `booking:commission:query`; `booking:commission:config` |

说明：
- 实际后台角色名由权限系统决定，本文不假写后台菜单、页面 path、已落地入口。
- 本文只定义能力角色，不推断现网菜单结构。

## 3. 页面真值现状

### 3.1 审查范围
- 页面文件审查范围：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/*`
- 前端 API 文件审查范围：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/*`

### 3.2 当前结论

| 审查对象 | 当前结论 | 说明 |
|---|---|---|
| BO-003 结算审批主页面 | 已核出 | `commission-settlement/index.vue` 真实存在 |
| BO-003 通知出站页面 | 已核出 | `commission-settlement/outbox/index.vue` 真实存在 |
| BO-004 独立后台页面文件 | 已核出 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission/index.vue` 已真实承接 `/booking/commission/*` |
| BO-004 独立前端 API 文件 | 已核出 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commission.ts` 已真实承接 `/booking/commission/*` |

### 3.3 页面真值口径
- `BO-004` 当前是“admin-only 页面/API 真值已闭环，但仍不可放量”的状态。
- 当前只认 `TechnicianCommissionController` 的 8 条真实 `/booking/commission/*` 接口，不认任何猜测性页面 path。
- 不得把 `commission-settlement/index.vue`、`commission-settlement/outbox/index.vue` 反推成 BO-004 页面。
- 本文不能被用于证明：
  - 已完成真实环境菜单执行；
  - 已完成发布证据闭环；
  - 已达到 release-ready。

## 4. 真实接口边界

| 场景 | Method + Path | 请求参数 | 响应 | 当前语义 |
|---|---|---|---|---|
| 按技师查询佣金记录 | `GET /booking/commission/list-by-technician` | `technicianId(Long)` | `list[]: TechnicianCommissionRespVO` | 返回技师下的佣金记录列表 |
| 按订单查询佣金记录 | `GET /booking/commission/list-by-order` | `orderId(Long)` | `list[]: TechnicianCommissionRespVO` | 返回订单下的佣金记录列表 |
| 查询待结算金额 | `GET /booking/commission/pending-amount` | `technicianId(Long)` | `Integer` | 返回该技师待结算佣金总额，单位分 |
| 单条佣金直结 | `POST /booking/commission/settle` | `commissionId(Long)` | `Boolean` | 直接把单条待结算佣金改为已结算 |
| 批量直结 | `POST /booking/commission/batch-settle` | `technicianId(Long)` | `Boolean` | 直接把该技师全部待结算佣金批量改为已结算 |
| 查询门店佣金配置 | `GET /booking/commission/config/list` | `storeId(Long)` | `list[]: TechnicianCommissionConfigDO` | 返回门店级佣金配置列表 |
| 保存门店佣金配置 | `POST /booking/commission/config/save` | body:`id?`,`storeId`,`commissionType`,`rate`,`fixedAmount?` | `Boolean` | `id` 有值更新，无值新增 |
| 删除门店佣金配置 | `DELETE /booking/commission/config/delete` | `id(Long)` | `Boolean` | 按配置主键删除 |

### 4.1 当前 controller 真值限制
- 8 条接口都没有 `degraded` 字段。
- `settle`、`batch-settle`、`config/save`、`config/delete` 当前都只返回 `Boolean` 结果。
- `settle` / `batch-settle` 当前 controller 不返回逐条结果明细。
- `config/save` 当前 controller 不区分 create/update 的独立 path，只以 `id` 是否为空判断。

### 4.2 合法空态与写后回读总则
- 当前必须承认以下都是合法空态，不得误判为失败：
  - `GET /booking/commission/list-by-technician => []`
  - `GET /booking/commission/list-by-order => []`
  - `GET /booking/commission/pending-amount => 0`
  - `GET /booking/commission/config/list => []`
- 当前必须承认以下是统一写规则：
  - `POST /booking/commission/settle`
  - `POST /booking/commission/batch-settle`
  - `POST /booking/commission/config/save`
  - `DELETE /booking/commission/config/delete`
- 上述 4 条写接口都必须按“写后回读确认”定义产品结果，不能把 `Boolean true` 直接等同为业务完成。

## 5. 关键字段真值

### 5.1 佣金记录字段

| 字段 | 含义 | 说明 |
|---|---|---|
| `id` | 佣金记录 ID | 主追溯键 |
| `technicianId` | 技师 ID | 技师维度查询键 |
| `orderId` | 预约订单 ID | 订单维度查询键 |
| `orderItemId` | 交易订单项 ID | 订单项追溯键 |
| `serviceOrderId` | 服务履约单 ID | 履约追溯键 |
| `userId` | 用户 ID | 用户关联键 |
| `storeId` | 门店 ID | 门店维度追溯键 |
| `commissionType` | 佣金类型 | `1=基础 2=点钟 3=加钟 4=卡项销售 5=商品 6=好评` |
| `baseAmount` | 订单金额（分） | 佣金计算基数 |
| `commissionRate` | 佣金比例 | 支持门店级覆盖 |
| `commissionAmount` | 佣金金额（分） | 正向记录为正数，冲正记录可能为负数 |
| `status` | 佣金状态 | `0=待结算 1=已结算 2=已取消` |
| `sourceBizNo` | 追溯业务号 | 审计主键之一 |
| `settlementId` | 结算单 ID | 若走 BO-003 创建结算单，此字段用于绑定 |
| `settlementTime` | 结算时间 | 直结或走 BO-003 结算完成后落值 |
| `createTime` | 创建时间 | 列表默认按此时间倒序时使用 |

### 5.2 佣金配置字段

| 字段 | 含义 | 说明 |
|---|---|---|
| `id` | 配置 ID | 删除 / 更新主键 |
| `storeId` | 门店 ID | 配置作用域 |
| `commissionType` | 佣金类型 | 与佣金记录中的 `commissionType` 一致 |
| `rate` | 佣金比例 | 必填 |
| `fixedAmount` | 固定金额（分） | 可选 |
| `createTime` | 创建时间 | 继承 `BaseDO` |
| `updateTime` | 更新时间 | 继承 `BaseDO` |
| `creator` | 创建人 | 继承 `BaseDO` |
| `updater` | 更新人 | 继承 `BaseDO` |

## 6. 产品规则

### 6.1 按技师维度列表
- 必须传 `technicianId`。
- 当前真实排序规则是按 `createTime` 倒序。
- 列表返回的是“记录列表”，不是金额聚合结果。
- 同一技师可能出现多条不同 `commissionType`、不同 `status` 的记录。
- 当前接口只负责查询，不负责生成结算单、审批、打款。

### 6.2 按订单维度列表
- 必须传 `orderId`。
- 当前真实 controller 返回的是列表，而不是单条记录对象。
- 当前 mapper 未声明稳定排序规则，因此产品文档不得假写“按时间倒序/正序”。
- 订单维度结果应按“记录集合”理解，不能默认一单只会返回一条佣金记录。

### 6.3 待结算金额
- 必须传 `technicianId`。
- 当前真实计算口径是：
  - 只统计该技师 `status = 0` 的待结算佣金记录；
  - 汇总字段为 `commissionAmount`；
  - 返回值单位为“分”。
- 当前接口返回的是聚合金额，不返回构成明细。
- 该能力可作为“是否需要进入 BO-003 结算审批”的前置判断，但它本身不是审批链路。

### 6.4 单条结算
- 必须传 `commissionId`。
- 当前真实服务语义：
  - 只处理 `status = 0` 的待结算记录；
  - 命中后把 `status` 改为 `1`，并写入 `settlementTime = now`；
  - 不创建 `settlementId`；
  - 不创建 BO-003 结算单；
  - 不返回逐条日志明细。
- 当前代码对“记录不存在”或“状态非待结算”是 no-op 语义：
  - controller 仍返回 `true`
  - 不代表状态一定发生变更
- 因此产品要求：
  - 直结后必须执行“写后回读确认”，以重新查询列表或明细作为结果确认依据；
  - 不能把 `true` 直接渲染成“已完成结算闭环”。

### 6.5 批量结算
- 必须传 `technicianId`。
- 当前真实服务语义：
  - 先拉取该技师全部 `status = 0` 的待结算记录；
  - 再逐条调用单条结算逻辑；
  - 不返回批量命中数、失败数、跳过数；
  - 只返回 `Boolean true`。
- 当前接口更像“批量直结执行器”，不是“批量结算审批任务”。
- 产品要求：
  - 批量执行后必须执行“写后回读确认”，重新拉取技师列表或待结算金额校验结果；
  - 不得把它与 BO-003 的“创建结算单 -> 审核 -> 打款”混写。

### 6.6 佣金配置
- 作用域是“门店级 + 佣金类型”。
- 当前真实默认佣金比例回退规则：
  - `1 基础服务` -> `0.15`
  - `2 点钟加成` -> `0.20`
  - `3 加钟服务` -> `0.20`
  - `4 卡项销售` -> `0.05`
  - `5 商品推荐` -> `0.10`
  - `6 好评奖励` -> `0`
- 若门店存在同类型配置且 `rate` 有值，则优先使用门店配置；否则回退枚举默认值。

### 6.7 配置列表
- 必须传 `storeId`。
- 当前真实查询语义是“查某门店全部配置记录”。
- 当前接口不承诺排序规则。
- 当前接口返回的是配置实体，不是假写过的页面字段模型。

### 6.8 保存配置
- 当前真实必填字段：
  - `storeId`
  - `commissionType`
  - `rate`
- `fixedAmount` 可选。
- `id` 有值表示更新；`id` 为空表示新增。
- 当前 controller 没有单独的“新增”或“更新” path，也没有返回新配置实体。
- 当前返回 `true` 仅表示请求已执行，不等于前端已拿到最终列表快照；保存后必须“写后回读确认”。

### 6.9 删除配置
- 必须传 `id`。
- 当前真实语义是按主键删除。
- 当前 controller 不返回被删对象，也不返回剩余列表。
- 删除后必须执行“写后回读确认”，重新调用 `GET /booking/commission/config/list` 校验结果。

## 7. 审计字段与留痕要求

### 7.1 佣金记录侧
- 必须保留以下审计键：
  - `id`
  - `technicianId`
  - `orderId`
  - `orderItemId`
  - `serviceOrderId`
  - `storeId`
  - `sourceBizNo`
  - `settlementId`
- 必须保留以下金额 / 状态字段：
  - `commissionType`
  - `baseAmount`
  - `commissionRate`
  - `commissionAmount`
  - `status`
  - `settlementTime`
- 必须保留以下通用留痕：
  - `createTime`
  - `updateTime`
  - `creator`
  - `updater`

### 7.2 配置侧
- 配置审计键至少包括：
  - `id`
  - `storeId`
  - `commissionType`
  - `rate`
  - `fixedAmount`
  - `createTime`
  - `updateTime`
  - `creator`
  - `updater`

### 7.3 当前留痕边界
- BO-004 当前没有独立操作日志接口。
- BO-004 当前不能假写成：
  - 已有“佣金配置变更日志页”
  - 已有“佣金直结操作日志页”
  - 已有“批量结算结果详情页”
- 若需要审批日志、驳回原因、打款凭证、通知出站重试日志，必须进入 BO-003 结算单链路。

## 8. 与 BO-003 的边界

| 维度 | BO-004 | BO-003 |
|---|---|---|
| 负责对象 | 佣金记录、待结算金额、门店佣金配置、直结动作 | 结算单、审核、驳回、打款、通知出站 |
| 真实 controller | `TechnicianCommissionController` | `TechnicianCommissionSettlementController` |
| 是否核到独立后台页面 | 未核到 | 已核到 `commission-settlement/index.vue`、`commission-settlement/outbox/index.vue` |
| 是否创建结算单 | 否 | 是，`POST /booking/commission-settlement/create` |
| 是否有审核 / 驳回 / 打款 | 否 | 是 |
| 是否有操作日志 / 通知出站 | 否 | 是 |
| 结算语义 | 直接把佣金记录状态改为已结算 | 先形成结算单，再走审批 / 打款流程 |

### 8.1 具体边界要求
- BO-004 的 `settle` / `batch-settle` 不能被写成“结算审批页面上的操作”。
- BO-003 的 `createSettlement` 虽然消费 `commissionIds`，但“选择哪些佣金记录、怎么看待结算金额、如何维护佣金配置”属于 BO-004。
- BO-003 页面存在，不能反推 BO-004 页面也存在。

## 9. 产品侧最终标签

| 能力 | Doc Closed | Engineering Blocked | Can Develop | Cannot Release |
|---|---|---|---|---|
| BO-004 `/booking/commission/*` 8 条接口边界 | Yes | No | Yes | Yes |
| BO-004 独立后台页面文件 | Yes | No | Yes | Yes |
| BO-004 独立前端 API 文件 | Yes | No | Yes | Yes |
| BO-004 写接口结果判定（写后回读确认） | Yes | No | Yes | Yes |

说明：
- `Can Develop` 仅表示产品真值已固定，可继续补页面、补前端 API、补读后确认流程。
- `Cannot Release` 表示在独立页面、独立 API 文件、写后回读样本全部补齐前，BO-004 仍不得写成可放量。

## 10. 非目标
- 不定义 BO-003 的结算单审批、驳回、打款、通知补偿细节。
- 不定义后台页面 path、菜单 path、路由注册、权限菜单树。
- 不定义手工创建佣金记录、手工冲正、人工补录计提记录页面。
- 不把 controller 已存在推断成独立后台页面已上线。
- 不把 `Boolean true` 型返回推断成“状态一定发生变化”。
