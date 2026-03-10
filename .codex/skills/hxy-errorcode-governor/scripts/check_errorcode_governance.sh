#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"
DOC_SCOPE=("$ROOT/docs/products/miniapp" "$ROOT/docs/contracts" "$ROOT/docs/plans")
REGISTER="$ROOT/docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

printf '%s\n' '[canonical-docs]'
for doc in \
  "$REGISTER" \
  "$ROOT/docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md"; do
  [[ -f "$doc" ]] && echo "[ok] $doc" || echo "[missing] $doc"
done

printf '\n%s\n' '[tbd-markers]'
rg -n 'TBD_[A-Z0-9_]+' "${DOC_SCOPE[@]}" || true

printf '\n%s\n' '[message-driven-risk]'
rg -n '按 message|按提示文案|按文案分支|按错误信息|message 分支' "${DOC_SCOPE[@]}" | rg -v '不按|禁止|不得|只按错误码|只能按错误码' || true

printf '\n%s\n' '[named-errorcodes-in-docs]'
rg --no-filename -o '[A-Z][A-Z0-9_]{5,}' "${DOC_SCOPE[@]}" | rg '^(BOOKING|PAY|PROMOTION|MINIAPP|TICKET|RESERVED|SIGN|COUPON|MEMBER|ADDRESS|WALLET|ORDER|REFUND)_' | sort -u | tee "$TMP_DIR/doc_named.txt" || true

printf '\n%s\n' '[numeric-errorcodes-in-docs]'
rg --no-filename -o '10[0-9]{8}|9[0-9]{6}' "${DOC_SCOPE[@]}" | sort -u | tee "$TMP_DIR/doc_num.txt" || true

if [[ -f "$REGISTER" ]]; then
  rg --no-filename -o '[A-Z][A-Z0-9_]{5,}' "$REGISTER" | rg '^(BOOKING|PAY|PROMOTION|MINIAPP|TICKET|RESERVED|SIGN|COUPON|MEMBER|ADDRESS|WALLET|ORDER|REFUND)_' | sort -u > "$TMP_DIR/reg_named.txt" || true
  rg --no-filename -o '10[0-9]{8}|9[0-9]{6}' "$REGISTER" | sort -u > "$TMP_DIR/reg_num.txt" || true

  printf '\n%s\n' '[codes-mentioned-outside-register]'
  comm -23 "$TMP_DIR/doc_named.txt" "$TMP_DIR/reg_named.txt" || true
  comm -23 "$TMP_DIR/doc_num.txt" "$TMP_DIR/reg_num.txt" || true
fi
