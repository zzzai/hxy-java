# MiniApp 分销与佣金域 PRD v1（2026-03-10）

## 0. 文档定位
- 目标：把分销中心、佣金、提现、团队、排行相关能力收口到真实页面、真实 API、真实资金口径，避免“页面存在但口径发散”。
- 分支：`feat/ui-four-account-reconcile-ops`
- 约束：
  - 只认当前 uniapp `pages/commission/*` 路由和 app controller 真值。
  - 不把注释掉的 `/pages/commission/apply`、原型里的代理资料页写成冻结能力。
  - 资金相关动作不得按提示文案判断成功，只认后端状态与错误码。
- 对齐基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
  - `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md`

## 1. 业务目标与非目标

### 1.1 业务目标
- 让用户在分销中心清楚区分“当前佣金、可提现佣金、冻结佣金、已提现佣金”。
- 让提现申请、审核、打款、确认收款、失败回退都可被产品、财务、客服统一解释。
- 让团队、推广订单、推广商品、用户排行、佣金排行都绑定到真实字段和真实接口。

### 1.2 非目标
- 不新增代理申请页、申诉页、撤回提现页、取消提现页。
- 不把后台结算单、申诉单、风控单直接映射成用户侧已上线页面。
- 不改动当前前端资金按钮行为，只冻结业务规则和验收口径。

## 2. 域能力总览

| 能力 | 真实页面 | 状态 | 真实 API | 结论 |
|---|---|---|---|---|
| 分销中心首页 | `/pages/commission/index` | `ACTIVE` | `/trade/brokerage-user/get` `/trade/brokerage-user/get-summary` | 真实菜单与资产总览入口 |
| 佣金钱包 / 提现记录 | `/pages/commission/wallet` | `ACTIVE` | `/trade/brokerage-user/get-summary` `/trade/brokerage-record/page` `/trade/brokerage-withdraw/page` | 当前佣金与流水承接页 |
| 提现申请 | `/pages/commission/withdraw` | `ACTIVE` | `/trade/config/get` `/system/dict-data/type` `/trade/brokerage-user/get` `/trade/brokerage-withdraw/create` | 真实提现入口 |
| 团队 | `/pages/commission/team` | `ACTIVE` | `/trade/brokerage-user/get-summary` `/trade/brokerage-user/child-summary-page` | 一级 / 二级成员查询 |
| 推广订单 | `/pages/commission/order` | `ACTIVE` | `/trade/brokerage-record/page` | 仅展示佣金记录，不是完整订单中心 |
| 推广商品 | `/pages/commission/goods` | `ACTIVE` | `/product/spu/page` `/trade/brokerage-record/get-product-brokerage-price` | 商品页叠加预计佣金 |
| 推广人排行 | `/pages/commission/promoter` | `ACTIVE` | `/trade/brokerage-user/rank-page-by-user-count` | 周 / 月排行 |
| 佣金排行 | `/pages/commission/commission-ranking` | `ACTIVE` | `/trade/brokerage-user/rank-page-by-price` `/trade/brokerage-user/get-rank-by-price` | 周 / 月排行 + 我的名次 |
| 我的资料 / 申请页 | 无真实用户页 | `缺页能力` | 无当前前端消费 | `/pages/commission/apply` 已被注释掉，不得写成现网能力 |
| 用户申诉 / 撤回 / 取消提现 | 无真实用户页 | `缺页能力` | 无当前前端消费 | 只能走客服 / 线下复核，不是已上线页面 |

## 3. 用户场景与页面流转

### 3.1 分销中心
1. 用户进入 `/pages/commission/index` 查看推广资格、资产摘要和菜单。
2. 菜单真实项只有：我的团队、佣金明细、分销订单、推广商品、邀请海报、推广排行、佣金排行。
3. `/pages/commission/apply` 当前在菜单代码中被注释，不能作为用户路径。

### 3.2 佣金与提现
1. 用户进入 `/pages/commission/wallet` 查看“当前佣金 / 冻结佣金 / 可提现佣金”与流水。
2. 用户从钱包页点击“提现”或“转余额”，都进入 `/pages/commission/withdraw` 或复用同一创建接口。
3. 提现页读取提现门槛、冻结天数、提现方式、银行字典，再调用 `POST /trade/brokerage-withdraw/create` 创建申请。
4. 钱包页历史记录中，只有 `status===10 && type===5 && payTransferId>0` 才能出现“确认收款”按钮。

