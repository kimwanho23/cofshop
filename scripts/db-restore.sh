#!/usr/bin/env bash
# 백업 파일을 복구한다. 운영 DB를 덮어쓰지 않도록 비어 있는 대상 DB에만 복구한다.
#   사용법: db-restore.sh <backup.sql.gz> <target-db>
#   DB_HOST, DB_USER, MYSQL_PWD: 필수 / DB_PORT(3306), DOCKER_NETWORK(host): 선택
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "usage: $0 <backup.sql.gz> <target-db>" >&2
  exit 2
fi

backup_file="$1"
target_db="$2"

: "${DB_HOST:?DB_HOST is required}"
: "${DB_USER:?DB_USER is required}"
: "${MYSQL_PWD:?MYSQL_PWD is required}"

DB_PORT="${DB_PORT:-3306}"
DOCKER_NETWORK="${DOCKER_NETWORK:-host}"

if ! printf '%s' "$target_db" | grep -Eq '^[A-Za-z0-9_]+$'; then
  echo "invalid target db name: $target_db" >&2
  exit 2
fi

checksum_file="${backup_file}.sha256"
if [ ! -f "$checksum_file" ]; then
  echo "checksum file not found: $checksum_file" >&2
  exit 1
fi
(cd "$(dirname "$backup_file")" && sha256sum -c "$(basename "$checksum_file")" >/dev/null)

run_mysql() {
  docker run -i --rm --network "$DOCKER_NETWORK" -e MYSQL_PWD mysql:8.0 \
    mysql --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" --default-character-set=utf8mb4 "$@"
}

table_count="$(run_mysql -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${target_db}'" | tr -d '\r')"
if [ "$table_count" != "0" ]; then
  echo "target db is not empty: ${target_db} (${table_count} tables)" >&2
  exit 1
fi

run_mysql -e "CREATE DATABASE IF NOT EXISTS \`${target_db}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
gzip -dc "$backup_file" | run_mysql "$target_db"

echo "restored ${backup_file} into ${target_db}"
