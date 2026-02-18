#!/usr/bin/env bash
set -euo pipefail

# D14: 服务商切换编排脚本
# - dry-run: 只做参数校验与脚本预演，不写库
# - apply: 先做配置快照，再执行参数写入/门店映射写入，最后 preflight 校验
#          若失败且开启自动回滚，则自动回滚配置与门店映射

APPID="${APPID:-}"
SP_MCHID="${SP_MCHID:-}"
SP_KEY="${SP_KEY:-}"
SUB_MCHID="${SUB_MCHID:-}"
SUB_APPID="${SUB_APPID:-}"
API_URL="${API_URL:-}"
SP_CERT_PATH="${SP_CERT_PATH:-}"
ENABLE_PAY="${ENABLE_PAY:-1}"
STORE_MAPPING_CSV="${STORE_MAPPING_CSV:-}"
MAPPING_CONFLICT_STRATEGY="${MAPPING_CONFLICT_STRATEGY:-block}"

APPLY=0
STRICT_PREFLIGHT=1
AUTO_ROLLBACK_ON_FAIL=1
NO_ALERT=0
NO_MAPPING=0

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_cutover_apply.sh [--apply] --appid wx... --sp-mchid 19... --sp-key xxx --sub-mchid 19...
    [--sub-appid wx...] [--api-url https://xxx] [--sp-cert-path /path/apiclient_cert.p12]
    [--enable-pay 0|1] [--store-mapping-csv FILE] [--mapping-conflict-strategy block|overwrite]
    [--no-mapping] [--non-strict-preflight] [--no-auto-rollback] [--no-alert]

说明：
  1) 默认 dry-run，不写库。
  2) --apply 时执行：
     - payment_config_snapshot.sh（先快照）
     - payment_service_provider_fill.sh --apply --no-snapshot（写配置）
     - payment_store_mapping_import.sh --apply --confirm（可选，写门店映射）
     - payment_preflight_check.sh（strict 默认开启）
  3) apply 失败时，默认自动回滚（配置 + 门店映射）。

参数：
  --apply                        真正执行切换（默认 dry-run）
  --appid                        小程序 AppID（必填）
  --sp-mchid                     服务商号（必填）
  --sp-key                       服务商 APIv2 Key（必填）
  --sub-mchid                    默认子商户号（必填）
  --sub-appid                    默认子商户 AppID（可选，默认同 appid）
  --api-url                      回调域名（可选）
  --sp-cert-path                 证书路径（可选）
  --enable-pay 0|1               支付开关（默认 1）
  --store-mapping-csv FILE       门店映射 CSV（可选）
  --mapping-conflict-strategy    block|overwrite（默认 block）
  --no-mapping                   跳过门店映射导入
  --non-strict-preflight         关闭 strict preflight（默认 strict）
  --no-auto-rollback             apply 失败时不自动回滚
  --no-alert                     异常时不推送机器人

退出码：
  0  成功
  2  失败（已回滚或待人工处理）
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apply)
      APPLY=1
      shift
      ;;
    --appid)
      APPID="$2"
      shift 2
      ;;
    --sp-mchid)
      SP_MCHID="$2"
      shift 2
      ;;
    --sp-key)
      SP_KEY="$2"
      shift 2
      ;;
    --sub-mchid)
      SUB_MCHID="$2"
      shift 2
      ;;
    --sub-appid)
      SUB_APPID="$2"
      shift 2
      ;;
    --api-url)
      API_URL="$2"
      shift 2
      ;;
    --sp-cert-path)
      SP_CERT_PATH="$2"
      shift 2
      ;;
    --enable-pay)
      ENABLE_PAY="$2"
      shift 2
      ;;
    --store-mapping-csv)
      STORE_MAPPING_CSV="$2"
      shift 2
      ;;
    --mapping-conflict-strategy)
      MAPPING_CONFLICT_STRATEGY="$2"
      shift 2
      ;;
    --no-mapping)
      NO_MAPPING=1
      shift
      ;;
    --non-strict-preflight)
      STRICT_PREFLIGHT=0
      shift
      ;;
    --no-auto-rollback)
      AUTO_ROLLBACK_ON_FAIL=0
      shift
      ;;
    --no-alert)
      NO_ALERT=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "未知参数: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${APPID}" || -z "${SP_MCHID}" || -z "${SP_KEY}" || -z "${SUB_MCHID}" ]]; then
  echo "错误：缺少必填参数（appid/sp-mchid/sp-key/sub-mchid）"
  exit 1
fi
if [[ -z "${SUB_APPID}" ]]; then
  SUB_APPID="${APPID}"
fi
if [[ "${ENABLE_PAY}" != "0" && "${ENABLE_PAY}" != "1" ]]; then
  echo "错误：--enable-pay 仅支持 0/1"
  exit 1
