#!/usr/bin/env bash
# Alertmanager로 테스트 알림을 보낸다. 사용 전 docker compose --profile monitoring up -d alertmanager 실행
set -euo pipefail

ALERTMANAGER_URL="${ALERTMANAGER_URL:-http://localhost:9093}"

curl -fsS -X POST "${ALERTMANAGER_URL}/api/v2/alerts" \
  -H "Content-Type: application/json" \
  -d '[{
    "labels": {"alertname": "TestAlert", "severity": "warning"},
    "annotations": {"summary": "Test alert", "description": "Checking the alert delivery path."}
  }]'

echo "sent. group_wait(30s) 후 수신 채널을 확인하세요."
