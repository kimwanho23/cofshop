package kwh.cofshop.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundCompensationRecoveryQueueService {

    private static final String REFUND_COMPENSATION_FAILURE_KEY = "refund:compensation:failures";
    private static final Duration QUEUE_TTL = Duration.ofDays(30);

    private final RedisTemplate<String, String> redisTemplate;

    public void enqueue(Long paymentId, Long memberId, Long orderId, String reasonCode) {
        enqueue(RecoveryTask.of(paymentId, memberId, orderId, reasonCode, 0, Instant.now()));
    }

    public void enqueue(RecoveryTask task) {
        String payload = serialize(task);
        push(payload);
    }

    public List<RecoveryTask> dequeueBatch(int maxCount) {
        int safeCount = Math.max(1, maxCount);
        List<RecoveryTask> tasks = new ArrayList<>(safeCount);
        for (int i = 0; i < safeCount; i++) {
            String payload = redisTemplate.opsForList().leftPop(REFUND_COMPENSATION_FAILURE_KEY);
            if (payload == null) {
                break;
            }
            parse(payload).ifPresentOrElse(tasks::add, () ->
                    log.warn("[Refund] 복구 큐 파싱 실패 payload={}", payload));
        }
        return tasks;
    }

    public void requeue(RecoveryTask task, String reasonCode) {
        enqueue(task.withNextRetry(reasonCode));
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

    private String serialize(RecoveryTask task) {
        String payload = String.format(
                "paymentId=%d,memberId=%d,orderId=%s,reason=%s,retry=%d,occurredAt=%s",
                task.paymentId(),
                task.memberId(),
                task.orderId() == null ? "null" : task.orderId().toString(),
                task.reasonCode(),
                task.retryCount(),
                task.occurredAt()
        );
        return payload;
    }

    private java.util.Optional<RecoveryTask> parse(String payload) {
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

            return java.util.Optional.of(
                    RecoveryTask.of(paymentId, memberId, orderId, reasonCode, retryCount, occurredAt)
            );
        } catch (Exception e) {
            log.warn("[Refund] 복구 큐 payload 파싱 실패 payload={}", payload, e);
            return java.util.Optional.empty();
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
}
