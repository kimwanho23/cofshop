package kwh.cofshop.order.service;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderRefundPort;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
class OrderRefundPortAdapter implements OrderRefundPort {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Override
    @Transactional(readOnly = true)
    public void validateRefundApproved(Long orderId) {
        OrderRefundRequestStatus refundRequestStatus = orderRepository.findRefundRequestStatusById(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));
        if (refundRequestStatus != OrderRefundRequestStatus.APPROVED) {
            throw new BusinessException(BusinessErrorCode.ORDER_REFUND_REQUEST_NOT_APPROVED);
        }
    }

    @Override
    @DistributedLock(keyName = "'order:' + #orderId")
    @Transactional
    public void completeRefund(Long orderId, String processedReason) {
        Order order = findOrder(orderId);
        orderService.cancelAndRestore(order, processedReason);
        orderService.completeRefundRequestByPayment(order, processedReason, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findPendingApprovedRefundOrderIds(LocalDateTime approvedBefore, int limit) {
        return orderRepository.findPendingApprovedRefundOrderIds(
                OrderRefundRequestStatus.APPROVED,
                List.of(OrderState.PAID, OrderState.REFUND_PENDING),
                approvedBefore,
                PageRequest.of(0, limit)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingApprovedRefunds(LocalDateTime approvedBefore) {
        return orderRepository.countPendingApprovedRefunds(
                OrderRefundRequestStatus.APPROVED,
                List.of(OrderState.PAID, OrderState.REFUND_PENDING),
                approvedBefore
        );
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));
    }
}
