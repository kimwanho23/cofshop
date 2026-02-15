package kwh.cofshop.payment.domain;

public enum PaymentStatus {
    READY,   // 결제 요청
    PAID,   // 결제 성공
    REFUND_PENDING, // 환불 진행 중 (외부/내부 정합성 보정용)
    FAILED, // 결제 실패
    CANCELLED  // 결제 취소
}