fi
if [[ "${MAPPING_CONFLICT_STRATEGY}" != "block" && "${MAPPING_CONFLICT_STRATEGY}" != "overwrite" ]]; then
  echo "错误：--mapping-conflict-strategy 仅支持 block|overwrite"
  exit 1
fi
if [[ ${NO_MAPPING} -eq 0 && -n "${STORE_MAPPING_CSV}" && ! -f "${STORE_MAPPING_CSV}" ]]; then
  echo "错误：门店映射文件不存在 -> ${STORE_MAPPING_CSV}"
  exit 1
fi
if [[ ${NO_MAPPING} -eq 0 && ${APPLY} -eq 1 && -z "${STORE_MAPPING_CSV}" ]]; then
  echo "提示：未提供 --store-mapping-csv，将仅切换默认子商户配置。"
fi

RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${ROOT_DIR}/runtime/payment_cutover_apply/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

run_step() {
  local step="$1"
  shift
  local log="${RUN_DIR}/${step}.log"
  set +e
  "$@" >"${log}" 2>&1
  local rc=$?
  set -e
  printf '%s' "${rc}"
}

echo "[cutover-apply] run_dir=${RUN_DIR}"
echo "[cutover-apply] mode=$([[ ${APPLY} -eq 1 ]] && echo apply || echo dry-run)"

snapshot_run_dir=""
snapshot_restore_sql=""
mapping_run_dir=""
mapping_rollback_sql=""
rolled_back=0

if [[ ${APPLY} -eq 0 ]]; then
  fill_rc="$(run_step "01_fill_dryrun" ./shell/payment_service_provider_fill.sh \
    --appid "${APPID}" \
    --sp-mchid "${SP_MCHID}" \
    --sp-key "${SP_KEY}" \
    --sub-mchid "${SUB_MCHID}" \
    --sub-appid "${SUB_APPID}" \
    ${API_URL:+--api-url "${API_URL}"} \
    ${SP_CERT_PATH:+--sp-cert-path "${SP_CERT_PATH}"} \
    --enable-pay "${ENABLE_PAY}")"

  if [[ ${NO_MAPPING} -eq 0 && -n "${STORE_MAPPING_CSV}" ]]; then
    mapping_rc="$(run_step "02_mapping_dryrun" ./shell/payment_store_mapping_import.sh \
      --csv "${STORE_MAPPING_CSV}" \
      --strict-submchid-format \
      --strict-submchid-unique \
      --conflict-strategy "${MAPPING_CONFLICT_STRATEGY}")"
  else
    mapping_rc="-"
  fi

  preflight_args=(./shell/payment_preflight_check.sh)
  if [[ ${STRICT_PREFLIGHT} -eq 1 ]]; then
    preflight_args+=(--strict)
  fi
  preflight_rc="$(run_step "03_preflight" "${preflight_args[@]}")"

  ready=1
  if [[ "${fill_rc}" != "0" ]]; then ready=0; fi
  if [[ "${mapping_rc}" != "-" && "${mapping_rc}" != "0" ]]; then ready=0; fi
  if [[ "${preflight_rc}" != "0" ]]; then ready=0; fi
