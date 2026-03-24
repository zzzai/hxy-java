# Reserved Runtime Closure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 `Reserved` 三域补齐真实小程序页面、真实 app controller、真实前后端绑定、最小数据对象和自动化测试。

**Architecture:** `Referral` 通过 promotion façade 复用现有 brokerage 真值；`Technician Feed` 在 booking 模块新增轻量 feed 真域对象；`Gift Card` 在 promotion 模块新增独立 gift-card 最小模型。所有域都保留默认关闭态 gate，并严格区分“工程闭环”和“发布闭环”。

**Tech Stack:** Uni-app/Vue3、Spring Boot、MyBatis Plus、JUnit 5、Node test、MySQL SQL migration。

---

### Task 1: Referral failing tests

**Files:**
- Create: `yudao-mall-uniapp/tests/referral-page-smoke.test.mjs`
- Create: `yudao-mall-uniapp/tests/referral-api-alignment.test.mjs`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-promotion/src/test/java/cn/iocoder/yudao/module/promotion/controller/app/referral/AppReferralControllerTest.java`

**Step 1: Write the failing tests**
- 断言 `pages.json` 注册 `pages/referral/index`
- 断言 `sheep/api/promotion/referral.js` 对齐 `/promotion/referral/bind-inviter`、`/overview`、`/reward-ledger/page`
- 断言 `AppReferralController` 输出真实 contract

**Step 2: Run tests to verify they fail**
Run: `node --test yudao-mall-uniapp/tests/referral-page-smoke.test.mjs yudao-mall-uniapp/tests/referral-api-alignment.test.mjs`
Expected: FAIL because files/routes do not exist.

Run: `mvn -pl yudao-module-mall/yudao-module-promotion -Dtest=AppReferralControllerTest test`
Expected: FAIL because controller/test target does not exist.

**Step 3: Write minimal implementation**
- 新增 `AppReferralController` 与 VO
- 新增 uniapp referral API/page
- 从用户中心或分销真实页面挂入口

**Step 4: Run tests to verify they pass**
- 运行上述 Node 与 Maven 测试

**Step 5: Commit**
```bash
git add yudao-mall-uniapp ruoyi-vue-pro-master
git commit -m "feat(reserved): add referral runtime"
```

### Task 2: Technician Feed failing tests

**Files:**
- Create: `yudao-mall-uniapp/tests/technician-feed-page-smoke.test.mjs`
- Create: `yudao-mall-uniapp/tests/technician-feed-api-alignment.test.mjs`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/app/AppTechnicianFeedControllerTest.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/TechnicianFeedServiceImplTest.java`

**Step 1: Write the failing tests**
- 断言 `pages/technician/feed.vue` 和 `pages.json` 注册存在
- 断言 API 对齐 `/booking/technician/feed/page|like|comment/create`
- 断言 controller/service 支持分页、点赞幂等、评论进入 `REVIEWING`

**Step 2: Run tests to verify they fail**
Run: `node --test yudao-mall-uniapp/tests/technician-feed-page-smoke.test.mjs yudao-mall-uniapp/tests/technician-feed-api-alignment.test.mjs`
Expected: FAIL.

Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=AppTechnicianFeedControllerTest,TechnicianFeedServiceImplTest test`
Expected: FAIL.

**Step 3: Write minimal implementation**
- 新增 DO/Mapper/Service/Controller/VO/SQL
- 新增 uniapp page 与 API
- 在技师详情页增加真实入口

**Step 4: Run tests to verify they pass**
- 运行上述 Node 与 Maven 测试

**Step 5: Commit**
```bash
git add yudao-mall-uniapp ruoyi-vue-pro-master
git commit -m "feat(reserved): add technician feed runtime"
```

### Task 3: Gift Card failing tests

**Files:**
- Create: `yudao-mall-uniapp/tests/gift-card-page-smoke.test.mjs`
- Create: `yudao-mall-uniapp/tests/gift-card-api-alignment.test.mjs`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-promotion/src/test/java/cn/iocoder/yudao/module/promotion/controller/app/giftcard/AppGiftCardControllerTest.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-promotion/src/test/java/cn/iocoder/yudao/module/promotion/service/giftcard/GiftCardServiceImplTest.java`

**Step 1: Write the failing tests**
- 断言 `pages/gift-card/*` 路由存在
- 断言 API 对齐 gift-card contract 五个接口
- 断言 service/controller 支持模板分页、下单、详情、核销、退款申请

**Step 2: Run tests to verify they fail**
Run: `node --test yudao-mall-uniapp/tests/gift-card-page-smoke.test.mjs yudao-mall-uniapp/tests/gift-card-api-alignment.test.mjs`
Expected: FAIL.

Run: `mvn -pl yudao-module-mall/yudao-module-promotion -Dtest=AppGiftCardControllerTest,GiftCardServiceImplTest test`
Expected: FAIL.

**Step 3: Write minimal implementation**
- 新增 gift-card DO/Mapper/Service/Controller/VO/SQL
- 新增 uniapp page 与 API
- 在用户中心或资产相关页挂真实入口

**Step 4: Run tests to verify they pass**
- 运行上述 Node 与 Maven 测试

**Step 5: Commit**
```bash
git add yudao-mall-uniapp ruoyi-vue-pro-master
git commit -m "feat(reserved): add gift card runtime"
```

### Task 4: Runtime ledger and blocker docs

**Files:**
- Modify: `docs/plans/2026-03-11-miniapp-reserved-runtime-readiness-register-v1.md`
- Modify: `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-final-integration-v1.md`
- Modify: `docs/contracts/2026-03-14-miniapp-runtime-blocker-contract-closure-v1.md`
- Modify: `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
- Modify: `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
- Create: `hxy/07_memory_archive/handoffs/2026-03-24/reserved-runtime-closure-window-a.md`

**Step 1: Update docs after code is real**
- 明确三域当前变为 runtime implemented / cannot release
- 写清默认 gate 关闭、真实样本未核出、仍不可放量

**Step 2: Run guards**
Run: `git diff --check`
Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
Expected: PASS.

**Step 3: Commit**
```bash
git add docs hxy
git commit -m "docs(reserved): update runtime closure truth"
```

### Task 5: Final verification

**Files:**
- Verify only

**Step 1: Run uniapp tests**
Run: `node --test yudao-mall-uniapp/tests/referral-page-smoke.test.mjs yudao-mall-uniapp/tests/referral-api-alignment.test.mjs yudao-mall-uniapp/tests/technician-feed-page-smoke.test.mjs yudao-mall-uniapp/tests/technician-feed-api-alignment.test.mjs yudao-mall-uniapp/tests/gift-card-page-smoke.test.mjs yudao-mall-uniapp/tests/gift-card-api-alignment.test.mjs`

**Step 2: Run Java tests**
Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=AppTechnicianFeedControllerTest,TechnicianFeedServiceImplTest test`
Run: `mvn -pl yudao-module-mall/yudao-module-promotion -Dtest=AppReferralControllerTest,AppGiftCardControllerTest,GiftCardServiceImplTest test`

**Step 3: Run repo guards**
Run: `git diff --check`
Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

**Step 4: Commit final integration if needed**
```bash
git add yudao-mall-uniapp ruoyi-vue-pro-master docs hxy
git commit -m "feat(reserved): close reserved runtime implementation"
```
