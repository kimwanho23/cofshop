package kwh.cofshop.coupon.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponDlqConsumer {

    private static final String DLQ_CONSUMER = "coupon-dlq-consumer";

    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelayString = "${coupon.stream.dlq.fixed-delay-ms:2000}")
    public void consumeDlq() {
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        List<MapRecord<String, String, String>> records = ops.read(
                Consumer.from(CouponStreamConstants.DLQ_GROUP, DLQ_CONSUMER),
                StreamOffset.create(CouponStreamConstants.DLQ_STREAM_KEY, ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, String, String> record : records) {
            log.warn("[CouponDLQ] message id={}, payload={}", record.getId(), record.getValue());
            ops.acknowledge(CouponStreamConstants.DLQ_STREAM_KEY, CouponStreamConstants.DLQ_GROUP, record.getId());
            ops.delete(CouponStreamConstants.DLQ_STREAM_KEY, record.getId());
        }
    }
}
