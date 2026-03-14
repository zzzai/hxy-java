# HXY Admin After-Sale Review Ticket PRD v1（2026-03-15）

## 1. 文档目标与边界
- 目标：把当前后台 `ADM-014` 售后单管理 / 详情、`ADM-015` 售后人工复核工单、`ADM-016` 售后工单 SLA 路由规则，收口为一份正式 PRD。
- 本文覆盖：
  - 售后单分页筛选、详情审查、同意 / 拒绝 / 收货 / 拒收 / 退款处理
  - 人工复核工单池分页、详情、单条收口、批量收口
  - 工单 SLA 路由规则分页、增删改、启停、路由预览
  - 退款规则来源、人工复核升级、SLA 路由边界
- 本文不覆盖：
  - 小程序用户侧售后申请页细节
  - 支付回调 `update-refunded` 的底层技术实现
  - 门店生命周期守卫与库存治理逻辑

## 2. 单一真值来源
### 2.1 页面真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/afterSale/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/afterSale/detail/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicket/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicketRoute/index.vue`

### 2.2 API / Controller 真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/trade/afterSale/index.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/trade/reviewTicket/index.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/trade/reviewTicketRoute/index.ts`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/controller/admin/aftersale/AfterSaleController.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/controller/admin/aftersale/AfterSaleRefundRuleController.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/controller/admin/aftersale/AfterSaleReviewTicketController.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/controller/admin/aftersale/AfterSaleReviewTicketRouteController.java`

### 2.3 上游产品 / 规则参考
- `hxy/03_payment/HXY-退款分层策略与工单升级规则-v1-2026-02-22.md`
- `docs/plans/2026-03-03-ticket-sla-store-lifecycle-guard-design.md`
- `docs/products/miniapp/2026-03-12-miniapp-after-sale-refund-prd-v1.md`

## 3. 当前业务结论
1. 当前后台已经形成完整售后运营三层结构：
   - 售后单页：处理单据和退款动作
   - 人工复核工单页：收口异常和高风险工单
   - SLA 路由页：维护路由规则与升级对象
2. 售后单页并不是“只有列表”；详情页已经展示退款审计最小集，包括：
   - `refundLimitSource`
   - `refundLimitRuleHint`
   - `refundLimitDetailJson`
   - `refundAuditStatus`
   - `refundExceptionType`
   - `refundAuditRemark`
3. 当前退款分层真值固定为：
   - 低风险退款可自动执行
   - 高风险退款进入总部人工复核工单池
   - 自动退款失败会升级到人工复核链路
4. 人工复核工单页当前主能力是“查询 + 收口”，不是配置页；`create` 虽有后台接口，但当前页面主证据仍是工单池和收口动作。
5. 工单 SLA 路由页当前是独立治理页，页面直接使用的真值能力为：
   - `page`
   - `get`
   - `create`
   - `update`
   - `delete`
   - `list-enabled`
   页面内的路由命中预览是基于启用规则集本地计算，不依赖 controller `/resolve` 结果。
6. 当前页面/API 未核到统一的服务端 `degraded=true / degradeReason` 语义，不能在产品文档里补造后端降级字段。

## 4. 角色与使用场景
| 角色 | 目标 | 使用页面 |
|---|---|---|
| 总部售后运营 | 查看售后单、审查退款原因、执行同意/拒绝/收货/退款动作 | `mall/trade/afterSale/index`; `mall/trade/afterSale/detail` |
| 总部财务 / 风控 | 审查高风险退款、收口异常工单 | `mall/trade/reviewTicket/index` |
| 运营治理 / 流程管理员 | 配置 SLA 路由规则、启停路由、验证升级链路是否命中 | `mall/trade/reviewTicketRoute/index` |
| 技术 / 运维支持 | 借助规则页预览和工单收口信息排查异常退款路径 | `mall/trade/reviewTicket/index`; `mall/trade/reviewTicketRoute/index` |

