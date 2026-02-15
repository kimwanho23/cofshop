package kwh.cofshop.payment.scheduler;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.payment.service.PaymentRefundTxService;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService;
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
        when(recoveryQueueService.dequeueBatch(20)).thenReturn(List.of(task));
        doThrow(new BusinessException(BusinessErrorCode.ORDER_CANNOT_CANCEL))
                .when(paymentRefundTxService).confirmRefund(10L, 1L);

        scheduler.recoverFailedCompensations();

        verify(recoveryQueueService).requeue(task, BusinessErrorCode.ORDER_CANNOT_CANCEL.getCode());
    }

    @Test
    @DisplayName("복구 스케줄러: 최대 재시도 초과면 재큐잉하지 않음")
    void recoverFailedCompensations_noRequeueWhenExceeded() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 20);
        ReflectionTestUtils.setField(scheduler, "maxRetries", 2);

        RecoveryTask task = RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 2, Instant.now());
        when(recoveryQueueService.dequeueBatch(20)).thenReturn(List.of(task));
        doThrow(new BusinessException(BusinessErrorCode.ORDER_CANNOT_CANCEL))
                .when(paymentRefundTxService).confirmRefund(10L, 1L);

        scheduler.recoverFailedCompensations();

        verify(recoveryQueueService, never()).requeue(task, BusinessErrorCode.ORDER_CANNOT_CANCEL.getCode());
    }
}
