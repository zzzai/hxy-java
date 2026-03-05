# Window A Handoff - Store SKU Stock Adjust Order (2026-03-06)

## Scope
- Branch: `feat/ui-four-account-reconcile-ops`
- Batch: Product backend minimum closed-loop for store SKU stock adjust order approval workflow.

## Delivered
- Added stock adjust order table SQL and indexes.
- Added order status enum and DAO/Mapper/VO contracts.
- Added admin APIs under `/product/store-sku/stock-adjust-order/*`:
  - `create`
  - `submit`
  - `approve`
  - `reject`
  - `cancel`
  - `get`
  - `page`
- Added workflow state machine:
  - `DRAFT -> PENDING -> APPROVED/REJECTED/CANCELLED`
- Approval reuses existing stock update chain and idempotent stock flow:
  - `bizType = MANUAL_<biz_type>`
  - `bizNo = orderNo`
- Added/updated audit fields:
  - `lastActionCode`
  - `lastActionOperator`
  - `lastActionTime`

## Verification
- `git diff --check` passed.
- `check_hxy_naming_guard.sh` passed.
- `check_hxy_memory_guard.sh` passed.
- Product targeted tests passed:
  - `ProductStoreSkuControllerTest`
  - `ProductStoreControllerTest`
  - `ProductStoreLifecycleBatchLogMapperTest`
  - `ProductStoreMappingServiceImplTest`
  - `ProductStoreServiceImplTest`
  - Total: 96 tests, `Failures: 0, Errors: 0`.

## Risks / Notes
- Existing untracked files outside this batch (`.codex/`, old handoff files) intentionally untouched.
- Overlay frontend integration remains in Window B scope.

## Next
- Merge/cherry-pick this backend batch to integration line.
- Then align overlay page with new stock adjust order APIs in UI window.
