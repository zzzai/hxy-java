# 小程序 P0 页面闭环原型（Window B）

## 运行方式

1. 直接在浏览器打开 `index.html`。
2. 如需联调真实后端，在页面顶部填写 `API Base`，例如：`http://localhost:48080/admin-api`。
3. 若后端接口返回旧版本不支持（404/405/501/未实现），页面会自动降级为本地原型数据并提示。

## 页面列表

- `pay-result.html` 支付结果页
- `order-list.html` 订单列表页（含详情）
- `aftersale-apply.html` 售后申请页
- `aftersale-list.html` 售后列表页
- `aftersale-detail.html` 售后详情/退款进度页
- `error.html` 全局异常兜底页

## 指定接口

- `/trade/order/page`
- `/trade/order/get-detail`
- `/trade/after-sale/create`
- `/trade/after-sale/page`
- `/trade/after-sale/get`
- `/pay/order/get`