### 3.3 团队 / 订单 / 商品 / 排行
1. `/pages/commission/team` 按一级 / 二级成员切页，并支持昵称搜索与按团队数 / 订单数 / 金额排序。
2. `/pages/commission/order` 展示佣金记录视角的“推广订单”，不是交易订单主表。
3. `/pages/commission/goods` 先拉商品分页，再逐条拉预计佣金区间。
4. `/pages/commission/promoter` 与 `/pages/commission/commission-ranking` 分别展示“按用户数排行”和“按佣金排行”。

## 4. 页面 route 真值

| 页面 route | 真实参数 | 页面角色 | 当前真值说明 |
|---|---|---|---|
| `/pages/commission/index` | 无 | 分销中心首页 | 菜单真值来自 `commission-menu.vue` |
| `/pages/commission/wallet` | `type?` | 佣金钱包 / 提现记录页 | 提现页成功后常带 `type=2` 跳回提现记录 tab |
| `/pages/commission/withdraw` | 无 | 提现申请页 | 不含提现单 ID，提交后只创建新申请 |
| `/pages/commission/team` | 无 | 团队页 | 搜索、排序、级别切换都走 query 参数，不改 route |
| `/pages/commission/order` | 无 | 推广订单页 | 通过 tab 切状态，不改 route |
| `/pages/commission/goods` | 无 | 推广商品页 | 分享是页面内动作，不改 route |
| `/pages/commission/promoter` | 无 | 推广人排行页 | 周 / 月通过请求时间窗切换 |
| `/pages/commission/commission-ranking` | 无 | 佣金排行页 | 周 / 月通过请求时间窗切换 |

## 5. 页面 -> API -> 字段关系

### 5.1 首页 / 钱包 / 资产口径

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/commission/index` | `GET /trade/brokerage-user/get` | 无 | `brokerageEnabled` `brokeragePrice` `frozenPrice` | 推广资格、可用佣金、冻结佣金 |
| `/pages/commission/index` `/pages/commission/wallet` `/pages/commission/team` | `GET /trade/brokerage-user/get-summary` | 无 | `yesterdayPrice` `withdrawPrice` `brokeragePrice` `frozenPrice` `firstBrokerageUserCount` `secondBrokerageUserCount` | 资产摘要、推广人数、昨日收益 |
| `/pages/commission/wallet` | `GET /trade/brokerage-record/page` | `pageNo` `pageSize` `bizType` `status?` | `id` `bizId` `title` `price` `status` `statusName` `createTime` `finishTime` | 佣金流水和推广订单 |
| `/pages/commission/wallet` | `GET /trade/brokerage-withdraw/page` | `pageNo` `pageSize` | `id` `type` `typeName` `status` `statusName` `price` `createTime` `payTransferId` `transferChannelPackageInfo` `transferChannelMchId` | 提现记录 |

### 5.2 提现申请

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/commission/withdraw` | `GET /trade/config/get` | 无 | `brokerageWithdrawMinPrice` `brokerageFrozenDays` `brokerageWithdrawTypes` | 最低提现额、冻结天数、提现方式 |
| `/pages/commission/withdraw` | `GET /system/dict-data/type` | `type=brokerage_bank_name` | `label` `value` 等字典字段 | 银行选择列表 |
| `/pages/commission/withdraw` | `GET /trade/brokerage-user/get` | 无 | `brokerageEnabled` `brokeragePrice` `frozenPrice` | 当前可提现金额和冻结金额 |
| `/pages/commission/withdraw` | `POST /trade/brokerage-withdraw/create` | `type` `price` `userAccount` `userName` `qrCodeUrl` `bankName` `bankAddress` `transferChannelCode` | `Long withdrawId` | 仅表示申请单创建成功，不表示到账成功 |

