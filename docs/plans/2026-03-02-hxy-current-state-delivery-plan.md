# HXY Current-State Delivery Plan Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在现有并轨架构下，优先打通“服务项目 + 实物商品 + 门店价库存 + 支付履约”主链路，消除占位实现与关键一致性缺口。  

**Architecture:** 保持 `product/trade/pay` 三域并轨，不做大重构；以“强约束补齐 + 状态机闭环 + 门禁验证”为主线推进。先收口 P0 交易主链，再推进 P1 结算/工单/生命周期，最后做 P2 性能与事件化。  

**Tech Stack:** Spring Boot + MyBatis-Plus + MySQL + Redis + Maven + GitHub Actions + Bash Gate Scripts

---

## 0. 复审结论（基于当前代码）

1. 已完成：门店 SKU 价库存映射、库存幂等流水与重试、模板版本/校验/预览/提交、memory/naming 门禁。  
2. 主要缺口：
   1. 预约网关仍为占位实现：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/booking/NoopTradeServiceBookingGateway.java`
   2. 订单模板快照未并入交易订单项核心链路（仅模板域与服务履约快照部分存在）。
   3. 套餐子项履约/退款状态机未闭环（看板仍是 Todo）。
   4. 技师提成计提/冲正、结算审批流与 SLA 未系统化落地（看板 Todo）。
3. 工程风险：工作区含大规模历史文档迁移/删除，需分离“业务开发提交”和“文档归档提交”。

---

## Task 1: 基线稳定与分支治理（P0-Day0）

**Files:**
- Modify: `hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md`
- Modify: `hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md`
- Modify: `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`

**Step 1: 冻结本期范围**
- 仅纳入以下交付：预约网关实接、订单模板快照、服务/实物分流强约束、套餐子项最小闭环、提成与结算最小版。

**Step 2: 拆分提交策略**
- 业务代码提交与文档清理提交分离，避免混淆回归范围。

**Step 3: 验证**
- Run: `CHECK_STAGED=1 CHECK_UNSTAGED=0 CHECK_UNTRACKED=0 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- Expected: `result=PASS`

---

## Task 2: 预约网关实接（P0）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/booking/TradeServiceBookingGateway.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/booking/NoopTradeServiceBookingGateway.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/booking/impl/BookingDomainGatewayImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/TradeServiceOrderServiceImpl.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/order/TradeServiceOrderServiceImplTest.java`

**Step 1: 先写失败测试**
- 覆盖：支付后建履约单 -> 调用真实 gateway -> 失败可重试 -> 成功不重复。

**Step 2: 实现最小可用网关**
- 接口层保留幂等键（`serviceOrderId` / `orderItemId`）与超时重试。

**Step 3: 回归**
- Run: `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-trade -Dtest=TradeServiceOrderServiceImplTest,TradeServiceOrderBookingRetryJobTest test`

---

## Task 3: 订单模板快照落单（P0）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/dal/dataobject/order/TradeOrderItemDO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/TradeOrderUpdateServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/convert/order/TradeOrderConvert.java`
- SQL: `ruoyi-vue-pro-master/sql/mysql/hxy/*order-item-template-snapshot*.sql`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/order/TradeOrderUpdateServiceTest.java`

**Step 1: 定义快照字段**
- `templateVersionId/templateSnapshotJson/priceSourceSnapshotJson`（不可变）。

**Step 2: 下单时写入快照**
- 快照来源只能来自已发布模板版本，缺失则阻断下单。

**Step 3: 回归**
- Run: `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-trade -Dtest=TradeOrderUpdateServiceTest test -Dsurefire.failIfNoSpecifiedTests=false`

---

## Task 4: 服务/实物分流强约束（P0）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/handler/TradeProductSkuOrderHandler.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/handler/TradeServiceOrderHandler.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceStoreSkuOverrideTest.java`

**Step 1: 强制规则**
- 服务商品禁止库存扣减；实物商品必须库存校验与扣减。
- 服务商品禁止走物流发货分支。

