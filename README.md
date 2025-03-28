# Rest API 기반 쇼핑몰 프로젝트입니다.
테스트 코드를 기반으로 로직을 검증하며, 
PostMan을 통해서 실제 URL에서 어떤 JSON 값을 리턴하는 지 테스트했습니다.

## 사용 기술
Spring Boot 3 / Spring Data JPA / QueryDSL / MYSQL/ Lombok

## 기능 목록
회원 기능
1. 회원가입
2. 로그인
3. 회원탈퇴
   - 탈퇴 회원은 MemberState(Enum class)를 변경
5. 회원 상태 변경
    - 관리자 전용
   
상품 관련
1. 상품 등록 / 수정 / 삭제
2. 상품 검색
3. 전체 상품 조회
4. 카테고리 등록
5. 전체 카테고리 조회

주문 관련
1. 주문 생성 / 주문 상태 변경 / 주문 취소
2. 하나의 주문 상품 정보 조회
3. 한 사람의 전체 주문 조회
4. 전체 주문 조회(관리자)

장바구니
1. 회원가입 시 장바구니 동시 생성
2. 회원 장바구니 목록 조회

상품 리뷰
1. 상품 리뷰 생성 / 수정/ 삭제
2. 한 회원은 하나의 상품에 대한 리뷰는 한 개씩만 1:1로 작성할 수 있다.
3. 리뷰는 페이지당 15개씩 불러오고, 페이징 메소드를 통해서 관리하며,
임의의 버튼을 누르면 1~15번 리뷰에서 16~30번 리뷰로 넘어가도록 구상하였다.

응답 및 예외 처리
공통 응답은 ApiResponse로 관리하며, 에러는 ErrorCode를 통해서 통합 관리한다.
잘못된 접근 등은 BadRequestException, 쇼핑몰 내부 비즈니스적 오류는 BusinessError를 리턴하는 등 
각 메세지의 에러 상태를 커스텀하여 관리한다.


# 겪은 문제 및 고민(트러블 슈팅)

## 공통 응답
ResponseEntity<>를 사용하여 Http 응답 메시지를 구성하고,
ApiResponse 공통 응답 객체를 통해서 상태 메시지와 데이터를 분리하여 JSON으로 보내는 쪽으로 구상하였다.
처음엔 ResponseEntity<?>의 구조로 와일드카드 문자를 이용하는 것이 편했으나, 
데이터 구조에 맞게 보내는 것이 맞다고 판단해서 ResponseEntity<ApiResponse<DTO>>> 와 같이 DTO 객체를 보내는 방식으로 변경하였다.

## 상품과 카테고리 구조에 대하여
기본적으로 쇼핑몰의 구조는 다양한데 그 중에서 하나의 상품에 여러 옵션이 있는 경우를 채택했다.
만약에 Item에 Option이 존재한다면 중복 데이터가 많아진다고 판단, 정규화가 필요하다고 생각해서 상품과 옵션을 분리하여 관리했다.
Item 쪽에서는 기본적인 상품명과 기본값, 배송비, 원산지 등의 정보를 보유한다.
Option 쪽에서는 Option에 따른 추가금을 부여하며, 수량을 관리해서 Option의 모든 수량이 0이 되면 상품이 매진처리된다.
카테고리 쪽에서는 Item과 연관관계를 맺고, 관계 테이블을 통해서 데이터 조회를 편하게 할 수 있게 설계하였다.
추후에 주문 쪽도 비슷한 구조를 지니게 되었다.

## 상품 이미지 관련 오류
상품 업로드 시에 추가적인 이미지를 업로드한다.
하나는 대표 이미지(썸네일), 나머지는 쇼핑몰에서 흔하게 볼 수 있는 상품 설명 이미지들이다.
처음에 막연하게 DTO에서 MultiPart를 사용했었는데, 굉장히 잘못된 구조였다..
기본적으로 DTO는 데이터 통신을 위한 틀인데, 이것이 스프링에 의존적인 구조가 된 것이다.
이미지 업로드는 서비스 쪽에서 처리하기 때문에 DTO에는 기본적인 이미지의 이름이나 타입 정보 변수만 선언하였다.

이렇게 다시 구조를 바꿔보니 기존 서비스 코드에서 이미지를 처리할 때 이게 상품과 맞는 이미지인지 어떻게 구분해야하지..?
단순히 반복문으로 순서에 맞춰서 이게 맞는 것인지 알 수 있는 건가? 라는 생각이 들었다.
Map 구조를 사용하면 Key 값에 맞춰 Value가 들어가기 때문에 그 문제를 해결할 수 있다고 생각했고, 적용하였다.
결론적으로 이미지 업로드 메소드는 Map<DTO(이미지의 정보, DB에 저장), IMAGE(실제 이미지, 파일로 저장)>같은 구조가 되었다.

