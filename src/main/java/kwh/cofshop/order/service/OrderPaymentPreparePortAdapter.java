package kwh.cofshop.order.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderPaymentPrepareInfo;
import kwh.cofshop.order.api.OrderPaymentPreparePort;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class OrderPaymentPreparePortAdapter implements OrderPaymentPreparePort {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderPaymentPrepareInfo prepare(Long orderId, Long memberId) {
        Order order = orderRepository.findByIdAndMember_Id(orderId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));

        order.pay();

        return new OrderPaymentPrepareInfo(
                order.getId(),
                order.getMerchantUid(),
                order.getFinalPrice(),
                order.getMember().getId(),
                order.getMember().getEmail(),
                order.getMember().getMemberName(),
                order.getMember().getTel()
        );
    }
}
