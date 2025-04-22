package kwh.cofshop.coupon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.coupon.dto.request.MemberCouponRequestDto;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberCouponRedisService {

    private final ObjectMapper objectMapper;

    private final RedisTemplate<String, String> redisTemplate; // Redis


    public boolean addToQueue(Long memberId, Long couponId) {
        // 수량 확인
        String stockStr = redisTemplate.opsForValue().get(stock(couponId));
        int stock = stockStr != null ? Integer.parseInt(stockStr) : 0;
        if (stock <= 0) return false;

        // 중복 발급 방지
        Long added = redisTemplate.opsForSet().add(issued(couponId), memberId.toString());
        if (added == null || added == 0) return false;

        // 큐에 추가 (ZSet, score는 현재 시간)
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(queue(couponId), memberId.toString(), now);
        return true;
    }


    public static String stock(Long couponId) {  // 재고
        return "coupon:stock:" + couponId;
    }

    public static String issued(Long couponId) { // 발급
        return "coupon:issued:" + couponId;
    }

    public static String queue(Long couponId) { // 큐에 적재
        return "coupon:queue:" + couponId;
    }
}
