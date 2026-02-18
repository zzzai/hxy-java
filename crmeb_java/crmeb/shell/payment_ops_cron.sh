#!/usr/bin/env bash
set -euo pipefail

# D6/D3/D7: 支付运维定时任务安装器（默认 dry-run）

APPLY=0
REMOVE=0
WINDOW_MINUTES="${WINDOW_MINUTES:-15}"
TAIL_LINES="${TAIL_LINES:-3000}"
MONITOR_CRON_EXPR="${MONITOR_CRON_EXPR:-*/5 * * * *}"
RECON_CRON_EXPR="${RECON_CRON_EXPR:-15 1 * * *}"
TICKET_CRON_EXPR="${TICKET_CRON_EXPR:-18 1 * * *}"
DECISION_TICKET_CRON_EXPR="${DECISION_TICKET_CRON_EXPR:-21 1 * * *}"
DRILL_CRON_EXPR="${DRILL_CRON_EXPR:-30 1 * * *}"
REPORT_CRON_EXPR="${REPORT_CRON_EXPR:-45 1 * * *}"
WARROOM_CRON_EXPR="${WARROOM_CRON_EXPR:-55 1 * * *}"
GONOGO_CRON_EXPR="${GONOGO_CRON_EXPR:-5 2 * * *}"
MORNING_BUNDLE_CRON_EXPR="${MORNING_BUNDLE_CRON_EXPR:-20 2 * * *}"
STATUS_CRON_EXPR="${STATUS_CRON_EXPR:-28 2 * * *}"
CONTRACT_CRON_EXPR="${CONTRACT_CRON_EXPR:-32 2 * * *}"
SLA_CRON_EXPR="${SLA_CRON_EXPR:-34 2 * * *}"
RETENTION_CRON_EXPR="${RETENTION_CRON_EXPR:-35 2 * * *}"
BOOKING_REPAIR_CRON_EXPR="${BOOKING_REPAIR_CRON_EXPR:-38 2 * * *}"
DECISION_CHAIN_CRON_EXPR="${DECISION_CHAIN_CRON_EXPR:-26 2 * * *}"
CRON_HEALTH_CRON_EXPR="${CRON_HEALTH_CRON_EXPR:-40 2 * * *}"
MAPPING_AUDIT_CRON_EXPR="${MAPPING_AUDIT_CRON_EXPR:-41 2 * * *}"
CUTOVER_GATE_CRON_EXPR="${CUTOVER_GATE_CRON_EXPR:-42 2 * * *}"
MAPPING_SMOKE_CRON_EXPR="${MAPPING_SMOKE_CRON_EXPR:-43 2 * * *}"
WEBHOOK_URL="${ALERT_WEBHOOK_URL:-}"
WEBHOOK_TYPE="${ALERT_WEBHOOK_TYPE:-wecom}"
REPORT_NOTIFY="${REPORT_NOTIFY:-0}"
TICKET_NOTIFY="${TICKET_NOTIFY:-0}"
DECISION_TICKET_NOTIFY="${DECISION_TICKET_NOTIFY:-0}"
TICKET_MAX_ROWS="${TICKET_MAX_ROWS:-200}"
TICKET_AMOUNT_P1_THRESHOLD_CENT="${TICKET_AMOUNT_P1_THRESHOLD_CENT:-100000}"
TICKET_OWNER_MAP_FILE="${TICKET_OWNER_MAP_FILE:-}"
TICKET_OWNER_DEFAULT="${TICKET_OWNER_DEFAULT:-payment-ops}"
TICKET_OWNER_P1="${TICKET_OWNER_P1:-payment-oncall}"
WARROOM_NOTIFY="${WARROOM_NOTIFY:-0}"
WARROOM_WINDOW_HOURS="${WARROOM_WINDOW_HOURS:-72}"
GONOGO_NOTIFY="${GONOGO_NOTIFY:-0}"
GONOGO_REQUIRE_APPLY_READY="${GONOGO_REQUIRE_APPLY_READY:-0}"
GONOGO_REQUIRE_BOOKING_REPAIR_PASS="${GONOGO_REQUIRE_BOOKING_REPAIR_PASS:-0}"
GONOGO_REQUIRE_MAPPING_SMOKE_GREEN="${GONOGO_REQUIRE_MAPPING_SMOKE_GREEN:-0}"
MORNING_BUNDLE_NOTIFY="${MORNING_BUNDLE_NOTIFY:-0}"
MORNING_BUNDLE_REQUIRE_APPLY_READY="${MORNING_BUNDLE_REQUIRE_APPLY_READY:-0}"
MORNING_BUNDLE_REQUIRE_BOOKING_REPAIR_PASS="${MORNING_BUNDLE_REQUIRE_BOOKING_REPAIR_PASS:-0}"
MORNING_BUNDLE_REQUIRE_MAPPING_SMOKE_GREEN="${MORNING_BUNDLE_REQUIRE_MAPPING_SMOKE_GREEN:-0}"
MORNING_BUNDLE_WINDOW_HOURS="${MORNING_BUNDLE_WINDOW_HOURS:-72}"
MORNING_BUNDLE_WINDOW_MINUTES="${MORNING_BUNDLE_WINDOW_MINUTES:-15}"
MORNING_BUNDLE_TAIL_LINES="${MORNING_BUNDLE_TAIL_LINES:-3000}"
MORNING_BUNDLE_STRICT_PREFLIGHT="${MORNING_BUNDLE_STRICT_PREFLIGHT:-0}"
STATUS_NOTIFY="${STATUS_NOTIFY:-0}"
STATUS_REFRESH="${STATUS_REFRESH:-0}"
STATUS_REFRESH_REQUIRE_APPLY_READY="${STATUS_REFRESH_REQUIRE_APPLY_READY:-0}"
STATUS_REQUIRE_BOOKING_REPAIR_PASS="${STATUS_REQUIRE_BOOKING_REPAIR_PASS:-0}"
STATUS_REQUIRE_DECISION_CHAIN_PASS="${STATUS_REQUIRE_DECISION_CHAIN_PASS:-0}"
STATUS_MAX_SUMMARY_AGE_MINUTES="${STATUS_MAX_SUMMARY_AGE_MINUTES:-240}"
STATUS_MAX_RECON_AGE_DAYS="${STATUS_MAX_RECON_AGE_DAYS:-2}"
STATUS_MAX_DAILY_REPORT_AGE_DAYS="${STATUS_MAX_DAILY_REPORT_AGE_DAYS:-2}"
CONTRACT_NOTIFY="${CONTRACT_NOTIFY:-0}"
CONTRACT_REQUIRE_ALL="${CONTRACT_REQUIRE_ALL:-0}"
SLA_NOTIFY="${SLA_NOTIFY:-0}"
SLA_LOOKBACK_DAYS="${SLA_LOOKBACK_DAYS:-14}"
SLA_DAYS="${SLA_DAYS:-1}"
RETENTION_NOTIFY="${RETENTION_NOTIFY:-0}"
RETENTION_KEEP_DAYS="${RETENTION_KEEP_DAYS:-7}"
BOOKING_REPAIR_NOTIFY="${BOOKING_REPAIR_NOTIFY:-0}"
BOOKING_REPAIR_WINDOW_HOURS="${BOOKING_REPAIR_WINDOW_HOURS:-72}"
BOOKING_REPAIR_APPLY="${BOOKING_REPAIR_APPLY:-0}"
DECISION_CHAIN_NOTIFY="${DECISION_CHAIN_NOTIFY:-0}"
CRON_HEALTH_NOTIFY="${CRON_HEALTH_NOTIFY:-0}"
MAPPING_AUDIT_NOTIFY="${MAPPING_AUDIT_NOTIFY:-0}"
MAPPING_AUDIT_STRICT_MISSING="${MAPPING_AUDIT_STRICT_MISSING:-0}"
CUTOVER_GATE_NOTIFY="${CUTOVER_GATE_NOTIFY:-0}"
CUTOVER_GATE_REQUIRE_APPLY_READY="${CUTOVER_GATE_REQUIRE_APPLY_READY:-0}"
CUTOVER_GATE_REQUIRE_BOOKING_REPAIR_PASS="${CUTOVER_GATE_REQUIRE_BOOKING_REPAIR_PASS:-0}"
CUTOVER_GATE_REQUIRE_MAPPING_SMOKE_GREEN="${CUTOVER_GATE_REQUIRE_MAPPING_SMOKE_GREEN:-0}"
CUTOVER_GATE_MAPPING_STRICT_MISSING="${CUTOVER_GATE_MAPPING_STRICT_MISSING:-0}"
CUTOVER_GATE_REQUIRE_MAPPING_GREEN="${CUTOVER_GATE_REQUIRE_MAPPING_GREEN:-0}"
CUTOVER_GATE_REQUIRE_MOCK_GREEN="${CUTOVER_GATE_REQUIRE_MOCK_GREEN:-0}"
CUTOVER_GATE_REQUIRE_HQ_REFUND_POLICY="${CUTOVER_GATE_REQUIRE_HQ_REFUND_POLICY:-1}"
CUTOVER_GATE_OWNER_MAP_FILE="${CUTOVER_GATE_OWNER_MAP_FILE:-}"
MAPPING_SMOKE_NOTIFY="${MAPPING_SMOKE_NOTIFY:-0}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="${ROOT_DIR}/runtime"
LOCK_DIR="${RUNTIME_DIR}/locks"
mkdir -p "${RUNTIME_DIR}"

