# MiniApp 支付提交与结果回流 PRD v1（2026-03-12）

## 0. 文档定位
- 目标：把用户侧支付主链独立成产品 PRD，覆盖支付渠道获取、支付单查询、支付提交、支付结果页与订单回流。
- 真实代码基线：
  - 页面：`/pages/pay/index`、`/pages/pay/result`
  - 前端 API：`yudao-mall-uniapp/sheep/api/pay/order.js`、`yudao-mall-uniapp/sheep/api/pay/channel.js`
  - Controller：`AppPayOrderController`、`AppPayChannelController`
- 约束：
  - 只认真实支付页和结果页
  - 支付成功不等于订单已完全履约，支付页只负责支付完成与结果回流

## 1. 产品目标
1. 支持获取可用支付渠道。
2. 支持提交支付订单。
3. 支持查询支付单详情与支付状态。
4. 支持支付完成后回流支付结果页和订单状态页。

## 2. 页面真值
- 支付提交页：`/pages/pay/index`
- 支付结果页：`/pages/pay/result`

不认以下历史/假想路径：
- `/pages/payment/index`
- `/pages/pay/success`
- `/pages/order/pay-result`

## 3. 接口真值

| 动作 | Method + Path | 说明 |
|---|---|---|
| 获取可用支付渠道编码 | `GET /pay/channel/get-enable-code-list` | 获取当前 app 可用渠道 |
| 查询支付单 | `GET /pay/order/get` | 查询支付单详情和状态 |
| 提交支付单 | `POST /pay/order/submit` | 发起支付 |
| 查询订单支付结果 | `GET /trade/order/pay-result` | 订单维度支付结果回流 |

## 4. 核心业务流程
1. 用户在订单确认完成后进入支付页。
2. 页面先获取可用支付渠道，按 app 范围展示可支付方式。
3. 用户提交支付单。
4. 提交成功后，根据支付结果进入支付结果页。
5. 支付结果页通过支付单查询和订单支付结果查询，确定最终展示状态并回流订单详情。

## 5. 关键规则

### 5.1 渠道规则
- 渠道列表必须以 `GET /pay/channel/get-enable-code-list` 返回结果为准。
- 禁止前端写死渠道可用性。

### 5.2 提交规则
- 提交支付单成功仅代表发起支付成功，不代表扣款成功。
- 支付失败或中断必须允许用户重新回到支付页再发起。

### 5.3 查询规则
- 支付单查询主接口：`GET /pay/order/get`
- 订单结果回流接口：`GET /trade/order/pay-result`
- 两者展示含义不同：
  - `pay/order/get` 面向支付单状态
  - `trade/order/pay-result` 面向订单支付结果展示

### 5.4 支付结果页规则
- 支付结果页必须区分：
  - 支付成功
  - 支付处理中 / 待确认
  - 支付失败
- `PAY_ORDER_NOT_FOUND` 不能伪装成支付成功，只能降级为待确认并允许刷新或回订单页核对。

## 6. 降级与异常口径
- `GET /pay/order/get` 不可用：允许降级为待确认状态，但必须提示稍后在订单页核对。
- 渠道列表不可用：不得默认展示不存在的渠道。
- `POST /pay/order/submit` 失败：一律 fail-close，不得显示支付成功态。

## 7. 验收标准
1. 支付页能正确拿到可用支付渠道并展示。
2. 提交支付单后，支付结果页能区分成功、待确认、失败。
3. `PAY_ORDER_NOT_FOUND` 展示为待确认，而不是成功。
4. 支付单查询降级时，用户仍能通过订单页继续核对支付状态。
5. 支付链路文案不得把“提交成功”写成“支付成功”。

## 8. 非目标
- 不定义渠道费率策略。
- 不定义钱包充值和钱包流水能力。
- 不把支付结果页扩写成履约或售后页面。
