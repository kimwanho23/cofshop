package kwh.cofshop.order.api;

public interface OrderStatePort {

    OrderStatus getOrderState(Long orderId);

    void changeOrderState(Long orderId, OrderStatus orderStatus);
}
