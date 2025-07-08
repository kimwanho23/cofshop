package kwh.cofshop.config;

import kwh.cofshop.coupon.worker.CouponStreamListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import io.lettuce.core.RedisBusyException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfig {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final CouponStreamListener couponStreamListener;

    public static final String STREAM_KEY = "stream:events";
    public static final String COUPON_GROUP = "consumer-group:coupon";
    private static final String CONSUMER_NAME = "coupon-consumer";
    // Cousumer 빈으로 등록
    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer() {
        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        var container = StreamMessageListenerContainer.create(redisConnectionFactory, options); // 컨테이너 생성

        // 리스너 그룹 생성, 해당 도메인의 그룹과 리스너를 매칭시킨다.
        Map<String, StreamListener<String, MapRecord<String, String, String>>> groupToListener = Map.of(
                COUPON_GROUP, couponStreamListener // 쿠폰 그룹은 쿠폰 리스너에 매칭
        );

        groupToListener.forEach((group, listener) -> container.receive(
                Consumer.from(group, CONSUMER_NAME),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                listener
        ));
        return container;
    }

    // XGROUP - ConsumerGroup 생성
    @EventListener(ContextRefreshedEvent.class)
    public void createStreamConsumerGroups() {
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(STREAM_KEY))) {
                redisTemplate.opsForStream().add(STREAM_KEY, Map.of("init", "init"));
                log.info("스트림 키 생성: '{}'", STREAM_KEY);
            }

            List<String> groups = List.of(COUPON_GROUP); // 후에 도메인이 추가될 시 List에 등록하면 된다.

            for (String group : groups) {
                try {
                    redisTemplate.opsForStream().createGroup(STREAM_KEY, group);
                    log.info("컨슈머 그룹 생성: '{}'", group);
                } catch (RedisSystemException e) {
                    if (e.getCause() instanceof RedisBusyException) {
                        log.info("컨슈머 그룹 이미 존재: '{}'", group);
                    } else {
                        log.error("컨슈머 그룹 생성 실패: '{}'", group, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("스트림 초기화 중 오류 발생", e);
        }
    }

}
