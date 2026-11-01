package kwh.cofshop.order.service;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class OrderStatePortAdapter implements OrderStatePort {

    private static final Map<OrderState, List<OrderState>> ALLOWED_CURRENT_STATES = Map.of(
            OrderState.PAID, List.of(
                    OrderState.WAITING_FOR_PAY,
                    OrderState.PAYMENT_PENDING,
                    OrderState.REFUND_PENDING,
                    OrderState.PAID
            ),
            OrderState.REFUND_PENDING, List.of(
                    OrderState.PAID,
                    OrderState.PREPARING_FOR_SHIPMENT,
                    OrderState.SHIPPING,
                    OrderState.DELIVERED,
                    OrderState.COMPLETED
            )
    );

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderStatus getOrderState(Long orderId) {
        OrderState orderState = orderRepository.findOrderStateById(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));
        return OrderStatus.valueOf(orderState.name());
    }

    @Override
    @DistributedLock(keyName = "'order:' + #orderId")
    @Transactional
    public void changeOrderState(Long orderId, OrderStatus orderStatus) {
        OrderState targetState = OrderState.valueOf(orderStatus.name());
        List<OrderState> currentStates = ALLOWED_CURRENT_STATES.get(targetState);
        if (currentStates == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_INVALID_STATE_TRANSITION);
        }

        int updated = targetState == OrderState.REFUND_PENDING
                ? orderRepository.beginRefund(orderId, currentStates, targetState)
                : orderRepository.updateOrderStateIfCurrentIn(orderId, currentStates, targetState);
        if (updated > 0) {
            return;
        }

        OrderState currentState = orderRepository.findOrderStateById(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));
        if (currentState == targetState) {
            return;
        }
        throw new BusinessException(BusinessErrorCode.ORDER_INVALID_STATE_TRANSITION);
    }

    @Override
    @DistributedLock(keyName = "'order:' + #orderId")
    @Transactional
    public void rollbackRefundPending(Long orderId) {
        orderRepository.rollbackRefund(orderId, OrderState.REFUND_PENDING, OrderState.PAID);
    }
}
