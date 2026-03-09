#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"

echo '[member-pages]'
for p in \
  yudao-mall-uniapp/pages/index/login.vue \
  yudao-mall-uniapp/pages/index/user.vue \
  yudao-mall-uniapp/pages/user/info.vue \
  yudao-mall-uniapp/pages/public/setting.vue \
  yudao-mall-uniapp/pages/app/sign.vue \
  yudao-mall-uniapp/pages/user/address/list.vue \
  yudao-mall-uniapp/pages/user/address/edit.vue \
  yudao-mall-uniapp/pages/user/wallet/money.vue \
  yudao-mall-uniapp/pages/user/wallet/score.vue \
  yudao-mall-uniapp/pages/coupon/list.vue \
  yudao-mall-uniapp/pages/activity/point/list.vue \
  yudao-mall-uniapp/pages/user/level.vue \
  yudao-mall-uniapp/pages/profile/assets.vue \
  yudao-mall-uniapp/pages/user/tag.vue; do
  if [[ -f "$ROOT/$p" ]]; then
    echo "[exists] $p"
  else
    echo "[missing] $p"
  fi
done

echo
printf '%s\n' '[member-frontend-api]'
rg -n "url:\s*['\"]/member/|url:\s*['\"]/pay/wallet|url:\s*['\"]/promotion/coupon|get-unused-count|point-activity" "$ROOT/yudao-mall-uniapp/sheep/api/member" "$ROOT/yudao-mall-uniapp/sheep/api/pay" "$ROOT/yudao-mall-uniapp/sheep/api/promotion" || true

echo
printf '%s\n' '[member-backend-controller]'
rg -n '@RequestMapping|@GetMapping|@PostMapping|@PutMapping' "$ROOT/ruoyi-vue-pro-master/yudao-module-member" -g'App*Controller.java' || true
