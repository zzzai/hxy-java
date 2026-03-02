# HXY Next Delivery Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在不推翻现有架构的前提下，完成“总部+门店”O2O主链路的生产化收口：门店SKU价库存、服务加项、售后工单、门店生命周期与发布门禁。

**Architecture:** 继续沿用“总部主数据（SPU/SKU）+门店映射覆写（价/库存/上下架）+交易域下单支付”的分层。短期不做大重构，先把主链路稳定性、幂等与可观测补齐，再推进性能和治理项。

**Tech Stack:** Spring Boot, MyBatis-Plus, MySQL, Quartz Job, Redis, JUnit5 + Mockito, Shell Gate Scripts.

---

## Scope Baseline（已完成）

- 佣金结算审批流 + SLA 预警/升级 + 通知 outbox 派发已可用。
- 新增 outbox 管理接口已完成：
  - `GET /booking/commission-settlement/notify-outbox-page`
  - `POST /booking/commission-settlement/notify-outbox-retry`

---

### Task 1: 佣金通知闭环收口（运营可用）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/TechnicianCommissionSettlementController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/TechnicianCommissionSettlementServiceImpl.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/TechnicianCommissionSettlementControllerTest.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/TechnicianCommissionSettlementServiceImplTest.java`

**Step 1: Write failing tests**
- 覆盖分页筛选组合（status+notifyType+channel）与手工重试批量异常分支。

**Step 2: Run test to verify it fails**
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=TechnicianCommissionSettlementControllerTest,TechnicianCommissionSettlementServiceImplTest test`

**Step 3: Write minimal implementation**
- 补充筛选参数校验、重试审计日志字段统一（reason/triggerSource）。

**Step 4: Run test to verify it passes**
- 同上命令，预期 PASS。

**Step 5: Commit**
- `feat(booking): finalize notify outbox query and manual retry guards`

---

### Task 2: 打通门店 SKU 价库存到下单/支付主链路（P0）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreMappingServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/handler/TradeProductSkuOrderHandler.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/test/java/cn/iocoder/yudao/module/product/service/store/ProductStoreMappingServiceImplTest.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceStoreSkuOverrideTest.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/order/handler/TradeProductSkuOrderHandlerTest.java`

**Step 1: Write failing tests**
- 自提门店必须走门店价/门店库存；非自提保持总部价库存。

**Step 2: Run test to verify it fails**
- `mvn -pl yudao-module-mall/yudao-module-product,yudao-module-mall/yudao-module-trade -Dtest=ProductStoreMappingServiceImplTest,TradePriceServiceStoreSkuOverrideTest,TradeProductSkuOrderHandlerTest test`

**Step 3: Write minimal implementation**
- 统一 trade 侧读取门店覆写逻辑，并在下单锁库与取消回补使用同一路径。

**Step 4: Run test to verify it passes**
- 同上命令，预期 PASS。

**Step 5: Commit**
- `feat(trade): enforce store-level sku price-stock path for pickup`

---

### Task 3: 库存并发与补偿（乐观锁+流水+重试）

**Files:**
- Create: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-02-hxy-store-sku-stock-ledger.sql`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/dal/dataobject/store/ProductStoreSkuStockLedgerDO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/dal/mysql/store/ProductStoreSkuStockLedgerMapper.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreMappingServiceImpl.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/job/order/TradeStoreSkuStockCompensateJob.java`

**Step 1: Write failing tests**
- 幂等键 `(bizType,bizNo,storeId,skuId)` 重复提交不重复扣减。
- 扣减失败记录可被补偿任务重试并收敛。

**Step 2: Run test to verify it fails**
- `mvn -pl yudao-module-mall/yudao-module-product,yudao-module-mall/yudao-module-trade -Dtest=ProductStoreMappingServiceImplTest,TradeServiceOrderBookingRetryJobTest test`

**Step 3: Write minimal implementation**
- 增加库存流水唯一键、状态字段、重试次数和下次执行时间。

**Step 4: Run test to verify it passes**
- 同上命令，预期 PASS。

**Step 5: Commit**
- `feat(stock): add idempotent store stock ledger and compensation job`

---

### Task 4: 模板约束补齐（版本快照 + affects_price/affects_stock）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/template/ProductTemplateGenerateServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/template/vo/ProductCategoryTemplateValidateReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/dal/dataobject/template/ProductCategoryAttrTplItemDO.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/test/java/cn/iocoder/yudao/module/product/service/template/ProductTemplateGenerateServiceImplTest.java`

**Step 1: Write failing tests**
- 服务类目禁止 `affectsStock=true`。
- 零售类目 `SKU_SPEC` 必须至少有一个 `affectsPrice=true`。
- 下单前可读取模板版本快照。

