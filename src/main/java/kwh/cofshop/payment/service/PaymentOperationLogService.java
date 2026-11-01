package kwh.cofshop.payment.service;

import kwh.cofshop.payment.domain.PaymentOperationLog;
import kwh.cofshop.payment.repository.PaymentOperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 처리 작업이 실패해도 이력은 남도록 별도 트랜잭션으로 저장
@Service
@RequiredArgsConstructor
public class PaymentOperationLogService {

    private final PaymentOperationLogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long paymentId, Long orderId, Long adminId, PaymentOperationLog.Action action,
                       PaymentOperationLog.Result result, String errorCode) {
        logRepository.save(PaymentOperationLog.of(paymentId, orderId, adminId, action, result, errorCode));
    }

    // 대사가 같은 불일치를 반복 발견해도 이력은 한 번만 남김
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMismatchOnce(Long paymentId, Long orderId, String errorCode) {
        if (logRepository.existsByPaymentIdAndAction(paymentId, PaymentOperationLog.Action.MISMATCH_DETECTED)) {
            return;
        }
        logRepository.save(PaymentOperationLog.of(
                paymentId, orderId, null,
                PaymentOperationLog.Action.MISMATCH_DETECTED, PaymentOperationLog.Result.FAILED, errorCode
        ));
    }
}
