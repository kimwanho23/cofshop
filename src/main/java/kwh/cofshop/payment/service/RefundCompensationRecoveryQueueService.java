package kwh.cofshop.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundCompensationRecoveryQueueService {

    private static final String REFUND_COMPENSATION_FAILURE_KEY = "refund:compensation:failures";
    private static final String REFUND_COMPENSATION_PROCESSING_KEY = "refund:compensation:failures:processing";
    private static final String REFUND_COMPENSATION_DEAD_LETTER_KEY = "refund:compensation:failures:dead-letter";
    private static final Duration QUEUE_TTL = Duration.ofDays(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void enqueue(Long paymentId, Long memberId, Long orderId, String reasonCode) {
        enqueue(RecoveryTask.of(paymentId, memberId, orderId, reasonCode, 0, Instant.now()));
    }

    public void enqueue(RecoveryTask task) {
        String payload = serialize(task);
        push(payload);
    }

    public List<ClaimedRecoveryTask> dequeueBatch(int maxCount) {
        restoreProcessingQueue();

        int safeCount = Math.max(1, maxCount);
        List<ClaimedRecoveryTask> tasks = new ArrayList<>(safeCount);
        for (int i = 0; i < safeCount; i++) {
            String payload = redisTemplate.opsForList().rightPopAndLeftPush(
                    REFUND_COMPENSATION_FAILURE_KEY,
                    REFUND_COMPENSATION_PROCESSING_KEY
            );
            if (payload == null) {
                break;
            }
            redisTemplate.expire(REFUND_COMPENSATION_PROCESSING_KEY, QUEUE_TTL);
            parse(payload).ifPresentOrElse(task -> tasks.add(ClaimedRecoveryTask.of(payload, task)),
                    () -> {
                        log.warn("[Refund] 복구 큐 파싱 실패 payload={}", payload);
                        boolean moved = moveToDeadLetter(payload, "PARSE_ERROR");
                        if (moved) {
                            acknowledgePayload(payload);
                        } else {
                            log.error("[Refund] DLQ 이동 실패로 ack 보류 payload={}", payload);
                        }
                    });
        }
        return tasks;
    }

    public void acknowledge(ClaimedRecoveryTask claimedTask) {
        acknowledgePayload(claimedTask.payload());
    }

    public void requeue(ClaimedRecoveryTask claimedTask, String reasonCode) {
        enqueue(claimedTask.task().withNextRetry(reasonCode));
        acknowledgePayload(claimedTask.payload());
    }

    public void deadLetter(ClaimedRecoveryTask claimedTask, String reasonCode) {
        boolean moved = moveToDeadLetter(claimedTask.payload(), reasonCode);
        if (!moved) {
            throw new IllegalStateException("Failed to move refund recovery task to dead-letter queue");
        }
        acknowledgePayload(claimedTask.payload());
    }

    public long pendingSize() {
        try {
            Long pending = redisTemplate.opsForList().size(REFUND_COMPENSATION_FAILURE_KEY);
            Long processing = redisTemplate.opsForList().size(REFUND_COMPENSATION_PROCESSING_KEY);
            return (pending == null ? 0L : pending) + (processing == null ? 0L : processing);
        } catch (Exception e) {
            log.error("[Refund] 복구 큐 크기 조회 실패", e);
            return 0L;
        }
    }

    public long deadLetterSize() {
        try {
            Long size = redisTemplate.opsForList().size(REFUND_COMPENSATION_DEAD_LETTER_KEY);
            return size == null ? 0L : size;
        } catch (Exception e) {
            log.error("[Refund] DLQ 크기 조회 실패", e);
            return 0L;
        }
    }

    public List<String> peekDeadLetterPayloads(int maxCount) {
        int safeCount = Math.max(1, maxCount);
        try {
            List<String> payloads = redisTemplate.opsForList()
                    .range(REFUND_COMPENSATION_DEAD_LETTER_KEY, 0, safeCount - 1L);
            return payloads == null ? List.of() : payloads;
        } catch (Exception e) {
            log.error("[Refund] DLQ 샘플 조회 실패", e);
            return List.of();
        }
    }

    public int replayDeadLetters(int maxCount) {
        int safeCount = Math.max(1, maxCount);
        int replayed = 0;
        for (int i = 0; i < safeCount; i++) {
            String deadLetterPayload = redisTemplate.opsForList().leftPop(REFUND_COMPENSATION_DEAD_LETTER_KEY);
            if (deadLetterPayload == null) {
                break;
            }

            Optional<RecoveryTask> taskToReplay = parseDeadLetter(deadLetterPayload);
            if (taskToReplay.isEmpty()) {
                redisTemplate.opsForList().rightPush(REFUND_COMPENSATION_DEAD_LETTER_KEY, deadLetterPayload);
                redisTemplate.expire(REFUND_COMPENSATION_DEAD_LETTER_KEY, QUEUE_TTL);
                continue;
            }

            push(serialize(taskToReplay.get()));
            replayed++;
        }
        return replayed;
    }

    private void push(String payload) {
        try {
            redisTemplate.opsForList().rightPush(REFUND_COMPENSATION_FAILURE_KEY, payload);
            redisTemplate.expire(REFUND_COMPENSATION_FAILURE_KEY, QUEUE_TTL);
        } catch (Exception e) {
            log.error("[Refund] 보상 실패 큐 적재 실패 payload={}", payload, e);
            throw new IllegalStateException("Failed to enqueue refund compensation recovery task", e);
        }
    }

    private void restoreProcessingQueue() {
        while (true) {
            String payload = redisTemplate.opsForList().rightPopAndLeftPush(
                    REFUND_COMPENSATION_PROCESSING_KEY,
                    REFUND_COMPENSATION_FAILURE_KEY
            );
            if (payload == null) {
                break;
            }
            redisTemplate.expire(REFUND_COMPENSATION_FAILURE_KEY, QUEUE_TTL);
            log.warn("[Refund] 미처리 복구 태스크를 본 큐로 복원 payload={}", payload);
        }
    }

    private void acknowledgePayload(String payload) {
        try {
            Long removed = redisTemplate.opsForList().remove(REFUND_COMPENSATION_PROCESSING_KEY, 1, payload);
            if (removed == null || removed == 0) {
                log.warn("[Refund] 복구 큐 ack 대상이 존재하지 않음 payload={}", payload);
            }
        } catch (Exception e) {
            log.error("[Refund] 복구 큐 ack 실패 payload={}", payload, e);
            throw new IllegalStateException("Failed to acknowledge refund compensation recovery task", e);
        }
    }

    private String serialize(RecoveryTask task) {
        return toJson(task);
    }

    private Optional<RecoveryTask> parse(String payload) {
        Optional<RecoveryTask> jsonParsed = parseJson(payload);
        if (jsonParsed.isPresent()) {
            return jsonParsed;
        }
        return parseLegacy(payload);
    }

    private Optional<RecoveryTask> parseJson(String payload) {
        try {
            return Optional.of(objectMapper.readValue(payload, RecoveryTask.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<RecoveryTask> parseLegacy(String payload) {
        try {
            Map<String, String> values = Stream.of(payload.split(","))
                    .map(part -> part.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));

            Long paymentId = Long.valueOf(values.get("paymentId"));
            Long memberId = Long.valueOf(values.get("memberId"));
            Long orderId = "null".equals(values.get("orderId")) ? null : Long.valueOf(values.get("orderId"));
            String reasonCode = values.getOrDefault("reason", "UNKNOWN");
            int retryCount = Integer.parseInt(values.getOrDefault("retry", "0"));
            Instant occurredAt = Instant.parse(values.getOrDefault("occurredAt", Instant.now().toString()));

            return Optional.of(
                    RecoveryTask.of(paymentId, memberId, orderId, reasonCode, retryCount, occurredAt)
            );
        } catch (Exception e) {
            log.debug("[Refund] 레거시 payload 파싱 실패 payload={}", payload, e);
            return Optional.empty();
        }
    }

    private Optional<RecoveryTask> parseDeadLetter(String deadLetterPayload) {
        try {
            DeadLetterEntry entry = objectMapper.readValue(deadLetterPayload, DeadLetterEntry.class);
            return parse(entry.payload());
        } catch (Exception ignored) {
            return parse(deadLetterPayload);
        }
    }

    private boolean moveToDeadLetter(String payload, String reasonCode) {
        String deadLetterPayload = toJson(DeadLetterEntry.of(payload, reasonCode, Instant.now()));
        try {
            redisTemplate.opsForList().rightPush(REFUND_COMPENSATION_DEAD_LETTER_KEY, deadLetterPayload);
            redisTemplate.expire(REFUND_COMPENSATION_DEAD_LETTER_KEY, QUEUE_TTL);
            return true;
        } catch (Exception e) {
            log.error("[Refund] DLQ 적재 실패 payload={}", payload, e);
            return false;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize refund recovery payload", e);
        }
    }

    public record RecoveryTask(
            Long paymentId,
            Long memberId,
            Long orderId,
            String reasonCode,
            int retryCount,
            Instant occurredAt
    ) {
        public static RecoveryTask of(
                Long paymentId,
                Long memberId,
                Long orderId,
                String reasonCode,
                int retryCount,
                Instant occurredAt
        ) {
            return new RecoveryTask(paymentId, memberId, orderId, reasonCode, retryCount, occurredAt);
        }

        public RecoveryTask withNextRetry(String reasonCode) {
            return new RecoveryTask(paymentId, memberId, orderId, reasonCode, retryCount + 1, Instant.now());
        }
    }

    public record ClaimedRecoveryTask(
            String payload,
            RecoveryTask task
    ) {
        public static ClaimedRecoveryTask of(String payload, RecoveryTask task) {
            return new ClaimedRecoveryTask(payload, task);
        }
    }

    private record DeadLetterEntry(
            String payload,
            String reasonCode,
            Instant failedAt
    ) {
        private static DeadLetterEntry of(String payload, String reasonCode, Instant failedAt) {
            return new DeadLetterEntry(payload, reasonCode, failedAt);
        }
    }
}
