# 환경 시크릿/DB 설정 런북

## 목적
- `development`, `staging`, `production` 환경 분리
- DB 커넥션 오류 없는 배포
- 비밀값을 코드가 아닌 Environment Secrets로 관리

## 권장 순서
1) DB 인프라 준비(RDS 또는 MySQL)
2) DB/사용자/권한 생성
3) 스키마 반영(마이그레이션)
4) GitHub Environment 생성
5) 환경별 Secrets 등록
6) `workflow_dispatch`로 환경별 배포 검증

## 1) DB를 먼저 준비해야 하나?
- 네, 먼저 준비해야 합니다.
- 앱은 부팅 시 DB 연결을 시도합니다.
- 현재 설정이 `ddl-auto=validate`여서 스키마 미존재 시 기동 실패합니다.

## 2) MySQL 생성 예시
```sql
CREATE DATABASE IF NOT EXISTS cofshop_dev
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cofshop_staging
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cofshop_prod
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'cofshop_app_dev'@'%' IDENTIFIED BY 'CHANGE_ME_DEV';
CREATE USER IF NOT EXISTS 'cofshop_app_staging'@'%' IDENTIFIED BY 'CHANGE_ME_STAGING';
CREATE USER IF NOT EXISTS 'cofshop_app_prod'@'%' IDENTIFIED BY 'CHANGE_ME_PROD';

GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON cofshop_dev.* TO 'cofshop_app_dev'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON cofshop_staging.* TO 'cofshop_app_staging'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON cofshop_prod.* TO 'cofshop_app_prod'@'%';

FLUSH PRIVILEGES;
```

## 2-1) 권한 모델 권장
- 앱 런타임 계정: 최소 권한
- 스키마 변경: 별도 마이그레이션 계정(Flyway/Liquibase) 사용

## 3) DB URL 예시
- dev: `jdbc:mysql://<dev-host>:3306/cofshop_dev?serverTimezone=UTC&characterEncoding=UTF-8&useUnicode=true`
- staging: `jdbc:mysql://<staging-host>:3306/cofshop_staging?serverTimezone=UTC&characterEncoding=UTF-8&useUnicode=true`
- prod: `jdbc:mysql://<prod-host>:3306/cofshop_prod?serverTimezone=UTC&characterEncoding=UTF-8&useUnicode=true`

## 4) GitHub Environments 생성
1) 저장소 `Settings > Environments` 이동
2) `development` 생성
3) `staging` 생성
4) `production` 생성
5) `production`에 Required reviewers 설정

## 5) Environment별 Secrets 키
- `DOCKER_IMAGE_NAME`
- `DOCKER_USERNAME`
- `DOCKER_HUB_ACCESS_TOKEN`
- `EC2_IP`
- `EC2_SSH_USER`
- `EC2_SSH_KEY`
- `SPRING_PROFILES_ACTIVE`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `REDIS_SSL_ENABLED`
- `IMP_API_SECRETKEY`
- `PORTONE_STORE_ID`
- `PORTONE_CHANNEL_KEY`
- `JWT_SECRET_KEY`

## 6) 값 가이드(예시)
| Environment | SPRING_PROFILES_ACTIVE | DB URL DB명 | DB 사용자 |
| --- | --- | --- | --- |
| `development` | `dev` | `cofshop_dev` | `cofshop_app_dev` |
| `staging` | `staging` | `cofshop_staging` | `cofshop_app_staging` |
| `production` | `prod` | `cofshop_prod` | `cofshop_app_prod` |

## 7) 등록 체크리스트
- [ ] MySQL/RDS에 `cofshop_dev`, `cofshop_staging`, `cofshop_prod` 생성
- [ ] 환경별 DB 계정/비밀번호 생성
- [ ] 런타임 최소 권한 부여
- [ ] 스키마 마이그레이션 완료
- [ ] GitHub Environment `development` 생성
- [ ] GitHub Environment `staging` 생성
- [ ] GitHub Environment `production` 생성
- [ ] 각 Environment에 동일 키명 Secrets 등록
- [ ] `production`에 reviewer 보호 정책 설정
- [ ] `workflow_dispatch -> development` 배포 확인
- [ ] `workflow_dispatch -> staging` 배포 확인
- [ ] `push main -> production` 배포 확인

## 7-1) 시크릿 일괄 등록(권장)
1) 예시 복사
```bash
mkdir -p .secrets
cp docs/examples/development.secrets.env.example .secrets/development.env
cp docs/examples/staging.secrets.env.example .secrets/staging.env
cp docs/examples/production.secrets.env.example .secrets/production.env
```

2) 각 파일 값 채우기 (`EC2_SSH_KEY`는 파일로 별도 주입)

3) 환경별 등록 실행
```bash
./scripts/gh-set-env-secrets.sh development .secrets/development.env ~/.ssh/ec2_key.pem
./scripts/gh-set-env-secrets.sh staging .secrets/staging.env ~/.ssh/ec2_key.pem
./scripts/gh-set-env-secrets.sh production .secrets/production.env ~/.ssh/ec2_key.pem
```

## 8) 배포 검증 절차
1) `workflow_dispatch` 실행
2) `target_environment=development` 배포
3) 헬스체크/로그로 DB 연결 확인
4) `target_environment=staging` 배포
5) 검증 완료 후 `main` 병합으로 production 배포

## 9) 자주 발생하는 실패
- `Missing required secret`: Environment에 키 누락
- `Access denied for user`: DB 사용자/비밀번호/권한 불일치
- `Unknown database`: DB 미생성 또는 DB명 오타
- `Schema-validation`: 스키마 미반영 상태
