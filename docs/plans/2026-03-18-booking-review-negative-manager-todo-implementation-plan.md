# Booking Review Negative Manager Todo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an admin-only negative-review manager todo layer on top of the existing booking review recovery ledger, using store contact data as the first-version manager target and keeping release truth conservative.

**Architecture:** Reuse `booking_review` as the single persistence object and keep miniapp routes untouched. Extend the admin review ledger/detail/dashboard with separate manager-todo fields and actions instead of overloading `followStatus`, derive the target manager from `ProductStoreService#getStore`, and do not add any external message channel, compensation logic, or release inflation.

**Tech Stack:** Java/Spring Boot/MyBatis, MySQL SQL migration, Vue 3 + Element Plus admin overlay, existing booking review controller/service tests, markdown governance docs.

---

### Task 1: Freeze the first-version manager target source in docs before code

**Files:**
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- Modify: `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- Modify: `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- Test: `git diff --check`

**Step 1: Add the frozen product note**

Write the first-version target source explicitly:

```md
- 第一版“店长”只认门店主数据 `contactName/contactMobile`
- 当前不承诺账号级店长通知，不承诺站内信、微信或短信
```

**Step 2: Run diff check**

Run: `git diff --check`
Expected: PASS.

**Step 3: Commit**

```bash
git add \
  docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md \
  docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md \
  docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md
git commit -m "docs(booking-review): freeze manager todo target source"
```

### Task 2: Add failing backend tests for manager todo fields and actions

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java`

**Step 1: Write the failing service tests**

Add tests like:

```java
@Test
void shouldInitializeManagerTodoForNegativeReview() {
    // create negative review
    // assert managerTodoStatus == PENDING_CLAIM
    // assert managerContactName / managerContactMobile copied from store
    // assert claimDeadlineAt / firstActionDeadlineAt / closeDeadlineAt populated
}

@Test
void shouldClaimAndCloseManagerTodo() {
    // seed review row with manager todo pending
    // call claim -> assert claimed fields
    // call first action -> assert processing fields
    // call close -> assert closed fields
}
```

**Step 2: Write the failing controller tests**

Add tests like:

```java
@Test
void shouldClaimManagerTodo() {
    // mock login user id
    // call controller.claimManagerTodo(reqVO)
    // verify service method invoked
}
```

**Step 3: Run tests to verify they fail**

Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest,BookingReviewControllerTest test`
Expected: FAIL because manager-todo fields, request VOs, and service methods do not exist yet.

**Step 4: Commit nothing yet**

Do not commit red tests alone unless they are isolated on purpose.

### Task 3: Add schema migration, enums, and DO fields

**Files:**
- Create: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-18-hxy-booking-review-negative-manager-todo.sql`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewManagerTodoStatusEnum.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewNegativeTriggerTypeEnum.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewDO.java`

**Step 1: Add the SQL migration**

Add columns such as:

```sql
ALTER TABLE booking_review
  ADD COLUMN negative_trigger_type VARCHAR(32) NULL,
  ADD COLUMN manager_contact_name VARCHAR(64) NULL,
  ADD COLUMN manager_contact_mobile VARCHAR(32) NULL,
  ADD COLUMN manager_todo_status INT NULL,
  ADD COLUMN manager_claim_deadline_at DATETIME NULL,
  ADD COLUMN manager_first_action_deadline_at DATETIME NULL,
  ADD COLUMN manager_close_deadline_at DATETIME NULL,
  ADD COLUMN manager_claimed_by_user_id BIGINT NULL,
  ADD COLUMN manager_claimed_at DATETIME NULL,
  ADD COLUMN manager_first_action_at DATETIME NULL,
  ADD COLUMN manager_closed_at DATETIME NULL,
  ADD COLUMN manager_latest_action_remark VARCHAR(500) NULL,
  ADD COLUMN manager_latest_action_by_user_id BIGINT NULL;
```

**Step 2: Mirror the fields in `BookingReviewDO`**

Use Java fields matching the SQL names and existing `LocalDateTime` style.

**Step 3: Run the mapper-level compile path**

Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest test`
Expected: Still FAIL, but now due to missing service logic instead of missing fields.

### Task 4: Extend admin request/response VO and dashboard contracts

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewPageReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewDashboardRespVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerTodoClaimReqVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerTodoFirstActionReqVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerTodoCloseReqVO.java`

**Step 1: Add new response fields**

Expose fields like:

```java
private String negativeTriggerType;
private String managerContactName;
private String managerContactMobile;
private Integer managerTodoStatus;
private LocalDateTime managerClaimDeadlineAt;
private LocalDateTime managerFirstActionDeadlineAt;
private LocalDateTime managerCloseDeadlineAt;
private Long managerClaimedByUserId;
private LocalDateTime managerClaimedAt;
private LocalDateTime managerFirstActionAt;
private LocalDateTime managerClosedAt;
private String managerLatestActionRemark;
private Long managerLatestActionByUserId;
```

**Step 2: Add list-page filters**

Add optional filters such as:

```java
private Boolean onlyManagerTodo;
private Integer managerTodoStatus;
private String managerSlaStatus;
```

**Step 3: Extend dashboard summary**

Add counts like:

```java
private Long managerTodoPendingCount;
private Long managerTodoClaimTimeoutCount;
private Long managerTodoFirstActionTimeoutCount;
private Long managerTodoCloseTimeoutCount;
private Long managerTodoClosedCount;
```

### Task 5: Implement service logic without overloading `followStatus`

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/convert/BookingReviewConvert.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewMapper.java`
- Reference: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreService.java`

