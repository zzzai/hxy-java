# Window A Handoff - Store Lifecycle Recheck (2026-03-05)

## Branch
- feat/ui-four-account-reconcile-ops

## Scope
- Product backend only; no overlay-vue3 UI changes.
- P1 lifecycle guard enhancement for batch traceability and recheck.

## Delivered
1. Added lifecycle batch log detail API:
   - `GET /product/store/lifecycle-batch-log/get?id=...`
   - Returns structured `detailView` parsed from `detailJson`.
   - Parse failure degrades to `detailParseError=true` (no 500).
2. Added lifecycle guard batch recheck API:
   - `POST /product/store/lifecycle-guard/recheck-by-batch`
   - Input: `logId` or `batchNo`.
   - Re-runs guard only; does not mutate lifecycle state.
3. Enhanced batch log snapshot fields:
   - `guardRuleVersion`
   - `guardConfigSnapshotJson`
4. Added SQL migration:
   - `2026-03-05-hxy-store-lifecycle-batch-log-guard-snapshot.sql`
5. Tests added/updated for controller/service/mapper paths.

## Risk / Notes
- `guardRuleVersion` currently derived from guard snapshot JSON hash.
- Recheck path supports malformed historical detail JSON via regex fallback storeId extraction.

## Verification
- Required guard checks + product module tests executed in A window (see commit report).

## Next
- UI side can consume `/lifecycle-batch-log/get` and `/lifecycle-guard/recheck-by-batch` for review panel and one-click recheck.