## 5. 页面与能力边界
### 5.1 售后单列表与详情页
页面：
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/afterSale/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/afterSale/detail/index.vue`

列表查询能力：
1. 商品名 `spuName`
2. 退款编号 `no`
3. 订单编号 `orderNo`
4. 售后状态 `status`
5. 售后方式 `way`
6. 售后类型 `type`
7. 退款上限来源 `refundLimitSource`
8. 创建时间 `createTime[]`
9. 基于状态 tab 的快速切换

详情与操作能力：
1. 查看售后基本信息、订单信息、买家信息、退款审计信息
2. 同意售后：`PUT /trade/after-sale/agree`
3. 拒绝售后：`PUT /trade/after-sale/disagree`
4. 确认收货：`PUT /trade/after-sale/receive`
5. 拒绝收货：`PUT /trade/after-sale/refuse`
6. 确认退款：`PUT /trade/after-sale/refund`
7. 读取详情：`GET /trade/after-sale/get-detail`
8. 列表分页：`GET /trade/after-sale/page`

固定边界：
1. 售后详情页是运营处理台，不是配置中心。
2. `refund-rule/get` 属于规则读取能力，当前没有独立 overlay 规则管理页面被纳入本 PRD 主体。
3. `update-refunded` 是支付回调链路，不属于后台人工页面操作。

### 5.2 人工复核工单页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicket/index.vue`

能力：
1. 分页查询工单：`GET /trade/after-sale/review-ticket/page`
2. 查看工单详情：`GET /trade/after-sale/review-ticket/get`
3. 单条收口：`PUT /trade/after-sale/review-ticket/resolve`
4. 批量收口：`POST /trade/after-sale/review-ticket/batch-resolve`

页面最小字段：
- `ticketType`
- `afterSaleId`
- `sourceBizNo`
- `orderId` `orderItemId`
- `userId`
- `ruleCode`
- `decisionReason`
- `severity`
- `escalateTo`
- `routeId` `routeScope` `routeDecisionOrder`
- `slaDeadlineTime`
- `status` `overdue`
- `triggerCount`
- `resolveActionCode` `resolveBizNo` `remark`
- `lastActionCode` `lastActionBizNo` `lastActionTime`

固定边界：
1. 当前页面主职责是收口工单，不是生成业务规则。
2. 批量收口必须继续按结构化结果解释：`success / skippedNotFound / skippedNotPending`，不能只看 message。

### 5.3 工单 SLA 路由规则页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicketRoute/index.vue`

能力：
1. 分页查询规则：`GET /trade/after-sale/review-ticket-route/page`
2. 查询详情：`GET /trade/after-sale/review-ticket-route/get`
3. 创建规则：`POST /trade/after-sale/review-ticket-route/create`
4. 更新规则：`PUT /trade/after-sale/review-ticket-route/update`
5. 删除规则：`DELETE /trade/after-sale/review-ticket-route/delete`
6. 读取启用规则：`GET /trade/after-sale/review-ticket-route/list-enabled`
7. 页面内支持单条启停、批量启停、批量删除，以及基于启用规则列表的本地预览

规则对象最小字段：
- `scope`
- `ruleCode`
- `ticketType`
- `severity`
- `escalateTo`
- `slaMinutes`
- `enabled`
- `sort`
- `remark`

固定边界：
1. 页面作用域真值只认：`RULE`、`TYPE_SEVERITY`、`TYPE_DEFAULT`、`GLOBAL_DEFAULT`。
2. 页面中的 `SYSTEM_FALLBACK` 只用于预览兜底展示，不是持久化规则作用域。
3. 虽然后端 controller 存在 `/resolve`、`batch-delete`、`batch-update-enabled`，但当前页面真值仍以页面实际调用的 CRUD 和 `list-enabled` 为准，不把未使用端点写成主能力。

## 6. 关键业务对象与字段最小集
### 6.1 售后单
- `id` `no`
- `status` `way` `type`
- `userId`
- `applyReason` `applyDescription` `applyPicUrls`
- `orderId` `orderNo` `orderItemId`
- `spuId` `spuName` `skuId` `properties` `picUrl` `count`
- `refundPrice`
- `refundLimitSource` `refundLimitSourceLabel`
- `refundLimitRuleHint`
- `refundLimitDetailJson` / `refundLimitDetail`
- `refundAuditStatus` `refundExceptionType` `refundAuditRemark`
- `payRefundId`
- `refundTime`
- `logisticsId` `logisticsNo` `deliveryTime` `receiveTime` `receiveReason`

### 6.2 人工复核工单
- `id`
- `ticketType`
- `afterSaleId`
- `sourceBizNo`
- `orderId` `orderItemId`
- `userId`
- `ruleCode`
- `decisionReason`
- `severity`
- `escalateTo`
- `routeId` `routeScope` `routeDecisionOrder`
- `slaDeadlineTime`
- `status` `overdue`
- `firstTriggerTime` `lastTriggerTime` `triggerCount`
- `resolvedTime` `resolverId` `resolverType`
- `resolveActionCode` `resolveBizNo`
- `lastActionCode` `lastActionBizNo` `lastActionTime`
- `remark` `createTime` `updateTime`

