package kwh.cofshop.payment.service;

import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService.RecoveryTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundCompensationRecoveryQueueServiceTest {

    private static final String KEY = "refund:compensation:failures";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private RefundCompensationRecoveryQueueService queueService;

    @Test
    @DisplayName("복구 큐 적재: payload 저장")
    void enqueue() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        queueService.enqueue(10L, 1L, 100L, "ORDER-CANNOT-CANCEL");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq(KEY), payloadCaptor.capture());
        verify(redisTemplate).expire(eq(KEY), any());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("paymentId=10");
        assertThat(payload).contains("memberId=1");
        assertThat(payload).contains("orderId=100");
        assertThat(payload).contains("reason=ORDER-CANNOT-CANCEL");
        assertThat(payload).contains("retry=0");
    }

    @Test
    @DisplayName("복구 큐 조회: payload 파싱")
    void dequeueBatch() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop(KEY))
                .thenReturn("paymentId=10,memberId=1,orderId=100,reason=ORDER-CANNOT-CANCEL,retry=2,occurredAt=2026-02-14T10:00:00Z")
                .thenReturn(null);

        List<RecoveryTask> tasks = queueService.dequeueBatch(10);

        assertThat(tasks).hasSize(1);
        RecoveryTask task = tasks.get(0);
        assertThat(task.paymentId()).isEqualTo(10L);
        assertThat(task.memberId()).isEqualTo(1L);
        assertThat(task.orderId()).isEqualTo(100L);
        assertThat(task.reasonCode()).isEqualTo("ORDER-CANNOT-CANCEL");
        assertThat(task.retryCount()).isEqualTo(2);
        assertThat(task.occurredAt()).isEqualTo(Instant.parse("2026-02-14T10:00:00Z"));
    }
}
