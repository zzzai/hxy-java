#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:48080/admin-api}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"
CATEGORY_ID="${CATEGORY_ID:-101}"
SPU_ID="${SPU_ID:-30001}"
TEMPLATE_VERSION_ID="${TEMPLATE_VERSION_ID:-}"
PREVIEW_IDEMPOTENCY_KEY="${PREVIEW_IDEMPOTENCY_KEY:-SPU30001-V1-CHAIN}"
SUMMARY_FILE="${SUMMARY_FILE:-/tmp/hxy_template_chain/summary.txt}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_product_template_generate_chain.sh [options]

Options:
  --base-url <url>            后端 admin-api 地址，默认 http://127.0.0.1:48080/admin-api
  --access-token <token>      管理后台 Bearer token（必填）
  --category-id <id>          类目ID，默认 101
  --spu-id <id>               SPU ID，默认 30001
  --template-version-id <id>  模板版本ID，可选
  --idempotency-key <key>     提交幂等键，默认 SPU30001-V1-CHAIN
  --summary-file <file>       摘要输出文件
  -h, --help                  显示帮助

Exit Code:
  0: PASS
  2: BLOCK（链路失败）
  1: 执行异常
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="$2"
      shift 2
      ;;
    --access-token)
      ACCESS_TOKEN="$2"
      shift 2
      ;;
    --category-id)
      CATEGORY_ID="$2"
      shift 2
      ;;
    --spu-id)
      SPU_ID="$2"
      shift 2
      ;;
    --template-version-id)
      TEMPLATE_VERSION_ID="$2"
      shift 2
      ;;
    --idempotency-key)
      PREVIEW_IDEMPOTENCY_KEY="$2"
      shift 2
      ;;
    --summary-file)
      SUMMARY_FILE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${ACCESS_TOKEN}" ]]; then
  echo "ACCESS_TOKEN is required" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl command not found" >&2
  exit 1
fi

HAS_JQ=0
if command -v jq >/dev/null 2>&1; then
  HAS_JQ=1
fi

mkdir -p "$(dirname "${SUMMARY_FILE}")"

http_post() {
  local url="$1"
  local payload="$2"
  curl -sS -X POST "${url}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${payload}"
}

json_code() {
  local body="$1"
  if [[ "${HAS_JQ}" -eq 1 ]]; then
    jq -r '.code // ""' <<<"${body}"
  else
    sed -n 's/.*"code":[ ]*\([0-9-]\+\).*/\1/p' <<<"${body}" | head -n1
  fi
}

json_data_field() {
  local body="$1"
  local field="$2"
  if [[ "${HAS_JQ}" -eq 1 ]]; then
    jq -r ".data.${field} // \"\"" <<<"${body}"
  else
    sed -n "s/.*\"${field}\":[ ]*\"\\{0,1\\}\\([^\",}]*\\).*/\\1/p" <<<"${body}" | head -n1
  fi
}

template_version_json_field=""
if [[ -n "${TEMPLATE_VERSION_ID}" ]]; then
  template_version_json_field="\"templateVersionId\": ${TEMPLATE_VERSION_ID},"
fi

validate_payload="$(cat <<JSON
{
  "categoryId": ${CATEGORY_ID},
  ${template_version_json_field}
  "items": [
    {
      "attributeId": 1,
      "attrRole": 2,
      "required": true,
      "affectsPrice": true,
      "affectsStock": false
    }
  ]
}
JSON
)"

preview_payload="$(cat <<JSON
{
  "spuId": ${SPU_ID},
  "categoryId": ${CATEGORY_ID},
  ${template_version_json_field}
  "baseSku": {
    "price": 9800,
    "marketPrice": 12800,
    "costPrice": 5200,
    "stock": 100
  },
  "specSelections": [
    { "attributeId": 1, "optionIds": [11, 12] },
    { "attributeId": 2, "optionIds": [21, 22] }
  ]
}
JSON
)"

echo "== HXY Template Chain Check =="
echo "base_url=${BASE_URL}"
echo "category_id=${CATEGORY_ID}"
echo "spu_id=${SPU_ID}"

validate_resp="$(http_post "${BASE_URL}/product/template/validate" "${validate_payload}")"
validate_code="$(json_code "${validate_resp}")"
if [[ "${validate_code}" != "0" ]]; then
  echo "BLOCK: validate failed"
  echo "${validate_resp}"
  exit 2
fi
echo "validate=PASS"

preview_resp="$(http_post "${BASE_URL}/product/template/sku-generator/preview" "${preview_payload}")"
preview_code="$(json_code "${preview_resp}")"
if [[ "${preview_code}" != "0" ]]; then
  echo "BLOCK: preview failed"
  echo "${preview_resp}"
  exit 2
fi
preview_task_no="$(json_data_field "${preview_resp}" "taskNo")"
if [[ -z "${preview_task_no}" ]]; then
  echo "BLOCK: preview taskNo empty"
  echo "${preview_resp}"
  exit 2
fi
echo "preview=PASS taskNo=${preview_task_no}"

commit_payload="$(cat <<JSON
{
  "taskNo": "${preview_task_no}",
  "idempotencyKey": "${PREVIEW_IDEMPOTENCY_KEY}"
}
JSON
)"

commit_resp="$(http_post "${BASE_URL}/product/template/sku-generator/commit" "${commit_payload}")"
commit_code="$(json_code "${commit_resp}")"
if [[ "${commit_code}" != "0" ]]; then
  echo "BLOCK: commit failed"
  echo "${commit_resp}"
  exit 2
fi
commit_task_no="$(json_data_field "${commit_resp}" "taskNo")"
echo "commit=PASS taskNo=${commit_task_no}"

commit_again_resp="$(http_post "${BASE_URL}/product/template/sku-generator/commit" "${commit_payload}")"
commit_again_code="$(json_code "${commit_again_resp}")"
if [[ "${commit_again_code}" != "0" ]]; then
  echo "BLOCK: idempotent commit failed"
  echo "${commit_again_resp}"
  exit 2
fi
idempotent_hit="$(json_data_field "${commit_again_resp}" "idempotentHit")"
if [[ "${idempotent_hit}" != "true" && "${idempotent_hit}" != "1" ]]; then
  echo "BLOCK: second commit did not hit idempotency"
  echo "${commit_again_resp}"
  exit 2
fi
echo "idempotency=PASS"

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=PASS"
  echo "base_url=${BASE_URL}"
  echo "category_id=${CATEGORY_ID}"
  echo "spu_id=${SPU_ID}"
  echo "preview_task_no=${preview_task_no}"
  echo "commit_task_no=${commit_task_no}"
  echo "idempotent_hit=${idempotent_hit}"
} > "${SUMMARY_FILE}"

echo "summary_file=${SUMMARY_FILE}"
echo "result=PASS"
