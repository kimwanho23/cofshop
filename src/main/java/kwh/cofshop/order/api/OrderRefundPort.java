package kwh.cofshop.order.api;

public interface OrderRefundPort {

    void validateRefundApproved(Long orderId);

    void completeRefund(Long orderId, String processedReason);
}
