# MiniApp 财务运营技师提成与结算 PRD v1（2026-03-12）

## 0. 文档定位
- 目标：把技师提成明细、佣金配置、结算单创建与审核、打款、通知补偿统一为后台财务运营产品文档。
- 真实代码基线：
  - 页面：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue`
  - API：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/commissionSettlement.ts`
  - Controller：
    - `TechnicianCommissionController`
    - `TechnicianCommissionSettlementController`
- 约束：只认当前真实结算页、通知出站页、佣金 controller 与接口；不扩写不存在的技师钱包后台页。

## 1. 产品目标
1. 支持查看技师佣金记录、按技师或订单查询、查看待结算总额。
2. 支持配置门店佣金规则。
3. 支持从佣金记录创建结算单。
4. 支持结算单提交审核、审核通过、审核驳回、打款确认。
5. 支持查看审核 SLA 超时列表。
6. 支持通知出站记录查询与人工重试。

## 2. 页面 / 能力真值

### 2.1 已验证后台页面
- 结算单主页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue`
- 通知出站页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue`

### 2.2 当前未验证独立后台页面
以下能力当前只确认 controller 存在，未在本轮核到独立页面文件：
- 技师佣金列表 `list-by-technician`
- 订单佣金记录 `list-by-order`
- 待结算金额 `pending-amount`
- 佣金配置 `config/list` / `config/save` / `config/delete`

这些能力仍属于系统真实业务能力，但不能在没有页面证据时被表述为“独立后台页面已完整上线”。

## 3. 接口真值

### 3.1 技师佣金管理
| 动作 | Method + Path | 说明 |
|---|---|---|
| 按技师查佣金 | `GET /booking/commission/list-by-technician` | 查询技师佣金记录 |
| 按订单查佣金 | `GET /booking/commission/list-by-order` | 查询订单佣金记录 |
| 查待结算金额 | `GET /booking/commission/pending-amount` | 查询技师待结算总额 |
| 单条结算佣金 | `POST /booking/commission/settle` | 结算单条佣金 |
| 按技师批量结算 | `POST /booking/commission/batch-settle` | 批量结算 |
| 查门店佣金配置 | `GET /booking/commission/config/list` | 查询配置 |
| 保存门店佣金配置 | `POST /booking/commission/config/save` | 新增或更新配置 |
| 删除门店佣金配置 | `DELETE /booking/commission/config/delete` | 删除配置 |

### 3.2 结算单管理
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

## 4. 关键字段真值

### 4.1 结算单字段
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

### 4.2 通知出站字段
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

## 5. 业务状态与规则

### 5.1 结算单状态
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

### 5.2 通知补偿规则
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

## 6. 页面操作规则
1. 创建结算单时必须明确选择佣金记录集合 `commissionIds`。
2. 提交审核可指定 `slaMinutes`，用于后续超时预警。
3. 审核通过只代表进入可打款状态，不等于已付款。
4. 打款动作必须带凭证号和备注，禁止空凭证确认。
5. 通知出站页面必须允许按 `status`、`notifyType`、`channel`、`lastActionCode`、`lastActionBizNo` 检索。

## 7. 角色分工
- 财务运营：创建结算单、审核、打款、处理通知补偿。
- 门店 / 区域负责人：查看结算状态，配合确认异常。
- 后端：保证结算状态流转、SLA、日志、通知出站一致。
- 前端后台：禁止把“提交审核成功”展示成“已结算完成”。

## 8. 验收标准
1. 结算单页面支持查询、审核、驳回、打款闭环。
2. 驳回时无 `rejectReason` 不能提交。
3. 打款时无 `payVoucherNo` 或无 `payRemark` 不能提交。
4. SLA 超时列表可独立查看待审核超时单。
5. 通知出站页面能查看失败记录并支持人工重试。
6. 批量重试返回明细结果，而不是简单 true/false。

## 9. 非目标
- 不定义技师实际发薪系统。
- 不定义税务或工资条规则。
- 不把 controller 存在但无页面证据的能力误写成“后台页面已完整上线”。