**Step 2: 异常码收口**
- 新增错误码用于“分流违规”并给前后端统一提示。

**Step 3: 回归**
- Run: `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-trade -Dtest=TradePriceServiceStoreSkuOverrideTest,TradeServiceOrderServiceImplTest test`

---

## Task 5: 套餐子项履约/退款最小闭环（P0->P1）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleServiceImpl.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/bo/TradeBundleItemSnapshotBO.java`
- SQL: `ruoyi-vue-pro-master/sql/mysql/hxy/*bundle-item*.sql`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleReviewTicketServiceImplTest.java`

**Step 1: 套餐拆子项快照入库**
- 下单时固定子项及金额分摊，禁止事后反查实时配置。

**Step 2: 售后按子项处理**
- 支持“已履约子项不可退/未履约可退”规则。

**Step 3: 回归**
- Run: `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-trade -Dtest=AfterSaleReviewTicketServiceImplTest,AfterSaleRefundDecisionServiceImplTest test`

---

## Task 6: 技师提成与结算审批SLA最小版（P1）

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/settlement/*`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/job/settlement/*`
- SQL: `ruoyi-vue-pro-master/sql/mysql/hxy/*technician-commission*.sql`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/settlement/*`

**Step 1: 计提与冲正模型**
- 仅服务子项、仅履约完成计提；退款触发冲正。

**Step 2: 结算审批流**
- 总部审批 -> 门店确认 -> 财务记账，附 SLA 时钟与升级策略。

**Step 3: 回归**
- 增加结算链路 gate（可复用 19/20 pipeline 模式）。

---

## Task 7: 门店生命周期流程引擎补齐（P1）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreServiceImpl.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/lifecycle/*`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/test/java/cn/iocoder/yudao/module/product/service/store/ProductStoreServiceImplTest.java`

**Step 1: 开店/停业/闭店守卫补齐**
- 强制检查商品映射、库存、未结订单、在途工单。

**Step 2: 事件触发**
- 触发交易、库存、售后模块的冻结/解冻动作。

**Step 3: 回归**
- Run: `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-product -Dtest=ProductStoreServiceImplTest test`

---

## Task 8: 发布门禁与CI收口（P2）

**Files:**
- Modify: `ruoyi-vue-pro-master/script/dev/run_payment_stagea_p0_17_18.sh`
- Modify: `ruoyi-vue-pro-master/script/dev/run_payment_stagea_p0_19_20.sh`
- Modify: `ruoyi-vue-pro-master/.github/workflows/payment-stagea-p0-17-18.yml`
- Modify: `ruoyi-vue-pro-master/.github/workflows/payment-stagea-p0-19-20.yml`

**Step 1: 新链路 gate**
- 新增服务履约、提成结算、套餐售后三类 gate。

**Step 2: 必检策略**
- required checks 强制包含 memory/naming + 交易主链 gate。

**Step 3: 回归**
- Run: `bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh --help`
- Run: `bash ruoyi-vue-pro-master/script/dev/run_payment_stagea_p0_17_18_local_ci.sh`

---

## 里程碑与顺序（建议）

1. M1（3-5 天）：Task 2 + Task 3 + Task 4（主链闭环）  
2. M2（5-7 天）：Task 5 + Task 6（可结算可追溯）  
3. M3（3-4 天）：Task 7 + Task 8（治理与发布收口）  

---

## 完成定义（DoD）

1. 支付后：服务履约单创建 -> 预约网关实接成功或可重试，不再依赖 Noop 占位。  
2. 交易订单项：模板版本与价格来源快照可审计。  
3. 分流规则：服务不扣库存、实物必扣库存，在自动化测试中有覆盖。  
4. 套餐售后：可按子项履约状态判断退款。  
5. 提成结算：可计提、可冲正、可审批、可追溯。  
6. 发布门禁：memory/naming/主链 gate 全部通过。  