## 상품 수정
상품 수정 시 낙관적 락 오류가 발생했다.
상품은 이미지, 카테고리, 옵션 등과 연관관계를 맺고 있기 때문에,
상품 ID를 기준으로 4개의 테이블에서 수정이 이루어져야 했다.
처음에 수정을 구현할 때, 각각의 테이블에서는 수정에 조금씩 다른 조건을 요구했다.
옵션은 삽입 / 삭제 말고도 자체적인 변경 감지가 필요했고,
이미지는 실제 파일의 삽입 / 삭제가 이루어져야 했다.
처음에 수정을 구현할 때 이 여러 가지의 요구사항을 하나의 메소드에서 구현했는데, 옵션 쪽에서 낙관적 락 오류가 발생했다.
그래서 기존의 요소 수정(updateExist), 새로운 옵션 삽입(newOption), 옵션 삭제(deleteOption)의 작업을 메소드로 분리한 다음
update 메소드에서 이 3가지의 메소드를 불러오는 방식으로 처리하여 낙관적 락 오류를 해결하였다.
기존 메서드에서는 한나의 메소드 안에서 연속적인 변경이 일어난 것 때문에 낙관적 락이 걸린 것으로 추정된다.
그래서 처음에 메소드를 각각의 트랜잭션으로 분리하는 방식으로 문제를 해결했는데, 만약 다른 트랜잭션에서 이미 커밋된 후 rollback이 일어난다면
데이터의 정합성이 깨질 것을 우려해서 한 메소드 안에 트랜잭션을 적용하였다.

## JPA N + 1 문제
대표적으로 생각나는 건 주문할 때 Order와 OrderItem쪽이고, 설계 시 주문과 주문상품 엔티티를 따로 만들었고, 연관관계를 통해서 관리했다.
만약에 주문 아이템 개수가 늘어나면, 늘어난 만큼 select 쿼리가 발생하는 n+1 문제가 발생하고, 주문 상품 개수가 늘어난다면 쿼리가 더 많이 실행되기 때문에 무조건 성능 저하의 원인이 된다.
FetchType.Lazy나, BatchSize 조절 등의 방법도 존재했지만, 대부분 Join 쿼리를 통해서 해결하였다.

## 시간 문제
데이터베이스에 자꾸 내가 저장한 시간과 다른 시간 정보가 들어갔다.
UTC 시간대를 사용하는 문제였다.
Properties에 Time_Zone을 Asia/Seoul로 바꿔주었다.

## 여러 스레드에서 동시에 주문이 들어온다면?
기본적인 주문 테스트 코드는 동작되었지만, 만약에 실제로 사용자가 몰리는 환경이라면 어떨까?
동시성 문제는 항상 대두되는 문제였기 때문에 테스트 코드에서 임의의 스레드를 10개 만들고 동시에 주문 요청을 해보았다.
당연히 트랜잭션에서 데드락이 발생하여 실패하였고, 이 동시성 문제는 쿼리 자체에서 비관적 락을 걸어서 해결하였다.

## 테스트 코드의 반복적인 사용
테스트 코드에서 공통적으로 사용되는 ObjectMapper나, MockMvc 테스트 도구 등이 모든 테스트 코드에서 사용되었다.
또한 멤버가 로그인 상태여야 하는 상품 등록이나 장바구니 테스트 코드들에서 모든 코드에 토큰 정보를 가져오는 코드를 일일히 쓸 필요는 없었다.
테스트 코드에서도 필요한 기본 정보를 미리 Setting Abstract Class로 분리하여 재사용성을 높였다.

## ArgumentResolver
토큰 정보와 관련되어서 설계 시에 애를 먹었던 부분이다.
기본적으로 토큰에는 '사용자를 식별할 수 있는 최소한의 정보를 담고, 서비스 로직에서 토큰 정보를 이용하여 조회한다'를 기본으로 커스텀 어노테이션을 만들고 싶었다.
스프링 자체적으로 UserDetails 기반의 @AuthenticationPrincipal 어노테이션을 제공하기는 하지만, CustomUserDetails을 통해서 멤버의 상태를 파악을 더 쉽게 할 수 있다고 생각했다.
loadByUserName에서 멤버의 로그인 가능 여부를 파악한 다음에 CustomUserDetails를 리턴하는 과정에서 DB와의 접촉을 하기 때문에,
토큰 생성 시에는 기본적인 멤버 식별자만 getPrincipal()을 통해서 가져와서 토큰을 생성하고, 서비스 로직에서 토큰의 식별자를 이용해서 현재 로그인 된 멤버를 파악하고, 비즈니스 로직에 접근하였다.

# 개발하면서..

## 클린 코드?
'비즈니스 로직은 순수해야 한다' 를 바탕으로, 가독성 좋은 코드를 작성하는 것도 어려운 일이라고 생각한다.
본인이 작성하기 편하게 처음에 구조를 짜는 게 정말 중요한 것 같다.
컨트롤러 쪽 코드는 굉장히 단순하게, 서비스 쪽에서는 깔끔하고 잘 동작할 수 있게 노력을 기울였다.
서비스 쪽에서 일단 기능을 구현해보고 하나의 코드에 너무 많은 역할이 주어진다고 싶으면 일단 리팩토링을 하는 쪽으로 프로젝트를 개발하였다.

## DB 구조를 어떻게 해야할까?
처음에 좋은 ERD를 다이어그램을 설계하는 게 맞다고 생각하지만,
생각보다 그게 쉽지 않기도 하고, 개발 진행 시에 계속해서 DB 구조를 바꿔야 할 일이 생긴다.

## 추가할만한 요소?
추천 상품이나, 쿠폰 시스템 같은 것도 추가해볼만하다고 생각한다.
