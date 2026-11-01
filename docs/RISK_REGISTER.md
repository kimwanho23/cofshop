# 위험 등록부

서비스 운영에 영향을 줄 수 있는 위험을 추적하는 문서입니다. 해결 순서는 [ROADMAP.md](ROADMAP.md)를 따릅니다.

- 최초 점검일: 2026-10-31
- 점검 방식: 코드 읽기. 별도 표시가 없으면 실제 환경에서 재현하지 않았습니다.

## 운영 규칙

- **등급**
  - 높음: 금전 손실, 재고·결제 정합성 훼손, 권한 우회
  - 보통: 특정 조건에서 잘못된 동작이 생기거나 운영 부담이 큼
  - 낮음: 영향이 제한적이거나 다른 안전장치가 있음
- **상태:** `미해결` → `진행 중` → `해결` 또는 `수용`
- **해결 조건:** 회귀 테스트나 검증 결과를 함께 남깁니다.
- **수용 조건:** 수용한 이유와 결정한 사람, 날짜를 적습니다.
- **코드 위치:** 줄 번호는 쉽게 바뀌므로 클래스와 메서드로 적습니다.

## 요약

| ID | 위험 | 등급 | 상태 | 단계 |
| --- | --- | --- | --- | --- |
| R-01 | 결제 확인 누락 시 돈만 받고 주문 취소 | 높음 | 해결 | 3 |
| R-02 | 채팅 도청·메시지 위조 | 높음 | 해결 | 1 |
| R-03 | 재고 덮어쓰기로 초과 판매 | 높음 | 해결 | 1 |
| R-04 | 환불 복구 큐 무한 증가 | 높음 | 해결 | 1 |
| R-05 | PG 환불 후 DB가 결제 완료로 복귀 | 높음 | 해결 | 3 |
| R-06 | 옵션 할인율이 주문 금액에 미반영 | 보통 | 해결 | 1 |
| R-07 | 0원 주문 완료 경로 없음 | 보통 | 해결 | 1 |
| R-08 | 승인된 환불 실행을 회원에게 의존 | 보통 | 해결 | 1 |
| R-09 | 미구매자 리뷰 작성 가능 | 보통 | 해결 | 1 |
| R-10 | 판매 중지 미구현, 구매 제한 우회 | 보통 | 해결 | 1 |
| R-11 | DB 스키마 변경을 수동 관리 | 높음 | 해결 | 2 |
| R-12 | 메트릭·알림 없음 | 높음 | 해결 | 2 |
| R-13 | 의존성 취약점 잔존 | 보통 | 수용 | 0 |
| R-14 | 통합·동시성 테스트 부재 | 높음 | 해결 | 1 |
| R-15 | 배포·롤백 미검증 | 높음 | 미해결 | 4 |
| R-16 | 분산 락 유지 시간 초과 가능 | 낮음 | 해결 | 4 |
| R-17 | 결제 생성 중복 처리 catch 미동작 가능 | 낮음 | 해결 | 3 |
| R-18 | 미검토 영역 존재 | 보통 | 해결 | 4 |
| R-19 | `@DistributedLock` 메서드가 실제 앱에서 전부 실패 | 높음 | 해결 | 1 |
| R-20 | RabbitMQ 비활성 설정에서 앱 기동 실패 | 높음 | 해결 | 1 |
| R-21 | 설정 템플릿이 없어 새 클론에서 실행 불가 | 높음 | 해결 | 2 |
| R-22 | 클라이언트 오류와 권한 거부가 500으로 응답 | 보통 | 해결 | 2 |
| R-23 | 로그인 실패 응답으로 계정 상태 노출 | 보통 | 해결 | 2 |
| R-24 | 관리자 재고 수정이 그 사이의 주문 차감을 덮어씀 | 보통 | 해결 | 1 |
| R-25 | 이미지 업로드가 기본 설정에서 전부 실패 | 높음 | 해결 | 2 |
| R-26 | 상품 이름 검색이 전부 실패 | 높음 | 해결 | 2 |
| R-27 | 판매 통계가 전부 실패하고 일반 회원에게 열려 있음 | 높음 | 해결 | 2 |
| R-28 | 장바구니 합계 API가 전부 실패 | 보통 | 해결 | 2 |
| R-29 | 실패한 쿠폰 발급 요청이 기록 없이 사라짐 | 보통 | 해결 (경로 제거) | 2 |
| R-30 | 파일 업로드 검증 부족 | 보통 | 해결 | 2 |
| R-32 | 상품과 카테고리 API의 공개 범위 오류 | 보통 | 해결 | 2 |
| R-33 | 상품 이미지 삭제가 DB에 반영되지 않고 파일만 지워짐 | 높음 | 해결 | 2 |
| R-34 | 구매 확정 후 환불 요청이 막힘 | 높음 | 해결 | 2 |
| R-35 | 탈퇴해도 회원 개인정보가 남음 | 높음 | 해결 | 2 |
| R-38 | 탈퇴·정지·비밀번호 변경 후에도 액세스 토큰이 유효함 | 높음 | 해결 | 2 |
| R-39 | 의존성 취약점 점검 | 보통 | 해결 (일부 수용) | 2 |
| R-36 | 관리자 접속기록과 2차 인증 없음 | 보통 | 미해결 (실제 판매 전 필수) | 4 |
| R-37 | 탈퇴 후 재가입으로 쿠폰 중복 발급 제한 우회 | 보통 | 해결 (인증 없는 값은 한계) | 2 |
| R-31 | 규모가 커지면 다시 볼 항목 | 낮음 | 미해결 | 4 |