BEGIN_MARK="# >>> payment ops managed >>>"
END_MARK="# <<< payment ops managed <<<"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_ops_cron.sh [--apply] [--remove] [--webhook URL] [--type wecom|dingtalk|feishu] [--report-notify 0|1] [--report-cron EXPR] [--ticket-notify 0|1] [--ticket-cron EXPR] [--ticket-max-rows N] [--ticket-amount-p1-threshold-cent N] [--ticket-owner-map-file FILE] [--ticket-owner-default NAME] [--ticket-owner-p1 NAME] [--decision-ticket-notify 0|1] [--decision-ticket-cron EXPR] [--warroom-notify 0|1] [--warroom-cron EXPR] [--warroom-window-hours N] [--go-nogo-notify 0|1] [--go-nogo-cron EXPR] [--go-nogo-require-apply-ready 0|1] [--go-nogo-require-booking-repair-pass 0|1] [--go-nogo-require-mapping-smoke-green 0|1] [--morning-bundle-notify 0|1] [--morning-bundle-cron EXPR] [--morning-bundle-require-apply-ready 0|1] [--morning-bundle-require-booking-repair-pass 0|1] [--morning-bundle-require-mapping-smoke-green 0|1] [--morning-bundle-window-hours N] [--morning-bundle-window N] [--morning-bundle-tail N] [--morning-bundle-strict-preflight 0|1] [--status-notify 0|1] [--status-cron EXPR] [--status-refresh 0|1] [--status-refresh-require-apply-ready 0|1] [--status-require-booking-repair-pass 0|1] [--status-require-decision-chain-pass 0|1] [--status-max-summary-age-minutes N] [--status-max-recon-age-days N] [--status-max-daily-report-age-days N] [--contract-notify 0|1] [--contract-cron EXPR] [--contract-require-all 0|1] [--sla-notify 0|1] [--sla-cron EXPR] [--sla-lookback-days N] [--sla-days N] [--retention-notify 0|1] [--retention-cron EXPR] [--retention-keep-days N] [--booking-repair-notify 0|1] [--booking-repair-cron EXPR] [--booking-repair-window-hours N] [--booking-repair-apply 0|1] [--decision-chain-notify 0|1] [--decision-chain-cron EXPR] [--cron-health-notify 0|1] [--cron-health-cron EXPR] [--mapping-audit-notify 0|1] [--mapping-audit-cron EXPR] [--mapping-audit-strict-missing 0|1] [--mapping-smoke-notify 0|1] [--mapping-smoke-cron EXPR] [--cutover-gate-notify 0|1] [--cutover-gate-cron EXPR] [--cutover-gate-require-apply-ready 0|1] [--cutover-gate-require-booking-repair-pass 0|1] [--cutover-gate-require-mapping-smoke-green 0|1] [--cutover-gate-mapping-strict-missing 0|1] [--cutover-gate-require-mapping-green 0|1] [--cutover-gate-require-mock-green 0|1] [--cutover-gate-require-hq-refund-policy 0|1] [--cutover-gate-owner-map-file FILE] [--db-host HOST] [--db-port PORT] [--db-name NAME] [--db-user USER] [--db-pass PASS] [--mysql-defaults-file FILE] [--window N] [--tail N]

