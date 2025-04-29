# Rest API 기반 쇼핑몰 프로젝트입니다.

Spring Boot를 기반으로 한 쇼핑몰 백엔드 서버를 구축하였으며, 모든 비즈니스 로직은 테스트 코드로 검증합니다.

API 통신은 Postman이나 MockMvc 객체를 활용하여 실제 URL 요청 시 반환되는 JSON 응답을 테스트하고 확인했습니다.

회원가입, 로그인, 상품 등록/수정, 장바구니, 주문, 리뷰 등 핵심 기능을 Restful API 형태로 제공합니다.  

또한 공통 응답 포맷, 예외 처리, 동적 검색(QueryDSL) 등의 비기능 요구사항도 충실히 반영하였습니다.


## 기능 목록
<details>
<summary><strong>회원 기능</strong></summary>

- 회원가입  
   - 사용자는 이메일, 비밀번호, 이름, 전화번호 등의 정보를 입력하여 회원 가입을 진행합니다.  
   - 회원 가입 시, 비밀번호는 암호화하여 저장됩니다.

- 로그인  
   - 사용자는 이메일과 비밀번호를 입력하여 로그인할 수 있습니다.  
   - 로그인 성공 시 JWT 토큰을 발급합니다.

- 회원 탈퇴  
   - 회원 탈퇴 시 실제 데이터 삭제 대신, MemberState(Enum)을 변경하여 Soft delete 처리합니다.  
   - 탈퇴한 회원은 로그인 및 서비스 이용이 불가능하며, 데이터는 보존됩니다. (추후 삭제 정책에 따라 완전 삭제 가능)

- 회원 상태 변경  
   - 관리자는 특정 회원의 상태(활성, 휴면, 탈퇴 등)를 변경할 수 있습니다.

</details>

</details>

<details>
<summary><strong>상품 기능</strong></summary>
   
- 상품 등록 / 수정 / 삭제  
   - 상품 정보를 등록하거나 수정, 삭제할 수 있습니다.

- 상품 검색  
   - 상품명 기반 검색 기능을 제공합니다.  
   - 가격대, 카테고리, 평점 필터링을 지원합니다.

- 상품 조회  
   - 전체 상품 목록을 페이징 방식으로 조회할 수 있습니다.

- 카테고리 등록  
   - 관리자가 상품 분류를 위한 카테고리를 생성할 수 있습니다.  
   - 카테고리는 계층 구조를 가질 수 있습니다.

- 전체 카테고리 조회  
   - 사용자 및 관리자가 전체 카테고리 목록을 조회할 수 있습니다.  

</details>

<details>
<summary><strong>주문 기능</strong></summary>

- 주문 생성  
   - 사용자는 상품을 선택하여 주문을 생성할 수 있습니다.  
   - 주문은 쿠폰, 회원 포인트, 상품 할인 등 할인가를 적용해서 생성할 수 있습니다.

- 주문 상태 변경  
   - 주문 생성 후, 관리자는 주문 상태(결제 완료, 배송 중, 배송 완료 등)를 변경할 수 있습니다.

- 주문 취소  
   - 사용자는 주문을 취소할 수 있습니다.

- 하나의 주문 상품 정보 조회  
   - 특정 주문에 대한 상세 정보를 조회할 수 있습니다. (상품명, 수량, 가격 등)

- 한 사람의 전체 주문 조회  
   - 로그인한 사용자는 본인의 모든 주문 내역을 조회할 수 있습니다.

- 전체 주문 조회 (관리자)  
   - 관리자가 전체 회원의 주문 내역을 조회할 수 있습니다.

</details>

<details><summary><strong>장바구니 기능</strong></summary>
   
- 장바구니에 상품 추가  
   - 사용자는 원하는 상품을 장바구니에 담을 수 있습니다.  
   - 상품 옵션과 수량을 함께 선택할 수 있습니다.

- 장바구니 상품 수량 수정  
   - 장바구니에 담긴 상품의 수량을 변경할 수 있습니다.

- 장바구니 상품 삭제  
   - 사용자는 장바구니에 담긴 상품을 삭제할 수 있습니다.

- 장바구니 목록 조회  
   - 현재 장바구니에 담긴 모든 상품을 조회할 수 있습니다.

- 장바구니 비우기  
   - 사용자는 장바구니에 담긴 모든 상품을 한 번에 삭제할 수 있습니다.
</details>


<details>
<summary><strong>상품 리뷰 기능</strong></summary>

- 상품 리뷰 작성  
   - 사용자는 상품에 대해 리뷰를 작성할 수 있습니다.  
   - 리뷰는 별점(평점)과 텍스트(내용)를 함께 작성할 수 있습니다.

