package kwh.cofshop.order.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class OrderStatePortAdapter implements OrderStatePort {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderStatus getOrderState(Long orderId) {
        return OrderStatus.valueOf(findOrder(orderId).getOrderState().name());
    }

    @Override
    @Transactional
    public void changeOrderState(Long orderId, OrderStatus orderStatus) {
        findOrder(orderId).changeOrderState(OrderState.valueOf(orderStatus.name()));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));
    }
}
