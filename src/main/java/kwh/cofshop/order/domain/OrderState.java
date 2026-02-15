package kwh.cofshop.order.domain;

public enum OrderState {
    WAITING_FOR_PAY,        // 결제 대기
    PAYMENT_PENDING,        // 결제 요청
    REFUND_PENDING,         // 환불 진행 중 (외부 환불 성공 후 내부 보상 처리 대기)
    PAID,                   // 결제 완료
    PREPARING_FOR_SHIPMENT, // 상품 준비 중
    SHIPPING,               // 배송 중
    DELIVERED,              // 배송 완료
    CANCELLED,              // 주문 취소
    COMPLETED               // 구매 확정
}
