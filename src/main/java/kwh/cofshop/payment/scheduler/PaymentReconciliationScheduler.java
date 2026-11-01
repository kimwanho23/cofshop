package kwh.cofshop.payment.scheduler;

import io.micrometer.core.instrument.Metrics;
import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import kwh.cofshop.payment.service.PaymentOperationLogService;
import kwh.cofshop.payment.service.PaymentReconciliationTxService;
import kwh.cofshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

// 고객이 결제 후 검증을 호출하지 않아도 PG 상태를 확인해 결제와 주문을 맞춤
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationScheduler {

    private final PaymentEntityRepository paymentEntityRepository;
    private final PaymentService paymentService;
    private final PaymentReconciliationTxService reconciliationTxService;
    private final PaymentOperationLogService operationLogService;

    @Value("${payment.reconciliation.min-age-minutes:5}")
    private long minAgeMinutes;

    @Value("${payment.reconciliation.max-age-hours:24}")
    private long maxAgeHours;

    @Value("${payment.reconciliation.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${payment.reconciliation.fixed-delay-ms:60000}")
    @DistributedLock(keyName = "'payment:reconciliation'", waitTime = 0, skipIfLocked = true, timeUnit = TimeUnit.MINUTES)
    public void reconcilePayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> paymentIds = paymentEntityRepository.findReconciliationTargets(
                PaymentStatus.READY,
                now.minusHours(Math.max(maxAgeHours, 1L)),
                now.minusMinutes(Math.max(minAgeMinutes, 0L)),
                PageRequest.of(0, Math.max(batchSize, 1))
        );

        for (Long paymentId : paymentIds) {
            reconcile(paymentId);
        }
    }

    private void reconcile(Long paymentId) {
        try {
            paymentService.reconcilePayment(paymentId);
        } catch (BusinessException e) {
            handleBusinessFailure(paymentId, e);
        } catch (Exception e) {
            log.error("[Reconcile] 대사 실패 paymentId={}", paymentId, e);
        }

        try {
            reconciliationTxService.markChecked(paymentId);
        } catch (Exception e) {
            log.error("[Reconcile] 확인 시각 저장 실패 paymentId={}", paymentId, e);
        }
    }

    private void recordMismatch(Long paymentId, String errorCode) {
        try {
            paymentEntityRepository.findById(paymentId).ifPresent(payment ->
                    operationLogService.recordMismatchOnce(paymentId, payment.getOrderId(), errorCode));
        } catch (Exception e) {
            log.error("[Reconcile] 불일치 이력 저장 실패 paymentId={}", paymentId, e);
        }
    }

    private void handleBusinessFailure(Long paymentId, BusinessException e) {
        if (e.getErrorCode() == BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY
                || e.getErrorCode() == BusinessErrorCode.PAYMENT_UID_DISCREPANCY) {
            log.error("[Reconcile] PG와 결제 정보 불일치 paymentId={}, code={}", paymentId, e.getErrorCode().getCode());
            Metrics.counter("payment.reconciliation.mismatch").increment();
            recordMismatch(paymentId, e.getErrorCode().getCode());
            return;
        }
        log.warn("[Reconcile] 대사 보류 paymentId={}, code={}", paymentId, e.getErrorCode().getCode());
    }
}