else
  snapshot_rc="$(run_step "01_snapshot" ./shell/payment_config_snapshot.sh --tag "cutover-apply")"
  if [[ "${snapshot_rc}" == "0" ]]; then
    snapshot_run_dir="$(sed -n 's/^\[config-snapshot\] run_dir=//p' "${RUN_DIR}/01_snapshot.log" | tail -n 1 || true)"
    if [[ -n "${snapshot_run_dir}" ]]; then
      snapshot_restore_sql="${snapshot_run_dir}/restore.sql"
    fi
  fi

  fill_rc="$(run_step "02_fill_apply" ./shell/payment_service_provider_fill.sh \
    --apply --no-snapshot \
    --appid "${APPID}" \
    --sp-mchid "${SP_MCHID}" \
    --sp-key "${SP_KEY}" \
    --sub-mchid "${SUB_MCHID}" \
    --sub-appid "${SUB_APPID}" \
    ${API_URL:+--api-url "${API_URL}"} \
    ${SP_CERT_PATH:+--sp-cert-path "${SP_CERT_PATH}"} \
    --enable-pay "${ENABLE_PAY}")"

  if [[ ${NO_MAPPING} -eq 0 && -n "${STORE_MAPPING_CSV}" ]]; then
    mapping_rc="$(run_step "03_mapping_apply" ./shell/payment_store_mapping_import.sh \
      --csv "${STORE_MAPPING_CSV}" \
      --strict-submchid-format \
      --strict-submchid-unique \
      --conflict-strategy "${MAPPING_CONFLICT_STRATEGY}" \
      --apply --confirm)"
    mapping_run_dir="$(sed -n 's/^运行产物目录: //p' "${RUN_DIR}/03_mapping_apply.log" | tail -n 1 || true)"
    if [[ -n "${mapping_run_dir}" ]]; then
      mapping_rollback_sql="${mapping_run_dir}/rollback.sql"
    fi
  else
    mapping_rc="-"
  fi

  preflight_args=(./shell/payment_preflight_check.sh)
  if [[ ${STRICT_PREFLIGHT} -eq 1 ]]; then
    preflight_args+=(--strict)
  fi
  preflight_rc="$(run_step "04_preflight_after_apply" "${preflight_args[@]}")"

  ready=1
  if [[ "${snapshot_rc}" != "0" ]]; then ready=0; fi
  if [[ "${fill_rc}" != "0" ]]; then ready=0; fi
  if [[ "${mapping_rc}" != "-" && "${mapping_rc}" != "0" ]]; then ready=0; fi
  if [[ "${preflight_rc}" != "0" ]]; then ready=0; fi

  if [[ "${ready}" != "1" && ${AUTO_ROLLBACK_ON_FAIL} -eq 1 ]]; then
    rollback_cfg_rc="-"
    rollback_mapping_rc="-"
    if [[ -n "${snapshot_restore_sql}" && -f "${snapshot_restore_sql}" ]]; then
      rollback_cfg_rc="$(run_step "90_rollback_config" ./shell/payment_config_restore.sh --sql "${snapshot_restore_sql}" --apply)"
    fi
    if [[ -n "${mapping_rollback_sql}" && -f "${mapping_rollback_sql}" ]]; then
      rollback_mapping_rc="$(run_step "91_rollback_mapping" ./shell/payment_store_mapping_rollback.sh --sql "${mapping_rollback_sql}" --apply)"
    fi
    if [[ "${rollback_cfg_rc}" == "0" || "${rollback_mapping_rc}" == "0" ]]; then
      rolled_back=1
    fi
  fi
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
mode=$([[ ${APPLY} -eq 1 ]] && echo apply || echo dry-run)
strict_preflight=${STRICT_PREFLIGHT}
auto_rollback_on_fail=${AUTO_ROLLBACK_ON_FAIL}
no_mapping=${NO_MAPPING}
mapping_conflict_strategy=${MAPPING_CONFLICT_STRATEGY}
store_mapping_csv=${STORE_MAPPING_CSV}
snapshot_run_dir=${snapshot_run_dir}
snapshot_restore_sql=${snapshot_restore_sql}
mapping_run_dir=${mapping_run_dir}
mapping_rollback_sql=${mapping_rollback_sql}
fill_rc=${fill_rc}
mapping_rc=${mapping_rc}
preflight_rc=${preflight_rc}
rolled_back=${rolled_back}
ready=${ready}
run_dir=${RUN_DIR}
TXT

cat > "${REPORT_FILE}" <<MD
# 支付切换编排报告

- run_id: \`${RUN_ID}\`
- mode: **$([[ ${APPLY} -eq 1 ]] && echo apply || echo dry-run)**
- strict_preflight: \`${STRICT_PREFLIGHT}\`
- auto_rollback_on_fail: \`${AUTO_ROLLBACK_ON_FAIL}\`
- no_mapping: \`${NO_MAPPING}\`
- mapping_conflict_strategy: \`${MAPPING_CONFLICT_STRATEGY}\`
- store_mapping_csv: \`${STORE_MAPPING_CSV:-<none>}\`

| 步骤 | rc | 说明 |
|---|---:|---|
| fill | ${fill_rc} | 服务商参数写入（dry-run/apply） |
| mapping | ${mapping_rc} | 门店映射导入（可选） |
| preflight | ${preflight_rc} | 切换后预检 |

## 结果

- ready: **${ready}**
- rolled_back: **${rolled_back}**
- summary: \`${SUMMARY_FILE}\`
MD

echo "[cutover-apply] summary=${SUMMARY_FILE}"
echo "[cutover-apply] ready=${ready}, rolled_back=${rolled_back}"

if [[ "${ready}" != "1" ]]; then
  if [[ ${NO_ALERT} -eq 0 && -x "${ALERT_SCRIPT}" ]]; then
    "${ALERT_SCRIPT}" \
      --title "支付切换编排失败" \
      --content "mode=$([[ ${APPLY} -eq 1 ]] && echo apply || echo dry-run); ready=${ready}; rolled_back=${rolled_back}; fill=${fill_rc}; mapping=${mapping_rc}; preflight=${preflight_rc}; summary=${SUMMARY_FILE}" || true
  fi
  exit 2
fi

exit 0
