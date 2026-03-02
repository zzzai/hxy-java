# Store SKU Chain Priority Plan Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 打通“门店 SKU 价库存”到下单/支付主链路，并补齐并发库存、模板铺货、服务加项、万店性能优化。  

**Architecture:** 以“总部 SPU/SKU 主数据 + 门店映射覆写”为核心。下单按 `pickUpStoreId` 优先读取 `hxy_store_product_sku` 覆写；库存走门店维度预占/回滚；运营侧提供模板化铺货；服务加项作为订单子项快照入单。  

**Tech Stack:** Spring Boot + MyBatis-Plus + MySQL + Redis + 定时任务/消息事件。

---

## P0：门店价库存主链路 + 并发补偿

### Task 1: 建立门店 SKU 查询/扣减 API（产品域）

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/api/store/ProductStoreSkuApi.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/api/store/dto/ProductStoreSkuRespDTO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/api/store/dto/ProductStoreSkuUpdateStockReqDTO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/api/store/ProductStoreSkuApiImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreMappingService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreMappingServiceImpl.java`

**Step 1: Write failing tests**
- Add tests for:
  - `getStoreSkuMap(storeId, skuIds)` 返回门店覆写价/库存。
  - `updateStoreSkuStock` 正负库存变更及不足报错。

**Step 2: Run test to verify it fails**
- Run: `mvn -pl yudao-module-mall/yudao-module-product -Dtest=ProductStoreSkuApiImplTest,ProductStoreMappingServiceImplTest test -DskipITs`
- Expected: fail due to missing API/service methods.

**Step 3: Implement minimal code**
- 新增 API + DTO。
- Service 提供按门店读取 SKU 覆写与库存变更。

**Step 4: Run test to verify it passes**
- Same command, expected PASS.

**Step 5: Commit**
- `feat(product): add store sku api for price-stock query and stock update`

### Task 2: 价格计算链路接入门店 SKU 覆写

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/price/bo/TradePriceCalculateReqBO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/convert/order/TradeOrderConvert.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceImplTest.java`

**Step 1: Write failing test**
- 用 `pickUpStoreId` 场景断言：
  - 读取门店 salePrice 而不是总部 SKU price。
  - 使用门店 stock 校验不足。

**Step 2: Verify RED**
- `mvn -pl yudao-module-mall/yudao-module-trade -Dtest=TradePriceServiceImplTest test -DskipITs`

**Step 3: Implement minimal code**
- `TradePriceCalculateReqBO` 增加 `storeId`（从 `pickUpStoreId` 映射）。
- `checkSkuList` 增加门店覆写路径；非自提场景保持原逻辑。

**Step 4: Verify GREEN**
- Same test command, expected PASS.

**Step 5: Commit**
- `feat(trade): apply store sku override price-stock in settlement`

### Task 3: 下单库存预占改为门店优先

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/handler/TradeProductSkuOrderHandler.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/TradeOrderUpdateServiceImpl.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/order/handler/TradeProductSkuOrderHandlerTest.java`

**Step 1: Write failing test**
- 自提订单：预占/取消回滚调用门店库存 API。
- 非自提订单：保持总部 SKU 库存逻辑。

**Step 2: Verify RED**
- `mvn -pl yudao-module-mall/yudao-module-trade -Dtest=TradeProductSkuOrderHandlerTest test -DskipITs`

**Step 3: Implement minimal code**
- Handler 根据 `order.pickUpStoreId` 决定库存更新目标。

**Step 4: Verify GREEN**
- Same command, expected PASS.

**Step 5: Commit**
- `feat(trade): reserve and rollback store sku stock for pickup orders`

### Task 4: 库存流水 + 幂等补偿

**Files:**
- Create SQL: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-01-hxy-store-sku-stock-log.sql`
- Create DO/Mapper/Service:
  - `.../product/dal/dataobject/store/ProductStoreSkuStockLogDO.java`
  - `.../product/dal/mysql/store/ProductStoreSkuStockLogMapper.java`
  - `.../product/service/store/ProductStoreSkuStockLogService.java`