参数：
  --apply            真正写入当前用户 crontab（默认 dry-run）
  --remove           删除已安装的 payment 托管任务块
  --webhook URL      告警机器人 webhook
  --type TYPE        告警类型：wecom/dingtalk/feishu（默认读取 ALERT_WEBHOOK_TYPE）
  --report-notify N  日报任务是否推送机器人（0=只生成文件, 1=生成并推送，默认 0）
  --report-cron EXPR 日报任务 cron 表达式（默认 45 1 * * *）
  --ticket-notify N  对账工单任务开关（0=关闭, 1=开启，默认 0）
  --ticket-cron EXPR 对账工单任务 cron 表达式（默认 18 1 * * *）
  --ticket-max-rows N 对账工单明细展示行数（默认 200）
  --ticket-amount-p1-threshold-cent N 金额升级P1阈值（分，默认 100000）
  --ticket-owner-map-file FILE  工单 owner 规则文件（可选）
  --ticket-owner-default NAME   工单默认 owner（默认 payment-ops）
  --ticket-owner-p1 NAME        工单 P1 默认 owner（默认 payment-oncall）
  --decision-ticket-notify N 判定链路工单任务开关（0=关闭, 1=开启，默认 0）
  --decision-ticket-cron EXPR 判定链路工单任务 cron 表达式（默认 21 1 * * *）
  --warroom-notify N warroom 值守看板任务开关（0=关闭, 1=开启，默认 0）
  --warroom-cron EXPR warroom 任务 cron 表达式（默认 55 1 * * *）
  --warroom-window-hours N warroom 幂等回归窗口小时（默认 72）
  --go-nogo-notify N  go/no-go 判定任务开关（0=关闭, 1=开启，默认 0）
  --go-nogo-cron EXPR go/no-go 任务 cron 表达式（默认 5 2 * * *）
  --go-nogo-require-apply-ready 0|1   判定时是否要求 apply-ready（默认 0）
  --go-nogo-require-booking-repair-pass 0|1   判定时是否要求 booking_verify_repair=PASS（默认 0）
  --go-nogo-require-mapping-smoke-green 0|1 判定时是否要求 mapping_smoke=GREEN（默认 0）
  --morning-bundle-notify N    早班一键包任务开关（0=关闭, 1=开启，默认 0）
  --morning-bundle-cron EXPR   早班一键包 cron 表达式（默认 20 2 * * *）
  --morning-bundle-require-apply-ready 0|1   一键包内部 go/no-go 是否要求 apply-ready（默认 0）
  --morning-bundle-require-booking-repair-pass 0|1   一键包内部 go/no-go 是否要求 booking_verify_repair=PASS（默认 0）
  --morning-bundle-require-mapping-smoke-green 0|1 一键包内部 go/no-go 是否要求 mapping_smoke=GREEN（默认 0）
  --morning-bundle-window-hours N   一键包 warroom 幂等窗口小时（默认 72）
  --morning-bundle-window N     一键包 ops_daily 窗口分钟（默认 15）
  --morning-bundle-tail N       一键包 ops_daily tail 行数（默认 3000）
  --morning-bundle-strict-preflight 0|1  一键包是否 strict preflight（默认 0）
  --status-notify N            值守总览任务开关（0=关闭, 1=开启，默认 0）
  --status-cron EXPR           值守总览 cron 表达式（默认 28 2 * * *）
  --status-refresh 0|1         值守总览是否先 refresh(morning-bundle)（默认 0）
  --status-refresh-require-apply-ready 0|1 refresh 时 apply-ready 门槛（默认 0）
  --status-require-booking-repair-pass 0|1 值守总览是否要求 booking_verify_repair=PASS（默认 0）
  --status-require-decision-chain-pass 0|1 值守总览是否要求 decision_chain_smoke=PASS（默认 0）
  --status-max-summary-age-minutes N    值守总览 summary 新鲜度阈值（分钟，默认 240）
  --status-max-recon-age-days N         值守总览 reconcile summary 新鲜度阈值（天，默认 2）
  --status-max-daily-report-age-days N  值守总览日报新鲜度阈值（天，默认 2）
  --contract-notify N          summary 协议回归任务开关（0=关闭, 1=开启，默认 0）
  --contract-cron EXPR         summary 协议回归 cron 表达式（默认 32 2 * * *）
  --contract-require-all 0|1   协议回归是否要求所有 summary 存在（默认 0）
  --sla-notify N               对账SLA守卫任务开关（0=关闭, 1=开启，默认 0）
  --sla-cron EXPR              对账SLA守卫 cron 表达式（默认 34 2 * * *）
  --sla-lookback-days N        SLA守卫回看天数（默认 14）
  --sla-days N                 SLA允许天数（默认 1）
  --retention-notify N         留存清理任务开关（0=关闭, 1=开启，默认 0）
  --retention-cron EXPR        留存清理任务 cron 表达式（默认 35 2 * * *）
  --retention-keep-days N      留存天数（默认 7，任务执行时使用 --apply）
  --booking-repair-notify N    预约核销修复巡检任务开关（0=关闭, 1=开启，默认 0）
  --booking-repair-cron EXPR   预约核销修复巡检 cron 表达式（默认 38 2 * * *）
  --booking-repair-window-hours N  预约核销修复巡检窗口小时（默认 72）
  --booking-repair-apply 0|1   预约核销修复任务是否执行自动修复（默认 0=dry-run）
  --decision-chain-notify N    判定链路同日优先离线自测任务开关（0=关闭, 1=开启，默认 0）
  --decision-chain-cron EXPR   判定链路离线自测 cron 表达式（默认 26 2 * * *）
  --cron-health-notify N       cron 自监控任务开关（0=关闭, 1=开启，默认 0）
  --cron-health-cron EXPR      cron 自监控 cron 表达式（默认 40 2 * * *）
  --mapping-audit-notify N     门店映射审计任务开关（0=关闭, 1=开启，默认 0）
  --mapping-audit-cron EXPR    门店映射审计 cron 表达式（默认 41 2 * * *）
  --mapping-audit-strict-missing 0|1  审计任务是否把缺失映射视为阻断（默认 0）
  --mapping-smoke-notify N     门店映射链路 smoke 任务开关（0=关闭, 1=开启，默认 0）
  --mapping-smoke-cron EXPR    门店映射链路 smoke cron 表达式（默认 43 2 * * *）
  --cutover-gate-notify N      切换上线拦截规则任务开关（0=关闭, 1=开启，默认 0）
  --cutover-gate-cron EXPR     切换上线拦截规则 cron 表达式（默认 42 2 * * *）
  --cutover-gate-require-apply-ready 0|1         上线拦截规则是否要求 apply-ready（默认 0）
  --cutover-gate-require-booking-repair-pass 0|1 上线拦截规则是否要求 booking_verify_repair=PASS（默认 0）
  --cutover-gate-require-mapping-smoke-green 0|1 上线拦截规则是否要求 mapping_smoke=GREEN（默认 0）
  --cutover-gate-mapping-strict-missing 0|1      上线拦截规则映射审计 missing 是否阻断（默认 0）
  --cutover-gate-require-mapping-green 0|1       上线拦截规则是否要求 mapping_audit=GREEN（默认 0）
  --cutover-gate-require-mock-green 0|1          上线拦截规则是否要求 mock_replay=GREEN（默认 0）
  --cutover-gate-require-hq-refund-policy 0|1    上线拦截规则是否要求总部退款策略达标（默认 1）
  --cutover-gate-owner-map-file FILE             上线拦截规则 mock 回放 owner 规则（默认沿用 ticket owner map）
  --db-host HOST     MySQL 地址（默认 127.0.0.1）
  --db-port PORT     MySQL 端口（默认 3306）
  --db-name NAME     MySQL 数据库名（默认 crmeb_java）
  --db-user USER     MySQL 用户（默认 root）
  --db-pass PASS     MySQL 密码（默认空）
  --mysql-defaults-file FILE   MySQL 凭据文件（推荐，优先于 --db-pass）
  --window N         巡检窗口分钟数（默认 15）
  --tail N           每次巡检读取日志尾行数（默认 3000）

