package kwh.cofshop.order.api;

public interface OrderStatePort {

    OrderStatus getOrderState(Long orderId);

    void changeOrderState(Long orderId, OrderStatus orderStatus);

    // PG 환불 실패가 확정됐을 때 환불 진행 전 상태로 되돌림
    void rollbackRefundPending(Long orderId);
}
