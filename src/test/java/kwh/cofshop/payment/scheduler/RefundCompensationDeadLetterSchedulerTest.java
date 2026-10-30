package kwh.cofshop.payment.scheduler;

import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundCompensationDeadLetterSchedulerTest {

    @Mock
    private RefundCompensationRecoveryQueueService recoveryQueueService;

    @InjectMocks
    private RefundCompensationDeadLetterScheduler scheduler;

    @Test
    @DisplayName("DLQ 스케줄러: 대기 건이 없으면 재처리하지 않음")
    void inspectDeadLetters_noPending() {
        when(recoveryQueueService.deadLetterSize()).thenReturn(0L);

        scheduler.inspectDeadLetters();

        verify(recoveryQueueService, never()).replayDeadLetters(anyInt());
        verify(recoveryQueueService, never()).peekDeadLetterPayloads(anyInt());
    }

    @Test
    @DisplayName("DLQ 스케줄러: 대기 건이 있으면 재처리 및 샘플 조회")
    void inspectDeadLetters_withPending() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 5);
        ReflectionTestUtils.setField(scheduler, "sampleSize", 2);
        when(recoveryQueueService.deadLetterSize()).thenReturn(3L, 1L);
        when(recoveryQueueService.replayDeadLetters(5)).thenReturn(2);
        when(recoveryQueueService.peekDeadLetterPayloads(2))
                .thenReturn(List.of("{\"payload\":\"a\"}", "{\"payload\":\"b\"}"));

        scheduler.inspectDeadLetters();

        verify(recoveryQueueService).replayDeadLetters(5);
        verify(recoveryQueueService).peekDeadLetterPayloads(2);
    }
}