环境变量：
  MONITOR_CRON_EXPR  默认 */5 * * * *
  RECON_CRON_EXPR    默认 15 1 * * *
  TICKET_CRON_EXPR   默认 18 1 * * *
  DECISION_TICKET_CRON_EXPR 默认 21 1 * * *
  DRILL_CRON_EXPR    默认 30 1 * * *
  REPORT_CRON_EXPR   默认 45 1 * * *
  WARROOM_CRON_EXPR  默认 55 1 * * *
  GONOGO_CRON_EXPR   默认 5 2 * * *
  GONOGO_REQUIRE_MAPPING_SMOKE_GREEN 默认 0
  MORNING_BUNDLE_CRON_EXPR 默认 20 2 * * *
  MORNING_BUNDLE_REQUIRE_MAPPING_SMOKE_GREEN 默认 0
  STATUS_CRON_EXPR 默认 28 2 * * *
  STATUS_REQUIRE_BOOKING_REPAIR_PASS 默认 0
  STATUS_REQUIRE_DECISION_CHAIN_PASS 默认 0
  CONTRACT_CRON_EXPR 默认 32 2 * * *
  SLA_CRON_EXPR 默认 34 2 * * *
  RETENTION_CRON_EXPR 默认 35 2 * * *
  BOOKING_REPAIR_CRON_EXPR 默认 38 2 * * *
  BOOKING_REPAIR_APPLY 默认 0
  DECISION_CHAIN_CRON_EXPR 默认 26 2 * * *
  CRON_HEALTH_CRON_EXPR 默认 40 2 * * *
  MAPPING_AUDIT_CRON_EXPR 默认 41 2 * * *
  MAPPING_SMOKE_CRON_EXPR 默认 43 2 * * *
  CUTOVER_GATE_CRON_EXPR 默认 42 2 * * *
  CUTOVER_GATE_REQUIRE_HQ_REFUND_POLICY 默认 1
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apply)
      APPLY=1
      shift
      ;;
    --remove)
      REMOVE=1
      shift
      ;;
    --webhook)
      WEBHOOK_URL="$2"
      shift 2
      ;;
    --type)
      WEBHOOK_TYPE="$2"
      shift 2
      ;;
    --report-notify)
      REPORT_NOTIFY="$2"
      shift 2
      ;;
    --report-cron)
      REPORT_CRON_EXPR="$2"
      shift 2
      ;;
    --ticket-notify)
      TICKET_NOTIFY="$2"
      shift 2
      ;;
    --ticket-cron)
      TICKET_CRON_EXPR="$2"
      shift 2
      ;;
    --ticket-max-rows)
      TICKET_MAX_ROWS="$2"
      shift 2
      ;;
    --ticket-amount-p1-threshold-cent)
      TICKET_AMOUNT_P1_THRESHOLD_CENT="$2"
      shift 2
      ;;
    --ticket-owner-map-file)
      TICKET_OWNER_MAP_FILE="$2"
      shift 2
      ;;
    --ticket-owner-default)
      TICKET_OWNER_DEFAULT="$2"
      shift 2
      ;;
    --ticket-owner-p1)
      TICKET_OWNER_P1="$2"
      shift 2
      ;;
    --decision-ticket-notify)
      DECISION_TICKET_NOTIFY="$2"
      shift 2
      ;;
    --decision-ticket-cron)
      DECISION_TICKET_CRON_EXPR="$2"
      shift 2
      ;;
    --warroom-notify)
      WARROOM_NOTIFY="$2"
      shift 2
      ;;
    --warroom-cron)
      WARROOM_CRON_EXPR="$2"
      shift 2
      ;;
    --warroom-window-hours)
      WARROOM_WINDOW_HOURS="$2"
      shift 2
      ;;
    --go-nogo-notify)
      GONOGO_NOTIFY="$2"
      shift 2
      ;;
    --go-nogo-cron)
      GONOGO_CRON_EXPR="$2"
      shift 2
      ;;
    --go-nogo-require-apply-ready)
      GONOGO_REQUIRE_APPLY_READY="$2"
      shift 2
      ;;
    --go-nogo-require-booking-repair-pass)
      GONOGO_REQUIRE_BOOKING_REPAIR_PASS="$2"
      shift 2
      ;;
    --go-nogo-require-mapping-smoke-green)
      GONOGO_REQUIRE_MAPPING_SMOKE_GREEN="$2"
      shift 2
      ;;
    --morning-bundle-notify)
      MORNING_BUNDLE_NOTIFY="$2"
      shift 2
      ;;
    --morning-bundle-cron)
      MORNING_BUNDLE_CRON_EXPR="$2"
      shift 2
      ;;
    --morning-bundle-require-apply-ready)
      MORNING_BUNDLE_REQUIRE_APPLY_READY="$2"
      shift 2
      ;;
    --morning-bundle-require-booking-repair-pass)
      MORNING_BUNDLE_REQUIRE_BOOKING_REPAIR_PASS="$2"
      shift 2
      ;;
    --morning-bundle-require-mapping-smoke-green)
      MORNING_BUNDLE_REQUIRE_MAPPING_SMOKE_GREEN="$2"
      shift 2
      ;;
    --morning-bundle-window-hours)
      MORNING_BUNDLE_WINDOW_HOURS="$2"
      shift 2
      ;;
    --morning-bundle-window)
      MORNING_BUNDLE_WINDOW_MINUTES="$2"
      shift 2
      ;;
    --morning-bundle-tail)
      MORNING_BUNDLE_TAIL_LINES="$2"
      shift 2
      ;;
    --morning-bundle-strict-preflight)
      MORNING_BUNDLE_STRICT_PREFLIGHT="$2"
      shift 2
      ;;
    --status-notify)
      STATUS_NOTIFY="$2"
      shift 2
      ;;
    --status-cron)
      STATUS_CRON_EXPR="$2"
      shift 2
      ;;
    --status-refresh)
      STATUS_REFRESH="$2"
      shift 2
      ;;
    --status-refresh-require-apply-ready)
      STATUS_REFRESH_REQUIRE_APPLY_READY="$2"
      shift 2
      ;;
    --status-require-booking-repair-pass)
      STATUS_REQUIRE_BOOKING_REPAIR_PASS="$2"
      shift 2
      ;;
    --status-require-decision-chain-pass)
      STATUS_REQUIRE_DECISION_CHAIN_PASS="$2"
      shift 2
      ;;
    --status-max-summary-age-minutes)
      STATUS_MAX_SUMMARY_AGE_MINUTES="$2"
      shift 2
      ;;
    --status-max-recon-age-days)
      STATUS_MAX_RECON_AGE_DAYS="$2"
      shift 2
      ;;
    --status-max-daily-report-age-days)
      STATUS_MAX_DAILY_REPORT_AGE_DAYS="$2"
      shift 2
      ;;
    --contract-notify)
      CONTRACT_NOTIFY="$2"
      shift 2
      ;;
    --contract-cron)
      CONTRACT_CRON_EXPR="$2"
      shift 2
      ;;
    --contract-require-all)
      CONTRACT_REQUIRE_ALL="$2"
      shift 2
      ;;
    --sla-notify)
      SLA_NOTIFY="$2"
      shift 2
      ;;
    --sla-cron)
      SLA_CRON_EXPR="$2"
      shift 2
      ;;
    --sla-lookback-days)
      SLA_LOOKBACK_DAYS="$2"
      shift 2
      ;;
    --sla-days)
      SLA_DAYS="$2"
      shift 2
      ;;
    --retention-notify)
      RETENTION_NOTIFY="$2"
      shift 2
      ;;
    --retention-cron)
      RETENTION_CRON_EXPR="$2"
      shift 2
      ;;
    --retention-keep-days)
      RETENTION_KEEP_DAYS="$2"
      shift 2
      ;;
    --booking-repair-notify)
      BOOKING_REPAIR_NOTIFY="$2"
      shift 2
      ;;
    --booking-repair-cron)
      BOOKING_REPAIR_CRON_EXPR="$2"
      shift 2
      ;;
    --booking-repair-window-hours)
      BOOKING_REPAIR_WINDOW_HOURS="$2"
      shift 2
      ;;
    --booking-repair-apply)
      BOOKING_REPAIR_APPLY="$2"
      shift 2
      ;;
    --decision-chain-notify)
      DECISION_CHAIN_NOTIFY="$2"
      shift 2
      ;;
    --decision-chain-cron)
      DECISION_CHAIN_CRON_EXPR="$2"
      shift 2
      ;;
    --cron-health-notify)
      CRON_HEALTH_NOTIFY="$2"
      shift 2
      ;;
    --cron-health-cron)
      CRON_HEALTH_CRON_EXPR="$2"
      shift 2
      ;;
    --mapping-audit-notify)
      MAPPING_AUDIT_NOTIFY="$2"
      shift 2
      ;;
    --mapping-audit-cron)
      MAPPING_AUDIT_CRON_EXPR="$2"
      shift 2
      ;;
    --mapping-audit-strict-missing)
      MAPPING_AUDIT_STRICT_MISSING="$2"
      shift 2
      ;;
    --mapping-smoke-notify)
      MAPPING_SMOKE_NOTIFY="$2"
      shift 2
      ;;
    --mapping-smoke-cron)
      MAPPING_SMOKE_CRON_EXPR="$2"
      shift 2
      ;;
    --cutover-gate-notify)
      CUTOVER_GATE_NOTIFY="$2"
      shift 2
      ;;
    --cutover-gate-cron)
      CUTOVER_GATE_CRON_EXPR="$2"
      shift 2
      ;;
    --cutover-gate-require-apply-ready)
      CUTOVER_GATE_REQUIRE_APPLY_READY="$2"
      shift 2
      ;;
    --cutover-gate-require-booking-repair-pass)
      CUTOVER_GATE_REQUIRE_BOOKING_REPAIR_PASS="$2"
      shift 2
      ;;
    --cutover-gate-require-mapping-smoke-green)
      CUTOVER_GATE_REQUIRE_MAPPING_SMOKE_GREEN="$2"
      shift 2
      ;;
    --cutover-gate-mapping-strict-missing)
      CUTOVER_GATE_MAPPING_STRICT_MISSING="$2"
      shift 2
      ;;
    --cutover-gate-require-mapping-green)
      CUTOVER_GATE_REQUIRE_MAPPING_GREEN="$2"
      shift 2
      ;;
    --cutover-gate-require-mock-green)
      CUTOVER_GATE_REQUIRE_MOCK_GREEN="$2"
      shift 2
      ;;
    --cutover-gate-require-hq-refund-policy)
      CUTOVER_GATE_REQUIRE_HQ_REFUND_POLICY="$2"
      shift 2
      ;;
    --cutover-gate-owner-map-file)
      CUTOVER_GATE_OWNER_MAP_FILE="$2"
      shift 2
      ;;
    --db-host)
      DB_HOST="$2"
      shift 2
      ;;
    --db-port)
      DB_PORT="$2"
      shift 2
      ;;
    --db-name)
      DB_NAME="$2"
      shift 2
      ;;
    --db-user)
      DB_USER="$2"
      shift 2
      ;;
    --db-pass)
      DB_PASS="$2"
      shift 2
      ;;
    --mysql-defaults-file)
      MYSQL_DEFAULTS_FILE="$2"
      shift 2
      ;;
    --window)
      WINDOW_MINUTES="$2"
      shift 2
      ;;
    --tail)
      TAIL_LINES="$2"
      shift 2
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

