package kwh.cofshop.coupon.redisstream.config;

import io.lettuce.core.RedisBusyException;
import kwh.cofshop.coupon.redisstream.worker.CouponStreamConstants;
import kwh.cofshop.coupon.redisstream.worker.CouponStreamConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "coupon.stream.enabled", havingValue = "true")
public class RedisStreamConfig {

    private static final String CONSUMER_NAME = "coupon-consumer";
    private static final int CONSUMER_COUNT = 3;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final CouponStreamConsumer couponStreamListener;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer() {
        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                .executor(Executors.newFixedThreadPool(CONSUMER_COUNT))
                .build();

        var container = StreamMessageListenerContainer.create(redisConnectionFactory, options);

        for (int i = 0; i < CONSUMER_COUNT; i++) {
            String consumerName = CONSUMER_NAME + i;
            container.receive(
                    Consumer.from(CouponStreamConstants.COUPON_GROUP, consumerName),
                    StreamOffset.create(CouponStreamConstants.STREAM_KEY, ReadOffset.lastConsumed()),
                    couponStreamListener
            );
        }
        return container;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void createStreamConsumerGroups() {
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(CouponStreamConstants.STREAM_KEY))) {
                redisTemplate.opsForStream().add(CouponStreamConstants.STREAM_KEY, Map.of("init", "init"));
                log.info("Created stream: {}", CouponStreamConstants.STREAM_KEY);
            }

            if (Boolean.FALSE.equals(redisTemplate.hasKey(CouponStreamConstants.DLQ_STREAM_KEY))) {
                redisTemplate.opsForStream().add(CouponStreamConstants.DLQ_STREAM_KEY, Map.of("init", "init"));
                log.info("Created DLQ stream: {}", CouponStreamConstants.DLQ_STREAM_KEY);
            }

            List<String> groups = List.of(CouponStreamConstants.COUPON_GROUP, CouponStreamConstants.DLQ_GROUP);
            for (String group : groups) {
                try {
                    if (CouponStreamConstants.DLQ_GROUP.equals(group)) {
                        redisTemplate.opsForStream().createGroup(CouponStreamConstants.DLQ_STREAM_KEY, group);
                    } else {
                        redisTemplate.opsForStream().createGroup(CouponStreamConstants.STREAM_KEY, group);
                    }
                    log.info("Created consumer group: {}", group);
                } catch (RedisSystemException e) {
                    if (e.getCause() instanceof RedisBusyException) {
                        log.info("Consumer group already exists: {}", group);
                    } else {
                        log.error("Failed to create consumer group: {}", group, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize Redis streams", e);
        }
    }
}
