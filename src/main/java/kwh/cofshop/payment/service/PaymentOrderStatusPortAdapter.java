package kwh.cofshop.payment.service;

import kwh.cofshop.order.api.OrderPaymentStatusPort;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class PaymentOrderStatusPortAdapter implements OrderPaymentStatusPort {

    private final PaymentEntityRepository paymentEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasReadyPayment(Long orderId) {
        if (orderId == null) {
            return false;
        }

        return paymentEntityRepository.findStatusByOrderId(orderId)
                .map(status -> status == PaymentStatus.READY)
                .orElse(false);
    }
}
