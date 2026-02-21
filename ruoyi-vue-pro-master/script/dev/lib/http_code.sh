#!/usr/bin/env bash
# shellcheck shell=bash

normalize_http_code() {
  local raw="${1:-}"
  local digits=""
  local code=""
  digits="$(printf '%s' "${raw}" | tr -cd '0-9')"
  if [[ ${#digits} -lt 3 ]]; then
    printf '000'
    return 0
  fi
  code="${digits:0:3}"
  case "${code}" in
    000|[1-5][0-9][0-9])
      ;;
    *)
      code="000"
      ;;
  esac
  printf '%s' "${code}"
}

curl_http_code() {
  local timeout_seconds="${1:-8}"
  shift || true
  local raw_code=""
  raw_code="$(curl -sS -m "${timeout_seconds}" -o /dev/null -w '%{http_code}' "$@" || true)"
  normalize_http_code "${raw_code}"
}
