# 모듈별 상세 문서

이 문서는 `cofshop` 백엔드의 주요 모듈을 패키지 기준으로 요약하고, 각 모듈의 책임과 주요 흐름을 설명합니다.

## 공통/인프라
- config: 보안, Swagger, Redis/Stream, Elasticsearch, WebSocket, QueryDSL, CORS, PortOne 설정
- global: 공통 응답 포맷(ApiResponse)과 예외 처리(GlobalExceptionHandler)
- aspect: 분산락/로깅/트랜잭션 보조 AOP
- argumentResolver: 로그인 사용자 주입과 분산락 키 파싱
- file: 업로드 파일 메타데이터/저장소 추상화

## 회원(member)
- 책임: 회원 가입/로그인, 상태(활성/탈퇴/정지) 관리, 멤버십 정책 적용
- 주요 구성: `domain`, `service`, `controller`, `policy`, `event` 패키지
- 흐름 요약: 가입/로그인 -> 상태 검증 -> 등급/권한 적용

## 상품(item)
- 책임: 상품/옵션/이미지/카테고리/리뷰 관리, 검색
- 주요 구성: 엔티티(`Item`, `ItemOption`, `ItemImg`, `Category`, `Review`) + MapStruct 매퍼
- 검색: Elasticsearch 연동 레포지토리(`item/repository/elasticSearch`)
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
- 책임: 쿠폰 생성/발급/사용/만료, Redis 기반 발급 처리
- Redis 구조
  - 재고/중복/순서 제어: `coupon:stock:*`, `coupon:issued:*`, `coupon:issued:order:*`, `coupon:seq:*`
  - 발급 큐: Redis Stream `stream:events`
  - 컨슈머 그룹: `consumer-group:coupon`
- 흐름 요약: 발급 요청 -> Redis Stream 적재 -> 컨슈머 처리 -> DB 반영
- 보조 작업: Pending 메시지 정리/스케줄러 기반 만료 처리

## 결제(payment)
- 책임: 결제 요청/검증/환불, 외부 PG(PortOne/Iamport) 연동
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
2) `PaymentService.createPaymentRequest()`에서 주문 조회 후 `order.pay()` 처리  
3) `PaymentEntity` 저장 -> 프론트 결제 화면으로 응답  
4) 결제 완료 후 검증 호출 -> `verifyPayment()`  
5) Iamport API 검증 후 금액/merchantUid 대조 -> 결제 상태 확정  
6) 환불 요청 시 `refundPayment()`에서 상태 확인 + Iamport 취소 호출  

### 쿠폰(coupon) 플로우 (Limited 쿠폰 발급)
1) 발급 요청 -> `MemberCouponRedisService.enqueueLimitedCouponIssueRequest()`  
2) Redis Stream `stream:events`에 메시지 적재  
3) `RedisStreamConfig`의 컨슈머 그룹이 메시지 수신  
4) `CouponStreamConsumer.onMessage()`에서 `CouponRedisService.issueCoupon()` 실행  
5) Lua 스크립트로 재고/중복/순서 제어 후 결과 반환  
6) 성공 시 ACK 처리, 실패 시 pending으로 남아 `PendingMessageCleaner`가 재처리  

### 채팅(chat) 플로우 (STOMP)
1) 클라이언트가 `/ws-chat` 연결  
2) 메시지 발행 -> `/app/chat.sendMessage`  
3) `ChatMessageController.handleMessage()`에서 저장 후 브로드캐스트  
4) 구독 채널 `/topic/chatroom.{roomId}`로 전파  
5) 삭제도 동일 경로(`/app/chat.deleteMessage`)로 처리  

---

필요 시 단계별 상태 전이/예외 처리까지 확장 가능합니다.
