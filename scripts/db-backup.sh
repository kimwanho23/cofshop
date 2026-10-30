#!/usr/bin/env bash
# MySQL 논리 백업. 비밀번호는 인자 대신 환경 변수 MYSQL_PWD로 전달한다.
#   DB_HOST, DB_NAME, DB_USER, MYSQL_PWD: 필수
#   DB_PORT(3306), DOCKER_NETWORK(host), BACKUP_DIR(./backups), BACKUP_RETENTION_DAYS(14): 선택
set -euo pipefail

: "${DB_HOST:?DB_HOST is required}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${MYSQL_PWD:?MYSQL_PWD is required}"

DB_PORT="${DB_PORT:-3306}"
DOCKER_NETWORK="${DOCKER_NETWORK:-host}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

mkdir -p "$BACKUP_DIR"
backup_file="${BACKUP_DIR}/${DB_NAME}-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
temp_file="${backup_file}.tmp"
trap 'rm -f "$temp_file"' EXIT

# --single-transaction: 서비스를 멈추지 않고 InnoDB 일관성 스냅샷을 백업
docker run -i --rm --network "$DOCKER_NETWORK" -e MYSQL_PWD mysql:8.0 \
  mysqldump --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" \
  --single-transaction --quick --routines --triggers --set-gtid-purged=OFF --no-tablespaces \
  --default-character-set=utf8mb4 "$DB_NAME" | gzip > "$temp_file"

# pipefail로 덤프 실패를 잡고, 완료 후에만 최종 이름으로 옮겨 불완전한 백업이 남지 않게 함
gzip -t "$temp_file"
mv "$temp_file" "$backup_file"
(cd "$BACKUP_DIR" && sha256sum "$(basename "$backup_file")" > "$(basename "$backup_file").sha256")

find "$BACKUP_DIR" -name "${DB_NAME}-*.sql.gz*" -type f -mtime "+${BACKUP_RETENTION_DAYS}" -delete

echo "$backup_file"