- 상품 리뷰 수정  
   - 작성한 리뷰를 수정할 수 있습니다.  
   - 별점과 텍스트 내용을 모두 수정할 수 있습니다.

- 상품 리뷰 삭제  
   - 작성한 리뷰를 삭제할 수 있습니다.  

- 상품 리뷰 조회  
   - 상품별로 작성된 모든 리뷰를 조회할 수 있습니다.  
   - 리뷰 목록은 최신순, 평점순 등으로 정렬할 수 있습니다.

</details>

<details>
<summary><strong>쿠폰 기능</strong></summary>

- 쿠폰 발급  
   - 관리자는 쿠폰을 생성하고, 사용자가 직접 쿠폰을 발급받을 수 있습니다.
   - 쿠폰은 고정 할인 쿠폰, % 할인 쿠폰을 정책별로 관리합니다.

- 쿠폰 사용  
   - 주문 시 보유한 쿠폰을 선택하여 할인 혜택을 적용할 수 있습니다.  
   - 쿠폰 조건(최소 주문 금액, 유효기간 등)에 따라 사용 여부가 결정됩니다.

- 쿠폰 조회  
   - 사용자는 자신이 보유한 쿠폰 목록을 조회할 수 있습니다.  
   - 쿠폰 사용 여부(미사용, 사용 완료, 만료)도 함께 표시됩니다.

</details>


## 비기능

<details><summary><strong>비기능 (Non-Functional Features)</strong></summary>

- 공통 응답 포맷 적용 (ResponseEntity + ApiResponse)  
   - 모든 API는 `code`, `message`, `data`를 포함하는 일관된 응답 구조를 반환합니다.  
   - 클라이언트는 일관된 구조의 응답을 받아 통신할 수 있습니다.

- 체계적인 예외 처리 (Error 인터페이스 + Enum 코드 세분화)  
   - 비즈니스 예외를 구분하여 관리하는 Enum 클래스를 정의하여 예외를 세분화합니다.  
   - 클라이언트는 에러 코드와 메시지를 통해 정확한 오류 원인을 확인할 수 있습니다.

- Spring Security + JWT를 이용한 인증/인가 구현  
   - 로그인 시 Access Token, Refresh Token을 발급하고, 토큰을 통해 인증/인가를 수행합니다.  

- 스케줄러(Scheduler)를 통한 자동 작업 실행  
   - 특정 시간대에 동작하는 서비스 코드를 스케줄링하여 실행합니다.  
   - 예를 들어 쿠폰 만료 처리 및 리뷰 시스템의 정합성 보장을 위해서 실행됩니다.

- 데이터베이스 락을 이용한 동시성 제어  
   - 특정 기능에서 DB Level에서 비관적 락(Pessimistic Lock 등)을 적용합니다.  
   - 재고 초과 주문이나 중복 주문을 방지합니다.

- QueryDSL을 이용한 동적 검색 쿼리 작성  
   - 검색 조건(상품명, 가격, 카테고리 등)에 따라 동적으로 SQL 쿼리를 생성합니다.  
   - JPA Native Query의 가독성이 떨어진다고 판단하여 QueryDSL을 선택했습니다.

- MapStruct를 통한 DTO ↔ Entity 매핑 자동화  
   - DTO와 Entity 간의 매핑을 MapStruct 라이브러리로 관리합니다.
   - DTO를 이용하여 통신하여 순환 참조를 방지합니다.

- 테스트 코드 작성  
   - 테스트 코드를 작성하여 로직을 검증합니다.

</details>


