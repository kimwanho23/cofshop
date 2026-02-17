package kwh.cofshop.order.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderRefundPort;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    @Transactional
    public void completeRefund(Long orderId, String processedReason) {
        Order order = findOrder(orderId);
        orderService.cancelAndRestore(order);
        orderService.completeRefundRequestByPayment(order, processedReason, LocalDateTime.now());
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));
    }
}
