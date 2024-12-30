package kwh.cofshop.order.domain;

public enum OrderState {
    NEW,          // 신규 주문
    PROCESSING,   // 배송 중
    SHIPPED,      // 배송 완료
    CANCELLED     // 주문 취소
}
