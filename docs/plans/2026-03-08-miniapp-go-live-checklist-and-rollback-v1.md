# MiniApp Go-Live Checklist & Rollback SOP v1 (2026-03-08)

## 1. Pre-Release Checklist
- Contract docs frozen and linked.
- Route map complete and approved.
- Error code matrix approved by frontend/backend/ops.
- StageB guard pipeline green.
- Key structured logs present (`runId/orderId/payRefundId/sourceBizNo/errorCode`).

## 2. Smoke Cases (Minimum)
1. Pay result success and degraded path.
2. After-sale create/list/detail/progress path.
3. Refund notify idempotent success and conflict.
4. Replay run summary + detail + sync tickets degraded path.
5. Coupon/points page no-data and business error rendering.

## 3. Rollback Triggers
- P0 route unavailable > 10 min.
- Conflict/error code explosion beyond threshold.
- Ticket sync failure backlog growing with no drain.

## 4. Rollback Actions
1. Disable high-risk UI entry points by config.
2. Keep core order/after-sale routes online.
3. Switch operators to replay job/manual SOP.
4. If needed, rollback required checks to StageA only:
   - `bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh`

## 5. Post-Rollback Audit
- Export affected `runId/orderId/payRefundId` set.
- Confirm replay and reconcile jobs drain backlog.
- Create incident summary with root cause and fix owner.