### 5.3 团队 / 排行 / 商品

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/commission/team` | `GET /trade/brokerage-user/child-summary-page` | `level` `nickname?` `sortingField?` `pageNo` `pageSize` | `id` `nickname` `avatar` `brokeragePrice` `brokerageOrderCount` `brokerageUserCount` `brokerageTime` | 团队成员列表 |
| `/pages/commission/promoter` | `GET /trade/brokerage-user/rank-page-by-user-count` | `times[0]` `times[1]` `pageNo` `pageSize` | `id` `nickname` `avatar` `brokerageUserCount` | 推广人排行 |
| `/pages/commission/commission-ranking` | `GET /trade/brokerage-user/rank-page-by-price` | `times[0]` `times[1]` `pageNo` `pageSize` | `id` `nickname` `avatar` `brokeragePrice` | 佣金排行 |
| `/pages/commission/commission-ranking` | `GET /trade/brokerage-user/get-rank-by-price` | `times[]` | `Integer` | 我的名次 |
| `/pages/commission/goods` | `GET /product/spu/page` | `pageNo` `pageSize` | `id` `name` `picUrl` `introduction` `price` `marketPrice` | 推广商品列表 |
| `/pages/commission/goods` | `GET /trade/brokerage-record/get-product-brokerage-price` | `spuId` | `enabled` `brokerageMinPrice` `brokerageMaxPrice` | 预计佣金区间 |

### 5.4 资金口径冻结

| 展示文案 | 真实字段 | 冻结口径 |
|---|---|---|
| 当前佣金（钱包页大字） | `withdrawPrice` | 历史累计已提现 / 已转余额口径，不等于可提现余额 |
| 可提现佣金 | `brokeragePrice` | 当前可用于发起提现的金额 |
| 冻结佣金 | `frozenPrice` | 冻结期内不可提现金额 |
| 提现成功 | `status = 11 WITHDRAW_SUCCESS` | 只有提现单状态进入 `11` 才能宣告到账 |
| 审核通过待转账 | `status = 10 AUDIT_SUCCESS` | 仍不可宣称“已到账” |

## 6. `ACTIVE / PLANNED_RESERVED / 缺页能力` 分层

### 6.1 `ACTIVE`
- 分销中心首页、钱包、提现、团队、推广订单、推广商品、推广人排行、佣金排行。
- 佣金流水、提现流水、名次查询、预计佣金区间查询。
- 提现门槛、冻结天数、提现方式、银行字典查询。

### 6.2 `PLANNED_RESERVED`
- 无当前真实用户页但后台 / 运行手册已存在的能力：结算单审批、申诉单、风控冻结批次管理、财务复核台账。
- 这些能力只存在后台或运行手册，不得被前端页面冒充为已上线。

### 6.3 缺页能力
- 用户侧代理申请页 / 我的资料页。
- 用户侧提现撤回、提现取消、提现申诉、冻结申诉页。
- 用户侧佣金明细导出页、资金对账页、财务回单页。

## 7. 错误码与用户恢复动作

| 错误码 | 场景 | 用户侧动作 | 产品约束 |
|---|---|---|---|
| `1011007000 BROKERAGE_USER_NOT_EXISTS` | 首次进入分销域 / 绑定推广人失败 | 刷新分销中心；仍失败则引导联系客服 | 不得展示“已成为推广员” |
| `1011007001 BROKERAGE_USER_FROZEN_PRICE_NOT_ENOUGH` | 冻结金额解冻 / 扣减异常 | 阻断资金动作，提示人工复核 | 不得自动冲减冻结余额 |
| `1011007002 BROKERAGE_BIND_SELF` | 绑定自己为推广人 | 阻断绑定，保留当前页面 | 不得吞错继续展示绑定成功 |
| `1011007003 BROKERAGE_BIND_USER_NOT_ENABLED` | 绑定无推广资格用户 | 阻断绑定，提示更换推广人 | 不得生成半绑定态 |
| `1011007005 BROKERAGE_BIND_MODE_REGISTER` | 非注册时尝试绑定 | 阻断绑定，提示当前场景不支持 | 不得 fallback 到任意时绑定 |
| `1011007006 BROKERAGE_BIND_OVERRIDE` | 已绑定推广人后再次绑定 | 阻断绑定，展示当前不可改绑 | 不得覆盖原绑定关系 |
| `1011007007 BROKERAGE_BIND_LOOP` | 下级绑定上级形成环 | 阻断绑定 | 不得自动选择其他推广人 |
| `1011007008 BROKERAGE_USER_LEVEL_NOT_SUPPORT` | 请求超出 2 级关系 | 阻断查询 | 不得展示伪三级团队 |
| `1011008000 BROKERAGE_WITHDRAW_NOT_EXISTS` | 查看提现详情 | 回提现记录页刷新一次 | 不得展示旧详情缓存为成功 |
| `1011008001 BROKERAGE_WITHDRAW_STATUS_NOT_AUDITING` | 审核态操作冲突 | 刷新提现详情 | 不得继续确认收款 |
| `1011008002 BROKERAGE_WITHDRAW_MIN_PRICE` | 提现金额低于门槛 | 保留表单并提示最小值 | 不得弹“提交成功” |
| `1011008003 BROKERAGE_WITHDRAW_USER_BALANCE_NOT_ENOUGH` | 可提现余额不足 | 刷新余额后重试 | 不得让前端自减金额绕过 |
| `1011008005-1011008009` | 提现打款状态回写失败 | 展示“处理中/复核中”，转财务复核 | 不得宣告到账 |

## 8. 降级语义与禁止伪成功规则

| 场景 | 降级类型 | 允许行为 | 禁止行为 |
|---|---|---|---|
| 提现申请提交 | `fail-close` | 申请失败时保留表单、允许改值重提 | 仅因弹窗成功文案就认定到账成功 |
| 钱包流水查询失败 | `fail-open` | 保留已加载列表、允许下拉刷新 | 清空列表后展示“暂无数据”冒充自然空态 |
| 团队 / 排行查询失败 | `fail-open` | 保留上次成功数据并提示刷新 | 把空列表当成真实零团队、零排行 |
| 推广商品佣金区间查询失败 | `fail-open` | 显示“预计佣金：计算中” | 猜测固定佣金金额 |
| 微信确认收款 | `fail-close` | 仅在 `status=10 && type=5 && payTransferId>0` 展示按钮 | 任何其他状态展示“确认收款” |

### 8.1 禁止伪成功
- `POST /trade/brokerage-withdraw/create` 成功只代表“申请单创建成功”，不是到账成功。
- `status=10 AUDIT_SUCCESS` 只代表审核通过或转账中，不能对用户承诺“提现成功”。
- 钱包页“转余额”同样走 `POST /trade/brokerage-withdraw/create`，不能和即时到账混淆。

## 9. 风险与真实字段差异

| 项目 | 真值 | 联调风险 |
|---|---|---|
| 团队订单数字段 | 后端 app VO 为 `brokerageOrderCount` | 当前前端模板使用 `item.orderCount`，A/C 需要确认字段兼容 |
| 推广订单页本质 | 佣金记录视图 | 不能把 `bizId` 当成交易订单详情全量数据 |
| 排行页时间窗 | 周 / 月依赖 `times[0]` `times[1]` | 不能用固定自然周文案替代接口返回 |

## 10. 是否阻断开发、是否阻断发布

| 判断项 | 结论 | 说明 |
|---|---|---|
| 既有分销页继续开发是否阻断 | `否` | 真实 route 和 app API 已存在 |
| 涉及申诉 / 撤回 / 取消提现等新页面开发是否阻断 | `是` | 当前无真实用户页和真实前端消费链路 |
| 仅按现有页面范围发布是否阻断 | `否` | 以现有八个页面和辅助 API 为边界 |
| 若把代理申请 / 申诉 / 财务结算能力写成已上线是否阻断发布 | `是` | 会造成资金口径和客服口径失真 |

## 11. 验收清单
- [ ] 文档明确冻结“当前佣金 = `withdrawPrice`，可提现 = `brokeragePrice`，冻结佣金 = `frozenPrice`”。
- [ ] 文档明确 `/pages/commission/apply` 当前不存在，不可作为用户路径。
- [ ] 提现审核、转账、确认收款、失败复核的状态口径不混淆。
- [ ] 团队、排行、推广订单、推广商品都绑定到真实字段和真实 API。
- [ ] 错误码与恢复动作覆盖绑定、提现、冻结、排行 / 团队查询失败等主要场景。
