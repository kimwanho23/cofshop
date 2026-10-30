package kwh.cofshop.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService.RecoveryTask;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService.ClaimedRecoveryTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundCompensationRecoveryQueueServiceTest {

    private static final String KEY = "refund:compensation:failures";
    private static final String PROCESSING_KEY = "refund:compensation:failures:processing";
    private static final String DEAD_LETTER_KEY = "refund:compensation:failures:dead-letter";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private RefundCompensationRecoveryQueueService queueService;

    @Test
    @DisplayName("복구 큐 적재: payload 저장")
    void enqueue() throws Exception {
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        queueService.enqueue(10L, 1L, 100L, "ORDER-CANNOT-CANCEL");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq(KEY), payloadCaptor.capture());
        verify(redisTemplate).expire(eq(KEY), any());

        String payload = payloadCaptor.getValue();
        RecoveryTask task = objectMapper.readValue(payload, RecoveryTask.class);
        assertThat(task.paymentId()).isEqualTo(10L);
        assertThat(task.memberId()).isEqualTo(1L);
        assertThat(task.orderId()).isEqualTo(100L);
        assertThat(task.reasonCode()).isEqualTo("ORDER-CANNOT-CANCEL");
        assertThat(task.retryCount()).isZero();
    }

    @Test
    @DisplayName("복구 큐 조회: payload 파싱")
    void dequeueBatch() throws Exception {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPopAndLeftPush(PROCESSING_KEY, KEY)).thenReturn(null);
        String payload = objectMapper.writeValueAsString(
                RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 2, Instant.parse("2026-02-14T10:00:00Z"))
        );
        when(listOperations.rightPopAndLeftPush(KEY, PROCESSING_KEY))
                .thenReturn(payload)
                .thenReturn(null);

        List<ClaimedRecoveryTask> tasks = queueService.dequeueBatch(10);

        assertThat(tasks).hasSize(1);
        ClaimedRecoveryTask claimedTask = tasks.get(0);
        RecoveryTask task = claimedTask.task();
        assertThat(claimedTask.payload()).isEqualTo(payload);
        assertThat(task.paymentId()).isEqualTo(10L);
        assertThat(task.memberId()).isEqualTo(1L);
        assertThat(task.orderId()).isEqualTo(100L);
        assertThat(task.reasonCode()).isEqualTo("ORDER-CANNOT-CANCEL");
        assertThat(task.retryCount()).isEqualTo(2);
        assertThat(task.occurredAt()).isEqualTo(Instant.parse("2026-02-14T10:00:00Z"));
    }

    @Test
    @DisplayName("복구 큐 재적재: 신규 payload 추가 후 기존 processing payload ack")
    void requeue() throws Exception {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.remove(PROCESSING_KEY, 1, "old-payload")).thenReturn(1L);

        ClaimedRecoveryTask claimedTask = ClaimedRecoveryTask.of(
                "old-payload",
                RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 2, Instant.parse("2026-02-14T10:00:00Z"))
        );

        queueService.requeue(claimedTask, "INTERNAL");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        InOrder inOrder = inOrder(listOperations);
        inOrder.verify(listOperations).rightPush(eq(KEY), payloadCaptor.capture());
        inOrder.verify(listOperations).remove(PROCESSING_KEY, 1, "old-payload");

        String payload = payloadCaptor.getValue();
        RecoveryTask task = objectMapper.readValue(payload, RecoveryTask.class);
        assertThat(task.paymentId()).isEqualTo(10L);
        assertThat(task.memberId()).isEqualTo(1L);
        assertThat(task.orderId()).isEqualTo(100L);
        assertThat(task.reasonCode()).isEqualTo("INTERNAL");
        assertThat(task.retryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("복구 큐 ack: processing payload 제거")
    void acknowledge() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.remove(PROCESSING_KEY, 1, "payload")).thenReturn(1L);

        ClaimedRecoveryTask claimedTask = ClaimedRecoveryTask.of(
                "payload",
                RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 0, Instant.now())
        );

        queueService.acknowledge(claimedTask);

        verify(listOperations).remove(PROCESSING_KEY, 1, "payload");
    }

    @Test
    @DisplayName("복구 큐 DLQ 이동: payload를 DLQ에 적재 후 processing에서 제거")
    void deadLetter() throws Exception {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.remove(PROCESSING_KEY, 1, "payload")).thenReturn(1L);

        ClaimedRecoveryTask claimedTask = ClaimedRecoveryTask.of(
                "payload",
                RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 5, Instant.parse("2026-02-14T10:00:00Z"))
        );

        queueService.deadLetter(claimedTask, "RETRY_EXCEEDED");

        ArgumentCaptor<String> deadLetterCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq(DEAD_LETTER_KEY), deadLetterCaptor.capture());
        verify(redisTemplate).expire(eq(DEAD_LETTER_KEY), any());
        JsonNode deadLetter = objectMapper.readTree(deadLetterCaptor.getValue());
        assertThat(deadLetter.get("payload").asText()).isEqualTo("payload");
        assertThat(deadLetter.get("reasonCode").asText()).isEqualTo("RETRY_EXCEEDED");
        verify(listOperations).remove(PROCESSING_KEY, 1, "payload");
    }

    @Test
    @DisplayName("복구 큐 DLQ 이동 실패: ack 하지 않고 예외")
    void deadLetter_failToPush() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPush(eq(DEAD_LETTER_KEY), any())).thenThrow(new RuntimeException("redis down"));

        ClaimedRecoveryTask claimedTask = ClaimedRecoveryTask.of(
                "payload",
                RecoveryTask.of(10L, 1L, 100L, "ORDER-CANNOT-CANCEL", 5, Instant.parse("2026-02-14T10:00:00Z"))
        );

        assertThatThrownBy(() -> queueService.deadLetter(claimedTask, "RETRY_EXCEEDED"))
                .isInstanceOf(IllegalStateException.class);
        verify(listOperations, never()).remove(PROCESSING_KEY, 1, "payload");
    }

    @Test
    @DisplayName("복구 큐 조회: 파싱 실패 payload는 DLQ로 이동")
    void dequeueBatch_parseFailMoveToDlq() throws Exception {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPopAndLeftPush(PROCESSING_KEY, KEY)).thenReturn(null);
        when(listOperations.rightPopAndLeftPush(KEY, PROCESSING_KEY))
                .thenReturn("invalid-payload")
                .thenReturn(null);
        when(listOperations.remove(PROCESSING_KEY, 1, "invalid-payload")).thenReturn(1L);

        List<ClaimedRecoveryTask> tasks = queueService.dequeueBatch(10);

        assertThat(tasks).isEmpty();
        ArgumentCaptor<String> deadLetterCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq(DEAD_LETTER_KEY), deadLetterCaptor.capture());
        verify(redisTemplate).expire(eq(DEAD_LETTER_KEY), any());
        JsonNode deadLetter = objectMapper.readTree(deadLetterCaptor.getValue());
        assertThat(deadLetter.get("payload").asText()).isEqualTo("invalid-payload");
        assertThat(deadLetter.get("reasonCode").asText()).isEqualTo("PARSE_ERROR");
        assertThat(deadLetter.get("failedAt").asText()).isNotBlank();
        verify(listOperations).remove(PROCESSING_KEY, 1, "invalid-payload");
    }

    @Test
    @DisplayName("DLQ 재처리: 파싱 가능한 태스크는 본 큐로 재적재")
    void replayDeadLetters() throws Exception {
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        String originalPayload = objectMapper.writeValueAsString(
                RecoveryTask.of(22L, 2L, 202L, "ORDER-CANNOT-CANCEL", 1, Instant.parse("2026-02-14T10:00:00Z"))
        );
        String deadLetterPayload = objectMapper.createObjectNode()
                .put("payload", originalPayload)
                .put("reasonCode", "PARSE_ERROR")
                .put("failedAt", "2026-02-14T10:10:00Z")
                .toString();

        when(listOperations.leftPop(DEAD_LETTER_KEY))
                .thenReturn(deadLetterPayload)
                .thenReturn(null);

        int replayed = queueService.replayDeadLetters(10);

        assertThat(replayed).isEqualTo(1);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq(KEY), payloadCaptor.capture());
        RecoveryTask replayedTask = objectMapper.readValue(payloadCaptor.getValue(), RecoveryTask.class);
        assertThat(replayedTask.paymentId()).isEqualTo(22L);
        assertThat(replayedTask.memberId()).isEqualTo(2L);
        assertThat(replayedTask.orderId()).isEqualTo(202L);
        assertThat(replayedTask.reasonCode()).isEqualTo("ORDER-CANNOT-CANCEL");
    }
}
