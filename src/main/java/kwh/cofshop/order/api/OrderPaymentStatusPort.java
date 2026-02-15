package kwh.cofshop.order.api;

public interface OrderPaymentStatusPort {

    boolean hasReadyPayment(Long orderId);
}
