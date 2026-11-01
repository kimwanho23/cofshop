# 프로젝트 개요 (cofshop)

이 문서는 `cofshop` 백엔드 프로젝트의 구조와 기술 스택을 요약합니다.

## 1) 한눈에 보는 구성
- 목적: 커피/쇼핑몰 도메인의 REST API 서버
- 주요 기능: 회원/상품/장바구니/주문/쿠폰/리뷰/결제/통계/채팅
- 문서: Swagger UI 제공 (springdoc 설정)
- 실서비스 전환 계획: [ROADMAP.md](ROADMAP.md), [RISK_REGISTER.md](RISK_REGISTER.md), [TESTING.md](TESTING.md)

## 2) 기술 스택
- Language: Java 17
- Framework: Spring Boot 3.5, Spring Modulith
- Build: Gradle
- Web: Spring MVC, Thymeleaf(결제 샘플 페이지), WebSocket(STOMP)
- Security: Spring Security + JWT
- Data: Spring Data JPA, QueryDSL, MapStruct, Flyway
- DB: MySQL 8.0 (운영), H2 (일부 빠른 테스트)
- Cache/Queue: Redis, Redisson(분산 락), RabbitMQ(선택)
- API Docs: springdoc-openapi (Swagger UI)
- Payment: PortOne V2 REST API
- Test: JUnit, Mockito, Testcontainers, Spring REST Docs

## 3) 프로젝트 디렉터리 구조 (요약)
- `src/main/java/kwh/cofshop`
  - `config/`: Redis/Swagger/WebSocket/QueryDSL 등 설정
  - `security/`: JWT, 인증/인가 필터, 로그인/로그아웃 처리
  - `global/`: 공통 응답 포맷, 예외 처리, 공통 엔티티
  - `member/`: 회원 도메인
  - `item/`: 상품/카테고리/리뷰/이미지/옵션
  - `order/`: 주문/주문아이템/배송정책
  - `cart/`: 장바구니
  - `coupon/`: 쿠폰 발급/사용, outbox
  - `payment/`: 결제 요청/검증/환불
  - `chat/`: 고객센터 1:1 채팅
  - `statistics/`: 매출/인기상품 통계
  - `aspect/`: 분산 락, 로깅
  - `argumentresolver/`: 커스텀 인자 리졸버
  - `file/`: 파일 업로드
- `src/main/resources/`
  - `application*.properties.template`: 설정 템플릿 (Git 추적)
  - `application*.properties`: 템플릿에서 생성하는 로컬 설정 (Git 제외)
  - `db/migration/`: Flyway 스키마 마이그레이션
  - `templates/payInicis.html`: 결제 샘플 페이지

## 4) 주요 도메인/모듈 설명
- 회원(Member): 가입/로그인/상태관리/권한
- 상품(Item): 상품/옵션/이미지/카테고리/리뷰, 판매 상태 변경
- 주문(Order): 주문 생성/취소/상태 변경, 환불 요청 처리
- 장바구니(Cart): 품목 추가/수정/삭제
- 쿠폰(Coupon): 발급/사용/만료 처리, outbox 스케줄러
- 결제(Payment): PortOne 연동, 결제 검증/환불, 환불 복구 큐
- 채팅(Chat): WebSocket 기반 1:1 채팅
- 통계(Statistics): 기간/일별 매출, 인기 상품 집계

## 5) 설정 및 실행 관련 포인트
- 최초 1회 `./scripts/init-local-config.sh`로 템플릿에서 설정 파일을 생성합니다.
- 프로파일: `dev`, `prod`. 비밀값은 환경 변수로 주입합니다. 목록은 `.env.example`을 참고하세요.
- 스키마는 Flyway가 관리하며 Hibernate는 `ddl-auto=validate`로 검증만 합니다.
- 기존 DB를 Flyway 관리로 전환할 때는 스키마가 V1과 같은지 확인한 뒤 `FLYWAY_BASELINE_ON_MIGRATE=true`로 1회 실행합니다.

## 6) 테스트
- 규칙과 필수 시나리오는 [TESTING.md](TESTING.md)를 따릅니다.
- `./gradlew test`로 실행합니다. MySQL·Redis 통합 테스트는 Docker가 필요하며, 없으면 건너뜁니다.

## 7) 빠른 확인 체크리스트
- Java 17 설치
- MySQL/Redis 준비 (`docker compose --profile local-db up -d mysql redis`)
- 설정 파일 생성과 환경 변수 설정
- `./gradlew bootRun` 또는 IDE 실행
