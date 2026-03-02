# P0 Template Binding + Booking Placeholder + Payment CI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 一次性完成模板主数据绑定、预约占位实接、支付 CI #23 收口，并补齐回归与治理文档。

**Architecture:** 商品域补主数据绑定并前置校验，交易域在价格计算阶段增加主数据兜底，预约网关在 booking 缺失时主动建占位并回写服务履约单，工程侧新增 PR 阶段支付回归 workflow 和 required checks 配置。

**Tech Stack:** Java/Spring Boot、MyBatis-Plus、JUnit5(Mockito/BaseDbUnitTest)、GitHub Actions、Bash。

---

### Task 1: 模板绑定数据结构落地

**Files:**
- Modify: `ruoyi-vue-pro-master/sql/mysql/ruoyi-modules-member-pay-mall.sql`
- Create: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-02-hxy-product-template-version-binding.sql`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/test/resources/sql/create_tables.sql`

**Steps:**
1. 增加 `product_spu.template_version_id`、`product_sku.template_version_id`。
2. SQL 迁移脚本补回填逻辑（服务商品优先按类目发布模板回填；SKU 继承 SPU）。
3. 更新测试建表脚本字段，保证单测可运行。

### Task 2: Product 主数据绑定校验实现

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/dal/dataobject/spu/ProductSpuDO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/dal/dataobject/sku/ProductSkuDO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/spu/vo/ProductSpuSaveReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/spu/vo/ProductSkuSaveReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/spu/vo/ProductSpuRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/spu/vo/ProductSkuRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/api/spu/dto/ProductSpuRespDTO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/api/sku/dto/ProductSkuRespDTO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/spu/ProductSpuServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/sku/ProductSkuService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/sku/ProductSkuServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/enums/ErrorCodeConstants.java`

**Steps:**
1. 新增字段到 DO/VO/DTO。
2. 在 `ProductSpuServiceImpl` 增加模板版本校验与 SKU 继承逻辑。
3. 调整 `ProductSkuService` 创建/更新签名，携带 SPU 模板版本并写入 SKU。
4. 新增必要错误码。

### Task 3: 交易价格模板版本主数据兜底

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceImpl.java`

**Steps:**
1. 在模板校验前先做 `item -> sku/spu` 的模板版本回填。
2. 统一重建 `templateVersionIds` 集合后拉取版本信息。
3. 保留已有“发布态/类目匹配/快照回填/服务必填”约束。

### Task 4: 预约占位实接

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingOrderStatusEnum.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingOrderService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingOrderServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-server/src/main/java/com/hxy/server/config/booking/ServerTradeServiceBookingGateway.java`

**Steps:**
1. booking 新增 `WAIT_BOOKING` 状态。
2. `BookingOrderService` 增加占位创建接口（按 `payOrderId` 幂等）。
3. `ServerTradeServiceBookingGateway` 查不到 booking 时主动创建占位并回写 `markBooked`。

### Task 5: 测试先行与回归

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/test/java/cn/iocoder/yudao/module/product/service/spu/ProductSpuTemplateVersionBindingTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceTemplateVersionValidationTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-server/src/test/java/com/hxy/server/config/booking/ServerTradeServiceBookingGatewayTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingOrderServiceImplTest.java`

**Steps:**
1. 先写失败用例：模板主数据绑定、价格兜底、占位创建与回写。
2. 实现最小代码使测试通过。
3. 跑目标模块测试集。

### Task 6: Payment CI #23 收口

**Files:**
- Create: `ruoyi-vue-pro-master/script/dev/run_payment_stagea_p0_23.sh`
- Create: `ruoyi-vue-pro-master/.github/workflows/payment-stagea-p0-23.yml`
- Modify: `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`

**Steps:**
1. 新增本地/CI通用脚本，封装 payment fast-track 回归。
2. 新增 PR 工作流，形成可阻断 check。
3. required checks 脚本纳入 `p0-23` context。

### Task 7: 验证与治理文档

**Files:**
- Modify: `hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md`
- Modify: `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`
- Modify: `hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md`

**Steps:**
1. 跑模块测试与门禁脚本。
2. 更新事实基线/ADR/执行看板。
3. 提交、推送、回写 PR 证据。
