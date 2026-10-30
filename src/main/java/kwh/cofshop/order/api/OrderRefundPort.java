package kwh.cofshop.order.api;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRefundPort {

    void validateRefundApproved(Long orderId);

    void completeRefund(Long orderId, String processedReason);

    List<Long> findPendingApprovedRefundOrderIds(LocalDateTime approvedBefore, int limit);

    long countPendingApprovedRefunds(LocalDateTime approvedBefore);
}
