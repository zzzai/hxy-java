# AfterSale Refund Limit Audit And Commission SLA Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Compute bundle refund cap from persisted service-order snapshot, persist refund-limit audit fields on after-sale create, and close one commission reversal + settlement/SLA validation gap.

**Architecture:** Keep current trade/booking boundaries. In trade, add a refund-limit decision object resolved from service-order snapshot first, then order-item snapshot fallback, and persist source/detail into after-sale row. In booking, strengthen settlement workflow by testing reversal commissions flowing into settlement draft totals.

**Tech Stack:** Spring Boot, MyBatis-Plus, MapStruct, JUnit5 Mockito, Maven.

---

### Task 1: Persist refund-limit audit fields in after-sale

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/dal/dataobject/aftersale/AfterSaleDO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/controller/admin/aftersale/vo/AfterSaleBaseVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/controller/app/aftersale/vo/AppAfterSaleRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/resources/sql/create_tables.sql`
- Create: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-02-hxy-after-sale-refund-limit-audit.sql`

### Task 2: Compute refund cap from persisted service-order snapshot

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/TradeServiceOrderServiceImpl.java`

### Task 3: TDD for trade changes

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleServiceImplBundleRefundValidationTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/order/TradeServiceOrderServiceImplTest.java`

### Task 4: Commission reversal + settlement/SLA gap test

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/TechnicianCommissionSettlementServiceImplTest.java`

### Task 5: Verification

**Run:**
- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-trade -am -Dtest=AfterSaleServiceImplBundleRefundValidationTest,TradeServiceOrderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-booking -Dtest=TechnicianCommissionSettlementServiceImplTest,TechnicianCommissionServiceImplCancelCommissionTest -Dsurefire.failIfNoSpecifiedTests=false test`
