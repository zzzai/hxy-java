# UI Miniapp P0 Page Closure - Window B

## 0. 交付范围

- 交付形态：`ui/stitch_/closure_p0` 可运行 HTML 原型（仓库中无可直接改造的小程序工程代码）
- 覆盖页面：
  - 支付结果页（成功/处理中/失败）
  - 订单列表页（按状态筛选）
  - 售后申请页
  - 售后列表页
  - 售后详情/退款进度页
  - 全局异常兜底页（网络异常/服务降级）

## 1. 页面联调清单

### 1.1 支付结果页 `pay-result.html`

- 接口：`GET /pay/order/get`
- 请求参数：
  - `id` 支付单 ID（页面输入）
  - `sync` 是否同步查询支付状态（默认 `true`）
- 关键响应字段：
  - `id` 支付单 ID
  - `no` 支付单号
  - `status` 支付状态（`0` 处理中，`10/20` 成功，其他按失败态展示）
  - `price` 支付金额（分）
  - `merchantOrderId` 关联交易单 ID
- 异常码映射：
  - `0`：正常渲染支付结果
  - `401`：提示登录态失效（由后端/网关处理）
  - `404/405/501`：判定旧后端不支持
  - `5xx`：服务降级，建议跳转全局异常页
- 降级行为：
  - 旧后端不支持时，显示黄条提示并回退本地 `payOrder` 原型数据，不阻断“查看订单”。

### 1.2 订单列表页 `order-list.html`

- 接口：`GET /trade/order/page`
- 请求参数：
  - `pageNo`
  - `pageSize`
  - `status`（页签：`0/10/20/30`）
  - `commentStatus`（待评价页签时传 `false`）
- 关键响应字段：
  - `list[]`：订单数组
  - `list[].id`
  - `list[].no`
  - `list[].status`
  - `list[].payPrice`
  - `list[].productCount`
  - `list[].items[]`（含 `id/spuName/properties/valueName/count`）
  - `total/pageNo/pageSize`
- 异常码映射：
  - `0`：正常渲染
  - `404/405/501`：旧后端不支持
  - `5xx`：服务降级
- 降级行为：
  - 旧后端不支持时，黄条提示 + 本地订单样例数据继续按状态筛选。

#### 详情子流程（同页）

- 接口：`GET /trade/order/get-detail`
- 请求参数：
  - `id` 交易订单 ID
  - `sync`（示例传 `true`）
- 关键响应字段：
  - `id/no/status/payPrice/items[]`
- 异常码映射：同上
- 降级行为：
  - 旧后端不支持时返回本地订单详情，保证“查看详情/发起售后”可继续。

### 1.3 售后申请页 `aftersale-apply.html`

- 接口 1：`GET /trade/order/get-detail`（加载订单项）
- 请求参数：
  - `id`（orderId）
  - `sync`
- 关键响应字段：
  - `items[]` + `items[].id/picUrl/spuName/properties/payPrice/count`

- 接口 2：`POST /trade/after-sale/create`
- 请求参数（字段命名严格对齐后端）：
  - `orderItemId`
  - `refundPrice`
  - `way`（`10` 仅退款，`20` 退款退货）
  - `applyReason`
  - `applyDescription`
  - `applyPicUrls`（数组）
- 关键响应字段：
  - `id`（售后单 ID）
- 异常码映射：
  - `0`：申请成功
  - `400`：参数校验失败
  - `404/405/501`：旧后端不支持
  - `5xx`：服务降级
- 降级行为：
  - 旧后端不支持时，以本地 mock 生成售后单 `id` 并提示“已降级，主流程继续”，不阻断“提交申请 -> 查看售后列表”。

### 1.4 售后列表页 `aftersale-list.html`

- 接口：`GET /trade/after-sale/page`
- 请求参数：
  - `pageNo`
  - `pageSize`
  - `statuses`（页签映射：`10`、`20,30,40`、`50`、`61,62,63`）
- 关键响应字段：
  - `list[]`
  - `list[].id/no/status/way/refundPrice/spuName/properties/updateTime/buttons`
  - `total/pageNo/pageSize`
- 异常码映射：
  - `0`：正常渲染
  - `404/405/501`：旧后端不支持
  - `5xx`：服务降级
- 降级行为：
  - 旧后端不支持时，使用本地售后样例并保持状态筛选可用，不阻断“查看详情/进度”。

### 1.5 售后详情/退款进度页 `aftersale-detail.html`

- 接口：`GET /trade/after-sale/get`
- 请求参数：
  - `id`（售后单 ID）
- 关键响应字段：
  - `id/no/status/way/refundPrice/spuName/properties/applyReason/applyDescription/createTime/updateTime/buttons`
- 异常码映射：
  - `0`：正常渲染
  - `404/405/501`：旧后端不支持
  - `5xx`：服务降级
- 降级行为：
  - 旧后端不支持时，显示本地售后详情与进度节点，保障“进度查看”主流程可继续。

### 1.6 全局异常兜底页 `error.html`

- 输入参数（query）：
  - `type`：`network` 或 `service`
  - `code`：错误码（可选）
  - `msg`：错误信息（可选）
  - `from`：来源页面（可选）
- 展示策略：
  - `network`：网络断连文案 + 重试建议
  - `service`：服务降级文案 + 回主流程建议
- 降级行为：
  - 作为统一兜底入口，页面可 `history.back()` 重试，或返回首页恢复主流程。

## 2. 统一异常码策略（前端侧）

- `code = 0`：正常路径
- `401`：登录态失效（提示重新登录）
- `404/405/501` 或错误信息包含 `未实现/不支持/Not Found`：判定“旧后端不支持”，触发降级
- `5xx`：服务降级（提示并引导异常页）
- 网络错误（fetch TypeError/超时）：提示网络异常，可跳转 `error.html?type=network`

## 3. 降级实现摘要

- 所有目标接口统一走 `api-client.js`。
- 若命中“旧后端不支持”特征，返回结构：
  - `ok: true`
  - `degraded: true`
  - `degradeReason: 旧后端未支持 <endpoint>，已降级到原型数据`
- 页面统一通过 banner 呈现降级提示，且继续渲染可操作数据，不阻断主流程。

## 4. 原型文件路径

- `ui/stitch_/closure_p0/index.html`
- `ui/stitch_/closure_p0/pay-result.html`
- `ui/stitch_/closure_p0/order-list.html`
- `ui/stitch_/closure_p0/aftersale-apply.html`
- `ui/stitch_/closure_p0/aftersale-list.html`
- `ui/stitch_/closure_p0/aftersale-detail.html`
- `ui/stitch_/closure_p0/error.html`
- `ui/stitch_/closure_p0/api-client.js`
- `ui/stitch_/closure_p0/mock-data.js`
- `ui/stitch_/closure_p0/app.js`
- `ui/stitch_/closure_p0/styles.css`
- `ui/stitch_/closure_p0/README.md`
