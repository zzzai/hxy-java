# 工单 SLA 配置中心化与门店生命周期守卫增强 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 完成工单 SLA 路由配置中心化（含管理端页面）并将门店生命周期守卫升级为全边界、按守卫项支持 `WARN/BLOCK` 渐进切换。

**Architecture:** 交易域新增 `trade-api` 聚合统计接口输出门店生命周期守卫数据；商品域通过 `trade-api + infra ConfigApi` 实现守卫判定，按守卫项读取模式并统一阻塞/审计。SLA 路由后端沿用 `trade_after_sale_review_ticket_route`，前端新增管理页直连现有控制器接口。

**Tech Stack:** Spring Boot, MyBatis-Plus, MyBatis-Plus-Join, JUnit5 + Mockito, Vue3 + TypeScript + Element Plus。

---

### Task 1: 交易域新增门店生命周期守卫统计 API（TDD）

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/api/store/TradeStoreLifecycleGuardApi.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/api/store/dto/TradeStoreLifecycleGuardStatRespDTO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/api/package-info.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/api/store/TradeStoreLifecycleGuardApiImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/dal/mysql/order/TradeOrderMapper.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/dal/mysql/aftersale/AfterSaleMapper.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/api/store/TradeStoreLifecycleGuardApiImplTest.java`

**Step 1: Write the failing test**

```java
@Test
void getStoreLifecycleGuardStat_shouldReturnPendingOrderAndInflightTicketCount() {
    when(tradeOrderMapper.selectCountByPickUpStoreIdAndStatuses(eq(88L), any())).thenReturn(5L);
    when(afterSaleMapper.selectCountByPickUpStoreIdAndStatuses(eq(88L), any())).thenReturn(2L);

    TradeStoreLifecycleGuardStatRespDTO resp = api.getStoreLifecycleGuardStat(88L);

    assertEquals(5L, resp.getPendingOrderCount());
    assertEquals(2L, resp.getInflightTicketCount());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl yudao-module-mall/yudao-module-trade -Dtest=TradeStoreLifecycleGuardApiImplTest test`
Expected: FAIL（`TradeStoreLifecycleGuardApi` / mapper 方法不存在）

**Step 3: Write minimal implementation**

```java
@Service
@Validated
public class TradeStoreLifecycleGuardApiImpl implements TradeStoreLifecycleGuardApi {

    @Resource
    private TradeOrderMapper tradeOrderMapper;
    @Resource
    private AfterSaleMapper afterSaleMapper;

    @Override
    public TradeStoreLifecycleGuardStatRespDTO getStoreLifecycleGuardStat(Long storeId) {
        Long pendingOrderCount = tradeOrderMapper.selectCountByPickUpStoreIdAndStatuses(
                storeId,
                Arrays.asList(TradeOrderStatusEnum.UNPAID.getStatus(),
                        TradeOrderStatusEnum.UNDELIVERED.getStatus(),
                        TradeOrderStatusEnum.DELIVERED.getStatus()));
        Long inflightTicketCount = afterSaleMapper.selectCountByPickUpStoreIdAndStatuses(
                storeId,
                AfterSaleStatusEnum.APPLYING_STATUSES);
        return new TradeStoreLifecycleGuardStatRespDTO()
                .setPendingOrderCount(pendingOrderCount == null ? 0L : pendingOrderCount)
                .setInflightTicketCount(inflightTicketCount == null ? 0L : inflightTicketCount);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -pl yudao-module-mall/yudao-module-trade -Dtest=TradeStoreLifecycleGuardApiImplTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/api/store/TradeStoreLifecycleGuardApi.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/api/store/dto/TradeStoreLifecycleGuardStatRespDTO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/api/store/TradeStoreLifecycleGuardApiImpl.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/dal/mysql/order/TradeOrderMapper.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/dal/mysql/aftersale/AfterSaleMapper.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/api/store/TradeStoreLifecycleGuardApiImplTest.java

git commit -m "feat(trade): add store lifecycle guard statistics api"
```

### Task 2: 商品域接入 guard 模式配置（WARN/BLOCK）并扩展错误码（TDD）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/pom.xml`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/enums/ErrorCodeConstants.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreServiceImpl.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/test/java/cn/iocoder/yudao/module/product/service/store/ProductStoreServiceImplTest.java`

**Step 1: Write failing tests for `BLOCK` and `WARN` semantics**

```java
@Test
void updateStoreLifecycle_shouldThrowWhenPendingOrdersAndModeBlock() {
    when(configApi.getConfigValueByKey("hxy.store.lifecycle.guard.pending-order.mode")).thenReturn("BLOCK");
    when(tradeStoreLifecycleGuardApi.getStoreLifecycleGuardStat(1010L))
            .thenReturn(new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(1L).setInflightTicketCount(0L));

    ServiceException ex = assertThrows(ServiceException.class,
            () -> productStoreService.updateStoreLifecycle(1010L, 35, "临时停业"));
    assertEquals(STORE_LIFECYCLE_CLOSE_BLOCKED_BY_PENDING_ORDER.getCode(), ex.getCode());
}

@Test
void updateStoreLifecycle_shouldAllowWhenPendingOrdersAndModeWarn() {
    when(configApi.getConfigValueByKey("hxy.store.lifecycle.guard.pending-order.mode")).thenReturn("WARN");
    when(tradeStoreLifecycleGuardApi.getStoreLifecycleGuardStat(1011L))
            .thenReturn(new TradeStoreLifecycleGuardStatRespDTO().setPendingOrderCount(2L).setInflightTicketCount(0L));

    assertDoesNotThrow(() -> productStoreService.updateStoreLifecycle(1011L, 35, "临时停业"));
    verify(storeAuditLogMapper).insert(argThat(log ->
            StringUtils.hasText(log.getReason()) && log.getReason().contains("LIFECYCLE_GUARD_WARN:pending-order:count=2")));
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl yudao-module-mall/yudao-module-product -Dtest=ProductStoreServiceImplTest test`
Expected: FAIL（缺少新错误码、ConfigApi/TradeStoreLifecycleGuardApi 依赖与守卫逻辑）

**Step 3: Write minimal implementation**

```java
private String evaluateDisableOrCloseGuards(Integer targetStatus, Integer targetLifecycleStatus, Long storeId) {
    // BLOCK -> throw; WARN -> accumulate reason
}
```

Key points:
- 新增 2 个错误码：
  - `STORE_LIFECYCLE_CLOSE_BLOCKED_BY_PENDING_ORDER`
  - `STORE_LIFECYCLE_CLOSE_BLOCKED_BY_INFLIGHT_TICKET`
- 新增依赖注入：`ConfigApi`、`TradeStoreLifecycleGuardApi`
- 守卫 key：
  - `hxy.store.lifecycle.guard.mapping.mode`
  - `hxy.store.lifecycle.guard.stock.mode`
  - `hxy.store.lifecycle.guard.stock-flow.mode`
  - `hxy.store.lifecycle.guard.pending-order.mode`
  - `hxy.store.lifecycle.guard.inflight-ticket.mode`
- `WARN` 命中只追加审计原因：`LIFECYCLE_GUARD_WARN:<guardKey>:count=<n>`
- 缺省/非法配置按 `BLOCK`

**Step 4: Run test to verify it passes**

Run: `mvn -pl yudao-module-mall/yudao-module-product -Dtest=ProductStoreServiceImplTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/pom.xml \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/enums/ErrorCodeConstants.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreServiceImpl.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/test/java/cn/iocoder/yudao/module/product/service/store/ProductStoreServiceImplTest.java

git commit -m "feat(product): add phased lifecycle guards with warn-block mode"
```

### Task 3: SLA 路由补充推荐规则与菜单 SQL、生命周期 guard 配置种子

**Files:**
- Modify: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-03-hxy-after-sale-review-ticket-route-config.sql`
- Create: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-03-hxy-store-lifecycle-guard-config-and-route-menu.sql`

**Step 1: Write failing verification checks**

```bash
rg -n "REFUND_LIMIT_CHANGED|review-ticket-route|hxy.store.lifecycle.guard.pending-order.mode" ruoyi-vue-pro-master/sql/mysql/hxy/*.sql
```

Expected: 未覆盖全部关键项（当前缺菜单+infra_config+推荐 RULE）

**Step 2: Implement SQL updates**

- 在 `2026-03-03-hxy-after-sale-review-ticket-route-config.sql` 增加 `RULE=REFUND_LIMIT_CHANGED` 的默认路由行。
- 新建 SQL：
  - 插入/幂等维护 5 个 `infra_config` 守卫 key，默认 `WARN`
  - 增加 `trade/review-ticket-route` 页面菜单（父级挂到 `after-sale`）
  - 增加按钮权限：query/create/update/delete
  - 授权 admin/operator

**Step 3: Run verification command**

Run: `rg -n "REFUND_LIMIT_CHANGED|hxy.store.lifecycle.guard|review-ticket-route" ruoyi-vue-pro-master/sql/mysql/hxy/*.sql`
Expected: 能检索到所有新增项

**Step 4: Commit**

```bash
git add \
  ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-03-hxy-after-sale-review-ticket-route-config.sql \
  ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-03-hxy-store-lifecycle-guard-config-and-route-menu.sql

git commit -m "chore(sql): seed lifecycle guard configs and review ticket route menu"
```

### Task 4: 管理端新增 SLA 路由配置页面与 API（TDD）

**Files:**
- Create: `/root/crmeb-java/.worktrees/ui-admin-vue3/src/api/mall/trade/reviewTicketRoute/index.ts`
- Create: `/root/crmeb-java/.worktrees/ui-admin-vue3/src/views/mall/trade/reviewTicketRoute/index.vue`

**Step 1: Write failing type-check entry points**

```ts
// in page setup
await ReviewTicketRouteApi.getReviewTicketRoutePage({ pageNo: 1, pageSize: 10 })
```

**Step 2: Run type check to confirm fail**

Run: `pnpm -C /root/crmeb-java/.worktrees/ui-admin-vue3 type-check`
Expected: FAIL（API 模块与页面不存在）

**Step 3: Add minimal implementation**

```ts
export const getReviewTicketRoutePage = async (params: any) => {
  return await request.get({ url: '/trade/after-sale/review-ticket-route/page', params })
}
```

Page capabilities:
- 搜索：scope/ruleCode/ticketType/severity/enabled
- 列表：scope, route key, escalateTo, slaMinutes, enabled, sort
- 操作：新增、编辑、启停（走 update）、删除
- `scope` 驱动动态字段显示

**Step 4: Run checks to verify pass**

Run: `pnpm -C /root/crmeb-java/.worktrees/ui-admin-vue3 type-check`
Expected: PASS

**Step 5: Commit (frontend repo)**

```bash
git -C /root/crmeb-java/.worktrees/ui-admin-vue3 add \
  src/api/mall/trade/reviewTicketRoute/index.ts \
  src/views/mall/trade/reviewTicketRoute/index.vue

git -C /root/crmeb-java/.worktrees/ui-admin-vue3 commit -m "feat(trade-ui): add after-sale review ticket route management page"
```

### Task 5: 回归与最终验证（verification-before-completion）

**Files:**
- Modify: `docs/plans/2026-03-03-ticket-sla-store-lifecycle-guard-design.md`（如实现偏差需同步）
- Modify: `hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md`（更新执行状态）

**Step 1: Run backend focused tests**

Run:
- `mvn -pl yudao-module-mall/yudao-module-trade -Dtest=TradeStoreLifecycleGuardApiImplTest,AfterSaleReviewTicketRouteServiceImplTest,DefaultAfterSaleReviewTicketRouteProviderTest test`
- `mvn -pl yudao-module-mall/yudao-module-product -Dtest=ProductStoreServiceImplTest test`

Expected: PASS

**Step 2: Run frontend checks**

Run: `pnpm -C /root/crmeb-java/.worktrees/ui-admin-vue3 lint && pnpm -C /root/crmeb-java/.worktrees/ui-admin-vue3 type-check`
Expected: PASS

**Step 3: Run workspace status check**

Run:
- `git status --short`
- `git -C /root/crmeb-java/.worktrees/ui-admin-vue3 status --short`

Expected: 仅包含本批文件改动

**Step 4: Commit docs/status updates**

```bash
git add \
  docs/plans/2026-03-03-ticket-sla-store-lifecycle-guard-design.md \
  hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md

git commit -m "docs: sync p1 lifecycle guard and sla route delivery status"
```