## 결제·주문

### R-01 결제 확인 누락 시 돈만 받고 주문 취소

- **위치:** `OrderService.autoCancelStaleUnpaidOrder`, `OrderService.validateCancelableByOrderApi`
- **상황:** 고객이 PG 결제 후 브라우저를 닫으면 `/verify`가 호출되지 않습니다. 30분 뒤 자동 취소가 DB의 `READY` 상태만 보고 주문을 취소합니다. 회원의 직접 취소도 같은 판단을 합니다.
- **영향:** 결제 금액이 환불되지 않은 채 주문만 취소됩니다.
- **근거:** 코드에 webhook 처리와 PG 대사 스케줄러가 없습니다.
- **해결:** `PaymentReconciliationScheduler`가 결제 요청 후 5분~24시간 지난 `READY` 결제를 1분마다 PG와 대조합니다. PG 결제 완료면 확정하고, 주문이 이미 취소됐으면 PG 취소까지 진행합니다. 자동 취소 전에도 PG를 확인하며, PG 조회가 실패하면 취소를 보류합니다.
- **검증:** `ApplicationIntegrationTest`(PG는 대체), `PaymentReconciliationSchedulerTest`, `OrderAutoCancelSchedulerTest`
- **남은 위험:** 조회·취소 형식은 PortOne 공식 OpenAPI 스키마와 대조했고(취소 응답 파싱 오류 수정), 2026-11-02에 KG이니시스 테스트 채널의 실제 결제 1건으로 대사와 취소를 확인했습니다(`PortOneLivePaymentFlowTest`). 다른 PG사와 실연동 채널은 확인하지 않았습니다. 24시간이 지난 뒤 결제된 건은 대사 대상이 아닙니다.

### R-03 재고 덮어쓰기로 초과 판매

- **위치:** `OrderItemService.restoreStock`, `OrderService.restoreOrderCompensation`, `ItemOptionRepository`
- **상황:** 차감은 비관적 락을 쓰지만 복구는 락 없이 읽은 값을 저장했습니다. 관리자 재고 수정은 원래 락을 사용하므로 해당하지 않습니다.
- **영향:** 재고 10을 읽은 취소 트랜잭션이 다른 주문의 차감(9)을 덮어쓰고 11을 저장합니다.
- **해결:** 복구도 옵션 락을 잡고 다시 읽어 반영합니다. 락은 ID 순서로 잡고, 취소 시 회원 → 옵션 순서로 맞췄습니다.
- **검증:** `OrderStockConcurrency*Test`. 수정을 되돌리면 H2 테스트가 재고 불일치로 실패합니다. MySQL 테스트는 Docker가 있는 환경에서만 실행됩니다.

### R-04 환불 복구 큐 무한 증가

- **위치:** `PaymentRefundTxService.confirmRefund`, `RefundCompensationRecoveryScheduler.recoverFailedCompensations`
- **상황:** 복구 실패 시 `confirmRefund`가 재시도 횟수 0인 작업을 넣고, 스케줄러도 횟수를 올린 작업을 넣습니다.
- **영향:** 실패가 계속되면 작업이 늘어나고, 횟수 0인 작업이 계속 생겨 DLQ로 넘어가지 않습니다.
- **해결:** 복구 경로는 적재하지 않는 `confirmRefundForRecovery`를 사용하고, 재시도 적재는 스케줄러만 담당합니다.
- **검증:** `PaymentRefundTxServiceTest`, `RefundCompensationRecoverySchedulerTest`

### R-05 PG 환불 후 DB가 결제 완료로 복귀

- **위치:** `PaymentService.executeRefundCancellation`
- **상황:** 취소 요청 타임아웃이나 취소 직후 상태 불일치 시 곧바로 `PAID`로 되돌립니다.
- **영향:** PG에서는 환불됐는데 DB는 결제 완료로 남습니다. 회원이 다시 요청해야 맞춰지며, 그동안 환불 승인 상태라 배송 진행은 막힙니다.
- **미확인:** PortOne의 취소 비동기 처리 여부. 타임아웃 경로는 이와 무관하게 발생합니다.
- **해결:** PG가 취소 실패를 명확히 응답한 경우에만 `PAID`로 되돌립니다. 타임아웃이나 처리 중이면 `REFUND_PENDING`을 유지하고 `ApprovedRefundRetryScheduler`가 PG 상태로 확정합니다.
- **검증:** `ApplicationIntegrationTest`(PG는 대체), `PaymentServiceTest`
- **남은 위험:** PG 장애가 길어지면 재시도와 `ApprovedRefundStuck` 알림이 계속됩니다. `/api/admin/payments`로 조회와 수동 재시도를 할 수 있지만 관리자 화면은 없습니다.

