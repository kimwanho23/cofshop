# 백업과 복구

## 백업

`mysqldump` 논리 백업이며 서비스를 멈추지 않습니다. 비밀번호는 인자가 아니라 `MYSQL_PWD` 환경 변수로 전달합니다.

```bash
DB_HOST=<host> DB_NAME=cofshop DB_USER=<user> MYSQL_PWD=<password> bash scripts/db-backup.sh
```

- 결과: `backups/<db>-<시각>.sql.gz`와 `.sha256` 파일
- 보관: 기본 14일 (`BACKUP_RETENTION_DAYS`)
- `backups/`는 Git에서 제외됩니다. **개인정보가 들어 있으므로 운영 서버 밖에 암호화해 보관합니다.**

## 복구

비어 있는 DB에만 복구합니다. 운영 DB를 바로 덮어쓰지 않고, 새 DB에 복구해 확인한 뒤 앱 연결을 옮깁니다.

```bash
DB_HOST=<host> DB_USER=<user> MYSQL_PWD=<password> bash scripts/db-restore.sh backups/<파일>.sql.gz cofshop_restored
```

복구 후 순서:

1. 주요 테이블 건수와 최근 주문을 확인합니다.
2. 앱의 `DB_URL`을 복구한 DB로 바꿉니다. Flyway 이력도 함께 복구되므로 필요한 마이그레이션만 적용됩니다.
3. 앱을 재시작하고 `/actuator/health`를 확인합니다.
4. 백업 시점 이후의 결제는 PortOne 내역과 대조합니다. DB에 없는 결제가 있을 수 있습니다.

## 리허설

임시 MySQL 컨테이너에서 백업과 복구를 실행해 스키마, 테이블 체크섬, 한글 데이터가 원본과 같은지 검증합니다. 비어 있지 않은 DB와 손상된 백업 파일을 거부하는지도 확인합니다. CI에서도 실행합니다.

```bash
bash scripts/db-backup-rehearsal.sh
```

## 2026-10-31 리허설 결과

- 데이터: 회원·상품·옵션 각 1,000건 (테스트 데이터)
- 백업 2초, 복구 9초, 원본과 일치
- **데이터가 적어 운영 시간을 대표하지 않습니다.**

## 아직 충족하지 못한 것

| 항목 | 상태 |
| --- | --- |
| RPO 5분 (가정치) | 미충족. 주기적 덤프만으로는 불가능하며 바이너리 로그나 관리형 DB의 시점 복구가 필요합니다. |
| RTO 1시간 (가정치) | 미확인. 운영 규모 데이터로 측정해야 합니다. |
| 정기 실행 | 없음. 호스팅이 정해지면 스케줄러와 백업 저장소를 구성합니다. |
| 업로드 파일 | 백업 대상 아님. `FILE_STORAGE=local`이면 볼륨 스냅샷이, `s3`이면 스토리지의 버전 관리나 복제가 필요합니다. |
| Redis | 백업 대상 아님. 환불 복구 큐, 쿠폰 재고, 리프레시 토큰이 있어 장애 시 복구 절차가 필요합니다. |
