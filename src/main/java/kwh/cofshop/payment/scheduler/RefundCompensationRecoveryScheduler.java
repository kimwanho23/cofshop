package kwh.cofshop.payment.scheduler;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.payment.service.PaymentRefundTxService;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService.RecoveryTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundCompensationRecoveryScheduler {

    private final RefundCompensationRecoveryQueueService recoveryQueueService;
    private final PaymentRefundTxService paymentRefundTxService;

    @Value("${payment.refund.recovery.batch-size:20}")
    private int batchSize;

    @Value("${payment.refund.recovery.max-retries:5}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${payment.refund.recovery.fixed-delay-ms:60000}")
    public void recoverFailedCompensations() {
        List<RecoveryTask> tasks = recoveryQueueService.dequeueBatch(batchSize);
        if (tasks.isEmpty()) {
            return;
        }

        for (RecoveryTask task : tasks) {
            try {
                paymentRefundTxService.confirmRefund(task.paymentId(), task.memberId());
                log.info("[RefundRecovery] 복구 성공 paymentId={}, memberId={}, retry={}",
                        task.paymentId(), task.memberId(), task.retryCount());
            } catch (BusinessException e) {
                if (task.retryCount() >= maxRetries) {
                    log.error("[RefundRecovery] 최대 재시도 초과 paymentId={}, memberId={}, retry={}, reason={}",
                            task.paymentId(), task.memberId(), task.retryCount(), e.getErrorCode().getCode(), e);
                    continue;
                }
                try {
                    recoveryQueueService.requeue(task, e.getErrorCode().getCode());
                    log.warn("[RefundRecovery] 복구 재시도 예약 paymentId={}, memberId={}, retry={}",
                            task.paymentId(), task.memberId(), task.retryCount() + 1, e);
                } catch (Exception queueError) {
                    log.error("[RefundRecovery] 재큐잉 실패 paymentId={}, memberId={}, retry={}",
                            task.paymentId(), task.memberId(), task.retryCount(), queueError);
                }
            } catch (Exception e) {
                if (task.retryCount() >= maxRetries) {
                    log.error("[RefundRecovery] 최대 재시도 초과(내부 오류) paymentId={}, memberId={}, retry={}",
                            task.paymentId(), task.memberId(), task.retryCount(), e);
                    continue;
                }
                try {
                    recoveryQueueService.requeue(task, "INTERNAL");
                    log.warn("[RefundRecovery] 내부 오류로 재시도 예약 paymentId={}, memberId={}, retry={}",
                            task.paymentId(), task.memberId(), task.retryCount() + 1, e);
                } catch (Exception queueError) {
                    log.error("[RefundRecovery] 내부 오류 재큐잉 실패 paymentId={}, memberId={}, retry={}",
                            task.paymentId(), task.memberId(), task.retryCount(), queueError);
                }
            }
        }
    }
}
