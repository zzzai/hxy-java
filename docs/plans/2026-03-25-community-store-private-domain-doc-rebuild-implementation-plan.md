# Community Store Private Domain Doc Rebuild Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rebuild the missing 9-document community-store-private-domain product pack with a stable index, current-state truth, and future delivery guidance.

**Architecture:** Freeze the expected 9 filenames with a lightweight Node test, then create one overview document plus eight topical documents under `docs/products/miniapp/`. Update incident/context records so the repo no longer treats these docs as unrecovered.

**Tech Stack:** Markdown docs, Node test runner, repo guard scripts.

---

### Task 1: Lock the missing 9-doc pack with a failing test

**Files:**
- Create: `tests/community-store-private-domain-docs.test.mjs`

**Step 1: Write the failing test**
- Assert all 9 expected files exist under `docs/products/miniapp/`.
- Assert the overview doc references the other 8 docs.

**Step 2: Run test to verify it fails**
Run: `node --test tests/community-store-private-domain-docs.test.mjs`
Expected: FAIL because none of the 9 files exist yet.

**Step 3: Commit**
```bash
git add tests/community-store-private-domain-docs.test.mjs
git commit -m "test(docs): lock private-domain doc pack"
```

### Task 2: Create the 9 private-domain docs

**Files:**
- Create: `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-overview-prd-v1.md`
- Create: `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-public-to-private-acquisition-prd-v1.md`
- Create: `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-membership-consent-and-lead-capture-prd-v1.md`
- Create: `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-in-store-conversion-touchpoint-prd-v1.md`
- Create: `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-wecom-community-retention-prd-v1.md`
- Create: `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-member-segmentation-task-prd-v1.md`
- Create: `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-social-fission-growth-prd-v1.md`
- Create: `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-store-ops-sop-prd-v1.md`
- Create: `docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-metrics-attribution-delivery-plan-v1.md`

**Step 1: Write the minimal docs**
- Overview doc: describe current truth, target north star, document map, phased roadmap.
- 8 topic docs: each must include current state, target flow, dependencies, gaps, and delivery suggestions.

**Step 2: Run test to verify it passes**
Run: `node --test tests/community-store-private-domain-docs.test.mjs`
Expected: PASS.

**Step 3: Commit**
```bash
git add docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-*.md
git commit -m "docs(private-domain): rebuild community store doc pack"
```

### Task 3: Sync incident/context docs to mark the pack rebuilt

**Files:**
- Modify: `docs/plans/2026-03-24-workspace-loss-incident-retrospective-v1.md`
- Modify: `docs/plans/2026-03-24-project-context-record-v1.md`
- Modify: `hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md`
- Modify: `hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md`

**Step 1: Update status wording**
- Replace “未恢复/待重建” with “已于 2026-03-25 重建” for the 9-doc pack.
- Keep `booking-review-p3-go-no-go-package` as unrecovered if still missing.

**Step 2: Re-run targeted test**
Run: `node --test tests/community-store-private-domain-docs.test.mjs`
Expected: PASS.

**Step 3: Commit**
```bash
git add docs/plans/2026-03-24-workspace-loss-incident-retrospective-v1.md docs/plans/2026-03-24-project-context-record-v1.md hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md
git commit -m "docs(private-domain): sync rebuild status"
```

### Task 4: Full verification

**Files:**
- Verify only

**Step 1: Run targeted tests**
Run: `node --test tests/community-store-private-domain-docs.test.mjs`
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
git add docs/plans/2026-03-25-community-store-private-domain-doc-rebuild-design.md docs/plans/2026-03-25-community-store-private-domain-doc-rebuild-implementation-plan.md tests/community-store-private-domain-docs.test.mjs docs/products/miniapp/2026-03-23-miniapp-community-store-private-domain-*.md docs/plans/2026-03-24-workspace-loss-incident-retrospective-v1.md docs/plans/2026-03-24-project-context-record-v1.md hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md
git commit -m "docs(private-domain): rebuild lost community store pack"
git push origin main
```
