package kwh.cofshop.order.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderCancellationPort;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class OrderCancellationPortAdapter implements OrderCancellationPort {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Override
    @Transactional
    public void cancelAndRestore(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));

        orderService.cancelAndRestore(order);
    }
}