**Step 2: Run test to verify it fails**
- `mvn -pl yudao-module-mall/yudao-module-product -Dtest=ProductTemplateGenerateServiceImplTest test`

**Step 3: Write minimal implementation**
- 增强校验器 + 快照落库字段映射。

**Step 4: Run test to verify it passes**
- 同上命令，预期 PASS。

**Step 5: Commit**
- `feat(product): enforce template version snapshot and spec role constraints`

---

### Task 5: 服务商品“加钟/加项目”并入交易订单明细（P1）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingAddonServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/dal/dataobject/order/TradeOrderItemDO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/convert/order/TradeOrderConvert.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/order/TradeServiceOrderServiceImplTest.java`

**Step 1: Write failing tests**
- 加项目订单项能携带 `addonType/addonRefId/addonAmount` 快照。

**Step 2: Run test to verify it fails**
- `mvn -pl yudao-module-mall/yudao-module-trade,yudao-module-mall/yudao-module-booking -Dtest=TradeServiceOrderServiceImplTest test`

**Step 3: Write minimal implementation**
- 在订单项层落加项字段，保证支付金额与展示明细一致。

**Step 4: Run test to verify it passes**
- 同上命令，预期 PASS。

**Step 5: Commit**
- `feat(trade): persist service addon snapshot in order item`

---

### Task 6: 售后统一工单中台（最小版升级）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleReviewTicketServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/controller/admin/aftersale/AfterSaleReviewTicketController.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleReviewTicketServiceImplTest.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/job/aftersale/AfterSaleReviewTicketEscalationJobTest.java`

**Step 1: Write failing tests**
- SLA 升级后 `severity/escalateTo` 迁移正确。
- 工单收口需保留 resolver 与 remark 审计。

**Step 2: Run test to verify it fails**
- `mvn -pl yudao-module-mall/yudao-module-trade -Dtest=AfterSaleReviewTicketServiceImplTest,AfterSaleReviewTicketEscalationJobTest test`

**Step 3: Write minimal implementation**
- 工单升级规则固定化（P2->P1->P0），收口审计字段完整。

**Step 4: Run test to verify it passes**
- 同上命令，预期 PASS。

**Step 5: Commit**
- `feat(trade): harden review ticket escalation and resolve audit`

---

### Task 7: 门店生命周期流程引擎补齐（开店/停业/闭店）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/store/ProductStoreController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/enums/store/ProductStoreLifecycleStatusEnum.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/test/java/cn/iocoder/yudao/module/product/service/store/ProductStoreServiceImplTest.java`

**Step 1: Write failing tests**
- 非法状态迁移被拒绝。
- 停业/闭店必须带原因；闭店前必须清理在售映射。

**Step 2: Run test to verify it fails**
- `mvn -pl yudao-module-mall/yudao-module-product -Dtest=ProductStoreServiceImplTest test`

**Step 3: Write minimal implementation**
- 明确状态机守卫与错误码，批量变更与单条变更一致。

**Step 4: Run test to verify it passes**
- 同上命令，预期 PASS。

**Step 5: Commit**
- `feat(store): enforce lifecycle transition state machine`

---

### Task 8: 发布门禁与运维收口（命名/内存/库存链路）

**Files:**
- Modify: `ruoyi-vue-pro-master/script/dev/run_payment_stagea_p0_19_20.sh`
- Create: `ruoyi-vue-pro-master/script/dev/check_store_sku_order_chain.sh`
- Modify: `ruoyi-vue-pro-master/.github/workflows/payment-stagea-p0-19-20.yml`
- Modify: `ruoyi-vue-pro-master/.github/workflows/hxy-naming-guard.yml`

**Step 1: Write failing gate case**
- 人为构造门店库存幂等冲突，门禁脚本应失败退出。

**Step 2: Run gate to verify it fails**
- `bash script/dev/check_store_sku_order_chain.sh`

**Step 3: Write minimal implementation**
- 将库存链路检查加入发布脚本和 CI 强制步骤。

**Step 4: Run gate to verify it passes**
- `bash script/dev/check_hxy_naming_guard.sh`
- `bash script/dev/check_hxy_memory_guard.sh`
- `bash script/dev/check_store_sku_order_chain.sh`

**Step 5: Commit**
- `chore(ci): add store sku chain gate into release pipeline`

---

## Recommended Execution Order

1. Task 2
2. Task 3
3. Task 4
4. Task 5
5. Task 6
6. Task 7
7. Task 1
8. Task 8

## Global Verification

- `mvn -pl yudao-module-mall/yudao-module-product test -DskipITs`
- `mvn -pl yudao-module-mall/yudao-module-trade test -DskipITs`
- `mvn -pl yudao-module-mall/yudao-module-booking test -DskipITs`
- `bash script/dev/check_hxy_naming_guard.sh`
- `bash script/dev/check_hxy_memory_guard.sh`