if [[ ${REMOVE} -eq 1 && ${APPLY} -eq 0 ]]; then
  echo "当前为 dry-run remove，仅展示将删除的托管块。"
fi

if ! [[ "${WINDOW_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --window 必须是正整数"
  exit 1
fi
if ! [[ "${TAIL_LINES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --tail 必须是正整数"
  exit 1
fi
if ! [[ "${DB_PORT}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --db-port 必须是正整数"
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "参数错误: --mysql-defaults-file 文件不存在 -> ${MYSQL_DEFAULTS_FILE}"
  exit 1
fi

WEBHOOK_TYPE="$(printf '%s' "${WEBHOOK_TYPE}" | tr '[:upper:]' '[:lower:]')"
if [[ "${WEBHOOK_TYPE}" != "wecom" && "${WEBHOOK_TYPE}" != "dingtalk" && "${WEBHOOK_TYPE}" != "feishu" ]]; then
  echo "参数错误: --type 仅支持 wecom/dingtalk/feishu"
  exit 1
fi
if [[ "${REPORT_NOTIFY}" != "0" && "${REPORT_NOTIFY}" != "1" ]]; then
  echo "参数错误: --report-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${TICKET_NOTIFY}" != "0" && "${TICKET_NOTIFY}" != "1" ]]; then
  echo "参数错误: --ticket-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${DECISION_TICKET_NOTIFY}" != "0" && "${DECISION_TICKET_NOTIFY}" != "1" ]]; then
  echo "参数错误: --decision-ticket-notify 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${TICKET_MAX_ROWS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --ticket-max-rows 必须是正整数"
  exit 1
fi
if ! [[ "${TICKET_AMOUNT_P1_THRESHOLD_CENT}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --ticket-amount-p1-threshold-cent 必须是正整数"
  exit 1
fi
if [[ -z "${TICKET_OWNER_DEFAULT}" ]]; then
  echo "参数错误: --ticket-owner-default 不能为空"
  exit 1
fi
if [[ -z "${TICKET_OWNER_P1}" ]]; then
  echo "参数错误: --ticket-owner-p1 不能为空"
  exit 1
fi
if [[ "${TICKET_OWNER_DEFAULT}" =~ [[:space:]\'\"] ]]; then
  echo "参数错误: --ticket-owner-default 不支持空白或引号字符"
  exit 1
fi
if [[ "${TICKET_OWNER_P1}" =~ [[:space:]\'\"] ]]; then
  echo "参数错误: --ticket-owner-p1 不支持空白或引号字符"
  exit 1
fi
if [[ -n "${TICKET_OWNER_MAP_FILE}" && ! -f "${TICKET_OWNER_MAP_FILE}" ]]; then
  echo "参数错误: --ticket-owner-map-file 文件不存在 -> ${TICKET_OWNER_MAP_FILE}"
  exit 1
fi
if [[ -n "${TICKET_OWNER_MAP_FILE}" && "${TICKET_OWNER_MAP_FILE}" =~ [[:space:]\'\"] ]]; then
  echo "参数错误: --ticket-owner-map-file 不支持空白或引号字符"
  exit 1
fi
if [[ "${WARROOM_NOTIFY}" != "0" && "${WARROOM_NOTIFY}" != "1" ]]; then
  echo "参数错误: --warroom-notify 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${WARROOM_WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --warroom-window-hours 必须是正整数"
  exit 1
fi
if [[ "${GONOGO_NOTIFY}" != "0" && "${GONOGO_NOTIFY}" != "1" ]]; then
  echo "参数错误: --go-nogo-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${GONOGO_REQUIRE_APPLY_READY}" != "0" && "${GONOGO_REQUIRE_APPLY_READY}" != "1" ]]; then
  echo "参数错误: --go-nogo-require-apply-ready 仅支持 0 或 1"
  exit 1
fi
if [[ "${GONOGO_REQUIRE_BOOKING_REPAIR_PASS}" != "0" && "${GONOGO_REQUIRE_BOOKING_REPAIR_PASS}" != "1" ]]; then
  echo "参数错误: --go-nogo-require-booking-repair-pass 仅支持 0 或 1"
  exit 1
fi
if [[ "${GONOGO_REQUIRE_MAPPING_SMOKE_GREEN}" != "0" && "${GONOGO_REQUIRE_MAPPING_SMOKE_GREEN}" != "1" ]]; then
  echo "参数错误: --go-nogo-require-mapping-smoke-green 仅支持 0 或 1"
  exit 1
fi
if [[ "${MORNING_BUNDLE_NOTIFY}" != "0" && "${MORNING_BUNDLE_NOTIFY}" != "1" ]]; then
  echo "参数错误: --morning-bundle-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${MORNING_BUNDLE_REQUIRE_APPLY_READY}" != "0" && "${MORNING_BUNDLE_REQUIRE_APPLY_READY}" != "1" ]]; then
  echo "参数错误: --morning-bundle-require-apply-ready 仅支持 0 或 1"
  exit 1
fi
if [[ "${MORNING_BUNDLE_REQUIRE_BOOKING_REPAIR_PASS}" != "0" && "${MORNING_BUNDLE_REQUIRE_BOOKING_REPAIR_PASS}" != "1" ]]; then
  echo "参数错误: --morning-bundle-require-booking-repair-pass 仅支持 0 或 1"
  exit 1
fi
if [[ "${MORNING_BUNDLE_REQUIRE_MAPPING_SMOKE_GREEN}" != "0" && "${MORNING_BUNDLE_REQUIRE_MAPPING_SMOKE_GREEN}" != "1" ]]; then
  echo "参数错误: --morning-bundle-require-mapping-smoke-green 仅支持 0 或 1"
  exit 1
fi
if [[ "${MORNING_BUNDLE_STRICT_PREFLIGHT}" != "0" && "${MORNING_BUNDLE_STRICT_PREFLIGHT}" != "1" ]]; then
  echo "参数错误: --morning-bundle-strict-preflight 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${MORNING_BUNDLE_WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --morning-bundle-window-hours 必须是正整数"
  exit 1
fi
if ! [[ "${MORNING_BUNDLE_WINDOW_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --morning-bundle-window 必须是正整数"
  exit 1
fi
if ! [[ "${MORNING_BUNDLE_TAIL_LINES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --morning-bundle-tail 必须是正整数"
  exit 1
fi
if [[ "${STATUS_NOTIFY}" != "0" && "${STATUS_NOTIFY}" != "1" ]]; then
  echo "参数错误: --status-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${STATUS_REFRESH}" != "0" && "${STATUS_REFRESH}" != "1" ]]; then
  echo "参数错误: --status-refresh 仅支持 0 或 1"
  exit 1
fi
if [[ "${STATUS_REFRESH_REQUIRE_APPLY_READY}" != "0" && "${STATUS_REFRESH_REQUIRE_APPLY_READY}" != "1" ]]; then
  echo "参数错误: --status-refresh-require-apply-ready 仅支持 0 或 1"
  exit 1
fi
if [[ "${STATUS_REQUIRE_BOOKING_REPAIR_PASS}" != "0" && "${STATUS_REQUIRE_BOOKING_REPAIR_PASS}" != "1" ]]; then
  echo "参数错误: --status-require-booking-repair-pass 仅支持 0 或 1"
  exit 1
fi
if [[ "${STATUS_REQUIRE_DECISION_CHAIN_PASS}" != "0" && "${STATUS_REQUIRE_DECISION_CHAIN_PASS}" != "1" ]]; then
  echo "参数错误: --status-require-decision-chain-pass 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${STATUS_MAX_SUMMARY_AGE_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --status-max-summary-age-minutes 必须是正整数"
  exit 1
fi
if ! [[ "${STATUS_MAX_RECON_AGE_DAYS}" =~ ^[0-9]+$ ]]; then
  echo "参数错误: --status-max-recon-age-days 必须是非负整数"
  exit 1
fi
if ! [[ "${STATUS_MAX_DAILY_REPORT_AGE_DAYS}" =~ ^[0-9]+$ ]]; then
  echo "参数错误: --status-max-daily-report-age-days 必须是非负整数"
  exit 1
fi
if [[ "${CONTRACT_NOTIFY}" != "0" && "${CONTRACT_NOTIFY}" != "1" ]]; then
  echo "参数错误: --contract-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${CONTRACT_REQUIRE_ALL}" != "0" && "${CONTRACT_REQUIRE_ALL}" != "1" ]]; then
  echo "参数错误: --contract-require-all 仅支持 0 或 1"
  exit 1
fi
if [[ "${SLA_NOTIFY}" != "0" && "${SLA_NOTIFY}" != "1" ]]; then
  echo "参数错误: --sla-notify 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${SLA_LOOKBACK_DAYS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --sla-lookback-days 必须是正整数"
  exit 1
fi
if ! [[ "${SLA_DAYS}" =~ ^[0-9]+$ ]]; then
  echo "参数错误: --sla-days 必须是非负整数"
  exit 1
fi
if [[ "${RETENTION_NOTIFY}" != "0" && "${RETENTION_NOTIFY}" != "1" ]]; then
  echo "参数错误: --retention-notify 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${RETENTION_KEEP_DAYS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --retention-keep-days 必须是正整数"
  exit 1
fi
if [[ "${BOOKING_REPAIR_NOTIFY}" != "0" && "${BOOKING_REPAIR_NOTIFY}" != "1" ]]; then
  echo "参数错误: --booking-repair-notify 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${BOOKING_REPAIR_WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --booking-repair-window-hours 必须是正整数"
  exit 1
fi
if [[ "${BOOKING_REPAIR_APPLY}" != "0" && "${BOOKING_REPAIR_APPLY}" != "1" ]]; then
  echo "参数错误: --booking-repair-apply 仅支持 0 或 1"
  exit 1
fi
if [[ "${DECISION_CHAIN_NOTIFY}" != "0" && "${DECISION_CHAIN_NOTIFY}" != "1" ]]; then
  echo "参数错误: --decision-chain-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${CRON_HEALTH_NOTIFY}" != "0" && "${CRON_HEALTH_NOTIFY}" != "1" ]]; then
  echo "参数错误: --cron-health-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${MAPPING_AUDIT_NOTIFY}" != "0" && "${MAPPING_AUDIT_NOTIFY}" != "1" ]]; then
  echo "参数错误: --mapping-audit-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${MAPPING_AUDIT_STRICT_MISSING}" != "0" && "${MAPPING_AUDIT_STRICT_MISSING}" != "1" ]]; then
  echo "参数错误: --mapping-audit-strict-missing 仅支持 0 或 1"
  exit 1
fi
if [[ "${MAPPING_SMOKE_NOTIFY}" != "0" && "${MAPPING_SMOKE_NOTIFY}" != "1" ]]; then
  echo "参数错误: --mapping-smoke-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${CUTOVER_GATE_NOTIFY}" != "0" && "${CUTOVER_GATE_NOTIFY}" != "1" ]]; then
  echo "参数错误: --cutover-gate-notify 仅支持 0 或 1"
  exit 1
fi
if [[ "${CUTOVER_GATE_REQUIRE_APPLY_READY}" != "0" && "${CUTOVER_GATE_REQUIRE_APPLY_READY}" != "1" ]]; then
  echo "参数错误: --cutover-gate-require-apply-ready 仅支持 0 或 1"
  exit 1
fi
if [[ "${CUTOVER_GATE_REQUIRE_BOOKING_REPAIR_PASS}" != "0" && "${CUTOVER_GATE_REQUIRE_BOOKING_REPAIR_PASS}" != "1" ]]; then
  echo "参数错误: --cutover-gate-require-booking-repair-pass 仅支持 0 或 1"
  exit 1
fi
if [[ "${CUTOVER_GATE_REQUIRE_MAPPING_SMOKE_GREEN}" != "0" && "${CUTOVER_GATE_REQUIRE_MAPPING_SMOKE_GREEN}" != "1" ]]; then
  echo "参数错误: --cutover-gate-require-mapping-smoke-green 仅支持 0 或 1"
  exit 1
fi
if [[ "${CUTOVER_GATE_MAPPING_STRICT_MISSING}" != "0" && "${CUTOVER_GATE_MAPPING_STRICT_MISSING}" != "1" ]]; then
  echo "参数错误: --cutover-gate-mapping-strict-missing 仅支持 0 或 1"
  exit 1
fi
if [[ "${CUTOVER_GATE_REQUIRE_MAPPING_GREEN}" != "0" && "${CUTOVER_GATE_REQUIRE_MAPPING_GREEN}" != "1" ]]; then
  echo "参数错误: --cutover-gate-require-mapping-green 仅支持 0 或 1"
  exit 1
fi
if [[ "${CUTOVER_GATE_REQUIRE_MOCK_GREEN}" != "0" && "${CUTOVER_GATE_REQUIRE_MOCK_GREEN}" != "1" ]]; then
  echo "参数错误: --cutover-gate-require-mock-green 仅支持 0 或 1"
  exit 1
fi
if [[ "${CUTOVER_GATE_REQUIRE_HQ_REFUND_POLICY}" != "0" && "${CUTOVER_GATE_REQUIRE_HQ_REFUND_POLICY}" != "1" ]]; then
  echo "参数错误: --cutover-gate-require-hq-refund-policy 仅支持 0 或 1"
  exit 1
fi
if [[ -n "${CUTOVER_GATE_OWNER_MAP_FILE}" && ! -f "${CUTOVER_GATE_OWNER_MAP_FILE}" ]]; then
  echo "参数错误: --cutover-gate-owner-map-file 文件不存在 -> ${CUTOVER_GATE_OWNER_MAP_FILE}"
  exit 1
fi
if [[ -n "${CUTOVER_GATE_OWNER_MAP_FILE}" && "${CUTOVER_GATE_OWNER_MAP_FILE}" =~ [[:space:]\'\"] ]]; then
  echo "参数错误: --cutover-gate-owner-map-file 不支持空白或引号字符"
  exit 1
fi
if ! command -v flock >/dev/null 2>&1; then
  echo "环境缺少 flock 命令，无法安装防重入 cron。"
  exit 1
fi

shell_sq() {
  printf '%s' "$1" | sed "s/'/'\"'\"'/g"
}

WEBHOOK_URL_ESC="$(shell_sq "${WEBHOOK_URL}")"
WEBHOOK_TYPE_ESC="$(shell_sq "${WEBHOOK_TYPE}")"
DB_HOST_ESC="$(shell_sq "${DB_HOST}")"
DB_NAME_ESC="$(shell_sq "${DB_NAME}")"
DB_USER_ESC="$(shell_sq "${DB_USER}")"
DB_PASS_ESC="$(shell_sq "${DB_PASS}")"
MYSQL_DEFAULTS_FILE_ESC="$(shell_sq "${MYSQL_DEFAULTS_FILE}")"

current_cron="$(crontab -l 2>/dev/null || true)"
clean_cron="$(printf '%s\n' "${current_cron}" | awk -v b="${BEGIN_MARK}" -v e="${END_MARK}" '
  $0==b {skip=1; next}
  $0==e {skip=0; next}
  skip==0 {print}
')"

if [[ ${REMOVE} -eq 1 ]]; then
  if [[ ${APPLY} -eq 1 ]]; then
    printf '%s\n' "${clean_cron}" | crontab -
    echo "已删除 payment 托管定时任务。"
  else
    echo "===== dry-run: 清理后 crontab ====="
    printf '%s\n' "${clean_cron}"
  fi
  exit 0
fi

ENV_PREFIX="ALERT_WEBHOOK_URL='${WEBHOOK_URL_ESC}' ALERT_WEBHOOK_TYPE='${WEBHOOK_TYPE_ESC}' DB_HOST='${DB_HOST_ESC}' DB_PORT='${DB_PORT}' DB_NAME='${DB_NAME_ESC}' DB_USER='${DB_USER_ESC}' MYSQL_DEFAULTS_FILE='${MYSQL_DEFAULTS_FILE_ESC}'"
if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
  ENV_PREFIX="${ENV_PREFIX} DB_PASS='${DB_PASS_ESC}'"
fi

monitor_line="${MONITOR_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_monitor.lock ./shell/payment_monitor_alert.sh --window ${WINDOW_MINUTES} --tail ${TAIL_LINES} || echo \"[lock] monitor skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_monitor_cron.log 2>&1"
recon_line="${RECON_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_reconcile.lock ./shell/payment_reconcile_daily.sh || echo \"[lock] reconcile skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_reconcile_cron.log 2>&1"
ticket_line=""
if [[ "${TICKET_NOTIFY}" == "1" ]]; then
  ticket_cmd="./shell/payment_reconcile_ticketize.sh --max-rows ${TICKET_MAX_ROWS} --amount-p1-threshold-cent ${TICKET_AMOUNT_P1_THRESHOLD_CENT} --owner-default ${TICKET_OWNER_DEFAULT} --owner-p1 ${TICKET_OWNER_P1}"
  if [[ -n "${TICKET_OWNER_MAP_FILE}" ]]; then
    ticket_cmd="${ticket_cmd} --owner-map-file ${TICKET_OWNER_MAP_FILE}"
  fi
  ticket_line="${TICKET_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_ticketize.lock bash -lc '${ticket_cmd} || true' || echo \"[lock] ticketize skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_ticketize_cron.log 2>&1"
fi
decision_ticket_line=""
if [[ "${DECISION_TICKET_NOTIFY}" == "1" ]]; then
  decision_ticket_cmd="./shell/payment_decision_ticketize.sh --owner-default ${TICKET_OWNER_DEFAULT} --owner-p1 ${TICKET_OWNER_P1}"
  if [[ -n "${TICKET_OWNER_MAP_FILE}" ]]; then
    decision_ticket_cmd="${decision_ticket_cmd} --owner-map-file ${TICKET_OWNER_MAP_FILE}"
  fi
  decision_ticket_line="${DECISION_TICKET_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_decision_ticketize.lock bash -lc '${decision_ticket_cmd} || true' || echo \"[lock] decision-ticketize skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_decision_ticketize_cron.log 2>&1"
fi
drill_line="${DRILL_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_drill.lock ./shell/payment_fullchain_drill.sh || echo \"[lock] drill skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_drill_cron.log 2>&1"
if [[ "${REPORT_NOTIFY}" == "1" ]]; then
  report_line="${REPORT_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_daily_report.lock ./shell/payment_daily_report_notify.sh || echo \"[lock] daily-report skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_daily_report_cron.log 2>&1"
else
  report_line="${REPORT_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_daily_report.lock ./shell/payment_daily_report.sh || echo \"[lock] daily-report skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_daily_report_cron.log 2>&1"
fi

warroom_line=""
if [[ "${WARROOM_NOTIFY}" == "1" ]]; then
  warroom_line="${WARROOM_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_warroom.lock ./shell/payment_warroom_dashboard.sh --window-hours ${WARROOM_WINDOW_HOURS} || echo \"[lock] warroom skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_warroom_cron.log 2>&1"
fi

gonogo_line=""
if [[ "${GONOGO_NOTIFY}" == "1" ]]; then
  gonogo_line="${GONOGO_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_go_nogo.lock ./shell/payment_go_nogo_decision.sh --require-apply-ready ${GONOGO_REQUIRE_APPLY_READY} --require-booking-repair-pass ${GONOGO_REQUIRE_BOOKING_REPAIR_PASS} --require-mapping-smoke-green ${GONOGO_REQUIRE_MAPPING_SMOKE_GREEN} || echo \"[lock] go-nogo skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_go_nogo_cron.log 2>&1"
fi

morning_bundle_line=""
if [[ "${MORNING_BUNDLE_NOTIFY}" == "1" ]]; then
  morning_bundle_cmd="./shell/payment_ops_morning_bundle.sh --window ${MORNING_BUNDLE_WINDOW_MINUTES} --tail ${MORNING_BUNDLE_TAIL_LINES} --window-hours ${MORNING_BUNDLE_WINDOW_HOURS} --require-apply-ready ${MORNING_BUNDLE_REQUIRE_APPLY_READY} --require-booking-repair-pass ${MORNING_BUNDLE_REQUIRE_BOOKING_REPAIR_PASS} --require-mapping-smoke-green ${MORNING_BUNDLE_REQUIRE_MAPPING_SMOKE_GREEN}"
  if [[ "${MORNING_BUNDLE_STRICT_PREFLIGHT}" == "1" ]]; then
    morning_bundle_cmd="${morning_bundle_cmd} --strict-preflight"
  fi
  morning_bundle_line="${MORNING_BUNDLE_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_morning_bundle.lock ${morning_bundle_cmd} || echo \"[lock] morning-bundle skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_morning_bundle_cron.log 2>&1"
fi

status_line=""
if [[ "${STATUS_NOTIFY}" == "1" ]]; then
  status_cmd="./shell/payment_ops_status.sh --require-booking-repair-pass ${STATUS_REQUIRE_BOOKING_REPAIR_PASS} --require-decision-chain-pass ${STATUS_REQUIRE_DECISION_CHAIN_PASS}"
  if [[ "${STATUS_REFRESH}" == "1" ]]; then
    status_cmd="${status_cmd} --refresh --refresh-require-apply-ready ${STATUS_REFRESH_REQUIRE_APPLY_READY}"
  fi
  status_cmd="${status_cmd} --max-summary-age-minutes ${STATUS_MAX_SUMMARY_AGE_MINUTES} --max-recon-age-days ${STATUS_MAX_RECON_AGE_DAYS} --max-daily-report-age-days ${STATUS_MAX_DAILY_REPORT_AGE_DAYS}"
  status_line="${STATUS_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_ops_status.lock ${status_cmd} || echo \"[lock] ops-status skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_ops_status_cron.log 2>&1"
fi

contract_line=""
if [[ "${CONTRACT_NOTIFY}" == "1" ]]; then
  contract_cmd="./shell/payment_summary_contract_check.sh"
  if [[ "${CONTRACT_REQUIRE_ALL}" == "1" ]]; then
    contract_cmd="${contract_cmd} --require-all"
  fi
  contract_line="${CONTRACT_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_contract_check.lock ${contract_cmd} || echo \"[lock] contract-check skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_contract_check_cron.log 2>&1"
fi

sla_line=""
if [[ "${SLA_NOTIFY}" == "1" ]]; then
  sla_cmd="./shell/payment_reconcile_sla_guard.sh --lookback-days ${SLA_LOOKBACK_DAYS} --sla-days ${SLA_DAYS}"
  sla_line="${SLA_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_reconcile_sla.lock ${sla_cmd} || echo \"[lock] reconcile-sla skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_reconcile_sla_cron.log 2>&1"
fi

retention_line=""
if [[ "${RETENTION_NOTIFY}" == "1" ]]; then
  retention_line="${RETENTION_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_retention.lock ./shell/payment_runtime_retention.sh --keep-days ${RETENTION_KEEP_DAYS} --apply || echo \"[lock] retention skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_retention_cron.log 2>&1"
fi

booking_repair_line=""
if [[ "${BOOKING_REPAIR_NOTIFY}" == "1" ]]; then
  booking_repair_cmd="./shell/payment_booking_verify_repair.sh --window-hours ${BOOKING_REPAIR_WINDOW_HOURS}"
  if [[ "${BOOKING_REPAIR_APPLY}" == "1" ]]; then
    booking_repair_cmd="${booking_repair_cmd} --apply"
  fi
  booking_repair_line="${BOOKING_REPAIR_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_booking_repair.lock bash -lc '${booking_repair_cmd} || true' || echo \"[lock] booking-repair skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_booking_repair_cron.log 2>&1"
fi

decision_chain_line=""
if [[ "${DECISION_CHAIN_NOTIFY}" == "1" ]]; then
  decision_chain_cmd="./shell/payment_decision_chain_smoke.sh"
  decision_chain_line="${DECISION_CHAIN_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_decision_chain_smoke.lock ${decision_chain_cmd} || echo \"[lock] decision-chain-smoke skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_decision_chain_smoke_cron.log 2>&1"
fi

cron_health_line=""
if [[ "${CRON_HEALTH_NOTIFY}" == "1" ]]; then
  cron_health_cmd="./shell/payment_cron_healthcheck.sh"
  cron_health_line="${CRON_HEALTH_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_cron_healthcheck.lock ${cron_health_cmd} || echo \"[lock] cron-healthcheck skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_cron_healthcheck_cron.log 2>&1"
fi

mapping_audit_line=""
if [[ "${MAPPING_AUDIT_NOTIFY}" == "1" ]]; then
  mapping_audit_cmd="./shell/payment_store_mapping_audit.sh --strict-missing ${MAPPING_AUDIT_STRICT_MISSING}"
  mapping_audit_line="${MAPPING_AUDIT_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_store_mapping_audit.lock bash -lc '${mapping_audit_cmd} || true' || echo \"[lock] mapping-audit skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_store_mapping_audit_cron.log 2>&1"
fi

mapping_smoke_line=""
if [[ "${MAPPING_SMOKE_NOTIFY}" == "1" ]]; then
  mapping_smoke_cmd="./shell/payment_store_mapping_pipeline_smoke.sh"
  mapping_smoke_line="${MAPPING_SMOKE_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_store_mapping_smoke.lock bash -lc '${mapping_smoke_cmd} || true' || echo \"[lock] mapping-smoke skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_store_mapping_smoke_cron.log 2>&1"
fi

cutover_gate_line=""
if [[ "${CUTOVER_GATE_NOTIFY}" == "1" ]]; then
  cutover_owner_map_file="${CUTOVER_GATE_OWNER_MAP_FILE}"
  if [[ -z "${cutover_owner_map_file}" ]]; then
    cutover_owner_map_file="${TICKET_OWNER_MAP_FILE}"
  fi
  cutover_gate_cmd="./shell/payment_cutover_gate.sh --require-apply-ready ${CUTOVER_GATE_REQUIRE_APPLY_READY} --require-booking-repair-pass ${CUTOVER_GATE_REQUIRE_BOOKING_REPAIR_PASS} --require-mapping-smoke-green ${CUTOVER_GATE_REQUIRE_MAPPING_SMOKE_GREEN} --mapping-strict-missing ${CUTOVER_GATE_MAPPING_STRICT_MISSING} --require-mapping-green ${CUTOVER_GATE_REQUIRE_MAPPING_GREEN} --require-mock-green ${CUTOVER_GATE_REQUIRE_MOCK_GREEN} --require-hq-refund-policy ${CUTOVER_GATE_REQUIRE_HQ_REFUND_POLICY} --owner-default ${TICKET_OWNER_DEFAULT} --owner-p1 ${TICKET_OWNER_P1}"
  if [[ -n "${cutover_owner_map_file}" ]]; then
    cutover_gate_cmd="${cutover_gate_cmd} --owner-map-file ${cutover_owner_map_file}"
  fi
  cutover_gate_line="${CUTOVER_GATE_CRON_EXPR} cd ${ROOT_DIR} && mkdir -p ${LOCK_DIR} && { ${ENV_PREFIX} flock -n ${LOCK_DIR}/payment_cutover_gate.lock bash -lc '${cutover_gate_cmd} || true' || echo \"[lock] cutover-gate skipped: lock busy\"; } >> ${RUNTIME_DIR}/payment_cutover_gate_cron.log 2>&1"
fi

managed_lines=(
  "${BEGIN_MARK}"
  "${monitor_line}"
  "${recon_line}"
)
if [[ -n "${ticket_line}" ]]; then
  managed_lines+=("${ticket_line}")
fi
if [[ -n "${decision_ticket_line}" ]]; then
  managed_lines+=("${decision_ticket_line}")
fi
managed_lines+=("${drill_line}")
managed_lines+=("${report_line}")
if [[ -n "${warroom_line}" ]]; then
  managed_lines+=("${warroom_line}")
fi
if [[ -n "${gonogo_line}" ]]; then
  managed_lines+=("${gonogo_line}")
fi
if [[ -n "${morning_bundle_line}" ]]; then
  managed_lines+=("${morning_bundle_line}")
fi
if [[ -n "${status_line}" ]]; then
  managed_lines+=("${status_line}")
fi
if [[ -n "${contract_line}" ]]; then
  managed_lines+=("${contract_line}")
fi
if [[ -n "${sla_line}" ]]; then
  managed_lines+=("${sla_line}")
fi
if [[ -n "${retention_line}" ]]; then
  managed_lines+=("${retention_line}")
fi
if [[ -n "${booking_repair_line}" ]]; then
  managed_lines+=("${booking_repair_line}")
fi
if [[ -n "${decision_chain_line}" ]]; then
  managed_lines+=("${decision_chain_line}")
fi
if [[ -n "${cron_health_line}" ]]; then
  managed_lines+=("${cron_health_line}")
fi
if [[ -n "${mapping_audit_line}" ]]; then
  managed_lines+=("${mapping_audit_line}")
fi
if [[ -n "${mapping_smoke_line}" ]]; then
  managed_lines+=("${mapping_smoke_line}")
fi
if [[ -n "${cutover_gate_line}" ]]; then
  managed_lines+=("${cutover_gate_line}")
fi
managed_lines+=("${END_MARK}")
managed_block="$(printf '%s\n' "${managed_lines[@]}")"

if [[ -n "${clean_cron}" ]]; then
  next_cron="${clean_cron}
${managed_block}"
else
  next_cron="${managed_block}"
fi

if [[ ${APPLY} -eq 1 ]]; then
  if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
    echo "警告: 当前将以明文 DB_PASS 写入 crontab，建议改用 --mysql-defaults-file。"
  fi
  printf '%s\n' "${next_cron}" | crontab -
  echo "已写入 payment 运维定时任务。"
  crontab -l | awk -v b="${BEGIN_MARK}" -v e="${END_MARK}" '
    $0==b {print;show=1;next}
    $0==e {print;show=0;next}
    show==1 {print}
  '
else
  echo "===== dry-run: 将写入的托管任务块 ====="
  printf '%s\n' "${managed_block}"
fi
