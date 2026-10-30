package kwh.cofshop.payment.scheduler;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.payment.service.PaymentRefundTxService;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService.ClaimedRecoveryTask;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService.RecoveryTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundCompensationRecoverySchedulerTest {

    @Mock
    private RefundCompensationRecoveryQueueService recoveryQueueService;

    @Mock
    private PaymentRefundTxService paymentRefundTxService;

    @InjectMocks
    private RefundCompensationRecoveryScheduler scheduler;

    @Test
    @DisplayName("복구 스케줄러: 재시도 가능하면 재큐잉")
    void recoverFailedCompensations_requeueOnBusinessFailure() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 20);
        ReflectionTestUtils.setField(scheduler, "maxRetries", 5);

        RecoveryTask task = RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 1, Instant.now());
        ClaimedRecoveryTask claimedTask = ClaimedRecoveryTask.of("payload", task);
        when(recoveryQueueService.dequeueBatch(20)).thenReturn(List.of(claimedTask));
        doThrow(new BusinessException(BusinessErrorCode.ORDER_CANNOT_CANCEL))
                .when(paymentRefundTxService).confirmRefundForRecovery(10L, 1L);

        scheduler.recoverFailedCompensations();

        verify(recoveryQueueService).requeue(claimedTask, BusinessErrorCode.ORDER_CANNOT_CANCEL.getCode());
    }

    @Test
    @DisplayName("복구 스케줄러: 최대 재시도 초과면 DLQ 이동")
    void recoverFailedCompensations_noRequeueWhenExceeded() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 20);
        ReflectionTestUtils.setField(scheduler, "maxRetries", 2);

        RecoveryTask task = RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 2, Instant.now());
        ClaimedRecoveryTask claimedTask = ClaimedRecoveryTask.of("payload", task);
        when(recoveryQueueService.dequeueBatch(20)).thenReturn(List.of(claimedTask));
        doThrow(new BusinessException(BusinessErrorCode.ORDER_CANNOT_CANCEL))
                .when(paymentRefundTxService).confirmRefundForRecovery(10L, 1L);

        scheduler.recoverFailedCompensations();

        verify(recoveryQueueService, never()).requeue(claimedTask, BusinessErrorCode.ORDER_CANNOT_CANCEL.getCode());
        verify(recoveryQueueService).deadLetter(claimedTask, BusinessErrorCode.ORDER_CANNOT_CANCEL.getCode());
    }

    @Test
    @DisplayName("복구 스케줄러: 복구 성공 시 ack")
    void recoverFailedCompensations_ackOnSuccess() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 20);
        ReflectionTestUtils.setField(scheduler, "maxRetries", 5);

        RecoveryTask task = RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 0, Instant.now());
        ClaimedRecoveryTask claimedTask = ClaimedRecoveryTask.of("payload", task);
        when(recoveryQueueService.dequeueBatch(20)).thenReturn(List.of(claimedTask));

        scheduler.recoverFailedCompensations();

        verify(paymentRefundTxService).confirmRefundForRecovery(10L, 1L);
        verify(recoveryQueueService).acknowledge(claimedTask);
    }

    @Test
    @DisplayName("복구 스케줄러: 내부 오류 재시도 초과면 DLQ 이동")
    void recoverFailedCompensations_deadLetterOnInternalFailureExceeded() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 20);
        ReflectionTestUtils.setField(scheduler, "maxRetries", 1);

        RecoveryTask task = RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 1, Instant.now());
        ClaimedRecoveryTask claimedTask = ClaimedRecoveryTask.of("payload", task);
        when(recoveryQueueService.dequeueBatch(20)).thenReturn(List.of(claimedTask));
        doThrow(new RuntimeException("unexpected"))
                .when(paymentRefundTxService).confirmRefundForRecovery(10L, 1L);

        scheduler.recoverFailedCompensations();

        verify(recoveryQueueService).deadLetter(claimedTask, "INTERNAL");
        verify(recoveryQueueService, never()).requeue(claimedTask, "INTERNAL");
    }
}
