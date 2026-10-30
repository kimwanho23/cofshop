package kwh.cofshop.payment.service;

import kwh.cofshop.order.api.OrderPaymentReconcilePort;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OrderPaymentReconcilePortAdapter implements OrderPaymentReconcilePort {

    private final PaymentEntityRepository paymentEntityRepository;
    private final PaymentService paymentService;

    @Override
    public void reconcileBeforeCancel(Long orderId) {
        paymentEntityRepository.findByOrderId(orderId)
                .ifPresent(paymentEntity -> paymentService.reconcilePayment(paymentEntity.getId()));
    }
}
