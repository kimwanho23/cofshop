package kwh.cofshop.order.api;

public enum OrderStatus {
    WAITING_FOR_PAY,
    PAYMENT_PENDING,
    REFUND_PENDING,
    PAID,
    PREPARING_FOR_SHIPMENT,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    COMPLETED;

    // 구매 확정 후에도 환불 가능. 요청 기간은 Order가 검사한다
    public boolean isRefundable() {
        return this == PAID || this == PREPARING_FOR_SHIPMENT || this == SHIPPING || this == DELIVERED || this == COMPLETED;
    }
}