### R-06 옵션 할인율이 주문 금액에 미반영

- **위치:** `OrderItem`의 `@Builder` 생성자 2개, `OrderItem.createOrderItem`
- **상황:** 빌더의 `build()`가 `discountRate`와 `id`가 없는 생성자를 호출합니다. 바이트코드로 확인했습니다.
- **영향:** 장바구니 합계에는 옵션 할인이 적용되지만 주문 금액에는 적용되지 않습니다. 현재 옵션 할인율을 설정하는 서비스 코드를 찾지 못했으므로 DB에 직접 넣은 경우에만 드러납니다.
- **해결:** 생성자를 하나로 통합했습니다.
- **검증:** `OrderItemTest`

### R-07 0원 주문 완료 경로 없음

- **위치:** `OrderService.applyPoint`, `Order.finalizePrice`, `PaymentService.createPaymentRequest`
- **상황:** 전액 할인과 무료 배송이 겹치면 최종 금액이 0원이지만 PG를 거치지 않는 처리가 없습니다.
- **영향:** PG가 0원 결제를 거부하면 주문이 자동 취소됩니다. PortOne의 동작은 미확인입니다.
- **해결:** 최종 금액이 0원이면 주문 생성 시 바로 `PAID`로 처리합니다. 환불도 PG 없이 승인 시 바로 완료합니다.
- **검증:** `OrderServiceTest`

### R-08 승인된 환불 실행을 회원에게 의존

- **위치:** `PaymentController.refundPayment`, `OrderService.processRefundRequestByAdmin`
- **상황:** 관리자는 승인만 하고, 실행 API는 회원 전용입니다.
- **영향:** 회원이 호출하지 않으면 환불되지 않은 채 배송도 막힌 상태로 남습니다.
- **해결:** 승인 커밋 후 `OrderRefundApprovedEvent`로 환불을 비동기 실행합니다. 실패하거나 이벤트가 유실되면 `ApprovedRefundRetryScheduler`가 5분 간격으로 재시도합니다. 회원 환불 API는 유지합니다.
- **검증:** `ApprovedRefundExecutorTest`, `ApprovedRefundRetrySchedulerTest`, `OrderRefundApprovedEventListenerTest`, `OrderPolicyQueryTest`, `ApplicationIntegrationTest`(PG는 대체), `PortOneLivePaymentFlowTest`(실제 테스트 채널, 수동 실행).

### R-17 결제 생성 중복 처리 catch 미동작 가능

- **위치:** `PaymentService.createPaymentRequest`
- **상황:** `save` 실패 후 같은 트랜잭션에서 재조회합니다. 트랜잭션이 이미 롤백 표시되어 커밋 시 실패할 가능성이 높습니다.
- **영향:** 분산 락이 먼저 막아 주므로 실제 발생 빈도는 낮습니다.
- **해결:** catch를 제거했습니다. 위반 시 공통 처리기가 409로 응답하고, 재요청하면 기존 결제를 재사용합니다.

## 권한·정책

### R-02 채팅 도청·메시지 위조

- **위치:** `WebSocketAuthChannelInterceptor.preSend`, `authorizeSubscription`, `WebSocketConfig.configureMessageBroker`
- **상황:**
  - 구독 검사는 목적지가 `/topic/chatroom.`으로 시작할 때만 동작하므로 `/topic/*` 구독이 통과합니다.
  - `SEND`는 로그인만 확인하므로 `/topic/chatroom.5`로 직접 보낼 수 있습니다.
- **영향:** 로그인한 사용자가 모든 방을 엿보거나, 저장과 참여자 검사 없이 임의의 메시지를 전달할 수 있습니다.
- **해결:** `SEND`는 `/app/` 목적지만, `SUBSCRIBE`는 `/topic/chatroom.{숫자}`와 참여자만 허용합니다.
- **검증:** `WebSocketAuthChannelInterceptorTest` (인터셉터 단위 테스트이며 실제 브로커 연결은 하지 않았습니다.)

### R-09 미구매자 리뷰 작성 가능

- **위치:** `ReviewService.save`
- **영향:** 구매하지 않은 사용자가 평점을 조작할 수 있습니다.
- **해결:** 해당 상품을 구매 확정(`COMPLETED`)한 회원만 작성할 수 있습니다. 기존 리뷰는 그대로 둡니다.
- **검증:** `ReviewServiceTest`, `OrderPolicyQueryTest`

### R-10 판매 중지 미구현, 구매 제한 우회

