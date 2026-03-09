#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"
TARGETS=("$ROOT/docs" "$ROOT/hxy" "$ROOT/ruoyi-vue-pro-master")
HEALTH_PATTERN='面诊|舌诊|体征检测|体征数据|体质标签|生理特征|abstractHealthTag|healthTag|biometric|diagnosis|tongue diagnosis|face diagnosis|physiological'

echo '[health-red-flags]'
rg -n "$HEALTH_PATTERN" "${TARGETS[@]}" || true

echo
printf '%s\n' '[userid-coupling-check]'
rg -n "userId.*($HEALTH_PATTERN)|($HEALTH_PATTERN).*userId" "${TARGETS[@]}" || true

echo
printf '%s\n' '[consent-purpose-audit]'
rg -n 'consentId|purposeCode|eb_label_policy|G0|G1|G2|audit' "$ROOT/docs" "$ROOT/hxy" || true
