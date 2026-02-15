package kwh.cofshop.coupon.infrastructure.legacy.redisstream.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "coupon.stream.enabled", havingValue = "true")
public class CouponStreamMonitor {

    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelayString = "${coupon.stream.monitor.fixed-delay-ms:5000}")
    public void reportStreamStats() {
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        Long streamSize = ops.size(CouponStreamConstants.STREAM_KEY);

        var groups = ops.groups(CouponStreamConstants.STREAM_KEY);
        StreamInfo.XInfoGroup groupInfo = null;
        for (StreamInfo.XInfoGroup group : groups) {
            if (CouponStreamConstants.COUPON_GROUP.equals(group.groupName())) {
                groupInfo = group;
                break;
            }
        }

        if (groupInfo == null) {
            return;
        }

        log.info("[CouponStream] size={}, pending={}, consumers={}",
                streamSize,
                groupInfo.pendingCount(),
                groupInfo.consumerCount());
    }
}
