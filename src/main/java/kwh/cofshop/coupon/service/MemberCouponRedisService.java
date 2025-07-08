package kwh.cofshop.coupon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class MemberCouponRedisService {

    private final RedisTemplate<String, String> redisTemplate; // Redis

    public static final String STREAM_KEY = "stream:events";

    // Producer - 요청을 큐에 등록
    public void enqueueLimitedCouponIssueRequest(Long memberId, Long couponId) {
        // Redis Stream에 메시지 추가
        redisTemplate.opsForStream().add(
                STREAM_KEY,
                Map.of("memberId", memberId.toString(), "couponId", couponId.toString())
        );
    }
}
