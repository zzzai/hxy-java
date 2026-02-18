#!/usr/bin/env bash
set -euo pipefail
export GIT_TERMINAL_PROMPT=0

# hxy-java 仓库同步脚本（双仓库）
# - root:  /root/crmeb-java
# - inner: /root/crmeb-java/crmeb_java

ROOT_REPO="${ROOT_REPO:-/root/crmeb-java}"
INNER_REPO="${INNER_REPO:-/root/crmeb-java/crmeb_java}"

ROOT_REMOTE="${ROOT_REMOTE:-origin}"
INNER_REMOTE="${INNER_REMOTE:-origin}"
ROOT_BRANCH="${ROOT_BRANCH:-main}"
INNER_BRANCH="${INNER_BRANCH:-1.4}"

TARGET="all"       # root|inner|all
MODE="check"       # check|sync|push
ALLOW_DIRTY=0
ALLOW_SUBMODULE_POINTER=0
DRY_RUN=0
CMD_TIMEOUT_SEC="${CMD_TIMEOUT_SEC:-30}"

usage() {
  cat <<'USAGE'
用法：
  ./hxy/tools/scripts/repo_sync.sh
    [--target root|inner|all]
    [--mode check|sync|push]
    [--root-remote NAME] [--root-branch NAME]
    [--inner-remote NAME] [--inner-branch NAME]
    [--allow-dirty]
    [--allow-submodule-pointer]
    [--cmd-timeout-sec N]
    [--dry-run]

说明：
  1) check：只检查（分支/脏变更/远端差异），不拉取不推送
  2) sync：先检查，再 pull --rebase
  3) push：先 sync，再 push

默认值：
  target=all, mode=check
  root=(origin/main), inner=(origin/1.4)
USAGE
}

log() {
  printf '[repo-sync] %s\n' "$1"
}

die() {
  printf '[repo-sync][ERROR] %s\n' "$1" >&2
  exit 1
}

run() {
  if [[ "${DRY_RUN}" == "1" ]]; then
    printf '[repo-sync][DRY-RUN] %s\n' "$*"
    return 0
  fi
  local rc=0
  set +e
  timeout "${CMD_TIMEOUT_SEC}" "$@"
  rc=$?
  set -e
  if [[ "${rc}" == "0" ]]; then
    return 0
  fi
  if [[ "${rc}" == "124" ]]; then
    die "命令超时(${CMD_TIMEOUT_SEC}s): $*"
  fi
  die "命令失败(rc=${rc}): $*"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --target)
      TARGET="$2"
      shift 2
      ;;
    --mode)
      MODE="$2"
      shift 2
      ;;
    --root-remote)
      ROOT_REMOTE="$2"
      shift 2
      ;;
    --root-branch)
      ROOT_BRANCH="$2"
      shift 2
      ;;
    --inner-remote)
      INNER_REMOTE="$2"
      shift 2
      ;;
    --inner-branch)
      INNER_BRANCH="$2"
      shift 2
      ;;
    --allow-dirty)
      ALLOW_DIRTY=1
      shift
      ;;
    --allow-submodule-pointer)
      ALLOW_SUBMODULE_POINTER=1
      shift
      ;;
    --cmd-timeout-sec)
      CMD_TIMEOUT_SEC="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "未知参数: $1"
      ;;
  esac
done

case "${TARGET}" in
  root|inner|all) ;;
  *) die "--target 仅支持 root|inner|all" ;;
esac

case "${MODE}" in
  check|sync|push) ;;
  *) die "--mode 仅支持 check|sync|push" ;;
esac

if ! [[ "${CMD_TIMEOUT_SEC}" =~ ^[1-9][0-9]*$ ]]; then
  die "--cmd-timeout-sec 必须是正整数"
fi

require_repo() {
  local repo="$1"
  git -C "${repo}" rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "不是 Git 仓库: ${repo}"
}

ensure_branch() {
  local repo="$1"
  local expected="$2"
  local label="$3"
  local current
  current="$(git -C "${repo}" branch --show-current)"
  [[ -n "${current}" ]] || die "${label} 当前处于 detached HEAD"
  if [[ "${current}" != "${expected}" ]]; then
    die "${label} 分支不匹配: current=${current}, expected=${expected}"
  fi
}

