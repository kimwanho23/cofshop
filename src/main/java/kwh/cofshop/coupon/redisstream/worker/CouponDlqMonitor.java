package kwh.cofshop.coupon.redisstream.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "coupon.stream.enabled", havingValue = "true")
public class CouponDlqMonitor {

    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(fixedDelayString = "${coupon.stream.monitor.fixed-delay-ms:5000}")
    public void reportDlqStats() {
        Long size = stringRedisTemplate.opsForStream().size(CouponStreamConstants.DLQ_STREAM_KEY);
        if (size == null || size == 0) {
            return;
        }
        log.warn("[CouponDLQ] pending failed messages size={}", size);
    }
}
