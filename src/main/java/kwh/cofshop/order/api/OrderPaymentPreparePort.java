package kwh.cofshop.order.api;

public interface OrderPaymentPreparePort {

    OrderPaymentPrepareInfo prepare(Long orderId, Long memberId);
}