- **위치:** `ItemOption.validatePurchasable`, `OrderItemService.createOrderItems`
- **상황:** 상품 상태를 판매 중지로 바꾸는 코드를 찾지 못했고 주문 시 상품 상태도 확인하지 않습니다. `itemLimit`은 주문 1건 기준입니다.
- **영향:** 상품 단위 판매 중단이 불가능하고, 주문을 나누면 구매 제한을 넘을 수 있습니다.
- **해결:** 판매자용 `PATCH /api/item/{itemId}/state`를 추가하고 주문 시 판매 상태를 검사합니다. `itemLimit`은 회원당 누적(취소·환불 제외, 기간 제한 없음)으로 검사합니다.
- **검증:** `OrderItemServiceTest`, `ItemServiceTest`, `ItemControllerTest`, `OrderPolicyQueryTest`

## 운영·품질

### R-11 DB 스키마 변경을 수동 관리

- **근거:** Flyway, Liquibase가 없고 `docs/sql`, `docs/db`에 스크립트가 흩어져 있습니다.
- **영향:** 운영 기본값이 `ddl-auto=validate`라서 적용을 빠뜨리면 앱이 기동되지 않고, 적용 이력도 남지 않습니다.
- **해결:** Flyway를 도입했습니다. V1은 엔티티 기준 Hibernate DDL이고, V2에서 스케줄러용 인덱스를 추가했습니다. 기존 `docs/sql`, `docs/db` 스크립트는 V1에 통합되어 제거했습니다.
- **검증:** `ApplicationIntegrationTest`가 빈 MySQL에 마이그레이션을 적용하고 `ddl-auto=validate`로 기동합니다.
- **주의:** 운영 데이터가 있는 DB에는 아직 적용해 보지 않았습니다.

### R-21 설정 템플릿이 없어 새 클론에서 실행 불가

- **상황:** `application*.properties`는 Git에서 제외되는데, 문서와 `init-local-config.sh`가 가리키는 `.template` 파일이 저장소에 없었습니다.
- **영향:** 새 클론이나 CI에서 빌드한 이미지에는 설정이 없어 앱을 실행할 수 없습니다.
- **해결:** 환경 변수 기반 템플릿 4개를 추가했습니다.
- **검증:** 깨끗한 복사본에서 `init-local-config.sh` 실행 후 `ApplicationIntegrationTest`로 확인합니다.

### R-23 로그인 실패 응답으로 계정 상태 노출

- **위치:** `CustomUserDetailsService`, `CustomAuthenticationProvider`, `CustomLoginFilter`
- **상황:** 비밀번호를 확인하기 전에 회원 상태를 검사해, 이메일만 알면 그 계정의 탈퇴·정지 여부를 알 수 있었습니다. 이 예외는 인증 예외가 아니어서 필터 밖으로 나가 서버 ERROR 로그도 남겼습니다.
- **근거:** `SecurityFlowIntegrationTest`로 실제 HTTP 요청에서 확인했습니다.
- **해결:** 상태는 비밀번호가 맞은 뒤에만 검사합니다. 비밀번호가 틀리거나 없는 계정이면 모두 `U-LOGIN`으로 응답합니다(`CustomAuthenticationFailureHandler`).

### R-24 관리자 재고 수정이 그 사이의 주문 차감을 덮어씀

- **위치:** `ItemOptionService.updateExistingItemOptions`, `ItemOption.updateOption`
- **상황:** 재고를 차이값이 아니라 절댓값으로 저장합니다. 관리자가 재고 100을 본 뒤 5건이 주문되고 다시 100으로 저장하면 차감 5건이 사라집니다. 락은 쓰기가 겹치는 것만 막습니다.
- **영향:** 상품 수정 화면을 저장할 때마다 초과 판매가 생길 수 있습니다.
- **해결:** 기존 옵션을 수정할 때 화면에서 읽은 재고(`expectedStock`)를 필수로 받습니다. 재고를 바꾸려는데 값이 달라졌으면 `ITEM-STOCK-CHANGED`(409)로 거부하고, 재고를 바꾸지 않는 수정은 현재 재고를 유지합니다.

### R-25 이미지 업로드가 기본 설정에서 전부 실패

- **위치:** `FileStore.getFullPath`
- **상황:** `file.dir` 기본값 `./uploads`가 상대 경로라, `transferTo`가 Tomcat 임시 폴더 기준으로 해석해 모든 업로드가 500으로 실패했습니다. 단위 테스트는 절대 경로와 모의 파일을 써서 드러나지 않았습니다.
- **해결:** 절대 경로로 변환합니다. 실제 HTTP 업로드와 이미지 제공을 테스트합니다.

### R-26 상품 이름 검색이 전부 실패