- Modify: `.../product/service/store/ProductStoreMappingServiceImpl.java`
- Create job: `.../trade/job/order/TradeStoreSkuStockCompensateJob.java`

**Step 1: Write failing tests**
- 重复请求同一幂等键不会重复扣减。
- 扣减失败可记录并可重试补偿。

**Step 2: Verify RED**
- `mvn -pl yudao-module-mall/yudao-module-product -Dtest=ProductStoreSkuStockLogServiceTest,ProductStoreMappingServiceImplTest test -DskipITs`

**Step 3: Implement minimal code**
- 库存变更记录 `bizType/bizNo/storeId/skuId/changeCount/status`。
- 失败记录进入重试任务扫描队列。

**Step 4: Verify GREEN**
- Same command, expected PASS.

**Step 5: Commit**
- `feat(stock): add store sku stock ledger and compensation retry`

### Task 5: P0 验收脚本

**Files:**
- Create: `ruoyi-vue-pro-master/script/dev/check_store_sku_order_chain.sh`

**Step 1: Write script test cases**
- Case A: 自提店有覆写价，结算价=门店价。
- Case B: 库存不足时报错。
- Case C: 取消订单库存回补。

**Step 2: Run and verify**
- `bash ruoyi-vue-pro-master/script/dev/check_store_sku_order_chain.sh`
- Expected: all cases pass.

**Step 3: Commit**
- `test(trade): add store sku order chain smoke script`

---

## P1：模板铺货 + 服务加项并入订单

### Task 6: 门店模板铺货与区域批量策略

**Files:**
- Create SQL: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-01-hxy-store-dispatch-template.sql`
- Create DO/Mapper/Service/Controller under `.../product/.../store/`
- Frontend:
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/store/template/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/storeTemplate.ts`

**Acceptance:**
- 可保存模板（SPU/SKU集 + 默认策略）。
- 按区域/分类/标签批量下发到门店。

### Task 7: 服务加项模型并入订单项明细

**Files:**
- Reuse SQL:
  - `ruoyi-vue-pro-master/sql/mysql/hxy/2026-02-22-hxy-service-commerce-unified-model.sql`
- Modify:
  - `.../trade/service/price/bo/TradePriceCalculateReqBO.java`
  - `.../trade/service/price/bo/TradePriceCalculateRespBO.java`
  - `.../trade/convert/order/TradeOrderConvert.java`
  - `.../trade/dal/dataobject/order/TradeOrderItemDO.java`

**Acceptance:**
- 订单项可落 `basePrice + addonItems + addonTotalPrice` 快照。
- 支付金额与展示明细一致，可追溯。

---

## P2：万店级性能与事件化

### Task 8: 缓存与热点优化

**Files:**
- `.../product/service/store/ProductStoreMappingServiceImpl.java`
- `.../trade/service/price/TradePriceServiceImpl.java`
- `.../trade/framework/order/config/TradeOrderProperties.java`

**Acceptance:**
- `storeId+skuId` 读链路缓存命中。
- 高并发下数据库压力可控。

### Task 9: 异步事件化同步

**Files:**
- Create event & listener in `.../trade/service/order/event/`
- Modify order create/cancel/pay handlers

**Acceptance:**
- 库存流水、门店状态变更异步解耦。
- 重试与死信可观测。

### Task 10: 监控与门禁

**Files:**
- Create: `ruoyi-vue-pro-master/script/dev/check_store_sku_release_gate.sh`
- Modify: `ruoyi-vue-pro-master/script/dev/run_payment_stagea_p0_19_20.sh`

**Acceptance:**
- 上线前强门禁校验门店库存链路、补偿队列、幂等一致性。

---

## Global Verification Commands

- Product module tests:
  - `mvn -pl yudao-module-mall/yudao-module-product test -DskipITs`
- Trade module tests:
  - `mvn -pl yudao-module-mall/yudao-module-trade test -DskipITs`
- Server package:
  - `mvn -pl yudao-server -am package -DskipTests`

