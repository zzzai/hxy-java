# Window A Handoff - MiniApp Member Domain Integration (2026-03-10)

## 1. Objective
- Integrate the 03-10 member domain docs from B/C/D into A-side single source.
- Separate three concepts explicitly:
  - docs added
  - runtime capability actually exists
  - freeze readiness is still blocked by route truth drift

## 2. Integration Result
1. Member domain docs are present in current branch and have been recognized by A-side index.
2. Member domain coverage improved from “doc gap” to “Ready, pending route normalization”.
3. A-side capability ledger was updated to distinguish:
   - `ACTIVE`: auth-social runtime chain, profile/security, sign-in, address, wallet/point ledger
   - `PLANNED_RESERVED`: level page, asset overview aggregate, asset-ledger API
4. 03-09 Frozen baseline remains intact; 03-10 member additions are not treated as Frozen yet.

## 3. Hard Blockers to Freeze
1. Member PRD still uses prototype/alias routes that do not match real uniapp files:
   - `/pages/public/login` vs actual auth entry chain + `/pages/index/login`
   - `/pages/user/index` vs actual `/pages/index/user`
   - `/pages/user/sign-in` vs actual `/pages/app/sign`
   - `/pages/address/list` vs actual `/pages/user/address/list`
   - `/pages/point/mall` vs actual `/pages/activity/point/list`
2. Declared pages missing in current branch:
   - `/pages/user/level`
   - `/pages/profile/assets`
   - `/pages/user/tag`
3. `GET /member/asset-ledger/page` is still `PLANNED_RESERVED` and must stay behind `miniapp.asset.ledger`.
4. `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md` received 03-10 member extensions; treat these rows as Ready addendum until freeze review completes.

## 4. B/C/D Coordination Notes
1. Window B
   - Normalize member route truth before asking for Frozen.
   - Do not describe missing pages as already active.
2. Window C
   - Keep `ACTIVE_SET` and `RESERVED_SET` separate.
   - Keep `/member/asset-ledger/page` out of active release gates.
3. Window D
   - KPI/runbook should bind to actual active member pages only.
   - Missing-page capabilities must not enter active release denominator.

## 5. Next Required Closure
1. Correct member PRD/field dictionary route truth.
2. Decide whether auth capability truth is route-based or modal-based, and document it consistently.
3. Re-run A-side freeze review only after route truth and missing-page status are explicitly closed.
