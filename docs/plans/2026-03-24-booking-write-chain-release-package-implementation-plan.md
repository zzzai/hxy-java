# Booking Write-Chain Release Package Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a durable release-package truth layer for `Booking` write-chain so release evidence, gray / rollback / sign-off, and No-Go judgement stop drifting across documents.

**Architecture:** Add a dedicated evidence ledger and release package review, then wire the latest booking runbook / closure review / project Go-No-Go package to those assets. Extend the existing evidence gate with document-truth checks so future changes cannot silently remove `Cannot Release / No-Go` boundaries.

**Tech Stack:** Markdown docs, bash gate script, Node test runner.

---

### Task 1: Lock the missing release-package assets with a failing test

**Files:**
- Modify: `tests/booking-write-chain-release-evidence-gate.test.mjs`

**Step 1: Write the failing test**
- Require the new evidence ledger and release package review docs to exist.
- Require both docs to contain `Cannot Release` and `No-Go` wording.

**Step 2: Run test to verify it fails**
Run: `node --test tests/booking-write-chain-release-evidence-gate.test.mjs`
Expected: FAIL because the new docs do not exist yet.

**Step 3: Commit**
```bash
git add tests/booking-write-chain-release-evidence-gate.test.mjs
git commit -m "test(booking): lock release package docs"
```

### Task 2: Add release-package docs and sync existing booking truth docs

**Files:**
- Create: `docs/plans/2026-03-24-miniapp-booking-write-chain-release-evidence-ledger-v1.md`
- Create: `docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-release-package-review-v1.md`
- Create: `hxy/07_memory_archive/handoffs/2026-03-24/booking-write-chain-release-package-window-a.md`
- Modify: `docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`
- Modify: `docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-closure-review-v1.md`
- Modify: `docs/products/miniapp/2026-03-24-miniapp-project-release-go-no-go-package-v1.md`

**Step 1: Write the minimal docs**
- Ledger must enumerate each real sample / allowlist / gray / rollback / sign-off gap and current status.
- Review must explain what P2/P3 artifacts prove, what remains blocked, and preserve `Doc Closed / Can Develop / Cannot Release / No-Go`.
- Existing booking docs must stop referring to “商品来源未闭环” as current blocker.

**Step 2: Run test to verify it passes**
Run: `node --test tests/booking-write-chain-release-evidence-gate.test.mjs`
Expected: PASS for the new doc existence + wording assertions.

**Step 3: Commit**
```bash
git add docs/plans/2026-03-24-miniapp-booking-write-chain-release-evidence-ledger-v1.md docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-release-package-review-v1.md hxy/07_memory_archive/handoffs/2026-03-24/booking-write-chain-release-package-window-a.md docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-closure-review-v1.md docs/products/miniapp/2026-03-24-miniapp-project-release-go-no-go-package-v1.md
git commit -m "docs(booking): add release package truth"
```

### Task 3: Extend the evidence gate with document-truth checks

**Files:**
- Modify: `ruoyi-vue-pro-master/script/dev/check_booking_write_chain_release_evidence_gate.sh`
- Modify: `tests/booking-write-chain-release-evidence-gate.test.mjs`

**Step 1: Write the failing assertion**
- Require the gate script to validate the new ledger/review docs.
- Require doc text checks for `Cannot Release`, `No-Go`, and `selftest pack 不能替代真实发布证据`.

**Step 2: Run test to verify it fails**
Run: `node --test tests/booking-write-chain-release-evidence-gate.test.mjs`
Expected: FAIL until the gate script checks the new docs.

**Step 3: Write minimal implementation**
- Add file/text checks in the gate script.
- Keep exit contract unchanged: `0=PASS`, `2=BLOCK`.

**Step 4: Run test to verify it passes**
Run: `node --test tests/booking-write-chain-release-evidence-gate.test.mjs`
Expected: PASS.

**Step 5: Commit**
```bash
git add ruoyi-vue-pro-master/script/dev/check_booking_write_chain_release_evidence_gate.sh tests/booking-write-chain-release-evidence-gate.test.mjs
git commit -m "test(booking): guard release package truth"
```

### Task 4: Full verification

**Files:**
- Verify only

**Step 1: Run targeted tests**
Run: `node --test tests/booking-write-chain-release-evidence-gate.test.mjs yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
Expected: PASS.

**Step 2: Run guard checks**
Run: `git diff --check`
Expected: PASS.

Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
Expected: PASS.

Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
Expected: PASS.

**Step 3: Commit and push**
```bash
git status --short
git add docs/plans/2026-03-24-booking-write-chain-release-package-design.md docs/plans/2026-03-24-booking-write-chain-release-package-implementation-plan.md docs/plans/2026-03-24-miniapp-booking-write-chain-release-evidence-ledger-v1.md docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-release-package-review-v1.md docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-closure-review-v1.md docs/products/miniapp/2026-03-24-miniapp-project-release-go-no-go-package-v1.md ruoyi-vue-pro-master/script/dev/check_booking_write_chain_release_evidence_gate.sh tests/booking-write-chain-release-evidence-gate.test.mjs hxy/07_memory_archive/handoffs/2026-03-24/booking-write-chain-release-package-window-a.md
git commit -m "docs(booking): close release package truth"
git push origin HEAD
```
