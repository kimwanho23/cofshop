package kwh.cofshop.order.api;

public interface OrderCancellationPort {

    void cancelAndRestore(Long orderId);
}
