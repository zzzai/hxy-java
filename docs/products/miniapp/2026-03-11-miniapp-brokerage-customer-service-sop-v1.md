# MiniApp Brokerage Customer Service SOP v1 (2026-03-11)

## 1. 目标与适用范围
- 目标：把分销域的客服接待、资金解释、提现异常升级、回访与工单闭环固化为统一 SOP，消除“申请成功=到账成功”“页面可访问=能力已上线”的误判。
- 适用页面与接口：
  - 分销中心：`/pages/commission/index`
  - 分销钱包 / 提现：`/pages/commission/wallet`、`/pages/commission/withdraw`
  - 团队 / 排行 / 推广商品 / 推广订单：`/pages/commission/team`、`/pages/commission/commission-ranking`、`/pages/commission/promoter`、`/pages/commission/goods`、`/pages/commission/order`
  - 真实 app API：`/trade/brokerage-user/*`、`/trade/brokerage-record/*`、`/trade/brokerage-withdraw/*`
- 对齐基线：
  - `docs/products/miniapp/2026-03-10-miniapp-brokerage-distribution-prd-v1.md`
  - `docs/contracts/2026-03-10-miniapp-brokerage-domain-contract-v1.md`
  - `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
- 当前 capability 真值：
  - `CAP-BROKERAGE-001 brokerage.runtime-pages = PLANNED_RESERVED`
  - 本文补齐的是客服 / 资金解释 / 升级闭环，不自动等价为 capability 升级 `ACTIVE`

## 2. 处理原则
- 分销域只按真实字段、真实状态和真实 errorCode 解释，不按文案推断资金结果。
- `POST /trade/brokerage-withdraw/create` 成功只代表“申请单创建成功”，不代表“已到账”。
- 只有满足 `status===10 && type===5 && payTransferId>0` 才允许展示“确认收款”或引导用户核对微信收款。
- 当前资金字段唯一真值：
  - `withdrawPrice` = 当前佣金
  - `brokeragePrice` = 可提现佣金
  - `frozenPrice` = 冻结佣金
- 团队统计字段真值是 `brokerageOrderCount`，不得沿用前端旧字段 `item.orderCount` 解释团队订单数。
- 本域当前没有服务端 `degraded/degradeReason` 字段；所有恢复动作只允许基于：
  - 显式 `errorCode`
  - `[]`
  - `null`
  - 状态字段
  - 人工 / 财务 / 运营接管动作
- 所有工单与升级记录必须携带：`runId/orderId/payRefundId/sourceBizNo/errorCode`，无值时统一填字符串 `"0"`。

## 3. 客服接待分层

| 层级 | 角色 | 职责 | 首响时限 |
|---|---|---|---|
| `L1` | 客服一线 | 识别场景、采集五键、按标准话术解释余额/提现/绑定状态 | 5 分钟 |
| `L2` | 客服组长 / 财务坐席 | 复核提现状态、补充证据、判断是否升级财务或域 on-call | 15 分钟 |
| `L3` | Brokerage on-call / 财务负责人 / Product on-call | 处理资金异常、字段错配、工单升级和恢复判定 | P0 5 分钟，P1 15 分钟 |

## 4. 标准处理流程

### 4.1 接待步骤
1. 先确认用户处于哪一类页面：分销中心、钱包/提现、团队、排行、推广商品、推广订单。
2. 采集页面路由、最近操作、错误码、时间、截图和五键。
3. 判断是查询场景还是资金写场景：
   - 查询：允许 `[]` / `null` / 上次成功数据保活
   - 写场景：一律 `FAIL_CLOSE`，不得伪成功
4. 按场景表执行标准话术与恢复动作。

### 4.2 转人工条件
满足任一条件必须转 `L2` 或 `L3`：
- 用户连续 2 次重试仍无法完成绑定、提现申请、确认收款。
- 提现状态和页面展示不一致，或用户反馈“已到账/未到账”存在争议。
- 命中 `1011008005-1011008009` 等提现打款状态回写异常。
- 团队/排行字段明显错位，页面无法解释 `brokerageOrderCount` 与旧 `orderCount` 差异。
- 同类问题 15 分钟内出现 3 单及以上。

### 4.3 回访闭环
1. 处理完成后 24 小时内回访。
2. 回访必须说明：问题原因、是否到账、是否仍需人工审核、下次正确入口。
3. 工单状态必须按 `Open -> Ack -> Mitigating -> Resolved -> Closed` 闭环。

## 5. 工单字段模板

| 字段 | 说明 | 规则 |
|---|---|---|
| `ticketType` | `BROKERAGE_CS_INCIDENT` / `BROKERAGE_FUNDS_REVIEW` | 必填 |
| `scene` | `bind/withdraw/withdraw-detail/team/rank/goods/order` | 必填 |
| `route` | 页面路由 | 必填 |
| `runId` | 发布/巡检/重放批次 | 无值填 `"0"` |
| `orderId` | 关联订单主键 | 无值填 `"0"` |
| `payRefundId` | 退款主键 | 无值填 `"0"` |
| `sourceBizNo` | 业务流水号 | 无值填 `"0"` |
| `errorCode` | 错误码或 `"0"` | 必填 |
| `withdrawId` | 提现申请单主键 | 无值填 `"0"` |
| `originCommissionId` | 原始佣金单据 | 无值填 `"0"` |
| `traceTicketId` | 财务/客服追踪单号 | 无值填 `"0"` |
| `fieldSnapshot` | 关键字段快照 | 必填 |
| `recoveryAction` | 已执行动作 | 必填 |

## 6. 场景话术与恢复动作

### 6.1 绑定推广人失败

| 错误码 | 用户话术 | 恢复动作 | 禁止行为 |
|---|---|---|---|
| `1011007002 BROKERAGE_BIND_SELF` | 不能把自己设为推广人，请更换邀请人后重试。 | 停留当前页，允许重新输入或重新扫码。 | 不得继续提示“绑定成功”。 |
| `1011007003 BROKERAGE_BIND_USER_NOT_ENABLED` | 当前邀请人暂不具备推广资格，请更换后再试。 | 保留当前页，重新选择推广人。 | 不得自动切到任意其他推广人。 |
| `1011007005 BROKERAGE_BIND_MODE_REGISTER` | 当前场景不支持绑定，请先完成正常登录/注册。 | 引导返回正常登录/注册链路。 | 不得偷偷绕过绑定时机限制。 |
| `1011007006 BROKERAGE_BIND_OVERRIDE` | 您已绑定推广人，当前关系不能直接修改。 | 展示既有绑定关系，转人工咨询。 | 不得覆盖原绑定关系。 |
| `1011007007 BROKERAGE_BIND_LOOP` | 当前推广关系异常，请更换邀请关系后重试。 | 转 `L2` 复核关系链。 | 不得自动修正上下级关系。 |

### 6.2 提现申请失败

| 错误码 | 用户话术 | 恢复动作 | 禁止行为 |
|---|---|---|---|
| `1011008002 BROKERAGE_WITHDRAW_MIN_PRICE` | 当前提现金额低于系统最低门槛，请调整金额后重试。 | 保留表单值和提现方式，提示最小金额。 | 不得弹出“提交成功”或清空表单。 |
| `1011008003 BROKERAGE_WITHDRAW_USER_BALANCE_NOT_ENOUGH` | 当前可提现佣金不足，请刷新余额后重新输入金额。 | 刷新 `brokeragePrice` 与 `withdrawPrice` 后重试。 | 不得允许前端自减金额绕过校验。 |
| `1011008005-1011008009` | 当前提现正在处理中或复核中，请稍后在提现记录中查看。 | 创建 `BROKERAGE_FUNDS_REVIEW` 工单，转财务复核。 | 不得对用户承诺“已到账”。 |

### 6.3 提现详情 / 到账争议

| 场景 | 用户话术 | 恢复动作 | 禁止行为 |
|---|---|---|---|
| `GET /trade/brokerage-withdraw/get` 返回 `null` | 当前记录暂不可查看，请返回提现记录页刷新后重试。 | 回提现列表页执行一次刷新；仍失败则转 `L2`。 | 不得把 `null` 解释成“提现成功”或“系统已处理完成”。 |
| `status != 10` 或 `type != 5` 或 `payTransferId <= 0` | 当前提现仍处于审核/处理中，请以后续记录页状态为准。 | 保持只读，不显示确认收款。 | 不得展示“确认收款”按钮。 |
| `status===10 && type===5 && payTransferId>0` | 系统已发起微信收款，请核对微信到账后确认收款。 | 允许进入确认收款链路，并记录 `payTransferId`。 | 不得在未满足条件时展示该入口。 |

### 6.4 团队 / 排行 / 推广商品异常

| 场景 | 标准话术 | 恢复动作 | 禁止行为 |
|---|---|---|---|
| 团队页字段不一致 | 当前团队统计正在刷新，请稍后再试；如持续异常，已转人工处理。 | 记录 `brokerageOrderCount`、`brokerageUserCount`、旧前端字段值，转产品/前端复核。 | 不得静默把 `brokerageOrderCount` 当 `orderCount` 继续展示。 |
| 排行页空结果或异常 | 当前排行数据暂不可用，请稍后下拉刷新。 | 保留上次成功数据；若无历史数据则展示空态说明。 | 不得把空列表解释成“暂无团队/无人推广”。 |
| 商品佣金区间查询失败 | 当前预计佣金暂无法计算，请稍后刷新。 | 展示“计算中”或只读占位。 | 不得猜测固定佣金金额。 |

## 7. 财务与客服协同约束
- 客服只能解释当前状态与下一步动作，不能越过财务承诺到账时间。
- 财务复核必须回填：`withdrawId`、`status`、`type`、`payTransferId`、复核时间、处理人。
- 分销域所有到账类问题以提现申请单状态机为唯一真值，不以弹窗文案、短信、截图单独判定。
- 如命中多单重复失败或批量错配，必须升级为 `P1`，由 brokerage on-call 与财务负责人共同处置。

## 8. 升级矩阵

| 级别 | 典型场景 | 升级对象 | 时限 |
|---|---|---|---|
| `P0` | 大面积误显示到账、关闭态仍允许确认收款、资金字段大面积错位 | 财务负责人 + Brokerage on-call + 发布负责人 | 5 分钟 |
| `P1` | 提现申请持续失败、绑定冲突集中爆发、团队/排行字段大面积错位 | Brokerage on-call + Product on-call | 15 分钟 |
| `P2` | 单用户绑定失败、个别提现表单失败、排行/团队短时空态 | 值班客服 + 域负责人 | 30 分钟 |

## 9. 验收清单
- [ ] 文档明确“提现申请成功 != 到账成功”。
- [ ] 文档明确只有 `status===10 && type===5 && payTransferId>0` 才允许确认收款。
- [ ] 文档明确 `withdrawPrice / brokeragePrice / frozenPrice` 是唯一资金字段真值。
- [ ] 文档明确团队页后端真值字段是 `brokerageOrderCount`。
- [ ] 客服、财务、on-call 的升级和回访闭环可直接执行。
