# Window A Handoff - Store Lifecycle Recheck Execute (2026-03-05)

## Branch
- feat/ui-four-account-reconcile-ops

## Scope
- Product backend only (no overlay page changes).
- P1 lifecycle guard second-phase: recheck execution ledger and query APIs.

## Delivered
1. New execute API (no state mutation):
   - `POST /product/store/lifecycle-guard/recheck-by-batch/execute`
   - Reuses guard evaluation and persists recheck ledger.
2. New recheck ledger APIs:
   - `GET /product/store/lifecycle-recheck-log/page`
   - `GET /product/store/lifecycle-recheck-log/get?id=...`
3. New table and schema migration:
   - `hxy_store_lifecycle_recheck_log`
   - SQL: `2026-03-05-hxy-store-lifecycle-recheck-log.sql`
4. Recheck traceability fields persisted:
   - `recheckNo/logId/batchNo/targetLifecycleStatus`
   - `totalCount/blockedCount/warningCount`
   - `detailJson/detailParseError`
   - `guardRuleVersion/guardConfigSnapshotJson/operator/source`
5. Robust degradation:
   - `detailJson` parse failure does not 500; returns `detailParseError=true`.

## Tests
- Controller tests: execute + recheck log page/get.
- Service tests: execute path persists ledger and does not mutate lifecycle status.
- Mapper tests: recheck log page wrapper filters.

## Notes
- Existing `recheck-by-batch` remains query-only.
- Execute and query paths are separated for audit clarity.
