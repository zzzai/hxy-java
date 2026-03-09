#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"
BATCH="${2:-2026-03-10}"

DOCS=(
  "$ROOT/docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md"
  "$ROOT/docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md"
  "$ROOT/docs/products/miniapp/${BATCH}-miniapp-capability-status-ledger-v1.md"
  "$ROOT/docs/products/miniapp/${BATCH}-miniapp-domain-doc-coverage-matrix-v1.md"
)

for doc in "${DOCS[@]}"; do
  if [[ -f "$doc" ]]; then
    echo "[ok] $doc"
  else
    echo "[missing] $doc"
  fi
done

echo
printf '%s\n' '[frozen-snapshot]'
rg -n 'Frozen|Ready|Draft' "$ROOT/docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md" || true
