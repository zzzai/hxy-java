# MiniApp 交易下单与订单主链 PRD v1（2026-03-12）

## 0. 文档定位
- 目标：把用户侧交易主链从服务蓝图中拆出，形成独立 PRD，覆盖购物车、结算、订单创建、订单详情、订单列表、收货、取消、删除。
- 真实代码基线：
  - 页面：`/pages/index/cart`、`/pages/order/confirm`、`/pages/order/list`、`/pages/order/detail`
  - 前端 API：`yudao-mall-uniapp/sheep/api/trade/order.js`
  - Controller：`AppTradeOrderController`
- 约束：
  - 只认当前真实 route 与真实 `method + path`
  - `/trade/order/get` 仅作为兼容 fallback，不作为主产品真值

## 1. 产品目标
1. 支持用户从购物车进入结算页并创建订单。
2. 支持订单列表、详情、订单数量查询。
3. 支持确认收货、取消订单、删除订单。
4. 支持商品级重新结算入口和订单物流轨迹回流。

## 2. 页面真值
- 购物车：`/pages/index/cart`
- 订单确认：`/pages/order/confirm`
- 订单列表：`/pages/order/list`
- 订单详情：`/pages/order/detail`

以下 alias 不是当前真值：
- `/pages/order/index`
- `/pages/trade/order-detail`
- `/pages/order/create`

## 3. 接口真值

| 动作 | Method + Path | 说明 |
|---|---|---|
| 订单结算 | `GET /trade/order/settlement` | 计算订单确认信息 |
| 商品结算信息 | `GET /trade/order/settlement-product` | 用于商品详情侧快速结算 |
| 创建订单 | `POST /trade/order/create` | 提交订单 |
| 订单详情 | `GET /trade/order/get-detail` | 主详情接口 |
| 订单列表 | `GET /trade/order/page` | 查询订单分页 |
| 订单数量 | `GET /trade/order/get-count` | 查询各状态数量 |
| 物流轨迹 | `GET /trade/order/get-express-track-list` | 查询快递轨迹 |
| 支付结果联动 | `GET /trade/order/pay-result` | 用于支付完成后回流订单状态 |
| 确认收货 | `PUT /trade/order/receive` | 收货 |
| 取消订单 | `DELETE /trade/order/cancel` | 取消 |
| 删除订单 | `DELETE /trade/order/delete` | 删除 |
| 订单项详情 | `GET /trade/order/item/get` | 单条订单项查询 |
| 订单项评价 | `POST /trade/order/item/create-comment` | 单条评价 |

兼容说明：
- 前端当前对 `GET /trade/order/get-detail` 保留 `GET /trade/order/get` fallback。
- 产品真值仍以 `GET /trade/order/get-detail` 为准，不得把 fallback 冻结成 canonical。

## 4. 核心业务流程
1. 用户从购物车进入订单确认页。
2. 结算接口返回收货地址、优惠券、配送方式、商品项和金额信息。
3. 用户确认信息后提交创建订单。
4. 创建成功后进入支付链路或回流订单详情。
5. 订单创建后，用户可以在订单列表、订单详情中查看状态并执行收货、取消、删除等动作。

## 5. 关键规则

### 5.1 结算规则
- `couponId`、`addressId`、`pickUpStoreId`、`deliveryType` 等字段为条件字段，无值时不下发。
- 订单商品项按 `items[i].skuId / items[i].count / items[i].cartId` 传参。
- 结算失败时必须保留当前购物上下文，不允许直接清空购物车态。

### 5.2 创建订单规则
- 创建失败必须按业务错误码提示，不按 message 分支。
- 创建失败后必须允许用户重新修改地址、优惠券、配送方式再提交。

### 5.3 订单详情规则
- 详情页以 `GET /trade/order/get-detail` 为主。
- 若命中旧后端 fallback，只能作为兼容展示，不得在产品文档中升级为长期真值。
- 若详情接口整体不可用，可降级为基础展示，但必须提示用户稍后重试或联系客服。

### 5.4 列表与数量规则
- 订单列表接口不可用时，可降级为空列表展示，但不能伪造成功数据。
- 订单数量必须与列表状态口径一致，不允许出现 tab 数与列表总数长期不一致。

### 5.5 状态动作规则
- 确认收货仅对可收货订单开放。
- 取消订单仅对可取消订单开放。
- 删除订单不等于取消订单，不得混用文案和操作结果。

## 6. 降级与异常口径
- `GET /trade/order/page` 不可用：允许空列表降级。
- `GET /trade/order/get-detail` 不可用：允许基础详情降级，但必须提示。
- 写操作 `create / receive / cancel / delete` 失败：一律 fail-close，禁止成功态文案、成功 icon、成功动效。

## 7. 验收标准
1. 用户可以从购物车进入结算页并完成订单创建。
2. 订单列表、详情、数量查询三者口径一致。
3. 取消、删除、收货只在合法状态下开放。
4. 详情接口 fallback 仅作为兼容，不得出现在新 PRD 真值列中。
5. 列表/详情查询降级不影响下单主链，但必须显式提示。

## 8. 非目标
- 不定义支付渠道策略。
- 不定义售后与退款链路。
- 不把历史 fallback 或 alias route 写成产品真值。