- **위치:** `ItemRepositoryImpl.nameCondition`
- **상황:** HQL이 해석하지 못하는 `MATCH ... AGAINST` 구문을 썼고 FULLTEXT 인덱스도 없어, 이름 검색은 항상 500이었습니다. 검색 결과에 상품 ID도 없어 상세로 이동할 수 없었습니다.
- **해결:** 대소문자 무시 부분 일치(`LIKE`)로 바꾸고 응답에 ID를 추가했습니다. 전체 스캔 비용은 R-31에서 관리합니다.

### R-27 판매 통계가 전부 실패하고 일반 회원에게 열려 있음

- **위치:** `OrderStatisticsPortAdapter`, `StatisticsController`
- **상황:** `DATE()` 결과가 `LocalDate`로 변환되지 않아 일별·기간별 통계가 500이었습니다. 복구되면 로그인한 회원 누구나 매출을 볼 수 있는 상태였고, 기간 제한도 없었습니다.
- **해결:** 변환을 고치고 매출 API를 관리자 전용으로 바꾸고 기간을 366일로 제한했습니다. 인기 상품은 회원에게 계속 제공합니다.

### R-28 장바구니 합계 API가 전부 실패

- **위치:** `CartItemRepositoryImpl.sumTotalPriceByMemberId`
- **상황:** SQL 나눗셈 결과가 `Double`로 나와 `ClassCastException`이 발생했습니다.
- **해결:** 주문 금액과 같은 도메인 가격 계산을 사용합니다.

### R-29 실패한 쿠폰 발급 요청이 기록 없이 사라짐

- **해결:** 해당 Redis Stream 경로를 제거했습니다. 쿠폰 발급은 요청 안에서 성공 또는 실패를 응답합니다. 배경은 [coupon-messaging-decision.md](architecture/coupon-messaging-decision.md)를 참고합니다.

### R-30 파일 업로드 검증 부족

- **위치:** `FileStore`, `FileController`, `GlobalExceptionHandler`
- **상황:** 확장자와 클라이언트가 보낸 Content-Type만 확인했고, 여러 파일 중 하나가 실패하면 앞선 파일이 공개 경로에 남았습니다. 임시 업로드는 모든 회원에게 열려 있었고, 크기 초과는 500으로 응답했습니다.
- **해결:** 파일 시그니처를 검사하고, 저장 전에 전부 검증하며, 실패 시 저장분을 지웁니다. 임시 업로드는 관리자 전용으로, 크기 초과는 413으로 바꿨습니다.

### R-32 상품과 카테고리 API의 공개 범위 오류

- **위치:** `SecurityConfig`, `ItemResponseDto`, `StatisticsController`, `CategoryService`
- **상황:** 비로그인 고객은 검색만 되고 상품 상세, 인기 상품, 카테고리를 볼 수 없었습니다. 반대로 상품 응답에 등록한 관리자의 이메일이, 로그인 회원용 인기 상품 통계에 품목별 매출이 포함되어 있었습니다. 하위가 있는 카테고리를 지우면 DB에서 하위까지 함께 삭제되었습니다.
- **해결:** 상품과 카테고리 조회를 공개하고, 응답에서 이메일을 뺐고, `last-7days` 통계를 관리자 전용으로 바꿨습니다. 하위 카테고리나 상품이 있으면 삭제를 409로 거부합니다.
- **검증:** `ShopFlowIntegrationTest`로 주문, 결제, 배송, 환불, 쿠폰, 관리 API를 실제 HTTP에서 확인합니다.

### R-33 상품 이미지 삭제가 DB에 반영되지 않고 파일만 지워짐

- **위치:** `ItemImgService.deleteItemImages`
- **상황:** 삭제할 이미지를 상품의 컬렉션에서 빼지 않아, 커밋 때 cascade가 삭제를 취소했습니다. 저장소의 파일은 지워지므로 상품 화면에 깨진 이미지가 남습니다.
- **해결:** 컬렉션에서도 제거합니다. `ShopFlowIntegrationTest`로 실제 DB에서 확인합니다.

### R-34 구매 확정 후 환불 요청이 막힘

- **위치:** `Order.requestRefundRequest`, `OrderStatePortAdapter`
- **상황:** 구매 확정하거나 배송 7일 뒤 자동 확정되면 환불을 요청할 수 없었습니다. 상품이 광고·계약과 다르면 공급 후 3개월까지 철회할 수 있고(전자상거래법 제17조 제3항), 이보다 불리한 약정은 무효입니다(제35조).
- **해결:** 구매 확정 후에도 배송 완료 3개월까지 요청을 받고 관리자가 판단합니다. PG 취소가 실패하면 구매 확정 상태로 돌아갑니다.
- **남은 것:** 이미 집계된 일별 통계와 작성된 리뷰는 환불해도 바뀌지 않습니다. 배송 완료 시각은 관리자가 상태를 바꾼 시각이고 실제 수령일이 아닙니다.

### R-35 탈퇴해도 회원 개인정보가 남음

