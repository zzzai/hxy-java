#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

TMP_DIR="${ROOT_DIR}/.tmp/test_update_wx_partner_channel"
rm -rf "${TMP_DIR}"
mkdir -p "${TMP_DIR}"

PRIVATE_KEY_FILE="${TMP_DIR}/private.pem"
PUBLIC_KEY_FILE="${TMP_DIR}/public.pem"
OUT_FILE="${TMP_DIR}/dry_run.out"
JSON_FILE="${TMP_DIR}/config.json"

cat > "${PRIVATE_KEY_FILE}" <<'EOF'
-----BEGIN PRIVATE KEY-----
AAA
-----END PRIVATE KEY-----
EOF

cat > "${PUBLIC_KEY_FILE}" <<'EOF'
-----BEGIN PUBLIC KEY-----\nBBB\n-----END PUBLIC KEY-----
EOF

DB_USER=test \
DB_PASSWORD=test \
DB_NAME=test \
PAY_APP_ID=1 \
SP_APP_ID=wx-test \
SP_MCH_ID=1739427215 \
SUB_MCH_ID=1106655249 \
API_V3_KEY=0123456789ABCDEF0123456789ABCDEF \
CERT_SERIAL_NO=ABCDEF1234567890 \
PUBLIC_KEY_ID=PUB_KEY_ID_TEST \
PRIVATE_KEY_FILE="${PRIVATE_KEY_FILE}" \
PUBLIC_KEY_FILE="${PUBLIC_KEY_FILE}" \
DRY_RUN=1 \
bash script/dev/update_wx_partner_channel.sh > "${OUT_FILE}"

sed -n '/^{/,$p' "${OUT_FILE}" > "${JSON_FILE}"
jq -e . "${JSON_FILE}" >/dev/null

EXPECT_CLASS="cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.weixin.WxPayClientConfig"
actual_class="$(jq -r '."@class" // empty' "${JSON_FILE}")"
if [[ "${actual_class}" != "${EXPECT_CLASS}" ]]; then
  echo "[FAIL] @class missing or mismatch, actual=${actual_class:-<empty>}" >&2
  exit 1
fi

python3 - <<'PY' "${JSON_FILE}"
import json
import sys

config_file = sys.argv[1]
with open(config_file, "r", encoding="utf-8") as f:
    data = json.load(f)

for key in ("privateKeyContent", "publicKeyContent"):
    value = data[key]
    if "\\n" in value:
        raise SystemExit(f"[FAIL] {key} still contains literal \\\\n")
    if "\n" not in value:
        raise SystemExit(f"[FAIL] {key} missing newline")

print("[PASS] update_wx_partner_channel.sh keeps class and pem newlines")
PY
