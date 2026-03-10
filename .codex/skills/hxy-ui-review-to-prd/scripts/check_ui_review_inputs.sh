#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"
KEYWORD="${2:-booking|member|search|home|coupon|wallet}"

printf '%s\n' '[ui-artifacts]'
rg -n "$KEYWORD" "$ROOT/ui" "$ROOT/docs/products/miniapp" -g'*.md' -g'*.json' -g'*.vue' || true

printf '\n%s\n' '[page-routes]'
rg -n '"root":|"path":' "$ROOT/yudao-mall-uniapp/pages.json" | rg "$KEYWORD" || true
rg --files "$ROOT/yudao-mall-uniapp/pages" | rg "$KEYWORD" || true

printf '\n%s\n' '[frontend-api]'
rg -n "url:\s*['\"]/" "$ROOT/yudao-mall-uniapp/sheep/api" | rg "$KEYWORD" || true

printf '\n%s\n' '[existing-docs]'
rg -n "$KEYWORD|field dictionary|acceptance|验收|错误码|降级" "$ROOT/docs/products/miniapp" "$ROOT/docs/contracts" "$ROOT/docs/plans" || true
