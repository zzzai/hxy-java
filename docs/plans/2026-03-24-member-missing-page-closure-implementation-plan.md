# Member Missing-Page Closure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 补齐 `Member` 域等级页、资产总览页、标签页的真实页面、入口、app 读取链路和文档真值。

**Architecture:** `level` 直接复用现有 member app controller；`tag` 在 member 模块补 app 读取接口；`assets` 在 `yudao-server` 集成层聚合 member/pay/promotion 真实数据，避免 `member -> promotion` 循环依赖。

**Tech Stack:** Spring Boot, Java, Vue3 + uni-app, Node test, JUnit Mockito

---

### Task 1: 写前端失败测试

**Files:**
- Create: `yudao-mall-uniapp/tests/member-missing-pages-smoke.test.mjs`
- Create: `yudao-mall-uniapp/tests/member-api-alignment.test.mjs`

**Step 1: 写失败测试**
- 断言 `pages.json` 存在 `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag`
- 断言 `pages/user/info.vue` 有三个真实入口
- 断言前端 member API 文件存在并指向真实路径

**Step 2: 运行测试确认失败**
- Run: `node --test yudao-mall-uniapp/tests/member-missing-pages-smoke.test.mjs yudao-mall-uniapp/tests/member-api-alignment.test.mjs`

**Step 3: 最小实现通过**
- 新建前端 API 文件
- 增加路由和入口

**Step 4: 再跑测试确认通过**
- Run 同上

### Task 2: 写后端失败测试

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-member/src/test/java/cn/iocoder/yudao/module/member/controller/app/level/AppMemberLevelControllerTest.java`
- Create: `ruoyi-vue-pro-master/yudao-module-member/src/test/java/cn/iocoder/yudao/module/member/controller/app/tag/AppMemberTagControllerTest.java`
- Create: `ruoyi-vue-pro-master/yudao-server/src/test/java/cn/iocoder/yudao/server/controller/app/member/AppMemberAssetLedgerControllerTest.java`

**Step 1: 写失败测试**
- `level` 断言 controller 暴露等级列表和经验记录查询真值
- `tag` 断言当前登录用户标签被解析并返回
- `asset-ledger` 断言聚合输出 summary + list + degraded 字段

**Step 2: 运行测试确认失败**
- Run: `mvn -pl yudao-module-member,yudao-server -Dtest=AppMemberLevelControllerTest,AppMemberTagControllerTest,AppMemberAssetLedgerControllerTest test`

**Step 3: 最小实现通过**
- 补 tag controller / vo
- 补 asset-ledger controller / service / vo

**Step 4: 再跑测试确认通过**
- Run 同上

### Task 3: 实现前端页面

**Files:**
- Create: `yudao-mall-uniapp/pages/user/level.vue`
- Create: `yudao-mall-uniapp/pages/profile/assets.vue`
- Create: `yudao-mall-uniapp/pages/user/tag.vue`
- Modify: `yudao-mall-uniapp/pages.json`
- Modify: `yudao-mall-uniapp/pages/user/info.vue`

**Step 1: 补等级页**
- 展示当前等级卡、等级列表、经验流水

**Step 2: 补资产页**
- 展示钱包/积分/优惠券汇总卡
- 展示统一资产台账列表

**Step 3: 补标签页**
- 展示当前标签列表、空态、刷新态

**Step 4: 复跑前端测试**
- Run: `node --test yudao-mall-uniapp/tests/member-missing-pages-smoke.test.mjs yudao-mall-uniapp/tests/member-api-alignment.test.mjs`

### Task 4: 文档回填

**Files:**
- Modify: `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`
- Modify: `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md`
- Modify: `docs/plans/2026-03-11-miniapp-member-missing-page-activation-checklist-v1.md`
- Create: `docs/products/miniapp/2026-03-24-miniapp-member-missing-page-closure-review-v1.md`

**Step 1: 更新 route truth**
- 把三个页面改成真实 route

**Step 2: 更新能力结论**
- 区分 `已完成开发` 与 `仍不可放量`

**Step 3: 新增 closure review**
- 固定本轮结论、剩余 blocker、后续 release gate 条件

### Task 5: 最终验证

**Files:**
- Verify only

**Step 1: 跑前端测试**
- Run: `node --test yudao-mall-uniapp/tests/member-missing-pages-smoke.test.mjs yudao-mall-uniapp/tests/member-api-alignment.test.mjs`

**Step 2: 跑后端测试**
- Run: `mvn -pl yudao-module-member,yudao-server -Dtest=AppMemberLevelControllerTest,AppMemberTagControllerTest,AppMemberAssetLedgerControllerTest test`

**Step 3: 跑仓库校验**
- Run: `git diff --check`
- Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
