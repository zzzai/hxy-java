#!/usr/bin/env bash
set -euo pipefail

OLD_MYSQL_VOLUME="yudao-system_mysql"
OLD_REDIS_VOLUME="yudao-system_redis"
NEW_MYSQL_VOLUME="hxy-platform_mysql"
NEW_REDIS_VOLUME="hxy-platform_redis"
BACKUP_ROOT="$(pwd)/backup/volume-migration"
DRY_RUN=0
ALLOW_LIVE=0
FORCE=0

usage() {
  cat <<'EOF'
Usage:
  bash script/docker/migrate_volume_names_to_hxy.sh [options]

Options:
  --dry-run                 仅打印动作，不执行
  --allow-live              允许在旧卷被运行中容器挂载时继续迁移
  --force                   即使新卷有数据也继续覆盖
  --old-mysql-volume NAME   旧 MySQL 卷名，默认 yudao-system_mysql
  --old-redis-volume NAME   旧 Redis 卷名，默认 yudao-system_redis
  --new-mysql-volume NAME   新 MySQL 卷名，默认 hxy-platform_mysql
  --new-redis-volume NAME   新 Redis 卷名，默认 hxy-platform_redis
  --backup-root PATH        备份根目录，默认 ./backup/volume-migration
  -h, --help                显示帮助

Data retention steps:
  1) 先把旧卷打包为 tar.gz 备份
  2) 再将旧卷数据拷贝到新卷
  3) 校验新卷文件数量不小于旧卷（旧卷非空时）
EOF
}

log() { printf '[volume-migrate] %s\n' "$*"; }
err() { printf '[volume-migrate][ERROR] %s\n' "$*" >&2; }

run_cmd() {
  if [[ "$DRY_RUN" -eq 1 ]]; then
    log "DRY-RUN: $*"
    return 0
  fi
  eval "$@"
}

ensure_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    err "docker command not found"
    exit 1
  fi
}

volume_exists() {
  docker volume inspect "$1" >/dev/null 2>&1
}

running_containers_using_volume() {
  docker ps --filter "volume=$1" --format '{{.Names}}'
}

assert_volume_not_in_use() {
  local vol="$1"
  local users
  users="$(running_containers_using_volume "$vol" || true)"
  if [[ -n "$users" && "$ALLOW_LIVE" -ne 1 ]]; then
    if [[ "$DRY_RUN" -eq 1 ]]; then
      log "DRY-RUN: volume $vol is mounted by running container(s): $users"
      log "DRY-RUN: would fail without --allow-live"
      return 0
    fi
    err "volume $vol is mounted by running container(s): $users"
    err "stop related containers first, or rerun with --allow-live"
    exit 1
  fi
}

volume_file_count() {
  docker run --rm -v "$1":/data alpine:3.20 sh -c "find /data -mindepth 1 | wc -l" | tr -d '[:space:]'
}

backup_volume() {
  local src="$1"
  local backup_dir="$2"
  local backup_file="$backup_dir/${src}.tar.gz"
  run_cmd "mkdir -p '$backup_dir'"
  run_cmd "docker run --rm -v '$src':/from -v '$backup_dir':/backup alpine:3.20 sh -c \"cd /from && tar czf '/backup/$(basename "$backup_file")' .\""
  log "backup created: $backup_file"
}

copy_volume() {
  local src="$1"
  local dst="$2"
  run_cmd "docker run --rm -v '$src':/from -v '$dst':/to alpine:3.20 sh -c \"cd /from && tar cf - . | tar xf - -C /to\""
}

ensure_target_volume_ready() {
  local dst="$1"
  if ! volume_exists "$dst"; then
    run_cmd "docker volume create '$dst' >/dev/null"
    return 0
  fi
  local dst_count
  dst_count="$(volume_file_count "$dst")"
  if [[ "$dst_count" != "0" && "$FORCE" -ne 1 ]]; then
    err "target volume $dst already has data ($dst_count entries). use --force to continue"
    exit 1
  fi
}

migrate_one() {
  local src="$1"
  local dst="$2"
  local backup_dir="$3"

  if ! volume_exists "$src"; then
    log "skip: source volume not found: $src"
    return 0
  fi

  assert_volume_not_in_use "$src"
  ensure_target_volume_ready "$dst"

  if [[ "$DRY_RUN" -eq 1 ]]; then
    log "plan: backup $src -> $backup_dir/${src}.tar.gz"
    log "plan: copy $src -> $dst"
    return 0
  fi

  local src_count dst_count
  src_count="$(volume_file_count "$src")"
  backup_volume "$src" "$backup_dir"
  copy_volume "$src" "$dst"
  dst_count="$(volume_file_count "$dst")"

  if [[ "$src_count" != "0" && "$dst_count" == "0" ]]; then
    err "verification failed: source $src has $src_count entries, target $dst has 0"
    exit 1
  fi
  log "migrated $src -> $dst (source entries: $src_count, target entries: $dst_count)"
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --dry-run) DRY_RUN=1; shift ;;
      --allow-live) ALLOW_LIVE=1; shift ;;
      --force) FORCE=1; shift ;;
      --old-mysql-volume) OLD_MYSQL_VOLUME="$2"; shift 2 ;;
      --old-redis-volume) OLD_REDIS_VOLUME="$2"; shift 2 ;;
      --new-mysql-volume) NEW_MYSQL_VOLUME="$2"; shift 2 ;;
      --new-redis-volume) NEW_REDIS_VOLUME="$2"; shift 2 ;;
      --backup-root) BACKUP_ROOT="$2"; shift 2 ;;
      -h|--help) usage; exit 0 ;;
      *) err "unknown option: $1"; usage; exit 1 ;;
    esac
  done
}

main() {
  parse_args "$@"
  ensure_docker

  local ts backup_dir
  ts="$(date +%Y%m%d_%H%M%S)"
  backup_dir="${BACKUP_ROOT}/${ts}"

  log "start migration"
  log "mysql: $OLD_MYSQL_VOLUME -> $NEW_MYSQL_VOLUME"
  log "redis: $OLD_REDIS_VOLUME -> $NEW_REDIS_VOLUME"
  log "backup dir: $backup_dir"

  migrate_one "$OLD_MYSQL_VOLUME" "$NEW_MYSQL_VOLUME" "$backup_dir"
  migrate_one "$OLD_REDIS_VOLUME" "$NEW_REDIS_VOLUME" "$backup_dir"

  log "done"
  log "next step: set MYSQL_VOLUME_NAME=$NEW_MYSQL_VOLUME and REDIS_VOLUME_NAME=$NEW_REDIS_VOLUME in script/docker/docker.env"
}

main "$@"