## EER 다이어그램
![eer](https://github.com/user-attachments/assets/607f5d11-6356-44b3-8f71-1c52d53bead7)



## API 명세 (주요 기능)

👉 [SWAGGER HTML](https://kimwanho23.github.io/cofshop)
<details>
<summary><strong>API 표 보기</strong></summary>

**상품 (ItemController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 많이 팔린 상품 조회 | GET | /api/item/populars |
| 상품 단건 조회 | GET | /api/item/{itemId} |
| 상품 등록 | POST | /api/item |
| 상품 검색 | POST | /api/item/search |
| 상품 수정 | PUT | /api/item/{itemId} |
| 상품 삭제 | DELETE | /api/item/{itemId} |

**주문 (OrderController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 주문 상품 정보 조회 | GET | /api/orders/{orderId} |
| 내 주문 목록 조회 | GET | /api/orders/me |
| 전체 주문 목록 조회 | GET | /api/orders |
| 주문 생성 | POST | /api/orders |
| 주문 취소 | PATCH | /api/orders/{orderId}/cancel |
| 상품 구매 확정 | PATCH | /api/orders/{orderId}/confirm |

**회원 (MemberController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 회원 정보 조회 | GET | /api/members/{memberId} |
| 전체 회원 목록 조회 | GET | /api/members |
| 회원 가입 | POST | /api/members/signup |
| 포인트 변경 | PATCH | /api/members/{memberId}/point |
| 회원 상태 변경 | PATCH | /api/members/{memberId}/state |
| 회원 탈퇴 | PATCH | /api/members/me/state |
| 비밀번호 변경 | PATCH | /api/members/me/password |

**장바구니 (CartController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 장바구니 존재 여부 확인 | GET | /api/carts/me/exist |
| 장바구니 생성 | POST | /api/carts/me |
| 장바구니 삭제 | DELETE | /api/carts/me |

**장바구니 상품 (CartItemController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 장바구니 목록 조회 | GET | /api/carts/me |
| 장바구니 총 금액 계산 | GET | /api/cart-items/me/total-price |
| 장바구니 상품 추가 | POST | /api/carts/me/items |
| 다수 상품 추가 | POST | /api/cart-items/me/items/list |
| 장바구니 상품 수량 변경 | PATCH | /api/cart-items/me/quantity |
| 장바구니 개별 상품 삭제 | DELETE | /api/carts/me/items/{itemOptionId} |
| 장바구니 전체 상품 삭제 | DELETE | /api/carts/me/items |

**보안 (AuthController)**

| 기능 | Method | URL |
| --- | --- | --- |
| Refresh Token 재발급 | POST | /api/auth/reissue |

**리뷰 (ReviewController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 상품 리뷰 목록 조회 | GET | /api/reviews/items/{itemId} |
| 리뷰 등록 | POST | /api/reviews/items/{itemId} | <!-- reivews → reviews 오타 수정 -->
| 리뷰 수정 | PUT | /api/reviews/{reviewId} |
| 리뷰 삭제 | DELETE | /api/reviews/{reviewId} |

**쿠폰 (CouponController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 전체 쿠폰 조회 | GET | /api/coupon |
| 쿠폰 단건 조회 | GET | /api/coupon/{couponId} |
| 쿠폰 생성 | POST | /api/coupon |
| 쿠폰 만료 처리 (스케줄러 관리) | PATCH | /api/coupon/expire |
| 쿠폰 상태 변경 | PATCH | /api/coupon/{couponId}/state |
| 쿠폰 취소 | PATCH | /api/coupon/{couponId}/cancel |

**회원 쿠폰 (MemberCouponController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 내 쿠폰 목록 조회 | GET | /api/memberCoupon/me |
| 쿠폰 발급 | POST | /api/memberCoupon/me/{couponId} |
| 회원 쿠폰 만료 처리 (스케줄러 관리) | PATCH | /api/memberCoupon/expire |

**카테고리 (CategoryController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 자식 카테고리 목록 조회 | GET | /api/categories/{categoryId}/children |
| 카테고리 경로 조회 | GET | /api/categories/{categoryId}/path |
| 전체 카테고리 목록 조회 | GET | /api/categories |
| 카테고리 등록 | POST | /api/categories |
| 카테고리 삭제 | DELETE | /api/categories/{id} |

**채팅방 (ChatRoomController) - 고객센터 1:1 채팅 서비스**

| 기능 | Method | URL |
| --- | --- | --- |
| 채팅방 생성 | POST | /api/chat-rooms |
| 상담사 채팅방 배정 | PATCH | /api/chat-rooms/{roomId}/join |
| 채팅방 종료 | PATCH | /api/chat-rooms/{roomId}/close |

**채팅 메시지 (ChatMessageController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 채팅 메시지 목록 조회 | GET | /api/chat-messages/{roomId}/messages |

**통계 (StatisticsController)**

| 기능 | Method | URL |
| --- | --- | --- |
| 기간별 판매량 조회 | GET | /api/statistics/sales-between |
| 최근 7일 인기 상품 조회 | GET | /api/statistics/last-7days |
| 하루 판매량 조회 | GET | /api/statistics/daily-sales |

</details>

## 기술 스택
Language | Java 17

Framework | Spring Boot 3 / Spring Data JPA / Spring Security

Database |  MYSQL / QueryDSL

Test | JUnit / Postman

Build / Gradle

Docs / Swagger

## Trouble Shooting
<details><summary><strong>Trouble Shooting 펼쳐 보기</strong></summary>

## 이미지 저장

상품 업로드 시 추가 이미지를 함께 업로드할 수 있도록 구성했습니다.  
하나는 대표 이미지(썸네일)이며, 나머지는 쇼핑몰에서 흔히 볼 수 있는 상품 설명용 이미지들입니다.

초기에는 막연하게 DTO에 MultipartFile을 직접 포함시켰지만, 이는 잘못된 구조였습니다.  
DTO는 기본적으로 데이터 통신을 위한 객체이며, 프레임워크(SPRING)에 의존성을 가지지 않아야 합니다.  
이에 따라 DTO에는 이미지 파일 자체가 아닌, 이미지 이름이나 타입과 같은 메타데이터만 선언하도록 수정했습니다.

구조를 개선하면서 한 가지 고민이 생겼습니다.  
"서비스 단에서 업로드된 이미지가 어떤 상품에 매칭되는지, 순서만으로 구분할 수 있을까?" 하는 문제였습니다.  
단순 반복문만으로는 정확한 매칭이 어려울 수 있다고 판단하여,  
**Map** 구조를 도입하여 Key(이미지 정보)와 Value(실제 이미지 파일)를 매칭하는 방식으로 개선했습니다.

결과적으로 이미지 업로드 메서드는  
`Map<DTO(이미지 정보, DB에 저장할 데이터), IMAGE(실제 업로드할 파일)>`  
형태로 구성되어, 이미지 파일과 데이터의 일관성을 안전하게 유지할 수 있게 되었습니다.

---

### 핵심 정리

- 문제: DTO에 MultipartFile을 직접 담아 스프링 의존성이 발생
- 원인: DTO가 스프링에 종속되는 구조였음
- 해결:
  - DTO에는 파일 메타데이터만 선언
  - 서비스 단에서 파일 처리
  - Map<DTO, File> 구조로 명확한 매핑 구현

---

## 상품 수정

상품 수정 시 `OptimisticLockingFailureException`(버전 불일치) 오류가 발생했습니다.

상품은 이미지, 카테고리, 옵션 등 다양한 연관관계를 맺고 있으며,  
상품 ID를 기준으로 여러 테이블(상품, 이미지, 옵션, 카테고리)에서 수정 작업이 동시에 이루어져야 했습니다.

처음 수정 기능을 구현할 때는  
- 옵션은 삽입/삭제뿐만 아니라 기존 데이터의 변경 감지(update)가 필요했고,  
- 이미지는 실제 파일 업로드/삭제가 이루어져야 했습니다.  

이러한 다양한 요구사항을 하나의 메소드에서 처리하다 보니,  
특히 옵션 수정 과정에서 `OptimisticLockingFailureException` 이 발생했습니다.  
(옵션 수정 과정 중, 동일 엔티티에 대해 연속적인 변경이 이루어지면서 버전 충돌이 발생한 것으로 추정)

이를 해결하기 위해 다음과 같은 구조 개선을 적용했습니다.

1. 기존 옵션 수정 (`updateExist`)  
2. 신규 옵션 삽입 (`newOption`)  
3. 옵션 삭제 (`deleteOption`)  

**이 세 가지 작업을 각각 별도의 메소드로 분리하고**,  
최종적으로 `update` 메소드에서 이들을 호출하는 방식으로 변경했습니다.  

또한, 메소드 분리만으로 끝내지 않고,  
**전체 update 프로세스는 하나의 트랜잭션 안에서 실행되도록 구성**하여,  
다른 작업이 중간에 커밋된 후 롤백될 경우 발생할 수 있는 데이터 정합성 문제도 예방했습니다.

---

### 핵심 정리

- 문제: 상품 수정 시 `OptimisticLockingFailureException` 발생 (버전 충돌)
- 원인: 하나의 메소드 내에서 연속적인 변경 작업이 이루어짐
- 해결:
  - 옵션 수정/삽입/삭제 작업을 별도 메소드로 분리
  - 전체 과정을 하나의 트랜잭션으로 묶어 정합성 확보

---

## JPA N+1 문제

주문(Order)과 주문상품(OrderItem) 엔티티를 설계하면서,  
연관관계 매핑에 따른 `N+1 문제`를 경험했습니다.

주문 목록을 조회할 때 주문상품이 여러 개 연결되면,  
주문 수만큼 추가적인 SELECT 쿼리가 발생하여 성능 저하가 발생합니다.  
이는 실제 서비스 환경에서 심각한 부하를 초래할 수 있습니다.

N+1 문제를 해결하기 위해 기본적으로 FetchType.LAZY를 적용하고,  
필요한 경우 BatchSize 조절을 통해 성능을 개선했습니다.  
그러나 근본적인 해결을 위해 대부분 **FETCH JOIN** 쿼리를 사용하여 필요한 데이터를 한 번에 가져오는 방식을 적용했습니다.

---

### 핵심 정리

- 문제: 주문 조회 시 N+1 쿼리 문제 발생
- 원인: LAZY 로딩으로 인해 연관 데이터 조회 시 추가 SELECT 발생
- 해결:
  - 기본적으로 FetchType.LAZY 적용
  - 실질 조회 시 FETCH JOIN을 사용하여 한 번에 조회

---

## 시간 문제

DB에 저장되는 시간 정보가 내가 의도한 시간과 다르게 저장되는 문제가 발생했습니다.  
이는 기본적으로 데이터베이스가 `UTC` 시간대를 사용하기 때문이었습니다.

`application.properties` 설정 파일에  
`Time_Zone`을 `Asia/Seoul`로 설정하여,  
서버와 DB 간의 시간 차이를 해결했습니다.

---

### 핵심 정리

- 문제: DB에 저장되는 시간과 실제 시간이 다름
- 원인: 기본 시간대(UTC) 사용
- 해결:
  - 서버 Time_Zone을 Asia/Seoul로 설정

---

## 여러 스레드에서 동시에 주문이 들어온다면?

기본적인 주문 테스트는 성공했지만, 실제 서비스 환경에서 사용자 트래픽이 몰릴 경우를 고려해야 했습니다.  
이를 테스트하기 위해 임의로 10개의 스레드를 만들어 동시에 주문 요청을 보내보았습니다.
결과적으로 트랜잭션 경합이 발생하여 데드락이 발생했고,  
이를 해결하기 위해 주문 로직 내에서 **비관적 락(Pessimistic Lock)** 을 적용했습니다.  
이를 통해 재고 감소 등 동시성 문제가 발생하지 않도록 안전성을 확보했습니다.

---

### 핵심 정리

- 문제: 동시 주문 요청 시 데드락 발생
- 원인: 다수의 트랜잭션 경합
- 해결:
  - 주문 로직에 비관적 락(Pessimistic Lock) 적용

---

## 테스트 코드의 반복적인 사용

테스트 코드 작성 과정에서 `ObjectMapper`, `MockMvc` 등 공통적으로 사용되는 도구를  
각 테스트 클래스에서 반복적으로 작성하는 비효율적인 문제가 있었습니다.

또한, 멤버 로그인 상태가 필요한 상품 등록이나 장바구니 테스트에서는  
매번 토큰 발급 로직을 중복 작성해야 했습니다.

이 문제를 해결하기 위해 **공통 설정(Abstract Class)** 을 분리하여,  
필요한 기본 정보 세팅을 재사용할 수 있도록 개선했습니다.  
이를 통해 테스트 코드의 중복을 줄이고 유지보수성을 높일 수 있었습니다.

---

### 핵심 정리

- 문제: 테스트 코드에서 반복적인 세팅 코드 발생
- 원인: 공통 객체(ObjectMapper, MockMvc) 및 토큰 발급 로직 중복
- 해결:
  - 공통 설정을 Abstract Class로 분리하여 재사용성 강화

---

## ArgumentResolver 적용

토큰 인증 과정에서 사용자 식별을 보다 유연하게 처리하기 위해  
**커스텀 ArgumentResolver**를 구현했습니다.

Spring Security에서는 기본적으로 `@AuthenticationPrincipal` 어노테이션을 제공하지만,  
`CustomUserDetails`를 활용하여 멤버 상태(활성/비활성 여부, 마지막 로그인 날짜 등)를 쉽게 파악할 수 있도록 개선했습니다.

`loadUserByUsername` 메소드에서는 멤버의 로그인 가능 여부를 판별하여  
`CustomUserDetails`를 리턴하고,  
토큰 생성 시에는 최소한의 정보(식별자)만 추출하여 불필요한 DB 접근을 방지했습니다.

결국 서비스 로직에서는 토큰에 담긴 식별자를 이용해 필요한 시점에 DB 조회를 수행하여,  
비즈니스 로직에 안전하게 접근할 수 있도록 설계했습니다.

---

### 핵심 정리

- 문제: 토큰 기반 사용자 식별 과정 복잡
- 원인: 인증 과정에서 사용자 상태를 명확히 파악하기 어려움
- 해결:
  - Custom ArgumentResolver 도입
  - CustomUserDetails를 통해 사용자 상태를 직접 관리
  - 최소한의 토큰 정보로 인증 처리

</details>