- **위치:** `MemberService.withdraw`, `OrderWithdrawalService`, `PaymentWithdrawalListener`
- **상황:** 탈퇴해도 상태만 `QUIT`로 바뀌고 이름, 이메일, 전화번호, 비밀번호 해시, 배송지가 그대로 남았습니다(개인정보 보호법 제21조).
- **해결:** 진행 중인 주문이 있으면 탈퇴를 거부합니다. 탈퇴하면 회원 정보를 지우고 포인트를 소멸시키며, 주문의 배송지와 결제의 구매자 정보를 지웁니다. 거래기록 보존에 필요한 값은 `order_personal_archive`에 분리 보관하고 주문일부터 5년이 지나면 삭제합니다(V7). 탈퇴 회원은 되돌릴 수 없고 같은 이메일과 전화번호로는 30일 동안 다시 가입할 수 없습니다(R-37).
- **남은 것:** 채팅 내용과 리뷰는 회원 ID와 함께 남습니다. 백업에 든 개인정보는 백업 보관 기간이 지나야 사라집니다.

### R-38 탈퇴·정지·비밀번호 변경 후에도 액세스 토큰이 유효함

- **위치:** `AccessTokenRevocationStore`, `JwtFilter`, `WebSocketAuthChannelInterceptor`
- **상황:** 액세스 토큰 수명이 1시간인데 요청마다 회원 상태를 확인하지 않아, 정지하거나 탈퇴해도 최대 1시간 동안 API를 쓸 수 있었습니다.
- **해결:** 무효화 시각을 Redis에 기록하고 그 전에 발급된 토큰을 HTTP와 WebSocket 연결에서 거부합니다. 비밀번호를 바꾸면 다시 로그인해야 합니다.
- **남은 것:** Redis 장애 중에는 모든 로그인 사용자를 막지 않도록 확인을 건너뛰므로 기존 토큰이 만료까지 유효합니다. 이미 열린 WebSocket 연결은 끊지 않습니다.

### R-39 의존성 취약점 점검

- **방법:** 2026-11-02에 런타임 의존성 194개를 OSV로 조회했습니다.
- **해결:** AWS SDK 자체에는 알려진 취약점이 없었습니다. 쓰지 않는 `apache5-client`가 함께 들어와 Boot가 낮춘 `httpclient5` 5.5.2, `httpcore5` 5.3.6(CVE-2026-64607, CVE-2026-54399, CVE-2026-54428)을 끌어왔으므로 제외했습니다. S3는 `url-connection-client`, PortOne은 JDK 클라이언트를 씁니다.
- **수용:** 수정본이 없고 해당 조건을 쓰지 않는 항목입니다.
  - `spring-webmvc` 6.2.19(CVE-2026-47890, CVE-2026-47884): SSE 뷰 프래그먼트와 `XsltView`를 쓰지 않습니다. Boot 3.5.16이 3.5 최신이라 근본 해결은 Boot 메이저 업그레이드입니다.
  - `querydsl-jpa` 5.0.0(CVE-2024-49203): 정렬 키를 `ItemRepositoryImpl.toOrderSpecifier`의 허용 목록으로만 처리해 사용자 입력이 쿼리에 들어가지 않습니다.
- **남은 것:** CI에 의존성 취약점 스캔이 없어 새 취약점은 다시 조회해야 알 수 있습니다.

### R-36 관리자 접속기록과 2차 인증 없음

- **상황:** 관리자의 회원 정보 조회 기록이 없고, 관리자도 비밀번호만으로 로그인합니다. 개인정보의 안전성 확보조치 기준 제5조, 제6조, 제8조 대상입니다.
- **조치:** 실제 고객 정보를 받기 전에 조회 기록, 권한 변경 기록, 2차 인증 또는 VPN을 적용합니다. 현재는 개인 프로젝트이고 실제 판매 계획이 없어 보류합니다.

### R-37 탈퇴 후 재가입으로 쿠폰 중복 발급 제한 우회

- **위치:** `MemberService.withdraw`, `member_coupon`의 `(member_id, coupon_id)` 유니크 제약
- **상황:** 탈퇴하면 이메일을 지우므로 같은 이메일로 다시 가입할 수 있고, 새 회원 ID로는 같은 쿠폰을 다시 받을 수 있습니다.
- **해결:** 탈퇴하면 이메일과 전화번호의 HMAC 해시를 `member_rejoin_block`에 30일 보관하고(`member.rejoin-block.days`, V8), 그동안 같은 값의 가입을 거부합니다. 원문은 저장하지 않으며 기간이 지나면 매일 삭제합니다. 비밀키는 `member.rejoin-block.hash-secret`이고 없으면 JWT 키를 씁니다. 키를 바꾸면 기존 제한은 풀립니다.
- **한계:** 이메일과 전화번호를 인증하지 않으므로 다른 값으로 가입하면 막을 수 없고, 타인의 번호로 가입 후 탈퇴하거나 가족이 같은 번호를 쓰면 정상 가입이 최대 30일 막힐 수 있습니다. 근본 해결은 휴대폰 본인 인증입니다. 실제 판매 전에 도입 여부를 정합니다.
- **처리방침:** 해시의 수집 목적과 30일 보관을 적습니다.

