# 민감정보 분리 가이드 및 환경변수 템플릿

## 현재 상태 요약
- `application.properties` / `application-key.properties`에 DB 비밀번호, JWT 시크릿, IMP 키 등이 포함되어 있음
- 운영 설정은 `application-prod.properties`에서 환경변수 기반으로 읽도록 구성됨

## 권장 분리 방식
1) 민감정보는 환경변수로 주입하고, 소스에는 템플릿만 유지
2) 로컬/운영 프로파일을 분리해 운영 시 `prod` 사용
3) `application-key.properties`는 VCS에서 제외(배포 환경에서 별도 주입)

## 환경변수 목록
- `DB_URL`: JDBC URL
- `DB_USERNAME`: DB 계정
- `DB_PASSWORD`: DB 비밀번호
- `REDIS_HOST`: Redis 호스트
- `REDIS_PORT`: Redis 포트
- `IMP_CODE`: PortOne 가맹점 코드
- `IMP_API_KEY`: PortOne API 키
- `IMP_API_SECRETKEY`: PortOne API 시크릿
- `JWT_SECRET_KEY`: JWT 시크릿

## 로컬 실행 예시 (PowerShell)
```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:mysql://localhost:3306/coffeeshop?serverTimezone=UTC&characterEncoding=UTF-8&useUnicode=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "{password}"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:IMP_CODE = "{imp_code}"
$env:IMP_API_KEY = "{imp_api_key}"
$env:IMP_API_SECRETKEY = "{imp_api_secret}"
$env:JWT_SECRET_KEY = "{jwt_secret}"
./gradlew bootRun
```

## 참고
- Spring Boot는 `.env` 파일을 자동으로 읽지 않으므로, 필요한 경우 Docker/CI에서 env 주입을 구성해야 함
- 배포 환경에서는 별도 Secret Manager 또는 CI 변수로 주입 권장

## 운영 환경 설정값 예시
아래는 운영 환경에서 자주 사용하는 기준값 예시입니다. 실제 값은 환경에 맞게 조정하세요.

### application-prod.properties (예시)
```properties
spring.application.name=cofshop

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Seoul
spring.jpa.properties.hibernate.default_batch_fetch_size=100
spring.jpa.show-sql=true

spring.redis.host=${REDIS_HOST}
spring.redis.port=${REDIS_PORT}

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
IMP_CODE=
IMP_API_KEY=
IMP_API_SECRETKEY=
JWT_SECRET_KEY=
```
