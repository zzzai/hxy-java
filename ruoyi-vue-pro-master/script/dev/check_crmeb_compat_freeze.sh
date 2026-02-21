#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPAT_DIR="${ROOT_DIR}/yudao-module-pay/src/main/java/cn/iocoder/yudao/module/pay/controller/compat/crmeb"
BASELINE_FILES="${ROOT_DIR}/script/dev/config/crmeb_compat_controller_files.baseline"
BASELINE_MAPPINGS="${ROOT_DIR}/script/dev/config/crmeb_compat_mappings.baseline"

require_file() {
  local path="$1"
  if [[ ! -f "${path}" ]]; then
    echo "[compat-freeze][FAIL] missing file: ${path}" >&2
    exit 2
  fi
}

require_file "${BASELINE_FILES}"
require_file "${BASELINE_MAPPINGS}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

current_files="${tmp_dir}/controller_files.current"
current_mappings="${tmp_dir}/mappings.current"

find "${COMPAT_DIR}" -maxdepth 1 -type f -name '*Controller.java' -printf '%f\n' | sort > "${current_files}"

if ! diff -u "${BASELINE_FILES}" "${current_files}" >/dev/null; then
  echo "[compat-freeze][FAIL] controller list changed from baseline" >&2
  diff -u "${BASELINE_FILES}" "${current_files}" >&2 || true
  exit 2
fi

while IFS= read -r file_name; do
  [[ -z "${file_name}" ]] && continue
  file_path="${COMPAT_DIR}/${file_name}"
  if ! rg -q '@Deprecated' "${file_path}"; then
    echo "[compat-freeze][FAIL] ${file_name} missing @Deprecated freeze marker" >&2
    exit 2
  fi
  if ! rg -q '兼容冻结说明' "${file_path}"; then
    echo "[compat-freeze][FAIL] ${file_name} missing freeze documentation note" >&2
    exit 2
  fi
done < "${current_files}"

rg --no-heading --line-number "@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\(" \
  "${COMPAT_DIR}"/*Controller.java \
  | sed -E "s#${COMPAT_DIR}/([^:]+):[0-9]+:#\\1:#" \
  | sort > "${current_mappings}"

if ! diff -u "${BASELINE_MAPPINGS}" "${current_mappings}" >/dev/null; then
  echo "[compat-freeze][FAIL] compat mappings changed from baseline" >&2
  diff -u "${BASELINE_MAPPINGS}" "${current_mappings}" >&2 || true
  exit 2
fi

echo "[compat-freeze] result=PASS"
