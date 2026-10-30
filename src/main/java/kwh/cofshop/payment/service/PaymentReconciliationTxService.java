package kwh.cofshop.payment.service;

import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationTxService {

    private final PaymentEntityRepository paymentEntityRepository;

    @Transactional
    public void markChecked(Long paymentId) {
        paymentEntityRepository.markReconciled(paymentId, LocalDateTime.now());
    }
}