### R-31 규모가 커지면 다시 볼 항목

남은 항목:

- **쿠폰 발급 부하:** 요청마다 쿠폰 행을 잠그므로 선착순에서 병목이 될 수 있습니다. 부하 테스트로 확인합니다.
- **상품 검색:** `LIKE` 전체 스캔입니다. 부하 테스트에서 느리면 ngram FULLTEXT 인덱스로 바꿉니다.
- **쿠폰 outbox 발행:** 트랜잭션 안에서 발행하므로 커밋이 실패하면 같은 메시지가 다시 나갈 수 있습니다. 소비자를 만들 때 메시지의 outbox ID로 중복을 거릅니다. 다중 인스턴스 중복 발행은 스케줄러 락으로 막았습니다.

정리한 항목:

- **RabbitMQ 소비자:** 소비자는 없지만 유지합니다. Redis Stream에서 옮겨 온 발행 경로이고 기본값이 꺼짐이며 `CouponOutboxRabbitIntegrationTest`로 확인합니다. 소비자는 이벤트를 쓰는 기능이 생길 때 추가합니다.
- **쿠폰 outbox 정리:** 발행 후 7일이 지난 `SENT` 행을 매일 1000건씩 나눠 지웁니다(`coupon.outbox.retention-days`). `FAILED` 행은 원인 확인을 위해 남깁니다.
- **로그인 이력:** 최근 100건만 응답하고 3개월이 지나면 매일 삭제합니다(`member.login-history.retention-months`, V6). 3개월은 통신비밀보호법 시행령 제41조의 기간을 따른 운영 기준이고, 쇼핑몰이 그 대상인지는 확정하지 못했습니다. 처리방침에 적습니다.
- **인기 상품 캐시:** 이전 기록의 Redis 캐시는 현재 코드에 없습니다. 요청마다 전체 주문 상품을 집계하던 것을 인스턴스별 메모리에 순위만 60초 보관하도록 바꿨습니다(`item.popular.cache-seconds`). 상품 정보는 매번 새로 읽으므로 가격과 판매 상태는 바로 반영되고, 순위만 최대 60초 늦습니다.

### R-22 클라이언트 오류와 권한 거부가 500으로 응답

- **위치:** `GlobalExceptionHandler`
- **상황:** `Exception` 처리기가 스프링 MVC 예외를 모두 받아 없는 경로, 잘못된 메서드, 파라미터 오류를 500으로 응답했습니다. `@PreAuthorize` 거부도 컨트롤러 호출 중 발생해 403이 아니라 500이 됩니다.
- **영향:** 정상 요청 오류가 서버 장애로 기록되어 5xx 알림이 오동작합니다.
- **해결:** 해당 예외별로 404, 405, 406, 415, 400, 403으로 응답합니다.
- **검증:** `GlobalExceptionHandlerWebTest`, `ActuatorIntegrationTest`

### R-12 메트릭·알림 없음

- **근거:** `build.gradle`에 Actuator, Micrometer, Prometheus, Sentry가 없습니다.
- **영향:** 복구 큐 적체, 결제 불일치, 오류율 증가를 사용자 신고 전에 알기 어렵습니다.
- **해결:** Actuator, Prometheus 메트릭, 업무 메트릭, 알림 규칙, Discord 전송을 구성했습니다. [MONITORING.md](MONITORING.md)를 참고하세요.
- **검증:** Prometheus의 `AppDown` 발동부터 Discord 전송까지 실행해 전송 실패 0건을 확인했습니다. 업무 메트릭 규칙은 `promtool` 테스트로만 확인했습니다.

### R-13 의존성 취약점 잔존

- **상태:** Spring Boot 3.5.16 등으로 올려 OSV 기준 Critical이 15건에서 2건으로 줄었습니다. 2026-10-31 기준이며, 커밋 `9ffc3b8`로 `main`에 반영했고 CI가 통과했습니다.
- **잔존 항목:**
  - Spring MVC Critical 2건 (`XsltView`, SSE 프래그먼트). 6.2.19까지 영향을 받고 6.2.20은 Maven Central에 없으며, 공개 수정 버전은 7.0.9 이상입니다. 코드에 `XsltView`, `/**` 뷰 매핑, `FragmentsRendering`, SSE 응답이 없어 취약 조건에 해당하지 않습니다. `XsltView`, SSE, 뷰 프래그먼트를 도입하기 전에 Spring Boot 4 전환이 필요합니다.
  - QueryDSL High 2건 (`querydsl-jpa`, `querydsl-apt`의 `orderBy` HQL 인젝션, 수정 버전 없음). 전체 `orderBy`를 확인한 결과 고정 컬럼이거나 `ItemRepositoryImpl.toOrderSpecifier`의 허용 목록을 거칩니다. 사용자 입력을 경로로 만드는 코드를 추가하면 안 됩니다.
  - Artemis Moderate 1건 (`artemis-project`). Spring Boot BOM의 부모 POM으로만 참조되며 Artemis 라이브러리를 사용하지 않습니다.
