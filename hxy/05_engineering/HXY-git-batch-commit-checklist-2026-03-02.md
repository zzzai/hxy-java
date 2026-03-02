# HXY Git Batch Commit Checklist (2026-03-02)

## Goal
- Control repository noise from large amounts of `??` files.
- Keep commits reviewable and rollback-friendly.
- Avoid accidental full-repo commits.

## Hard Rules
1. Never use `git add .` at repo root.
2. Only stage whitelist paths by domain.
3. Every batch must have verification output before commit.
4. Keep local archive/raw recovery assets out of git.

## One-time Setup
Use repo root `/root/crmeb-java`:

```bash
git status --short | awk '$1=="??"{print $2}' | wc -l
git status --short | awk '$1=="??"{print $2}' | cut -d/ -f1 | sort | uniq -c | sort -nr
```

## Batch Plan

### Batch 1: Booking/Trade Backend
Scope:
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/**`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/**` (if this batch includes trade)

Commands:

```bash
git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking
# Optional when trade changes are included
# git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade

mvn -f ruoyi-vue-pro-master/pom.xml \
  -pl yudao-module-mall/yudao-module-booking -am \
  -Dtest=TechnicianCommissionServiceImplCancelCommissionTest,TechnicianCommissionSettlementServiceImplTest,TechnicianCommissionSettlementControllerTest,TechnicianCommissionSettlementMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

git diff --cached --name-only
git commit -m "feat(booking): settlement sla filters and reversal idempotency"
```

### Batch 2: Admin Overlay (Vue3)
Scope:
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/**`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/**`

Commands:

```bash
git add ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api
git add ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views

git diff --cached --name-only
git commit -m "feat(admin): settlement sla filters in overlay-vue3"
```

### Batch 3: SQL Migrations
Scope:
- `ruoyi-vue-pro-master/sql/mysql/hxy/**`

Commands:

```bash
git add ruoyi-vue-pro-master/sql/mysql/hxy

git diff --cached --name-only
git commit -m "feat(sql): hxy booking commission settlement menu and schema migrations"
```

### Batch 4: CI + Release Gates + Dev Scripts
Scope:
- `ruoyi-vue-pro-master/.github/workflows/**`
- `ruoyi-vue-pro-master/script/dev/**`

Commands:

```bash
git add ruoyi-vue-pro-master/.github/workflows
git add ruoyi-vue-pro-master/script/dev

git diff --cached --name-only
git commit -m "chore(ci): enforce hxy gates and stage checks"
```

### Batch 5: Governance Memory Docs
Scope:
- `hxy/00_governance/**`
- `hxy/06_roadmap/**`
- `hxy/07_memory_archive/handoffs/**`
- `hxy/99_index/memory-index.yaml`

Commands:

```bash
git add hxy/00_governance
git add hxy/06_roadmap
git add hxy/07_memory_archive/handoffs
# If exists
# git add hxy/99_index/memory-index.yaml

git diff --cached --name-only
git commit -m "docs(governance): update hxy baseline adr roadmap handoff"
```

## Daily Audit Commands

```bash
# Remaining untracked files
git status --short | awk '$1=="??"{print $2}'

# Remaining untracked count by top-level path
git status --short | awk '$1=="??"{print $2}' | cut -d/ -f1 | sort | uniq -c | sort -nr

# Ensure staged set is expected
git diff --cached --name-only
```

## Notes for This Repository
- The repository is currently in a long-running migration state; many `??` are historical or local assets.
- Treat `hxy/07_memory_archive/recovered_*`, `hxybase/`, and local archive bundles as local-only data.
- Commit frequently in narrow batches to keep PR/review and rollback safe.
