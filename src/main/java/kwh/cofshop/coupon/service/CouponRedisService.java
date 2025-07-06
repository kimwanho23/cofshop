package kwh.cofshop.coupon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@RequiredArgsConstructor
@Service
public class CouponRedisService {
    private final RedisTemplate<String, String> redisTemplate; // Redis

    // 초기 재고 설정
    public void setInitCouponCount(Long couponId, Integer couponCount) {
        redisTemplate.opsForValue().set("coupon:stock:" + couponId, String.valueOf(couponCount));
    }

    // 재고 여부 확인
    public boolean hasStock(Long couponId) {
        String key = "coupon:stock:" + couponId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null && Integer.parseInt(value) > 0;
    }

    // 쿠폰 재고 회복
    public void restoreStock(Long couponId) {
        String key = "coupon:stock:" + couponId;
        redisTemplate.opsForValue().increment(key);
    }

    // 쿠폰 수량 감소
    public boolean decreaseStock(Long couponId) {
        String script =
                "local stock = redis.call('GET', KEYS[1]) " +
                        "if not stock or tonumber(stock) <= 0 then return 0 end " +
                        "redis.call('DECR', KEYS[1]) return 1";

        String key = "coupon:stock:" + couponId;

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key)
        );

        return result != null && result == 1;
    }

    // 회원의 쿠폰 발급 여부
    public boolean isAlreadyIssued(Long couponId, Long memberId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("coupon:issued:" + couponId, String.valueOf(memberId)));
    }

    // 회원의 쿠폰 발급 기록 저장
    public void saveIssued(Long couponId, Long memberId) {
        redisTemplate.opsForSet().add("coupon:issued:" + couponId, String.valueOf(memberId));
    }
}
