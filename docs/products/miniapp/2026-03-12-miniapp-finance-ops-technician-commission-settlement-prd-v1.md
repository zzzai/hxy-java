# MiniApp 财务运营技师提成结算审批 PRD v1（2026-03-12）

## 0. 文档定位
- 目标：把 `BO-003 技师提成结算 / 审核 / 驳回 / 打款 / 通知补偿` 收口到真实结算审批页面、真实通知出站页面、真实 `commission-settlement` controller 口径。
- 真实代码基线：
  - 页面：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue`
  - API：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commissionSettlement.ts`
  - Controller：`TechnicianCommissionSettlementController`
- 边界说明：
  - `BO-004 技师提成明细 / 计提管理` 已拆到 `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-detail-config-prd-v1.md`
  - 本文不再承担 `list-by-technician`、`list-by-order`、`pending-amount`、`settle`、`batch-settle`、`config/*` 的最终单一真值
- 约束：只认当前真实结算页、通知出站页、`commissionSettlement.ts` 与 `TechnicianCommissionSettlementController`；不反推 BO-004 独立页面已存在。

## 1. 产品目标
1. 支持从既有佣金记录集合创建结算单。
2. 支持结算单提交审核、审核通过、审核驳回、打款确认。
3. 支持查看审核 SLA 超时列表。
4. 支持通知出站记录查询与人工重试。
5. 固定 BO-003 与 BO-004 的边界，避免把结算审批页面误写成佣金明细 / 计提管理页面。

## 2. BO-003 / BO-004 拆分边界

| 维度 | BO-003 | BO-004 |
|---|---|---|
| 负责对象 | 结算单、审核、驳回、打款、通知出站 | 佣金记录、待结算金额、门店佣金配置、单条 / 批量直结 |
| 真实 controller | `TechnicianCommissionSettlementController` | `TechnicianCommissionController` |
| 真实页面 | `commission-settlement/index.vue`; `commission-settlement/outbox/index.vue` | 当前未核到独立后台页面文件 |
| 单一真值文档 | 本文 | `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-detail-config-prd-v1.md` |
| 页面闭环结论 | 已核到真实页面与 API 文件 | 当前仍是“仅接口闭环 + 页面真值待核” |

## 3. 页面 / 能力真值

### 3.1 已验证后台页面
- 结算单主页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue`
- 通知出站页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue`

### 3.2 页面真值边界
- BO-003 只认以上两页和 `commissionSettlement.ts`。
- BO-004 不在本文内补写页面 path、菜单 path、前端入口。

## 4. 接口真值

| 动作 | Method + Path | 说明 |
|---|---|---|
| 创建结算单 | `POST /booking/commission-settlement/create` | 根据佣金记录创建结算单 |
| 提交审核 | `POST /booking/commission-settlement/submit` | 进入审核流程，可传 SLA 分钟数 |
| 审核通过 | `POST /booking/commission-settlement/approve` | 审核通过 |
| 审核驳回 | `POST /booking/commission-settlement/reject` | 驳回并记录原因 |
| 打款 | `POST /booking/commission-settlement/pay` | 记录打款凭证与备注 |
| 查询结算单详情 | `GET /booking/commission-settlement/get` | 详情 |
| 分页查询结算单 | `GET /booking/commission-settlement/page` | 分页列表 |
| 查询结算单列表 | `GET /booking/commission-settlement/list` | 简化列表 |
| 查询 SLA 超时待审核单 | `GET /booking/commission-settlement/sla-overdue-list` | 超时单 |
| 查询操作日志 | `GET /booking/commission-settlement/log-list` | 审核 / 驳回 / 打款日志 |
| 查询通知出站列表 | `GET /booking/commission-settlement/notify-outbox-list` | 简化列表 |
| 分页查询通知出站 | `GET /booking/commission-settlement/notify-outbox-page` | 分页列表 |
| 人工重试通知 | `POST /booking/commission-settlement/notify-outbox-retry` | 重试通知 |
| 批量重试通知 | `POST /booking/commission-settlement/notify-outbox-batch-retry` | 返回批量结果明细 |

说明：
- `POST /booking/commission-settlement/create` 只接收 `commissionIds + remark`，不定义 BO-004 如何查询、筛选、配置这些佣金记录。
- BO-003 当前真实前端 API 文件仍只有 `commissionSettlement.ts`，不得回退。

## 5. 关键字段真值

### 5.1 结算单字段
- `settlementNo`
- `storeId`
- `technicianId`
- `status`
- `commissionCount`
- `totalCommissionAmount`
- `reviewSubmitTime`
- `reviewDeadlineTime`
- `reviewWarned`
- `reviewEscalated`
- `reviewedTime`
- `reviewerId`
- `reviewRemark`
- `rejectReason`
- `paidTime`
- `payerId`
- `payVoucherNo`
- `payRemark`
- `remark`
- `overdue`

### 5.2 通知出站字段
- `settlementId`
- `notifyType`
- `channel`
- `severity`
- `status`
- `retryCount`
- `nextRetryTime`
- `sentTime`
- `lastErrorMsg`
- `lastActionCode`
- `lastActionBizNo`
- `lastActionTime`

## 6. 业务状态与规则

### 6.1 结算单状态
从当前前端页面状态标签映射可见，至少存在以下状态段：
- `0`
- `10`
- `20`
- `30`
- `40`
- `50`

当前产品规则：
- 只有完成“创建 -> 提交审核 -> 审核通过 -> 打款”链路，才能视为完整结算闭环。
- 驳回必须填写 `rejectReason`。
- 打款必须填写：
  - `payVoucherNo`
  - `payRemark`
- `overdue=true` 仅代表审核 SLA 超时，不等于已驳回或已支付。

### 6.2 通知补偿规则
- 通知出站失败允许人工重试。
- 批量重试必须返回：
  - `totalCount`
  - `retriedCount`
  - `skippedNotExistsCount`
  - `skippedStatusInvalidCount`
  - `retriedIds`
  - `skippedNotExistsIds`
  - `skippedStatusInvalidIds`
- “已重试”不等于“业务已送达”，必须保留状态明细。

## 7. 页面操作规则
1. 创建结算单时必须明确选择佣金记录集合 `commissionIds`。
2. 提交审核可指定 `slaMinutes`，用于后续超时预警。
3. 审核通过只代表进入可打款状态，不等于已付款。
4. 打款动作必须带凭证号和备注，禁止空凭证确认。
5. 通知出站页面必须允许按 `status`、`notifyType`、`channel`、`lastActionCode`、`lastActionBizNo` 检索。

## 8. 角色分工
- 财务运营：创建结算单、审核、打款、处理通知补偿。
- 门店 / 区域负责人：查看结算状态，配合确认异常。
- 后端：保证结算状态流转、SLA、日志、通知出站一致。
- 前端后台：禁止把“提交审核成功”展示成“已结算完成”。

## 9. 验收标准
1. 结算单页面支持查询、审核、驳回、打款闭环。
2. 驳回时无 `rejectReason` 不能提交。
3. 打款时无 `payVoucherNo` 或无 `payRemark` 不能提交。
4. SLA 超时列表可独立查看待审核超时单。
5. 通知出站页面能查看失败记录并支持人工重试。
6. 批量重试返回明细结果，而不是简单 true/false。

## 10. 非目标
- 不定义技师实际发薪系统。
- 不定义税务或工资条规则。
- 不把 BO-004 controller 存在但无页面证据的能力误写成“后台页面已完整上线”。
- 不让本文继续承担 BO-004 的最终单一真值。
