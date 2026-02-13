# 민감정보 분리 가이드 및 환경변수 템플릿

## 현재 상태 요약
- 운영 설정은 `application-prod.properties`에서 환경변수 기반으로 읽도록 구성됨
- 테스트 설정은 더미 기본값 + 환경변수 오버라이드 방식으로 정리됨
- DB 생성/환경 시크릿 등록 실무 절차는 `docs/ENVIRONMENT_SETUP_RUNBOOK.md` 참고

## 권장 분리 방식
1) 민감정보는 환경변수로 주입하고, 소스에는 템플릿만 유지
2) 로컬/운영 프로파일을 분리해 운영 시 `prod` 사용
3) `application-key.properties`는 VCS에서 제외(배포 환경에서 별도 주입)
4) 초기 로컬 설정 파일은 `./scripts/init-local-config.sh`로 템플릿에서 생성
5) CI/CD는 GitHub Environment(`development`, `production`) 별 시크릿을 분리 관리
6) `compose.yml`은 Git에 포함하고, 실제 비밀값은 `.env`(gitignore)로 분리

## 템플릿 위치
- `src/main/resources/application.properties.template`
- `src/main/resources/application-dev.properties.template`
- `src/main/resources/application-prod.properties.template`
- `src/main/resources/application-key.properties.template`

## 환경변수 목록
- `DB_URL`: JDBC URL
- `DB_USERNAME`: DB 계정
- `DB_PASSWORD`: DB 비밀번호
- `REDIS_HOST`: Redis 호스트
- `REDIS_PORT`: Redis 포트
- `REDIS_PASSWORD`: Redis 비밀번호(선택)
- `REDIS_SSL_ENABLED`: Redis TLS 사용 여부 (`true`/`false`)
- `SPRING_PROFILES_ACTIVE`: 실행 프로파일 (`dev`/`staging`/`prod`)
- `IMP_API_SECRETKEY`: PortOne API 시크릿
- `PORTONE_STORE_ID`: PortOne Browser SDK Store ID
- `PORTONE_CHANNEL_KEY`: PortOne Browser SDK Channel Key
- `JWT_SECRET_KEY`: JWT 시크릿

## GitHub Environment 시크릿 키(권장)
- `development`, `staging`, `production` 환경을 만들고 아래 키명을 동일하게 각각 등록
- `DOCKER_IMAGE_NAME`, `DOCKER_USERNAME`, `DOCKER_HUB_ACCESS_TOKEN`
- `EC2_IP`, `EC2_SSH_USER`, `EC2_SSH_KEY`
- `SPRING_PROFILES_ACTIVE`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL_ENABLED`
- `IMP_API_SECRETKEY`
- `PORTONE_STORE_ID`, `PORTONE_CHANNEL_KEY`
- `JWT_SECRET_KEY`

## GitHub Actions 동작 방식
- `pull_request(main)`: 시크릿 스캔 + 테스트만 실행, 배포 안 함
- `push(main)`: `production` 환경으로 배포
- `workflow_dispatch`: `development`/`staging`/`production` 선택 배포

## 운영 체크리스트
1) GitHub `Settings > Environments`에서 `development`, `staging`, `production` 생성
2) 위 키 목록을 각 Environment에 모두 등록
3) `production` 환경에 필수 reviewer/보호 규칙 설정
4) 저장소 공용 `Repository secrets`의 동일 키는 제거하거나 최소화

## 로컬 실행 예시 (PowerShell)
```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:mysql://localhost:3306/coffeeshop?serverTimezone=UTC&characterEncoding=UTF-8&useUnicode=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "{password}"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:REDIS_PASSWORD = "{redis_password_optional}"
$env:REDIS_SSL_ENABLED = "false"
$env:IMP_API_SECRETKEY = "{imp_api_secret}"
$env:PORTONE_STORE_ID = "{store_id}"
$env:PORTONE_CHANNEL_KEY = "{channel_key}"
$env:JWT_SECRET_KEY = "{jwt_secret}"
./gradlew bootRun
```

## 참고
- Spring Boot는 `.env` 파일을 자동으로 읽지 않으므로, 필요한 경우 Docker/CI에서 env 주입을 구성해야 함
- 배포 환경에서는 별도 Secret Manager 또는 CI 변수로 주입 권장
- Dockerfile 기본값은 `SPRING_PROFILES_ACTIVE=prod`이며, 실행 시 `-e SPRING_PROFILES_ACTIVE=...`로 오버라이드 가능
- GitHub Actions 배포 워크플로는 `environment` 컨텍스트를 사용하므로, 환경별 동일 키명을 각각 등록해 분리 운용 가능

## 운영 환경 설정값 예시
아래는 운영 환경에서 자주 사용하는 기준값 예시입니다. 실제 값은 환경에 맞게 조정하세요.

### application-prod.properties (예시)
```properties
spring.application.name=cofshop

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Seoul
spring.jpa.properties.hibernate.default_batch_fetch_size=100
spring.jpa.show-sql=false

spring.redis.host=${REDIS_HOST}
spring.redis.port=${REDIS_PORT}
spring.redis.password=${REDIS_PASSWORD:}
spring.redis.ssl.enabled=${REDIS_SSL_ENABLED:false}

file.dir=C:/cof/image
```

### 환경변수 (.env.example 기준)
```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_URL=
DB_USERNAME=
DB_PASSWORD=
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=
REDIS_SSL_ENABLED=false
IMP_API_SECRETKEY=
PORTONE_STORE_ID=
PORTONE_CHANNEL_KEY=
JWT_SECRET_KEY=
```
