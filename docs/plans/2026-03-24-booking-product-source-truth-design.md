# Booking Product Source Truth Design

## 1. 设计目标
- 把 `Booking create / addon` 的商品来源统一收口到“服务目录 / 商品选择结果”。
- 消除 `order-confirm` 从 `slot` 猜 `spuId/skuId`、`addon` 只传 `parentOrderId,addonType` 的双重漂移。
- 在不碰规划态 `/product/catalog/page` 的前提下，复用当前真实 `ACTIVE` 的商品列表 / 商品详情能力完成最小闭环。

## 2. 当前问题
- `technician-detail -> order-confirm` 当前只带 `timeSlotId/technicianId/storeId`，没有稳定商品来源。
- `order-confirm` 当前从 `state.slot.spuId/skuId` 猜商品来源，这不属于稳定真值。
- `addon` 当前只提交 `parentOrderId,addonType`；`UPGRADE / ADD_ITEM` 没有真实商品选择结果。
- 这导致 `create` 与 `addon` 虽然 path 已对齐，但仍然不能形成可审计的真实写链。

## 3. 方案比较

### 方案 A：新增 booking 专用服务目录/商品选择页
- 新增 booking 域专用商品选择页，复用 `GET /product/spu/page` 和 `GET /product/spu/get-detail`。
- 选择结果统一回传 `spuId/skuId/spuName/skuName`。
- `create` 与 `addon` 共用这一套结果。
- 优点：真值单一、污染面最小、回归边界清晰。
- 缺点：需要新增页面与回跳协议。

### 方案 B：复用 `/pages/goods/list` 与 `/pages/goods/index`，增加 booking mode
- 在现有商品列表/详情页内增加 booking 选择模式。
- 优点：少造页面。
- 缺点：会污染当前商品主链、点击行为和 SKU 选择弹窗；回归面较大。

### 方案 C：在 `order-confirm` / `addon` 内嵌轻量选择器
- 两个页面各自发商品列表与 SKU 详情请求。
- 优点：页面数少。
- 缺点：会形成两套商品来源逻辑，长期仍会漂移。

## 4. 采用方案
- 采用方案 A。
- 原因：它最符合当前项目“单一真值 + 最小侵入 + 可审计”的要求。

## 5. 目标数据流

### 5.1 create
1. 用户在 `technician-detail` 选中时段。
2. 跳转到 booking 专用服务目录页，并带上：`storeId`,`technicianId`,`timeSlotId`,`flow=create`。
3. 用户选择商品与 SKU。
4. 服务目录页跳转到 `order-confirm`，显式带上：
   - `timeSlotId`
   - `technicianId`
   - `storeId`
   - `spuId`
   - `skuId`
   - `spuName?`
   - `skuName?`
5. `order-confirm` 只提交显式 route 中的 `spuId/skuId`，不再从 `slot` 猜测。

### 5.2 addon
1. 用户从 `order-detail` 进入 `addon`。
2. `addonType=EXTEND_TIME`：可沿用母单 `spuId/skuId`，不强制重新选商品。
3. `addonType=UPGRADE / ADD_ITEM`：跳转到 booking 专用服务目录页，并带上：`parentOrderId`,`storeId`,`addonType`,`flow=addon`。
4. 用户选择商品与 SKU。
5. 服务目录页回跳 `addon`，显式带上：
   - `parentOrderId`
   - `addonType`
   - `spuId`
   - `skuId`
   - `spuName?`
   - `skuName?`
6. `addon` 提交时：
   - `EXTEND_TIME`：允许无新的 `spuId/skuId`，沿用母单
   - `UPGRADE / ADD_ITEM`：必须带新的 `spuId/skuId`

## 6. 页面与组件
- 新增：`yudao-mall-uniapp/pages/booking/service-select.vue`
- 可能复用：`yudao-mall-uniapp/sheep/components/s-select-sku/s-select-sku.vue`
- 变更：
  - `yudao-mall-uniapp/pages/booking/technician-detail.vue`
  - `yudao-mall-uniapp/pages/booking/order-confirm.vue`
  - `yudao-mall-uniapp/pages/booking/addon.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/pages.json`

## 7. API 与字段策略
- 继续只用当前真实 API：
  - `GET /product/spu/page`
  - `GET /product/spu/get-detail`
  - `POST /booking/order/create`
  - `POST /app-api/booking/addon/create`
- 不引入规划态 API：
  - `GET /product/catalog/page`
  - `POST /booking/addon-intent/submit`
- booking 专用服务目录页的结果字段以 route 参数显式传递，不再从其他响应体侧推。

## 8. 错误与降级边界
- 商品列表拉取失败：服务目录页空态 + fail-close，不自动进入 `order-confirm` / `addon submit`。
- 商品详情或 SKU 不可选：禁止确认，停留当前页。
- `order-confirm` 若缺 `spuId/skuId`：禁止提交，提示先选择服务项目。
- `addon` 若 `addonType=UPGRADE / ADD_ITEM` 且缺 `spuId/skuId`：禁止提交。
- 不补造 `degraded=true / degradeReason`。

## 9. 测试策略
- 先补 booking logic / page-level 单测：
  - `create` 路由必须显式携带商品来源。
  - `order-confirm` 提交不能再读 `slot.spuId/skuId`。
  - `addon` 对不同 `addonType` 的提交规则正确。
- 再补页面 smoke test：
  - 服务目录页无选择时不能确认。
  - 带回商品结果后才可提交。
- 最后回写文档：
  - booking PRD
  - page field dictionary
  - closure review
  - 项目总账

## 10. 结论
- `Booking` 写链当前最优先要补的不是更多发布材料，而是商品来源真值。
- 该设计完成后，`Booking` 写链的工程 blocker 将从“商品来源真值 + 发布证据”收敛为“主要剩真实发布证据”。