- **수용:** 저장소 소유자(kimwanho23)가 2026-10-31에 남은 Dependabot 경고 5건(#7, #8, #19, #156, #157)을 dismiss했습니다. 이유는 위 잔존 항목의 취약 조건에 해당하지 않기 때문입니다.
- **재검토 조건:** `XsltView`, SSE, 뷰 프래그먼트 도입, 사용자 입력을 쿼리 경로로 만드는 코드 추가, `spring-boot-starter-artemis` 추가, Spring Boot 4 전환 시 다시 판단합니다.
### R-14 통합·동시성 테스트 부재

- **근거:** 테스트 클래스 76개 중 59개가 Mockito 기반이고, Testcontainers와 동시성 테스트가 없습니다.
- **영향:** 락, 트랜잭션, 쿼리 문제를 테스트로 발견할 수 없습니다. R-03이 이 경우입니다.
- **진행:** Testcontainers로 MySQL·Redis 동시성 테스트와 앱 전체 통합 테스트(`ApplicationIntegrationTest`)를 추가했습니다. 이 테스트로 R-19, R-20을 발견했습니다. `ConcurrencyIntegrationTest`(재고, 포인트, 쿠폰, 취소, 결제, 환불)와 `SecurityFlowIntegrationTest`(실제 HTTP의 로그인, 토큰, 권한)를 추가해 필수 시나리오를 채웠습니다. 이 과정에서 R-23, R-24를 발견했습니다.
- **남은 점:** 동시성 테스트는 PG만 대체하고 앱 인스턴스 1개로 실행합니다. 다중 인스턴스는 `MultiInstanceIntegrationTest`로 확인했고, 부하 조건은 스테이징에서 확인합니다.

### R-19 `@DistributedLock` 메서드가 실제 앱에서 전부 실패

- **위치:** `DistributedLockAspect`의 `@Order`
- **상황:** `@Order(HIGHEST_PRECEDENCE)`로 애스펙트가 `ExposeInvocationInterceptor`보다 먼저 실행되어 어노테이션 인자 바인딩이 실패했습니다. 주문 생성·취소, 결제, 환불 등 락을 쓰는 메서드가 모두 `IllegalStateException`으로 실패합니다.
- **근거:** 실제 스프링 컨텍스트 테스트에서 재현했습니다. mock 기반 테스트는 프록시를 거치지 않아 통과했습니다.
- **해결:** 순서를 `HIGHEST_PRECEDENCE + 100`으로 변경했습니다.
- **검증:** `DistributedLockIntegrationTest`(트랜잭션 커밋 후 해제 포함), `ApplicationIntegrationTest`

### R-20 RabbitMQ 비활성 설정에서 앱 기동 실패

- **위치:** `LoggingCouponIssueEventPublisher`
- **상황:** 스캔 대상에 `@ConditionalOnMissingBean`을 사용해 자기 자신이 조회되어 등록이 취소됐습니다. `coupon.rabbit.enabled=false`이면 발행기 빈이 없어 앱이 기동되지 않습니다.
- **해결:** `@ConditionalOnProperty`로 변경했습니다.
- **검증:** `ApplicationIntegrationTest`. RabbitMQ를 켠 설정의 기동은 검증하지 않았습니다.

### R-15 배포·롤백 미검증

- **근거:** CI의 deploy 잡이 `if: false`로 꺼져 있습니다.
- **영향:** 출시 또는 장애 시점에 절차 문제가 처음 드러납니다.
- **조치:** 스테이징 배포와 롤백을 리허설합니다.
- **순서:** 남은 작업 중 가장 마지막에 진행합니다 (결정).

### R-16 분산 락 유지 시간 초과 가능

- **위치:** `DistributedLock`(기본 30초 고정), `PaymentService.refundPayment`
- **상황:** 유지 시간을 지정해 자동 연장이 되지 않습니다. 환불은 PG 호출 3번(각 최대 8초)과 내부 락 대기를 합치면 30초를 넘을 수 있습니다.
- **영향:** 락이 먼저 풀려 같은 작업이 동시에 실행될 수 있습니다. DB 행 락과 상태 검사가 일부 막아 줍니다.
- **해결:** 기본값을 Redisson watchdog 자동 연장으로 바꾸고, 스케줄러의 5~10초 고정 유지 시간도 제거했습니다.

### R-18 미검토 영역 존재

- **범위:** 파일 업로드, 통계 집계, 상품 검색, 쿠폰 outbox·DLQ 워커, 로그인 이력을 점검했습니다.
- **결과:** R-25~R-30을 수정했고, 남은 항목은 R-31에 적었습니다. 쿼리는 `ShopFlowIntegrationTest`로 실제 HTTP와 MySQL에서 확인합니다.
