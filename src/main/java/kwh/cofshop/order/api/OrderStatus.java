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
    COMPLETED
}
