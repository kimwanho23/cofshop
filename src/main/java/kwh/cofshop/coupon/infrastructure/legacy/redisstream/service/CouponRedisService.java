package kwh.cofshop.coupon.infrastructure.legacy.redisstream.service;

import kwh.cofshop.coupon.domain.CouponIssueState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CouponRedisService {
    private final RedisTemplate<String, String> redisTemplate; // Redis

    private static final String STOCK_KEY = "coupon:stock:";
    private static final String ISSUED_SET_KEY = "coupon:issued:";
    private static final String ORDER_ZSET_KEY = "coupon:issued:order:";
    private static final String SEQ_KEY = "coupon:seq:";

    // 초기 재고 설정
    public void setInitCouponCount(Long couponId, Integer couponCount) {
        redisTemplate.opsForValue().set("coupon:stock:" + couponId, String.valueOf(couponCount));
    }

    // Lua 스크립트
    public CouponIssueState issueCoupon(Long couponId, Long memberId) {

        String script = """
                -- 1. 이미 발급된 사용자라면 종료
                if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
                    return {'already_issued', '0'}
                end
                
                -- 2. 재고 확인
                local stock = redis.call('GET', KEYS[1])
                if not stock then
                    return {'stock_not_initialized', '0'}
                end
                if tonumber(stock) <= 0 then
                    return {'out_of_stock', '0'}
                end
                
                -- 3. 순차 발급을 위한 score 조합
                local seq = redis.call('INCR', KEYS[4])
                local score = tonumber(ARGV[2]) * 1000000 + seq
                
                -- 4. 쿠폰 발급 처리
                redis.call('DECR', KEYS[1])
                redis.call('SADD', KEYS[2], ARGV[1])
                redis.call('ZADD', KEYS[3], score, ARGV[1])
                
                -- 5. 성공 응답
                return {'success', '1'}
                
                """;

        String stockKey = STOCK_KEY + couponId;       // coupon:stock:{couponId}
        String issuedSetKey = ISSUED_SET_KEY + couponId; // coupon:issued:{couponId}
        String orderZSetKey = ORDER_ZSET_KEY + couponId; // coupon:order:{couponId}
        String seqKey = SEQ_KEY + couponId;              // coupon:seq:{couponId}

        List<String> keys = List.of(stockKey, issuedSetKey, orderZSetKey, seqKey); // 재고, 중복, 순서, 발급
        List<String> args = List.of(String.valueOf(memberId), String.valueOf(System.currentTimeMillis()));

        List<?> result = redisTemplate.execute(
                new DefaultRedisScript<>(script, List.class),
                keys,
                args.toArray()
        );
        if (result == null || result.isEmpty() || result.get(0) == null) {
            throw new IllegalStateException("Redis Lua returned empty result");
        }

        String status = String.valueOf(result.get(0));

        return switch (status) {
            case "success" -> CouponIssueState.SUCCESS;
            case "already_issued" -> CouponIssueState.ALREADY_ISSUED;
            case "out_of_stock" -> CouponIssueState.OUT_OF_STOCK;
            case "stock_not_initialized" -> CouponIssueState.STOCK_NOT_INITIALIZED;
            default -> throw new IllegalStateException("Unknown Redis Lua result: " + status);
        };
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

    // Redis 선점 성공 이후 DB 저장 실패 시 되돌림
    public void rollbackIssuedCoupon(Long couponId, Long memberId) {
        String memberIdValue = String.valueOf(memberId);

        redisTemplate.opsForValue().increment(STOCK_KEY + couponId);
        redisTemplate.opsForSet().remove(ISSUED_SET_KEY + couponId, memberIdValue);
        redisTemplate.opsForZSet().remove(ORDER_ZSET_KEY + couponId, memberIdValue);
    }
}
