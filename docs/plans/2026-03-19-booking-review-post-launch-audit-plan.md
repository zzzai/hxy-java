# Booking Review Post-Launch Audit Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete the next six follow-up tasks for booking review negative manager todo, covering residual audit, historical-data verification, manager truth review, admin enhancement backlog, release gate hardening, and final integration handoff.

**Architecture:** Keep the current admin-only manager todo capability conservative and truthful. Fix only issues proven by current code and tests, document what remains intentionally out of scope, and harden release truth so later windows cannot inflate it into automatic notify or release-ready status.

**Tech Stack:** Java/Spring Boot/MyBatis, Maven/JUnit, Vue 3 admin overlay, Markdown governance/product/contract/runbook docs.

---

### Task 1: Residual audit of backend and admin overlay

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`

**Step 1:** Audit real state transitions and historical lazy-init behavior.
**Step 2:** Add a failing test if backend allows invalid manager todo transitions.
**Step 3:** Implement the minimal backend guard.
**Step 4:** Re-run booking review tests.
**Step 5:** Record the residual conclusion in the final integration review.

### Task 2: Historical data and boundary verification

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`
- Modify: `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- Create: `docs/products/miniapp/2026-03-19-miniapp-booking-review-history-and-boundary-audit-v1.md`

**Step 1:** Add coverage for historical negative review lazy-init boundary.
**Step 2:** Verify how missing order/store truth behaves today.
**Step 3:** Document exact historical-data truth and no-go boundaries.

### Task 3: Manager account truth audit

**Files:**
- Create: `docs/products/miniapp/2026-03-19-miniapp-booking-review-manager-ownership-truth-review-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- Modify: `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`

**Step 1:** Audit current codebase for stable manager account truth.
**Step 2:** Freeze the conclusion as either "exists" or "not proven".
**Step 3:** Update PRD/contract wording so later work cannot assume `managerUserId` exists.

### Task 4: Admin enhancement candidate backlog

**Files:**
- Create: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`

**Step 1:** Inventory current admin UX gaps from real pages.
**Step 2:** Split into "can do next" vs "not now".
**Step 3:** Keep all candidates inside admin-only scope.

### Task 5: Release gate hardening

**Files:**
- Modify: `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- Modify: `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
- Modify: `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`

**Step 1:** Strengthen No-Go wording for auto-notify, reward, compensation, and release inflation.
**Step 2:** Ensure failure-mode doc matches actual backend fail-close semantics.
**Step 3:** Reassert `Can Develop / Cannot Release` in the capability ledger.

### Task 6: Final integration handoff

**Files:**
- Create: `hxy/07_memory_archive/handoffs/2026-03-19/booking-review-post-launch-window-a.md`
- Modify: `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`

**Step 1:** Summarize absorbed commits and current truths.
**Step 2:** Record what remains out of scope.
**Step 3:** Add downstream notes for future B/C/D/E windows.

### Verification

Run after implementation:
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest,BookingReviewControllerTest test`