**Step 1: Initialize negative manager todo on review creation**

In `createReview`, when the review is negative:

```java
ProductStoreDO store = productStoreService.getStore(order.getStoreId());
review.setNegativeTriggerType(resolveNegativeTriggerType(reqVO));
review.setManagerContactName(store != null ? store.getContactName() : null);
review.setManagerContactMobile(store != null ? store.getContactMobile() : null);
review.setManagerTodoStatus(BookingReviewManagerTodoStatusEnum.PENDING_CLAIM.getStatus());
review.setManagerClaimDeadlineAt(review.getSubmitTime().plusMinutes(10));
review.setManagerFirstActionDeadlineAt(review.getSubmitTime().plusMinutes(30));
review.setManagerCloseDeadlineAt(review.getSubmitTime().plusHours(24));
```

Do not send messages. Do not alter miniapp behavior.

**Step 2: Add three explicit service methods**

Add methods like:

```java
void claimManagerTodo(Long reviewId, Long operatorId);
void recordManagerFirstAction(Long reviewId, Long operatorId, String remark);
void closeManagerTodo(Long reviewId, Long operatorId, String remark);
```

**Step 3: Keep `followStatus` separate**

Do not auto-write `followStatus=PROCESSING/CLOSED` from manager todo actions in v1.

**Step 4: Run tests**

Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest test`
Expected: PASS for service tests.

### Task 6: Add admin controller endpoints for claim, first action, and close

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java`

**Step 1: Add controller methods**

Expose endpoints:

```java
@PostMapping("/manager-todo/claim")
public CommonResult<Boolean> claimManagerTodo(@Valid @RequestBody BookingReviewManagerTodoClaimReqVO reqVO) { ... }

@PostMapping("/manager-todo/first-action")
public CommonResult<Boolean> recordManagerFirstAction(@Valid @RequestBody BookingReviewManagerTodoFirstActionReqVO reqVO) { ... }

@PostMapping("/manager-todo/close")
public CommonResult<Boolean> closeManagerTodo(@Valid @RequestBody BookingReviewManagerTodoCloseReqVO reqVO) { ... }
```

Use `SecurityFrameworkUtils.getLoginUserId()` just like reply/follow-status.

**Step 2: Run controller tests**

Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest test`
Expected: PASS.

### Task 7: Extend the admin overlay API client and review ledger pages

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`

**Step 1: Add API types and functions**

In `review.ts`, add types and requests like:

```ts
export interface BookingReviewManagerTodoClaimReq { reviewId: number }
export interface BookingReviewManagerTodoFirstActionReq { reviewId: number; remark: string }
export interface BookingReviewManagerTodoCloseReq { reviewId: number; remark: string }

export const claimManagerTodo = async (data: BookingReviewManagerTodoClaimReq) =>
  await request.post({ url: '/booking/review/manager-todo/claim', data })
```

**Step 2: Extend the ledger list**

Add:
- filter `只看店长待办`
- filter `店长待办状态`
- filter `SLA 状态`
- columns for contact, todo status, three deadlines, and SLA badge

**Step 3: Extend the detail page**

Add a dedicated “店长待办” card with:
- target contact
- status
- deadlines
- claim / first action / close buttons

Keep existing reply and follow-status cards unchanged.

**Step 4: Extend the dashboard**

Add cards for:
- 待认领
- 首次处理超时
- 闭环超时
- 已闭环

### Task 8: Update product, contract, runbook, gate, and ledger docs conservatively

**Files:**
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- Modify: `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- Modify: `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
- Modify: `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- Modify: `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- Modify: `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
- Modify: `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`

**Step 1: Add the new admin-only truth**

Document:
- trigger = negative review only
- channel = admin backlog only
- target = store contact as first-version manager truth
- no external push
- no reward/compensation

**Step 2: Keep release truth conservative**

Explicitly preserve:

```md
- 当前仍不能写成“系统已自动通知店长”
- 当前仍不能写成 booking review release-ready
- 当前仍没有外部消息通道证据
```

**Step 3: Run doc guards**

Run:
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

Expected: PASS.

### Task 9: Run focused verification and commit in small batches

**Files:**
- All files above

**Step 1: Run backend tests**

Run:

```bash
cd ruoyi-vue-pro-master
mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest,BookingReviewControllerTest test
```

Expected: PASS.

**Step 2: Run guard checks**

Run:

```bash
cd /root/crmeb-java
git diff --check
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh
CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh
```

Expected: PASS.

**Step 3: Commit by layer**

Suggested commits:

```bash
git add \
  ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-18-hxy-booking-review-negative-manager-todo.sql \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewManagerTodoStatusEnum.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewNegativeTriggerTypeEnum.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewDO.java
git commit -m "feat(booking-review): add manager todo schema"
```

```bash
git add \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewService.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java
git commit -m "feat(booking-review): add manager todo actions"
```

```bash
git add \
  ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts \
  ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue \
  ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue \
  ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue \
  docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md \
  docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md \
  docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md \
  docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md \
  docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md \
  docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md \
  docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md
git commit -m "docs(booking-review): document manager todo truth"
```

### Task 10: Final review before any release discussion

**Files:**
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`

**Step 1: Re-check the final wording**

Confirm these statements remain true:

```md
- admin backlog only
- no external manager message channel
- no automatic reward
- no automatic compensation
- Can Develop / Cannot Release
```

**Step 2: Do not widen release scope**

If any document implies “系统已自动通知店长” or “booking review 已可放量”, stop and fix the wording before merge.