### 6.3 SLA 路由规则
- `id`
- `scope`
- `ruleCode`
- `ticketType`
- `severity`
- `escalateTo`
- `slaMinutes`
- `enabled`
- `sort`
- `remark`
- `createTime`

## 7. 关键流程
### 7.1 售后运营处理流程
1. 运营在售后单页按状态、方式、类型、上限来源筛选待处理单。
2. 进入详情页查看退款审计信息和退款上限明细。
3. 根据业务判断执行 `agree / disagree / receive / refuse / refund`。
4. 若命中高风险规则或自动退款失败，售后单进入人工复核链路。

### 7.2 人工复核工单收口流程
1. 总部在工单池按 `ticketType / status / severity / overdue / sourceBizNo` 筛选工单。
2. 打开详情确认规则编码、路由结果、SLA 截止时间和历史动作。
3. 填写 `resolveActionCode`、可选 `resolveBizNo`、`resolveRemark`。
4. 调用单条或批量收口接口。
5. 收口后以最新详情回读结果为准，确认工单状态已收敛。

### 7.3 SLA 路由配置流程
1. 流程管理员选择作用域：`RULE / TYPE_SEVERITY / TYPE_DEFAULT / GLOBAL_DEFAULT`。
2. 维护升级对象、SLA 分钟数、启停状态和排序。
3. 保存规则后刷新分页列表。
4. 页面拉取启用规则列表，在本地做路由预览，验证给定 `ruleCode / ticketType / severity` 的命中结果。

## 8. 状态、空态、错误与降级
### 8.1 售后单与退款审计
- 列表为空是合法空态。
- 退款上限明细解析失败时，详情页会保留原文并显示 warning；这不等于“规则不存在”。
- 当前未核到统一服务端降级字段，退款失败只能按普通失败或升级工单处理。

### 8.2 工单收口
- 单条和批量收口都必须按结构化字段解释结果，尤其是批量结果中的 `skippedNotFoundCount` 与 `skippedNotPendingCount`。
- 不能把“批量接口返回成功”直接等同于全部工单已收口，必须读取结果结构或再次回读详情。

### 8.3 路由规则治理
- `severity` 只有在 `TYPE_SEVERITY` 作用域下才是显式配置项，其他作用域固定为页面约束值。
- `SYSTEM_FALLBACK` 只可作为页面预览兜底，不能写入正式规则集。
- 当前没有证据表明后端返回统一 `degraded=true / degradeReason`，路由异常时只能通过规则配置、启停和人工处理兜底。

## 9. 禁止性边界
1. 不得把小程序售后申请逻辑和后台售后运营 PRD 混成一份文档。
2. 不得把 `update-refunded` 支付回调接口描述成后台人工页面能力。
3. 不得把路由规则页未实际使用的 controller 端点当成页面主能力冻结。
4. 不得把工单池“查询成功”误写成“工单已收口”；收口必须有动作编码和结果回读。
5. 不得继续用历史规则说明文档替代正式 PRD；本文落盘后，以本文为准。

## 10. 验收标准
### 10.1 ADM-014 售后单管理 / 详情
- [ ] 列表筛选字段和详情字段最小集明确
- [ ] 退款审计来源、规则提示、明细快照边界明确
- [ ] 同意/拒绝/收货/拒收/退款动作边界明确

### 10.2 ADM-015 售后人工复核工单
- [ ] 分页、详情、单条收口、批量收口边界明确
- [ ] 收口字段和结果解释口径明确
- [ ] 高风险退款进入工单池的边界明确

### 10.3 ADM-016 售后工单 SLA 路由规则
- [ ] 作用域、严重级别、升级对象、SLA 分钟字段明确
- [ ] 页面实际调用的 CRUD + `list-enabled` 真值明确
- [ ] 预览逻辑是本地命中，不误写成后端强依赖能力

## 11. 最终结论
1. `ADM-014` 的产品真值是“总部售后运营处理台”，其核心是退款审计信息和人工动作闭环。
2. `ADM-015` 的产品真值是“高风险 / 异常退款人工复核工单池”。
3. `ADM-016` 的产品真值是“售后工单 SLA 路由治理台”，不是通用工单系统。
4. 本文落盘后，`ADM-014`、`ADM-015`、`ADM-016` 在全项目业务台账中的 PRD 完整度应提升为 `完整`。
