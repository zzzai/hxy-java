#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"

printf '%s\n' '[frontend-booking-api]'
nl -ba "$ROOT/yudao-mall-uniapp/sheep/api/trade/booking.js" | sed -n '1,120p'

echo
printf '%s\n' '[backend-booking-controllers]'
rg -n '@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping' "$ROOT/ruoyi-vue-pro-master" -g'AppBooking*Controller.java' -g'AppTechnicianController.java' -g'AppTimeSlotController.java'

echo
printf '%s\n' '[booking-docs]'
rg -n 'booking|technician|slot|addon|cancel|1030004012|1030004016' "$ROOT/docs/products/miniapp" "$ROOT/docs/contracts" || true
