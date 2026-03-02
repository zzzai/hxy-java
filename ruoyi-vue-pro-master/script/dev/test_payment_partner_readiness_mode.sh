#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

TMP_DIR="${ROOT_DIR}/.tmp/test_payment_partner_readiness_mode"
rm -rf "${TMP_DIR}"
mkdir -p "${TMP_DIR}"

BAD_CONFIG="${TMP_DIR}/wx_partner_channel.invalid.json"
cat > "${BAD_CONFIG}" <<'JSON'
{
  "apiVersion": "v3",
  "partnerMode": true,
  "appId": "wx97fb30aed3983c2c",
  "subAppId": "wx97fb30aed3983c2c",
  "mchId": "1739427215",
  "subMchId": "",
  "apiV3Key": "0123456789ABCDEF0123456789ABCDEF",
  "certSerialNo": "2453A4F2A3DCCDCFC314C35935ACAC03A03F8B06",
  "privateKeyContent": "-----BEGIN PRIVATE KEY-----\\nKEY\\n-----END PRIVATE KEY-----",
  "publicKeyId": "PUB_KEY_ID_20260221"
}
JSON

assert_row_status() {
  local file="$1"
  local scenario_key="$2"
  local expect_status="$3"
  local actual
  actual="$(awk -F'\t' -v key="${scenario_key}" 'NR>1 && $2==key {print $6; exit}' "${file}")"
  if [[ "${actual}" != "${expect_status}" ]]; then
    echo "[FAIL] ${scenario_key} expect=${expect_status} actual=${actual:-<empty>} file=${file}" >&2
    exit 1
  fi
}

run_dev_warn() {
  local run_id="dev_warn_${RANDOM}"
  local out_base="${TMP_DIR}/dev"
  local out_dir="${out_base}/${run_id}"

  set +e
  bash script/dev/run_payment_abnormal_scenario_replay.sh \
    --run-id "${run_id}" \
    --out-base-dir "${out_base}" \
    --run-tests 0 \
    --run-notify-smoke 0 \
    --run-retry-policy-check 0 \
    --run-partner-readiness-check 1 \
    --run-reconcile-check 0 \
    --partner-readiness-config-file "${BAD_CONFIG}" \
    --partner-readiness-strict 0
  local rc=$?
  set -e

  if [[ "${rc}" != "0" ]]; then
    echo "[FAIL] dev mode expected rc=0, actual=${rc}" >&2
    exit 1
  fi

  assert_row_status "${out_dir}/scenario_result.tsv" "mchid_relation_mismatch" "WARN"
}

run_prod_block() {
  local run_id="prod_block_${RANDOM}"
  local out_base="${TMP_DIR}/prod"
  local out_dir="${out_base}/${run_id}"

  set +e
  bash script/dev/run_payment_abnormal_scenario_replay.sh \
    --run-id "${run_id}" \
    --out-base-dir "${out_base}" \
    --run-tests 0 \
    --run-notify-smoke 0 \
    --run-retry-policy-check 0 \
    --run-partner-readiness-check 1 \
    --run-reconcile-check 0 \
    --partner-readiness-config-file "script/dev/samples/wx_partner_channel.sample.json" \
    --partner-readiness-strict 1
  local rc=$?
  set -e

  if [[ "${rc}" != "2" ]]; then
    echo "[FAIL] strict mode expected rc=2, actual=${rc}" >&2
    exit 1
  fi

  assert_row_status "${out_dir}/scenario_result.tsv" "mchid_relation_mismatch" "BLOCK"
}

run_dev_warn
run_prod_block

echo "[PASS] partner readiness strict/non-strict behavior"
