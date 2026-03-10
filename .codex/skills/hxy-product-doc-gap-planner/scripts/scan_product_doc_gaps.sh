#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"
KEYWORD="${2:-member|booking|gift|referral|feed|search|wallet|coupon|logistics|compliance}"

printf '%s\n' '[core-docs]'
for doc in \
  "$ROOT/docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md" \
  "$ROOT/docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md" \
  "$ROOT/docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md" \
  "$ROOT/docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md"; do
  [[ -f "$doc" ]] && echo "[ok] $doc" || echo "[missing] $doc"
done

printf '\n%s\n' '[coverage-matrix-hits]'
rg -n "$KEYWORD|coverageScore|P0|P1|P2" "$ROOT/docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md" || true

printf '\n%s\n' '[capability-ledger-hits]'
rg -n "$KEYWORD|ACTIVE|PLANNED_RESERVED|DEPRECATED" "$ROOT/docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md" || true

printf '\n%s\n' '[domain-docs]'
rg -n "$KEYWORD" "$ROOT/docs/products/miniapp" "$ROOT/docs/contracts" "$ROOT/docs/plans" || true
