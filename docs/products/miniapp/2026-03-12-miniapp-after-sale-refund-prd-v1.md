# MiniApp 售后与退款主链 PRD v1（2026-03-12）

## 0. 文档定位
- 目标：把用户侧售后 / 退款主链从服务蓝图拆出，形成独立 PRD，覆盖售后申请、售后列表、售后详情、售后日志、回寄、退款进度。
- 真实代码基线：
  - 页面：`/pages/order/aftersale/apply`、`/pages/order/aftersale/list`、`/pages/order/aftersale/detail`、`/pages/order/aftersale/log`、`/pages/order/aftersale/return-delivery`
  - 前端 API：`yudao-mall-uniapp/sheep/api/trade/afterSale.js`
  - Controller：`AppAfterSaleController`、`AppAfterSaleLogController`
- 约束：
  - 只认当前真实售后页面
  - 不把 `/pages/refund/progress` 一类历史描述写成当前真实 route

## 1. 产品目标
1. 支持用户在线提交售后申请。
2. 支持查看售后列表、售后详情和售后进度日志。
3. 支持用户填写回寄信息。
4. 支持查看退款进度。
5. 在后端能力不全时，明确降级到人工处理，而不是伪成功。

## 2. 页面真值
- 售后申请：`/pages/order/aftersale/apply`
- 售后列表：`/pages/order/aftersale/list`
- 售后详情：`/pages/order/aftersale/detail`
- 售后日志：`/pages/order/aftersale/log`
- 回寄录入：`/pages/order/aftersale/return-delivery`

说明：
- 退款进度属于售后详情的业务能力延伸，当前不是独立真值页面。

## 3. 接口真值

| 动作 | Method + Path | 说明 |
|---|---|---|
| 售后分页 | `GET /trade/after-sale/page` | 售后列表 |
| 售后详情 | `GET /trade/after-sale/get` | 售后详情 |
| 提交售后 | `POST /trade/after-sale/create` | 创建售后申请 |
| 回寄信息录入 | `PUT /trade/after-sale/delivery` | 回寄物流录入 |
| 取消售后 | `DELETE /trade/after-sale/cancel` | 取消售后申请 |
| 售后日志 | `GET /trade/after-sale-log/list` | 售后进度日志 |
| 退款进度 | `GET /trade/after-sale/refund-progress` | 查询退款进度 |

## 4. 核心业务流程
1. 用户从订单详情发起售后申请。
2. 售后创建成功后，用户可在售后列表和详情中查看状态。
3. 若需要回寄，用户进入回寄页面录入物流。
4. 用户可查看售后日志和退款进度。
5. 若接口不支持在线动作，必须明确转人工处理。

## 5. 关键规则

### 5.1 创建售后规则
- 创建失败不能伪装成成功，必须明确提示“转人工处理”或联系客服。
- 售后申请是否允许发起，以订单与售后状态为准。

### 5.2 列表与详情规则
- 售后列表不可用时，允许降级为空列表，但必须说明不影响下单主流程。
- 售后详情不可用时，允许降级为基础展示，但必须提示稍后重试。

### 5.3 回寄规则
- 回寄录入失败时，不允许显示“回寄成功”。
- 若在线回寄接口不可用，必须明确转人工处理。

### 5.4 退款进度规则
- 退款进度接口真实存在：`GET /trade/after-sale/refund-progress`
- 当前产品展示应以售后详情页为主承接，不得在 PRD 中虚构新的独立进度页路由。

### 5.5 售后日志规则
- 售后日志可降级为空日志展示，但不能伪造节点。
- 空日志不等于售后成功或失败，只表示进度明细不可用。

## 6. 降级与异常口径
- `GET /trade/after-sale/page` 不可用：允许空列表降级。
- `POST /trade/after-sale/create` 不可用：降级到人工处理，禁止成功态。
- `GET /trade/after-sale/get` 不可用：允许基础展示降级。
- `GET /trade/after-sale-log/list` 不可用：允许空日志降级。
- `PUT /trade/after-sale/delivery` 不可用：降级到人工回寄登记。

## 7. 验收标准
1. 用户可提交售后申请，并在列表、详情中看到记录。
2. 用户可录入回寄信息，失败时不会出现伪成功。
3. 售后日志与退款进度能被查询；若不可用，降级提示清晰。
4. 当前真实页面全部在 `/pages/order/aftersale/*` 体系下，不得写成旧 alias route。
5. 退款进度能力必须以真实接口存在为准，而不是臆造页面路由。

## 8. 非目标
- 不定义后台退款回调重放。
- 不定义支付网关退款协议。
- 不把人工处理通道写成线上自动成功能力。
