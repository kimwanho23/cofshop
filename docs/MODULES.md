# 모듈별 상세 문서

이 문서는 `cofshop` 백엔드의 주요 모듈을 패키지 기준으로 요약하고, 각 모듈의 책임과 주요 흐름을 설명합니다.

## 공통/인프라
- config: 보안, Swagger, Redis, WebSocket, QueryDSL, CORS, PortOne 설정
- global: 공통 애노테이션/예외/에러코드/공통 도메인 타입
- aspect: 분산락/로깅/트랜잭션 보조 AOP
- argumentResolver: 로그인 사용자 주입과 분산락 키 파싱
- file: 업로드 파일 메타데이터/저장소 추상화

## Spring Modulith 경계(현재)
- 루트 모듈은 `cart`, `chat`, `coupon`, `file`, `global`, `item`, `member`, `order`, `payment`, `security`, `statistics` 총 11개이며 모두 `CLOSED`로 선언
- `allowedDependencies`를 통해 모듈 간 허용 의존만 명시
- 공개 `NamedInterface`
  - `member::{api,domain,event,dtoRequest,dtoResponse}`
  - `item::{api,domain}`
  - `order::{api,domain}`
  - `coupon::api`
  - `file::api` (패키지: `file.domain`)
  - `global::{annotation,domain,exception,errorcodes}`
  - `security::{userdetails,token}`
- `payment`/`statistics`는 `order::api`로만 주문 영역에 접근
- `cart`/`chat`/`coupon`/`item`/`security`/`order`의 회원 접근은 `member::api` 포트(`MemberReadPort`, `MemberPointPort`)를 사용
- `cart`/`order`의 상품 조회는 `item::api(ItemReadPort)`를 통해 수행
- `item`은 `file::api`만 참조해 파일 모듈 의존 범위를 최소화
- 아키텍처 스모크 테스트(`ModularitySmokeTest`)에서 모듈 목록, `CLOSED` 여부, 핵심 의존 관계를 검증

## 회원(member)
- 책임: 회원 가입/로그인, 상태(활성/탈퇴/정지) 관리, 멤버십 정책 적용
- 주요 구성: `domain`, `service`, `controller`, `policy`, `event` 패키지
- 흐름 요약: 가입/로그인 -> 상태 검증 -> 등급/권한 적용

## 상품(item)
- 책임: 상품/옵션/이미지/카테고리/리뷰 관리, 검색
- 주요 구성: 엔티티(`Item`, `ItemOption`, `ItemImg`, `Category`, `Review`) + MapStruct 매퍼
- 검색: 현재는 DB 기반 조회 흐름으로 단순화
- 흐름 요약: 등록/수정/삭제, 카테고리 트리 구성, 상품 검색/리뷰 관리

## 주문(order)
- 책임: 주문 생성/취소/확정, 배송 정책 적용
- 주요 구성: `Order`, `OrderItem`, `Address` 엔티티와 `DeliveryFeePolicy`
- 흐름 요약: 주문 생성 -> 상태 변경 -> 결제 연계/취소 처리

## 장바구니(cart)
- 책임: 장바구니 생성/조회/수정/삭제, 품목 수량/금액 계산
- 주요 구성: `Cart`, `CartItem` 엔티티 + 매퍼/레포지토리
- 흐름 요약: 품목 추가/수정 -> 총합 계산 -> 주문 전송

## 쿠폰(coupon)
- 책임: 쿠폰 생성/발급/사용/만료
- 패키지 구조: `api`, `controller`, `service`, `domain`, `repository`, `dto`, `mapper` 중심으로 단순화
- 부가 구성: 비동기/운영성 코드는 `messaging`, `scheduler`, `repository.outbox`, `service.outbox`로 분리
- 발급: 조건부 UPDATE로 수량 차감, 유니크 제약으로 1인 1장. 전환 배경은 [coupon-messaging-decision.md](architecture/coupon-messaging-decision.md) 참고
- 이벤트: outbox에 저장 후 스케줄러가 발행 (RabbitMQ 또는 로그)
- 보조 작업: 스케줄러 기반 만료 처리

## 결제(payment)
- 책임: 결제 요청/검증/환불, 외부 PG(PortOne V2) 연동
- 주요 구성: `PaymentEntity`, `PaymentService`, `PaymentController`
- 동시성: `@DistributedLock`으로 결제/환불 동시 처리 제어
- 흐름 요약: 주문 상태 변경 -> 결제 준비 -> PG 검증 -> 결제 상태 반영/환불

## 채팅(chat)
- 책임: 고객센터 1:1 채팅, 메시지 송수신
- WebSocket/STOMP
  - 연결 엔드포인트: `/ws-chat`
  - 브로드캐스트 토픽: `/topic/chatroom.{roomId}`
  - 애플리케이션 prefix: `/app`
- 흐름 요약: 채팅방 생성 -> 메시지 발행/삭제 -> 실시간 구독 전파

## 통계(statistics)
- 책임: 매출/인기 상품 통계 집계
- 구성: 스케줄러와 커스텀 레포지토리 기반 집계
- 흐름 요약: 주기적 집계 -> API 제공

## 보안(security)
- 책임: JWT 발급/검증, 로그인/로그아웃 필터, 인가 처리
- 구성: `JwtFilter`, `JwtTokenProvider`, `CustomUserDetails*`, `SecurityConfig`

---

## 상세 플로우

### 결제(payment) 플로우 (요약 시퀀스)
1) 클라이언트가 결제 준비 요청 -> `PaymentController`  
2) `PaymentService.createPaymentRequest()`에서 `order.api.OrderPaymentPreparePort.prepare()` 호출  
3) `PaymentEntity` 저장 -> 프론트 결제 화면으로 응답  
4) 결제 완료 후 검증 호출 -> `verifyPayment()`  
5) PortOne API 검증 후 금액/merchantUid 대조 -> 결제 상태 확정  
6) 환불 요청 시 `refundPayment()`에서 상태 확인 + PortOne 취소 호출  

### 쿠폰(coupon) 플로우 (Limited 쿠폰 발급)
1) 발급 요청 수신  
2) 쿠폰 상태, 기간, 중복 검증  
3) 조건부 UPDATE로 수량 차감 (수량이 없으면 실패)  
4) 회원 쿠폰과 outbox 이벤트를 같은 트랜잭션으로 저장  
5) 스케줄러가 outbox 이벤트 발행, 실패 시 재시도  

### 채팅(chat) 플로우 (STOMP)
1) 클라이언트가 `/ws-chat` 연결  
2) 메시지 발행 -> `/app/chat.sendMessage`  
3) `ChatMessageController.handleMessage()`에서 저장 후 브로드캐스트  
4) 구독 채널 `/topic/chatroom.{roomId}`로 전파  
5) 삭제도 동일 경로(`/app/chat.deleteMessage`)로 처리  

---

필요 시 단계별 상태 전이/예외 처리까지 확장 가능합니다.
