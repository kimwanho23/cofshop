package kwh.cofshop.order.api;

// 탈퇴 회원의 주문 개인정보를 분리 보관한 뒤 발행한다. 결제 모듈은 이 이벤트로 구매자 정보를 지운다
public record OrderMemberWithdrawnEvent(Long memberId) {
}
