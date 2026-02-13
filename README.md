# cofshop — 쇼핑몰 백엔드 REST API

![Java](https://img.shields.io/badge/Java-17-007396)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F)
![Gradle](https://img.shields.io/badge/Build-Gradle-02303A)

Spring Boot 기반 쇼핑몰 백엔드 서버입니다. 회원/상품/장바구니/주문/쿠폰/결제/채팅/통계 기능을 REST API로 제공하며, 테스트 코드로 주요 비즈니스 로직을 검증합니다.

## 목차
- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [구조 요약](#구조-요약)
- [주요 기능](#주요-기능)
- [API 문서](#api-문서)
- [대표 API 예시](#대표-api-예시)
- [공통 응답 형식](#공통-응답-형식)
- [EER 다이어그램](#eer-다이어그램)
- [실행 방법](#실행-방법)
- [환경 변수](#환경-변수)
- [문서/자료](#문서자료)
- [테스트](#테스트)
- [운영/보안 유의사항](#운영보안-유의사항)
- [Trouble Shooting](#trouble-shooting)

## 프로젝트 개요
- 도메인: 쇼핑몰(상품/주문/쿠폰/결제/리뷰/통계)
- 아키텍처: Spring Boot REST API + Redis Stream + WebSocket(STOMP)
- 문서: Swagger UI 제공

## 기술 스택
| 구분 | 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3, Spring Data JPA, Spring Security |
| Database | MySQL, H2(test) |
| Cache/Queue | Redis, Redisson |
| Search | Elasticsearch |
| Docs | springdoc-openapi (Swagger UI) |
| Build | Gradle |
| Test | JUnit, Spring Test, REST Docs |

## 구조 요약
- `src/main/java/kwh/cofshop`
  - `config/` 설정 (Security, Redis, Swagger, WebSocket, QueryDSL 등)
  - `global/` 공통 응답/예외 처리
  - `member/`, `item/`, `order/`, `cart/`, `coupon/`, `payment/`, `chat/`, `statistics/`
  - `aspect/`, `argumentResolver/`, `security/`
- `src/main/resources/`
  - `application*.properties` (프로파일/환경 설정)
  - `templates/` 결제 관련 템플릿
- `docs/` 상세 문서

## 주요 기능
- 회원: 가입/로그인/상태 관리, JWT 인증
- 상품: 등록/수정/조회/검색, 카테고리, 리뷰
- 주문: 생성/취소/상태 변경, 배송 정책 적용
- 장바구니: 담기/수정/삭제, 총 금액 계산
- 쿠폰: Redis Stream 기반 발급 처리, 만료 스케줄링
- 결제: PortOne(Iamport) 연동, 결제 검증/환불
- 채팅: WebSocket(STOMP) 1:1 상담
- 통계: 기간별/일별 매출, 인기 상품 집계
- 비기능: 공통 응답 포맷, 예외 처리, 동시성 제어, QueryDSL, MapStruct

## API 문서
| 구분 | 경로 |
| --- | --- |
| Swagger UI (Public) | https://kimwanho23.github.io/cofshop |
| Swagger UI (Local) | `/swagger-ui` |

## 대표 API 예시
### 상품 검색
```bash
curl -X POST http://localhost:8080/api/item/search \
  -H "Content-Type: application/json" \
  -d '{"keyword":"coffee","minPrice":0,"maxPrice":50000,"categoryId":1}'
```

### 응답 예시 (요약)
```json
{
  "header": {
    "status": "OK",
    "code": "S-OK"
  },
  "body": {
    "message": "API LOAD SUCCESSFUL",
    "timestamp": "2025-01-01T12:00:00",
    "data": {
      "content": [
        {
          "itemName": "Ethiopia Guji",
          "price": 18000,
          "discount": 10,
          "deliveryFee": 0,
          "categoryId": 1,
          "itemState": "SELLING"
        }
      ],
      "totalElements": 1,
      "totalPages": 1,
      "number": 0,
      "size": 20
    }
  }
}
```

### 주문 생성
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {access_token}" \
  -d '{"orderItems":[{"itemOptionId":10,"quantity":2}],"couponId":3}'
```

### 주문 생성 응답 예시 (요약)
```json
{
  "header": {
    "status": "CREATED",
    "code": "C-OK"
  },
  "body": {
    "message": "CREATED SUCCESSFUL",
    "timestamp": "2025-01-01T12:00:00",
    "data": {
      "orderId": 1001,
      "orderState": "PAID_READY",
      "deliveryRequest": "문 앞에 놓아주세요",
      "address": {
        "city": "Seoul",
        "street": "Teheran-ro 123",
        "zipCode": "06236"
      },
      "deliveryFee": 3000,
      "totalPrice": 36000,
      "discountFromCoupon": 3000,
      "usePoint": 0,
      "finalPrice": 33000,
      "orderItemResponseDto": [
        {
          "itemName": "Ethiopia Guji",
          "price": 18000,
          "additionalPrice": 0,
          "discountRate": 10,
          "origin": "Ethiopia",
          "quantity": 2
        }
      ]
    }
  }
}
```

### 결제 검증
```bash
curl -X POST http://localhost:8080/api/payments/{paymentId}/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {access_token}" \
  -d '{"impUid":"imp_123","merchantUid":"order_456","amount":36000}'
```

### 결제 검증 응답 예시 (요약)
```
HTTP/1.1 200 OK
(empty body)
```

### 쿠폰 발급
```bash
curl -X POST http://localhost:8080/api/memberCoupon/me/3 \
  -H "Authorization: Bearer {access_token}"
```

### 쿠폰 발급 응답 예시 (요약)
```json
{
  "header": {
    "status": "CREATED",
    "code": "C-OK"
  },
  "body": {
    "message": "CREATED SUCCESSFUL",
    "timestamp": "2025-01-01T12:00:00",
    "data": "쿠폰 발급 요청 완료"
  }
}
```

## 공통 응답 형식
모든 API는 공통 응답 포맷을 사용합니다.
```json
{
  "header": {
    "status": "OK",
    "code": "S-OK"
  },
  "body": {
    "message": "API LOAD SUCCESSFUL",
    "timestamp": "2025-01-01T12:00:00",
    "data": {}
  }
}
```

## EER 다이어그램
![eer](https://github.com/user-attachments/assets/607f5d11-6356-44b3-8f71-1c52d53bead7)

## 실행 방법
### 사전 준비
- Java 17
- MySQL
- Redis

### 실행
```powershell
./gradlew bootRun
```

### 프로파일별 실행
```powershell
# 초기 1회: 템플릿에서 로컬 설정 파일 생성
./scripts/init-local-config.sh

# dev
$env:SPRING_PROFILES_ACTIVE = "dev"
./gradlew bootRun

# prod
$env:SPRING_PROFILES_ACTIVE = "prod"
./gradlew bootRun
```

### Docker Compose 실행
`compose.yml`은 Git에 포함하고, 실제 값은 `.env`로 분리해서 사용합니다.

```bash
# 1) .env 준비
cp .env.example .env

# 2) RDS/외부 DB를 사용하는 기본 실행 (app + redis)
docker compose up --build -d

# 3) 로컬 MySQL까지 함께 실행 (app + redis + mysql)
docker compose --profile local-db up --build -d
```

로컬 MySQL 프로필(`local-db`)을 사용할 때는 `.env`의 `DB_URL`을 `mysql` 컨테이너 기준으로 맞춰야 합니다.
예: `jdbc:mysql://mysql:3306/cofshop?serverTimezone=UTC&characterEncoding=UTF-8&useUnicode=true`
PortOne V2 샘플 결제 페이지는 `/payments/sample`에서 확인할 수 있습니다.

## 환경 변수
민감정보는 환경 변수로 주입하는 것을 권장합니다. 예시는 `.env.example` 참고.

| 변수 | 설명 |
| --- | --- |
| `DB_URL` | JDBC URL |
| `DB_USERNAME` | DB 계정 |
| `DB_PASSWORD` | DB 비밀번호 |
| `REDIS_HOST` | Redis 호스트 |
| `REDIS_PORT` | Redis 포트 |
| `REDIS_PASSWORD` | Redis 비밀번호(선택) |
| `REDIS_SSL_ENABLED` | Redis TLS 사용 여부(`true/false`) |
| `IMP_API_SECRETKEY` | PortOne API 시크릿 |
| `PORTONE_STORE_ID` | PortOne Browser SDK Store ID |
| `PORTONE_CHANNEL_KEY` | PortOne Browser SDK Channel Key |
| `JWT_SECRET_KEY` | JWT 시크릿 |
| `SPRING_PROFILES_ACTIVE` | 실행 프로파일 (`dev`/`staging`/`prod`) |

## 문서/자료
- `docs/PROJECT_OVERVIEW.md`: 전체 구조 요약
- `docs/MODULES.md`: 모듈 상세 및 플로우
- `docs/SECRETS_AND_ENV.md`: 민감정보 분리/운영 설정 가이드
- `docs/ENVIRONMENT_SETUP_RUNBOOK.md`: DB 생성 + 환경 시크릿 등록 런북
- `docs/README_ENCODING.md`: README 인코딩 안내
- `docs/TROUBLESHOOTING.md`: 트러블슈팅 모음
- `docs/INDEX.md`: 문서 인덱스

## 테스트
```powershell
./gradlew test
```

## 운영/보안 유의사항
- `src/main/resources/application*.properties`는 Git 추적에서 제외되어 있으니, 초기 1회 `./scripts/init-local-config.sh`를 실행해 템플릿에서 생성하세요.
- 추적 가능한 템플릿은 `src/main/resources/application*.properties.template`에 있습니다.
- 실제 비밀값(DB/Redis/결제/JWT)은 템플릿이 아닌 환경 변수/Secret Manager로 주입하세요.
- CI는 `gitleaks` 시크릿 스캔을 수행하며, 규칙은 `.gitleaks.toml`에서 관리합니다.
- GitHub Actions 배포는 `GitHub Environments`(`development`, `production`) 단위로 시크릿을 분리해 관리하세요.
- GitHub 공개 리포지토리에는 시크릿을 커밋하지 않도록 주의하세요.

## Trouble Shooting
자세한 내용은 `docs/TROUBLESHOOTING.md` 참고.
