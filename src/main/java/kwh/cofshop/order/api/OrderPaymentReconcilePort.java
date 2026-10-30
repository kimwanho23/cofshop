package kwh.cofshop.order.api;

public interface OrderPaymentReconcilePort {

    // 자동 취소 전 PG 상태로 결제를 맞춤. PG 조회가 실패하면 예외를 던져 취소를 보류하게 함
    void reconcileBeforeCancel(Long orderId);
}
