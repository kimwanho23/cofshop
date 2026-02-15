# 프로젝트 개요 (cofshop)

이 문서는 `cofshop` 백엔드 프로젝트의 대략적인 구조와 기술 스택을 요약합니다. (Spring Boot 기반)

## 1) 한눈에 보는 구성
- 목적: 커피/쇼핑몰 도메인의 REST API 서버
- 주요 기능: 회원/상품/장바구니/주문/쿠폰/리뷰/결제/통계/채팅
- 문서: Swagger UI 제공 (springdoc 설정)
- 보조 자료: DB EER 다이어그램, JMeter 시나리오, 구성도 이미지 등 (`정보/` 폴더)

## 2) 기술 스택
- Language: Java 17
- Framework: Spring Boot 3.4.0
- Build: Gradle
- Web: Spring MVC, Thymeleaf(결제 템플릿), WebSocket
- Security: Spring Security + JWT
- Data: Spring Data JPA, QueryDSL, MapStruct
- DB: MySQL (runtime), H2 (test)
- Cache/Queue: Redis, Redisson (분산락)
- Search: Elasticsearch
- API Docs: springdoc-openapi (Swagger UI)
- Payment: iamport-rest-client-java (PortOne/아임포트 계열)
- Test: JUnit, Spring Test, Spring REST Docs

## 3) 프로젝트 디렉터리 구조 (요약)
- `src/main/java/kwh/cofshop`
  - `config/`: 보안/Swagger/Redis/Elasticsearch/WebSocket 등 설정
  - `security/`: JWT, 인증/인가 필터, 로그인/로그아웃 처리
  - `global/`: 공통 응답 포맷, 예외 처리, 공통 엔티티
  - `member/`: 회원 도메인 (엔티티/서비스/컨트롤러/정책)
  - `item/`: 상품/카테고리/리뷰/이미지/옵션
  - `order/`: 주문/주문아이템/배송정책
  - `cart/`: 장바구니
  - `coupon/`: 쿠폰 발급/사용, Redis Stream 기반 작업자
  - `payment/`: 결제 요청/검증/환불
  - `chat/`: 고객센터 1:1 채팅
  - `statistics/`: 매출/인기상품 통계
  - `aspect/`: AOP, 분산락, 로깅
  - `argumentResolver/`: 커스텀 인자 리졸버
  - `file/`: 파일 업로드 도메인
- `src/main/resources/`
  - `application.properties`, `application-dev.properties`, `application-prod.properties`, `application-key.properties`
  - `templates/payInicis.html`
  - `static/favicon.ico`
- `Dockerfile`: 컨테이너 빌드용
- `README.md`: 기능 및 API 문서(README 인코딩 이슈 가능)
- `정보/`: 구성도, EER, JMeter 시나리오/리포트, 기타 문서

## 4) 주요 도메인/모듈 설명
- 회원(Member): 가입/로그인/상태관리/권한
- 상품(Item): 상품/옵션/이미지/카테고리/리뷰
- 주문(Order): 주문 생성/취소/상태 변경
- 장바구니(Cart): 품목 추가/수정/삭제
- 쿠폰(Coupon): 발급/사용/만료 처리, Redis Stream 소비자/스케줄러
- 결제(Payment): PortOne(아임포트) 연동, 결제 검증/환불
- 채팅(Chat): WebSocket 기반 1:1 채팅
- 통계(Statistics): 기간/일별 매출, 인기 상품 집계

## 5) 설정 및 실행 관련 포인트
- 프로파일
  - 기본: `application.properties`
  - 개발: `application-dev.properties`
  - 운영: `application-prod.properties`
  - 키: `application-key.properties` (민감정보 포함)
- DB
  - MySQL 연결 정보는 `application*.properties`에 정의됨
  - `spring.jpa.hibernate.ddl-auto=update` (dev/prod)
- Redis
  - `spring.redis.host`, `spring.redis.port`
- 파일 업로드 경로
  - `file.dir=C:/cof/image`

## 6) 테스트
- `src/test/java/kwh/cofshop` 아래에 도메인/컨트롤러/서비스 테스트 존재
- REST Docs, MockMvc 테스트 구조 일부 존재

## 7) 주의 사항 (보안)
- `application.properties` 및 `application-key.properties`에 DB 비밀번호/JWT 시크릿/IMP 키가 포함되어 있음
  - 실제 배포/공유 전에는 환경 변수 또는 별도 Secret 관리로 분리 권장

## 8) 빠른 확인 체크리스트
- Java 17 설치
- MySQL/Redis 준비
- 필요한 환경변수 설정 (prod 기준)
- `./gradlew bootRun` 또는 IDE 실행

---

필요하면 모듈별 상세 문서(예: 결제 플로우, 쿠폰 발급 정책, 채팅 프로토콜)를 추가로 정리할 수 있습니다.
