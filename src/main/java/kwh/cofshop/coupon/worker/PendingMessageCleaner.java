package kwh.cofshop.coupon.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingMessageCleaner {

    private final StringRedisTemplate redisTemplate;

    public static final String STREAM_KEY = "stream:events";
    public static final String COUPON_GROUP = "consumer-group:coupon";

    /**
     * 10초마다 Pending 메시지 중 처리된 것으로 간주되는 것들을 Ack + Del
     */
    /**
     * 10초마다 Pending 메시지 중 처리된 것으로 간주되는 것들을 Ack + Del
     */
    @Scheduled(fixedDelay = 1000)
    public void cleanPendingMessages() {
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        PendingMessages pendingMessages = ops.pending(STREAM_KEY, COUPON_GROUP, Range.unbounded(), 100);

        if (pendingMessages.isEmpty()) {
            return;
        }

        List<PendingMessage> messages = pendingMessages.stream().toList();

        for (PendingMessage msg : messages) {
            try {
                log.info("[PendingCleaner] 처리 시도: id={}, consumer={}, idle={}ms", msg.getId(), msg.getConsumerName(), msg.getTotalDeliveryCount());

                Long acked = ops.acknowledge(STREAM_KEY, COUPON_GROUP, msg.getId());
                log.info("[PendingCleaner] ACK 처리됨: id={}, acked={}", msg.getId(), acked);

                if (acked > 0) {
                    Long deleted = ops.delete(STREAM_KEY, msg.getId());
                    log.info("[PendingCleaner] 삭제됨: id={}, deleted={}", msg.getId(), deleted);
                }

            } catch (Exception e) {
                log.error("[PendingCleaner] 메시지 처리 실패: {}", msg.getId(), e);
            }
        }
    }
}
