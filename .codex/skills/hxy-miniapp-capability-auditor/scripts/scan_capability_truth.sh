#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"
KEYWORD="${2:-order|aftersale|refund|booking|coupon|point|wallet|address|search|member}"

echo "[routes]"
rg -n '"root":|"path":' "$ROOT/yudao-mall-uniapp/pages.json" | rg "$KEYWORD" || true
rg --files "$ROOT/yudao-mall-uniapp/pages" | rg "$KEYWORD" || true

echo
printf '%s\n' "[frontend-api]"
rg -n "url:\s*['\"]/[A-Za-z0-9/_?-]+" "$ROOT/yudao-mall-uniapp/sheep/api" | rg "$KEYWORD" || true

echo
printf '%s\n' "[backend-controller]"
rg -n '@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping' "$ROOT/ruoyi-vue-pro-master" -g'*App*Controller.java' | rg "$KEYWORD" || true

echo
printf '%s\n' "[docs]"
rg -n "$KEYWORD" "$ROOT/docs/products/miniapp" "$ROOT/docs/contracts" "$ROOT/docs/plans" || true