ensure_remote_branch() {
  local repo="$1"
  local remote="$2"
  local branch="$3"
  local label="$4"
  if [[ "${DRY_RUN}" == "1" ]]; then
    return 0
  fi
  if ! git -C "${repo}" ls-remote --exit-code --heads "${remote}" "${branch}" >/dev/null 2>&1; then
    die "${label} 远端分支不存在: ${remote}/${branch}"
  fi
}

ensure_clean_if_required() {
  local repo="$1"
  local label="$2"
  local dirty
  dirty="$(git -C "${repo}" status --porcelain)"
  if [[ -n "${dirty}" && "${ALLOW_DIRTY}" != "1" ]]; then
    die "${label} 存在未提交变更（默认禁止同步/推送）。可先提交，或临时加 --allow-dirty"
  fi
}

ahead_behind() {
  local repo="$1"
  local remote="$2"
  local branch="$3"
  if [[ "${DRY_RUN}" == "1" ]]; then
    echo "0 0"
    return 0
  fi
  local counts
  counts="$(git -C "${repo}" rev-list --left-right --count "HEAD...${remote}/${branch}" 2>/dev/null || echo "0 0")"
  # 输出: ahead behind
  echo "${counts}"
}

check_submodule_pointer_guard() {
  # root 仓库中的 gitlink（crmeb_java）变更，默认禁止直接 push
  local changed
  changed="$(git -C "${ROOT_REPO}" status --porcelain | awk '{print $2}' | grep -x 'crmeb_java' || true)"
  if [[ -z "${changed}" ]]; then
    return 0
  fi

  if [[ "${ALLOW_SUBMODULE_POINTER}" != "1" ]]; then
    die "检测到 root 仓库 gitlink 变更(crmeb_java)。默认阻断。若确认要推，请加 --allow-submodule-pointer"
  fi

  local inner_head
  inner_head="$(git -C "${INNER_REPO}" rev-parse HEAD)"
  if ! git -C "${INNER_REPO}" ls-remote --exit-code "${INNER_REMOTE}" "${inner_head}" >/dev/null 2>&1; then
    die "crmeb_java HEAD(${inner_head}) 未在 ${INNER_REMOTE} 可见，禁止推送 root 指针"
  fi
}

check_repo() {
  local repo="$1"
  local label="$2"
  local remote="$3"
  local branch="$4"

  require_repo "${repo}"
  ensure_branch "${repo}" "${branch}" "${label}"
  run git -C "${repo}" fetch "${remote}" --prune
  ensure_remote_branch "${repo}" "${remote}" "${branch}" "${label}"
  ensure_clean_if_required "${repo}" "${label}"

  local counts ahead behind
  counts="$(ahead_behind "${repo}" "${remote}" "${branch}")"
  ahead="$(echo "${counts}" | awk '{print $1}')"
  behind="$(echo "${counts}" | awk '{print $2}')"
  log "${label} status: branch=${branch}, remote=${remote}/${branch}, ahead=${ahead}, behind=${behind}"
}

sync_repo() {
  local repo="$1"
  local label="$2"
  local remote="$3"
  local branch="$4"

  check_repo "${repo}" "${label}" "${remote}" "${branch}"
  if [[ "${MODE}" == "check" ]]; then
    return 0
  fi

  run git -C "${repo}" pull --rebase "${remote}" "${branch}"

  if [[ "${MODE}" == "push" ]]; then
    run git -C "${repo}" push "${remote}" "HEAD:${branch}"
    log "${label} push done -> ${remote}/${branch}"
  fi
}

main() {
  log "start: target=${TARGET}, mode=${MODE}, root=${ROOT_REMOTE}/${ROOT_BRANCH}, inner=${INNER_REMOTE}/${INNER_BRANCH}"

  if [[ "${TARGET}" == "inner" || "${TARGET}" == "all" ]]; then
    sync_repo "${INNER_REPO}" "inner" "${INNER_REMOTE}" "${INNER_BRANCH}"
  fi

  if [[ "${TARGET}" == "root" || "${TARGET}" == "all" ]]; then
    if [[ "${MODE}" == "push" ]]; then
      check_submodule_pointer_guard
    fi
    sync_repo "${ROOT_REPO}" "root" "${ROOT_REMOTE}" "${ROOT_BRANCH}"
  fi

  log "done"
}

main
