#!/usr/bin/env bash
# .env의 PortOne 설정을 확인한다. 존재하지 않는 결제 건을 조회할 뿐 결제나 취소는 하지 않으며, 값은 출력하지 않는다.
set -euo pipefail

ENV_FILE="${1:-.env}"
BASE_URL="${IMP_API_BASE_URL:-https://api.portone.io}"

read_env() {
  grep -E "^$1=" "$ENV_FILE" | head -n 1 | cut -d= -f2- | tr -d '\r"'"'" || true
}

secret="$(read_env IMP_API_SECRETKEY)"
store_id="$(read_env PORTONE_STORE_ID)"
channel_key="$(read_env PORTONE_CHANNEL_KEY)"

[ -n "$store_id" ] && echo "PORTONE_STORE_ID: 있음" || echo "PORTONE_STORE_ID: 비어 있음 (결제창에 필요)"
[ -n "$channel_key" ] && echo "PORTONE_CHANNEL_KEY: 있음" || echo "PORTONE_CHANNEL_KEY: 비어 있음 (결제창에 필요)"

if [ -z "$secret" ]; then
  echo "IMP_API_SECRETKEY: 비어 있음"
  exit 1
fi

status="$(curl -s -m 20 -o /dev/null -w '%{http_code}' \
  -H "Authorization: PortOne ${secret}" \
  "${BASE_URL}/payments/cofshop-connection-check-$(date +%s)")"

case "$status" in
  404) echo "API 시크릿: 정상 (인증 통과, 없는 결제 건이라 404)" ;;
  401) echo "API 시크릿: 거부됨 (401). V2 API 시크릿인지 확인하세요."; exit 1 ;;
  403) echo "API 시크릿: 권한 없음 (403). 시크릿의 권한을 확인하세요."; exit 1 ;;
  *) echo "API 시크릿: 확인 실패 (HTTP ${status})"; exit 1 ;;
esac
