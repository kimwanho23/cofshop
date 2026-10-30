#!/usr/bin/env bash
# 임시 MySQL 컨테이너에서 백업과 복구를 실행해 원본과 같은지 검증한다. 다른 DB는 건드리지 않는다.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_ID="cofshop-rehearsal-$$"
NETWORK="$RUN_ID"
CONTAINER="$RUN_ID-mysql"
WORK_DIR="$(mktemp -d)"
ROOT_PASSWORD="rehearsal-only"

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

sql() {
  docker exec -i -e MYSQL_PWD="$ROOT_PASSWORD" "$CONTAINER" \
    mysql --user=root --default-character-set=utf8mb4 "$@"
}

docker network create "$NETWORK" >/dev/null
docker run -d --name "$CONTAINER" --network "$NETWORK" \
  -e MYSQL_ROOT_PASSWORD="$ROOT_PASSWORD" -e MYSQL_DATABASE=source_db mysql:8.0 >/dev/null

echo "waiting for mysql..."
for _ in $(seq 1 60); do
  if sql -N -e "SELECT 1" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

for migration in "$ROOT_DIR"/src/main/resources/db/migration/V*.sql; do
  sql source_db < "$migration"
done

sql source_db <<'EOF'
INSERT INTO member (email, member_name, member_pwd, tel, member_state, role, created_at, last_password_change, point)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
SELECT CONCAT('user', n, '@example.com'), CONCAT('회원', n), 'pw', '01000000000', 'ACTIVE', 'MEMBER', NOW(6), NOW(6), n FROM seq;
INSERT INTO item (item_name, price, discount, item_state, origin, seller_id, average_rating, review_count, delivery_fee)
SELECT CONCAT('상품 ☕ ', member_id), 1000 + member_id, 0, 'SELL', '브라질', member_id, 0, 0, 0 FROM member;
INSERT INTO item_option (description, additional_price, stock, option_state, item_id)
SELECT '기본', 0, 100, 'SELL', item_id FROM item;
EOF

snapshot() {
  local db="$1"
  local tables
  tables="$(sql -N -e "SELECT GROUP_CONCAT(table_name ORDER BY table_name SEPARATOR ', ') FROM information_schema.tables WHERE table_schema='${db}'" | tr -d '\r')"
  sql -N -e "SELECT table_name, column_name, column_type, is_nullable FROM information_schema.columns WHERE table_schema='${db}' ORDER BY table_name, ordinal_position" | tr -d '\r'
  sql -N "$db" -e "CHECKSUM TABLE ${tables}" | tr -d '\r' | sed -E "s/^${db}\.//"
  sql -N "$db" -e "SELECT COUNT(*), MAX(member_name) FROM member" | tr -d '\r'
}

snapshot source_db > "$WORK_DIR/source.txt"

export DB_HOST="$CONTAINER" DB_NAME=source_db DB_USER=root MYSQL_PWD="$ROOT_PASSWORD"
export DOCKER_NETWORK="$NETWORK" BACKUP_DIR="$WORK_DIR/backups"

backup_start="$(date +%s)"
backup_file="$(bash "$ROOT_DIR"/scripts/db-backup.sh | tail -n 1)"
restore_start="$(date +%s)"
bash "$ROOT_DIR"/scripts/db-restore.sh "$backup_file" restored_db
restore_end="$(date +%s)"

snapshot restored_db > "$WORK_DIR/restored.txt"

if ! diff "$WORK_DIR/source.txt" "$WORK_DIR/restored.txt" > "$WORK_DIR/diff.txt"; then
  echo "REHEARSAL FAILED: restored database differs" >&2
  cat "$WORK_DIR/diff.txt" >&2
  exit 1
fi

echo "checks: non-empty target must be refused"
if bash "$ROOT_DIR"/scripts/db-restore.sh "$backup_file" restored_db >/dev/null 2>&1; then
  echo "REHEARSAL FAILED: restore overwrote a non-empty database" >&2
  exit 1
fi

echo "checks: corrupted backup must be refused"
printf 'x' >> "$backup_file"
if bash "$ROOT_DIR"/scripts/db-restore.sh "$backup_file" corrupted_db >/dev/null 2>&1; then
  echo "REHEARSAL FAILED: restore accepted a corrupted backup" >&2
  exit 1
fi

echo "REHEARSAL OK"
echo "tables and checksums compared: $(wc -l < "$WORK_DIR/source.txt") lines"
echo "sample: $(tail -n 1 "$WORK_DIR/restored.txt")"
echo "backup seconds: $((restore_start - backup_start)), restore seconds: $((restore_end - restore_start))"
