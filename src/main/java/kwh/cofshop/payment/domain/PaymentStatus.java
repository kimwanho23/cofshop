package kwh.cofshop.payment.domain;

public enum PaymentStatus {
    READY,   // 결제 요청
    PAID,   // 결제 성공
    FAILED, // 결제 실패
    CANCELLED  // 결제 취소
}