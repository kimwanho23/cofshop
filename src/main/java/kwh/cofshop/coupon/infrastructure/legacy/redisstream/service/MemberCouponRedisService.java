package kwh.cofshop.coupon.infrastructure.legacy.redisstream.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

import static kwh.cofshop.coupon.infrastructure.legacy.redisstream.worker.CouponStreamConstants.STREAM_KEY;

@RequiredArgsConstructor
@Service
public class MemberCouponRedisService {

    private final RedisTemplate<String, String> redisTemplate; // Redis

    // Producer - 발급 요청을 스트림에 적재
    public void enqueueLimitedCouponIssueRequest(Long memberId, Long couponId) {
        // Redis Stream에 메시지 추가
        redisTemplate.opsForStream().add(
                STREAM_KEY,
                Map.of("memberId", memberId.toString(), "couponId", couponId.toString())
        );
    }
}
