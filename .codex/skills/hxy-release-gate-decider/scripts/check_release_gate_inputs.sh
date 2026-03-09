#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"
DOCS=(
  "$ROOT/docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md"
  "$ROOT/docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md"
  "$ROOT/docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md"
  "$ROOT/docs/contracts/2026-03-09-miniapp-reserved-disabled-gate-spec-v1.md"
  "$ROOT/docs/plans/2026-03-09-miniapp-degraded-pool-governance-v1.md"
  "$ROOT/docs/plans/2026-03-09-miniapp-release-gate-kpi-runbook-v1.md"
)
for doc in "${DOCS[@]}"; do
  [[ -f "$doc" ]] && echo "[ok] $doc" || echo "[missing] $doc"
done

echo
printf '%s\n' '[gate-keywords]'
rg -n 'Go|No-Go|degraded_pool|RESERVED_DISABLED|rollback|P0' "$ROOT/docs/products/miniapp" "$ROOT/docs/contracts" "$ROOT/docs/plans" || true
